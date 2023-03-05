package com.giyeok.bibix.repo

import com.giyeok.bibix.BibixIdProto.InputHashes
import com.giyeok.bibix.base.BaseRepo
import com.giyeok.bibix.base.BibixValue
import com.giyeok.bibix.repo.BibixRepoProto.BibixRepo
import com.giyeok.bibix.repo.BibixRepoProto.TargetData
import com.giyeok.bibix.repo.BibixRepoProto.TargetState
import com.giyeok.bibix.runner.RunConfigProto
import com.giyeok.bibix.utils.toProto
import com.google.protobuf.ByteString
import com.google.protobuf.util.JsonFormat
import com.google.protobuf.util.Timestamps
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import java.nio.file.FileSystem
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import kotlin.io.path.*

// bibix 빌드 폴더의 내용을 관리
class Repo(
  val fileSystem: FileSystem,
  val projectRoot: Path,
  val bbxbuildDirectory: Path,
  val runConfig: RunConfigProto.RunConfig,
  // repo data
  val repoDataFile: Path,
  val targets: MutableMap<ByteString, TargetData.Builder>,
  val outputNames: MutableMap<String, ByteString>,
  // targets
  val objectsDirectory: Path,
  val outputsDirectory: Path,
  // shared - maven cache 폴더 등
  val sharedRootDirectory: Path,
  val sharedDirectoriesMap: MutableMap<String, Path>,
  val directoryLocker: DirectoryLocker,
  val debuggingMode: Boolean = false,
) : BaseRepo {
  private fun now() = Timestamps.fromMillis(System.currentTimeMillis())

  private val mutex = Mutex()

  data class ObjectDirectory(
    val objectIdHash: ByteString,
    val objectIdHashHex: String,
    val directory: Path,
    val inputHashChanged: Boolean,
  )

//  fun prepareObjectDirectory(
//    objectId: BibixIdProto.ObjectId,
//    inputHashes: BibixIdProto.InputHashes
//  ): ObjectDirectory {
//    val objectIdHash = objectId.hashString()
//    val objectIdHashHex = objectIdHash.toHexString()
//    val inputsHash = inputHashes.hashString()
//    // objectIdHash -> objectId, inputsHash 정보 저장
//    val hashChanged = synchronized(this) {
//      if (repoMeta.buildingTargetsMap.containsKey(objectIdHashHex)) true else {
//        val hashChanged = repoMeta.objectsMap[objectIdHashHex]?.inputsHash != inputsHash
//        repoMeta.putObjects(objectIdHashHex, objectInfo {
//          this.inputsHash = inputsHash
//          this.startTime = now()
//        })
//        repoMeta.putBuildingTargets(objectIdHashHex, true)
//        if (debuggingMode) {
//          // object id detail은 디버깅모드에서만 저장
//          repoMeta.putObjectIds(objectIdHashHex, objectId)
//        }
//        hashChanged
//      }
//    }
//    commitRepoMeta()
//    // object 디렉토리는 destDirectory를 가져갈때 생성하자. 아예 파일 output이 나오지 않는 빌드 룰도 꽤 많아서
//    return ObjectDirectory(
//      objectIdHash,
//      objectIdHashHex,
//      objectsDirectory.resolve(objectIdHashHex),
//      hashChanged
//    )
//  }

  override fun prepareSharedDirectory(
    sharedRepoName: String
  ): Path = synchronized(this) {
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

//  fun markFinished(objectId: BibixIdProto.ObjectId) {
//    synchronized(this) {
//      val targetIdHash = objectId.hashString().toHexString()
//      repoMeta.removeBuildingTargets(targetIdHash)
//      repoMeta.putObjects(targetIdHash, repoMeta.objectsMap[targetIdHash]?.copy {
//        this.endTime = now()
//        this.duration = Timestamps.between(this.startTime, this.endTime)
//      })
//    }
//    commitRepoMeta()
//  }

  private suspend fun commitRepoData() {
    repoDataFile.writeText(JsonFormat.printer().print(createRepoData()))
  }

  private suspend fun createRepoData(): BibixRepo = mutex.withLock {
    BibixRepo.newBuilder().also { builder ->
      targets.forEach { (_, targetData) ->
        builder.addTargets(targetData)
      }
      outputNames.forEach { (outputName, targetId) ->
        builder.addOutputNames(outputName {
          this.outputName = outputName
          this.targetId = targetId
        })
      }
    }.build()
  }

  private var lastUpdated: Instant? = null

  private suspend fun repoDataUpdated() {
    // TODO 임의로 5초에 한번만 저장하게 했는데 더 잘 할 수 없을까..
    val last = mutex.withLock { lastUpdated }
    if (last == null || Duration.between(last, Instant.now()) >= Duration.ofSeconds(5)) {
      mutex.withLock {
        lastUpdated = Instant.now()
      }
      commitRepoData()
    }
  }

  private suspend fun getTargetData(targetId: ByteString): TargetData.Builder? =
    mutex.withLock { targets[targetId] }

  private suspend fun updateTargetData(
    targetId: ByteString,
    updater: (TargetData.Builder) -> Unit
  ) {
    mutex.withLock {
      val builder = targets.getOrPut(targetId) {
        TargetData.newBuilder()
          .setTargetId(targetId)
      }

      updater(builder)
    }
    repoDataUpdated()
  }

  suspend fun putObjectHash(objectHash: ObjectHash) {
    val targetId = objectHash.targetId.targetIdBytes

    updateTargetData(targetId) { builder ->
      builder.targetIdData = objectHash.targetId.targetIdData
    }
  }

  suspend fun startBuildingTarget(targetIdBytes: ByteString, objectHash: ObjectHash) {
    updateTargetData(targetIdBytes) { builder ->
      val state = builder.stateBuilder
      state.buildStartTime = now()
      state.inputsHash = objectHash.inputsHash
      state.objectId = objectHash.objectId.objectIdBytes
    }
  }

  suspend fun targetBuildingFailed(targetIdBytes: ByteString, message: String) {
    updateTargetData(targetIdBytes) { builder ->
      val state = builder.stateBuilder
      state.buildFailedBuilder.buildFailTime = now()
      state.buildFailedBuilder.errorMessage = message
    }
  }

  suspend fun targetBuildingSucceeded(targetIdBytes: ByteString, resultValue: BibixValue) {
    updateTargetData(targetIdBytes) { builder ->
      val state = builder.stateBuilder
      state.buildSucceededBuilder.buildEndTime = now()
      state.buildSucceededBuilder.resultValue = resultValue.toProto()
    }
  }

  suspend fun linkNameToObject(nameTokens: List<String>, objectHash: ObjectHash) {
    val name = nameTokens.joinToString(".")
    val linkFile = outputsDirectory.resolve(name)
    linkFile.deleteIfExists()
    val targetDirectory = objectsDirectory.resolve(objectHash.targetId.targetIdHex).absolute()
    if (targetDirectory.exists()) {
      linkFile.createSymbolicLinkPointingTo(targetDirectory)
    }
    mutex.withLock {
      outputNames[name] = objectHash.targetId.targetIdBytes
    }
    repoDataUpdated()
  }

  suspend fun shutdown() {
    commitRepoData()
  }

  suspend fun getPrevInputsHashOf(targetId: ByteString): ByteString? =
    getTargetData(targetId)?.let { targetData ->
      if (targetData.state.stateCase == BibixRepoProto.TargetState.StateCase.BUILD_SUCCEEDED) {
        targetData.state.inputsHash
      } else {
        null
      }
    }

  suspend fun getPrevTargetState(targetId: ByteString): TargetState? =
    getTargetData(targetId)?.let { targetData ->
      targetData.state
    }

  companion object {
    fun load(mainDirectory: Path, debuggingMode: Boolean = false): Repo {
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
      val repoData = BibixRepo.newBuilder()
      if (repoDataFile.exists()) {
        try {
          repoDataFile.bufferedReader().use { reader ->
            JsonFormat.parser().merge(reader, repoData)
          }
        } catch (e: IOException) {
          repoDataFile.writeText("{}")
          repoData.clearTargets()
          repoData.clearOutputNames()
        }
        // TODO 파싱하다 오류 생기면 클리어하고 다시 시도
      } else {
        repoDataFile.writeText("{}")
      }
//      if (!debuggingMode) {
//        repoData.clearTargets()
//      }

      val targets = mutableMapOf<ByteString, TargetData.Builder>()
      repoData.targetsList.forEach { targetData ->
        targets[targetData.targetId] = targetData.toBuilder()
      }
      val outputNames = mutableMapOf<String, ByteString>()
      repoData.outputNamesList.forEach { outputName ->
        outputNames[outputName.outputName] = outputName.targetId
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
      return Repo(
        fileSystem = mainDirectory.fileSystem,
        projectRoot = mainDirectory,
        bbxbuildDirectory = bbxbuildDirectory,
        runConfig = runConfig.build(),
        repoDataFile = repoDataFile,
        targets = targets,
        outputNames = outputNames,
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
