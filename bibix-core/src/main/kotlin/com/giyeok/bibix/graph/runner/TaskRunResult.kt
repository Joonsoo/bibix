package com.giyeok.bibix.graph.runner

import com.giyeok.bibix.graph.TaskEdgeType

sealed class TaskRunResult {
  // 이 노드를 실행하기 위해 먼저 준비되어야 하는 prerequisite edge들을 반환한다.
  // 이미 그래프에 있는 엣지도 반환할 수 있으니 걸러서 사용해야 한다.
  data class UnfulfilledPrerequisites(val prerequisites: List<Pair<GlobalTaskId, TaskEdgeType>>):
    TaskRunResult()

  data class BibixProjectImportRequired(
    val projectLocation: BibixProjectLocation,
    // param: import된 project id
    val afterImport: (Int) -> Unit
  ): TaskRunResult()

  data class ImmediateResult(val result: NodeResult): TaskRunResult()

  data class LongRunningResult(val runner: suspend () -> TaskRunResult): TaskRunResult()
}
