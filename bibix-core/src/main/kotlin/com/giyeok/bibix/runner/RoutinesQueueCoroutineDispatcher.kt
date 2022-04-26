package com.giyeok.bibix.runner

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Runnable
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext

class RoutinesQueueCoroutineDispatcher : CoroutineDispatcher() {
  sealed class NextRoutine {
    data class BuildTaskRoutine(
      val routineId: BuildTaskRoutineId,
      val block: Runnable
    ) : NextRoutine()

    data class BuildFinished(val buildRequestName: String) : NextRoutine()
  }

  private val routineIdCounter = AtomicInteger()

  val routinesQueue = LinkedBlockingQueue<NextRoutine>()

  override fun dispatch(context: CoroutineContext, block: Runnable) {
    val buildTask = context[BuildTaskElement.Key]!!.buildTask
    routinesQueue.add(
      NextRoutine.BuildTaskRoutine(
        BuildTaskRoutineId(buildTask, routineIdCounter.incrementAndGet()), block
      )
    )
  }

  fun buildFinished(buildRequestName: String) {
    routinesQueue.add(NextRoutine.BuildFinished(buildRequestName))
  }
}
