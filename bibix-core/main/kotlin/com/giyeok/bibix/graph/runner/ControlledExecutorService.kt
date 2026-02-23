package com.giyeok.bibix.graph.runner

import com.github.ajalt.mordant.animation.animation
import com.github.ajalt.mordant.rendering.TextColors.cyan
import com.github.ajalt.mordant.rendering.TextStyles.bold
import com.github.ajalt.mordant.rendering.TextStyles.dim
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.widgets.Text
import java.util.concurrent.Executors

class ExecutorTracker(threadCount: Int) {
  private val threadIdLocal = ThreadLocal<Int>()
  private val threads = mutableListOf<Thread>()
  private val threadTasks = mutableListOf<BuildTask?>()
  private val taskThreadId = mutableMapOf<BuildTask, Int>()

  private val spinnerFrames = listOf("⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏")
  private var spinnerIdx = 0
  private var lastPrintedMs = 0L

  private val terminal = Terminal()
  private val animation = terminal.animation<List<BuildTask?>> { tasks ->
    val activeTasks = tasks.filterNotNull()
    val spinner = cyan(spinnerFrames[spinnerIdx])
    Text(buildString {
      if (activeTasks.isEmpty()) {
        append("$spinner ${dim("대기 중...")}")
      } else {
        activeTasks.forEachIndexed { i, task ->
          if (i > 0) appendLine()
          append("$spinner ${bold(task.toString().take(100))}")
        }
      }
    })
  }

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
    val now = System.currentTimeMillis()
    if (now - lastPrintedMs < 100) return
    lastPrintedMs = now
    spinnerIdx = (spinnerIdx + 1) % spinnerFrames.size
    animation.update(threadTasks.toList())
  }
}
