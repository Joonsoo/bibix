package com.giyeok.bibix.interpreter.task

import com.giyeok.bibix.interpreter.coroutine.TaskElement
import com.giyeok.bibix.interpreter.expr.EvaluationResult
import com.google.common.annotations.VisibleForTesting
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

  private val memoMap = mutableMapOf<Task, MutableStateFlow<EvaluationResult?>>()
  private val memoMutex = Mutex()

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

    return withContext(currentCoroutineContext() + TaskElement(task)) {
      // TODO requester 태스크는 suspend로 돌리고 task를 비어있는 스레드에 할당해서 돌리고
      body(task)
      // TODO 결과가 나오면 requester 태스크를 다시 active로 돌린다
    }
  }

  suspend fun withTaskMemo(
    requester: Task,
    task: Task,
    body: suspend CoroutineScope.(Task) -> EvaluationResult
  ): EvaluationResult = coroutineScope {
    val (newMemo, stateFlow) = memoMutex.withLock {
      val existing = memoMap[task]
      if (existing == null) {
        val newStateFlow = MutableStateFlow<EvaluationResult?>(null)
        memoMap[task] = newStateFlow
        Pair(true, newStateFlow)
      } else {
        Pair(false, existing)
      }
    }
    if (newMemo) {
      val result = withTask(requester, task, body)
      stateFlow.emit(result)
      result
    } else {
      stateFlow.filterNotNull().first()
    }
  }
}
