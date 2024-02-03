package com.giyeok.bibix.base

interface ProgressLogger {
  fun logVerbose(message: String)
  fun logInfo(message: String)
  fun logError(message: String)

  fun logException(exception: Exception) {
    logError(exception.stackTraceToString())
  }
}

object DummyProgressLogger: ProgressLogger {
  override fun logVerbose(message: String) {
    // do nothing
  }

  override fun logInfo(message: String) {
    // do nothing
  }

  override fun logError(message: String) {
    // do nothing
  }
}
