package com.giyeok.bibix.interpreter.task

import com.giyeok.bibix.base.BibixValue
import com.giyeok.bibix.base.CName
import com.giyeok.bibix.base.SourceId

sealed class Task {
  data class UserBuildRequest(val name: CName) : Task()

  // importDefId는 import def의 ast id
  data class ResolveImport(val sourceId: SourceId, val importDefId: Int) : Task()

  data class EvaluateExpr(val sourceId: SourceId, val exprId: Int, val thisValue: BibixValue?) :
    Task()

  // actionDefId는 action def의 ast id
  data class ExecuteAction(val sourceId: SourceId, val actionDefId: Int) : Task()
}
