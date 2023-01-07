package com.giyeok.bibix.interpreter.task

import com.giyeok.bibix.interpreter.coroutine.TaskElement
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext

// 태스크 사이의 관계를 저장해서 싸이클을 찾아내는 클래스
class TaskRelGraph {
  fun add(requester: Task, task: Task): Task {
    // TODO
    return task
  }

  suspend fun <T> withTask(requester: Task, task: Task, body: suspend (Task) -> T): T {
    add(requester, task)
    return withContext(currentCoroutineContext() + TaskElement(task)) {
      // TODO requester 태스크는 suspend로 돌리고 task를 비어있는 스레드에 할당해서 돌리고
      body(task)
      // TODO 결과가 나오면 requester 태스크를 다시 active로 돌린다
    }
  }
}
