package com.giyeok.bibix.runner

import com.giyeok.bibix.base.BaseRepo
import com.giyeok.bibix.utils.toHexString
import com.google.protobuf.ByteString
import com.google.protobuf.util.JsonFormat
import com.google.protobuf.util.Timestamps
import java.io.File
import java.nio.file.Files
import kotlin.io.path.deleteIfExists

// bibix 빌드 폴더의 내용을 관리
class Repo(
  val mainDirectory: File,
  val bibixDirectory: File,
  val runConfig: RunConfigProto.RunConfig,
  val repoMetaFile: File,
  val repoMeta: BibixRepoProto.BibixRepo.Builder,
  // targets
  val objectsDirectory: File,
  val outputsDirectory: File,
  // sources
  val sourcesDirectory: File,
  // shared - maven cache 폴더 등
  val sharedRootDirectory: File,
  val sharedDirectoriesMap: MutableMap<String, File>,
  val debuggingMode: Boolean = false,
) : BaseRepo {
  private fun now() = Timestamps.fromMillis(System.currentTimeMillis())

  data class ObjectDirectory(
    val objectIdHash: ByteString,
    val objectIdHashHex: String,
    val directory: File,
    val hashChanged: Boolean,
  )

  fun prepareObjectDirectory(
    objectId: BibixIdProto.ObjectId,
    inputHashes: BibixIdProto.InputHashes
  ): ObjectDirectory {
    val objectIdHash = objectId.hashString()
    val objectIdHashHex = objectIdHash.toHexString()
    val inputsHash = inputHashes.hashString()
    // objectIdHash -> objectId, inputsHash 정보 저장
    val hashChanged = synchronized(this) {
      if (repoMeta.buildingTargetsMap.containsKey(objectIdHashHex)) true else {
        val hashChanged = repoMeta.objectsMap[objectIdHashHex]?.inputsHash != inputsHash
        repoMeta.putObjects(objectIdHashHex, objectInfo {
          this.inputsHash = inputsHash
          this.startTime = now()
        })
        repoMeta.putBuildingTargets(objectIdHashHex, true)
        if (debuggingMode) {
          // object id detail은 디버깅모드에서만 저장
          repoMeta.putObjectIds(objectIdHashHex, objectId)
        }
        hashChanged
      }
    }
    commitRepoMeta()
    // object 디렉토리는 destDirectory를 가져갈때 생성하자. 아예 파일 output이 나오지 않는 빌드 룰도 꽤 많아서
    return ObjectDirectory(
      objectIdHash,
      objectIdHashHex,
      File(objectsDirectory, objectIdHashHex),
      hashChanged
    )
  }

  fun prepareSourceDirectory(
    sourceId: BibixIdProto.SourceId,
  ): File {
    val sourceIdHash = sourceId.hashString().toHexString()

    val directory = File(sourcesDirectory, sourceIdHash)
    directory.mkdir()
    return directory
  }

  override fun prepareSharedDirectory(
    sharedRepoName: String
  ): File = synchronized(this) {
    val directory = sharedDirectoriesMap[sharedRepoName]
    if (directory == null) {
      val newDirectory = File(sharedRootDirectory, sharedRepoName)
      newDirectory.mkdir()
      newDirectory
    } else {
      directory
    }
  }

  fun markFinished(objectId: BibixIdProto.ObjectId) {
    synchronized(this) {
      val targetIdHash = objectId.hashString().toHexString()
      repoMeta.removeBuildingTargets(targetIdHash)
      repoMeta.putObjects(targetIdHash, repoMeta.objectsMap[targetIdHash]?.copy {
        this.endTime = now()
        this.duration = Timestamps.between(this.startTime, this.endTime)
      })
    }
    commitRepoMeta()
  }

  private fun commitRepoMeta() = synchronized(this) {
    repoMetaFile.writeText(JsonFormat.printer().print(repoMeta))
  }

  fun linkNameTo(name: String, targetId: BibixIdProto.ObjectId) {
    val targetIdHash = targetId.hashString().toHexString()
    val linkFile = File(outputsDirectory, name).toPath()
    linkFile.deleteIfExists()
    val targetDirectory = File(objectsDirectory, targetIdHash).canonicalFile
    if (targetDirectory.exists()) {
      Files.createSymbolicLink(
        linkFile,
        targetDirectory.toPath(),
      )
    }
    synchronized(this) {
      repoMeta.putObjectNames(name, targetIdHash)
    }
    commitRepoMeta()
  }

  fun finalize() {
    commitRepoMeta()
  }

  companion object {
    fun load(mainDirectory: File, debuggingMode: Boolean = false): Repo {
      val bibixDirectory = File(mainDirectory, "bbxbuild")
      bibixDirectory.mkdir()
      val runConfigFile = File(bibixDirectory, "config.json")
      val runConfig = RunConfigProto.RunConfig.newBuilder()
      if (runConfigFile.exists()) {
        JsonFormat.parser().merge(runConfigFile.reader().buffered(), runConfig)
      } else {
        runConfig.maxThreads = 3
        runConfigFile.writeText(JsonFormat.printer().print(runConfig))
      }
      val repoMetaFile = File(bibixDirectory, "repo.json")
      val repoMeta = BibixRepoProto.BibixRepo.newBuilder()
      if (repoMetaFile.exists()) {
        JsonFormat.parser().merge(repoMetaFile.reader().buffered(), repoMeta)
        // TODO 파싱하다 오류 생기면 클리어하고 다시 시도
      } else {
        repoMetaFile.writeText("{}")
      }
      if (!debuggingMode) {
        repoMeta.clearObjectIds()
      }
      val objectsDirectory = File(bibixDirectory, "objects")
      objectsDirectory.mkdir()
      val outputsDirectory = File(bibixDirectory, "outputs")
      outputsDirectory.mkdir()
      val sourcesDirectory = File(bibixDirectory, "sources")
      sourcesDirectory.mkdir()
      val sharedRootDirectory = File(bibixDirectory, "shared")
      sharedRootDirectory.mkdir()
      return Repo(
        mainDirectory = mainDirectory,
        bibixDirectory = bibixDirectory,
        runConfig = runConfig.build(),
        repoMetaFile = repoMetaFile,
        repoMeta = repoMeta,
        objectsDirectory = objectsDirectory,
        outputsDirectory = outputsDirectory,
        sourcesDirectory = sourcesDirectory,
        sharedRootDirectory = sharedRootDirectory,
        sharedDirectoriesMap = mutableMapOf(),
        debuggingMode = debuggingMode,
      )
    }
  }
}
