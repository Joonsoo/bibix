package com.giyeok.bibix.graph.runner

import java.util.concurrent.Executors

class ExecutorTracker(threadCount: Int) {
  private val threadIdLocal = ThreadLocal<Int>()
  private val threads = mutableListOf<Thread>()
  private val threadTasks = mutableListOf<BuildTask?>()
  private val taskThreadId = mutableMapOf<BuildTask, Int>()

  // ANSI escape codes
  private val ESC = "\u001B"
  private val CURSOR_UP = "$ESC[1A"
  private val ERASE_LINE = "$ESC[2K"
  private val RESET = "$ESC[0m"
  private val BOLD = "$ESC[1m"
  private val CYAN = "$ESC[36m"
  private val DIM = "$ESC[2m"

  private val SPINNER_FRAMES = listOf("⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏")
  private var spinnerIdx = 0
  private var occupiedLines = 0
  private var lastPrintedMs = 0L

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

    // 이전 출력 줄 지우기
    if (occupiedLines > 0) {
      repeat(occupiedLines) {
        print(CURSOR_UP + ERASE_LINE + "\r")
      }
    }

    val activeTasks = threadTasks.filterNotNull()
    spinnerIdx = (spinnerIdx + 1) % SPINNER_FRAMES.size
    val spinner = "$CYAN${SPINNER_FRAMES[spinnerIdx]}$RESET"

    if (activeTasks.isEmpty()) {
      println("$spinner $DIM대기 중...$RESET")
      occupiedLines = 1
    } else {
      val lines = activeTasks.map { task ->
        val label = task.toString().take(100)
        "$spinner $BOLD$label$RESET"
      }
      lines.forEach { println(it) }
      occupiedLines = lines.size
    }
  }
}
