package com.giyeok.bibix.graph.runner

import com.giyeok.bibix.*
import com.giyeok.bibix.ast.BibixAst
import com.giyeok.bibix.base.*
import com.giyeok.bibix.graph.*
import com.giyeok.bibix.repo.TargetId
import com.giyeok.bibix.repo.extractInputHashes
import com.giyeok.bibix.repo.hashString
import com.giyeok.bibix.utils.toBibix
import com.giyeok.bibix.utils.toInstant
import com.giyeok.bibix.utils.toProto
import com.google.protobuf.empty
import java.nio.file.FileSystems
import kotlin.io.path.absolutePathString

fun GlobalTaskRunner.evaluateCallExprNode(
  prjInstanceId: ProjectInstanceId,
  node: CallExprNode
): TaskRunResult {
  val posArgs = node.posParams.map { getResult(prjInstanceId, it)!! }
  val namedArgs = node.namedParams.mapValues { (_, arg) ->
    getResult(prjInstanceId, arg)!!
  }
  return when (val callee = getResult(prjInstanceId, node.callee)!!) {
    is NodeResult.BuildRuleResult -> {
      val params = callee.buildRuleNode.def.params
      val requiredParamNames =
        params.filter { !it.optional && it.defaultValue == null }.map { it.name }.toSet()
      organizeParams(
        prjInstanceId = prjInstanceId,
        paramTypes = callee.params,
        requiredParamNames = requiredParamNames,
        paramDefaultValueTasks = callee.buildRuleNode.paramDefaultValues,
        posArgs = posArgs,
        namedArgs = namedArgs,
        calleeProjectInstanceId = callee.prjInstanceId
      ) { args ->
        TaskRunResult.LongRunningResult {
          callBuildRule(prjInstanceId, node.id, callee, args)
        }
      }
    }

    is NodeResult.DataClassTypeResult -> {
      val fieldNames = callee.fields.map { it.first }
      organizeParams(
        prjInstanceId = prjInstanceId,
        paramTypes = callee.fields,
        requiredParamNames = fieldNames.toSet() - callee.defaultValues.keys,
        paramDefaultValueTasks = callee.defaultValues,
        posArgs = posArgs,
        namedArgs = namedArgs,
        calleeProjectInstanceId = callee.prjInstanceId,
      ) { args ->
        val value = ClassInstanceValue(callee.packageName, callee.className, args)
        TaskRunResult.ImmediateResult(NodeResult.ValueResult(value))
      }
    }

    else -> {
      println(callee)
      TODO()
    }
  }
}

private fun GlobalTaskRunner.organizeParams(
  prjInstanceId: ProjectInstanceId,
  paramTypes: List<Pair<String, BibixType>>,
  requiredParamNames: Set<String>,
  paramDefaultValueTasks: Map<String, TaskId>,
  posArgs: List<NodeResult>,
  namedArgs: Map<String, NodeResult>,
  calleeProjectInstanceId: ProjectInstanceId,
  whenReady: (args: Map<String, BibixValue>) -> TaskRunResult
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
  return globalWithResults(
    // default param task는 callee쪽에서 정의한 것이기 때문
    calleeProjectInstanceId,
    defaultParamTasks.map { it.second }.toSet(),
    TaskEdgeType.ValueDependency
  ) { defaultResults ->
    val defaultParamTasksMap = defaultParamTasks.toMap()
    val defaultValues = defaultParamTasksMap.mapValues { (_, taskId) ->
      defaultResults.getValue(taskId)
    }
    val argResults: Map<String, NodeResult> = posArgsMap + namedArgs + defaultValues
    check(argResults.all { it.value is NodeResult.ValueResult })
    val args = argResults.mapValues { (_, result) ->
      (result as NodeResult.ValueResult).value
    }

    val paramTypesMap = paramTypes.toMap()
    withCoercedValues(args, paramTypesMap, prjInstanceId) { coercedArgs ->
      val noneArgs = unspecifiedParamNames - coercedArgs.keys
      val finalArgs = coercedArgs + noneArgs.associateWith { NoneValue }
      check(paramNames.toSet() == finalArgs.keys)
      whenReady(finalArgs)
    }
  }
}

private suspend fun GlobalTaskRunner.callBuildRule(
  prjInstanceId: ProjectInstanceId,
  taskId: TaskId,
  buildRule: NodeResult.BuildRuleResult,
  args: Map<String, BibixValue>,
): TaskRunResult {
  fun finishBuildRuleReturnValue(returned: BibixValue): TaskRunResult = when (returned) {
    is NClassInstanceValue -> {
      // TODO 테스트 필요
      val graph = globalGraph.getProjectGraph(prjInstanceId.projectId)
      when (val lookupResult =
        graph.nameLookupTable.lookupName(returned.nameTokens)) {
        is NameEntryFound -> {
          val nameEntry = lookupResult.entry
          check(nameEntry is ClassNameEntry)
          check(nameEntry.def is BibixAst.DataClassDef)
          checkNotNull(graph.packageName)

          val classInstanceValue = ClassInstanceValue(
            graph.packageName,
            nameEntry.def.name,
            returned.fieldValues
          )

          TaskRunResult.ImmediateResult(NodeResult.ValueResult(classInstanceValue))

          globalWithResult(prjInstanceId, nameEntry.id, TaskEdgeType.TypeDependency) { cls ->
            check(cls is NodeResult.DataClassTypeResult)
            finalizeClassInstanceValue(prjInstanceId, taskId, classInstanceValue, cls)
          }
        }

        is NameInImport -> {
          TODO()
        }

        else -> throw IllegalStateException()
      }
    }

    is ClassInstanceValue -> {
      // 현재는 반환된 클래스의 패키지 이름은 이 시점에 이미 알려져 있다

      val classDefineProjectId =
        globalGraph.getProjectIdByPackageName(returned.packageName)
          ?: throw IllegalStateException()
      val classDefineGraph = globalGraph.getProjectGraph(classDefineProjectId)
      val classDefineTask = when (val lookupResult =
        classDefineGraph.nameLookupTable.lookupName(listOf(returned.className))) {
        is NameEntryFound -> {
          check(lookupResult.entry is ClassNameEntry)
          lookupResult.entry.id
        }

        else -> {
          TODO()
        }
      }

      when (classDefineProjectId) {
        MainProjectId.projectId -> {
          globalWithResult(
            // 여기서 prjInstanceId로 뭐를 줘야되지?
            prjInstanceId = MainProjectId,
            localTaskId = classDefineTask,
            edgeType = TaskEdgeType.TypeDependency
          ) { dataClassResult ->
            check(dataClassResult is NodeResult.DataClassTypeResult)
            finalizeClassInstanceValue(prjInstanceId, taskId, returned, dataClassResult)
          }
        }

        PreludeProjectId.projectId -> {
          globalWithResult(
            // 여기서 prjInstanceId로 뭐를 줘야되지?
            prjInstanceId = PreludeProjectId,
            localTaskId = classDefineTask,
            edgeType = TaskEdgeType.TypeDependency
          ) { dataClassResult ->
            check(dataClassResult is NodeResult.DataClassTypeResult)
            finalizeClassInstanceValue(prjInstanceId, taskId, returned, dataClassResult)
          }
        }

        else -> {
          // TODO 이렇게 하는게 맞나..?
          globalWithResult(
            prjInstanceId = buildRule.prjInstanceId,
            localTaskId = buildRule.buildRuleNode.returnType,
            edgeType = TaskEdgeType.TypeDependency
          ) { returnTypeResult ->
            when (returnTypeResult) {
              is NodeResult.DataClassTypeResult ->
                finalizeClassInstanceValue(prjInstanceId, taskId, returned, returnTypeResult)

              is NodeResult.TypeResult -> {
                // TODO 같은 프로젝트 안에서 정의된 타입인 경우에 이렇게 되는건가..? 아 어려워
                globalWithResult(
                  prjInstanceId = buildRule.prjInstanceId,
                  localTaskId = classDefineTask,
                  edgeType = TaskEdgeType.TypeDependency
                ) { dataClassResult ->
                  check(dataClassResult is NodeResult.DataClassTypeResult)
                  finalizeClassInstanceValue(prjInstanceId, taskId, returned, dataClassResult)
                }
              }

              else -> {
                TODO()
              }
            }
          }
        }
      }
    }

    else -> {
      withCoercedValue(returned, buildRule.returnType, prjInstanceId) { coerced ->
        saveResult(prjInstanceId, taskId, NodeResult.ValueResult(coerced))
      }
    }
  }

  fun handleBuildRuleReturn(result: BuildRuleReturn): TaskRunResult =
    when (result) {
      is BuildRuleReturn.ValueReturn -> finishBuildRuleReturnValue(result.value)

      is BuildRuleReturn.FailedReturn ->
        throw IllegalStateException(
          "Failed in build rule: ${result.exception.message}",
          result.exception
        )

      is BuildRuleReturn.WithDirectoryLock -> {
        TaskRunResult.LongRunningResult { handleBuildRuleReturn(result.withLock()) }
      }

      is BuildRuleReturn.GetTypeDetails -> TODO()
      is BuildRuleReturn.EvalAndThen -> TODO()

      BuildRuleReturn.DoneReturn -> throw IllegalStateException()
    }

  val buildContext =
    getBuildContext(buildRule.buildRuleData, prjInstanceId, buildRule.prjInstanceId, args)
  return when (val result = buildRule.implMethod.invoke(buildRule.implInstance, buildContext)) {
    is BibixValue -> finishBuildRuleReturnValue(result)
    is BuildRuleReturn -> handleBuildRuleReturn(result)

    else ->
      throw IllegalStateException("Invalid return from build rule")
  }
}

private fun GlobalTaskRunner.finalizeClassInstanceValue(
  prjInstanceId: ProjectInstanceId,
  taskId: TaskId,
  value: ClassInstanceValue,
  dataClass: NodeResult.DataClassTypeResult,
): TaskRunResult {
  // TODO 이 내용은 coerce value 쪽으로 옮겨야 함
  //  -> 지금같은 상태로는 클래스 field로 들어가 있는 class instance에 대해선 이와 같은 보정이 이루어지지 않음
  // dataClassResult.fields와 defaultValues를 보고 value를 수정해서 반환
  //  - 빠져있는 optional 필드에 NoneValue 넣기
  //  - 빠져있는 default value 필드 채우기
  //  - 필드별 type coercion
  val fieldMaps = dataClass.fields.toMap()
  check(fieldMaps.keys.containsAll(value.fieldValues.keys)) { "Unknown fields" }

  val requiredFields = fieldMaps.keys - (dataClass.defaultValues.keys + dataClass.optionalFields)
  check(value.fieldValues.keys.containsAll(requiredFields)) { "Missing required fields" }

  val defaultFieldTasks = dataClass.defaultValues - value.fieldValues.keys

  return globalWithResults(
    prjInstanceId = dataClass.prjInstanceId,
    localTaskIds = defaultFieldTasks.values.toSet(),
    edgeType = TaskEdgeType.ValueDependency
  ) { defaultFieldValues ->
    withCoercedValues(value.fieldValues, fieldMaps, dataClass.prjInstanceId) { coercedFields ->
      val defaults = defaultFieldTasks.mapValues { (_, taskId) ->
        (defaultFieldValues.getValue(taskId) as NodeResult.ValueResult).value
      }
      val nones = (dataClass.optionalFields - value.fieldValues.keys).associateWith { NoneValue }
      check(value.fieldValues.keys.intersect(defaults.keys).isEmpty())
      check(value.fieldValues.keys.intersect(nones.keys).isEmpty())
      check(defaults.keys.intersect(nones.keys).isEmpty())
      val fieldValues = coercedFields + defaults + nones
      check(fieldValues.keys == fieldMaps.keys)

      saveResult(
        prjInstanceId,
        taskId,
        NodeResult.ValueResult(ClassInstanceValue(value.packageName, value.className, fieldValues))
      )
    }
  }
}

private suspend fun GlobalTaskRunner.getBuildContext(
  buildRuleData: BibixIdProto.BuildRuleData,
  callerProjectInstanceId: ProjectInstanceId,
  calleeProjectInstanceId: ProjectInstanceId,
  args: Map<String, BibixValue>
): BuildContext {
  val argsMapProto = argsMap {
    args.toList().sortedBy { it.first }.forEach { (argName, argValue) ->
      this.pairs.add(argPair {
        this.name = argName
        this.value = argValue.toProto()
      })
    }
  }
  val targetIdDataProto = targetIdData {
    this.sourceId = sourceIdOf(calleeProjectInstanceId)
    this.buildRule = buildRuleData
    this.argsMap = argsMapProto
  }
  val targetId = TargetId(targetIdDataProto)

  val inputHashes = argsMapProto.extractInputHashes()
  // repo의 targetId의 inputHashes 값과 새로 계산한 inputHashes 값을 비교
  val prevInputHashes = repo.getPrevInputsHashOf(targetId.targetIdBytes)
  val hashChanged =
    if (prevInputHashes == null) true else prevInputHashes != inputHashes.hashString()

  val prevState = repo.getPrevTargetState(targetId.targetIdBytes)

  return BuildContext(
    buildEnv = buildEnv,
    fileSystem = FileSystems.getDefault(),
    mainBaseDirectory = globalGraph.projectLocations.getValue(MainProjectId.projectId).projectRoot,
    callerBaseDirectory = globalGraph.projectLocations[callerProjectInstanceId.projectId]?.projectRoot,
    ruleDefinedDirectory = globalGraph.projectLocations[calleeProjectInstanceId.projectId]?.projectRoot,
    arguments = args,
    targetIdData = targetId.targetIdData,
    targetId = targetId.targetIdHex,
    hashChanged = hashChanged,
    prevBuildTime = prevState?.buildStartTime?.toInstant(),
    prevResult = prevState?.buildSucceeded?.resultValue?.toBibix(),
    destDirectoryPath = repo.objectsDirectory.resolve(targetId.targetIdHex),
    progressLogger = object: ProgressLogger {
      override fun logInfo(message: String) {
        println(message)
      }

      override fun logError(message: String) {
        println(message)
      }
    },
    repo = repo
  )
}

fun GlobalTaskRunner.sourceIdOf(prjInstanceId: ProjectInstanceId): BibixIdProto.SourceId {
  if (prjInstanceId.projectId == MainProjectId.projectId) {
    return sourceId {
      this.bibixVersion = Constants.BIBIX_VERSION
      this.mainSource = empty { }
    }
  }
  if (prjInstanceId.projectId == PreludeProjectId.projectId) {
    return sourceId {
      this.bibixVersion = Constants.BIBIX_VERSION
      this.preludeSource = empty { }
    }
  }
  if (prjInstanceId.projectId in preloadedPluginIds.values) {
    return sourceId {
      this.bibixVersion = Constants.BIBIX_VERSION
      this.preloadedPlugin = preloadedPluginIds.inverse()[prjInstanceId.projectId]!!
    }
  }
  val projectLocation = globalGraph.projectLocations[prjInstanceId.projectId]!!
  return sourceId {
    // TODO 이름이 왜 obj hash지?
    this.externalPluginObjhash = externalBibixProject {
      this.rootDirectory = projectLocation.projectRoot.absolutePathString()
      this.scriptName = projectLocation.scriptName
    }
  }
}
