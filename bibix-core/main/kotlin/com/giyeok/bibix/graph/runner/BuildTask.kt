package com.giyeok.bibix.graph.runner

import com.giyeok.bibix.base.BibixType
import com.giyeok.bibix.base.BibixValue
import com.giyeok.bibix.base.ClassInstanceValue
import com.giyeok.bibix.graph.*
import com.giyeok.bibix.plugins.jvm.ClassPkg

sealed class BuildTask
sealed class CacheableBuildTask: BuildTask()

// TODO BuildTask들은 ParallelGraphRunner에서 해시/동일성 비교가 계속 이루어지므로 가볍게 만들기
//  - 특히 BibixValue는 runner 같은 곳에 등록하고 ID만 사용하도록 하거나 하는 식으로 변경할 것

data class EvalTarget(
  val projectId: Int,
  val importInstanceId: Int,
  val name: BibixName
): CacheableBuildTask()

data class EvalAction(
  val projectId: Int,
  val importInstanceId: Int,
  val name: BibixName
): BuildTask()

data class ExecAction(
  val projectId: Int,
  val importInstanceId: Int,
  val actionName: BibixName,
  // val letLocals: Map<String, BibixValue>,
  val letLocalsId: Int,
): BuildTask()

data class ExecActionCallExpr(
  val projectId: Int,
  val importInstanceId: Int,
  val callStmt: ActionDef.CallStmt,
  val letLocals: Map<String, BibixValue>,
): BuildTask()

data class EvalExpr(
  val projectId: Int,
  val exprNodeId: ExprNodeId,
  val importInstanceId: Int,
  val localVars: Map<String, BibixValue>,
  val thisValue: ClassInstanceValue?,
): BuildTask() {
  constructor(
    projectId: Int,
    exprNodeId: ExprNodeId,
    importInstanceId: Int,
    thisValue: ClassInstanceValue?,
  ): this(projectId, exprNodeId, importInstanceId, mapOf(), thisValue)
}

data class TypeCastValue(
  val value: BibixValue,
  val type: BibixType,
  // valueProjectId는 value가 string일 때 path로 변경할 때 base directory를 판별하는 데 사용된다
  val valueProjectId: Int,
): BuildTask()

data class FinalizeBuildRuleReturnValue(
  // build rule이 정의된 위치에 대한 정보
  val buildRuleDefCtx: BuildRuleDefContext,
  val value: BibixValue,
  val projectId: Int,
): BuildTask()

// build rule이 정의된 위치의 정보
data class BuildRuleDefContext(val projectId: Int, val importInstanceId: Int, val name: BibixName) {
  companion object {
    fun from(buildRule: BuildTaskResult.BuildRuleResult): BuildRuleDefContext =
      BuildRuleDefContext(buildRule.projectId, buildRule.importInstanceId, buildRule.name)

    fun from(actionRule: BuildTaskResult.ActionRuleResult): BuildRuleDefContext =
      BuildRuleDefContext(actionRule.projectId, actionRule.importInstanceId, actionRule.name)
  }
}

data class EvalBuildRuleMeta(
  val projectId: Int,
  val importInstanceId: Int,
  val name: BibixName
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

data class EvalCallExpr(
  // buildRuleDefCtx가 eval and then으로 ruleName이라는 rule을 호출하려고 한다
  val buildRuleDefCtx: BuildRuleDefContext,
  val ruleName: BibixName,
  val projectId: Int,
  val importInstanceId: Int,
  val params: Map<String, BibixValue>
): CacheableBuildTask()

data class EvalCallee(
  val projectId: Int,
  val importInstanceId: Int,
  val callee: Callee
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

data class EvalSuperClass(
  val projectId: Int,
  val className: String
): BuildTask()

data class EvalSuperClassByName(
  val packageName: String,
  val className: String
): BuildTask()

data class EvalTypeByName(
  val packageName: String,
  val className: String
): BuildTask()

data class EvalType(
  val projectId: Int,
  // val importInstanceId: Int,
  val typeNodeId: TypeNodeId
): BuildTask()

data class Import(
  val projectId: Int,
  val importInstanceId: Int,
  val importName: BibixName
): BuildTask()

data class EvalImportSource(
  val projectId: Int,
  val importInstanceId: Int,
  val importName: BibixName,
  val importSource: ImportSource
): BuildTask()

data class ImportFromPrelude(val name: BibixName): BuildTask()

// TODO NewImportInstance는 굳이 build task로 안 만들어도 될듯한데?
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

  sealed class ResultWithValue: FinalResult() {
    abstract val value: BibixValue
    abstract fun withNewValue(newValue: BibixValue): ResultWithValue
  }

  data class ValueResult(override val value: BibixValue): ResultWithValue() {
    override fun withNewValue(newValue: BibixValue): ResultWithValue = ValueResult(newValue)
  }

  data class ValueOfTargetResult(
    override val value: BibixValue,
    val targetId: String
  ): ResultWithValue() {
    override fun withNewValue(newValue: BibixValue): ResultWithValue =
      ValueOfTargetResult(newValue, targetId)
  }

  data class ImportInstanceResult(val projectId: Int, val importInstanceId: Int): FinalResult()

  data class TypeResult(val type: BibixType): FinalResult()

  data class BuildRuleResult(
    val projectId: Int,
    val name: BibixName,
    // default field들을 evaluate할 때 사용할 import instance id
    val importInstanceId: Int,
    val buildRuleDef: BuildRuleDef,
    val paramTypes: List<Pair<String, BibixType>>,
    val impl: BuildRuleImpl,
    val implMethodName: String
  ): FinalResult()

  sealed class BuildRuleImpl {
    data class NativeImpl(val implInstance: Any): BuildRuleImpl()
    data class NonNativeImpl(val classPkg: ClassPkg, val implClassName: String): BuildRuleImpl()
  }

  // build rule을 값으로 받아가려고 하는 경우, BuildRuleResult를 사용하려고 하면 impl값을 미리 evaluate해야 하고,
  // impl에서 build rule 값을 사용하면 싸이클이 생기기 때문에 BuildRuleMeta를 별도로 둔다
  data class BuildRuleMetaResult(
    val projectId: Int,
    val name: BibixName,
    // default field들을 evaluate할 때 사용할 import instance id
    val importInstanceId: Int,
    val buildRuleDef: BuildRuleDef,
    val paramTypes: List<Pair<String, BibixType>>,
  ): FinalResult()

  data class ActionResult(
    val projectId: Int,
    val importInstanceId: Int,
    val actionName: BibixName,
    val actionDef: ActionDef
  ): FinalResult()

  data class ActionRuleResult(
    val projectId: Int,
    val name: BibixName,
    val importInstanceId: Int,
    val actionRuleDef: ActionRuleDef,
    val paramTypes: List<Pair<String, BibixType>>,
    val impl: BuildRuleImpl,
    val implMethodName: String
  ): FinalResult()

  data class DataClassResult(
    val projectId: Int,
    val packageName: String,
    val name: BibixName,
    // default field들을 evaluate할 때 사용할 var ctx id
    val importInstanceId: Int,
    val dataClassDef: DataClassDef,
    val fieldTypes: List<Pair<String, BibixType>>,
    val customCasts: List<Pair<BibixType, ExprNodeId>>,
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

  data class EnumTypeResult(
    val projectId: Int,
    val packageName: String,
    val name: BibixName,
    val values: List<String>
  ): FinalResult()

  data class TypeCastFailResult(val value: BibixValue, val type: BibixType): FinalResult()
  class ValueFinalizeFailResult(val values: List<BibixValue>): FinalResult()

  // action rule을 실행했는데 값을 반환한 경우 returnValue에 넣어준다.. 그런데 기본은 값이 없는게 정상
  class ActionRuleDoneResult(val returnValue: BibixValue?): FinalResult()

  data class WithResult(
    val task: BuildTask,
    val func: (FinalResult) -> BuildTaskResult
  ): BuildTaskResult()

  data class WithResultList(
    val tasks: List<BuildTask>,
    val func: (List<FinalResult>) -> BuildTaskResult
  ): BuildTaskResult()

  data class LongRunning constructor(
    val preCondition: suspend () -> Unit = {},
    val body: () -> Any,
    val postCondition: suspend () -> Unit = {},
    val after: suspend (Any) -> BuildTaskResult
  ): BuildTaskResult()

  data class DuplicateTargetResult(val targetId: String): BuildTaskResult()
}
