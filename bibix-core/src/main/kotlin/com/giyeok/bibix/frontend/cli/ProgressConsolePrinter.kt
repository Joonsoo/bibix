package com.giyeok.bibix.frontend.cli

import com.giyeok.bibix.frontend.ProgressNotifier
import com.giyeok.bibix.frontend.ThreadState
import com.giyeok.bibix.interpreter.BibixInterpreter
import com.giyeok.bibix.interpreter.task.Task
import kotlinx.coroutines.runBlocking

class ProgressConsolePrinter : ProgressNotifier {
  private lateinit var interpreter: BibixInterpreter
  private var occupiedLines = 0

  override fun setInterpreter(interpreter: BibixInterpreter) {
    this.interpreter = interpreter
  }

  override fun notifyProgresses(progressesFunc: () -> List<ThreadState?>) {
    repeat(occupiedLines) { print("\b\r") }
    val progresses = progressesFunc()
    occupiedLines = progresses.size
    progresses.forEachIndexed { index, state ->
      if (state == null || !state.isActive) {
        println("$index: IDLE")
      } else {
        println("$index: ${state.lastMessage.time}")
        when (val task = state.task) {
          is Task.EvalExpr -> runBlocking {
            val astNode = interpreter.g.getReferredNodeById(task.sourceId, task.exprId)
            val projectPath = interpreter.sourceManager.getProjectRoot(task.sourceId)
            val taskText = interpreter.sourceManager.sourceText(task.sourceId, astNode!!)
            println("Evaluating expression at ${task.sourceId} $projectPath ${astNode.start}-${astNode.end}")
            println(taskText)
          }

          is Task.EvalCallExpr -> runBlocking {
            val astNode = interpreter.g.getReferredNodeById(task.sourceId, task.exprId)
            val projectPath = interpreter.sourceManager.getProjectRoot(task.sourceId)
            val taskText = interpreter.sourceManager.sourceText(task.sourceId, astNode!!)
            println("Evaluating call expression at ${task.sourceId} $projectPath ${astNode.start}-${astNode.end}")
            println(taskText)
          }

          else -> {
            // Do nothing
          }
        }
      }
    }
  }
}
