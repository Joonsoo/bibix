package com.giyeok.bibix.interpreter.task

import com.giyeok.bibix.BibixIdProto.ObjectId
import com.giyeok.bibix.base.BibixValue
import com.giyeok.bibix.interpreter.coroutine.TaskElement
import com.giyeok.bibix.interpreter.expr.EvaluationResult
import com.giyeok.bibix.repo.hashString
import com.google.common.annotations.VisibleForTesting
import com.google.protobuf.ByteString
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

// 태스크 사이의 관계를 저장해서 싸이클을 찾아내는 클래스
class TaskRelGraph {
  @VisibleForTesting
  val deps = ConcurrentHashMap<Task, MutableSet<Task>>()

  private val depsMutex = Mutex()

  private fun add(requester: Task, task: Task): Task {
    deps.getOrPut(requester) { mutableSetOf() }.add(task)
    return task
  }

  private suspend fun findCycleBetween(start: Task, end: Task): List<Task>? = depsMutex.withLock {
    fun traverse(task: Task, path: List<Task>): List<Task>? {
      if (task == start) {
        return path
      }
      val outgoing = deps[task] ?: setOf()
      outgoing.forEach { next ->
        val found = traverse(next, path + next)
        if (found != null) {
          return found
        }
      }
      return null
    }
    deps[start]?.forEach { next ->
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

  private val objHashMap = mutableMapOf<ByteString, ObjectId>()
  private val objMemoMap = mutableMapOf<ByteString, MutableStateFlow<BibixValue?>>()
  private val memoMutex = Mutex()

  suspend fun withMemo(
    objId: ObjectId,
    body: suspend CoroutineScope.(ObjectId) -> BibixValue
  ): BibixValue = coroutineScope {
    val objIdHash = objId.hashString()
    val (newMemo, stateFlow) = memoMutex.withLock {
      val existing = objMemoMap[objIdHash]
      if (existing == null) {
        val newStateFlow = MutableStateFlow<BibixValue?>(null)
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
      val result = body(objId)
      stateFlow.emit(result)
      result
    } else {
      stateFlow.filterNotNull().first()
    }
  }
}
