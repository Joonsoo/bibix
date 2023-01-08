package com.giyeok.bibix.interpreter

import com.giyeok.bibix.runner.ProgressIndicator
import com.giyeok.bibix.runner.ProgressIndicatorContainer
import com.giyeok.bibix.runner.ProgressIndicatorImpl

class FakeProgressIndicatorContainer : ProgressIndicatorContainer {
  override fun notifyUpdated(progressIndicator: ProgressIndicator) {
  }

  override fun ofCurrentThread(): ProgressIndicator {
    return ProgressIndicatorImpl(this, 0)
  }
}