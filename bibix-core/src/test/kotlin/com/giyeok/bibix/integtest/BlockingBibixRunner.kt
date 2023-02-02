package com.giyeok.bibix.integtest

import com.giyeok.bibix.frontend.BuildFrontend
import com.giyeok.bibix.frontend.NoopProgressNotifier
import com.giyeok.bibix.interpreter.BibixProject
import org.junit.jupiter.api.Test
import kotlin.io.path.Path

object BlockingBibixRunner {
  @Test
  fun test() {
    val frontend = BuildFrontend(
      mainProject = BibixProject(Path("../testproject"), null),
      buildArgsMap = mapOf(),
      actionArgs = listOf(),
      progressNotifier = NoopProgressNotifier(),
      debuggingMode = true
    )

    val results = frontend.blockingBuildTargets(listOf("test1"))
    println(results)
  }
}
