package com.giyeok.bibix.graph

import com.giyeok.bibix.ast.BibixParser
import org.junit.jupiter.api.Test
import kotlin.math.absoluteValue

class TasksGraphTest {
  @Test
  fun test() {
    val source =
      this::class.java.getResourceAsStream("/test1.bbx")!!.readAllBytes().decodeToString()
    val script = BibixParser.parse(source)

    val graph = TasksGraph.fromScript(script, setOf("glob", "git", "env", "bibix"))
    println(graph)

    println(dotGraphFrom(graph, source))
  }
}

fun String.indentWidth(): Int {
  val indent = indexOfFirst { !it.isWhitespace() }
  return if (indent < 0) 0 else indent
}

fun dotGraphFrom(graph: TasksGraph, source: String): String {
  val writer = CodeWriter()
  writer.writeLine("digraph tasks {")
  writer.indent {
    graph.nodes.forEach { (id, node) ->
      val astNode = graph.astNodes[id.nodeId]
      val nodeSource = astNode?.let { source.substring(it.start, it.end) }
      val nodeDescription = when (node) {
        is ExprNode -> "expr\n$nodeSource"
        is ImportNode -> "import\n$nodeSource"
        is TargetNode -> "target\n$nodeSource"
        is ImportedTaskNode -> "imported(${node.remainingNames})\n$nodeSource"
        is VarNode -> "var\n$nodeSource"
        is PreludeTaskNode -> "prelude ${node.name}"
        is ImportInstanceNode -> "import instance\n$nodeSource"
      }
      val lines = "${node.id.nodeId} $nodeDescription".lines()
      val linesTrimmed = if (lines.size <= 1) {
        lines
      } else {
        val firstIndent = lines.first().indentWidth()
        val minCommonIndent = lines.drop(1).minOf(String::indentWidth)
        if (firstIndent >= minCommonIndent) {
          lines.map { it.substring(minCommonIndent) }
        } else if (firstIndent == 0) {
          listOf(lines.first()) + lines.drop(1).map { it.substring(minCommonIndent) }
        } else {
          lines
        }
      }
      val textEscaped = linesTrimmed.joinToString("") {
        it.replace("\"", "\\\"") + "\\l"
      }
      writer.writeLine("${id.toNodeId()} [label=\"$textEscaped\", shape=rect];")
    }

    graph.edges.forEach { edge ->
      writer.writeLine("${edge.start.toNodeId()} -> ${edge.end.toNodeId()};")
    }
  }
  writer.writeLine("}")
  return writer.toString()
}

fun TaskId.toNodeId(): String =
  if (this.additionalId == null) "n${this.nodeId}" else "n${this.nodeId}_${this.additionalId.hashCode().absoluteValue}"

private operator fun String.times(count: Int): String =
  (0 until count).joinToString("") { this }
