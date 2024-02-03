package com.giyeok.bibix.frontend

import com.giyeok.bibix.interpreter.BibixInterpreter
import com.giyeok.bibix.interpreter.task.Task
import java.time.Instant

interface ProgressNotifier {
  fun setInterpreter(interpreter: BibixInterpreter)

  fun notifyProgresses(progressesFunc: () -> List<ThreadState?>)
}

data class ThreadState(val task: Task, val isActive: Boolean, val lastMessage: ProgressMessage)

data class ProgressMessage(val time: Instant, val level: String, val message: String)

class NoopProgressNotifier : ProgressNotifier {
  override fun setInterpreter(interpreter: BibixInterpreter) {
    // Do nothing
  }

  override fun notifyProgresses(progressesFunc: () -> List<ThreadState?>) {
    // Do nothing
  }
}
