package com.giyeok.bibix.graph.runner

import com.giyeok.bibix.*
import com.giyeok.bibix.repo.newDigest
import com.google.protobuf.kotlin.toByteString
import com.google.protobuf.util.Timestamps
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.*

// File hash는 동일한 빌드 run에서는 파일이 변경되지 않는다고 가정하고 해시값을 캐시해놓는 곳
class FileHashStore {
  val fileHashCache: MutableMap<Path, BibixIdProto.FileHash> = mutableMapOf()
  val directoryHashCache: MutableMap<Path, BibixIdProto.DirectoryHash> = mutableMapOf()

  private inline fun <T> withCache(
    path: Path,
    cacheMap: MutableMap<Path, T>,
    calculateHash: () -> T
  ): T {
    val existing = synchronized(this) { cacheMap[path] }
    if (existing != null) {
      return existing
    }

    val newHash = calculateHash()
    synchronized(this) { cacheMap[path] = newHash }
    return newHash
  }

  // race condition으로 인해서 같은 파일의 해시를 두번 계산해도 큰 문제는 없으니 그냥 쉽게 가자
  private fun fileHashOf(path: Path): BibixIdProto.FileHash =
    withCache(path, fileHashCache) {
      val digest = newDigest()
      val buffer = ByteArray(1000)
      path.inputStream().buffered().use { stream ->
        val read = stream.read(buffer, 0, 1000)
        if (read > 0) {
          digest.update(buffer, 0, read)
        }
      }
      val fileHash = digest.digest().toByteString()

      fileHash {
        this.path = path.pathString
        this.lastModifiedTime = Timestamps.fromMillis(Files.getLastModifiedTime(path).toMillis())
        this.size = Files.size(path)
        this.sha1Hash = fileHash
      }
    }

  private fun directoryHashOf(path: Path): BibixIdProto.DirectoryHash =
    withCache(path, directoryHashCache) {
      val elems = path.listDirectoryEntries().sortedBy { it.name }
      directoryHash {
        this.path = path.pathString
        elems.forEach { elem ->
          if (Files.isDirectory(elem)) {
            this.directories.add(directoryHashOf(elem))
          } else {
            this.files.add(fileHashOf(elem))
          }
        }
      }
    }

  private fun getHashOf(path: Path): Pair<BibixIdProto.FileHash?, BibixIdProto.DirectoryHash?> =
    synchronized(this) {
      Pair(fileHashCache[path], directoryHashCache[path])
    }

  fun extractInputHashes(argsMap: BibixIdProto.ArgsMap): BibixIdProto.InputHashes {
    fun traverseValue(value: BibixValueProto.BibixValue): List<String> = when (value.valueCase) {
      BibixValueProto.BibixValue.ValueCase.PATH_VALUE -> listOf(value.pathValue)
      BibixValueProto.BibixValue.ValueCase.FILE_VALUE -> listOf(value.fileValue)
      BibixValueProto.BibixValue.ValueCase.DIRECTORY_VALUE -> listOf(value.directoryValue)
      BibixValueProto.BibixValue.ValueCase.LIST_VALUE ->
        value.listValue.valuesList.flatMap { traverseValue(it) }

      BibixValueProto.BibixValue.ValueCase.SET_VALUE ->
        value.setValue.valuesList.flatMap { traverseValue(it) }

      BibixValueProto.BibixValue.ValueCase.TUPLE_VALUE ->
        value.tupleValue.valuesList.flatMap { traverseValue(it) }

      BibixValueProto.BibixValue.ValueCase.NAMED_TUPLE_VALUE ->
        value.namedTupleValue.valuesList.flatMap { traverseValue(it.value) }

      BibixValueProto.BibixValue.ValueCase.DATA_CLASS_INSTANCE_VALUE ->
        value.dataClassInstanceValue.fieldsList.flatMap { traverseValue(it.value) }

      else -> listOf()
    }
    return inputHashesFromPaths(argsMap.pairsList.flatMap { traverseValue(it.value) })
  }

  fun inputHashesFromPaths(paths: List<String>): BibixIdProto.InputHashes = inputHashes {
    val sortedPaths = paths.map { Path(it).normalize().absolute() }.distinct().sorted()
    sortedPaths.forEach { path ->
      val (fileHash, directoryHash) = getHashOf(path)
      if (fileHash != null || directoryHash != null) {
        if (fileHash != null) {
          check(directoryHash == null)
          files.add(fileHash)
        } else {
          check(directoryHash != null)
          directories.add(directoryHash)
        }
      } else {
        if (Files.isDirectory(path)) {
          directories.add(directoryHashOf(path))
        } else {
          if (path.exists()) {
            files.add(fileHashOf(path))
          }
        }
      }
    }
  }
}
