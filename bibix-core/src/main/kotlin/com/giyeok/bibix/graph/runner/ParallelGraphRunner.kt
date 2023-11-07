package com.giyeok.bibix.graph.runner

import com.giyeok.bibix.repo.BibixRepoProto
import com.giyeok.bibix.utils.toBibix
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.trySendBlocking
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

// graph 버전에서는 다른건 다 한 스레드에서 돌고,
// LongRunning과 SuspendLongRunning만 여러 스레드에서 분산시켜서 돌린다
// 또, build task간의 관계를 그래프(정확히는 DAG) 형태로 잘 관리해서 싸이클 감지와 추후 시각화 기능에 사용한다

// TODO 1. 모든 결과 저장하지 말고 필요한 것만 저장하도록 수정
//  - 어차피 call expr에서 target id로 long running job들은 캐싱되고 있으니 여기선 좀더 적극적으로 해도 괜찮음
// TODO 2. 지금은 중간에 태스크 하나라도 실패하면 모든 동작을 중단시키는데, 문제 없는 것들은 최대한 완료하도록 수정
//  - 그러렴 BuildTask간의 관계를 관리해야 함
class ParallelGraphRunner(
  val runner: BuildGraphRunner,
  val longRunningJobExecutor: ExecutorService,
  val jobExecutorTracker: ExecutorTracker?,
) {
  private val mainJobExecutor = Executors.newSingleThreadExecutor()
  private val longRunningJobDispatcher = longRunningJobExecutor.asCoroutineDispatcher()

  private sealed class BuildRunUpdate {
    data class Task(val task: BuildTask): BuildRunUpdate()
    data class Result(val task: BuildTask, val result: BuildTaskResult): BuildRunUpdate()
    data class Failed(val task: BuildTask, val exception: Throwable): BuildRunUpdate()
  }

  private val updateChannel = Channel<BuildRunUpdate>(Channel.UNLIMITED)

  suspend fun runTasks(vararg tasks: BuildTask): Map<BuildTask, BuildTaskResult?> =
    runTasks(tasks.toList())

  private val runningTasks = mutableSetOf<BuildTask>()
  private val resultMap = mutableMapOf<BuildTask, BuildTaskResult.FinalResult>()

  private data class DependentSingleFunc(
    val parentTask: BuildTask,
    val subTask: BuildTask,
    val func: (BuildTaskResult.FinalResult) -> BuildTaskResult
  )

  private data class DependentMultiFunc(
    val parentTask: BuildTask,
    val subTasks: List<BuildTask>,
    val func: (List<BuildTaskResult.FinalResult>) -> BuildTaskResult
  )

  private val dependentSingleFuncs = mutableListOf<DependentSingleFunc>()
  private val dependentMultiFuncs = mutableListOf<DependentMultiFunc>()
  private val activeLongRunningJobs = AtomicInteger(0)
  private val duplicateTargets = mutableMapOf<String, MutableList<BuildTask>>()

  private suspend fun processBuildTask(task: BuildTask) {
    if (task in runningTasks) {
      // 이미 실행된(현재 실행중이거나 처리되어서 결과가 있는) task는 생략
      return
    }
    runningTasks.add(task)
    try {
      val result = runner.runBuildTask(task)
      // taskResultQueue.add(Pair(nextTask, result))
      updateChannel.send(BuildRunUpdate.Result(task, result))
    } catch (e: Throwable) {
      updateChannel.send(BuildRunUpdate.Failed(task, e))
    }
  }

  private suspend fun processTaskResult(task: BuildTask, result: BuildTaskResult) {
    when (result) {
      is BuildTaskResult.FinalResult -> {
        resultMap[task] = result
      }

      is BuildTaskResult.LongRunning -> {
        activeLongRunningJobs.incrementAndGet()
        // println("Starting long running...")
        longRunningJobExecutor.execute {
          jobExecutorTracker?.notifyJobStartedFor(task)
          try {
            val funcResult = try {
              BuildRunUpdate.Result(task, result.func())
            } catch (e: Throwable) {
              BuildRunUpdate.Failed(task, e)
            }
            updateChannel.trySendBlocking(funcResult)
          } finally {
            activeLongRunningJobs.decrementAndGet()
            jobExecutorTracker?.notifyJobFinished(task)
          }
        }
      }

      is BuildTaskResult.SuspendLongRunning -> {
        activeLongRunningJobs.incrementAndGet()
        // println("Starting (suspend) long running...")
        CoroutineScope(longRunningJobDispatcher).launch {
          jobExecutorTracker?.notifySuspendJobStartedFor(task)
          try {
            val funcResult = try {
              BuildRunUpdate.Result(task, result.func())
            } catch (e: Throwable) {
              BuildRunUpdate.Failed(task, e)
            }
            updateChannel.trySendBlocking(funcResult)
          } finally {
            activeLongRunningJobs.decrementAndGet()
            jobExecutorTracker?.notifySuspendJobFinished(task)
          }
        }
      }

      is BuildTaskResult.WithResult -> {
        updateChannel.send(BuildRunUpdate.Task(result.task))
        addTaskDependency(task, result.task, result.func)
      }

      is BuildTaskResult.WithResultList -> {
        result.tasks.forEach {
          updateChannel.send(BuildRunUpdate.Task(it))
        }
        addTaskDependencies(task, result.tasks, result.func)
      }

      is BuildTaskResult.DuplicateTargetResult -> {
        duplicateTargets.getOrPut(result.targetId) { mutableListOf() }.add(task)
      }
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
    // println("$parentTask -> $subTask")
    dependentSingleFuncs.add(DependentSingleFunc(parentTask, subTask, func))
  }

  private fun addTaskDependencies(
    parentTask: BuildTask,
    subTasks: List<BuildTask>,
    func: (List<BuildTaskResult.FinalResult>) -> BuildTaskResult
  ) {
    // TODO parentTask -> subTasks 관계 저장 - 진행 상황 파악용
    // println("$parentTask ->")
    // subTasks.forEach {
    //   println("  $it")
    // }
    dependentMultiFuncs.add(DependentMultiFunc(parentTask, subTasks, func))
  }

  private suspend fun processDependentTasks() {
    val iter1 = dependentSingleFuncs.iterator()
    while (iter1.hasNext()) {
      val (parent, sub, func) = iter1.next()
      val result = resultMap[sub]
      if (result != null) {
        val funcResult = try {
          BuildRunUpdate.Result(parent, func(result))
        } catch (e: Throwable) {
          BuildRunUpdate.Failed(parent, e)
        }
        // taskResultQueue.add(parent to funcResult)
        updateChannel.send(funcResult)
        iter1.remove()
      }
    }

    val iter2 = dependentMultiFuncs.iterator()
    while (iter2.hasNext()) {
      val (parent, subs, func) = iter2.next()
      val results = subs.mapNotNull { resultMap[it] }
      if (results.size == subs.size) {
        val funcResult = try {
          BuildRunUpdate.Result(parent, func(results))
        } catch (e: Throwable) {
          BuildRunUpdate.Failed(parent, e)
        }
        // taskResultQueue.add(parent to funcResult)
        updateChannel.send(funcResult)
        iter2.remove()
      }
    }
  }

  private suspend fun processDuplicateTargets() {
    val iter = duplicateTargets.iterator()
    while (iter.hasNext()) {
      val (targetId, tasks) = iter.next()
      val targetState = runner.repo.getTargetState(targetId)
      checkNotNull(targetState)
      if (targetState.stateCase == BibixRepoProto.TargetState.StateCase.BUILD_SUCCEEDED) {
        val resultValue = targetState.buildSucceeded.resultValue.toBibix()
        val result = BuildTaskResult.ValueOfTargetResult(resultValue, targetId)
        tasks.forEach { task ->
          // taskResultQueue.add(task to result)
          updateChannel.send(BuildRunUpdate.Result(task, result))
        }
        iter.remove()
      }
    }
  }

  suspend fun runTasks(tasks: Collection<BuildTask>): Map<BuildTask, BuildTaskResult.FinalResult?> {
    val loop = CoroutineScope(mainJobExecutor.asCoroutineDispatcher()).async {
      for (update in updateChannel) {
        when (update) {
          is BuildRunUpdate.Task -> processBuildTask(update.task)
          is BuildRunUpdate.Result -> processTaskResult(update.task, update.result)
          is BuildRunUpdate.Failed -> {
            // TODO 오류도 기록하기?
            // 중간에 오류가 발생한 경우
            update.exception.printStackTrace()
            updateChannel.close()
            break
          }
        }
        processDependentTasks()
        processDuplicateTargets()
        // 작업이 모두 완료되었으면 break
        if (tasks.all { it in resultMap }) break
      }
      runner.repo.commitRepoData()
      tasks.associateWith { resultMap[it] }
    }
    // taskQueue.addAll(tasks)
    tasks.distinct().forEach {
      updateChannel.send(BuildRunUpdate.Task(it))
    }
    return loop.await()
  }
}
