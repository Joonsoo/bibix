package com.giyeok.bibix.repo

import com.giyeok.bibix.*
import com.giyeok.bibix.base.BibixValue
import com.giyeok.bibix.base.SourceId
import com.giyeok.bibix.utils.toProto
import com.google.protobuf.ByteString
import com.google.protobuf.kotlin.toByteString
import com.google.protobuf.util.Timestamps
import java.io.File
import java.security.MessageDigest

// TODO proto는 항상 같은 bytes를 반환한다는 보장이 없기 때문에 개선 필요.
// 다만 보통은 같은 object -> 같은 해시가 나오고,
// 혹 다른 값이 나오더라도 그냥 불필요하게 추가로 빌드하는 상황이 발생할 수 있는 것일 뿐이라 큰 문제는 아님

fun SourceId.toProto(): BibixIdProto.SourceId = sourceId {
  // TODO
}

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

fun BibixValue.hashString() = this.toProto().hashString()

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

fun BibixIdProto.RuleImplId.hashString(): ByteString =
  sha1Hash(this.toByteArray())

fun BibixIdProto.ObjectId.hashString(): ByteString =
  sha1Hash(this.toByteArray())

fun BibixIdProto.SourceId.hashString(): ByteString =
  sha1Hash(this.toByteArray())

fun inputHashesFromPaths(paths: List<String>): BibixIdProto.InputHashes {
  fun fileHashOf(file: File): BibixIdProto.FileHash {
    val digest = newDigest()
    val buffer = ByteArray(1000)
    file.inputStream().buffered().use { stream ->
      stream.read(buffer, 0, 1000)
      digest.update(buffer)
    }
    val fileHash = digest.digest().toByteString()

    // TODO file.lastModified()나 file.length() 사용해도 괜찮나..?
    return fileHash {
      this.path = file.path
      this.lastModifiedTime = Timestamps.fromMillis(file.lastModified())
      this.size = file.length()
      this.sha1Hash = fileHash
    }
  }

  fun traverseDirectory(file: File): BibixIdProto.DirectoryHash {
    val elems = (file.listFiles() ?: arrayOf()).sortedBy { it.name }
    return directoryHash {
      this.path = file.path
      elems.forEach { elem ->
        if (elem.isDirectory) {
          this.directories.add(traverseDirectory(elem))
        } else {
          this.files.add(fileHashOf(elem))
        }
      }
    }
  }

  return inputHashes {
    paths.distinct().sorted().map { File(it) }.forEach {
      if (it.isDirectory) {
        directories.add(traverseDirectory(it))
      } else {
        files.add(fileHashOf(it))
      }
    }
  }
}

fun BibixIdProto.ArgsMap.extractInputHashes(): BibixIdProto.InputHashes {
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

//sealed class RuleImplId
//
//data class NativeRuleImpl(val bibixVersion: String) : RuleImplId()
//data class RuleImplTargetId(val targetId: TargetId) : RuleImplId()
//
//data class TargetId(
//  val ruleImplId: RuleImplId,
//  val argsMap: ArgsMap,
//  val inputHashes: InputHashes,
//)
//
//data class ActionId(
//  val actionDefTargetId: TargetId,
//  val argsMap: ArgsMap,
//  val inputHashes: InputHashes,
//)
//
//data class ArgsMap(val args: Map<CName, BibixValue>) {
//  fun generateHash(): ByteString = TODO()
//}
//
//// path는 root script 기준 상대 경로
//
//class DirectoryHash(
//  val path: String,
//  val directories: List<DirectoryHash>,
//  val files: List<FileHash>,
//)
//
//class FileHash(
//  val path: String,
//  val lastModifiedTime: Timestamp,
//  val size: Long,
//  val sha1Hash: ByteString,
//)
//
//class InputHashes(
//  val directoryHash: List<DirectoryHash>,
//  val files: List<FileHash>,
//)
