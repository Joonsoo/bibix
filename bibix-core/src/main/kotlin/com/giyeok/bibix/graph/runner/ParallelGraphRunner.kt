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
  private val runnerMutex = Mutex()

  private val targetValuesMutex = Mutex()
  private val targetValues =
    mutableMapOf<String, MutableStateFlow<BuildTaskResult.ValueOfTargetResult?>>()

  private suspend inline fun <T> safeRunnerRun(block: () -> T): T =
    runnerMutex.withLock {
      block()
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

  suspend fun runTasks(tasks: List<BuildTask>): Map<BuildTask, BuildTaskResult.FinalResult> {
    val results = tasks.map { runTaskToFinalOrFailure(it) }.awaitAll()
    return tasks.zip(results).toMap()
      .filter { (_, result) -> result is FailureOr.Result }
      .mapValues { (_, result) -> (result as FailureOr.Result).result }
  }

  private suspend fun handleResultOrFailure(
    task: BuildTask,
    resultOrFailure: FailureOr<BuildTaskResult>
  ): Deferred<FailureOr<BuildTaskResult.FinalResult>> = when (resultOrFailure) {
    is FailureOr.Failure ->
      CompletableDeferred(FailureOr.Failure(resultOrFailure.error))

    is FailureOr.Result -> when (val result = resultOrFailure.result) {
      is BuildTaskResult.FinalResult -> {
        if (result is BuildTaskResult.ValueOfTargetResult) {
          targetValuesMutex.withLock {
            val existing = targetValues[result.targetId]
            if (existing != null) {
              existing.value = result
            } else {
              targetValues[result.targetId] = MutableStateFlow(result)
            }
          }
        }
        CompletableDeferred(FailureOr.Result(result))
      }

      is BuildTaskResult.WithResult ->
        async {
          when (val subResult = runTaskToFinalOrFailure(result.task).await()) {
            is FailureOr.Failure<*> ->
              FailureOr.Failure(subResult.error)

            is FailureOr.Result -> {
              val next = try {
                FailureOr.Result(safeRunnerRun {
                  result.func(subResult.result)
                })
              } catch (e: Throwable) {
                FailureOr.Failure(e)
              }
              handleResultOrFailure(task, next).await()
            }
          }
        }

      is BuildTaskResult.WithResultList -> {
        async {
          val subResults = result.tasks.map { runTaskToFinalOrFailure(it) }.awaitAll()
          if (subResults.any { it is FailureOr.Failure }) {
            val errors = subResults.filterIsInstance<FailureOr.Failure<*>>().map { it.error }
            FailureOr.Failure(IllegalStateException(errors.first()))
          } else {
            val next = try {
              val subResultsList = subResults.map { (it as FailureOr.Result).result }
              FailureOr.Result(safeRunnerRun {
                result.func(subResultsList)
              })
            } catch (e: Throwable) {
              FailureOr.Failure(e)
            }
            handleResultOrFailure(task, next).await()
          }
        }
      }

      is BuildTaskResult.LongRunning ->
        async {
          val stateFlow = MutableStateFlow<FailureOr<BuildTaskResult.FinalResult>?>(null)
          executor.execute {
            jobExecutorTracker?.notifyJobStartedFor(task)
            launch {
              try {
                val bodyResult = try {
                  result.preCondition()
                  FailureOr.Result(result.body())
                } catch (e: Throwable) {
                  FailureOr.Failure(e)
                } finally {
                  try {
                    result.postCondition()
                  } catch (e: Throwable) {
                    // TODO 이건 어떻게 처리하지?
                    e.printStackTrace()
                  }
                }
                val longRunningResult = when (bodyResult) {
                  is FailureOr.Failure -> FailureOr.Failure(bodyResult.error)
                  is FailureOr.Result -> safeRunnerRun {
                    try {
                      FailureOr.Result(result.after(bodyResult.result))
                    } catch (e: Throwable) {
                      FailureOr.Failure(e)
                    }
                  }
                }
                stateFlow.value = handleResultOrFailure(task, longRunningResult).await()
              } finally {
                jobExecutorTracker?.notifyJobFinished(task)
              }
            }
          }
          stateFlow.filterNotNull().first()
        }

      is BuildTaskResult.DuplicateTargetResult -> {
        // TODO target 빌드가 실패하는 경우 처리가 안됨..
        val stateFlow = targetValuesMutex.withLock {
          targetValues.getOrPut(result.targetId) { MutableStateFlow(null) }
        }
        async { FailureOr.Result(stateFlow.filterNotNull().first()) }
      }
    }
  }

  private suspend fun runTaskToFinalOrFailure(task: BuildTask): Deferred<FailureOr<BuildTaskResult.FinalResult>> {
    val result = try {
      FailureOr.Result(safeRunnerRun { runner.runBuildTask(task) })
    } catch (e: Throwable) {
      FailureOr.Failure(e)
    }
    return handleResultOrFailure(task, result)
  }

  suspend fun runTasksOrFailure(tasks: List<BuildTask>): Map<BuildTask, FailureOr<BuildTaskResult.FinalResult>> {
    val results = tasks.map { runTaskToFinalOrFailure(it) }.awaitAll()
    return tasks.zip(results).toMap()
  }
}

sealed class FailureOr<T> {
  data class Result<T>(val result: T): FailureOr<T>()
  data class Failure<T>(val error: Throwable): FailureOr<T>() {
    init {
      error.printStackTrace()
    }
  }
}
