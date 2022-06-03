package com.giyeok.bibix.runner

class BuildTaskRelGraph {
  private val deps = mutableMapOf<BuildTask, MutableSet<BuildTask>>()

  fun addDependency(requestTask: BuildTask, task: BuildTask) {
    deps.getOrPut(requestTask) { mutableSetOf() }.add(task)
  }

  // end를 활용해서 좀더 빠르게 할 수도 있지 않을지?
  fun findCycleBetween(start: BuildTask, end: BuildTask): List<BuildTask>? = synchronized(this) {
    fun traverse(task: BuildTask, path: List<BuildTask>): List<BuildTask>? {
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
}
