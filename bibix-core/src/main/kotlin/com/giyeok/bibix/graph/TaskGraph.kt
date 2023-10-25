package com.giyeok.bibix.graph

import com.giyeok.bibix.ast.BibixAst


// TaskGraph는 프로젝트 하나(즉 스크립트 하나)의 내용만 포함한다.
// import해서 사용하는 다른 프로젝트의 def들은 별도의 TaskGraph로 관리한다.
class TaskGraph(
  val astNodes: Map<Int, BibixAst.AstNode>,
  val nameLookupTable: NameLookupTable,
  val packageName: String?,
  val nodes: Map<TaskId, TaskNode>,
  val edges: List<TaskEdge>,
  val scriptVars: Map<String, TaskId>,
  val varRedefs: Map<TaskId, Map<String, TaskId>>,
) {
  val edgesByStart = edges.groupBy { it.start }
  val edgesByEnd = edges.groupBy { it.end }

  companion object {
    fun fromScript(
      script: BibixAst.BuildScript,
      preloadedPluginNames: Set<String>,
      preludeNames: Set<String>
    ): TaskGraph = fromDefs(
      script.packageName?.tokens?.joinToString("."),
      script.defs,
      preloadedPluginNames,
      preludeNames,
      false
    )

    fun fromDefs(
      packageName: String?,
      defs: List<BibixAst.Def>,
      preloadedPluginNames: Set<String>,
      preludeNames: Set<String>,
      nativeAllowed: Boolean,
    ): TaskGraph {
      val nodeIdsMap = mutableMapOf<Int, BibixAst.AstNode>()
      defs.forEach { def ->
        traverseAst(def) { nodeIdsMap[it.nodeId] = it }
      }
      val nameLookup = NameLookupTable.fromDefs(defs)
      val builder = TaskGraphBuilder(nodeIdsMap, nameLookup, packageName)
      val rootNameScope = ScopedNameLookupTable(listOf(), nameLookup, null)
      val nameLookupCtx =
        NameLookupContext(nameLookup, preloadedPluginNames, preludeNames, rootNameScope)
      builder.addDefs(defs, GraphBuildContext(nameLookupCtx, mapOf(), false, nativeAllowed), true)
      return builder.build()
    }

//    fun fromDefs(
//      packageName: BibixAst.Name?,
//      defs: List<BibixAst.Def>,
//      preloadedPluginNames: Set<String>,
//      preludeNames: Set<String>,
//      nativeAllowed: Boolean,
//    ): TaskGraph = fromDefs(packageName, defs, preloadedPluginNames, preludeNames, nativeAllowed)
  }

  fun reachableNodesFrom(tasks: List<TaskId>): Set<TaskId> {
    val reachables = mutableSetOf<TaskId>()

    fun traverse(nodeId: TaskId) {
      if (!reachables.contains(nodeId)) {
        reachables.add(nodeId)
        edgesByStart[nodeId]?.forEach { outgoing ->
          traverse(outgoing.end)
        }
      }
    }
    tasks.forEach { traverse(it) }
    return reachables
  }
}
