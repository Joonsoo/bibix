package com.giyeok.bibix.graph.runner

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class GlobalTaskDepsGraphBuilder(
  val graph: GlobalTaskGraph,
  private val nodes: MutableSet<GlobalTaskId>,
  private val edges: MutableSet<GlobalTaskEdge>,
  private val depsCounts: MutableMap<GlobalTaskId, Int>,
) {
  constructor(graph: GlobalTaskGraph, nodes: Collection<GlobalTaskId> = setOf()): this(
    graph,
    nodes.toMutableSet(),
    mutableSetOf(),
    mutableMapOf()
  )

  private val edgesByPair = edges.associateBy { it.pair }
  private val edgesByEnd: MutableMap<GlobalTaskId, MutableSet<GlobalTaskEdge>> =
    edges.groupBy { it.end }.mapValues { (_, edges) -> edges.toMutableSet() }.toMutableMap()

  fun addNode(node: GlobalTaskId) {
    nodes.add(node)
  }

  fun hasNode(node: GlobalTaskId) = nodes.contains(node)

  fun addEdge(edge: GlobalTaskEdge) {
    val existing = edgesByPair[edge.pair]
    check(existing == null || existing == edge)
    edges.add(edge)
    edgesByEnd.getOrPut(edge.end) { mutableSetOf() }.add(edge)
    depsCounts[edge.start] = depsCounts.getOrDefault(edge.start, 0) + 1
  }

  fun build(): GlobalTaskDepsGraph =
    GlobalTaskDepsGraph(graph, nodes, edges, edgesByEnd, mutableSetOf(), depsCounts)
}

class GlobalTaskDepsGraph(
  val graph: GlobalTaskGraph,
  val nodes: Set<GlobalTaskId>,
  val edges: Set<GlobalTaskEdge>,
  val edgesByEnd: Map<GlobalTaskId, Set<GlobalTaskEdge>>,
  private val finishedEdges: MutableSet<GlobalTaskEdge>,
  private val depsCounts: MutableMap<GlobalTaskId, Int>,
) {
  init {
    check(edges.containsAll(finishedEdges))
  }

  private val ready = Channel<GlobalTaskId>(Channel.UNLIMITED)

  // readyIds = ready의 read-only 버전
  val nextNodeIds: ReceiveChannel<GlobalTaskId> = ready

  init {
    nodes.filter { (depsCounts[it] ?: 0) == 0 }.forEach {
      runBlocking {
        ready.send(it)
      }
    }
  }

  private val mutex = Mutex()

  suspend fun finishEdge(edge: GlobalTaskEdge) {
    val endIsReady: Boolean
    mutex.withLock {
      check(edge in edges && edge !in finishedEdges)
      finishedEdges.add(edge)
      val depsCount = depsCounts[edge.start]
      checkNotNull(depsCount)
      if (depsCount == 1) {
        depsCounts.remove(edge.start)
        endIsReady = true
      } else {
        depsCounts[edge.start] = depsCount - 1
        endIsReady = false
      }
    }
    if (endIsReady) {
      ready.send(edge.start)
    } else if (isDone()) {
      ready.close()
    }
  }

  suspend fun finishNode(node: GlobalTaskId) {
    val edges = mutex.withLock {
      check(nodes.contains(node))
      edgesByEnd[node]
    }
    edges?.forEach { edge ->
      finishEdge(edge)
    }
  }

  suspend fun isDone() = mutex.withLock {
    edges.size == finishedEdges.size
  }

  suspend fun addEdges(newEdges: List<GlobalTaskEdge>): Boolean {
    return false
  }
}
