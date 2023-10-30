package com.giyeok.bibix.graph.runner2

import com.giyeok.bibix.BibixIdProto
import com.giyeok.bibix.base.BibixType
import com.giyeok.bibix.base.BibixValue
import com.giyeok.bibix.base.DataClassType
import com.giyeok.bibix.graph.BuildRuleNode
import com.giyeok.bibix.graph.TaskId
import com.giyeok.bibix.plugins.PluginInstanceProvider
import java.lang.reflect.Method

sealed class NodeResult {
  data class BuildRuleResult(
    val contextId: TaskContextId,
    val buildRuleNode: BuildRuleNode,
    val params: List<Pair<String, BibixType>>,
    val returnType: BibixType,
    val implInstance: Any,
    val implMethod: Method,
    val buildRuleData: BibixIdProto.BuildRuleData,
  ): NodeResult()

  data class ImportResult(val projectId: Int, val importName: List<String>?): NodeResult()

  data class ImportInstanceResult(
    val contextId: TaskContextId,
    val importName: List<String>?,
  ): NodeResult()

  class PreloadedPluginResult: NodeResult()

  data class ValueResult(val value: BibixValue, val isContextFree: Boolean): NodeResult()

  data class PluginInstanceProviderResult(val provider: PluginInstanceProvider): NodeResult()

  open class TypeResult(val type: BibixType): NodeResult()

  class DataClassTypeResult(
    val contextId: TaskContextId,
    val packageName: String,
    val className: String,
    val fields: List<Pair<String, BibixType>>,
    val optionalFields: Set<String>,
    val defaultValues: Map<String, TaskId>,
    val classBodyElems: List<TaskId>,
  ): TypeResult(DataClassType(packageName, className))
}
