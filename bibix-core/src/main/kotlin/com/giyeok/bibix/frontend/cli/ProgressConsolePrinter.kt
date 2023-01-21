package com.giyeok.bibix.frontend.cli

import com.giyeok.bibix.frontend.ProgressNotifier
import com.giyeok.bibix.frontend.ThreadState

class ProgressConsolePrinter : ProgressNotifier {
  private var occupiedLines = 0
  override fun notifyProgresses(progressesFunc: () -> List<ThreadState?>) = synchronized(this) {
    repeat(occupiedLines) { print("\b\r") }
    val progresses = progressesFunc()
    occupiedLines = progresses.size
    progresses.forEachIndexed { index, state ->
      println("$index: $state")
    }
  }
}
