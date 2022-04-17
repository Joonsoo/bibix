package com.giyeok.bibix.runner

import com.giyeok.bibix.utils.ProgressIndicator
import java.util.concurrent.LinkedBlockingQueue

class BuildTaskRoutinesManager(
  private val runTasks: (Collection<BuildTask>) -> Unit,
  private val logger: BuildTaskRoutineLogger,
) {
  data class BuildTaskRoutineBody(
    val requires: List<BuildTask>,
    val routine: (List<Any>, ProgressIndicator<BuildTaskRoutineId>) -> Unit
  )

  private var routineIdCounter = 0
  fun nextRoutineId(): Int = synchronized(this) {
    routineIdCounter += 1
    routineIdCounter
  }

  private val runningTasks = mutableSetOf<BuildTask>()
  private val startedRoutines = mutableSetOf<Int>()

  private val taskResults = mutableMapOf<BuildTask, Any>()

  private val routines = mutableMapOf<BuildTaskRoutineId, BuildTaskRoutineBody>()

  // value들이 key에 depend함
  private val requiredBy = mutableMapOf<BuildTask, MutableSet<BuildTaskRoutineId>>()

  sealed class NextRoutine {
    data class BuildTaskRoutine(
      val routineId: BuildTaskRoutineId,
      val block: (ProgressIndicator<BuildTaskRoutineId>) -> Unit
    ) : NextRoutine()

    object BuildFinished : NextRoutine()
  }

  val routinesQueue = LinkedBlockingQueue<NextRoutine>()

  // "task 처리의 과정에서 필요한 루틴으로, requires가 모두 준비되면 routine을 실행해라"
  fun require(
    task: BuildTask,
    requires: List<BuildTask>,
    routine: (List<Any>, ProgressIndicator<BuildTaskRoutineId>) -> Unit
  ) {
    synchronized(this) {
      // runningTasks에 이미 들어가 있을 수도 있음
      runningTasks.add(task)

      val routineId = BuildTaskRoutineId(task, nextRoutineId())
      routines[routineId] = BuildTaskRoutineBody(requires, routine)

      val requiredTasks = requires.toSet() - taskResults.keys
      if (requiredTasks.isEmpty()) {
        logger.logRequires(routineId, requires, requiredTasks)
        startTaskRoutine(routineId)
      } else {
        logger.logRequires(routineId, requires, requiredTasks)
        requiredTasks.forEach { requiring ->
          requiredBy.getOrPut(requiring) { mutableSetOf() }.add(routineId)
        }
        runTasks(requiredTasks - runningTasks)
      }
    }
  }

  fun registerTaskResult(task: BuildTask, value: Any) {
    // task의 결과가 value로 완료되었으니 task에 depend중인 태스크들을 이어서 실행
    synchronized(this) {
      taskResults[task] = value
      runningTasks.remove(task)
      // task가 완료되어야 실행될 수 있는 것들 중에 모든 requirement가 만족된 것들을 추려서 실행
      // TODO 혹시 이미 실행중인 것이 또 실행될 수 있으면 걸려야 함
      val nextTasks = requiredBy[task]?.filter { dependent ->
        val deps = routines.getValue(dependent)
        deps.requires.all { taskResults.containsKey(it) }
      }?.toSet() ?: setOf()
      if (nextTasks.isEmpty() && runningTasks.isEmpty()) {
        // 빌드 종료. 더이상 할 일도 없고 돌고있는 일도 없는 경우
        routinesQueue.add(NextRoutine.BuildFinished)
      } else {
        nextTasks.forEach { startTaskRoutine(it) }
      }
    }
  }

  // synchronized 블럭 내에서 실행되어야 함
  private fun startTaskRoutine(routineId: BuildTaskRoutineId) {
    check(!startedRoutines.contains(routineId.id))
    val routineBody = routines.remove(routineId)
    // TODO routineBody가 null일 수도 있나?
    checkNotNull(routineBody)
    startedRoutines.add(routineId.id)
    val args = routineBody.requires.map { taskResults.getValue(it) }
    logger.logRoutineAdded(routineId)
    routinesQueue.add(NextRoutine.BuildTaskRoutine(routineId) { progressIndicator ->
      logger.logRoutineStarted(routineId)
      routineBody.routine(args, progressIndicator)
      logger.logRoutineEnded(routineId)
    })
  }
}
