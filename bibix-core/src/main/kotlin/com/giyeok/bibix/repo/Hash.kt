package com.giyeok.bibix.repo

import com.giyeok.bibix.*
import com.giyeok.bibix.base.BibixValue
import com.giyeok.bibix.utils.toProto
import com.google.protobuf.ByteString
import com.google.protobuf.kotlin.toByteString
import com.google.protobuf.util.Timestamps
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.*

fun newDigest() = MessageDigest.getInstance("SHA-1")

fun sha1Hash(bytes: ByteString): ByteString {
  // TODO MessageDigest를 매번 얻어와야하나?
  // TODO bytes.toByteArray()할 때랑 ByteString.copyFrom 할 때 메모리 카피 발생. 최적화 필요
  val digest = newDigest()
  return digest.digest(bytes.toByteArray()).toByteString()
}

fun sha1Hash(bytes: ByteArray): ByteString {
  // TODO MessageDigest를 매번 얻어와야하나?
  // TODO bytes.toByteArray()할 때랑 ByteString.copyFrom 할 때 메모리 카피 발생. 최적화 필요
  val digest = newDigest()
  return digest.digest(bytes).toByteString()
}

fun BibixValueProto.BibixValue.hashString(): ByteString =
  sha1Hash(this.toByteString())

fun BibixIdProto.ArgsMap.hashString(): ByteString {
  val argPairsList = this.pairsList.sortedBy { it.name }
  // TODO sha1Hash 함수에서와 동일한 문제. 수정 필요
  val digest = newDigest()
  argPairsList.forEach {
    digest.update(it.toByteArray())
  }
  return digest.digest().toByteString()
}

fun BibixIdProto.SourceId.hashString(): ByteString =
  sha1Hash(this.toByteArray())

suspend fun inputHashesFromPaths(paths: List<String>): BibixIdProto.InputHashes {
  fun fileHashOf(path: Path): BibixIdProto.FileHash {
    val digest = newDigest()
    val buffer = ByteArray(1000)
    path.inputStream().buffered().use { stream ->
      stream.read(buffer, 0, 1000)
      digest.update(buffer)
    }
    val fileHash = digest.digest().toByteString()

    // TODO file.lastModified()나 file.length() 사용해도 괜찮나..?
    return fileHash {
      this.path = path.pathString
      this.lastModifiedTime = Timestamps.fromMillis(Files.getLastModifiedTime(path).toMillis())
      this.size = Files.size(path)
      this.sha1Hash = fileHash
    }
  }

  fun traverseDirectory(path: Path): BibixIdProto.DirectoryHash {
    val elems = path.listDirectoryEntries().sortedBy { it.name }
    return directoryHash {
      this.path = path.pathString
      elems.forEach { elem ->
        if (Files.isDirectory(elem)) {
          this.directories.add(traverseDirectory(elem))
        } else {
          this.files.add(fileHashOf(elem))
        }
      }
    }
  }

  return inputHashes {
    paths.distinct().sorted().map { Path(it) }.forEach { path ->
      if (Files.isDirectory(path)) {
        directories.add(traverseDirectory(path))
      } else {
        if (path.exists()) {
          files.add(fileHashOf(path))
        }
      }
    }
  }
}

fun BibixValue.hashString() = this.toProto().hashString()

suspend fun BibixIdProto.ArgsMap.extractInputHashes(): BibixIdProto.InputHashes {
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
  return inputHashesFromPaths(this.pairsList.flatMap { traverseValue(it.value) })
}

fun BibixIdProto.InputHashes.hashString() =
  sha1Hash(this.toByteArray())
