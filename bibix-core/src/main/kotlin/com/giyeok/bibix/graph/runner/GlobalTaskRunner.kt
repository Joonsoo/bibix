package com.giyeok.bibix.graph.runner

import com.giyeok.bibix.BibixIdProto.ObjectIdData
import com.giyeok.bibix.ast.BibixAst
import com.giyeok.bibix.ast.BibixParser
import com.giyeok.bibix.base.BibixValue
import com.giyeok.bibix.base.StringValue
import com.giyeok.bibix.graph.*
import com.giyeok.bibix.plugins.PreloadedPlugin
import com.google.protobuf.ByteString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class GlobalTaskRunner private constructor(
  val preloadedPlugins: Map<String, PreloadedPlugin>,
  val preludePlugin: PreloadedPlugin,
  val globalGraph: GlobalTaskGraph,
  val mainProjectId: Int,
  val callExprStates: MutableMap<GlobalTaskId, CallExprRunState>,
  val results: MutableMap<GlobalTaskId, BibixValue>,
  val importInstances: MutableMap<Int, MutableList<ImporterId>>,
) {
  constructor(
    mainProjectLocation: BibixProjectLocation,
    mainProjectScript: BibixAst.BuildScript,
    preludePlugin: PreloadedPlugin,
    preloadedPlugins: Map<String, PreloadedPlugin>,
  ): this(
    preloadedPlugins,
    preludePlugin,
    GlobalTaskGraph(
      mapOf(
        1 to (mainProjectLocation to TaskGraph.fromScript(
          mainProjectScript,
          preloadedPlugins.keys,
          NameLookupTable.fromDefs(preludePlugin.defs).names.keys
        ))
      )
    ),
    1,
    mutableMapOf(),
    mutableMapOf(),
    mutableMapOf()
  )

  companion object {
    suspend fun create(
      mainProjectLocation: BibixProjectLocation,
      preludePlugin: PreloadedPlugin,
      preloadedPlugins: Map<String, PreloadedPlugin>
    ): GlobalTaskRunner {
      val mainScript = BibixParser.parse(mainProjectLocation.readScript())
      return GlobalTaskRunner(mainProjectLocation, mainScript, preludePlugin, preloadedPlugins)
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

  private val mutex = Mutex()
  private val evalResults = mutableMapOf<GlobalTaskId, MutableStateFlow<BibixValue?>>()

  suspend fun getNodeResult(nodeId: GlobalTaskId): BibixValue = mutex.withLock {
    val flow = evalResults[nodeId]
    checkNotNull(flow)
    flow.value ?: throw IllegalStateException()
  }

  suspend fun evaluateNode(nodeId: GlobalTaskId): BibixValue =
    when (val node = globalGraph.getNode(nodeId)) {
      is MemberAccessNode -> {
        StringValue("")
      }

      is PreloadedPluginNode -> {
        StringValue("")
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
              val elemValue =
                getNodeResult(GlobalTaskId(nodeId.projectInstanceId, node.exprElems[elemCounter++]))
              elemValue as StringValue
            }

            is BibixAst.SimpleExpr -> {
              val elemValue =
                getNodeResult(GlobalTaskId(nodeId.projectInstanceId, node.exprElems[elemCounter++]))
              elemValue as StringValue
            }
          }
        }
        check(elemCounter == node.exprElems.size)
        StringValue(builder.toString())
      }

      is PreludeTaskNode -> {
        preludePlugin.defs
        StringValue("TODO")
      }

      else -> {
        StringValue("TODO")
      }
    }

  suspend fun memoEvaluateNode(nodeId: GlobalTaskId): BibixValue {
    // TODO literal같은 trivial value들은 굳이 memoize 하지 말자
    val memo: MutableStateFlow<BibixValue?>
    val toEvaluate: Boolean
    mutex.withLock {
      val memoFlow = evalResults[nodeId]
      if (memoFlow == null) {
        memo = MutableStateFlow(null)
        evalResults[nodeId] = memo
        toEvaluate = true
      } else {
        memo = memoFlow
        toEvaluate = false
      }
    }
    return if (toEvaluate) {
      val result = evaluateNode(nodeId)
      memo.value = result
      result
    } else {
      memo.first { it != null }!!
    }
  }

  suspend fun resolveImport(importNodeId: GlobalTaskId) {
    val importNode = globalGraph.getNode(importNodeId)
    check(importNode is ImportNode)
    val sourceNodeId = when (val importDef = importNode.import) {
      is BibixAst.ImportAll -> {
        TaskId(importDef.source.nodeId)
      }

      is BibixAst.ImportFrom -> {
        TaskId(importDef.source.nodeId)
      }
    }
    val sourceNode = globalGraph.getNode(
      GlobalTaskId(importNodeId.projectInstanceId, sourceNodeId)
    )
    println(sourceNode)
  }

  // import node id -> set<import instance node id>
  fun findRequiredImportsFor(tasks: Set<GlobalTaskId>): Map<GlobalTaskId, Set<GlobalTaskId>> {
    val result = mutableMapOf<GlobalTaskId, MutableSet<GlobalTaskId>>()
    // TODO 이렇게 하면 같은 프로젝트 그래프를 여러번 돌 수도 있는데?
    // TODO MultiTaskRunner 의 edges 를 고려해야 함
    val reachableNodes = globalGraph.reachableNodesFrom(tasks)
    val importedTasks =
      reachableNodes.filter { globalGraph.getNode(it) is ImportedTaskNode }
    val importInstances =
      reachableNodes.filter { globalGraph.getNode(it) is ImportInstanceNode }
    importInstances.forEach { importInstance ->
      val edges = globalGraph.edgesByStart(importInstance)
        .filter { it.edgeType == TaskEdgeType.ImportInstance }
      check(edges.size == 1)
      val importNode = edges.first().end
      check(globalGraph.getNode(importNode) is ImportNode)

      result.getOrPut(importNode) { mutableSetOf() }.add(importInstance)
    }
    return result
  }
}

data class CallExprRunState(
  val objectIdData: ObjectIdData,
  val objectId: ByteString,
)
