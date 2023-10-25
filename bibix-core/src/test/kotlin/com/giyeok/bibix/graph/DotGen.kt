package com.giyeok.bibix.graph

import com.giyeok.bibix.graph.runner.*
import kotlin.math.absoluteValue

fun dotGraphFrom(
  graph: TaskGraph,
  source: String,
  pid: ProjectInstanceId? = null
): String {
  val writer = CodeWriter()
  writer.writeLine("digraph tasks {")
  writer.indent {
    writeGraphContent(graph, source, writer, pid)
  }
  writer.writeLine("}")
  return writer.toString()
}

fun writeGraphContent(
  graph: TaskGraph,
  source: String,
  writer: CodeWriter,
  pid: ProjectInstanceId? = null
) {
  graph.nodes.forEach { (id, node) ->
    val astNode = graph.astNodes[id.nodeId]
    val nodeSource = astNode?.let { source.substring(it.start, it.end) }
    val nodeDescription = when (node) {
      is ExprNode<*> -> "expr\n$nodeSource"
      is TypeNode<*> -> "type\n$nodeSource"
      is ImportNode -> "import\n$nodeSource"
      is TargetNode -> "target\n$nodeSource"
      is VarNode -> "var\n$nodeSource"
      is PreloadedPluginNode -> "preloaded ${node.name}"
      is MemberAccessNode -> "member ${node.target} ${node.remainingNames}"
      is PreludeTaskNode -> "prelude ${node.name}"
      is PreludeMemberNode -> "prelude ${node.preludeName} ${node.remainingNames}"
      is ImportInstanceNode -> "import instance(${node.id.additionalId})\n$nodeSource"
      is BuildRuleNode -> "buildrule\n$nodeSource"
      is BibixTypeNode -> "bibixtype\n${node.bibixType}"
      is DataClassTypeNode -> "class\n$nodeSource"
      is SuperClassTypeNode -> "super class\n$nodeSource"
      is EnumTypeNode -> "enum\n$nodeSource"
      is EnumValueNode -> "enum val\n${node.enumDef.name}.${node.memberName}"
      is ClassElemCastNode -> "class cast\n$nodeSource"
      is NativeImplNode -> "native"
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
    writer.writeLine("${id.toNodeId(pid)} [label=\"$textEscaped\", shape=rect];")
  }

  graph.edges.forEach { edge ->
    val edgeLabel = edge.edgeType.toLabelString()
    writer.writeLine("${edge.start.toNodeId(pid)} -> ${edge.end.toNodeId(pid)} [label=\"$edgeLabel\"];")
  }

  graph.varRedefs.forEach { (pluginId, varRedef) ->
    varRedef.forEach { (varName, varRedefValue) ->
      writer.writeLine("${pluginId.toNodeId(pid)} -> ${varRedefValue.toNodeId(pid)} [label=\"redef $varName\"];")
    }
  }
}

fun dotGraphFrom(runner: GlobalTaskRunner): String {
  val writer = CodeWriter()
  writer.writeLine("digraph tasks {")
  writer.indent {
    val mainGraph = runner.mainProjectGraph
    val mainSource = runner.globalGraph.projectSources.getValue(MainProjectId.projectId)
    writeGraphContent(mainGraph, mainSource, writer, MainProjectId)

    runner.importInstances.forEach { (projectId, importerIds) ->
      importerIds.forEach { importerId ->
        val graph = runner.globalGraph.projectGraphs.getValue(projectId)
        val source = runner.globalGraph.projectSources.getValue(projectId)
        writeGraphContent(graph, source, writer, ImportedProjectId(projectId, importerId))
      }
    }
  }
  writer.writeLine("}")
  return writer.toString()
}

fun TaskEdgeType.toLabelString(): String = when (this) {
  TaskEdgeType.Definition -> "def"
  TaskEdgeType.ValueDependency -> "val"
  TaskEdgeType.CalleeDependency -> "callee"
  TaskEdgeType.Reference -> "ref"
  TaskEdgeType.ImportDependency -> "import"
  TaskEdgeType.TypeDependency -> "type"
  TaskEdgeType.ImportInstance -> "import"
  TaskEdgeType.DefaultValueDependency -> "defval"
  TaskEdgeType.ClassInherit -> "inherit"
  TaskEdgeType.ClassMember -> "member"
  TaskEdgeType.OverridingValueDependency -> "override"
}

private operator fun String.times(count: Int): String =
  (0 until count).joinToString("") { this }
