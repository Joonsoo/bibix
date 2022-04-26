package com.giyeok.bibix.runner

import com.giyeok.bibix.buildscript.BuildGraph
import com.giyeok.bibix.buildscript.ExprNode
import java.time.Duration
import java.time.Instant

interface BuildTaskRoutineLogger {
  fun logRoutineAdded(routineId: BuildTaskRoutineId)
  fun logRoutineStarted(routineId: BuildTaskRoutineId)
  fun logRoutineEnded(routineId: BuildTaskRoutineId)
  fun logRequires(
    routineId: BuildTaskRoutineId,
    requirings: List<BuildTask>,
    newRequirings: Set<BuildTask>,
  )
}

class BuildTaskRoutineLoggerImpl(
  private val tasks: MutableList<BuildTask> = mutableListOf(),
  private val added: MutableMap<BuildTaskRoutineId, Instant> = mutableMapOf(),
  private val started: MutableMap<BuildTaskRoutineId, Instant> = mutableMapOf(),
  private val ended: MutableMap<BuildTaskRoutineId, Instant> = mutableMapOf(),
  private val requires: MutableMap<BuildTaskRoutineId, List<BuildTask>> = mutableMapOf(),
) : BuildTaskRoutineLogger {
  override fun logRoutineAdded(routineId: BuildTaskRoutineId) {
    synchronized(this) {
      if (!tasks.contains(routineId.buildTask)) {
        tasks.add(routineId.buildTask)
      }
      added[routineId] = Instant.now()
    }
  }

  override fun logRoutineStarted(routineId: BuildTaskRoutineId) {
    synchronized(this) {
      started[routineId] = Instant.now()
    }
  }

  override fun logRoutineEnded(routineId: BuildTaskRoutineId) {
    synchronized(this) {
      ended[routineId] = Instant.now()
    }
  }

  override fun logRequires(
    routineId: BuildTaskRoutineId,
    requirings: List<BuildTask>,
    newRequirings: Set<BuildTask>,
  ) {
    synchronized(this) {
      requires[routineId] = requires.getOrDefault(routineId, listOf()) + requirings
    }
  }

  fun printLogs(buildGraph: BuildGraph) {
    synchronized(this) {
      check(added.keys.containsAll(started.keys))
      check(added.keys.containsAll(ended.keys))
      check(added.keys.map { it.buildTask }.containsAll(tasks))

      val routineIdsByTask = added.keys.groupBy { it.buildTask }
      val routinesByDurations = mutableMapOf<BuildTaskRoutineId, Duration>()

      tasks.forEach { task ->
        val routineIds = routineIdsByTask.getValue(task)
        println(task)
        when (task) {
          is BuildTask.EvalExpr -> {
            val exprGraph = buildGraph.exprGraphs[task.exprGraphId]
            // TODO
            when (val exprNode = task.exprNode) {
              is ExprNode.CallExprNode -> {
                val callExpr = exprGraph.callExprs[exprNode.callExprId]
                println("  -> ${callExpr.target}")
              }
              else -> {
                // do nothing
              }
            }
          }
          else -> {
            // do nothing
          }
        }
        routineIds.forEachIndexed { index, routineId ->
          val addedTime = added[routineId]!!
          val startedTime = started[routineId]!!
          val endedTime = ended[routineId]!!
          val addedToStarted = Duration.between(addedTime, startedTime)
          val startedToEnded = Duration.between(startedTime, endedTime)
          routinesByDurations[routineId] = startedToEnded
          println("  $index: ${routineId.id} $addedTime +${addedToStarted.toMillis()} +${startedToEnded.toMillis()}")
          val requirings = requires[routineId] ?: listOf()
          if (requirings.isNotEmpty()) {
            println("    requires ${requirings.first()}")
            requirings.drop(1).forEach { requiring ->
              println("        $requiring")
            }
          }
        }
      }

      routinesByDurations.entries.toList().sortedByDescending { it.value }.take(10)
        .forEach { (routineId, duration) ->
          println("$duration $routineId")
        }
    }
  }
}
