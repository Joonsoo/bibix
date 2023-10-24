package com.giyeok.bibix.graph.runner

import com.giyeok.bibix.BibixIdProto.ObjectIdData
import com.giyeok.bibix.ast.BibixAst
import com.giyeok.bibix.ast.BibixParser
import com.giyeok.bibix.base.*
import com.giyeok.bibix.graph.*
import com.giyeok.bibix.plugins.PreloadedPlugin
import com.google.protobuf.ByteString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class GlobalTaskRunner private constructor(
  val globalGraph: GlobalTaskGraph,
  val mainProjectId: Int,
  val preludeProjectId: Int,
  val preloadedPluginIds: Map<String, Int>,
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

      globalGraph.addProject(
        2,
        TaskGraph.fromDefs(preludePlugin.defs, preloadedPlugins.keys, preludeNames, true),
        preludePlugin.script
      )

      var preloadedPluginIdCounter = 3
      val preloadedPluginIds = mutableMapOf<String, Int>()
      preloadedPlugins.forEach { (name, plugin) ->
        preloadedPluginIds[name] = preloadedPluginIdCounter
        globalGraph.addProject(
          preloadedPluginIdCounter,
          TaskGraph.fromDefs(plugin.defs, preloadedPlugins.keys, preludeNames, true),
          plugin.script
        )
        preloadedPluginIdCounter += 1
      }

      // TODO prelude와 preloaded plugin간의 연결

      return GlobalTaskRunner(
        globalGraph, 1, 2, preloadedPluginIds, mutableMapOf(), mutableMapOf(), mutableMapOf()
      )
    }
  }

  private var lastProjectId = 1

  private fun nextProjectId(): Int {
    lastProjectId += 1
    return lastProjectId
  }

  val mainProjectGraph get() = globalGraph.getProject(mainProjectId)

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

  val results = mutableMapOf<GlobalTaskId, NodeResult>()

  fun getResult(prjInstanceId: ProjectInstanceId, taskId: TaskId): NodeResult? =
    results[GlobalTaskId(prjInstanceId, taskId)]

  suspend fun runTask(taskId: GlobalTaskId): TaskRunResult {
    val prjInstanceId = taskId.projectInstanceId
    when (val node = globalGraph.getNode(taskId)) {
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
        results[taskId] = ImportResult(importedProjectId)
        // resolveImport(taskId.projectInstanceId, node)
        println(node)
      }

      is ImportInstanceNode -> {
        val importResult = getResult(prjInstanceId, node.importNode)
        check(importResult is ImportResult)
        results[taskId] = ImportInstanceResult(
          importResult.projectId,
          node.varRedefs.mapValues { (_, varValueTaskId) ->
            GlobalTaskId(prjInstanceId, varValueTaskId)
          })
        importInstances.getOrPut(importResult.projectId) { mutableListOf() }.add(taskId)
      }

      is ImportedTaskNode -> {
        val importInstance = getResult(prjInstanceId, node.importInstanceNode)
        check(importInstance is ImportInstanceResult)
      }

      is PreloadedPluginNode -> {
        val preloadedPluginProjectId = preloadedPluginIds.getValue(node.name)
        // ImportedProjectId(preloadedPluginProjectId, taskId)
        println("$node $preloadedPluginProjectId")
        results[taskId] = ImportResult(preloadedPluginProjectId)
        // importInstances.getOrPut(preloadedPluginProjectId) { mutableListOf() }.add(taskId)
        // 여기서는 특별히 할 일이 없는 것 같은데? projectId나 넣고 끝인듯?
      }

//      is PreloadedPluginInstanceNode -> {
//        val preloadedPlugin =
//          globalGraph.getNode(taskId.projectInstanceId, node.preloadedPluginNode)
//        println("$node $preloadedPlugin")
//      }

      is MemberAccessNode -> {}
      is BibixTypeNode -> {}
      is BuildRuleNode -> {}
      is ClassElemCastNode -> {}
      is DataClassTypeNode -> {}
      is EnumTypeNode -> {}
      is EnumValueNode -> {}
      is NativeImplNode -> {}
      is PreludeMemberNode -> {}

      is PreludeTaskNode -> {
        val preludeGraph = globalGraph.projectGraphs.getValue(preludeProjectId)
        when (val lookupResult = preludeGraph.nameLookupTable.lookupName(listOf(node.name))) {
          is NameEntryFound -> {
            val entry = lookupResult.entry
            val nodeResult = getResult(prjInstanceId, entry.id)
            return if (nodeResult != null) {
              TaskRunResult.ImmediateResult(nodeResult)
            } else {
              TaskRunResult.UnfulfilledPrerequisites(
                listOf(GlobalTaskId(prjInstanceId, entry.id) to TaskEdgeType.Reference)
              )
            }
          }

          is EnumValueFound -> TODO()
          is NameFromPrelude -> TODO()
          is NameInImport -> TODO()
          is NameNotFound -> TODO()
          is NameOfPreloadedPlugin -> TODO()
          is NamespaceFound -> TODO()
        }
      }

      is SuperClassTypeNode -> {}
      is TargetNode -> {}
      is CollectionTypeNode -> {}
      is NamedTupleTypeNode -> {}
      is TupleTypeNode -> {}
      is TypeNameNode -> {}
      is UnionTypeNode -> {}
      is VarNode -> {}
    }
    // TODO
    return TaskRunResult.ImmediateResult(BuildRuleResult())
  }

  private val mutex = Mutex()
  private val evalResults = mutableMapOf<GlobalTaskId, MutableStateFlow<BibixValue?>>()

  suspend fun getNodeResult(nodeId: GlobalTaskId): BibixValue = mutex.withLock {
    val flow = evalResults[nodeId]
    checkNotNull(flow)
    flow.value ?: throw IllegalStateException()
  }

  suspend fun getNodeResult(projectInstanceId: ProjectInstanceId, taskId: TaskId): BibixValue =
    getNodeResult(GlobalTaskId(projectInstanceId, taskId))

  suspend fun evaluateExprNode(prjInstanceId: ProjectInstanceId, node: ExprNode<*>): BibixValue =
    when (node) {
      is NoneLiteralNode -> NoneValue
      is BooleanLiteralNode -> BooleanValue(node.literal.value)

      is MemberAccessExprNode -> {
        val targetNode = globalGraph.getNode(prjInstanceId, node.target)
        if (node.memberNames.isEmpty()) {
          // targetNode의 실행 결과
        } else {
          // targetNode의 실행 결과에서 memberAccess
        }
        StringValue("$node $targetNode ${node.memberNames}")
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
              val elemValue = getNodeResult(prjInstanceId, node.exprElems[elemCounter++])
              elemValue as StringValue
            }

            is BibixAst.SimpleExpr -> {
              val elemValue = getNodeResult(prjInstanceId, node.exprElems[elemCounter++])
              elemValue as StringValue
            }
          }
        }
        check(elemCounter == node.exprElems.size)
        StringValue(builder.toString())
      }

      is CallExprNode -> {
        StringValue("TODO")
      }

      is CastExprNode -> TODO()

      is TupleNode -> {
        val elems = node.elemNodes.map { getNodeResult(prjInstanceId, it) }
        TupleValue(elems)
      }

      is NamedTupleNode -> {
        val elems = node.elemNodes.map { (name, valueNode) ->
          name to getNodeResult(prjInstanceId, valueNode)
        }
        NamedTupleValue(elems)
      }

      is ListExprNode -> {
        val elems = mutableListOf<BibixValue>()
        node.elems.forEach { elem ->
          val elemValue = getNodeResult(prjInstanceId, elem.valueNode)
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
        ListValue(elems)
      }

      is MergeExprNode -> TODO()
      is NameRefNode -> getNodeResult(prjInstanceId, node.valueNode)
      is ParenExprNode -> getNodeResult(prjInstanceId, node.body)
      is ThisRefNode -> TODO()
    }
//
//  suspend fun memoEvaluateNode(nodeId: GlobalTaskId): BibixValue {
//    // TODO literal같은 trivial value들은 굳이 memoize 하지 말자
//    val memo: MutableStateFlow<BibixValue?>
//    val toEvaluate: Boolean
//    mutex.withLock {
//      val memoFlow = evalResults[nodeId]
//      if (memoFlow == null) {
//        memo = MutableStateFlow(null)
//        evalResults[nodeId] = memo
//        toEvaluate = true
//      } else {
//        memo = memoFlow
//        toEvaluate = false
//      }
//    }
//    return if (toEvaluate) {
//      val result = evaluateExprNode(nodeId)
//      memo.value = result
//      result
//    } else {
//      memo.first { it != null }!!
//    }
//  }
//
//  suspend fun resolveImport(importNodeId: GlobalTaskId) {
//    val importNode = globalGraph.getNode(importNodeId)
//    check(importNode is ImportNode)
//    val sourceNodeId = when (val importDef = importNode.import) {
//      is BibixAst.ImportAll -> {
//        TaskId(importDef.source.nodeId)
//      }
//
//      is BibixAst.ImportFrom -> {
//        TaskId(importDef.source.nodeId)
//      }
//    }
//    val sourceNode = globalGraph.getNode(
//      GlobalTaskId(importNodeId.projectInstanceId, sourceNodeId)
//    )
//    println(sourceNode)
//  }
}

data class CallExprRunState(
  val objectIdData: ObjectIdData,
  val objectId: ByteString,
)
