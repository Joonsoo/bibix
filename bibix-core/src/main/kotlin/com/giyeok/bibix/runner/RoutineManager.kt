package com.giyeok.bibix.runner

import com.giyeok.bibix.buildscript.BuildGraph
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import kotlin.system.exitProcess

class RoutineManager(
  val buildGraph: BuildGraph,
  val coroutineDispatcher: RoutinesQueueCoroutineDispatcher
) {
  fun <T> executeSuspend(
    task: BuildTask,
    routine: suspend () -> T,
    callback: (T) -> Unit
  ) {
    // CoroutineContext에 task 추가
    CoroutineScope(coroutineDispatcher).launch(coroutineDispatcher + BuildTaskElement(task)) {
      try {
        val result = routine()
        callback(result)
      } catch (e: Exception) {
        markTaskFailed(BibixBuildException(task, e.message ?: "", e))
      }
    }
  }

  fun markTaskFailed(e: BibixBuildException) {
    when (val task = e.task) {
      is BuildTask.EvalExpr -> {
        val exprGraph = buildGraph.exprGraphs[task.exprGraphId]
        val (sourceId, parseNode) = exprGraph.exprLocation
        println("@ $sourceId, ${parseNode.range()}")
        println(parseNode.sourceText())
      }
      is BuildTask.CallAction -> {
        val exprGraph = buildGraph.exprGraphs[task.exprGraphId]
        val (sourceId, parseNode) = exprGraph.exprLocation
        println("@ $sourceId, ${parseNode.range()}")
        println(parseNode.sourceText())
      }
      else -> {
        // do nothing
      }
    }
    e.printStackTrace()
    exitProcess(1)
  }

  fun buildFinished(buildRequestName: String) {
    coroutineDispatcher.buildFinished(buildRequestName)
  }

  private val mutex = Mutex()
  private val startedTasks = mutableSetOf<BuildTask>()
  private val taskStates = ConcurrentHashMap<BuildTask, StateFlow<Any?>>()

  fun isTaskFinished(task: BuildTask): Boolean =
    taskStates[task]?.value != null

  suspend fun asyncInvoke(task: BuildTask, routine: suspend () -> Any) {
    mutex.withLock("asyncInvoke $task") {
      if (!startedTasks.contains(task)) {
        startedTasks.add(task)
        val taskState = MutableStateFlow<Any?>(null)
        taskStates[task] = taskState
        coroutineDispatcher.dispatch(currentCoroutineContext()) {
          executeSuspend(task, routine) { result ->
            taskState.compareAndSet(expect = null, update = result)
          }
        }
      }
    }
  }

  suspend fun waitForTaskResult(task: BuildTask): Any =
    taskStates.getValue(task).filterNotNull().first()
}
