package com.giyeok.bibix.interpreter.task

import com.giyeok.bibix.interpreter.coroutine.TaskElement
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

// 태스크 사이의 관계를 저장해서 싸이클을 찾아내는 클래스
class TaskRelGraph {
  private val deps = ConcurrentHashMap<Task, MutableSet<Task>>()
  private val mutex = Mutex()

  private fun add(requester: Task, task: Task): Task {
    deps.getOrPut(requester) { mutableSetOf() }.add(task)
    return task
  }

  private suspend fun findCycleBetween(start: Task, end: Task): List<Task>? = mutex.withLock {
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

  suspend fun <T> withTask(requester: Task, task: Task, body: suspend (Task) -> T): T {
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
}
