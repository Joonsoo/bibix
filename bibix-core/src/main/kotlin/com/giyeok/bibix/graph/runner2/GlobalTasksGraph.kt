package com.giyeok.bibix.graph.runner2

import com.giyeok.bibix.graph.*
import com.giyeok.bibix.graph.runner.toNodeId
import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import kotlinx.coroutines.runBlocking
import java.nio.file.Path
import kotlin.io.path.absolute
import kotlin.io.path.readText

// import된 프로젝트들을 포함해서 여러 프로젝트들의 TaskGraph를 통합
class GlobalTaskGraph private constructor(
  val projectLocations: BiMap<Int, BibixProjectLocation>,
  val projectGraphs: MutableMap<Int, TaskGraph>,
  val projectSources: MutableMap<Int, String>,
  val globalEdges: MutableList<GlobalTaskEdge>,
) {
  constructor(
    projects: Map<Int, ProjectInfo>,
  ): this(
    HashBiMap.create(projects.mapValues { it.value.location }),
    projects.mapValues { it.value.graph }.toMutableMap(),
    projects.mapValues { it.value.scriptSource }.toMutableMap(),
    mutableListOf()
  )

  data class ProjectInfo(
    val location: BibixProjectLocation,
    val scriptSource: String,
    val graph: TaskGraph,
  )

  private val edgesByStart =
    mutableMapOf<GlobalTaskId, MutableMap<Pair<GlobalTaskId, GlobalTaskId>, GlobalTaskEdge>>()
  private val edgesByEnd =
    mutableMapOf<GlobalTaskId, MutableMap<Pair<GlobalTaskId, GlobalTaskId>, GlobalTaskEdge>>()

  fun addProject(projectId: Int, location: BibixProjectLocation, graph: TaskGraph, source: String) {
    check(projectId !in projectGraphs)
    check(!projectLocations.containsValue(location))
    projectLocations[projectId] = location
    projectGraphs[projectId] = graph
    projectSources[projectId] = source
  }

  // prelude, preloaded 플러그인도 일종의 프로젝트. 그들은 location은 없음
  fun addProject(projectId: Int, graph: TaskGraph, source: String) {
    check(projectId !in projectGraphs)
    projectGraphs[projectId] = graph
    projectSources[projectId] = source
  }

  fun getProjectGraph(projectId: Int): TaskGraph =
    projectGraphs[projectId] ?: throw IllegalStateException()

  fun getProjectIdByLocation(location: BibixProjectLocation): Int? =
    projectLocations.inverse()[location]

  fun addGlobalEdge(edge: GlobalTaskEdge) {
    check(edge.start.contextId != edge.end.contextId)
    check(edge.start.contextId.projectId in projectGraphs)
    check(edge.end.contextId.projectId in projectGraphs)

    globalEdges.add(edge)

    val byStartMap = edgesByStart.getOrPut(edge.start) { mutableMapOf() }
    val byStart = byStartMap[edge.pair]
    check(byStart == null || byStart == edge)
    byStartMap[edge.pair] = edge

    val byEndMap = edgesByEnd.getOrPut(edge.end) { mutableMapOf() }
    val byEnd = byEndMap[edge.pair]
    check(byEnd == null || byEnd == edge)
    byEndMap[edge.pair] = edge
  }

  fun getNode(projectId: Int, taskId: TaskId): TaskNode =
    getProjectGraph(projectId).nodes[taskId] ?: throw IllegalStateException()

  fun edgesByStart(node: GlobalTaskId): Set<GlobalTaskEdge> {
    val localGraph = projectGraphs[node.contextId.projectId]
      ?: throw IllegalStateException()
    val localEdgesFromNode = localGraph.edgesByStart[node.taskId] ?: listOf()
    val globalEdgesFromNode = edgesByStart[node]?.values?.toSet() ?: setOf()
    return localEdgesFromNode.map { edge ->
      check(edge.start == node.taskId)
      GlobalTaskEdge(node, GlobalTaskId(node.contextId, edge.end), edge.edgeType)
    }.toSet() + globalEdgesFromNode
  }

  fun edgesByEnd(node: GlobalTaskId): Set<GlobalTaskEdge> {
    val localGraph = projectGraphs[node.contextId.projectId]
      ?: throw IllegalStateException()
    val localEdgesToNode = localGraph.edgesByEnd[node.taskId] ?: listOf()
    val globalEdgesToNode = edgesByEnd[node]?.values?.toSet() ?: setOf()
    return localEdgesToNode.map { edge ->
      check(edge.end == node.taskId)
      GlobalTaskEdge(GlobalTaskId(node.contextId, edge.start), node, edge.edgeType)
    }.toSet() + globalEdgesToNode
  }

  fun reachableNodesFrom(nodes: Collection<GlobalTaskId>): Set<GlobalTaskId> {
    val reachables = mutableSetOf<GlobalTaskId>()

    fun traverse(nodeId: GlobalTaskId) {
      if (!reachables.contains(nodeId)) {
        reachables.add(nodeId)
        edgesByStart(nodeId).forEach { outgoing ->
          traverse(outgoing.end)
        }
      }
    }
    nodes.distinct().forEach { traverse(it) }
    return reachables
  }

  fun depsGraphFrom(nodes: Set<GlobalTaskId>): GlobalTaskDepsGraph {
//    val depsGraph = GlobalTaskDepsGraphBuilder(this)
//
//    fun traverse(nodeId: GlobalTaskId) {
//      if (!depsGraph.hasNode(nodeId)) {
//        depsGraph.addNode(nodeId)
//        edgesByStart(nodeId).forEach { outgoing ->
//          depsGraph.addEdge(outgoing)
//          traverse(outgoing.end)
//        }
//      }
//    }
//    nodes.distinct().forEach { traverse(it) }
//    return depsGraph.build()
    val depsGraph = GlobalTaskDepsGraph(this)
    runBlocking {
      depsGraph.addNodesAndReachables(nodes)
    }
    return depsGraph
  }

  fun getProjectIdByPackageName(packageName: String) =
    projectGraphs.entries.find { it.value.packageName == packageName }?.key
}

data class BibixProjectLocation(val projectRoot: Path, val scriptName: String) {
  constructor(projectRoot: Path): this(projectRoot.normalize().absolute(), "build.bbx")

  init {
    check(projectRoot.normalize() == projectRoot && projectRoot.isAbsolute)
  }

  suspend fun readScript(): String =
    projectRoot.resolve(scriptName).readText()
}

data class TaskContextId(val projectId: Int, val contextId: Int)

data class GlobalTaskId(val contextId: TaskContextId, val taskId: TaskId) {
  // dev purpose
  fun toNodeId(): String =
    "${contextId.projectId}_${contextId.contextId}_${taskId.toNodeId()}"
}

data class GlobalTaskEdge(
  val start: GlobalTaskId,
  val end: GlobalTaskId,
  val edgeType: TaskEdgeType
) {
  val pair get() = Pair(start, end)
}
