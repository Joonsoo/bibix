package com.giyeok.bibix.interpreter.coroutine

import com.giyeok.bibix.base.ProgressLogger
import com.giyeok.bibix.runner.BuildTaskRoutineId
import java.time.Instant

interface ProgressIndicatorContainer {
  fun notifyUpdated(progressIndicator: ProgressIndicator)

  fun ofCurrentThread(): ProgressIndicator
}

interface ProgressIndicator : ProgressLogger {
  val isFinished: Boolean

  fun markStarted(task: BuildTaskRoutineId)
  override fun logInfo(message: String)
  override fun logError(message: String)
  fun updateProgressDescription(description: String)
  fun markSuspended()
  fun markFinished()

  fun routineIdAndProgress(): Pair<BuildTaskRoutineId, String>?
}

class ProgressIndicatorImpl(val container: ProgressIndicatorContainer, val index: Int) :
  ProgressIndicator, ProgressLogger {
  private var routineId: BuildTaskRoutineId? = null
  private var startTime: Instant? = null
  private var endTime: Instant? = null
  private var description: String? = null

  override val isFinished: Boolean get() = endTime != null

  override fun markStarted(task: BuildTaskRoutineId) {
    synchronized(this) {
      this.routineId = task
      this.startTime = Instant.now()
      this.endTime = null
      this.description = "Starting..."
    }
    container.notifyUpdated(this)
  }

  override fun logInfo(message: String) {
    // TODO
  }

  override fun logError(message: String) {
    synchronized(this) {
      this.description = message
    }
    container.notifyUpdated(this)
  }

  override fun updateProgressDescription(description: String) {
    synchronized(this) {
      this.description = description
    }
    container.notifyUpdated(this)
  }

  override fun markSuspended() {
    synchronized(this) {
      this.endTime = Instant.now()
      this.description = "Suspended..."
    }
    container.notifyUpdated(this)
  }

  override fun markFinished() {
    synchronized(this) {
      this.endTime = Instant.now()
      this.description = "Finished"
    }
    container.notifyUpdated(this)
  }

  override fun routineIdAndProgress(): Pair<BuildTaskRoutineId, String>? = synchronized(this) {
    if (routineId == null) null else Pair(routineId!!, description!!)
  }
}
