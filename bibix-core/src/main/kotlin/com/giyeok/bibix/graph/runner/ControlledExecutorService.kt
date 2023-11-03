package com.giyeok.bibix.graph.runner

import java.util.concurrent.Executors

class ExecutorTracker(threadCount: Int) {
  private val threadIdLocal = ThreadLocal<Int>()
  private val threads = mutableListOf<Thread>()
  private val threadTasks = mutableListOf<BuildTask?>()
  private val taskThreadId = mutableMapOf<BuildTask, Int>()

  val executor = Executors.newFixedThreadPool(threadCount) { runnable ->
    val thread = synchronized(this) {
      val threadId = threads.size
      val thread = Thread {
        threadIdLocal.set(threadId)
        runnable.run()
      }
      threads.add(thread)
      threadTasks.add(null)
      thread
    }
    thread
  }

  fun notifyJobStartedFor(task: BuildTask) {
    val threadId = threadIdLocal.get()
    synchronized(this) {
      threadTasks[threadId] = task
      taskThreadId[task] = threadId
    }
    printStatus()
  }

  fun notifyJobFinished(task: BuildTask) {
    val threadId = threadIdLocal.get()
    synchronized(this) {
      val threadIdByThread = taskThreadId.remove(task)
      if (threadIdByThread == threadId) {
        threadTasks[threadId] = null
      } else {
        // 이런 상황이 일반적으론 생기면 안 되는데 미묘한 타이밍 문제로 생길 수도 있을 듯..
      }
    }
    printStatus()
  }

  fun notifySuspendJobStartedFor(task: BuildTask) {
    val threadId = threadIdLocal.get()
    synchronized(this) {
      threadTasks[threadId] = task
      taskThreadId[task] = threadId
    }
    printStatus()
  }

  fun notifySuspendJobFinished(task: BuildTask) {
    synchronized(this) {
      val startedThreadId = checkNotNull(taskThreadId.remove(task))
      if (threadTasks[startedThreadId] != task) {
        threadTasks[startedThreadId] = null
      }
    }
    printStatus()
  }

  fun printStatus() = synchronized(this) {
    threadTasks.forEachIndexed { index, buildTask ->
      println("$index: ${buildTask.toString().take(100)}")
    }
    println()
  }
}
