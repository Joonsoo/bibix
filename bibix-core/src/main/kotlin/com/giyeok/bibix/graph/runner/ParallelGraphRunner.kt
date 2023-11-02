package com.giyeok.bibix.graph.runner

import com.giyeok.bibix.repo.BibixRepoProto
import com.giyeok.bibix.utils.toBibix
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicInteger

// graph 버전에서는 다른건 다 한 스레드에서 돌고,
// LongRunning과 SuspendLongRunning만 여러 스레드에서 분산시켜서 돌린다
// 또, build task간의 관계를 그래프(정확히는 DAG) 형태로 잘 관리해서 싸이클 감지와 추후 시각화 기능에 사용한다
class ParallelGraphRunner(val runner: BuildGraphRunner, val executor: ExecutorService) {
  private val dispatcher = executor.asCoroutineDispatcher()

  private val taskQueue = LinkedBlockingQueue<BuildTask>()
  private val taskResultQueue = LinkedBlockingQueue<Pair<BuildTask, BuildTaskResult>>()

  fun runTasks(vararg tasks: BuildTask): Map<BuildTask, BuildTaskResult> = runTasks(tasks.toSet())

  private val resultMap = mutableMapOf<BuildTask, BuildTaskResult.FinalResult>()

  data class DependentSingleFunc(
    val parentTask: BuildTask,
    val subTask: BuildTask,
    val func: (BuildTaskResult.FinalResult) -> BuildTaskResult
  )

  data class DependentMultiFunc(
    val parentTask: BuildTask,
    val subTasks: List<BuildTask>,
    val func: (List<BuildTaskResult.FinalResult>) -> BuildTaskResult
  )

  private val dependentSingleFuncs = mutableListOf<DependentSingleFunc>()
  private val dependentMultiFuncs = mutableListOf<DependentMultiFunc>()
  private val activeLongRunningJobs = AtomicInteger(0)
  private val duplicateTargets = mutableMapOf<String, MutableList<BuildTask>>()

  private fun processBuildTasks() {
    var nextTask = taskQueue.poll()
    while (nextTask != null) {
      val result = runner.runBuildTask(nextTask)
      taskResultQueue.add(Pair(nextTask, result))
      nextTask = taskQueue.poll()
    }
  }

  private fun processTaskResults() {
    var pair = taskResultQueue.poll()
    while (pair != null) {
      val (task, result) = pair
      when (result) {
        is BuildTaskResult.FinalResult -> {
          resultMap[task] = result
        }

        is BuildTaskResult.LongRunning -> {
          activeLongRunningJobs.incrementAndGet()
          executor.execute {
            try {
              val funcResult = result.func()
              taskResultQueue.add(Pair(task, funcResult))
            } finally {
              activeLongRunningJobs.decrementAndGet()
            }
          }
        }

        is BuildTaskResult.SuspendLongRunning -> {
          activeLongRunningJobs.incrementAndGet()
          CoroutineScope(dispatcher).launch {
            try {
              val funcResult = result.func()
              taskResultQueue.add(Pair(task, funcResult))
            } finally {
              activeLongRunningJobs.decrementAndGet()
            }
          }
        }

        is BuildTaskResult.WithResult -> {
          taskQueue.add(result.task)
          addTaskDependency(task, result.task, result.func)
        }

        is BuildTaskResult.WithResultList -> {
          taskQueue.addAll(result.tasks)
          addTaskDependencies(task, result.tasks, result.func)
        }

        is BuildTaskResult.DuplicateTargetResult -> {
          duplicateTargets.getOrPut(result.targetId) { mutableListOf() }.add(task)
        }
      }
      pair = taskResultQueue.poll()
    }
  }

  private fun addTaskDependency(
    parentTask: BuildTask,
    // parentTask를 실행하기 위해 실행되어야 하는 subTask
    subTask: BuildTask,
    // subTask가 실행되고 나서 그 결과와 함께 실행되어야 하는 func
    func: (BuildTaskResult.FinalResult) -> BuildTaskResult
  ) {
    // TODO parentTask -> subTask 관계 저장 - 진행 상황 파악용
    dependentSingleFuncs.add(DependentSingleFunc(parentTask, subTask, func))
  }

  private fun addTaskDependencies(
    parentTask: BuildTask,
    subTasks: List<BuildTask>,
    func: (List<BuildTaskResult.FinalResult>) -> BuildTaskResult
  ) {
    // TODO parentTask -> subTasks 관계 저장 - 진행 상황 파악용
    dependentMultiFuncs.add(DependentMultiFunc(parentTask, subTasks, func))
  }

  private fun processDependentTasks() {
    val iter1 = dependentSingleFuncs.iterator()
    while (iter1.hasNext()) {
      val (parent, sub, func) = iter1.next()
      val result = resultMap[sub]
      if (result != null) {
        val funcResult = func(result)
        taskResultQueue.add(parent to funcResult)
        iter1.remove()
      }
    }

    val iter2 = dependentMultiFuncs.iterator()
    while (iter2.hasNext()) {
      val (parent, subs, func) = iter2.next()
      val results = subs.mapNotNull { resultMap[it] }
      if (results.size == subs.size) {
        val funcResult = func(results)
        taskResultQueue.add(parent to funcResult)
        iter2.remove()
      }
    }
  }

  private fun processDuplicateTargets() {
    val iter = duplicateTargets.iterator()
    while (iter.hasNext()) {
      val (targetId, tasks) = iter.next()
      val targetState = runner.repo.getTargetState(targetId)
      checkNotNull(targetState)
      if (targetState.stateCase == BibixRepoProto.TargetState.StateCase.BUILD_SUCCEEDED) {
        val resultValue = targetState.buildSucceeded.resultValue.toBibix()
        val result = BuildTaskResult.ValueOfTargetResult(resultValue, targetId)
        tasks.forEach { task ->
          taskResultQueue.add(task to result)
        }
        iter.remove()
      }
    }
  }

  fun runTasks(tasks: Set<BuildTask>): Map<BuildTask, BuildTaskResult> {
    taskQueue.addAll(tasks)
    while (taskQueue.isNotEmpty() || taskResultQueue.isNotEmpty() ||
      dependentSingleFuncs.isNotEmpty() || dependentMultiFuncs.isNotEmpty() ||
      activeLongRunningJobs.get() > 0 ||
      duplicateTargets.isNotEmpty()
    ) {
      processBuildTasks()
      processTaskResults()
      processDependentTasks()
      processDuplicateTargets()
      // TODO 이러면 할 일 없어도 계속 돌아서 별로.. 자고 있다가 필요할 때만 일어나서 일하게 수정
    }
    return tasks.associateWith { resultMap.getValue(it) }
  }
}
