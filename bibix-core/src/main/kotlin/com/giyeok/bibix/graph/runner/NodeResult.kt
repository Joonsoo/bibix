package com.giyeok.bibix.graph.runner

import com.giyeok.bibix.BibixIdProto.BuildRuleData
import com.giyeok.bibix.base.BibixType
import com.giyeok.bibix.base.BibixValue
import com.giyeok.bibix.base.DataClassType
import com.giyeok.bibix.graph.BuildRuleNode
import com.giyeok.bibix.graph.TaskId
import com.giyeok.bibix.plugins.PluginInstanceProvider
import java.lang.reflect.Method

sealed class NodeResult {
  data class BuildRuleResult(
    // 이 build rule이 정의된 project instance id
    val prjInstanceId: ProjectInstanceId,
    val buildRuleNode: BuildRuleNode,
    val params: List<Pair<String, BibixType>>,
    val returnType: BibixType,
    val implInstance: Any,
    val implMethod: Method,
    val buildRuleData: BuildRuleData,
  ): NodeResult()

  data class ImportResult(val projectId: Int, val importName: List<String>?): NodeResult()

  data class ImportInstanceResult(
    val prjInstanceId: ProjectInstanceId,
    val varRedefs: Map<String, GlobalTaskId>,
    val importName: List<String>?,
  ): NodeResult() {
    val projectId get() = prjInstanceId.projectId
  }

  class PreloadedPluginResult: NodeResult()

  class ValueResult(val value: BibixValue): NodeResult()

  data class PluginInstanceProviderResult(val provider: PluginInstanceProvider): NodeResult()

  open class TypeResult(val type: BibixType): NodeResult()

  class DataClassTypeResult(
    val prjInstanceId: ProjectInstanceId,
    val packageName: String,
    val className: String,
    val fields: List<Pair<String, BibixType>>,
    val optionalFields: Set<String>,
    val defaultValues: Map<String, TaskId>,
    val classBodyElems: List<TaskId>,
  ): TypeResult(DataClassType(packageName, className))
}
