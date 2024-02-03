package com.giyeok.bibix.interpreter.coroutine

import com.giyeok.bibix.interpreter.task.Task
import kotlin.coroutines.CoroutineContext

data class TaskElement(val task: Task) : CoroutineContext.Element {
  object Key : CoroutineContext.Key<TaskElement>

  override val key: CoroutineContext.Key<*> get() = Key
}
