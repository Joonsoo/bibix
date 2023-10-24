package com.giyeok.bibix.graph.runner

sealed class NodeRunState

class ImportRunState: NodeRunState()


sealed class NodeResult

class BuildRuleResult: NodeResult()

class NativeImplResult: NodeResult()

data class ImportResult(val projectId: Int): NodeResult()

data class ImportInstanceResult(val projectId: Int, val varRedefs: Map<String, GlobalTaskId>):
  NodeResult()

class PreloadedPluginResult: NodeResult()

class TargetResult: NodeResult()

class VarResult: NodeResult()

class RunnableResult: NodeResult()
