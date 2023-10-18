package com.giyeok.bibix.graph

import com.giyeok.bibix.frontend.BuildFrontend
import com.giyeok.bibix.graph.runner.BibixProjectLocation
import com.giyeok.bibix.graph.runner.MainProjectId
import com.giyeok.bibix.graph.runner.GlobalTaskId
import com.giyeok.bibix.graph.runner.GlobalTaskRunner
import com.giyeok.bibix.plugins.prelude.preludePlugin
import kotlinx.coroutines.*
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

      println(runner)

      val targetTask = runner.getMainProjectTaskId("xyz")

      val job = CoroutineScope(Executors.newFixedThreadPool(4).asCoroutineDispatcher()).async {
        val depsGraph = runner.globalGraph.depsGraphFrom(setOf(targetTask))
        for (nextNode in depsGraph.readyIds) {
          runner.memoEvaluateNode(nextNode)
          depsGraph.finishNode(nextNode)
          if (depsGraph.isDone()) {
            break
          }
        }
        println(depsGraph)
      }
      job.await()
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
