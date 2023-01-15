package com.giyeok.bibix.repo

import com.giyeok.bibix.BibixIdProto
import com.giyeok.bibix.base.BaseRepo
import com.giyeok.bibix.runner.RunConfigProto
import com.giyeok.bibix.utils.toHexString
import com.google.protobuf.ByteString
import com.google.protobuf.util.JsonFormat
import com.google.protobuf.util.Timestamps
import java.nio.file.FileSystem
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.*

// bibix 빌드 폴더의 내용을 관리
class Repo(
  val fileSystem: FileSystem,
  val mainDirectory: Path,
  val bibixDirectory: Path,
  val runConfig: RunConfigProto.RunConfig,
  val repoMetaFile: Path,
  val repoMeta: BibixRepoProto.BibixRepo.Builder,
  // targets
  val objectsDirectory: Path,
  val outputsDirectory: Path,
  // sources
  val sourcesDirectory: Path,
  // shared - maven cache 폴더 등
  val sharedRootDirectory: Path,
  val sharedDirectoriesMap: MutableMap<String, Path>,
  val debuggingMode: Boolean = false,
) : BaseRepo {
  private fun now() = Timestamps.fromMillis(System.currentTimeMillis())

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

//  fun prepareSourceDirectory(
//    sourceId: BibixIdProto.SourceId,
//  ): Path {
//    val sourceIdHash = sourceId.hashString().toHexString()
//
//    val directory = sourcesDirectory.resolve(sourceIdHash)
//    if (directory.notExists()) {
//      directory.createDirectory()
//    }
//    return directory
//  }

  override fun prepareSharedDirectory(
    sharedRepoName: String
  ): Path = synchronized(this) {
    val directory = sharedDirectoriesMap[sharedRepoName]
    if (directory == null) {
      val newDirectory = sharedRootDirectory.resolve(sharedRepoName)
      if (newDirectory.notExists()) {
        newDirectory.createDirectory()
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

  private fun commitRepoMeta() = synchronized(this) {
    Files.writeString(repoMetaFile, JsonFormat.printer().print(repoMeta))
  }

//  fun linkNameTo(name: String, targetId: BibixIdProto.ObjectId) {
//    val targetIdHash = targetId.hashString().toHexString()
//    val linkFile = outputsDirectory.resolve(name)
//    linkFile.deleteIfExists()
//    val targetDirectory = objectsDirectory.resolve(targetIdHash).absolute()
//    if (Files.exists(targetDirectory)) {
//      Files.createSymbolicLink(linkFile, targetDirectory)
//    }
//    synchronized(this) {
//      repoMeta.putObjectNames(name, targetIdHash)
//    }
//    commitRepoMeta()
//  }

  fun finalize() {
    commitRepoMeta()
  }

  companion object {
    fun load(mainDirectory: Path, debuggingMode: Boolean = false): Repo {
      val bibixDirectory = mainDirectory.resolve("bbxbuild")
      if (!Files.exists(bibixDirectory)) {
        Files.createDirectory(bibixDirectory)
      }
      val runConfigFile = bibixDirectory.resolve("config.json")
      val runConfig = RunConfigProto.RunConfig.newBuilder()
      if (Files.exists(runConfigFile)) {
        Files.newBufferedReader(runConfigFile).use { reader ->
          JsonFormat.parser().merge(reader, runConfig)
        }
      } else {
        runConfig.maxThreads = 3
        runConfigFile.writeText(JsonFormat.printer().print(runConfig))
      }
      val repoMetaFile = bibixDirectory.resolve("repo.json")
      val repoMeta = BibixRepoProto.BibixRepo.newBuilder()
      if (Files.exists(repoMetaFile)) {
        Files.newBufferedReader(repoMetaFile).use { reader ->
          JsonFormat.parser().merge(reader, repoMeta)
        }
        // TODO 파싱하다 오류 생기면 클리어하고 다시 시도
      } else {
        repoMetaFile.writeText("{}")
      }
      if (!debuggingMode) {
        repoMeta.clearObjectIds()
      }
      val objectsDirectory = bibixDirectory.resolve("objects")
      if (objectsDirectory.notExists()) {
        objectsDirectory.createDirectory()
      }
      val outputsDirectory = bibixDirectory.resolve("outputs")
      if (outputsDirectory.notExists()) {
        outputsDirectory.createDirectory()
      }
      val sourcesDirectory = bibixDirectory.resolve("sources")
      if (sourcesDirectory.notExists()) {
        sourcesDirectory.createDirectory()
      }
      val sharedRootDirectory = bibixDirectory.resolve("shared")
      if (sharedRootDirectory.notExists()) {
        sharedRootDirectory.createDirectory()
      }
      return Repo(
        fileSystem = mainDirectory.fileSystem,
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
