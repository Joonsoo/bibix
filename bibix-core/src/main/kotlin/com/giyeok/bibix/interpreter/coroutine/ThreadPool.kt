package com.giyeok.bibix.interpreter.coroutine

import com.giyeok.bibix.base.BibixValue
import com.giyeok.bibix.base.SourceId
import com.giyeok.bibix.interpreter.expr.EvaluationResult
import kotlinx.coroutines.Runnable

class ThreadPool(val numThreads: Int) : ProgressIndicatorContainer, Memo {
  override suspend fun memoize(
    sourceId: SourceId,
    exprId: Int,
    thisValue: BibixValue?,
    eval: suspend () -> EvaluationResult
  ): EvaluationResult {
    return eval()
  }

  override fun notifyUpdated(progressIndicator: ProgressIndicator) {
    TODO("Not yet implemented")
  }

  override fun ofCurrentThread(): ProgressIndicator {
    TODO("Not yet implemented")
  }

  fun execute(taskBlock: Runnable) {
    TODO()
  }

  fun printProgresses() {
    TODO("Not yet implemented")
  }
}
