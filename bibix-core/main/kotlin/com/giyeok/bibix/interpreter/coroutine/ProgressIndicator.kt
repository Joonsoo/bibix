package com.giyeok.bibix.interpreter.coroutine

import com.giyeok.bibix.base.ProgressLogger
import com.giyeok.bibix.frontend.ProgressMessage
import com.giyeok.bibix.frontend.ThreadState
import com.giyeok.bibix.interpreter.task.Task
import java.time.Instant

// Task별로 시작 시각 -> 종료 시각 기록
// Thread별로 거쳐간 Task들(과 각각의 시작/종료 시각), 현재 상태
// TaskRelGraph와 연동해야 할까? Task간의 관계..
interface ProgressIndicatorContainer {
  fun notifyUpdated(progressIndicator: ProgressIndicator)

  fun ofCurrentThread(): ProgressIndicator
}

class ProgressIndicator(val container: ProgressIndicatorContainer, val threadIdx: Int) :
  ProgressLogger {
  private var task: Task? = null
  private var isActive: Boolean = false
  private var lastMessage: ProgressMessage = ProgressMessage(Instant.now(), "D", "Created")

  fun setTask(task: Task) = synchronized(this) {
    this.task = task
    this.isActive = true
  }

  fun setFinished() = synchronized(this) {
    this.isActive = false
  }

  override fun logVerbose(message: String) = synchronized(this) {
    this.lastMessage = ProgressMessage(Instant.now(), "V", message)
    container.notifyUpdated(this)
  }

  override fun logInfo(message: String) = synchronized(this) {
    this.lastMessage = ProgressMessage(Instant.now(), "I", message)
    container.notifyUpdated(this)
  }

  override fun logError(message: String) = synchronized(this) {
    this.lastMessage = ProgressMessage(Instant.now(), "E", message)
    container.notifyUpdated(this)
  }

  fun toThreadState(): ThreadState? = task?.let { ThreadState(it, isActive, lastMessage) }
}
