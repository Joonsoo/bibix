package com.giyeok.bibix.integtest

import com.giyeok.bibix.frontend.BuildFrontend
import com.giyeok.bibix.frontend.NoopProgressNotifier
import com.giyeok.bibix.graph.BibixProjectLocation
import com.giyeok.bibix.graph.runner.EvalTarget
import com.giyeok.bibix.interpreter.BibixProject
import com.giyeok.bibix.interpreter.expr.Definition
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import kotlin.io.path.Path

object BlockingBibixRunner {
  @Test
  fun test() {
    val frontend = BuildFrontend(
      mainProjectLocation = BibixProjectLocation(Path("../testproject")),
      buildArgsMap = mapOf(),
      actionArgs = listOf(),
      debuggingMode = true
    )

    val definitions = frontend.mainScriptDefinitions()
    assertThat(definitions.keys).containsExactly(
      "newbibix",
      "ktjvm",
      "scala",
      "java",
      "maven",
      "kotlinVersion",
      "test2",
      "test1",
      "mytest",
      "mytest.test1",
    )
    val buildTargets = definitions.filterValues { it is EvalTarget }
    assertThat(buildTargets.keys).containsExactly(
      "kotlinVersion",
      "test1",
      "test2",
      "mytest.test1"
    )

//    val results = frontend.blockingBuildTargets(listOf("test1"))
//    println(results)
  }
}
