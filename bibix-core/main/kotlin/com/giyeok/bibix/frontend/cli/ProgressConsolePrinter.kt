package com.giyeok.bibix.frontend.cli

import com.giyeok.bibix.frontend.ProgressNotifier
import com.giyeok.bibix.frontend.ThreadState
import com.giyeok.bibix.interpreter.BibixInterpreter
import com.giyeok.bibix.interpreter.task.Task
import com.github.ajalt.mordant.animation.animation
import com.github.ajalt.mordant.rendering.TextColors.cyan
import com.github.ajalt.mordant.rendering.TextColors.red
import com.github.ajalt.mordant.rendering.TextStyles.bold
import com.github.ajalt.mordant.rendering.TextStyles.dim
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.widgets.Text
import java.time.Duration
import java.time.Instant

class ProgressConsolePrinter : ProgressNotifier {
  private lateinit var interpreter: BibixInterpreter
  private var lastPrinted: Instant? = null
  private var spinnerIdx = 0

  private val spinnerFrames = listOf("⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏")
  private val terminal = Terminal()
  private val animation = terminal.animation<List<ThreadState?>> { states ->
    val activeStates = states.filterNotNull().filter { it.isActive }
    val now = Instant.now()
    val spinner = cyan(spinnerFrames[spinnerIdx])
    Text(buildString {
      if (activeStates.isEmpty()) {
        append("$spinner ${dim("대기 중...")}")
      } else {
        activeStates.forEachIndexed { i, state ->
          if (i > 0) appendLine()
          val label = bold(taskShortLabel(state.task))
          val msg = when (state.lastMessage.level) {
            "E" -> red(state.lastMessage.message.take(80))
            "V" -> dim(state.lastMessage.message.take(80))
            else -> state.lastMessage.message.take(80)
          }
          val elapsed = formatElapsed(Duration.between(state.lastMessage.time, now))
          append("$spinner $label  $msg  ${dim("($elapsed)")}")
        }
      }
    })
  }

  override fun setInterpreter(interpreter: BibixInterpreter) {
    this.interpreter = interpreter
  }

  override fun notifyProgresses(progressesFunc: () -> List<ThreadState?>) {
    val now = Instant.now()
    if (lastPrinted == null || Duration.between(lastPrinted, now) >= Duration.ofMillis(100)) {
      lastPrinted = now
      spinnerIdx = (spinnerIdx + 1) % spinnerFrames.size
      animation.update(progressesFunc())
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
