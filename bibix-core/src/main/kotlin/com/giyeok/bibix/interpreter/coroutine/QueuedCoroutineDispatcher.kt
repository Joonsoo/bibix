package com.giyeok.bibix.interpreter.coroutine

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Runnable
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

class QueuedCoroutineDispatcher(private val threadPool: ThreadPool) : CoroutineDispatcher() {
  data class TaskBlock(val block: Runnable)

  private val queue = LinkedBlockingQueue<TaskBlock>()

  override fun dispatch(context: CoroutineContext, block: Runnable) {
    val task = context[TaskElement.Key]!!.task
    queue.add(TaskBlock(block))
  }

  fun waitUntilQueueIsEmpty() {
    while (true) {
      val nextBlock = queue.poll(10, TimeUnit.SECONDS)
      if (nextBlock != null) {
        threadPool.execute(nextBlock.block)
      }
      threadPool.printProgresses()
    }
  }
}
