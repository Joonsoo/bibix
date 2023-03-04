package com.giyeok.bibix.interpreter.task

import com.giyeok.bibix.ast.BibixAst
import com.giyeok.bibix.base.BibixValue
import com.giyeok.bibix.base.CName
import com.giyeok.bibix.base.SourceId
import com.giyeok.bibix.interpreter.coroutine.TaskElement
import com.giyeok.bibix.repo.BibixValueWithObjectHash
import com.giyeok.bibix.repo.ObjectHash
import com.google.common.annotations.VisibleForTesting
import com.google.protobuf.ByteString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

// 태스크 사이의 관계를 저장해서 싸이클을 찾아내는 클래스
class TaskRelGraph {
  @VisibleForTesting
  val downstream = mutableMapOf<Task, MutableList<Task>>()

  @VisibleForTesting
  val upstream = mutableMapOf<Task, MutableList<Task>>()

  private val depsMutex = Mutex()

  private val referredNodes = ConcurrentHashMap<Pair<SourceId, Int>, BibixAst.AstNode>()

  private suspend fun add(requester: Task, task: Task): Task {
    depsMutex.withLock {
      downstream.getOrPut(requester) { mutableListOf() }.add(task)
      upstream.getOrPut(task) { mutableListOf() }.add(requester)
    }
    return task
  }

  private suspend fun findCycleBetween(start: Task, end: Task): List<Task>? = depsMutex.withLock {
    fun traverse(task: Task, path: List<Task>): List<Task>? {
      if (task == start) {
        return path
      }
      val outgoing = downstream[task] ?: setOf()
      outgoing.forEach { next ->
        val found = traverse(next, path + next)
        if (found != null) {
          return found
        }
      }
      return null
    }
    downstream[start]?.forEach { next ->
      val found = traverse(next, listOf(start, next))
      if (found != null) {
        return found
      }
    }
    return null
  }

  suspend fun <T> withTask(
    requester: Task,
    task: Task,
    body: suspend CoroutineScope.(Task) -> T
  ): T {
    check(requester != task)

    add(requester, task)

    val cycle = findCycleBetween(requester, task)
    check(cycle == null) { "Cycle found: $cycle" }

    return withContext(currentCoroutineContext() + TaskElement(task)) { body(task) }
  }

  fun evalExprTask(
    sourceId: SourceId,
    astNode: BibixAst.AstNode,
    thisValue: BibixValue?
  ): Task.EvalExpr {
    referredNodes[Pair(sourceId, astNode.nodeId)] = astNode
    return Task.EvalExpr(sourceId, astNode.nodeId, thisValue)
  }

  fun evalCallExprTask(
    sourceId: SourceId,
    astNode: BibixAst.AstNode,
    thisValue: BibixValue?
  ): Task {
    referredNodes[Pair(sourceId, astNode.nodeId)] = astNode
    return Task.EvalCallExpr(sourceId, astNode.nodeId, thisValue)
  }

  fun getReferredNodeById(sourceId: SourceId, nodeId: Int): BibixAst.AstNode? =
    referredNodes[Pair(sourceId, nodeId)]

  private val objHashMap = mutableMapOf<ByteString, ObjectHash>()
  private val objMemoMap = mutableMapOf<ByteString, MutableStateFlow<BibixValueWithObjectHash?>>()
  private val memoMutex = Mutex()

  private val outputNames = mutableMapOf<CName, ObjectHash>()

  @VisibleForTesting
  suspend fun getObjHashMap() = memoMutex.withLock { objHashMap }

  @VisibleForTesting
  suspend fun getObjMemoMap() = memoMutex.withLock { objMemoMap }

  suspend fun addOutputMemo(name: CName, objectHash: ObjectHash): Boolean =
    memoMutex.withLock {
      outputNames.putIfAbsent(name, objectHash) == null
    }

  suspend fun withMemo(
    objId: ObjectHash,
    body: suspend CoroutineScope.(ObjectHash) -> BibixValue
  ): BibixValueWithObjectHash = coroutineScope {
    val objIdHash = objId.hashString
    val (newMemo, stateFlow) = memoMutex.withLock {
      val existing = objMemoMap[objIdHash]
      if (existing == null) {
        val newStateFlow = MutableStateFlow<BibixValueWithObjectHash?>(null)
        objMemoMap[objIdHash] = newStateFlow
        check(!objHashMap.containsKey(objIdHash))
        objHashMap[objIdHash] = objId
        Pair(true, newStateFlow)
      } else {
        check(objHashMap[objIdHash] == objId)
        Pair(false, existing)
      }
    }
    if (newMemo) {
      val result = BibixValueWithObjectHash(body(objId), objId)
      stateFlow.emit(result)
      result
    } else {
      stateFlow.filterNotNull().first()
    }
  }

  fun upstreamPathsTo(task: Task): List<List<Task>> {
    fun traverse(task: Task, path: List<Task>): List<List<Task>> {
      val ups = upstream[task]?.toList() ?: listOf()
      return if (ups.isEmpty()) listOf(path) else {
        ups.flatMap { up ->
          traverse(up, path + up)
        }
      }
    }
    return traverse(task, listOf(task))
  }
}
