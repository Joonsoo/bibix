package com.giyeok.bibix.graph

import com.giyeok.bibix.base.Architecture
import com.giyeok.bibix.base.BuildEnv
import com.giyeok.bibix.base.OS
import com.giyeok.bibix.frontend.BlockingBuildGraphRunner
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
    )

    try {
//    println(runner.runToFinal(EvalTarget(1, 0, BibixName("ee"))))

      BlockingBuildGraphRunner(runner).runToFinal(
        ExecAction(1, 0, BibixName("sugarformat.testProto.generate"), 0)
      )
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
    }
  }
}
