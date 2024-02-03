package com.giyeok.bibix.frontend.cli

import com.giyeok.bibix.frontend.ProgressNotifier
import com.giyeok.bibix.frontend.ThreadState
import com.giyeok.bibix.interpreter.BibixInterpreter
import com.giyeok.bibix.interpreter.TaskDescriptor
import com.giyeok.bibix.interpreter.task.Task
import kotlinx.coroutines.runBlocking
import java.time.Duration
import java.time.Instant

class ProgressConsolePrinter : ProgressNotifier {
  private lateinit var interpreter: BibixInterpreter
  private var lastPrinted: Instant? = null
  private var occupiedLines = 0

  override fun setInterpreter(interpreter: BibixInterpreter) {
    this.interpreter = interpreter
  }

  override fun notifyProgresses(progressesFunc: () -> List<ThreadState?>) {
    val now = Instant.now()
    if (lastPrinted == null || Duration.between(lastPrinted, now) >= Duration.ofMillis(500)) {
      lastPrinted = now
      repeat(occupiedLines) { print("\b\r") }
      val progresses = progressesFunc()
      occupiedLines = progresses.size
      progresses.forEachIndexed { index, state ->
        if (state == null || !state.isActive) {
          println("$index: IDLE")
        } else {
          println("$index: ${state.lastMessage.time} $state")
        }
        state?.task?.let { task ->
          TaskDescriptor(interpreter.g, interpreter.sourceManager).printTaskDescription(task)
        }
      }
    }
  }
}
