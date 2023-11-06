package com.giyeok.bibix.graph

import com.giyeok.bibix.graph.runner.BuildTask

class BuildTaskRelsGraph {
  private val buildTasksGraph = mutableMapOf<BuildTask, MutableSet<BuildTask>>()

  fun addTaskRel(parentTask: BuildTask, childTask: BuildTask) {
    buildTasksGraph.getOrPut(parentTask) { mutableSetOf() }.add(childTask)
  }

  fun addTaskRel(parentTask: BuildTask, childTasks: Collection<BuildTask>) {
    buildTasksGraph.getOrPut(parentTask) { mutableSetOf() }.addAll(childTasks)
  }

  inner class Subgraph(val tasks: Set<BuildTask>) {
    fun printGraph() {
      val buildTaskNumbers = mutableMapOf<BuildTask, String>()

      println("digraph G {")
      tasks.forEach { task ->
        if (task !in buildTaskNumbers) {
          val nodeId = "n${buildTaskNumbers.size + 1}"
          buildTaskNumbers[task] = nodeId
          println("  $nodeId [label=\"${escapeDotString("$task")}\"];")
        }
      }
      tasks.forEach { start ->
        buildTasksGraph[start]?.intersect(tasks)?.forEach { end ->
          println("  ${buildTaskNumbers.getValue(start)} -> ${buildTaskNumbers.getValue(end)};")
        }
      }
      println("}")
    }
  }

  fun checkCycle() {
    buildTasksGraph.keys.forEach { node ->
      checkCycle(node)
    }
  }

  fun checkCycle(task: BuildTask) {
    checkCycle(task, listOf())
  }

  fun checkCycle(task: BuildTask, path: List<BuildTask>) {
    if (task in path) {
      println("!! $path")
      Subgraph(path.toSet()).printGraph()
    }
    buildTasksGraph[task]?.forEach {
      checkCycle(it, path + task)
    }
  }
}
