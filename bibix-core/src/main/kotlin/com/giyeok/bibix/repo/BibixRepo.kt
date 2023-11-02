package com.giyeok.bibix.repo

import com.giyeok.bibix.base.BaseRepo
import com.giyeok.bibix.base.BibixValue
import com.giyeok.bibix.base.BuildContext
import com.giyeok.bibix.graph.BibixName
import com.giyeok.bibix.repo.BibixRepoProto.BibixRepoData
import com.giyeok.bibix.repo.BibixRepoProto.TargetState
import com.giyeok.bibix.repo.TargetStateKt.buildFailed
import com.giyeok.bibix.repo.TargetStateKt.buildSucceeded
import com.giyeok.bibix.runner.RunConfigProto
import com.giyeok.bibix.utils.toProto
import com.google.protobuf.Timestamp
import com.google.protobuf.empty
import com.google.protobuf.util.JsonFormat
import com.google.protobuf.util.Timestamps
import java.io.IOException
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
  val runConfig: RunConfigProto.RunConfig,
  // repo data
  val repoDataFile: Path,
  val repoData: BibixRepoData.Builder,
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
        Duration.between(lastUpdated, Instant.now()) >= Duration.ofSeconds(5)
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
    repoDataFile.writeText(JsonFormat.printer().print(repoData))
  }

  fun targetStarted(context: BuildContext) = synchronized(this) {
    val uniqueRunId = this.uniqueRunId
    repoData.putTargetIdData(context.targetId, context.targetIdData)
    repoData.putTargetStates(context.targetId, targetState {
      this.uniqueRunId = uniqueRunId
      this.buildStartTime = timeProvider().toProto()
      this.inputHashes = context.inputHashes
      this.inputHashString = context.inputHashString
      this.buildStarted = empty {}
    })
    saveRepoData()
  }

  fun targetSucceeded(targetId: String, resultValue: BibixValue) = synchronized(this) {
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
      synchronized(this) {
        repoData.putOutputNames(name.toString(), targetId)
      }
    }
    saveRepoData()
  }

  fun shutdown() {
    commitRepoData()
  }

  fun getTargetState(targetId: String): TargetState? = synchronized(this) {
    repoData.getTargetStatesOrDefault(targetId, null)
  }

  companion object {
    fun load(
      mainDirectory: Path,
      uniqueRunId: String,
      debuggingMode: Boolean = false
    ): BibixRepo {
      val bbxbuildDirectory = mainDirectory.resolve("bbxbuild")
      if (!Files.exists(bbxbuildDirectory)) {
        Files.createDirectory(bbxbuildDirectory)
      }
      val runConfigFile = bbxbuildDirectory.resolve("config.json")
      val runConfig = RunConfigProto.RunConfig.newBuilder()
      if (runConfigFile.exists()) {
        runConfigFile.bufferedReader().use { reader ->
          JsonFormat.parser().merge(reader, runConfig)
        }
      } else {
        runConfig.maxThreads = 3
        runConfigFile.writeText(JsonFormat.printer().print(runConfig))
      }
      val repoDataFile = bbxbuildDirectory.resolve("repo.json")
      val repoData = BibixRepoData.newBuilder()
      if (repoDataFile.exists()) {
        try {
          repoDataFile.bufferedReader().use { reader ->
            JsonFormat.parser().merge(reader, repoData)
          }
        } catch (e: IOException) {
          repoDataFile.writeText("{}")
          repoData.clearTargetIdData()
          repoData.clearTargetStates()
          repoData.clearOutputNames()
        }
        // TODO 파싱하다 오류 생기면 클리어하고 다시 시도
      } else {
        repoDataFile.writeText("{}")
      }

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
        runConfig = runConfig.build(),
        repoDataFile = repoDataFile,
        repoData = repoData,
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
