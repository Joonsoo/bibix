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
        writer.println("EvalExpr ${task.sourceId} $projectPath ${astNode::class.simpleName} ${astNode.start}-${astNode.end}")
        writer.println(taskText)
      }

      is Task.EvalCallExpr -> runBlocking {
        val astNode = g.getReferredNodeById(task.sourceId, task.exprId)
        val projectPath = sourceManager.getProjectRoot(task.sourceId)
        val taskText = sourceManager.sourceText(task.sourceId, astNode!!)
        writer.println("EvalCallExpr at ${task.sourceId} $projectPath ${astNode.start}-${astNode.end}")
        writer.println(taskText)
      }

      is Task.EvalDefinitionTask -> {}
      is Task.EvalName -> {}
      is Task.EvalType -> {}
      is Task.ExecuteAction -> {}
      is Task.ExecuteActionCall -> {}
      is Task.FindVarRedefsTask -> runBlocking {
        val projectPath = sourceManager.getProjectRoot(task.cname.sourceId)
        writer.println(projectPath)
      }

      is Task.LookupName -> {}
      is Task.PluginRequestedCallExpr -> {}

      is Task.ResolveImport -> runBlocking {
        val astNode = g.getReferredNodeById(task.sourceId, task.importDefId)
        val projectPath = sourceManager.getProjectRoot(task.sourceId)
        val taskText = sourceManager.sourceText(task.sourceId, astNode!!)
        writer.println("resolve import at ${task.sourceId} $projectPath ${astNode.start}-${astNode.end}")
        writer.println(taskText)
      }

      is Task.ResolveImportSource -> {}
      Task.RootTask -> {}
      is Task.UserBuildRequest -> {}
    }
  }

  fun taskDescription(task: Task): String {
    val writer = StringWriter()
    printTaskDescription(task, PrintWriter(writer))
    return writer.toString()
  }

  fun printTaskDescription(task: Task) {
    val writer = PrintWriter(System.out)
    printTaskDescription(task, writer)
    writer.flush()
  }
}
