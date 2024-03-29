package com.giyeok.bibix.interpreter.task

import com.giyeok.bibix.base.BibixValue
import com.giyeok.bibix.base.CName
import com.giyeok.bibix.base.SourceId
import com.giyeok.bibix.interpreter.expr.Definition
import com.giyeok.bibix.interpreter.expr.NameLookupContext

sealed class Task {
  object RootTask : Task()

  data class UserBuildRequest(val name: List<String>) : Task()

  // importDefId는 import def의 ast id
  data class ResolveImport(val sourceId: SourceId, val importDefId: Int) : Task()

  data class ResolveImportSource(val sourceId: SourceId, val importSourceId: Int) : Task()

  data class EvalExpr(val sourceId: SourceId, val exprId: Int, val thisValue: BibixValue?) :
    Task()

  data class EvalType(val sourceId: SourceId, val typeExprId: Int) : Task()

  data class EvalName(
    val nameLookupContext: NameLookupContext,
    val name: List<String>,
    val thisValue: BibixValue?
  ) : Task()

  data class EvalDefinitionTask(val definition: Definition, val thisValue: BibixValue?) : Task()

  data class LookupName(val nameLookupContext: NameLookupContext, val name: List<String>) : Task()

  data class PluginRequestedCallExpr(val sourceId: SourceId, val id: Int) : Task()

  data class EvalCallExpr(val sourceId: SourceId, val exprId: Int, val thisValue: BibixValue?) :
    Task()

  data class FindVarRedefsTask(val cname: CName) : Task()

  // actionDefId는 action def의 ast id
  data class ExecuteAction(val sourceId: SourceId, val actionDefId: Int) : Task()

  data class ExecuteActionCall(val sourceId: SourceId, val actionDefId: Int) : Task()
}
