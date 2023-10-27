package com.giyeok.bibix.graph

import com.giyeok.bibix.base.Architecture
import com.giyeok.bibix.base.BuildEnv
import com.giyeok.bibix.base.OS
import com.giyeok.bibix.frontend.BuildFrontend
import com.giyeok.bibix.graph.runner.BibixProjectLocation
import com.giyeok.bibix.graph.runner.GlobalTaskEdge
import com.giyeok.bibix.graph.runner.GlobalTaskRunner
import com.giyeok.bibix.graph.runner.toNodeId
import com.giyeok.bibix.plugins.prelude.preludePlugin
import com.giyeok.bibix.repo.BibixRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
      val mainProjectLocation =
        BibixProjectLocation(Path.of("bibix-core/src/test/resources").absolute(), "test1.bbx")
      val buildEnv = BuildEnv(OS.Linux("", ""), Architecture.X86_64)
      val repo = BibixRepo.load(mainProjectLocation.projectRoot)
      val runner = GlobalTaskRunner.create(
        mainProjectLocation,
        preludePlugin,
        BuildFrontend.defaultPreloadedPlugins,
        buildEnv,
        repo
      )

      val targetTask = runner.getMainProjectTaskId("abc")

      val dispatcher = Executors.newFixedThreadPool(4).asCoroutineDispatcher()

      val depsGraph = runner.globalGraph.depsGraphFrom(setOf(targetTask))
      println(dotGraphFrom(depsGraph))
      println()

      val runnerMutex = Mutex()
      for (nextNode in depsGraph.nextNodeIds) {
        println("runTask: ${nextNode.toNodeId()}")
        val taskRunResult = try {
          runnerMutex.withLock {
            runner.runTask(nextNode)
          }
        } catch (e: Exception) {
          e.printStackTrace()
          throw e
        }

        suspend fun handleResult(taskRunResult: GlobalTaskRunner.TaskRunResult) {
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
//            CoroutineScope(dispatcher).launch {
              val longRunningResult = try {
                taskRunResult.runner()
              } catch (e: Exception) {
                e.printStackTrace()
                // 실행을 종료해야 하는데..?
                throw e
              }
              handleResult(longRunningResult)
              // depsGraph.finishNode(nextNode)
//            }
            }
          }
        }

        handleResult(taskRunResult)

        depsGraph.printStatus()
        println(dotGraphFrom(depsGraph))
        println(depsGraph)
      }
      check(depsGraph.isDone())
    }
  }
}
