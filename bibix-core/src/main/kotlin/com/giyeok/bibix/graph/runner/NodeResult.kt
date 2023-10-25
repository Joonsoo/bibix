package com.giyeok.bibix.graph.runner

import com.giyeok.bibix.base.BibixType
import com.giyeok.bibix.base.BibixValue
import com.giyeok.bibix.graph.BuildRuleNode

sealed class NodeRunState

class ImportRunState: NodeRunState()


sealed class NodeResult {
  data class BuildRuleResult(
    // 이 build rule이 정의된 project instance id
    val prjInstanceId: ProjectInstanceId,
    val buildRuleNode: BuildRuleNode
  ): NodeResult()

  class NativeImplResult: NodeResult()

  data class ImportResult(val projectId: Int): NodeResult()

  data class ImportInstanceResult(
    val prjInstanceId: ProjectInstanceId,
    val varRedefs: Map<String, GlobalTaskId>
  ): NodeResult() {
    val projectId get() = prjInstanceId.projectId
  }

  class PreloadedPluginResult: NodeResult()

  class TargetResult: NodeResult()

  class ValueResult(val value: BibixValue): NodeResult()

  class VarResult: NodeResult()

  class RunnableResult: NodeResult()

  class TypeResult(val type: BibixType): NodeResult()
}
