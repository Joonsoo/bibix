package com.giyeok.bibix.runner

import com.giyeok.bibix.buildscript.BuildGraph
import java.util.*
import java.util.concurrent.Executors

class ThreadPool(
  val buildGraph: BuildGraph,
  val routineManager: RoutineManager,
  val maxThreads: Int
) : ProgressIndicatorContainer {
  private val progressIndicatorThreadLocal = ThreadLocal<ProgressIndicator>()
  private val runningTasks: MutableList<BuildTaskRoutineId?> = MutableList(maxThreads) { null }
  private val progressIndicators = List(maxThreads) { idx -> ProgressIndicatorImpl(this, idx) }
  private val executors = List(maxThreads) { idx ->
    val executor = Executors.newSingleThreadExecutor()
    executor.execute {
      progressIndicatorThreadLocal.set(progressIndicators[idx])
    }
    executor
  }
  val tasksQueue = LinkedList<Pair<BuildTaskRoutineId, Runnable>>()

  override fun ofCurrentThread(): ProgressIndicator =
    progressIndicatorThreadLocal.get()!!

  override fun notifyUpdated(progressIndicator: ProgressIndicator) = synchronized(this) {
    printProgresses()
  }

  fun execute(routineId: BuildTaskRoutineId, block: Runnable) {
    // 몇번째 스레드에서 실행할 지 결정
    val threadIndex: Int? = findAndMarkSlot(routineId)
    if (threadIndex != null) {
      executeAt(threadIndex, routineId, block)
    } else {
      // 당장 비어있는 스레드가 없으면 큐에 넣어두기
      synchronized(this) {
        tasksQueue.add(Pair(routineId, block))
      }
    }
  }

  private fun String.oneLine(): String {
    val lines = this.split('\n')
    return lines.joinToString("") { it.trim() }
  }

  private fun taskDescription(task: BuildTask): String =
    when (task) {
      is BuildTask.BuildRequest -> {
        "rootTask(${task.buildRequestName})"
      }
      is BuildTask.EvalExpr -> {
        val exprGraph = buildGraph.exprGraphs[task.exprGraphId]
        val (sourceId, parseNode) = exprGraph.exprLocation
        "@$sourceId ${parseNode.sourceText().oneLine()}"
      }
      is BuildTask.CallAction -> {
        val exprGraph = buildGraph.exprGraphs[task.exprGraphId]
        val (sourceId, parseNode) = exprGraph.exprLocation
        "@$sourceId, ${parseNode.sourceText().oneLine()}"
      }
      else -> {
        task.toString()
      }
    }

  fun printProgresses() = synchronized(this) {
//    progressIndicators.forEach { progressIndicator ->
//      val taskAndProgress = progressIndicator.routineIdAndProgress()
//      val progressDescription = if (taskAndProgress == null) {
//        "..."
//      } else {
//        val (routineId, progressDescripton) = taskAndProgress
//        val taskString = taskDescription(routineId.buildTask)
//        val frontLength = 80
//        val endLength = 20
//        val shortString = if (taskString.length <= frontLength + endLength) taskString else {
//          taskString.substring(0, frontLength - 1) + ".." +
//            taskString.substring(taskString.length - endLength - 1)
//        }
//        "$shortString(${routineId.id}): $progressDescripton"
//      }
//      println("${progressIndicator.index}: $progressDescription")
//    }
  }

  private fun findAndMarkSlot(routineId: BuildTaskRoutineId): Int? = synchronized(this) {
    val idx = runningTasks.indexOf(null)
    if (idx < 0) null else {
      runningTasks[idx] = routineId
      idx
    }
  }

  private fun executeAt(threadIndex: Int, task: BuildTaskRoutineId, block: Runnable) {
    val progressIndicator = progressIndicators[threadIndex]
    progressIndicator.markStarted(task)
    executors[threadIndex].execute {
      block.run()
      synchronized(this) {
        runningTasks[threadIndex] = null
        if (routineManager.isTaskFinished(task.buildTask)) {
          progressIndicator.markFinished()
        } else {
          progressIndicator.markSuspended()
        }
        // 큐에 있는 태스크 빼서 실행하기
        startNextQueue()
      }
    }
  }

  private fun startNextQueue() {
    synchronized(this) {
      val nextTask = tasksQueue.peek()
      if (nextTask != null) {
        val threadId = findAndMarkSlot(nextTask.first)
        if (threadId != null) {
          val removed = tasksQueue.remove()
          check(nextTask === removed)
          executeAt(threadId, nextTask.first, nextTask.second)
        }
      }
    }
  }
}
