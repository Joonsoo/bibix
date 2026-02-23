package com.giyeok.bibix.frontend.cli

import com.giyeok.bibix.frontend.ProgressNotifier
import com.giyeok.bibix.frontend.ThreadState
import com.giyeok.bibix.interpreter.BibixInterpreter
import com.giyeok.bibix.interpreter.task.Task
import java.time.Duration
import java.time.Instant

class ProgressConsolePrinter : ProgressNotifier {
  private lateinit var interpreter: BibixInterpreter
  private var lastPrinted: Instant? = null
  private var occupiedLines = 0
  private var spinnerIdx = 0

  // ANSI escape codes
  private val ESC = "\u001B"
  private val CURSOR_UP = "$ESC[1A"
  private val ERASE_LINE = "$ESC[2K"
  private val RESET = "$ESC[0m"
  private val BOLD = "$ESC[1m"
  private val CYAN = "$ESC[36m"
  private val YELLOW = "$ESC[33m"
  private val GREEN = "$ESC[32m"
  private val RED = "$ESC[31m"
  private val DIM = "$ESC[2m"

  private val SPINNER_FRAMES = listOf("⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏")

  override fun setInterpreter(interpreter: BibixInterpreter) {
    this.interpreter = interpreter
  }

  override fun notifyProgresses(progressesFunc: () -> List<ThreadState?>) {
    val now = Instant.now()
    if (lastPrinted == null || Duration.between(lastPrinted, now) >= Duration.ofMillis(100)) {
      lastPrinted = now

      // 이전에 출력한 줄들을 위로 올라가며 지운다
      if (occupiedLines > 0) {
        repeat(occupiedLines) {
          print(CURSOR_UP + ERASE_LINE + "\r")
        }
      }

      val progresses = progressesFunc()
      val activeStates = progresses.filterNotNull().filter { it.isActive }

      spinnerIdx = (spinnerIdx + 1) % SPINNER_FRAMES.size
      val spinner = CYAN + SPINNER_FRAMES[spinnerIdx] + RESET

      val lines = mutableListOf<String>()

      if (activeStates.isEmpty()) {
        lines += "$spinner $DIM대기 중...$RESET"
      } else {
        for (state in activeStates) {
          val taskLabel = taskShortLabel(state.task)
          val level = state.lastMessage.level
          val msg = state.lastMessage.message.take(80)

          val levelColor = when (level) {
            "E" -> RED
            "V" -> DIM
            else -> ""
          }
          val msgColored = "$levelColor$msg$RESET"

          val elapsed = Duration.between(state.lastMessage.time, now)
          val elapsedStr = formatElapsed(elapsed)

          lines += "$spinner $BOLD$taskLabel$RESET  $msgColored  $DIM($elapsedStr)$RESET"
        }
      }

      occupiedLines = lines.size
      lines.forEach { println(it) }
    }
  }

  private fun taskShortLabel(task: Task): String = when (task) {
    is Task.EvalExpr -> "eval"
    is Task.EvalCallExpr -> "call"
    is Task.EvalDefinitionTask -> "def"
    is Task.EvalName -> "name"
    is Task.EvalType -> "type"
    is Task.ExecuteAction -> "action"
    is Task.ExecuteActionCall -> "action-call"
    is Task.FindVarRedefsTask -> "var-redef"
    is Task.LookupName -> "lookup"
    is Task.PluginRequestedCallExpr -> "plugin-call"
    is Task.ResolveImport -> "import"
    is Task.ResolveImportSource -> "import-src"
    Task.RootTask -> "root"
    is Task.UserBuildRequest -> "build"
    else -> task::class.simpleName ?: "task"
  }

  private fun formatElapsed(duration: Duration): String {
    val secs = duration.seconds
    val ms = duration.toMillisPart()
    return if (secs > 0) "${secs}s" else "${ms}ms"
  }
}
