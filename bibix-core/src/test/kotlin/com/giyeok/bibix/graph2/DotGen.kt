package com.giyeok.bibix.graph2

import com.giyeok.bibix.graph.CodeWriter
import com.giyeok.bibix.graph.indentWidth
import kotlin.math.absoluteValue

fun dotGraphFrom(graph: ExprGraph, source: String): String {
  val writer = CodeWriter()
  writer.writeLine("digraph tasks {")
  writer.indent {
    graph.nodes.forEach { (id, node) ->
      val nodeDescription =
        dotEscape(id.toNodeId() + "\\l" + nodeDescription(id, node, graph, source))
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
    is CallExprNode -> "post ${source.substring(node.callExpr.start, node.callExpr.end)}"
    is CallExprCallNode -> source.substring(node.callExpr.start, node.callExpr.end)
    is CallExprParamCoercionNode -> "param coercion ${node.paramLocation}"
    is LocalBuildRuleRef -> "local buildrule ${node.name}"
    is LocalDataClassRef -> "local data class ${node.name}"
    is LocalTargetRef -> "local target ${node.name}"
    is LocalVarRef -> "local var ${node.name}"
    is LocalEnumValue -> "local enum ${node.enumType} ${node.enumValue}"
    is ImportedExpr -> "imported ${node.import} ${node.varCtxId} ${node.name}"
    is ImportedExprFromPreloaded -> "imported preloaded ${node.pluginName} ${node.name}"
    is ImportedExprFromPrelude -> "imported prelude ${node.name} ${node.remaining}"
    is ValueCastNode -> "cast"
  }

fun ExprNodeId.toNodeId(): String = "n${this.hashCode().absoluteValue}"

fun dotEscape(text: String): String {
  val lines = text.lines()
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
  return linesTrimmed.joinToString("") {
    it.replace("\"", "\\\"") + "\\l"
  }
}