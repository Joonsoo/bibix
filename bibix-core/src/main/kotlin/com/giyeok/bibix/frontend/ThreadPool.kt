package com.giyeok.bibix.frontend

import com.giyeok.bibix.interpreter.coroutine.ProgressIndicator
import com.giyeok.bibix.interpreter.coroutine.ProgressIndicatorContainer
import com.giyeok.bibix.interpreter.coroutine.TaskElement
import com.giyeok.bibix.interpreter.task.Task
import com.google.common.annotations.VisibleForTesting
import kotlinx.coroutines.*
import java.io.Closeable
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

// listener is for testing
class ThreadPool(
  val numThreads: Int,
  private val progressNotifier: ProgressNotifier,
  private val listener: (ThreadPoolEvent) -> Unit = {}
) : CoroutineDispatcher(), ProgressIndicatorContainer, Closeable {

  private val localProgressIndicator = ProgressIndicator(this, -1)
  private val progressIndicators = List(numThreads) { idx -> ProgressIndicator(this, idx) }
  private val progressIndicatorThreadLocal = ThreadLocal<ProgressIndicator>()

  private val runningTasks: MutableList<Task?> = MutableList(numThreads) { null }
  private val freeThreadIds = LinkedBlockingQueue((0 until numThreads).toList())

  private val progressLoggerExecutor = Executors.newSingleThreadExecutor()
  private val executors = List(numThreads) { idx ->
    val executor = Executors.newSingleThreadExecutor()
    executor.execute {
      progressIndicatorThreadLocal.set(progressIndicators[idx])
    }
    executor
  }

  data class TaskBlock(val job: Job, val task: Task, val block: Runnable)

  @VisibleForTesting
  val queue = LinkedBlockingQueue<TaskBlock>()

  fun setLocalProgressIndicator() {
    val existing = progressIndicatorThreadLocal.get()
    check(existing == null || existing === localProgressIndicator)
    progressIndicatorThreadLocal.set(localProgressIndicator)
  }

  override fun dispatch(context: CoroutineContext, block: Runnable) {
    val job = context[Job]!!
    val task = context[TaskElement.Key]!!.task
    val taskBlock = TaskBlock(job, task, block)
    queue.add(taskBlock)
    listener(ThreadPoolEvent.Enqueued(taskBlock))
  }

  private fun findAndAssignThread(): Int? {
    val idx = freeThreadIds.poll(1, TimeUnit.SECONDS)
    return if (idx == null) null else {
      check(idx in 0 until numThreads)
      check(runningTasks[idx] == null)
      idx
    }
  }

  private fun executeBlockAtThread(block: TaskBlock, threadIdx: Int) {
    listener(ThreadPoolEvent.BlockAssigned(block, threadIdx))
    synchronized(this) {
      runningTasks[threadIdx] = block.task
      progressIndicators[threadIdx].setTask(block.task)
    }

    executors[threadIdx].execute {
      listener(ThreadPoolEvent.BlockStarted(block, threadIdx))
      try {
        block.block.run()
      } finally {
        listener(ThreadPoolEvent.BlockFinished(block, threadIdx))
        synchronized(this) {
          progressIndicators[threadIdx].setFinished()
          runningTasks[threadIdx] = null
        }
        freeThreadIds.add(threadIdx)
      }
    }
    listener(ThreadPoolEvent.BlockSentToExecutor(block, threadIdx))
  }

  fun processTasks(rootJob: Job) {
    while (!rootJob.isCompleted || queue.isNotEmpty()) {
      val nextBlock = queue.poll(10, TimeUnit.SECONDS)
      if (nextBlock != null) {
        var assignedThreadIdx = findAndAssignThread()
        while (assignedThreadIdx == null) {
          assignedThreadIdx = findAndAssignThread()
        }
        executeBlockAtThread(nextBlock, assignedThreadIdx)
      }
      notifyProgresses()
    }
  }

  override fun notifyUpdated(progressIndicator: ProgressIndicator) {
    notifyProgresses()
  }

  override fun ofCurrentThread(): ProgressIndicator = progressIndicatorThreadLocal.get()

  private fun notifyProgresses() {
    progressLoggerExecutor.execute {
      progressNotifier.notifyProgresses {
        progressIndicators.map { it.toThreadState() }
      }
    }
  }

  override fun close() {
    executors.forEach { it.shutdown() }
  }
}

sealed class ThreadPoolEvent {
  data class Enqueued(val taskBlock: ThreadPool.TaskBlock) : ThreadPoolEvent()

  data class BlockAssigned(val taskBlock: ThreadPool.TaskBlock, val threadIdx: Int) :
    ThreadPoolEvent()

  data class BlockSentToExecutor(val taskBlock: ThreadPool.TaskBlock, val threadIdx: Int) :
    ThreadPoolEvent()

  data class BlockStarted(val taskBlock: ThreadPool.TaskBlock, val threadIdx: Int) :
    ThreadPoolEvent()

  data class BlockFinished(val taskBlock: ThreadPool.TaskBlock, val threadIdx: Int) :
    ThreadPoolEvent()
}
