package com.giyeok.bibix.interpreter.hash

import com.giyeok.bibix.*
import com.giyeok.bibix.base.*
import com.giyeok.bibix.interpreter.BibixInterpreter
import com.giyeok.bibix.interpreter.expr.EvaluationResult
import com.giyeok.bibix.repo.extractInputHashes
import com.giyeok.bibix.repo.hashString
import com.giyeok.bibix.utils.toProto
import com.google.protobuf.empty

class ObjectHasher(private val interpreter: BibixInterpreter) {
// TODO proto는 항상 같은 bytes를 반환한다는 보장이 없기 때문에 개선 필요.
// 다만 보통은 같은 object -> 같은 해시가 나오고,
// 혹 다른 값이 나오더라도 그냥 불필요하게 추가로 빌드하는 상황이 발생할 수 있는 것일 뿐이라 큰 문제는 아님

  suspend fun hash(
    callingSourceId: SourceId,
    buildRule: EvaluationResult.RuleDef.BuildRuleDef,
    params: Map<String, BibixValue>
  ): ObjectHash {
    val callingSourceId = protoOf(callingSourceId)
    val argsMap = params.toArgsMapProto()
    val inputHashes = argsMap.extractInputHashes()
    val objectId = objectId {
      this.callingSourceId = callingSourceId
      when (buildRule) {
        is EvaluationResult.RuleDef.BuildRuleDef.PreloadedBuildRuleDef ->
          this.bibixVersion = Constants.BIBIX_VERSION

        is EvaluationResult.RuleDef.BuildRuleDef.UserBuildRuleDef ->
          this.ruleImplObjhash = buildRule.implValue.hashString()
      }
      this.className = buildRule.className
      this.methodName = buildRule.methodName
      this.argsMap = argsMap
      this.inputHashes = inputHashes
    }
    return ObjectHash(objectId, objectId.hashString())
  }

  private fun protoOf(sourceId: SourceId): BibixIdProto.SourceId = when (sourceId) {
    MainSourceId -> sourceId { this.mainSource = empty { } }
    is PreloadedSourceId -> sourceId { this.preloadedPlugin = sourceId.name }
    is ExternSourceId -> sourceId { this.externPluginObjhash = TODO() }
  }

  fun Map<String, BibixValue>.toArgsMapProto(): BibixIdProto.ArgsMap {
    val value = this
    return argsMap {
      this.pairs.addAll(value.entries.toList()
        .sortedBy { it.key }
        .map {
          argPair {
            this.name = it.key
            this.value = it.value.toProto()
          }
        })
    }
  }

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
}
