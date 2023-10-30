package com.giyeok.bibix.graph.runner2

import com.giyeok.bibix.*
import com.giyeok.bibix.base.*
import com.giyeok.bibix.graph.CallExprCallNode
import com.giyeok.bibix.graph.TaskEdgeType
import com.giyeok.bibix.graph.TaskId
import com.giyeok.bibix.graph.runner2.GlobalTaskRunner.Companion.mainProjectId
import com.giyeok.bibix.graph.runner2.GlobalTaskRunner.Companion.preludeProjectId
import com.giyeok.bibix.repo.TargetId
import com.giyeok.bibix.repo.extractInputHashes
import com.giyeok.bibix.repo.hashString
import com.giyeok.bibix.utils.toBibix
import com.giyeok.bibix.utils.toInstant
import com.giyeok.bibix.utils.toProto
import com.google.protobuf.empty
import java.nio.file.FileSystems
import kotlin.io.path.absolutePathString

fun GlobalTaskRunner.evaluateCallExprCallNode(
  gid: GlobalTaskId,
  node: CallExprCallNode
): TaskRunResult {
  val posArgs = node.posParams.map { getResult(gid.contextId, it)!! }
  val namedArgs = node.namedParams.mapValues { (_, arg) ->
    getResult(gid.contextId, arg)!!
  }
  return when (val callee = getResult(gid.contextId, node.callee)!!) {
    is NodeResult.BuildRuleResult -> {
      val params = callee.buildRuleNode.def.params
      val requiredParamNames =
        params.filter { !it.optional && it.defaultValue == null }.map { it.name }.toSet()
      organizeParams(
        paramTypes = callee.params,
        requiredParamNames = requiredParamNames,
        paramDefaultValueTasks = callee.buildRuleNode.paramDefaultValues,
        posArgs = posArgs,
        namedArgs = namedArgs,
        calleeContextId = callee.contextId,
      ) { args, isContextFree ->
//        callBuildRule(gid.contextId, node.id, callee, args)
        TODO()
      }
    }

    is NodeResult.DataClassTypeResult -> {
      val fieldNames = callee.fields.map { it.first }
      organizeParams(
        paramTypes = callee.fields,
        requiredParamNames = fieldNames.toSet() - callee.defaultValues.keys,
        paramDefaultValueTasks = callee.defaultValues,
        posArgs = posArgs,
        namedArgs = namedArgs,
        calleeContextId = callee.contextId,
      ) { args, isContextFree ->
        val value = ClassInstanceValue(callee.packageName, callee.className, args)
        valueResult(gid, isContextFree, value)
      }
    }

    else -> {
      println(callee)
      TODO()
    }
  }
}

private fun GlobalTaskRunner.organizeParams(
  paramTypes: List<Pair<String, BibixType>>,
  requiredParamNames: Set<String>,
  paramDefaultValueTasks: Map<String, TaskId>,
  posArgs: List<NodeResult>,
  namedArgs: Map<String, NodeResult>,
  calleeContextId: TaskContextId,
  whenReady: (args: Map<String, BibixValue>, isContextFree: Boolean) -> TaskRunResult
): TaskRunResult {
  val paramNames = paramTypes.map { it.first }
  // callee의 parameter 목록을 보고 posParams와 namedParams와 맞춰본다
  val posArgsMap = posArgs.zip(paramNames) { arg, name -> name to arg }.toMap()

  val remainingParamNames = paramNames.drop(posArgs.size).toSet()
  check(remainingParamNames.containsAll(namedArgs.keys)) { "Unknown parameters" }

  val unspecifiedParamNames = remainingParamNames - namedArgs.keys
  check(unspecifiedParamNames.all { it !in requiredParamNames }) { "Required parameters are not specified" }

  // 만약 callee의 default param이 필요한데 그 값이 없으면 prerequisite으로 반환하고
  // 모든 값이 충족되었으면 TaskRunResult.LongRunningResult 로 build rule을 실행하는 코드를 반환한다
  val defaultParamTasks = unspecifiedParamNames.mapNotNull { paramName ->
    paramDefaultValueTasks[paramName]?.let { paramName to it }
  }
  return withResults(
    // default param task는 callee쪽에서 정의한 것이기 때문
    calleeContextId,
    defaultParamTasks.map { it.second }.toSet(),
  ) { defaultResults ->
    val defaultParamTasksMap = defaultParamTasks.toMap()
    val defaultValues = defaultParamTasksMap.mapValues { (_, taskId) ->
      defaultResults.getValue(taskId)
    }
    val argResults: Map<String, NodeResult> = posArgsMap + namedArgs + defaultValues
    check(argResults.all { it.value is NodeResult.ValueResult })
    val allArgsAreContextFree =
      argResults.values.all { (it as NodeResult.ValueResult).isContextFree }
    val args = argResults.mapValues { (_, result) ->
      (result as NodeResult.ValueResult).value
    }

    whenReady(args, allArgsAreContextFree)
  }
}

//private fun GlobalTaskRunner.callBuildRule(
//  gid: TaskContextId,
//  taskId: TaskId,
//  buildRule: NodeResult.BuildRuleResult,
//  args: Map<String, BibixValue>,
//): TaskRunResult {
//  fun handleBuildRuleReturn(result: BuildRuleReturn): TaskRunResult =
//    when (result) {
//      is BuildRuleReturn.ValueReturn ->
//        // valueResult(gid.contextId, NodeResult.ValueResult(result.value, true))
//        TODO()
//
//      is BuildRuleReturn.FailedReturn ->
//        throw IllegalStateException(
//          "Failed in build rule: ${result.exception.message}",
//          result.exception
//        )
//
//      is BuildRuleReturn.WithDirectoryLock -> {
//        TaskRunResult.LongRunningResult { handleBuildRuleReturn(result.withLock()) }
//      }
//
//      is BuildRuleReturn.GetTypeDetails -> TODO()
//      is BuildRuleReturn.EvalAndThen -> TODO()
//
//      BuildRuleReturn.DoneReturn -> throw IllegalStateException()
//    }
//
//  return TaskRunResult.LongRunningResult {
//    val buildContext = getBuildContext(
//      buildRuleData = buildRule.buildRuleData,
//      callerProjectInstanceId = prjInstanceId,
//      calleeProjectInstanceId = buildRule.prjInstanceId,
//      args = args
//    )
//    when (val result = buildRule.implMethod.invoke(buildRule.implInstance, buildContext)) {
//      is BibixValue -> saveResult(prjInstanceId, taskId, NodeResult.ValueResult(result))
//      is BuildRuleReturn -> handleBuildRuleReturn(result)
//
//      else ->
//        throw IllegalStateException("Invalid return from build rule")
//    }
//  }
//}
//
//private suspend fun GlobalTaskRunner.getBuildContext(
//  buildRuleData: BibixIdProto.BuildRuleData,
//  callerProjectInstanceId: ProjectInstanceId,
//  calleeProjectInstanceId: ProjectInstanceId,
//  args: Map<String, BibixValue>
//): BuildContext {
//  val argsMapProto = argsMap {
//    args.toList().sortedBy { it.first }.forEach { (argName, argValue) ->
//      this.pairs.add(argPair {
//        this.name = argName
//        this.value = argValue.toProto()
//      })
//    }
//  }
//  val targetIdDataProto = targetIdData {
//    this.sourceId = sourceIdOf(calleeProjectInstanceId)
//    this.buildRule = buildRuleData
//    this.argsMap = argsMapProto
//  }
//  val targetId = TargetId(targetIdDataProto)
//
//  val inputHashes = argsMapProto.extractInputHashes()
//  // repo의 targetId의 inputHashes 값과 새로 계산한 inputHashes 값을 비교
//  val prevInputHashes = repo.getPrevInputsHashOf(targetId.targetIdBytes)
//  val hashChanged =
//    if (prevInputHashes == null) true else prevInputHashes != inputHashes.hashString()
//
//  val prevState = repo.getPrevTargetState(targetId.targetIdBytes)
//
//  return BuildContext(
//    buildEnv = buildEnv,
//    fileSystem = FileSystems.getDefault(),
//    mainBaseDirectory = globalGraph.projectLocations.getValue(MainProjectId.projectId).projectRoot,
//    callerBaseDirectory = globalGraph.projectLocations[callerProjectInstanceId.projectId]?.projectRoot,
//    ruleDefinedDirectory = globalGraph.projectLocations[calleeProjectInstanceId.projectId]?.projectRoot,
//    arguments = args,
//    targetIdData = targetId.targetIdData,
//    targetId = targetId.targetIdHex,
//    hashChanged = hashChanged,
//    prevBuildTime = prevState?.buildStartTime?.toInstant(),
//    prevResult = prevState?.buildSucceeded?.resultValue?.toBibix(),
//    destDirectoryPath = repo.objectsDirectory.resolve(targetId.targetIdHex),
//    progressLogger = object: ProgressLogger {
//      override fun logInfo(message: String) {
//        println(message)
//      }
//
//      override fun logError(message: String) {
//        println(message)
//      }
//    },
//    repo = repo
//  )
//}
//
//fun GlobalTaskRunner.sourceIdOf(contextId: TaskContextId): BibixIdProto.SourceId {
//  if (contextId.projectId == mainProjectId) {
//    return sourceId {
//      this.bibixVersion = Constants.BIBIX_VERSION
//      this.mainSource = empty { }
//    }
//  }
//  if (contextId.projectId == preludeProjectId) {
//    return sourceId {
//      this.bibixVersion = Constants.BIBIX_VERSION
//      this.preludeSource = empty { }
//    }
//  }
//  if (contextId.projectId in preloadedPluginIds.values) {
//    return sourceId {
//      this.bibixVersion = Constants.BIBIX_VERSION
//      this.preloadedPlugin = preloadedPluginIds.inverse()[contextId.projectId]!!
//    }
//  }
//  val projectLocation = globalGraph.projectLocations[contextId.projectId]!!
//  return sourceId {
//    // TODO 이름이 왜 obj hash지?
//    this.externalPluginObjhash = externalBibixProject {
//      this.rootDirectory = projectLocation.projectRoot.absolutePathString()
//      this.scriptName = projectLocation.scriptName
//    }
//  }
//}
