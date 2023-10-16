package com.giyeok.bibix.graph

import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.nio.file.Path

// import된 프로젝트들을 포함해서 여러 프로젝트들의 TaskGraph를 통합
class MultiTaskGraph private constructor(
  // main project는 projectId=1
  val mainProjectLocation: BibixProjectLocation,
  val mainProjectTaskGraph: TaskGraph,
  val projects: BiMap<Int, BibixProjectLocation>,
  private val projectGraphs: MutableMap<Int, TaskGraph>,
  private val importInstances: Map<Int, MutableList<ImporterId>>,
  private val edges: MutableList<MultiTaskEdge>,
) {
  constructor(mainProjectLocation: BibixProjectLocation, mainProjectTaskGraph: TaskGraph): this(
    mainProjectLocation,
    mainProjectTaskGraph,
    HashBiMap.create(mapOf(1 to mainProjectLocation)),
    mutableMapOf(1 to mainProjectTaskGraph),
    mutableMapOf(),
    mutableListOf()
  )

  private val mutex = Mutex()
  private var lastProjectId = projects.keys.max()

  private suspend fun nextProjectId(): Int = mutex.withLock {
    lastProjectId += 1
    return lastProjectId
  }

  suspend fun addNewProject(projectLocation: BibixProjectLocation) {
  }

  suspend fun linkImportInstance(importInstance: MultiTaskId) {

  }
}

data class BibixProjectLocation(val projectRoot: Path, val scriptName: String)

data class ImporterId(val importerProjectId: Int, val importInstanceTaskId: TaskId)
data class ImportInstance(val projectId: Int, val importer: ImporterId)

sealed class MultiTaskId
data class MainProjectTaskId(val projectId: Int, val taskId: TaskId): MultiTaskId()
data class ImportedProjectTaskId(val imported: ImportInstance, val taskId: TaskId): MultiTaskId()

data class MultiTaskEdge(
  val start: MultiTaskId,
  val end: MultiTaskId,
  val edgeType: MultiTaskEdgeType
)

enum class MultiTaskEdgeType {
  ValueDependency,
  OverridingValueDependency,
}
