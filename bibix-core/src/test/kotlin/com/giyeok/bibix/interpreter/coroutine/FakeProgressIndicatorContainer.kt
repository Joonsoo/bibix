package com.giyeok.bibix.interpreter.coroutine

class FakeProgressIndicatorContainer : ProgressIndicatorContainer {
  override fun notifyUpdated(progressIndicator: ProgressIndicator) {
  }

  override fun ofCurrentThread(): ProgressIndicator {
    return ProgressIndicator(this, 0)
  }
}
