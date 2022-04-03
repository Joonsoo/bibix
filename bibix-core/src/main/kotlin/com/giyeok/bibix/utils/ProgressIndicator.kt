package com.giyeok.bibix.utils

import java.time.Instant

interface ProgressIndicatorContainer<T> {
  fun notifyUpdated(progressIndicator: ProgressIndicator<T>)
}

class ProgressIndicator<T>(val container: ProgressIndicatorContainer<T>, val index: Int) {
  private var task: T? = null
  private var startTime: Instant? = null
  private var endTime: Instant? = null
  private var description: String? = null

  val isFinished: Boolean get() = endTime != null

  fun markStarted(task: T) {
    synchronized(this) {
      this.task = task
      this.startTime = Instant.now()
      this.endTime = null
      this.description = "Starting..."
    }
    container.notifyUpdated(this)
  }

  fun infoLog(log: String) {
    // TODO
  }

  fun errorLog(log: String) {
    synchronized(this) {
      this.description = log
    }
    container.notifyUpdated(this)
  }

  fun updateProgressDescription(description: String) {
    synchronized(this) {
      this.description = description
    }
    container.notifyUpdated(this)
  }

  fun markFinished() {
    synchronized(this) {
      this.endTime = Instant.now()
      this.description = "Finished"
    }
    container.notifyUpdated(this)
  }

  fun currentProgressDescription(): String = synchronized(this) {
    if (task == null) {
      "..."
    } else {
      "$task: $description"
    }
  }
}
