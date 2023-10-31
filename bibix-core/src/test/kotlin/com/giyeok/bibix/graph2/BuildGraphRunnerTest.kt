package com.giyeok.bibix.graph2

import com.giyeok.bibix.base.Architecture
import com.giyeok.bibix.base.BuildEnv
import com.giyeok.bibix.base.OS
import com.giyeok.bibix.frontend.BuildFrontend
import com.giyeok.bibix.graph2.runner.BuildGraphRunner
import com.giyeok.bibix.graph2.runner.BuildTask
import com.giyeok.bibix.graph2.runner.BuildTaskResult
import com.giyeok.bibix.graph2.runner.EvalTarget
import com.giyeok.bibix.plugins.prelude.preludePlugin
import com.giyeok.bibix.repo.BibixRepo
import kotlinx.coroutines.runBlocking
import org.codehaus.plexus.classworlds.ClassWorld
import org.junit.jupiter.api.Test
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
        mainProjectLocation,
        preludePlugin,
        BuildFrontend.defaultPreloadedPlugins,
        buildEnv,
        repo,
        ClassWorld()
      )
    }

    println(runner.runToFinal(EvalTarget(1, 0, BibixName("x"))))
    println(runner.runToFinal(EvalTarget(1, 0, BibixName("x2"))))
    println(runner.runToFinal(EvalTarget(1, 0, BibixName("y"))))
    println(runner.runToFinal(EvalTarget(1, 0, BibixName("y2"))))
    println(runner.runToFinal(EvalTarget(1, 0, BibixName("q"))))
    println(runner.runToFinal(EvalTarget(1, 0, BibixName("q"))))
    println(runner.runToFinal(EvalTarget(1, 0, BibixName("v"))))
  }

  fun BuildGraphRunner.runToFinal(buildTask: BuildTask): BuildTaskResult.FinalResult =
    handleResult(this.runBuildTask(buildTask))

  fun BuildGraphRunner.handleResult(result: BuildTaskResult): BuildTaskResult.FinalResult =
    when (result) {
      is BuildTaskResult.FinalResult -> result
      is BuildTaskResult.WithResult -> {
        val derivedTask = runToFinal(result.task)
        handleResult(result.func(derivedTask))
      }

      is BuildTaskResult.WithResultList -> {
        val derivedTasks = result.tasks.map { runToFinal(it) }
        handleResult(result.func(derivedTasks))
      }

      is BuildTaskResult.LongRunning -> {
        handleResult(result.func())
      }

      is BuildTaskResult.SuspendLongRunning -> {
        handleResult(runBlocking { result.func() })
      }

      else -> throw AssertionError()
    }
}
