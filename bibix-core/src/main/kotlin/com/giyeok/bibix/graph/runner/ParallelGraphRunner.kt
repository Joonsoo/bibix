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

  private suspend fun safeRunTaskOrFailure(task: BuildTask): NonFinalResultOrFailure =
    mutex.withLock {
      try {
        NonFinalResultOrFailure.Result(runner.runBuildTask(task))
      } catch (e: Throwable) {
        NonFinalResultOrFailure.Failure(e)
      }
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

  suspend fun runTasks(tasks: List<BuildTask>): Map<BuildTask, BuildTaskResult.FinalResult> {
    val results = tasks.map { runTaskToFinal(it) }.awaitAll()
    return tasks.zip(results).toMap()
  }

  private suspend fun runTaskToFinal(task: BuildTask): Deferred<BuildTaskResult.FinalResult> =
    handleResult(task, safeRunTask(task))

  private suspend fun handleResult(
    task: BuildTask,
    result: BuildTaskResult
  ): Deferred<BuildTaskResult.FinalResult> = when (result) {
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

    is BuildTaskResult.WithResult ->
      async {
        val subResult = runTaskToFinal(result.task).await()
        handleResult(task, result.func(subResult)).await()
      }

    is BuildTaskResult.WithResultList ->
      async {
        val subResults = result.tasks.map { runTaskToFinal(it) }.awaitAll()
        handleResult(task, result.func(subResults)).await()
      }

    is BuildTaskResult.LongRunning ->
      async {
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

    is BuildTaskResult.SuspendLongRunning ->
      async {
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

  private suspend fun handleResultOrFailure(
    task: BuildTask,
    resultOrFailure: NonFinalResultOrFailure
  ): Deferred<ResultOrFailure> = when (resultOrFailure) {
    is NonFinalResultOrFailure.Failure ->
      CompletableDeferred(ResultOrFailure.Failure(resultOrFailure.error))

    is NonFinalResultOrFailure.Result -> when (val result = resultOrFailure.result) {
      is BuildTaskResult.FinalResult -> {
        // TODO targetValues
        CompletableDeferred(ResultOrFailure.Result(result))
      }

      is BuildTaskResult.WithResult ->
        async {
          when (val subResult = runTaskToFinalOrFailure(result.task).await()) {
            is ResultOrFailure.Failure ->
              ResultOrFailure.Failure(subResult.error)

            is ResultOrFailure.Result -> {
              val next = try {
                NonFinalResultOrFailure.Result(result.func(subResult.result))
              } catch (e: Throwable) {
                NonFinalResultOrFailure.Failure(e)
              }
              handleResultOrFailure(task, next).await()
            }
          }
        }

      is BuildTaskResult.WithResultList -> {
        async {
          val subResults = result.tasks.map { runTaskToFinalOrFailure(it) }.awaitAll()
          if (subResults.any { it is ResultOrFailure.Failure }) {
            val errors = subResults.filterIsInstance<ResultOrFailure.Failure>().map { it.error }
            ResultOrFailure.Failure(IllegalStateException(errors.first()))
          } else {
            val next = try {
              NonFinalResultOrFailure.Result(result.func(subResults.map { (it as ResultOrFailure.Result).result }))
            } catch (e: Throwable) {
              NonFinalResultOrFailure.Failure(e)
            }
            handleResultOrFailure(task, next).await()
          }
        }
      }

      is BuildTaskResult.LongRunning -> TODO()
      is BuildTaskResult.SuspendLongRunning -> TODO()
      is BuildTaskResult.DuplicateTargetResult -> TODO()
    }
  }

  private suspend fun runTaskToFinalOrFailure(task: BuildTask): Deferred<ResultOrFailure> =
    handleResultOrFailure(task, safeRunTaskOrFailure(task))

  suspend fun runTasksOrFailure(tasks: List<BuildTask>): Map<BuildTask, ResultOrFailure> {
    val results = tasks.map { runTaskToFinalOrFailure(it) }.awaitAll()
    return tasks.zip(results).toMap()
  }
}

sealed class ResultOrFailure {
  data class Result(val result: BuildTaskResult.FinalResult): ResultOrFailure()
  data class Failure(val error: Throwable): ResultOrFailure()
}

sealed class NonFinalResultOrFailure {
  data class Result(val result: BuildTaskResult): NonFinalResultOrFailure()
  data class Failure(val error: Throwable): NonFinalResultOrFailure()
}
