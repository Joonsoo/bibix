package com.giyeok.bibix.interpreter

import com.giyeok.bibix.interpreter.task.Task
import com.giyeok.bibix.interpreter.task.TaskRelGraph
import kotlinx.coroutines.runBlocking
import java.io.PrintWriter
import java.io.StringWriter

class TaskDescriptor(val g: TaskRelGraph, val sourceManager: SourceManager) {
  fun printTaskDescription(task: Task, writer: PrintWriter) {
    when (task) {
      is Task.EvalExpr -> runBlocking {
        val astNode = g.getReferredNodeById(task.sourceId, task.exprId)
        val projectPath = sourceManager.getProjectRoot(task.sourceId)
        val taskText = sourceManager.sourceText(task.sourceId, astNode!!)
        writer.println("EvalExpr ${task.sourceId} $projectPath ${astNode.start}-${astNode.end}")
        writer.println(taskText)
      }

      is Task.EvalCallExpr -> runBlocking {
        val astNode = g.getReferredNodeById(task.sourceId, task.exprId)
        val projectPath = sourceManager.getProjectRoot(task.sourceId)
        val taskText = sourceManager.sourceText(task.sourceId, astNode!!)
        writer.println("EvalCallExpr at ${task.sourceId} $projectPath ${astNode.start}-${astNode.end}")
        writer.println(taskText)
      }

      else -> {
        // Do nothing
      }
    }
  }

  fun taskDescription(task: Task): String {
    val writer = StringWriter()
    printTaskDescription(task, PrintWriter(writer))
    return writer.toString()
  }

  fun printTaskDescription(task: Task) {
    printTaskDescription(task, PrintWriter(System.out))
  }
}
