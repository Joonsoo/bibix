package com.giyeok.bibix.graph.runner

import com.giyeok.bibix.graph.TaskEdgeType
import com.giyeok.bibix.graph.TaskGraph
import com.giyeok.bibix.graph.TaskId
import com.giyeok.bibix.graph.TaskNode
import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import kotlinx.coroutines.runBlocking
import java.nio.file.Path
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
    projectLocations.inverse().get(location)

  fun addGlobalEdge(edge: GlobalTaskEdge) {
    check(edge.start.projectInstanceId != edge.end.projectInstanceId)
    check(edge.start.projectInstanceId.projectId in projectGraphs)
    check(edge.end.projectInstanceId.projectId in projectGraphs)

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

  fun getNode(node: GlobalTaskId): TaskNode = getNode(node.projectInstanceId, node.taskId)

  fun getNode(projectInstanceId: ProjectInstanceId, taskId: TaskId): TaskNode {
    val project = getProjectGraph(projectInstanceId.projectId)
    return project.nodes[taskId]
      ?: throw IllegalStateException()
  }

  fun edgesByStart(node: GlobalTaskId): Set<GlobalTaskEdge> {
    val localGraph = projectGraphs[node.projectInstanceId.projectId]
      ?: throw IllegalStateException()
    val localEdgesFromNode = localGraph.edgesByStart[node.taskId] ?: listOf()
    val globalEdgesFromNode = edgesByStart[node]?.values?.toSet() ?: setOf()
    return localEdgesFromNode.map { edge ->
      check(edge.start == node.taskId)
      GlobalTaskEdge(node, GlobalTaskId(node.projectInstanceId, edge.end), edge.edgeType)
    }.toSet() + globalEdgesFromNode
  }

  fun edgesByEnd(node: GlobalTaskId): Set<GlobalTaskEdge> {
    val localGraph = projectGraphs[node.projectInstanceId.projectId]
      ?: throw IllegalStateException()
    val localEdgesToNode = localGraph.edgesByEnd[node.taskId] ?: listOf()
    val globalEdgesToNode = edgesByEnd[node]?.values?.toSet() ?: setOf()
    return localEdgesToNode.map { edge ->
      check(edge.end == node.taskId)
      GlobalTaskEdge(GlobalTaskId(node.projectInstanceId, edge.start), node, edge.edgeType)
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
}

data class BibixProjectLocation(val projectRoot: Path, val scriptName: String) {
  constructor(projectRoot: Path): this(projectRoot, "build.bbx")

  init {
    check(projectRoot.isAbsolute)
  }

  suspend fun readScript(): String =
    projectRoot.resolve(scriptName).readText()
}

// A 프로젝트의 import instance node X -> import node Y. Y가 import하는 프로젝트가 B이면:
// ImportInstanceId(Y, ImporterId(A, X))
//data class ImporterId(val importer: ProjectInstanceId, val importTaskId: TaskId)
//data class ImportInstanceId(val projectId: Int, val importer: ImporterId)

sealed class ProjectInstanceId {
  abstract val projectId: Int
}

data object MainProjectId: ProjectInstanceId() {
  override val projectId: Int = 1
}

data object PreludeProjectId: ProjectInstanceId() {
  override val projectId: Int = 2
}

data class ImportedProjectId(
  override val projectId: Int,
  val importer: GlobalTaskId
): ProjectInstanceId()

data class GlobalTaskId(val projectInstanceId: ProjectInstanceId, val taskId: TaskId)

data class GlobalTaskEdge(
  val start: GlobalTaskId,
  val end: GlobalTaskId,
  val edgeType: TaskEdgeType
) {
  val pair get() = Pair(start, end)
}
