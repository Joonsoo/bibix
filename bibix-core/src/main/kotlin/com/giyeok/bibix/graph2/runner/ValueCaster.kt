package com.giyeok.bibix.graph2.runner

import com.giyeok.bibix.base.BibixType
import com.giyeok.bibix.base.BibixValue
import com.giyeok.bibix.graph2.BibixName
import com.giyeok.bibix.graph2.BuildGraph
import com.giyeok.bibix.graph2.ExprGraph

class ValueCaster(
  val projectId: Int,
  val projectPackageName: String?,
  val varRedefs: Map<BibixName, Map<Int, BuildGraph.VarCtx>>,
  val exprGraph: ExprGraph,
  val importInstanceId: Int,
) {
  fun castValue(value: BibixValue, type: BibixType): BuildTaskResult {
    // TODO
    return BuildTaskResult.ValueResult(value)
  }
}
