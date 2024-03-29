package com.giyeok.bibix.graph

import kotlin.math.absoluteValue

fun dotGraphFrom(graph: ExprGraph, source: String): String {
  val writer = CodeWriter()
  writer.writeLine("digraph tasks {")
  writer.indent {
    graph.nodes.forEach { (id, node) ->
      val nodeDescription =
        escapeDotString(id.toNodeId() + "\\l" + nodeDescription(id, node, graph, source))
      writer.writeLine("${id.toNodeId()} [label=\"$nodeDescription\", shape=rect];")
    }

    graph.edges.forEach { edge ->
      writer.writeLine("${edge.start.toNodeId()} -> ${edge.end.toNodeId()};")
    }
  }
  writer.writeLine("}")
  return writer.toString()
}

fun nodeDescription(id: ExprNodeId, node: ExprGraphNode, graph: ExprGraph, source: String): String =
  when (node) {
    is ExprAstNode<*> -> source.substring(node.ast.start, node.ast.end)
    is MemberAccessNode -> "member ${node.target} ${node.memberNames}"
    is CallExprNode -> "${source.substring(node.callExpr.start, node.callExpr.end)}"
    is LocalBuildRuleRef -> "local buildrule ${node.name}"
    is LocalDataClassRef -> "local data class ${node.name}"
    is LocalTargetRef -> "local target ${node.name}"
    is LocalVarRef -> "local var ${node.name}"
    is LocalEnumValue -> "local enum ${node.enumType} ${node.enumValue}"
    is LocalActionRef -> "local action ${node.name}"
    is LocalActionRuleRef -> "local action rule ${node.name}"
    is ImportedExpr -> "imported ${node.import} ${node.name}"
    is ImportedExprFromPrelude -> "imported prelude ${node.name}"
    is ValueCastNode -> "cast"
  }

fun ExprNodeId.toNodeId(): String = "n${this.hashCode().absoluteValue}"

fun escapeDotString(text: String): String {
  val lines = text.lines()
//  val linesTrimmed = if (lines.size <= 1) {
//    lines
//  } else {
//    val firstIndent = lines.first().indentWidth()
//    val minCommonIndent = lines.drop(1).minOf(String::indentWidth)
//    if (firstIndent >= minCommonIndent) {
//      lines.map { it.substring(minCommonIndent) }
//    } else if (firstIndent == 0) {
//      listOf(lines.first()) + lines.drop(1).map { it.substring(minCommonIndent) }
//    } else {
//      lines
//    }
//  }
  return lines.joinToString("") {
    it.replace("\"", "\\\"") + "\\l"
  }
}
