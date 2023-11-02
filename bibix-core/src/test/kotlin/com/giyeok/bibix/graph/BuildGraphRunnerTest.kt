package com.giyeok.bibix.graph

import com.giyeok.bibix.base.Architecture
import com.giyeok.bibix.base.BibixValue
import com.giyeok.bibix.base.BuildEnv
import com.giyeok.bibix.base.OS
import com.giyeok.bibix.frontend.BuildFrontend
import com.giyeok.bibix.graph.runner.*
import com.giyeok.bibix.plugins.prelude.preludePlugin
import com.giyeok.bibix.repo.BibixRepo
import kotlinx.coroutines.runBlocking
import org.codehaus.plexus.classworlds.ClassWorld
import org.junit.jupiter.api.Test
import java.nio.file.FileSystems
import java.nio.file.Path
import kotlin.io.path.absolute

class BuildGraphRunnerTest {
  @Test
  fun test() {
    val mainProjectLocation =
      BibixProjectLocation(Path.of("bibix-core/src/test/resources/varredefs/b").absolute())
    val buildEnv = BuildEnv(OS.Linux("", ""), Architecture.X86_64)
    val repo = BibixRepo.load(mainProjectLocation.projectRoot)
    val runner = runBlocking {
      BuildGraphRunner.create(
        mainProjectLocation = mainProjectLocation,
        preludePlugin = preludePlugin,
        preloadedPlugins = BuildFrontend.defaultPreloadedPlugins,
        buildEnv = buildEnv,
        fileSystem = FileSystems.getDefault(),
        repo = repo,
        classWorld = ClassWorld()
      )
    }

    runner.runAction(1, 0, BibixName("myaction"))

    println(runner.runToFinal(EvalTarget(1, 0, BibixName("x"))))
    println(runner.runToFinal(EvalTarget(1, 0, BibixName("x2"))))

    println(runner.runToFinal(EvalTarget(1, 0, BibixName("aa"))))
    println(runner.runToFinal(EvalTarget(1, 0, BibixName("bb"))))
    println(runner.runToFinal(EvalTarget(1, 0, BibixName("cc"))))
    println(runner.runToFinal(EvalTarget(1, 0, BibixName("ss"))))
    println(runner.runToFinal(EvalTarget(1, 0, BibixName("dd"))))
    println(runner.runToFinal(EvalTarget(1, 0, BibixName("ee"))))
  }

  fun BuildGraphRunner.runAction(projectId: Int, importInstanceId: Int, name: BibixName) {
    val buildGraph = multiGraph.getProjectGraph(projectId)
    val action = buildGraph.actions.getValue(name)

    // TODO action.argsName
    val letLocals = mutableMapOf<String, BibixValue>()
    action.stmts.forEach { stmt ->
      when (stmt) {
        is ActionDef.LetStmt -> {
          val result =
            runToFinal(EvalExpr(projectId, stmt.exprNodeId, importInstanceId, letLocals, null))
          check(result is BuildTaskResult.ValueResult)
          letLocals[stmt.name] = result.value
        }

        is ActionDef.CallStmt ->
          runToFinal(ExecActionCallExpr(projectId, importInstanceId, stmt, letLocals))
      }
    }
  }

  fun BuildGraphRunner.runToFinal(buildTask: BuildTask): BuildTaskResult.FinalResult =
    handleResult(buildTask, this.runBuildTask(buildTask))

  fun BuildGraphRunner.handleResult(
    buildTask: BuildTask,
    result: BuildTaskResult
  ): BuildTaskResult.FinalResult =
    when (result) {
      is BuildTaskResult.FinalResult -> result
      is BuildTaskResult.WithResult -> {
//        println("$buildTask -> ${result.task}")
        val derivedTaskResult = runToFinal(result.task)
        handleResult(buildTask, result.func(derivedTaskResult))
      }

      is BuildTaskResult.WithResultList -> {
//        println("$buildTask ->")
//        result.tasks.forEach {
//          println("  $it")
//        }
        val derivedTaskResults = result.tasks.map { runToFinal(it) }
        handleResult(buildTask, result.func(derivedTaskResults))
      }

      is BuildTaskResult.LongRunning -> {
        handleResult(buildTask, result.func())
      }

      is BuildTaskResult.SuspendLongRunning -> {
        handleResult(buildTask, runBlocking { result.func() })
      }

      else -> throw AssertionError()
    }
}
