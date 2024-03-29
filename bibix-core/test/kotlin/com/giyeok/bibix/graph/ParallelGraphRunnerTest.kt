package com.giyeok.bibix.graph

import com.giyeok.bibix.base.Architecture
import com.giyeok.bibix.base.BuildEnv
import com.giyeok.bibix.base.OS
import com.giyeok.bibix.frontend.BuildFrontend
import com.giyeok.bibix.graph.runner.BuildGraphRunner
import com.giyeok.bibix.graph.runner.ClassPkgRunner
import com.giyeok.bibix.graph.runner.EvalTarget
import com.giyeok.bibix.graph.runner.ExecutorTracker
import com.giyeok.bibix.graph.runner.ParallelGraphRunner
import com.giyeok.bibix.plugins.prelude.preludePlugin
import com.giyeok.bibix.repo.BibixRepo
import kotlinx.coroutines.runBlocking
import org.codehaus.plexus.classworlds.ClassWorld
import org.junit.jupiter.api.Test
import java.nio.file.FileSystems
import java.nio.file.Path
import kotlin.io.path.absolute

class ParallelGraphRunnerTest {
  @Test
  fun test() {
    val mainProjectLocation =
      BibixProjectLocation(Path.of("bibix-core/src/test/resources/cycle").absolute())
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
    )
    val tracker = ExecutorTracker(4)

    val prunner = ParallelGraphRunner(runner, tracker.executor, tracker)
    val results = runBlocking {
      prunner.runTasksOrFailure(
        // EvalTarget(1, 0, BibixName("dd"))
        listOf(EvalTarget(1, 0, BibixName("a")))
//        ExecAction(1, 0, BibixName("runLocalAction"), mapOf())
      )
    }
    repo.shutdown()
    println(results)
  }
}
