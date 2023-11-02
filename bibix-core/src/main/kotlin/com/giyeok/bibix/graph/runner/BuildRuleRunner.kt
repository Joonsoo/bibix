package com.giyeok.bibix.graph.runner

import com.giyeok.bibix.BibixIdProto
import com.giyeok.bibix.base.*
import com.giyeok.bibix.buildRuleData
import com.giyeok.bibix.graph.BibixName
import com.giyeok.bibix.repo.BibixRepoProto
import com.giyeok.bibix.repo.hashString
import com.giyeok.bibix.sourceId
import com.giyeok.bibix.targetIdData
import com.giyeok.bibix.utils.toBibix
import com.giyeok.bibix.utils.toHexString
import com.giyeok.bibix.utils.toInstant
import com.google.protobuf.empty
import kotlinx.coroutines.runBlocking

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
    buildRule.paramTypes,
    buildRule.buildRuleDef.def.params.requiredParamNames(),
    buildRule.projectId,
    buildRule.importInstanceId,
    buildRule.buildRuleDef.paramDefaultValues,
    posArgs,
    namedArgs,
  ) { args ->
    val buildContext = createBuildContext(buildGraphRunner, callerProjectId, buildRule, args)
    val runner = BuildRuleRunner(
      buildGraphRunner,
      callerProjectId,
      callerImportInstanceId,
      FinalizeBuildRuleReturnValue.FinalizeContext.from(buildRule),
      block
    )
    BuildTaskResult.LongRunning {
      val result = buildRule.implMethod.invoke(buildRule.implInstance, buildContext)
      runner.handleBuildReturn(result)
    }
  }
}

private fun createBuildContext(
  buildGraphRunner: BuildGraphRunner,
  callerProjectId: Int,
  buildRule: BuildTaskResult.BuildRuleResult,
  args: Map<String, BibixValue>
): BuildContext {
  val mainLocation = buildGraphRunner.multiGraph.projectLocations.getValue(1)
  val callerLocation = buildGraphRunner.multiGraph.projectLocations[callerProjectId]
  val ruleDefinedLocation = buildGraphRunner.multiGraph.projectLocations[buildRule.projectId]

  val argsMap = argsMapFrom(args)
  val targetIdData = targetIdData {
    // TODO
    this.sourceId = sourceIdFrom(buildGraphRunner, callerProjectId)
    this.buildRule = buildRuleData {
      this.buildRuleSourceId = sourceIdFrom(buildGraphRunner, buildRule.projectId)
      if (buildRule.buildRuleDef.implTarget == null) {
        this.nativeImpl = empty { }
      } else {
        // TODO
        this.bibixValueHash
      }
      this.buildRuleClassName = buildRule.buildRuleDef.implClassName
      this.buildRuleMethodName = buildRule.buildRuleDef.implMethodName
    }
    this.argsMap = argsMap
  }
  val targetId = targetIdData.hashString()
  val targetIdHex = targetId.toHexString()

  // TODO: BibixRepo 기능 정리
  //  - target들의 실행 상태 잘 기록하기
  val repo = buildGraphRunner.repo
  val prevInputHashes = runBlocking { repo.getPrevInputsHashOf(targetId) }
  val hashChanged = if (prevInputHashes == null) true else prevInputHashes != argsMap.hashString()

  // TODO: 같은 run에선 같은 target id는 값을 재활용하자

  val targetState = runBlocking { repo.getPrevTargetState(targetId) }
  val prevResult = targetState?.let {
    if (targetState.stateCase == BibixRepoProto.TargetState.StateCase.BUILD_SUCCEEDED) {
      targetState.buildSucceeded.resultValue.toBibix()
    } else {
      null
    }
  }

  return BuildContext(
    buildEnv = buildGraphRunner.buildEnv,
    fileSystem = buildGraphRunner.fileSystem,
    mainBaseDirectory = mainLocation.projectRoot,
    callerBaseDirectory = callerLocation?.projectRoot,
    ruleDefinedDirectory = ruleDefinedLocation?.projectRoot,
    arguments = args,
    targetIdData = targetIdData,
    targetId = targetIdHex,
    hashChanged = hashChanged,
    prevBuildTime = targetState?.buildStartTime?.toInstant(),
    prevResult = prevResult,
    destDirectoryPath = repo.objectsDirectory.resolve(targetIdHex),
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

private fun sourceIdFrom(
  buildGraphRunner: BuildGraphRunner,
  projectId: Int
): BibixIdProto.SourceId {
  // TODO 구현하기
  return sourceId {
    this.bibixVersion = Constants.BIBIX_VERSION
  }
}

class BuildRuleRunner(
  val buildGraphRunner: BuildGraphRunner,
  val callerProjectId: Int,
  val callerImportInstanceId: Int,
  val finalizeCtx: FinalizeBuildRuleReturnValue.FinalizeContext,
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
      is BuildRuleReturn.GetTypeDetails -> TODO()
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
      is BuildRuleReturn.GetTypeDetails -> TODO()
      is BuildRuleReturn.WithDirectoryLock -> handleWithDirectoryLock(result, ::handleActionReturn)
    }

    else -> throw IllegalStateException("Unsupported return value from action rule: $result")
  }

  private fun handleEvalAndThen(
    result: BuildRuleReturn.EvalAndThen,
    afterThen: (BuildRuleReturn) -> BuildTaskResult
  ): BuildTaskResult = buildGraphRunner.lookupExprValue(
    finalizeCtx.projectId,
    BibixName(result.ruleName),
    finalizeCtx.importInstanceId,
  ) { lookupResult ->
    check(lookupResult is BuildTaskResult.BuildRuleResult)

    val params = result.params.entries
    BuildTaskResult.WithResultList(params.map {
      FinalizeBuildRuleReturnValue(
        finalizeCtx,
        it.value,
        // TODO 여기 projectId와 importInstanceId가 이게 맞나?
        finalizeCtx.projectId,
        finalizeCtx.importInstanceId
      )
    }) { finalized ->
      val finValues = finalized.map {
        check(it is BuildTaskResult.ValueResult)
        it.value
      }
      val finalizedParams = params.map { it.key }.zip(finValues).toMap()
      organizeParamsAndRunBuildRule(
        buildGraphRunner,
        // TODO 여기서 주는 projectId와 importInstanceId가 이게 맞나?
        callerProjectId,
        callerImportInstanceId,
        lookupResult,
        listOf(),
        finalizedParams
      ) { evalResult ->
        // TODO evalResult를 buildRuleResult의 return type으로 cast
        check(evalResult is BuildTaskResult.ValueResult)
        BuildTaskResult.LongRunning {
          afterThen(result.whenDone(evalResult.value))
        }
      }
    }
  }

  private fun handleWithDirectoryLock(
    result: BuildRuleReturn.WithDirectoryLock,
    afterThen: (BuildRuleReturn) -> BuildTaskResult
  ): BuildTaskResult {
    // TODO lock result.directory
    // TODO 그런데 이런식으로 락을 잡으면 중간에 풀리는게 아닌가..?
    return BuildTaskResult.LongRunning {
      val withLock = try {
        result.withLock()
      } finally {
        // TODO unlock
      }
      afterThen(withLock)
    }
  }
}
