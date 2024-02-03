package com.giyeok.bibix.interpreter

import com.giyeok.bibix.interpreter.task.Task

class BibixExecutionException(
  message: String,
  val trace: List<Task>,
  cause: Throwable? = null
) : Exception(message, cause) {
  constructor(
    message: String,
    tasksMap: Map<Int, Task>,
    trace: List<Int>,
    cause: Throwable? = null
  ) :
    this(message, trace.map { tasksMap.getValue(it) }, cause)
}
