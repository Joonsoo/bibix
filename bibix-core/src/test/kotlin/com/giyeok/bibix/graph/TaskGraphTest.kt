package com.giyeok.bibix.graph

import com.giyeok.bibix.ast.BibixParser
import com.giyeok.bibix.frontend.BuildFrontend
import com.giyeok.bibix.plugins.prelude.preludePlugin
import com.giyeok.bibix.plugins.prelude.preludeSource
import org.junit.jupiter.api.Test

class TaskGraphTest {
  @Test
  fun test() {
    val source =
      this::class.java.getResourceAsStream("/test1.bbx")!!.readAllBytes().decodeToString()
    val script = BibixParser.parse(source)

    val preloadedPluginNames = BuildFrontend.defaultPreloadedPlugins.keys
    val preludeNames = setOf(
      "bibix",
      "Env",
      "OS",
      "Linux",
      "OSX",
      "Windows",
      "Arch",
      "BibixProject",
      "git",
      "glob",
      "env",
      "currentEnv"
    )
    val graph = TaskGraph.fromScript(script, preloadedPluginNames, preludeNames)
    println(graph)

    println(dotGraphFrom(graph, source))
  }

  @Test
  fun testPrelude() {
    val preludeGraph = TaskGraph.fromDefs(preludePlugin.defs, setOf("bibix"), setOf("native"))
    println(dotGraphFrom(preludeGraph, preludeSource))
  }
}

fun String.indentWidth(): Int {
  val indent = indexOfFirst { !it.isWhitespace() }
  return if (indent < 0) 0 else indent
}
