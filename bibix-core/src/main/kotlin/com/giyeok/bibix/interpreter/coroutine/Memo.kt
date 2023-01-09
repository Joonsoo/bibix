package com.giyeok.bibix.interpreter.coroutine

import com.giyeok.bibix.base.BibixValue
import com.giyeok.bibix.base.SourceId
import com.giyeok.bibix.interpreter.expr.EvaluationResult

interface Memo {
  suspend fun memoize(
    sourceId: SourceId,
    exprId: Int,
    thisValue: BibixValue?,
    eval: suspend () -> EvaluationResult
  ): EvaluationResult
}

data class MemoToken(val sourceId: SourceId, val exprId: Int, val thisValue: BibixValue?)

class MemoImpl() : Memo {
  override suspend fun memoize(
    sourceId: SourceId,
    exprId: Int,
    thisValue: BibixValue?,
    eval: suspend () -> EvaluationResult
  ): EvaluationResult = eval()
}