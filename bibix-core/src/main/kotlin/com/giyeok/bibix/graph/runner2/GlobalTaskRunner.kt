package com.giyeok.bibix.graph.runner2

import com.giyeok.bibix.ast.BibixParser
import com.giyeok.bibix.base.BibixValue
import com.giyeok.bibix.base.BuildEnv
import com.giyeok.bibix.base.ClassInstanceValue
import com.giyeok.bibix.graph.*
import com.giyeok.bibix.graph.runner.ClassPkgRunner
import com.giyeok.bibix.plugins.PluginInstanceProvider
import com.giyeok.bibix.plugins.PreloadedPlugin
import com.giyeok.bibix.repo.BibixRepo
import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import org.codehaus.plexus.classworlds.ClassWorld

class GlobalTaskRunner(
  val globalGraph: GlobalTaskGraph,
  val preloadedPluginIds: BiMap<String, Int>,
  val preloadedPluginInstanceProviders: Map<Int, PluginInstanceProvider>,
  val preludeNames: Set<String>,
  val buildEnv: BuildEnv,
  val repo: BibixRepo,
  val classPkgRunner: ClassPkgRunner,
) {
  companion object {
    val mainProjectId = 1
    val preludeProjectId = 2

    suspend fun create(
      mainProjectLocation: BibixProjectLocation,
      preludePlugin: PreloadedPlugin,
      preloadedPlugins: Map<String, PreloadedPlugin>,
      buildEnv: BuildEnv,
      repo: BibixRepo,
      classWorld: ClassWorld,
    ): GlobalTaskRunner {
      val preludeNames = NameLookupTable.fromDefs(preludePlugin.defs).names.keys

      val mainScriptSource = mainProjectLocation.readScript()
      val mainScript = BibixParser.parse(mainScriptSource)
      val mainGraph = TaskGraph.fromScript(mainScript, preloadedPlugins.keys, preludeNames)

      val globalGraph = GlobalTaskGraph(
        mapOf(
          mainProjectId to GlobalTaskGraph.ProjectInfo(
            mainProjectLocation,
            mainScriptSource,
            mainGraph
          )
        )
      )

      val preloadedPluginInstanceProviders = mutableMapOf<Int, PluginInstanceProvider>()

      globalGraph.addProject(
        preludeProjectId,
        TaskGraph.fromDefs(
          packageName = preludePlugin.packageName,
          defs = preludePlugin.defs,
          preloadedPluginNames = preloadedPlugins.keys,
          preludeNames = preludeNames,
          nativeAllowed = true
        ),
        preludePlugin.script
      )
      preloadedPluginInstanceProviders[preludeProjectId] = preludePlugin.pluginInstanceProvider

      var preloadedPluginIdCounter = 3
      val preloadedPluginIds = mutableMapOf<String, Int>()
      preloadedPlugins.forEach { (name, plugin) ->
        preloadedPluginIds[name] = preloadedPluginIdCounter
        globalGraph.addProject(
          preloadedPluginIdCounter,
          TaskGraph.fromDefs(
            packageName = plugin.packageName,
            defs = plugin.defs,
            preloadedPluginNames = preloadedPlugins.keys,
            preludeNames = preludeNames,
            nativeAllowed = true
          ),
          plugin.script
        )
        preloadedPluginInstanceProviders[preloadedPluginIdCounter] = plugin.pluginInstanceProvider
        preloadedPluginIdCounter += 1
      }

      // TODO prelude와 preloaded plugin간의 연결

      return GlobalTaskRunner(
        globalGraph = globalGraph,
        preloadedPluginIds = HashBiMap.create(preloadedPluginIds),
        preloadedPluginInstanceProviders = preloadedPluginInstanceProviders,
        preludeNames = preludeNames,
        buildEnv = buildEnv,
        repo = repo,
        classPkgRunner = ClassPkgRunner(classWorld)
      )
    }
  }

  val mainProjectGraph get() = globalGraph.getProjectGraph(mainProjectId)

  // project id -> value
  val results = mutableMapOf<Int, MutableMap<TaskId, NodeResult>>()

  // project id -> (context id -> project instance context)
  val contexts = mutableMapOf<Int, MutableMap<Int, TaskContext>>()
  val csResults = mutableMapOf<TaskContextId, MutableMap<TaskId, NodeResult>>()

  fun getContext(cid: TaskContextId): TaskContext =
    contexts[cid.projectId]!![cid.contextId]!!

  fun getResult(gid: GlobalTaskId): NodeResult? = getResult(gid.contextId, gid.taskId)

  fun getResult(contextId: TaskContextId, taskId: TaskId): NodeResult? =
    (results[contextId.projectId]?.get(taskId)) ?: (csResults[contextId]?.get(taskId))

  // gid의 결과가 context free이면 true, 아니면 false
  // gid의 결과가 있는 경우에만 사용 가능. 없으면 IllegalStateException
  fun isContextFreeResult(gid: GlobalTaskId): Boolean {
    if (results[gid.contextId.projectId]?.get(gid.taskId) != null) {
      return true
    }
    check(csResults[gid.contextId]?.containsKey(gid.taskId) ?: false)
    return false
  }

  fun getValueResult(contextId: TaskContextId, taskId: TaskId): NodeResult.ValueResult? {
    val result = getResult(contextId, taskId)
    return if (result != null) {
      check(result is NodeResult.ValueResult)
      result
    } else {
      null
    }
  }

  fun valueResult(gid: GlobalTaskId, result: NodeResult.ValueResult): TaskRunResult {
    if (result.isContextFree) {
      results.getOrPut(gid.contextId.projectId) { mutableMapOf() }[gid.taskId] = result
    } else {
      csResults.getOrPut(gid.contextId) { mutableMapOf() }[gid.taskId] = result
    }
    return TaskRunResult.ImmediateResult(result)
  }

  fun valueResult(gid: GlobalTaskId, isContextFree: Boolean, value: BibixValue): TaskRunResult =
    valueResult(gid, NodeResult.ValueResult(value, isContextFree))

  fun cfValueResult(gid: GlobalTaskId, value: BibixValue): TaskRunResult =
    valueResult(gid, NodeResult.ValueResult(value, true))

  fun csValueResult(gid: GlobalTaskId, value: BibixValue): TaskRunResult =
    valueResult(gid, NodeResult.ValueResult(value, false))

  fun withResult(
    contextId: TaskContextId,
    taskId: TaskId,
    func: (NodeResult) -> TaskRunResult
  ): TaskRunResult {
    val result = getResult(contextId, taskId)
    return if (result == null) {
      TaskRunResult.UnfulfilledPrerequisites(
        listOf(GlobalTaskId(contextId, taskId) to TaskEdgeType.ValueDependency)
      )
    } else {
      func(result)
    }
  }

  fun withResults(
    contextId: TaskContextId,
    taskIds: Set<TaskId>,
    func: (Map<TaskId, NodeResult>) -> TaskRunResult
  ): TaskRunResult {
    TODO()
  }

  fun withValueResult(
    contextId: TaskContextId,
    taskId: TaskId,
    func: (NodeResult.ValueResult) -> TaskRunResult
  ): TaskRunResult = withResult(contextId, taskId) { result ->
    check(result is NodeResult.ValueResult)
    func(result)
  }

  fun runTask(gid: GlobalTaskId): TaskRunResult =
    when (val node = globalGraph.getNode(gid.contextId.projectId, gid.taskId)) {
      is ExprNode<*> -> runExprTask(gid, node)
      is ActionDefNode -> TODO()
      is ActionRuleDefNode -> TODO()
      is BibixTypeNode -> TODO()
      is BuildRuleNode -> TODO()
      is CallExprParamCoercionNode -> TODO()
      is ClassElemCastNode -> TODO()
      is DataClassTypeNode -> TODO()
      is EnumTypeNode -> TODO()
      is EnumValueNode -> TODO()
      is ImportInstanceNode -> {
        TODO()
      }
      is ImportNode -> TODO()
      is MemberAccessNode -> TODO()
      is NativeImplNode -> TODO()
      is PreloadedPluginNode -> TODO()
      is PreludeMemberNode -> TODO()
      is PreludeTaskNode -> TODO()
      is SuperClassTypeNode -> TODO()
      is TargetNode -> TODO()
      is CollectionTypeNode -> TODO()
      is NamedTupleTypeNode -> TODO()
      is TupleTypeNode -> TODO()
      is TypeNameNode -> TODO()
      is UnionTypeNode -> TODO()
      is ValueCoercionNode -> TODO()
      is VarNode -> {
        val context = getContext(gid.contextId)
        val redef = context.varRedefs[node.name]
        if (redef != null) {
          TODO()
        } else {
          TODO()
        }
      }
    }
}

data class TaskContext(
  val varRedefs: Map<String, GlobalTaskId>,
  val thisValue: ClassInstanceValue?
)
