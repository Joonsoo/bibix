package com.giyeok.bibix.graph.runner

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ExecutorService

class ParallelGraphRunner(
  val runner: BuildGraphRunner,
  val executor: ExecutorService,
  val jobExecutorTracker: ExecutorTracker?,
) {
  private val mutex = Mutex()

  private suspend fun safeRunTask(task: BuildTask): BuildTaskResult = mutex.withLock {
    runner.runBuildTask(task)
  }

  private inline fun launch(crossinline block: suspend () -> Unit) {
    CoroutineScope(executor.asCoroutineDispatcher()).launch {
      block()
    }
  }

  private inline fun <T> async(crossinline block: suspend () -> T): Deferred<T> =
    CoroutineScope(executor.asCoroutineDispatcher()).async {
      block()
    }

  private val targetValues =
    mutableMapOf<String, MutableStateFlow<BuildTaskResult.ValueOfTargetResult?>>()

  private suspend fun handleResult(
    task: BuildTask,
    result: BuildTaskResult
  ): Deferred<BuildTaskResult.FinalResult> =
    when (result) {
      is BuildTaskResult.FinalResult -> {
        if (result is BuildTaskResult.ValueOfTargetResult) {
          mutex.withLock {
            val existing = targetValues[result.targetId]
            if (existing != null) {
              existing.value = result
            } else {
              targetValues[result.targetId] = MutableStateFlow(result)
            }
          }
        }
        CompletableDeferred(result)
      }

      is BuildTaskResult.WithResult -> {
        val subResult = runTaskToFinal(result.task).await()
        handleResult(task, result.func(subResult))
      }

      is BuildTaskResult.WithResultList -> {
        val subResults = result.tasks.map { runTaskToFinal(it) }.awaitAll()
        handleResult(task, result.func(subResults))
      }

      is BuildTaskResult.LongRunning -> async {
        val stateFlow = MutableStateFlow<BuildTaskResult.FinalResult?>(null)
        executor.execute {
          jobExecutorTracker?.notifyJobStartedFor(task)
          val funcResult = try {
            result.func()
          } finally {
            jobExecutorTracker?.notifyJobFinished(task)
          }
          launch {
            stateFlow.value = handleResult(task, funcResult).await()
          }
        }
        stateFlow.filterNotNull().first()
      }

      is BuildTaskResult.SuspendLongRunning -> async {
        val stateFlow = MutableStateFlow<BuildTaskResult.FinalResult?>(null)
        executor.execute {
          jobExecutorTracker?.notifyJobStartedFor(task)
          launch {
            val funcResult = try {
              result.func()
            } finally {
              jobExecutorTracker?.notifyJobFinished(task)
            }
            stateFlow.value = handleResult(task, funcResult).await()
          }
        }
        stateFlow.filterNotNull().first()
      }

      is BuildTaskResult.DuplicateTargetResult -> mutex.withLock {
        val stateFlow = targetValues.getOrPut(result.targetId) { MutableStateFlow(null) }
        async { stateFlow.filterNotNull().first() }
      }
    }

  private suspend fun runTaskToFinal(task: BuildTask): Deferred<BuildTaskResult.FinalResult> =
    handleResult(task, safeRunTask(task))

  suspend fun runTasks(tasks: List<BuildTask>): Map<BuildTask, BuildTaskResult.FinalResult?> {
    val results = tasks.map { runTaskToFinal(it) }.awaitAll()
    return tasks.zip(results).toMap()
  }
}
