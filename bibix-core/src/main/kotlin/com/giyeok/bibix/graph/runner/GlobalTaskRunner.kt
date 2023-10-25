package com.giyeok.bibix.graph.runner

import com.giyeok.bibix.BibixIdProto.ObjectIdData
import com.giyeok.bibix.ast.BibixAst
import com.giyeok.bibix.ast.BibixParser
import com.giyeok.bibix.base.*
import com.giyeok.bibix.graph.*
import com.giyeok.bibix.plugins.PluginInstanceProvider
import com.giyeok.bibix.plugins.PreloadedPlugin
import com.giyeok.bibix.targetIdData
import com.google.protobuf.ByteString
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.nio.file.FileSystems
import java.nio.file.Path

class GlobalTaskRunner private constructor(
  val globalGraph: GlobalTaskGraph,
  val preloadedPluginIds: Map<String, Int>,
  val preloadedPluginInstanceProviders: Map<Int, PluginInstanceProvider>,
  val callExprStates: MutableMap<GlobalTaskId, CallExprRunState>,
  val exprResults: MutableMap<GlobalTaskId, BibixValue>,
  val importInstances: MutableMap<Int, MutableList<GlobalTaskId>>,
) {
  companion object {
    suspend fun create(
      mainProjectLocation: BibixProjectLocation,
      preludePlugin: PreloadedPlugin,
      preloadedPlugins: Map<String, PreloadedPlugin>
    ): GlobalTaskRunner {
      val preludeNames = NameLookupTable.fromDefs(preludePlugin.defs).names.keys

      val mainScriptSource = mainProjectLocation.readScript()
      val mainScript = BibixParser.parse(mainScriptSource)
      val mainGraph = TaskGraph.fromScript(mainScript, preloadedPlugins.keys, preludeNames)

      val globalGraph = GlobalTaskGraph(
        mapOf(
          1 to GlobalTaskGraph.ProjectInfo(mainProjectLocation, mainScriptSource, mainGraph)
        )
      )

      val preloadedPluginInstanceProviders = mutableMapOf<Int, PluginInstanceProvider>()

      globalGraph.addProject(
        2,
        TaskGraph.fromDefs(
          preludePlugin.packageName,
          preludePlugin.defs,
          preloadedPlugins.keys,
          preludeNames,
          true
        ),
        preludePlugin.script
      )
      preloadedPluginInstanceProviders[2] = preludePlugin.pluginInstanceProvider

      var preloadedPluginIdCounter = 3
      val preloadedPluginIds = mutableMapOf<String, Int>()
      preloadedPlugins.forEach { (name, plugin) ->
        preloadedPluginIds[name] = preloadedPluginIdCounter
        globalGraph.addProject(
          preloadedPluginIdCounter,
          TaskGraph.fromDefs(
            plugin.packageName,
            plugin.defs,
            preloadedPlugins.keys,
            preludeNames,
            true
          ),
          plugin.script
        )
        preloadedPluginInstanceProviders[preloadedPluginIdCounter] = plugin.pluginInstanceProvider
        preloadedPluginIdCounter += 1
      }

      // TODO prelude와 preloaded plugin간의 연결

      return GlobalTaskRunner(
        globalGraph,
        preloadedPluginIds,
        preloadedPluginInstanceProviders,
        mutableMapOf(),
        mutableMapOf(),
        mutableMapOf()
      )
    }
  }

  private var lastProjectId = 1

  private fun nextProjectId(): Int {
    lastProjectId += 1
    return lastProjectId
  }

  val mainProjectGraph get() = globalGraph.getProjectGraph(MainProjectId.projectId)

  fun getMainProjectTaskId(taskNameTokens: List<String>): GlobalTaskId {
    when (val lookupResult = mainProjectGraph.nameLookupTable.lookupName(taskNameTokens)) {
      is NameEntryFound -> {
        when (val entry = lookupResult.entry) {
          is TargetNameEntry -> return GlobalTaskId(MainProjectId, entry.id)
          is ActionNameEntry -> TODO()
          is ActionRuleNameEntry -> TODO()
          is BuildRuleNameEntry -> TODO()
          is ClassNameEntry -> TODO()
          is EnumNameEntry -> TODO()
          is ImportNameEntry -> TODO()
          is VarNameEntry -> TODO()
        }
      }

      else -> throw IllegalStateException("Cannot find task: ${taskNameTokens.joinToString(".")}")
    }
  }

  fun getMainProjectTaskId(taskName: String): GlobalTaskId =
    getMainProjectTaskId(taskName.split('.'))

  fun getImportedProject(importNodeId: GlobalTaskId) {
    val importNode = globalGraph.getNode(importNodeId)
    check(importNode is ImportNode)
    println(importNode)
    importInstances
  }

  sealed class TaskRunResult {
    // 이 노드를 실행하기 위해 먼저 준비되어야 하는 prerequisite edge들을 반환한다.
    // 이미 그래프에 있는 엣지도 반환할 수 있으니 걸러서 사용해야 한다.
    data class UnfulfilledPrerequisites(val prerequisites: List<Pair<GlobalTaskId, TaskEdgeType>>):
      TaskRunResult()

    data class ImmediateResult(val result: NodeResult): TaskRunResult()

    data class LongRunningResult(val runner: suspend () -> NodeResult): TaskRunResult()
  }

  private val resultsMutex = Mutex()
  private val results = mutableMapOf<GlobalTaskId, NodeResult>()

  fun getResult(prjInstanceId: ProjectInstanceId, taskId: TaskId): NodeResult? =
    results[GlobalTaskId(prjInstanceId, taskId)]

  fun globalResultOrPrerequisite(
    prjInstanceId: ProjectInstanceId,
    localTaskId: TaskId,
    edgeType: TaskEdgeType
  ): TaskRunResult {
    val nodeResult = getResult(prjInstanceId, localTaskId)
    return if (nodeResult != null) {
      TaskRunResult.ImmediateResult(nodeResult)
    } else {
      TaskRunResult.UnfulfilledPrerequisites(
        listOf(GlobalTaskId(prjInstanceId, localTaskId) to edgeType)
      )
    }
  }

  fun runTask(taskId: GlobalTaskId): TaskRunResult {
    check(taskId !in results)

    val prjInstanceId = taskId.projectInstanceId

    fun resultOrPrerequisite(localTaskId: TaskId, edgeType: TaskEdgeType): TaskRunResult =
      globalResultOrPrerequisite(prjInstanceId, localTaskId, edgeType)

    val result: TaskRunResult = when (val node = globalGraph.getNode(taskId)) {
      is ExprNode<*> -> evaluateExprNode(prjInstanceId, node)

      is ImportNode -> {
        val importSourceNode = globalGraph.getNode(prjInstanceId, node.importSource)
        val importedProjectId: Int = when (importSourceNode) {
          is PreloadedPluginNode -> {
            println(importSourceNode)
            preloadedPluginIds.getValue(importSourceNode.name)
          }

          is ExprNode<*> -> {
            println(importSourceNode)
            val importSource = evaluateExprNode(prjInstanceId, importSourceNode)
            // TODO importSource는 BibixProject 값일 것 - script 읽어서 globalGraph에 추가
            println(importSource)
            123
          }

          else -> throw IllegalStateException()
        }
        TaskRunResult.ImmediateResult(NodeResult.ImportResult(importedProjectId))
      }

      is PreloadedPluginNode -> {
        val preloadedPluginProjectId = preloadedPluginIds.getValue(node.name)
        // ImportedProjectId(preloadedPluginProjectId, taskId)
        println("$node $preloadedPluginProjectId")
        // importInstances.getOrPut(preloadedPluginProjectId) { mutableListOf() }.add(taskId)
        // 여기서는 특별히 할 일이 없는 것 같은데? projectId나 넣고 끝인듯?
        TaskRunResult.ImmediateResult(NodeResult.ImportResult(preloadedPluginProjectId))
      }

      is ImportInstanceNode -> {
        val importResult = getResult(prjInstanceId, node.importNode)
        check(importResult is NodeResult.ImportResult)
        importInstances.getOrPut(importResult.projectId) { mutableListOf() }.add(taskId)
        TaskRunResult.ImmediateResult(
          NodeResult.ImportInstanceResult(
            ImportedProjectId(importResult.projectId, taskId),
            node.varRedefs.mapValues { (_, varValueTaskId) ->
              GlobalTaskId(prjInstanceId, varValueTaskId)
            })
        )
      }

      is MemberAccessNode -> {
        if (node.remainingNames.isEmpty()) {
          return resultOrPrerequisite(node.target, TaskEdgeType.ValueDependency)
        }
        when (val target = getResult(prjInstanceId, node.target)) {
          is NodeResult.ImportInstanceResult -> {
            val importedGraph = globalGraph.getProjectGraph(target.projectId)
            when (val nameLookupResult =
              importedGraph.nameLookupTable.lookupName(node.remainingNames)) {
              is NameEntryFound -> {
                globalResultOrPrerequisite(
                  target.prjInstanceId,
                  nameLookupResult.entry.id,
                  TaskEdgeType.ValueDependency
                )
              }

              else -> {
                println(nameLookupResult)
                TODO()
              }
            }
          }

          is NodeResult.ValueResult -> TODO()
          is NodeResult.TargetResult -> TODO()

          else -> TODO()
        }
      }

      is BibixTypeNode -> {
        TaskRunResult.ImmediateResult(NodeResult.TypeResult(node.bibixType))
      }

      is BuildRuleNode -> {
        val impl = getResult(prjInstanceId, node.implTarget)
        if (impl == null) {
          TaskRunResult.UnfulfilledPrerequisites(
            listOf(GlobalTaskId(prjInstanceId, node.implTarget) to TaskEdgeType.ValueDependency)
          )
        } else {
          check(impl is NodeResult.RunnableResult)
          TaskRunResult.ImmediateResult(NodeResult.BuildRuleResult(prjInstanceId, node, impl))
        }
      }

      is ClassElemCastNode -> {
        TODO()
      }

      is DataClassTypeNode -> {
        val pkg = globalGraph.projectGraphs.getValue(prjInstanceId.projectId).packageName
        checkNotNull(pkg) { "Package name is not set" }
        TaskRunResult.ImmediateResult(NodeResult.TypeResult(DataClassType(pkg, node.defNode.name)))
      }

      is EnumTypeNode -> {
        val pkg = globalGraph.projectGraphs.getValue(prjInstanceId.projectId).packageName
        checkNotNull(pkg) { "Package name is not set" }
        TaskRunResult.ImmediateResult(NodeResult.TypeResult(EnumType(pkg, node.defNode.name)))
      }

      is EnumValueNode -> {
        val typeNodeResult = getResult(prjInstanceId, node.enumTypeNode)!!
        check(typeNodeResult is NodeResult.TypeResult)
        val enumType = typeNodeResult.type
        check(enumType is EnumType)
        // TODO check node.memberName is valid name for the enum type
        TaskRunResult.ImmediateResult(
          NodeResult.ValueResult(
            EnumValue(enumType.packageName, enumType.enumName, node.memberName)
          )
        )
      }

      is NativeImplNode -> {
        val instanceProvider = preloadedPluginInstanceProviders.getValue(prjInstanceId.projectId)
        val impl = instanceProvider.getInstance(node.className)
        val method = try {
          impl::class.java.getMethod(node.methodName ?: "build", BuildContext::class.java)
        } catch (e: NoSuchMethodException) {
          throw IllegalStateException("No such method", e)
        }
        check(method.trySetAccessible()) { "Method is not accessible" }
        TaskRunResult.ImmediateResult(NodeResult.RunnableResult(impl, method))
      }

      is PreludeMemberNode -> {
        TODO()
      }

      is PreludeTaskNode -> {
        val preludeGraph = globalGraph.projectGraphs.getValue(PreludeProjectId.projectId)
        when (val lookupResult = preludeGraph.nameLookupTable.lookupName(listOf(node.name))) {
          is NameEntryFound -> {
            val entry = lookupResult.entry
            globalResultOrPrerequisite(PreludeProjectId, entry.id, TaskEdgeType.Reference)
          }

          is EnumValueFound -> TODO()
          is NameFromPrelude -> TODO()
          is NameInImport -> TODO()
          is NameNotFound -> TODO()
          is NameOfPreloadedPlugin -> TODO()
          is NamespaceFound -> TODO()
        }
      }

      is SuperClassTypeNode -> {
        TODO()
      }

      is TargetNode -> {
        println("$taskId $node")
        resultOrPrerequisite(node.valueNode, TaskEdgeType.Definition)
      }

      is CollectionTypeNode -> {
        when (val collectionName = node.collectionType.name) {
          "set", "list" -> {
            check(node.typeParams.size == 1)
            val elemTypeNode = node.typeParams.first()
            val elemType = getResult(prjInstanceId, elemTypeNode)
            if (elemType == null) {
              TaskRunResult.UnfulfilledPrerequisites(
                listOf(GlobalTaskId(prjInstanceId, elemTypeNode) to TaskEdgeType.TypeDependency)
              )
            } else {
              check(elemType is NodeResult.TypeResult)
              val type = when (collectionName) {
                "set" -> SetType(elemType.type)
                "list" -> ListType(elemType.type)
                else -> throw AssertionError()
              }
              TaskRunResult.ImmediateResult(NodeResult.TypeResult(type))
            }
          }

          else -> throw IllegalStateException("Unknown collection type: ${node.collectionType.name}")
        }
      }

      is NamedTupleTypeNode -> {
        TODO()
      }

      is TupleTypeNode -> {
        TODO()
      }

      is TypeNameNode -> {
        resultOrPrerequisite(node.typeNode, TaskEdgeType.TypeDependency)
      }

      is UnionTypeNode -> {
        val elemTypes = node.elemTypes.map { getResult(prjInstanceId, it) }
        if (elemTypes.contains(null)) {
          TaskRunResult.UnfulfilledPrerequisites(node.elemTypes.map {
            GlobalTaskId(prjInstanceId, it) to TaskEdgeType.TypeDependency
          })
        } else {
          val types = elemTypes.map { (it as NodeResult.TypeResult).type }
          TaskRunResult.ImmediateResult(NodeResult.TypeResult(UnionType(types)))
        }
      }

      is VarNode -> {
        TODO()
      }
    }
    when (result) {
      is TaskRunResult.ImmediateResult -> {
        results[taskId] = result.result
      }

      is TaskRunResult.LongRunningResult -> {
        TaskRunResult.LongRunningResult {
          val nodeResult = result.runner()
          resultsMutex.withLock {
            results[taskId] = nodeResult
          }
          nodeResult
        }
      }

      is TaskRunResult.UnfulfilledPrerequisites -> {
        // do nothing
      }
    }
    return result
  }

  fun getValResult(prjInstanceId: ProjectInstanceId, taskId: TaskId): BibixValue? {
    val result = getResult(prjInstanceId, taskId)
    return if (result == null) null else {
      check(result is NodeResult.ValueResult)
      result.value
    }
  }

  private fun v(value: BibixValue): TaskRunResult =
    TaskRunResult.ImmediateResult(NodeResult.ValueResult(value))

  // TODO
  private fun getBuildContext(args: Map<String, BibixValue>): BuildContext = BuildContext(
    BuildEnv(OS.Linux("", ""), Architecture.X86_64),
    FileSystems.getDefault(),
    Path.of(""),
    Path.of(""),
    null,
    args,
    targetIdData { },
    "",
    false,
    null,
    null,
    Path.of(""),
    object: ProgressLogger {
      override fun logInfo(message: String) {
        println(message)
      }

      override fun logError(message: String) {
        println(message)
      }
    },
    object: BaseRepo {
      override fun prepareSharedDirectory(sharedRepoName: String): Path =
        Path.of("")
    }
  )

  fun evaluateExprNode(prjInstanceId: ProjectInstanceId, node: ExprNode<*>): TaskRunResult =
    when (node) {
      is NoneLiteralNode -> v(NoneValue)
      is BooleanLiteralNode -> v(BooleanValue(node.literal.value))

      is MemberAccessExprNode -> {
        val targetResult = getResult(prjInstanceId, node.target)!!
        println(targetResult)
        val targetNode = globalGraph.getNode(prjInstanceId, node.target)
        if (node.memberNames.isEmpty()) {
          // targetNode의 실행 결과
        } else {
          // targetNode의 실행 결과에서 memberAccess
        }
        v(StringValue("$node $targetNode ${node.memberNames}"))
      }

      is StringNode -> {
        val builder = StringBuilder()
        var elemCounter = 0
        node.stringExpr.elems.forEach { elem ->
          when (elem) {
            is BibixAst.EscapeChar -> {
              // TODO escape
              builder.append("\\${elem.code}")
            }

            is BibixAst.JustChar -> builder.append(elem.chr)
            is BibixAst.ComplexExpr -> {
              val elemValue = getValResult(prjInstanceId, node.exprElems[elemCounter++])!!
              // TODO coercion
              elemValue as StringValue
            }

            is BibixAst.SimpleExpr -> {
              val elemValue = getValResult(prjInstanceId, node.exprElems[elemCounter++])
              // TODO coercion
              elemValue as StringValue
            }
          }
        }
        check(elemCounter == node.exprElems.size)
        v(StringValue(builder.toString()))
      }

      is CallExprNode -> {
        val posParams = node.posParams.map { getResult(prjInstanceId, it)!! }
        val namedArgs = node.namedParams.mapValues { (_, arg) ->
          getResult(prjInstanceId, arg)!!
        }
        val callee = getResult(prjInstanceId, node.callee)!!
        println("$callee($posParams, $namedArgs)")
        when (callee) {
          is NodeResult.BuildRuleResult -> {
            // callee의 parameter 목록을 보고 posParams와 namedParams와 맞춰본다
            val paramNames = callee.buildRuleNode.def.params.map { it.name }
            val posArgs = posParams.zip(paramNames) { arg, name -> name to arg }.toMap()

            val remainingParamNames = paramNames.drop(posParams.size).toSet()
            check(remainingParamNames.containsAll(namedArgs.keys)) { "Unknown parameters" }

            val ruleParams = callee.buildRuleNode.def.params.associateBy { it.name }
            val unspecifiedParamNames = remainingParamNames - namedArgs.keys
            check(unspecifiedParamNames.all { unspecified ->
              val param = ruleParams.getValue(unspecified)
              param.optional || param.defaultValue != null
            }) { "Required parameters are not specified" }

            // 만약 callee의 default param이 필요한데 그 값이 없으면 prerequisite으로 반환하고
            // 모든 값이 충족되었으면 TaskRunResult.LongRunningResult 로 build rule을 실행하는 코드를 반환한다
            val defaultParamTasks = unspecifiedParamNames.associateWith {
              callee.buildRuleNode.paramDefaultValues.getValue(it)
            }
            val defaults = defaultParamTasks.mapValues { (_, value) ->
              getResult(prjInstanceId, value)
            }
            val missingDefaults = defaults.filter { it.value == null }
            if (missingDefaults.isNotEmpty()) {
              TaskRunResult.UnfulfilledPrerequisites(missingDefaults.keys.map { missingParamName ->
                val defaultParamTask = defaultParamTasks.getValue(missingParamName)
                GlobalTaskId(callee.prjInstanceId, defaultParamTask) to TaskEdgeType.ValueDependency
              })
            } else {
              val defaultValues = defaults.mapValues { (_, value) -> value!! }

              val argResults: Map<String, NodeResult> = posArgs + namedArgs + defaultValues
              check(argResults.all { it.value is NodeResult.ValueResult })
              val args = argResults.mapValues { (_, result) ->
                (result as NodeResult.ValueResult).value
              }
              TaskRunResult.LongRunningResult {
                val buildContext = getBuildContext(args)
                val result =
                  callee.impl.method.invoke(callee.impl.instance, buildContext) as BibixValue
                NodeResult.ValueResult(result)
              }
            }
          }

          is NodeResult.RunnableResult -> {
            TODO()
          }

          else -> TODO()
        }
      }

      is CastExprNode -> TODO()

      is TupleNode -> {
        val elems = node.elemNodes.map { getValResult(prjInstanceId, it)!! }
        v(TupleValue(elems))
      }

      is NamedTupleNode -> {
        val elems = node.elemNodes.map { (name, valueNode) ->
          name to getValResult(prjInstanceId, valueNode)!!
        }
        v(NamedTupleValue(elems))
      }

      is ListExprNode -> {
        val elems = mutableListOf<BibixValue>()
        node.elems.forEach { elem ->
          val elemValue = getValResult(prjInstanceId, elem.valueNode)!!
          if (!elem.isEllipsis) {
            elems.add(elemValue)
          } else {
            when (elemValue) {
              is ListValue -> elems.addAll(elemValue.values)
              is SetValue -> elems.addAll(elemValue.values)
              else -> throw IllegalStateException()
            }
          }
        }
        v(ListValue(elems))
      }

      is MergeExprNode -> TODO()
      is NameRefNode -> {
        val value = getValResult(prjInstanceId, node.valueNode)
        v(value!!)
      }

      is ParenExprNode -> v(getValResult(prjInstanceId, node.body)!!)
      is ThisRefNode -> TODO()
    }
}

data class CallExprRunState(
  val objectIdData: ObjectIdData,
  val objectId: ByteString,
)
