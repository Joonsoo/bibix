package com.giyeok.bibix.repo

import com.giyeok.bibix.BibixIdProto.InputHashes
import com.giyeok.bibix.BibixIdProto.TargetIdData
import com.giyeok.bibix.base.BaseRepo
import com.giyeok.bibix.base.BibixValue
import com.giyeok.bibix.base.ProgressLogger
import com.giyeok.bibix.graph.BibixName
import com.giyeok.bibix.repo.BibixRepoProto.*
import com.giyeok.bibix.repo.TargetStateKt.buildFailed
import com.giyeok.bibix.repo.TargetStateKt.buildSucceeded
import com.giyeok.bibix.runner.RunConfigProto.RunConfig
import com.giyeok.bibix.utils.toProto
import com.google.protobuf.ByteString
import com.google.protobuf.Message
import com.google.protobuf.Timestamp
import com.google.protobuf.empty
import com.google.protobuf.util.Durations
import com.google.protobuf.util.JsonFormat
import com.google.protobuf.util.Timestamps
import java.nio.file.FileSystem
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.util.*
import kotlin.io.path.*

// bibix 빌드 폴더의 내용을 관리
class BibixRepo(
  val fileSystem: FileSystem,
  val timeProvider: () -> Instant,
  val uniqueRunId: String,
  val projectRoot: Path,
  val bbxbuildDirectory: Path,
  val runConfig: RunConfig,
  // repo data
  val repoDataFile: Path,
  val repoData: BibixRepoData.Builder,
  // target logs data
  val targetLogsFile: Path,
  val targetLogs: BibixTargetLogs.Builder,
  // targets
  val objectsDirectory: Path,
  val outputsDirectory: Path,
  // shared - maven cache 폴더 등
  val sharedRootDirectory: Path,
  val sharedDirectoriesMap: MutableMap<String, Path>,
  val directoryLocker: DirectoryLocker,
  val debuggingMode: Boolean = false,
): BaseRepo {
  private fun now() = Timestamps.fromMillis(System.currentTimeMillis())

  override fun prepareSharedDirectory(sharedRepoName: String): Path = synchronized(this) {
    val directory = sharedDirectoriesMap[sharedRepoName]
    if (directory == null) {
      val newDirectory = sharedRootDirectory.resolve(sharedRepoName)
      if (newDirectory.notExists()) {
        newDirectory.createDirectory()
        sharedDirectoriesMap[sharedRepoName] = newDirectory
      }
      newDirectory
    } else {
      directory
    }
  }

  private fun saveRepoData() {
    // TODO 임의로 5초에 한번만 저장하게 했는데 더 잘 할 수 없을까..
    val updateNeeded = synchronized(this) {
      if (lastUpdated == null ||
        Duration.between(lastUpdated, Instant.now()) >= Duration.ofSeconds(30)
      ) {
        lastUpdated = Instant.now()
        true
      } else {
        false
      }
    }
    if (updateNeeded) {
      commitRepoData()
    }
  }

  private fun commitRepoData() {
    synchronized(this) {
      repoDataFile.outputStream().buffered().use { writer ->
        repoData.build().writeTo(writer)
      }
      targetLogsFile.writeText(JsonFormat.printer().print(targetLogs))
    }
  }

  private val transientResults = mutableMapOf<String, BibixValue>()

  // withPrevState는 사용 가능한 기존의 target state가 있을 떄, 그 값을 재사용할 수 있는지 확인하기 위함
  fun <T> targetStarted(
    targetId: String,
    targetIdData: TargetIdData,
    inputHashes: InputHashes,
    inputHashString: ByteString,
    withPrevState: (prevState: TargetState, transientResult: BibixValue?) -> T?
  ): Pair<T?, TargetState?> = synchronized(this) {
    val uniqueRunId = this.uniqueRunId
    fun putData() {
      repoData.putTargetIdData(targetId, targetIdData)
      repoData.putTargetStates(targetId, targetState {
        this.uniqueRunId = uniqueRunId
        this.buildStartTime = timeProvider().toProto()
        this.inputHashes = inputHashes
        this.inputHashString = inputHashString
        this.buildStarted = empty {}
      })
      saveRepoData()
    }

    val prevState = repoData.getTargetStatesOrDefault(targetId, null)
    val transientResult = transientResults[targetId]
    // TODO 설정에 따라 이전 run에서 만든 것이라도 일정 시간 이내이면 재사용 시도하도록
    if (prevState == null) {
      // (이번 run에서) 처음 실행하는 것이면 putData하고 null(reuse할 것 없음), null(prevState 없음) 반환
      putData()
      Pair(null, null)
    } else {
      fun checkCanReuse(): Boolean {
        if (prevState.uniqueRunId == uniqueRunId) {
          // 같은 run에서 실행되는 같은 target id면 무조건 재사용 시도
          return true
        }
        if (!runConfig.hasTargetResultReuseDuration()) {
          // config에서 target result reuse를 설정하지 않았으면 재사용 불가
          return false
        }
        if (prevState.stateCase != TargetState.StateCase.BUILD_SUCCEEDED) {
          // 기존 결과가 실패였으면 재사용 불가
          return false
        }
        if (prevState.inputHashString != inputHashString) {
          // input hash값에 바뀐게 있으면 재사용 불가
          return false
        }
        // inputHashString이 동일하면 inputHashes도 같아야 함
        check(prevState.inputHashes == inputHashes)

        if (Durations.isNegative(runConfig.targetResultReuseDuration)) {
          // config에서 reuse duration이 음수이면 항상 재사용
          return true
        }

        val age =
          Timestamps.between(prevState.buildSucceeded.buildEndTime, timeProvider().toProto())
        // config에서 설정한 유효시간 이내이면 재사용
        return Durations.compare(age, runConfig.targetResultReuseDuration) < 0
      }

      val canReuse = checkCanReuse()
      val reuse = if (canReuse) withPrevState(prevState, transientResult) else null
      if (reuse == null) {
        // reuse할 것이 없으면 putData하고 null(reuse할 것 없음), prevState 반환
        putData()
      }
      Pair(reuse, prevState)
    }
  }

  fun targetSucceeded(
    targetId: String,
    resultValue: BibixValue,
    // isTransientResult 가 true이면 repo data에 기록하지 않고 이번 run 에서만 사용하고 없어진다
    isTransientResult: Boolean
  ) {
    if (isTransientResult) {
      synchronized(this) {
        transientResults[targetId] = resultValue
      }
    } else {
      synchronized(this) {
        val uniqueRunId = this.uniqueRunId
        val prevState = repoData.targetStatesMap[targetId]
        if (prevState == null) {
          // 오류상황인데.. 그냥 대충 넣고 지나가자
          repoData.putTargetStates(targetId, targetState {
            this.uniqueRunId = uniqueRunId
            this.buildSucceeded = buildSucceeded {
              this.buildEndTime = timeProvider().toProto()
              this.resultValue = resultValue.toProto()
            }
          })
        } else {
          repoData.putTargetStates(targetId, prevState.toBuilder().apply {
            this.buildSucceeded = buildSucceeded {
              this.buildEndTime = timeProvider().toProto()
              this.resultValue = resultValue.toProto()
            }
          }.build())
        }
        saveRepoData()
      }
    }
  }

  // TODO 필요한 곳에서 targetFailed 호출하도록 수정
  fun targetFailed(targetId: String, message: String) = synchronized(this) {
    val uniqueRunId = this.uniqueRunId
    val prevState = repoData.targetStatesMap[targetId]
    if (prevState == null) {
      // 오류상황인데.. 그냥 대충 넣고 지나가자
      repoData.putTargetStates(targetId, targetState {
        this.uniqueRunId = uniqueRunId
        this.buildFailed = buildFailed {
          this.buildFailTime = timeProvider().toProto()
          this.errorMessage = message
        }
      })
    } else {
      repoData.putTargetStates(targetId, prevState.toBuilder().apply {
        this.buildFailed = buildFailed {
          this.buildFailTime = timeProvider().toProto()
          this.errorMessage = message
        }
      }.build())
    }
    saveRepoData()
  }

  private var lastUpdated: Instant? = null

  // objects 폴더 밑에 targetId의 이름을 가진 폴더가 있으면 outputs 폴더에 링크를 만든다
  fun linkNameToObjectIfExists(name: BibixName, targetId: String) {
    val linkFile = outputsDirectory.resolve(name.toString())
    linkFile.deleteIfExists()
    val targetDirectory = objectsDirectory.resolve(targetId).normalize().absolute()
    if (targetDirectory.exists()) {
      linkFile.createSymbolicLinkPointingTo(targetDirectory)
    }
    synchronized(this) {
      repoData.putOutputNames(name.toString(), targetId)
    }
    saveRepoData()
  }

  fun shutdown() {
    commitRepoData()
  }

  fun getTargetState(targetId: String): TargetState? = synchronized(this) {
    repoData.getTargetStatesOrDefault(targetId, null)
  }

  fun getTargetIdData(targetId: String): TargetIdData? = synchronized(this) {
    repoData.getTargetIdDataOrDefault(targetId, null)
  }

  private inner class ProgressLoggerRepoImpl(
    private var logsBuilderOpt: LogBlocks.Builder? = null,
    private val initLogsBuilder: () -> LogBlocks.Builder
  ): ProgressLogger {
    private fun initializeLogsBuilder(): LogBlocks.Builder {
      check(logsBuilderOpt == null)
      val newBuilder = synchronized(this@BibixRepo) {
        initLogsBuilder()
      }
      logsBuilderOpt = newBuilder
      return newBuilder
    }

    private fun addLog(level: LogLevel, message: String) {
      if (level.number >= runConfig.minLogLevel.number) {
        val logsBuilder = synchronized(this) {
          logsBuilderOpt ?: initializeLogsBuilder()
        }
        val now = timeProvider().toProto()
        synchronized(this@BibixRepo) {
          logsBuilder.addBlocks(logBlock {
            this.level = level
            this.time = now
            this.message = message
          })
        }
        // TODO repo data와 targets log 저장 루틴을 다르게 해야..
        saveRepoData()
      }
    }

    override fun logVerbose(message: String) {
      addLog(LogLevel.VERBOSE, message)
    }

    override fun logInfo(message: String) {
      addLog(LogLevel.INFO, message)
    }

    override fun logError(message: String) {
      addLog(LogLevel.ERROR, message)
    }
  }

  fun progressLoggerFor(targetIdHex: String): ProgressLogger {
    // 어차피 같은 target은 두번 이상 실행되지 않기 때문에(그래야 하기 때문에) 기존의 targetLogs를 검색할 필요는 없다
    return ProgressLoggerRepoImpl {
      targetLogs.addTargetLogsBuilder()
        .setUniqueRunId(uniqueRunId)
        .setTargetId(targetIdHex)
        .blocksBuilder
    }
  }

  fun progressLoggerForAction(
    projectId: Int,
    importInstanceId: Int,
    name: BibixName
  ): ProgressLogger {
    return ProgressLoggerRepoImpl {
      targetLogs.addActionLogsBuilder()
        .setUniqueRunId(uniqueRunId)
        .setProjectId(projectId)
        .setImportInstanceId(importInstanceId)
        .setActionName(name.toString())
        .blocksBuilder
    }
  }

  companion object {
    private fun <T: Message.Builder> readJsonOrDefault(
      file: Path,
      builder: T,
      default: (T) -> T
    ): T {
      if (file.exists()) {
        try {
          file.bufferedReader().use { reader ->
            JsonFormat.parser().merge(reader, builder)
          }
          return builder
        } catch (_: Exception) {
        }
      }
      val defaultBuilder = default(builder)
      file.writeText(JsonFormat.printer().print(defaultBuilder))
      return defaultBuilder
    }

    fun load(
      mainDirectory: Path,
      targetLogFileName: String = "log.json",
      uniqueRunId: String = UniqueIdGen.generate(),
      debuggingMode: Boolean = false
    ): BibixRepo {
      val bbxbuildDirectory = mainDirectory.resolve("bbxbuild")
      if (!Files.exists(bbxbuildDirectory)) {
        Files.createDirectory(bbxbuildDirectory)
      }
      val runConfigFile = bbxbuildDirectory.resolve("config.json")
      val runConfig = readJsonOrDefault(runConfigFile, RunConfig.newBuilder()) { builder ->
        builder.clear()
          .setMaxThreads(8)
          .setMinLogLevel(LogLevel.INFO)
          .setTargetResultReuseDuration(Durations.fromHours(1))
      }.build()

      val repoDataFile = bbxbuildDirectory.resolve("repo.pb")
      val repoData = try {
        repoDataFile.inputStream().buffered().use { input ->
          BibixRepoData.parseFrom(input).toBuilder()
        }
      } catch (e: Exception) {
        repoDataFile.writeText("")
        BibixRepoData.newBuilder()
      }

      val targetLogsFile = bbxbuildDirectory.resolve(targetLogFileName)
      val targetLogs = BibixTargetLogs.newBuilder()
      // 기존 로그는 파싱하지 않고 날림

      val objectsDirectory = bbxbuildDirectory.resolve("objects")
      if (objectsDirectory.notExists()) {
        objectsDirectory.createDirectory()
      }
      val outputsDirectory = bbxbuildDirectory.resolve("outputs")
      if (outputsDirectory.notExists()) {
        outputsDirectory.createDirectory()
      }
      val sharedRootDirectory = bbxbuildDirectory.resolve("shared")
      if (sharedRootDirectory.notExists()) {
        sharedRootDirectory.createDirectory()
      }
      return BibixRepo(
        uniqueRunId = uniqueRunId,
        fileSystem = mainDirectory.fileSystem,
        timeProvider = Instant::now,
        projectRoot = mainDirectory,
        bbxbuildDirectory = bbxbuildDirectory,
        runConfig = runConfig,
        repoDataFile = repoDataFile,
        repoData = repoData,
        targetLogsFile = targetLogsFile,
        targetLogs = targetLogs,
        objectsDirectory = objectsDirectory,
        outputsDirectory = outputsDirectory,
        sharedRootDirectory = sharedRootDirectory,
        sharedDirectoriesMap = mutableMapOf(),
        directoryLocker = DirectoryLockerImpl(),
        debuggingMode = debuggingMode,
      )
    }
  }
}

fun Instant.toProto(): Timestamp = Timestamps.fromDate(Date.from(this))
