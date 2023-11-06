package com.giyeok.bibix.graph.runner

import com.giyeok.bibix.*
import com.giyeok.bibix.base.*
import com.giyeok.bibix.graph.BibixName
import com.giyeok.bibix.repo.BibixRepoProto
import com.giyeok.bibix.repo.extractInputHashes
import com.giyeok.bibix.repo.hashString
import com.giyeok.bibix.utils.toBibix
import com.giyeok.bibix.utils.toHexString
import com.giyeok.bibix.utils.toInstant
import com.giyeok.bibix.utils.toProto
import com.google.protobuf.empty
import kotlin.io.path.absolutePathString

fun organizeParamsAndRunBuildRule(
  buildGraphRunner: BuildGraphRunner,
  callerProjectId: Int,
  callerImportInstanceId: Int,
  buildRule: BuildTaskResult.BuildRuleResult,
  posArgs: List<BibixValue>,
  namedArgs: Map<String, BibixValue>,
  block: (BuildTaskResult.FinalResult) -> BuildTaskResult
): BuildTaskResult {
  return organizeParams(
    callerProjectId,
    buildRule.paramTypes,
    buildRule.buildRuleDef.def.params.requiredParamNames(),
    buildRule.projectId,
    buildRule.importInstanceId,
    buildRule.buildRuleDef.paramDefaultValues,
    posArgs,
    namedArgs,
  ) { args ->
    withBuildContext(buildGraphRunner, callerProjectId, buildRule, args) { buildContext ->
      // TODO target이 실패한 경우에 repo에 업데이트
      val runner = BuildRuleRunner(
        buildGraphRunner,
        callerProjectId,
        callerImportInstanceId,
        BuildRuleDefContext.from(buildRule)
      ) { result ->
        if (result is BuildTaskResult.ResultWithValue) {
          block(BuildTaskResult.ValueOfTargetResult(result.value, buildContext.targetId))
        } else {
          block(result)
        }
      }
      BuildTaskResult.LongRunning {
        val result = buildRule.implMethod.invoke(buildRule.implInstance, buildContext)
        runner.handleBuildReturn(result)
      }
    }
  }
}

private fun withBuildContext(
  buildGraphRunner: BuildGraphRunner,
  callerProjectId: Int,
  buildRule: BuildTaskResult.BuildRuleResult,
  args: Map<String, BibixValue>,
  block: (BuildContext) -> BuildTaskResult
): BuildTaskResult {
  val argsMap = argsMapFrom(args)
  val targetIdData = targetIdData {
    // target id에 caller측에 대한 정보가 들어갈 필요가 있나?
    //  - target의 일반적 의미상으론 필요가 없는게 맞는 것 같음
    //  - 하지만 BuildContext에 caller에 대한 정보(caller의 디렉토리 등)가 들어있기 때문에
    //    보수적으로 보면 caller에 대한 정보가 target id에도 반영되는 것이 맞을듯 함
    this.sourceId = sourceIdFrom(buildGraphRunner, callerProjectId)
    this.buildRule = buildRuleData {
      this.buildRuleSourceId = sourceIdFrom(buildGraphRunner, buildRule.projectId)
      if (buildRule.buildRuleDef.implTarget == null) {
        check(buildRule.classPkg == null)
        this.nativeImpl = empty { }
      } else {
        this.bibixValueHash = checkNotNull(buildRule.classPkg).toBibix().toProto().hashString()
      }
      this.buildRuleClassName = buildRule.buildRuleDef.implClassName
      this.buildRuleMethodName = buildRule.buildRuleDef.implMethodName
    }
    this.argsMap = argsMap
  }
  val targetId = targetIdData.hashString()
  val targetIdHex = targetId.toHexString()

  val inputHashes = argsMap.extractInputHashes()
  val inputHashString = inputHashes.hashString()

  val repo = buildGraphRunner.repo

  val (reuse, prevState) =
    repo.targetStarted(targetIdHex, targetIdData, inputHashes, inputHashString) { prevState ->
      val prevTargetIdData = repo.getTargetIdData(targetIdHex)
      // 혹시나 불일치하는 경우가 생기지 않는지 확인
      check(prevTargetIdData == targetIdData)

      when (prevState.stateCase) {
        BibixRepoProto.TargetState.StateCase.BUILD_STARTED -> {
          // 동시에 같은 target이 두 번 이상 호출되려고 하면, 즉, 이미 target started인데
          // 같은 target을 또 실행하려고 하면 다른쪽 타겟의 결과를 기다렸다가 그걸 사용한다.
          BuildTaskResult.DuplicateTargetResult(targetIdHex)
        }

        BibixRepoProto.TargetState.StateCase.BUILD_SUCCEEDED -> {
          // 실패한 경우도 재사용할 수 있겠지만.. 일단 성공한 경우만
          BuildTaskResult.ValueOfTargetResult(
            prevState.buildSucceeded.resultValue.toBibix(),
            targetIdHex
          )
        }

        BibixRepoProto.TargetState.StateCase.BUILD_FAILED -> TODO()
        else -> throw AssertionError()
      }
    }

  if (reuse != null) {
    return reuse
  }

  val mainLocation = buildGraphRunner.multiGraph.projectLocations.getValue(1)
  val callerLocation = buildGraphRunner.multiGraph.projectLocations[callerProjectId]
  val ruleDefinedLocation = buildGraphRunner.multiGraph.projectLocations[buildRule.projectId]

  val hashChanged = if (prevState == null) true else {
    prevState.inputHashString != inputHashString
  }

  val prevResult =
    if (prevState != null && prevState.stateCase == BibixRepoProto.TargetState.StateCase.BUILD_SUCCEEDED) {
      prevState.buildSucceeded.resultValue.toBibix()
    } else {
      null
    }

  val buildContext = BuildContext(
    buildEnv = buildGraphRunner.buildEnv,
    fileSystem = buildGraphRunner.fileSystem,
    mainBaseDirectory = mainLocation.projectRoot,
    callerBaseDirectory = callerLocation?.projectRoot,
    ruleDefinedDirectory = ruleDefinedLocation?.projectRoot,
    arguments = args,
    targetIdData = targetIdData,
    targetIdBytes = targetId,
    targetId = targetIdHex,
    inputHashes = inputHashes,
    inputHashString = inputHashString,
    prevInputHashes = prevState?.inputHashes,
    prevInputHashString = prevState?.inputHashString,
    hashChanged = hashChanged,
    prevBuildTime = prevState?.buildStartTime?.toInstant(),
    prevResult = prevResult,
    destDirectoryPath = repo.objectsDirectory.resolve(targetIdHex),
    progressLogger = repo.progressLoggerFor(targetIdHex),
    repo = repo
  )
  return block(buildContext)
}

private fun sourceIdFrom(
  buildGraphRunner: BuildGraphRunner,
  projectId: Int
): BibixIdProto.SourceId {
  return sourceId {
    when (projectId) {
      1 -> {
        this.bibixVersion = Constants.BIBIX_VERSION
        this.mainSource = empty {}
      }

      2 -> {
        this.bibixVersion = Constants.BIBIX_VERSION
        this.preludeSource = empty {}
      }

      in buildGraphRunner.preloadedPluginIds.inverse() -> {
        this.bibixVersion = Constants.BIBIX_VERSION
        this.preloadedPlugin = buildGraphRunner.preloadedPluginIds.inverse().getValue(projectId)
      }

      else -> {
        val projectLocation = buildGraphRunner.multiGraph.projectLocations.getValue(projectId)
        this.externalPluginObjhash = externalBibixProject {
          this.rootDirectory = projectLocation.projectRoot.absolutePathString()
          this.scriptName = projectLocation.scriptName
        }
      }
    }
  }
}

class BuildRuleRunner(
  val buildGraphRunner: BuildGraphRunner,
  val callerProjectId: Int,
  val callerImportInstanceId: Int,
  // build rule이 정의된 위치
  val buildRuleDefCtx: BuildRuleDefContext,
  val whenDoneBlock: (BuildTaskResult.FinalResult) -> BuildTaskResult
) {
  fun handleBuildReturn(result: Any?): BuildTaskResult = when (result) {
    is BibixValue -> whenDoneBlock(BuildTaskResult.ValueResult(result))
    is BuildRuleReturn -> when (result) {
      BuildRuleReturn.DoneReturn -> throw IllegalStateException("build rule must not return DoneReturn")
      is BuildRuleReturn.ValueReturn -> whenDoneBlock(BuildTaskResult.ValueResult(result.value))
      is BuildRuleReturn.FailedReturn ->
        throw IllegalStateException("Plugin returned error", result.exception)

      is BuildRuleReturn.EvalAndThen -> handleEvalAndThen(result, ::handleBuildReturn)
      is BuildRuleReturn.GetTypeDetails -> handleGetTypeDetails(result, ::handleBuildReturn)
      is BuildRuleReturn.WithDirectoryLock -> handleWithDirectoryLock(result, ::handleBuildReturn)
    }

    else -> throw IllegalStateException("Unsupported return value from build rule: $result")
  }

  fun handleActionReturn(result: Any?): BuildTaskResult = when (result) {
    null -> whenDoneBlock(BuildTaskResult.ActionRuleDoneResult(null))
    is BibixValue -> whenDoneBlock(BuildTaskResult.ActionRuleDoneResult(result))
    is BuildRuleReturn -> when (result) {
      BuildRuleReturn.DoneReturn -> whenDoneBlock(BuildTaskResult.ActionRuleDoneResult(null))
      is BuildRuleReturn.ValueReturn -> whenDoneBlock(BuildTaskResult.ValueResult(result.value))
      is BuildRuleReturn.FailedReturn ->
        throw IllegalStateException("Plugin returned error", result.exception)

      is BuildRuleReturn.EvalAndThen -> handleEvalAndThen(result, ::handleActionReturn)
      is BuildRuleReturn.GetTypeDetails -> handleGetTypeDetails(result, ::handleActionReturn)
      is BuildRuleReturn.WithDirectoryLock -> handleWithDirectoryLock(result, ::handleActionReturn)
    }

    else -> throw IllegalStateException("Unsupported return value from action rule: $result")
  }

  private fun handleEvalAndThen(
    result: BuildRuleReturn.EvalAndThen,
    afterThen: (BuildRuleReturn) -> BuildTaskResult
  ): BuildTaskResult {
    val params = result.params.entries
    return BuildTaskResult.WithResultList(params.map {
      FinalizeBuildRuleReturnValue(
        buildRuleDefCtx,
        it.value,
        callerProjectId,
      )
    }) { finalized ->
      val finValues = finalized.map {
        check(it is BuildTaskResult.ResultWithValue)
        it.value
      }
      val finalizedParams = params.map { it.key }.zip(finValues).toMap()

      BuildTaskResult.WithResult(
        EvalCallExpr(
          buildRuleDefCtx,
          BibixName(result.ruleName),
          callerProjectId,
          callerImportInstanceId,
          finalizedParams
        )
      ) { evalResult ->
        check(evalResult is BuildTaskResult.ResultWithValue)
        BuildTaskResult.LongRunning {
          afterThen(result.whenDone(evalResult.value))
        }
      }
    }
  }

  private fun handleGetTypeDetails(
    result: BuildRuleReturn.GetTypeDetails,
    afterThen: (BuildRuleReturn) -> BuildTaskResult
  ): BuildTaskResult {
    if (result.relativeNames.isNotEmpty()) {
      TODO()
    }
    return BuildTaskResult.WithResultList(result.typeNames.map { typeName ->
      EvalTypeByName(typeName.packageName, typeName.typeName)
    }) { results ->
      val typeDetails = result.typeNames.zip(results).associate { (name, result) ->
        val typeDetail = when (result) {
          is BuildTaskResult.DataClassResult -> {
            val fieldTypes = result.fieldTypes.toMap()
            val fields = result.dataClassDef.def.fields.map { field ->
              RuleParam(field.name, fieldTypes.getValue(field.name).toTypeValue(), field.optional)
            }
            DataClassTypeDetails(result.packageName, result.name.toString(), fields)
          }

          is BuildTaskResult.SuperClassHierarchyResult -> {
            val subTypes = result.subTypes.map { it.name.toString() }
            SuperClassTypeDetails(result.packageName, result.name.toString(), subTypes)
          }

          is BuildTaskResult.EnumTypeResult -> {
            EnumTypeDetails(result.packageName, result.name.toString(), result.values)
          }

          else -> throw IllegalStateException()
        }
        name to typeDetail
      }
      BuildTaskResult.LongRunning {
        afterThen(result.whenDone(TypeDetailsMap(typeDetails, mapOf())))
      }
    }
  }

  private fun handleWithDirectoryLock(
    result: BuildRuleReturn.WithDirectoryLock,
    afterThen: (BuildRuleReturn) -> BuildTaskResult
  ): BuildTaskResult {
    // TODO lock result.directory
    // TODO 그런데 이런식으로 락을 잡으면 중간에 풀리는게 아닌가..?
    val directoryLocker = buildGraphRunner.repo.directoryLocker
    return BuildTaskResult.SuspendLongRunning {
      directoryLocker.acquireLock(result.directory)
      val withLockResult = try {
        result.withLock()
      } finally {
        directoryLocker.releaseLock(result.directory)
      }
      afterThen(withLockResult)
    }
  }
}
