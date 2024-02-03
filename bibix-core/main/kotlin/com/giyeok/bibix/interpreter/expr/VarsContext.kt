package com.giyeok.bibix.interpreter.expr

import com.giyeok.bibix.ast.BibixAst
import com.giyeok.bibix.base.SourceId
import com.giyeok.bibix.interpreter.ExprEvalContext

class VarsContext(
  val overriddenVars: Map<List<String>, Pair<ExprEvalContext, BibixAst.Expr>>,
  val sourcesPath: SourcesPath,
) {
  fun push(
    sourceId: SourceId,
    overridings: Map<List<String>, Pair<ExprEvalContext, BibixAst.Expr>>
  ): VarsContext {
    return VarsContext(overridings, SourcesPath.Cons(sourceId, sourcesPath))
  }

  constructor() : this(mapOf(), SourcesPath.Nil)

  sealed class SourcesPath {
    data class Cons(val sourceId: SourceId, val next: SourcesPath) : SourcesPath()
    object Nil : SourcesPath()
  }
}
