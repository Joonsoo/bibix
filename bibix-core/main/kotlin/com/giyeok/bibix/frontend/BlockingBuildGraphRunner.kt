package com.giyeok.bibix.frontend

import com.giyeok.bibix.graph.runner.BuildGraphRunner
import com.giyeok.bibix.graph.runner.BuildTask
import com.giyeok.bibix.graph.runner.BuildTaskResult
import kotlinx.coroutines.*
import java.util.concurrent.Executors

class BlockingBuildGraphRunner(val runner: BuildGraphRunner) {
  private val suspendDispatcher = Executors.newFixedThreadPool(2).asCoroutineDispatcher()

  fun runToFinal(buildTask: BuildTask): BuildTaskResult.FinalResult {
    println("run: $buildTask")
    val result = runner.runBuildTask(buildTask)
    return handleResult(buildTask, result)
  }

  fun handleResult(
    buildTask: BuildTask,
    result: BuildTaskResult
  ): BuildTaskResult.FinalResult =
    when (result) {
      is BuildTaskResult.FinalResult -> result
      is BuildTaskResult.WithResult -> {
        println("$buildTask")
        println("  ${result.task}")
        val derivedTaskResult = runToFinal(result.task)
        handleResult(buildTask, result.func(derivedTaskResult))
      }

      is BuildTaskResult.WithResultList -> {
        println("$buildTask")
        result.tasks.forEach {
          println("  $it")
        }
        val derivedTaskResults = result.tasks.map { runToFinal(it) }
        val funcResult = result.func(derivedTaskResults)
        handleResult(buildTask, funcResult)
      }

      is BuildTaskResult.LongRunning -> {
        println("Long running (suspend)...")
        val funcResult = runBlocking(suspendDispatcher) {
          result.preCondition()
          val bodyResult = try {
            result.body()
          } catch (e: Throwable) {
            e.printStackTrace()
            throw e
          } finally {
            result.postCondition()
          }
          result.after(bodyResult)
        }
        handleResult(buildTask, funcResult)
      }

      else -> throw AssertionError()
    }
}
