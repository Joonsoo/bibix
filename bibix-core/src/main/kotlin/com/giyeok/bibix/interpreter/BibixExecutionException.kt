package com.giyeok.bibix.interpreter

import com.giyeok.bibix.interpreter.task.Task

class BibixExecutionException(message: String, val trace: List<Task>) : Exception(message) {
  constructor(message: String, tasksMap: Map<Int, Task>, trace: List<Int>) :
    this(message, trace.map { tasksMap.getValue(it) })
}
