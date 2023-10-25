package com.giyeok.bibix.graph

import com.giyeok.bibix.frontend.BuildFrontend
import com.giyeok.bibix.graph.runner.*
import com.giyeok.bibix.plugins.prelude.preludePlugin
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.util.concurrent.Executors
import kotlin.io.path.absolute

class GlobalTaskGraphTest {
  @Test
  fun test() {
    runBlocking {
      val runner = GlobalTaskRunner.create(
        BibixProjectLocation(Path.of("bibix-core/src/test/resources").absolute(), "test1.bbx"),
        preludePlugin,
        BuildFrontend.defaultPreloadedPlugins
      )

      println(dotGraphFrom(runner))
      println()

      val targetTask = runner.getMainProjectTaskId("abc")

      val dispatcher = Executors.newFixedThreadPool(4).asCoroutineDispatcher()

      val depsGraph = runner.globalGraph.depsGraphFrom(setOf(targetTask))
      val runnerMutex = Mutex()
      for (nextNode in depsGraph.nextNodeIds) {
        val taskRunResult = try {
          runnerMutex.withLock {
            runner.runTask(nextNode)
          }
        } catch (e: Exception) {
          e.printStackTrace()
          throw e
        }
        when (taskRunResult) {
          is GlobalTaskRunner.TaskRunResult.UnfulfilledPrerequisites -> {
            val prerequisiteEdges = taskRunResult.prerequisites.map { (end, type) ->
              GlobalTaskEdge(nextNode, end, type)
            }
            depsGraph.addEdges(prerequisiteEdges)
          }

          is GlobalTaskRunner.TaskRunResult.ImmediateResult -> {
            taskRunResult.result
            depsGraph.finishNode(nextNode)
          }

          is GlobalTaskRunner.TaskRunResult.LongRunningResult -> {
            CoroutineScope(dispatcher).launch {
              taskRunResult.runner()
              depsGraph.finishNode(nextNode)
            }
          }
        }
      }
      check(depsGraph.isDone())

//      val requiredImports = runner.findRequiredImportsFor(setOf(targetTask))
//      val job = CoroutineScope(Executors.newFixedThreadPool(4).asCoroutineDispatcher()).async {
//        val importTasks = requiredImports.keys.map { requiredImport ->
//          async { runner.resolveImport(requiredImport) }
//        }
//        importTasks.awaitAll()
//      }
//      job.await()
//      println(requiredImports)
    }
  }
}
