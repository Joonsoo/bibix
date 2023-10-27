package com.giyeok.bibix.graph.runner

import com.giyeok.bibix.BibixIdProto.BuildRuleData
import com.giyeok.bibix.base.BibixType
import com.giyeok.bibix.base.BibixValue
import com.giyeok.bibix.base.DataClassType
import com.giyeok.bibix.graph.BuildRuleNode
import com.giyeok.bibix.graph.TaskId
import java.lang.reflect.Method

sealed class NodeRunState

class ImportRunState: NodeRunState()


sealed class NodeResult {
  data class BuildRuleResult(
    // 이 build rule이 정의된 project instance id
    val prjInstanceId: ProjectInstanceId,
    val buildRuleNode: BuildRuleNode,
    val params: List<Pair<String, BibixType>>,
    val impl: RunnableResult,
    val buildRuleData: BuildRuleData,
  ): NodeResult()

  data class ImportResult(val projectId: Int): NodeResult()

  data class ImportInstanceResult(
    val prjInstanceId: ProjectInstanceId,
    val varRedefs: Map<String, GlobalTaskId>
  ): NodeResult() {
    val projectId get() = prjInstanceId.projectId
  }

  class PreloadedPluginResult: NodeResult()

  class ValueResult(val value: BibixValue): NodeResult()

  data class RunnableResult(val instance: Any, val method: Method): NodeResult()

  open class TypeResult(val type: BibixType): NodeResult()

  class DataClassTypeResult(
    val prjInstanceId: ProjectInstanceId,
    val packageName: String,
    val className: String,
    val fields: List<Pair<String, BibixType>>,
    val defaultValues: Map<String, TaskId>,
    val classBodyElems: List<TaskId>,
  ): TypeResult(DataClassType(packageName, className))
}
