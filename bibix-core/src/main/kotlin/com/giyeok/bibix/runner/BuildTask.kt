package com.giyeok.bibix.runner

import com.giyeok.bibix.base.BibixValue
import com.giyeok.bibix.base.CName
import com.giyeok.bibix.base.SourceId
import com.giyeok.bibix.buildscript.ExprNode
import com.giyeok.bibix.buildscript.ImportSource

sealed class BuildTask {
  data class BuildRequest(val buildRequestName: String) : BuildTask()

  data class ResolveName(val cname: CName) : BuildTask()

  data class ResolveImport(val origin: SourceId, val importDefId: Int) : BuildTask()

  data class ResolveImportSource(val origin: SourceId, val importSource: ImportSource) : BuildTask()

  data class EvalExpr(
    val origin: SourceId,
    val exprGraphId: Int,
    val exprNode: ExprNode,
    val thisValue: BibixValue?,
  ) : BuildTask()

  data class CallAction(val origin: SourceId, val exprGraphId: Int) : BuildTask()
}
