package com.giyeok.bibix.graph

import com.giyeok.bibix.base.Architecture
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
import kotlin.io.path.absolutePathString

class BuildGraphRunnerTest {
  @Test
  fun test() {
    val mainProjectLocation =
      BibixProjectLocation(Path.of("/home/joonsoo/Documents/workspace/sugarproto").absolute())
    val buildEnv = BuildEnv(OS.Linux("", ""), Architecture.X86_64)
    val repo = BibixRepo.load(mainProjectLocation.projectRoot)
    val runner = BuildGraphRunner.create(
      mainProjectLocation = mainProjectLocation,
      preludePlugin = preludePlugin,
      preloadedPlugins = BuildFrontend.defaultPreloadedPlugins,
      buildEnv = buildEnv,
      fileSystem = FileSystems.getDefault(),
      repo = repo,
      classPkgRunner = ClassPkgRunner(ClassWorld()),
      pluginOverride = NoPluginOverrides
    )

    try {
//    println(runner.runToFinal(EvalTarget(1, 0, BibixName("ee"))))

      runner.runToFinal(ExecAction(1, 0, BibixName("sugarformat.testProto.generate"), mapOf()))
//
//    println(runner.runToFinal(EvalTarget(1, 0, BibixName("x"))))
//    println(runner.runToFinal(EvalTarget(1, 0, BibixName("x2"))))
//
//    println(runner.runToFinal(EvalTarget(1, 0, BibixName("aa"))))
//    println(runner.runToFinal(EvalTarget(1, 0, BibixName("bb"))))
//    println(runner.runToFinal(EvalTarget(1, 0, BibixName("cc"))))
//    println(runner.runToFinal(EvalTarget(1, 0, BibixName("ss"))))
//    println(runner.runToFinal(EvalTarget(1, 0, BibixName("dd"))))
//    println(runner.runToFinal(EvalTarget(1, 0, BibixName("ee"))))
    } finally {
      runner.multiGraph.projectLocations.forEach { (id, loc) ->
        println("$id: ${loc.projectRoot.absolutePathString()}")
      }

      taskRels.checkCycle()
    }
  }

  val taskRels = BuildTaskRelsGraph()


  fun BuildGraphRunner.runToFinal(buildTask: BuildTask): BuildTaskResult.FinalResult =
    handleResult(buildTask, this.runBuildTask(buildTask))

  fun BuildGraphRunner.handleResult(
    buildTask: BuildTask,
    result: BuildTaskResult
  ): BuildTaskResult.FinalResult =
    when (result) {
      is BuildTaskResult.FinalResult -> result
      is BuildTaskResult.WithResult -> {
        taskRels.addTaskRel(buildTask, result.task)
        println("$buildTask")
        println("  ${result.task}")
        val derivedTaskResult = runToFinal(result.task)
        handleResult(buildTask, result.func(derivedTaskResult))
      }

      is BuildTaskResult.WithResultList -> {
        taskRels.addTaskRel(buildTask, result.tasks)
        println("$buildTask")
        result.tasks.forEach {
          println("  $it")
        }
        val derivedTaskResults = result.tasks.map { runToFinal(it) }
        handleResult(buildTask, result.func(derivedTaskResults))
      }

      is BuildTaskResult.LongRunning -> {
        println("Long running...")
        handleResult(buildTask, result.func())
      }

      is BuildTaskResult.SuspendLongRunning -> {
        println("Long running (suspend)...")
        handleResult(buildTask, runBlocking { result.func() })
      }

      else -> throw AssertionError()
    }
}
