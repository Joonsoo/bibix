package com.giyeok.bibix.utils

import java.util.*
import java.util.concurrent.Executors

class ThreadPool<T>(val maxThreads: Int) : ProgressIndicatorContainer<T> {
  val runningTasks: MutableList<T?> = MutableList(maxThreads) { null }
  val executors = List(maxThreads) { Executors.newSingleThreadExecutor() }
  val progressIndicators = List(maxThreads) { ProgressIndicator(this, it) }
  val tasksQueue = LinkedList<Pair<T, (ProgressIndicator<T>) -> Unit>>()

  override fun notifyUpdated(progressIndicator: ProgressIndicator<T>) = synchronized(this) {
    // TODO 화면에 표시되는 progress 업데이트
    printProgresses()
  }

  fun execute(task: T, block: (ProgressIndicator<T>) -> Unit) {
    // 몇번째 스레드에서 실행할 지 결정
    val threadIndex: Int? = findAndMarkSlot(task)
    if (threadIndex != null) {
      executeAt(threadIndex, task, block)
    } else {
      // 당장 비어있는 스레드가 없으면 큐에 넣어두기
      synchronized(this) {
        tasksQueue.add(Pair(task, block))
      }
    }
  }

  fun printProgresses() = synchronized(this) {
    progressIndicators.forEach { progressIndicator ->
      println("${progressIndicator.index}: ${progressIndicator.currentProgressDescription()}")
    }
  }

  private fun findAndMarkSlot(task: T): Int? = synchronized(this) {
    val idx = runningTasks.indexOf(null)
    if (idx < 0) null else {
      runningTasks[idx] = task
      idx
    }
  }

  private fun executeAt(threadIndex: Int, task: T, block: (ProgressIndicator<T>) -> Unit) {
    val progressIndicator = progressIndicators[threadIndex]
    progressIndicator.markStarted(task)
    executors[threadIndex].execute {
      block(progressIndicator)
      synchronized(this) {
        runningTasks[threadIndex] = null
        progressIndicator.markFinished()
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
