package com.giyeok.bibix.graph

// import된 프로젝트들을 포함해서 여러 프로젝트들의 TaskGraph를 통합
class MultiTaskGraph(
  // main project는 projectId=1 번으로 본다
  val mainProjectGraph: TaskGraph,
  private val importedProjectGraphs: MutableMap<Int, TaskGraph>,
  private val edges: List<MultiTaskEdge>,
) {
}

data class ImportInstance(
  val importerProjectId: Int,
  val importedProjectId: Int,
  val importInstanceTaskId: TaskId,
)

sealed class MultiTaskId
data class MainProjectTaskId(val taskId: TaskId): MultiTaskId()
data class ImportedProjectTaskId(val imported: ImportInstance, val taskId: TaskId): MultiTaskId()

data class MultiTaskEdge(val start: MultiTaskId, val end: MultiTaskId, val edgeType: MultiTaskEdgeType)

enum class MultiTaskEdgeType {
  ValueDependency,
  OverridingValueDependency,
}
