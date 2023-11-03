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

class BuildGraphRunnerTest {
  @Test
  fun test() {
    val mainProjectLocation =
      BibixProjectLocation(Path.of("bibix-core/src/test/resources/varredefs/b").absolute())
    val buildEnv = BuildEnv(OS.Linux("", ""), Architecture.X86_64)
    val repo = BibixRepo.load(mainProjectLocation.projectRoot)
    val runner = BuildGraphRunner.create(
      mainProjectLocation = mainProjectLocation,
      preludePlugin = preludePlugin,
      preloadedPlugins = BuildFrontend.defaultPreloadedPlugins,
      buildEnv = buildEnv,
      fileSystem = FileSystems.getDefault(),
      repo = repo,
      classWorld = ClassWorld()
    )

    println(runner.runToFinal(EvalTarget(1, 0, BibixName("protoGen"))))

    runner.runToFinal(ExecAction(1, 0, BibixName("myaction"), mapOf()))

    println(runner.runToFinal(EvalTarget(1, 0, BibixName("x"))))
    println(runner.runToFinal(EvalTarget(1, 0, BibixName("x2"))))

    println(runner.runToFinal(EvalTarget(1, 0, BibixName("aa"))))
    println(runner.runToFinal(EvalTarget(1, 0, BibixName("bb"))))
    println(runner.runToFinal(EvalTarget(1, 0, BibixName("cc"))))
    println(runner.runToFinal(EvalTarget(1, 0, BibixName("ss"))))
    println(runner.runToFinal(EvalTarget(1, 0, BibixName("dd"))))
    println(runner.runToFinal(EvalTarget(1, 0, BibixName("ee"))))
  }

  private val buildTasksGraph = mutableMapOf<BuildTask, MutableSet<BuildTask>>()

  fun checkCycle(task: BuildTask, path: List<BuildTask>) {
    if (task in path) {
      println("!! $path")
    }
    buildTasksGraph[task]?.forEach {
      checkCycle(it, path + task)
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
        buildTasksGraph.getOrPut(buildTask) { mutableSetOf() }.add(result.task)
        println("$buildTask -> ${result.task}")
        val derivedTaskResult = runToFinal(result.task)
        handleResult(buildTask, result.func(derivedTaskResult))
      }

      is BuildTaskResult.WithResultList -> {
        buildTasksGraph.getOrPut(buildTask) { mutableSetOf() }.addAll(result.tasks)
        println("$buildTask ->")
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

      else -> {
        val buildTaskNumbers = mutableMapOf<BuildTask, String>()
        println("digraph G {")
        buildTasksGraph.forEach { (start, ends) ->
          fun addNode(task: BuildTask) {
            if (task !in buildTaskNumbers) {
              val nodeId = "n${buildTaskNumbers.size + 1}"
              buildTaskNumbers[task] = nodeId
              println("  $nodeId [label=\"${escapeDotString("$task")}\"];")
            }
          }
          addNode(start)
          ends.forEach { addNode(it) }
        }
        buildTasksGraph.forEach { (start, ends) ->
          ends.forEach { end ->
            println("  ${buildTaskNumbers.getValue(start)} -> ${buildTaskNumbers.getValue(end)};")
          }
        }
        println("}")
        checkCycle(buildTask, listOf())
        throw AssertionError()
      }
    }
}
