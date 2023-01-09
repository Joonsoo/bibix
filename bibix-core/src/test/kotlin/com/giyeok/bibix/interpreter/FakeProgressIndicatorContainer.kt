package com.giyeok.bibix.interpreter

import com.giyeok.bibix.interpreter.coroutine.ProgressIndicator
import com.giyeok.bibix.interpreter.coroutine.ProgressIndicatorContainer
import com.giyeok.bibix.interpreter.coroutine.ProgressIndicatorImpl

class FakeProgressIndicatorContainer : ProgressIndicatorContainer {
  override fun notifyUpdated(progressIndicator: ProgressIndicator) {
  }

  override fun ofCurrentThread(): ProgressIndicator {
    return ProgressIndicatorImpl(this, 0)
  }
}