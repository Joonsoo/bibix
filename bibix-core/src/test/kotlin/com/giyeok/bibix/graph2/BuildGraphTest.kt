package com.giyeok.bibix.graph2

import com.giyeok.bibix.ast.BibixParser
import com.giyeok.bibix.frontend.BuildFrontend
import com.giyeok.bibix.plugins.prelude.preludePlugin
import org.junit.jupiter.api.Test

class BuildGraphTest {
  @Test
  fun test() {
    val source =
      this::class.java.getResourceAsStream("/test1.bbx")!!.readAllBytes().decodeToString()
    val script = BibixParser.parse(source)

    val preloadedPluginNames = BuildFrontend.defaultPreloadedPlugins.keys
    val preludeNames = NameLookupTable.fromDefs(preludePlugin.defs).names.keys
    val graph = BuildGraph.fromScript(script, preloadedPluginNames, preludeNames)
    println(dotGraphFrom(graph.exprGraph, source))
  }
}
