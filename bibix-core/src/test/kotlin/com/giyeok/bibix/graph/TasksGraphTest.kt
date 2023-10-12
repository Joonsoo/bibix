package com.giyeok.bibix.graph

import com.giyeok.bibix.ast.BibixParser
import org.junit.jupiter.api.Test

class TasksGraphTest {
  @Test
  fun test() {
    val script = BibixParser.parse(
      this::class.java.getResourceAsStream("/test1.bbx")!!.readAllBytes().decodeToString()
    )
    val graph = TasksGraph.fromScript(script)
    println(graph)
  }
}
