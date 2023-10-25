package com.giyeok.bibix.graph.runner

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

  // depsCounts에 노드의 key가 아예 없다는 건 준비가 완료돼서 ready로 보내졌다는 의미이고
  // depsCounts[key] == 0 이면 준비는 됐지만 아직 ready로 보내지진 않았다는 뜻이다
  private val depsCounts = mutableMapOf<GlobalTaskId, Int>()

  private val ready = Channel<GlobalTaskId>(Channel.UNLIMITED)

  // readyIds = ready의 read-only 버전
  val nextNodeIds: ReceiveChannel<GlobalTaskId> = ready

  init {
    nodes.forEach { node ->
      if (node !in depsCounts) {
        runBlocking {
          ready.send(node)
        }
      }
    }
  }

  private val mutex = Mutex()

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
      ready.send(edge.start)
    } else if (isDone()) {
      ready.close()
    }
  }

  suspend fun finishNode(node: GlobalTaskId) {
    mutex.withLock {
      check(node in nodes)
      finishedNodes.add(node)
      edgesByEnd[node]?.forEach { edge ->
        finishEdge(edge)
      }
    }
  }

  suspend fun isDone() = mutex.withLock {
    edges.size == finishedEdges.size
  }

  // rootNodes와, rootNodes로부터 접근 가능한 노드들을 모두 추가한다
  suspend fun addNodesAndReachables(rootNodes: Set<GlobalTaskId>) {
    val newReadys = mutex.withLock {
      // TODO
      rootNodes.groupBy { it.projectInstanceId.projectId }.map { (projectId, newNodesIn) ->
        val graph = globalGraph.getProjectGraph(projectId)
        val reachables = graph.reachableNodesFrom(newNodesIn.map { it.taskId })
        val prjInstanceIds = newNodesIn.map { it.projectInstanceId }.toSet()

        fun traverse(taskId: TaskId) {
          prjInstanceIds.forEach { prjInstanceId ->
            val globalTaskId = GlobalTaskId(prjInstanceId, taskId)
            if (nodes.add(globalTaskId)) {
              depsCounts[globalTaskId] = 0
              graph.edgesByStart[taskId]?.forEach { outgoing ->
                check(outgoing.start == taskId)
                val endNode = GlobalTaskId(prjInstanceId, outgoing.end)
                edges.add(GlobalTaskEdge(globalTaskId, endNode, outgoing.edgeType))
                depsCounts[globalTaskId] = (depsCounts[globalTaskId] ?: 0) + 1
                traverse(outgoing.end)
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
    newReadys.forEach { ready.send(it) }
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
          edges.add(newEdge)
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
    newReadys.forEach { ready.send(it) }
  }
}
