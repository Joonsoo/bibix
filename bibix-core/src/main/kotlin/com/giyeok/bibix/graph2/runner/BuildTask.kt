package com.giyeok.bibix.graph2.runner

import com.giyeok.bibix.base.BibixType
import com.giyeok.bibix.base.BibixValue
import com.giyeok.bibix.base.ClassInstanceValue
import com.giyeok.bibix.graph2.*
import java.lang.reflect.Method

sealed class BuildTask

data class EvalTarget(
  val projectId: Int,
  val importInstanceId: Int,
  val name: BibixName
): BuildTask()

data class EvalExpr(
  val projectId: Int,
  val exprNodeId: ExprNodeId,
  val importInstanceId: Int,
  val thisValue: ClassInstanceValue?
): BuildTask()

data class TypeCastValue(
  val value: BibixValue,
  val type: BibixType,
  val projectId: Int,
  val importInstanceId: Int
): BuildTask()

data class EvalBuildRule(
  val projectId: Int,
  val importInstanceId: Int,
  val name: BibixName
): BuildTask()

data class EvalVar(
  val projectId: Int,
  val importInstanceId: Int,
  val name: BibixName
): BuildTask()

data class EvalDataClass(
  val projectId: Int,
  val importInstanceId: Int,
  val name: BibixName
): BuildTask()

data class EvalType(val projectId: Int, val typeNodeId: TypeNodeId): BuildTask()

data class Import(val projectId: Int, val varCtxId: Int, val importName: BibixName): BuildTask()

data class ImportFromPrelude(val name: String, val remainingNames: List<String>): BuildTask()

data class ImportPreloaded(val pluginName: String): BuildTask()

data class NewImportInstance(
  val projectId: Int,
  val redefs: Map<BibixName, GlobalExprNodeId>
): BuildTask()

data class GlobalExprNodeId(val projectId: Int, val varCtxId: Int, val exprNodeId: ExprNodeId)


sealed class BuildTaskResult {
  sealed class FinalResult: BuildTaskResult()

  data class ValueResult(val value: BibixValue): FinalResult()

  data class ImportResult(val projectId: Int, val graph: BuildGraph): FinalResult()

  data class ImportInstanceResult(val projectId: Int, val importInstanceId: Int): FinalResult()

  data class TypeResult(val type: BibixType): FinalResult()

  data class BuildRuleResult(
    val projectId: Int,
    val name: BibixName,
    // default field들을 evaluate할 때 사용할 import instance id
    val importInstanceId: Int,
    val buildRuleDef: BuildRuleDef,
    val paramTypes: List<Pair<String, BibixType>>,
    val implInstance: Any,
    val implMethod: Method
  ): FinalResult()

  data class DataClassResult(
    val projectId: Int,
    val packageName: String,
    val name: BibixName,
    // default field들을 evaluate할 때 사용할 var ctx id
    val importInstanceId: Int,
    val dataClassDef: DataClassDef,
    val fieldTypes: List<Pair<String, BibixType>>,
  ): FinalResult()

  class TypeCastFailResult(val value: BibixValue, val type: BibixType): FinalResult()

  data class WithResult(
    val task: BuildTask,
    val func: (FinalResult) -> BuildTaskResult
  ): BuildTaskResult()

  data class WithResultList(
    val tasks: List<BuildTask>,
    val func: (List<FinalResult>) -> BuildTaskResult
  ): BuildTaskResult()

  data class LongRunning(val func: () -> BuildTaskResult): BuildTaskResult()

  data class SuspendLongRunning(val func: suspend () -> BuildTaskResult): BuildTaskResult()
}
