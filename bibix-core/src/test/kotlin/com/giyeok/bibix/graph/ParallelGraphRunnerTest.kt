package com.giyeok.bibix.graph

import com.giyeok.bibix.base.Architecture
import com.giyeok.bibix.base.BuildEnv
import com.giyeok.bibix.base.OS
import com.giyeok.bibix.frontend.BuildFrontend
import com.giyeok.bibix.graph.runner.BuildGraphRunner
import com.giyeok.bibix.graph.runner.EvalTarget
import com.giyeok.bibix.graph.runner.ExecAction
import com.giyeok.bibix.graph.runner.ParallelGraphRunner
import com.giyeok.bibix.plugins.prelude.preludePlugin
import com.giyeok.bibix.repo.BibixRepo
import org.codehaus.plexus.classworlds.ClassWorld
import org.junit.jupiter.api.Test
import java.nio.file.FileSystems
import java.nio.file.Path
import java.util.concurrent.Executors
import kotlin.io.path.absolute

class ParallelGraphRunnerTest {
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

    val prunner = ParallelGraphRunner(runner, Executors.newFixedThreadPool(4))
    val results = prunner.runTasks(
      ExecAction(1, 0, BibixName("myaction"), mapOf()),
      EvalTarget(1, 0, BibixName("aa")),
      EvalTarget(1, 0, BibixName("dd"))
    )
    println(results)
  }
}
