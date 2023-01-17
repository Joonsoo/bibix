package com.giyeok.bibix.frontend

import com.giyeok.bibix.interpreter.task.Task
import java.time.Instant

interface ProgressNotifier {
  fun notifyProgresses(progressesFunc: () -> List<ThreadState?>)
}

data class ThreadState(val task: Task, val isActive: Boolean, val lastMessage: ProgressMessage)

data class ProgressMessage(val time: Instant, val level: String, val message: String)
