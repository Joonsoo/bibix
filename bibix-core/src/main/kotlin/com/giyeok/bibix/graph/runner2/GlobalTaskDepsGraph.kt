package com.giyeok.bibix.graph.runner2

import com.giyeok.bibix.graph.TaskId
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class GlobalTaskDepsGraph(
  val globalGraph: GlobalTaskGraph,
) {
  private val nodes = mutableSetOf<GlobalTaskId>()
  private val edges = mutableSetOf<GlobalTaskEdge>()
  private val edgesByEnd = mutableMapOf<GlobalTaskId, MutableSet<GlobalTaskEdge>>()
  private val finishedNodes = mutableSetOf<GlobalTaskId>()
  private val finishedEdges = mutableSetOf<GlobalTaskEdge>()

  val allNodes get():Set<GlobalTaskId> = nodes
  val allEdges get():Set<GlobalTaskEdge> = edges

  // depsCounts에 노드의 key가 아예 없다는 건 준비가 완료돼서 ready로 보내졌다는 의미이고
  // depsCounts[key] == 0 이면 준비는 됐지만 아직 ready로 보내지진 않았다는 뜻이다
  private val depsCounts = mutableMapOf<GlobalTaskId, Int>()

  private val ready = Channel<GlobalTaskId>(Channel.UNLIMITED)

  // readyIds = ready의 read-only 버전
  val nextNodeIds: ReceiveChannel<GlobalTaskId> = ready

  init {
    check(depsCounts.all { it.value > 0 })
    nodes.forEach { node ->
      if (node !in depsCounts) {
        runBlocking {
          notifyNewReady(node)
        }
      }
    }
  }

  private suspend fun notifyNewReady(node: GlobalTaskId) {
    ready.send(node)
  }

  private val mutex = Mutex()

  suspend fun finishNode(node: GlobalTaskId) {
    mutex.withLock {
      check(node in nodes && node !in finishedNodes)
      finishedNodes.add(node)
      edgesByEnd[node]?.forEach { edge ->
        finishEdge(edge)
      }
      if (isDone) {
        ready.close()
      }
    }
  }

  private suspend fun finishEdge(edge: GlobalTaskEdge) {
    check(edge in edges && edge !in finishedEdges)

    val endIsReady: Boolean
    finishedEdges.add(edge)

    val depsCount = depsCounts[edge.start]!!
    if (depsCount == 1) {
      endIsReady = true
      depsCounts.remove(edge.start)
    } else {
      endIsReady = false
      depsCounts[edge.start] = depsCount - 1
    }

    if (endIsReady) {
      notifyNewReady(edge.start)
    }
  }

  private val isDone get() = edges.size == finishedEdges.size

  suspend fun isDone(): Boolean = mutex.withLock { isDone }

  private fun addEdge(edge: GlobalTaskEdge) {
    check(edge !in edges)
    edges.add(edge)
    edgesByEnd.getOrPut(edge.end) { mutableSetOf() }.add(edge)
  }

  // rootNodes와, rootNodes로부터 접근 가능한 노드들을 모두 추가한다
  suspend fun addNodesAndReachables(rootNodes: Set<GlobalTaskId>) {
    val newReadys = mutex.withLock {
      // TODO
      rootNodes.groupBy { it.contextId.projectId }.map { (projectId, newNodesIn) ->
        val graph = globalGraph.getProjectGraph(projectId)
        val reachables = graph.reachableNodesFrom(newNodesIn.map { it.taskId }, true)
        val prjInstanceIds = newNodesIn.map { it.contextId }.toSet()

        fun traverse(taskId: TaskId) {
          prjInstanceIds.forEach { prjInstanceId ->
            val globalTaskId = GlobalTaskId(prjInstanceId, taskId)
            if (nodes.add(globalTaskId)) {
              check(globalTaskId !in depsCounts)
              depsCounts[globalTaskId] = 0
              graph.edgesByStart[taskId]
                ?.filter { it.edgeType.isRequired }
                ?.forEach { outgoing ->
                  check(outgoing.start == taskId)
                  val endNode = GlobalTaskId(prjInstanceId, outgoing.end)
                  val newEdge = GlobalTaskEdge(globalTaskId, endNode, outgoing.edgeType)
                  addEdge(newEdge)
                  if (endNode in finishedNodes) {
                    finishedEdges.add(newEdge)
                  } else {
                    depsCounts[globalTaskId] = (depsCounts[globalTaskId] ?: 0) + 1
                    traverse(outgoing.end)
                  }
                }
            }
          }
        }
        reachables.forEach { traverse(it) }
      }
      val newReadys = (nodes - finishedNodes).filter { depsCounts[it] == 0 }
      newReadys.forEach { depsCounts.remove(it) }
      newReadys
    }
    newReadys.forEach { notifyNewReady(it) }
  }

  suspend fun addEdges(newEdges: List<GlobalTaskEdge>) {
    val realNewEdges = mutex.withLock { newEdges - edges }
    if (realNewEdges.isEmpty()) {
      return
    }
    val newEnds = mutex.withLock {
      check(realNewEdges.all { it.start in nodes })
      realNewEdges.map { it.end } - nodes
    }
    addNodesAndReachables(newEnds.toSet())
    val newReadys = mutex.withLock {
      realNewEdges.forEach { newEdge ->
        if (newEdge !in edges) {
          addEdge(newEdge)
          if (newEdge.end in finishedNodes) {
            finishedEdges.add(newEdge)
          } else {
            depsCounts[newEdge.start] = (depsCounts[newEdge.start] ?: 0) + 1
          }
        }
      }
      val newReadys =
        (realNewEdges.map { it.start }.toSet() - finishedNodes).filter { depsCounts[it] == 0 }
      newReadys.forEach { depsCounts.remove(it) }
      newReadys
    }
    newReadys.forEach { notifyNewReady(it) }
  }

  suspend fun printStatus() = mutex.withLock {
    allNodes.forEach { node ->
      println("${node.toNodeId()}: ${depsCounts[node]}")
    }
    println("finishedNodes: ${finishedNodes.size}")
    println("finishedEdges: ${finishedEdges.size}")
  }

  suspend fun depsCountOf(taskId: GlobalTaskId): Int? = mutex.withLock {
    depsCounts[taskId]
  }

  suspend fun isNodeFinished(node: GlobalTaskId): Boolean = mutex.withLock {
    node in finishedNodes
  }

  suspend fun isEdgeFinished(edge: GlobalTaskEdge): Boolean = mutex.withLock {
    edge in finishedEdges
  }
}
