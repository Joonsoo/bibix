package com.giyeok.bibix.graph.runner

import java.util.concurrent.ConcurrentHashMap

// TODO TaskRelManager는 기본적으로 진행 상황 파악을 위한 시각화 기능을 위한 것
//      현재는 빌드 태스크 사이클 감지도 하고 있지만, 이건 나중에는 build graph를 로드할 때 하면 좋을 듯
class TaskRelManager {
  // TaskRelManager는 모든 BuildTask의 관계를 관리하지는 않고, 그 중 관심 있는 몇몇 종류만 관리한다.
  // 그래서 cycle 검사할 때 notableRels만 검색해서 검색 속도를 빠르게 하기 위함
  private val notableAncestors = ConcurrentHashMap<BuildTask, MutableSet<BuildTask>>()
  private val notableRels = TasksGraph()

  fun addRootTask(task: BuildTask) {
    check(task.isNotableTask())
    notableAncestors[task] = mutableSetOf(task)
  }

  private fun BuildTask.isNotableTask() = when (this) {
    is EvalAction, is EvalCallExpr, is EvalTarget, is Import -> true
    else -> false
  }

  fun markTaskFinished(task: BuildTask) {
  }

  fun markTaskFailed(failedTask: BuildTask, subResult: FailureOr.Failure<*>) {
  }

  private fun checkCycleFrom(task: BuildTask, path: TaskList): TasksCycle? {
    if (path.contains(task)) {
      return TasksCycle(path.takeUntil(task, mutableListOf(task)))
    }

    val nextPath = TaskList.Cons(task, path)
    notableRels.childrenOf(task)?.forEach { child ->
      val cycle = checkCycleFrom(child, nextPath)
      if (cycle != null) {
        return cycle
      }
    }
    return null
  }

  private fun addNotableTaskRel(parentTask: BuildTask, childTask: BuildTask) {
    if (childTask.isNotableTask()) {
      notableAncestors[parentTask]?.forEach { ancestor ->
        notableRels.addRel(ancestor, childTask)
      }
      notableAncestors[childTask] = mutableSetOf(childTask)
    } else {
      notableAncestors[parentTask]?.let { ancestors ->
        notableAncestors.getOrPut(childTask) { ConcurrentHashMap.newKeySet() }.addAll(ancestors)
      }
    }
  }

  // 새로 추가된 task 관계에 의해 싸이클이 생기면 TasksCycle을 반환한다. 싸이클이 발견되지 않으면 null을 반환한다
  fun addTaskRelation(parentTask: BuildTask, childTask: BuildTask): TasksCycle? {
    addNotableTaskRel(parentTask, childTask)
    return if (childTask.isNotableTask()) {
      checkCycleFrom(childTask, TaskList.Nil)
    } else {
      null
    }
  }

  // 새로 추가된 task 관계에 의해 싸이클이 생기면 TasksCycle을 반환한다. 싸이클이 발견되지 않으면 null을 반환한다
  fun addTaskRelations(parentTask: BuildTask, childrenTasks: List<BuildTask>): TasksCycle? {
    childrenTasks.forEach { childTask ->
      addNotableTaskRel(parentTask, childTask)
    }
    val notableChildren = childrenTasks.filter { it.isNotableTask() }
    if (notableChildren.isNotEmpty()) {
      notableChildren.forEach { child ->
        val cycle = checkCycleFrom(child, TaskList.Nil)
        if (cycle != null) {
          return cycle
        }
      }
    }
    return null
  }
}

data class TasksCycle(val path: List<BuildTask>)

sealed class TaskList {
  abstract fun contains(task: BuildTask): Boolean

  fun takeUntil(task: BuildTask, list: MutableList<BuildTask>): List<BuildTask> {
    var pointer = this
    while (pointer != Nil) {
      check(pointer is Cons)
      list.add(pointer.task)
      if (task == pointer.task) break
      pointer = pointer.parent
    }
    return list
  }

  data class Cons(val task: BuildTask, val parent: TaskList): TaskList() {
    override fun contains(task: BuildTask): Boolean =
      if (this.task == task) true else parent.contains(task)
  }

  data object Nil: TaskList() {
    override fun contains(task: BuildTask): Boolean = false
  }
}

class TasksGraph {
  private val parentToChild = ConcurrentHashMap<BuildTask, MutableSet<BuildTask>>()

  fun addRel(parent: BuildTask, child: BuildTask) {
    parentToChild.getOrPut(parent) { ConcurrentHashMap.newKeySet() }.add(child)
  }

  fun addRels(parent: BuildTask, children: List<BuildTask>) {
    parentToChild.getOrPut(parent) { ConcurrentHashMap.newKeySet() }.addAll(children)
  }

  fun childrenOf(parent: BuildTask) = parentToChild[parent]
}
