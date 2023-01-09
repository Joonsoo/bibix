package com.giyeok.bibix.interpreter.task

import com.giyeok.bibix.base.BibixValue
import com.giyeok.bibix.base.SourceId

sealed class Task {
  data class UserBuildRequest(val name: List<String>) : Task()

  // importDefId는 import def의 ast id
  data class ResolveImport(val sourceId: SourceId, val importDefId: Int) : Task()

  data class ResolveImportSource(val sourceId: SourceId, val importSourceId: Int) : Task()

  data class EvalExpr(val sourceId: SourceId, val exprId: Int, val thisValue: BibixValue?) :
    Task()

  data class EvalCallExpr(val sourceId: SourceId, val exprId: Int, val thisValue: BibixValue?) :
    Task()

  // actionDefId는 action def의 ast id
  data class ExecuteAction(val sourceId: SourceId, val actionDefId: Int) : Task()

  data class ExecuteActionCall(val sourceId: SourceId, val actionDefId: Int) : Task()
}
