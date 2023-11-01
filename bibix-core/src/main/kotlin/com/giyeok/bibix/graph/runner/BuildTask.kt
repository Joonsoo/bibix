package com.giyeok.bibix.graph.runner

import com.giyeok.bibix.base.BibixType
import com.giyeok.bibix.base.BibixValue
import com.giyeok.bibix.base.ClassInstanceValue
import com.giyeok.bibix.graph.*
import java.lang.reflect.Method

sealed class BuildTask

data class EvalTarget(
  val projectId: Int,
  val importInstanceId: Int,
  val name: BibixName
): BuildTask()

data class ExecAction(
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
  val importInstanceId: Int,
): BuildTask()

data class FinalizeBuildRuleReturnValue(
  val buildRule: BuildTaskResult.BuildRuleResult,
  val value: BibixValue,
  val projectId: Int,
  val importInstanceId: Int,
): BuildTask()

data class EvalBuildRule(
  val projectId: Int,
  val importInstanceId: Int,
  val name: BibixName
): BuildTask()

data class EvalActionRule(
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
  val className: BibixName
): BuildTask()

data class EvalDataClassByName(
  val packageName: String,
  val className: String,
): BuildTask()

data class EvalSuperClassHierarchyByName(
  val packageName: String,
  val className: String
): BuildTask()

data class EvalType(val projectId: Int, val typeNodeId: TypeNodeId): BuildTask()

data class Import(
  val projectId: Int,
  val importInstanceId: Int,
  val importName: BibixName
): BuildTask()

data class ImportFromPrelude(val name: String, val remainingNames: List<String>): BuildTask()

data class ImportPreloaded(val pluginName: String): BuildTask()

data class NewImportInstance(
  val projectId: Int,
  val redefs: Map<BibixName, GlobalExprNodeId>
): BuildTask()

data class GlobalExprNodeId(
  val projectId: Int,
  val importInstanceId: Int,
  val exprNodeId: ExprNodeId
)


sealed class BuildTaskResult {
  sealed class FinalResult: BuildTaskResult()

  data class ValueResult(val value: BibixValue): FinalResult()

  data class ImportResult(
    val projectId: Int,
    val graph: BuildGraph,
    val namePrefix: List<String>
  ): FinalResult()

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

  data class ActionRuleResult(
    val projectId: Int
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

  data class SuperClassHierarchyResult(
    val projectId: Int,
    val packageName: String,
    val name: BibixName,
    val subTypes: List<SubType>,
  ): FinalResult() {
    data class SubType(val name: BibixName, val subs: List<SubType>) {
      val isDataClass = subs.isEmpty()
      val isSuperClass = subs.isNotEmpty()

      fun allSubDataClasses(): Set<BibixName> =
        if (isDataClass) setOf(name) else subs.flatMap { it.allSubDataClasses() }.toSet()
    }

    val allSubDataClasses: Set<BibixName>
      get() = subTypes.flatMap { it.allSubDataClasses() }.toSet()
  }

  class TypeCastFailResult(val value: BibixValue, val type: BibixType): FinalResult()
  class ValueFinalizeFailResult(): FinalResult()

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
