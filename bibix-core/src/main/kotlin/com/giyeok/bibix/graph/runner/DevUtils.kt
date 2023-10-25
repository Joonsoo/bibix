package com.giyeok.bibix.graph.runner

import com.giyeok.bibix.graph.TaskId
import kotlin.math.absoluteValue

fun TaskId.toNodeId(projectInstanceId: ProjectInstanceId?): String =
  if (projectInstanceId == null) {
    this.toNodeId()
  } else {
    GlobalTaskId(projectInstanceId, this).toNodeId()
  }

fun TaskId.toNodeId(): String =
  if (this.additionalId == null) "n${this.nodeId}" else "n${this.nodeId}_${this.additionalId.hashCode().absoluteValue}"

fun GlobalTaskId.toNodeId(): String {
  val projectInstanceId = when (val instanceId = this.projectInstanceId) {
    is MainProjectId -> "main_${instanceId.projectId}"
    is PreludeProjectId -> "prelude_${instanceId.projectId}"
    is ImportedProjectId -> {
      val importer = instanceId.importer
      val importerDesc = "${importer.projectInstanceId.toNodeId()}_${importer.taskId.toNodeId()}"
      "imp_${instanceId.projectId}_${instanceId.projectId}_$importerDesc"
    }
  }
  return "g_${projectInstanceId}_${this.taskId.toNodeId()}"
}

fun ProjectInstanceId.toNodeId(): String = when (this) {
  is ImportedProjectId -> TODO()
  is MainProjectId -> "main"
  is PreludeProjectId -> "prelude"
}
