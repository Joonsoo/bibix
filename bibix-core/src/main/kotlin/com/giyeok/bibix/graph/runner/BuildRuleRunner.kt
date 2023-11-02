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

class BuildRuleRunner(
  val buildGraphRunner: BuildGraphRunner,
  val projectId: Int,
  val importInstanceId: Int,
  val finalizeCtx: FinalizeBuildRuleReturnValue.FinalizeContext,
  val whenDoneBlock: (BuildTaskResult) -> BuildTaskResult
) {
  fun organizeParamsAndRunBuildRule(
    buildRule: BuildTaskResult.BuildRuleResult,
    posArgs: List<BibixValue>,
    namedArgs: Map<String, BibixValue>,
    block: (BuildTaskResult) -> BuildTaskResult
  ): BuildTaskResult {
    val requiredParams = buildRule.buildRuleDef.def.params
      .filter { param -> !param.optional && param.defaultValue == null }
      .map { it.name }.toSet()
    return organizeParams(
      buildRule.paramTypes,
      requiredParams,
      buildRule.projectId,
      buildRule.importInstanceId,
      buildRule.buildRuleDef.paramDefaultValues,
      posArgs,
      namedArgs,
    ) { args ->
      val buildContext = createBuildContext(projectId, buildRule, args)
      runBuildRule(buildRule, buildContext, block)
    }
  }

  private fun runBuildRule(
    buildRule: BuildTaskResult.BuildRuleResult,
    buildContext: BuildContext,
    block: (BuildTaskResult) -> BuildTaskResult
  ): BuildTaskResult = BuildTaskResult.LongRunning {
    val result = buildRule.implMethod.invoke(buildRule.implInstance, buildContext)

//    fun handleBuildRuleReturn(result: BuildRuleReturn): BuildTaskResult = when (result) {
//      is BuildRuleReturn.ValueReturn -> block(BuildTaskResult.ValueResult(result.value))
//      is BuildRuleReturn.FailedReturn -> throw result.exception
//      BuildRuleReturn.DoneReturn -> throw IllegalStateException()
//
//      is BuildRuleReturn.EvalAndThen -> {
//        buildGraphRunner.lookupExprValue(
//          buildRule.projectId,
//          BibixName(result.ruleName),
//          buildRule.importInstanceId,
//        ) { lookupResult ->
//          check(lookupResult is BuildTaskResult.BuildRuleResult)
//
//          val params = result.params.entries
//          BuildTaskResult.WithResultList(params.map {
//            FinalizeBuildRuleReturnValue(
//              buildRule,
//              it.value,
//              buildRule.projectId,
//              buildRule.importInstanceId
//            )
//          }) { finalized ->
//            val finValues = finalized.map {
//              check(it is BuildTaskResult.ValueResult)
//              it.value
//            }
//            val finalizedParams = params.map { it.key }.zip(finValues).toMap()
//            organizeParamsAndRunBuildRule(lookupResult, listOf(), finalizedParams) { evalResult ->
//              // TODO evalResult를 buildRuleResult의 return type으로 cast
//              check(evalResult is BuildTaskResult.ValueResult)
//              BuildTaskResult.LongRunning {
//                handleBuildRuleReturn(result.whenDone(evalResult.value))
//              }
//            }
//          }
//        }
//      }
//
//      is BuildRuleReturn.GetTypeDetails -> {
//        TODO()
//      }
//
//      is BuildRuleReturn.WithDirectoryLock -> {
//        BuildTaskResult.LongRunning {
//          // TODO directory lock for result.directory
//          handleBuildRuleReturn(result.withLock())
//        }
//      }
//    }

    when (result) {
      is BibixValue -> block(BuildTaskResult.ValueResult(result))
      is BuildRuleReturn -> handleBuildRuleReturn(result)

      else -> throw IllegalStateException("Unsupported return value from build rule: $result")
    }
  }

  fun handleBuildRuleReturn(result: BuildRuleReturn): BuildTaskResult = when (result) {
    is BuildRuleReturn.ValueReturn -> whenDoneBlock(BuildTaskResult.ValueResult(result.value))
    is BuildRuleReturn.FailedReturn -> throw result.exception
    BuildRuleReturn.DoneReturn -> throw IllegalStateException()

    is BuildRuleReturn.EvalAndThen -> {
      buildGraphRunner.lookupExprValue(
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
          organizeParamsAndRunBuildRule(lookupResult, listOf(), finalizedParams) { evalResult ->
            // TODO evalResult를 buildRuleResult의 return type으로 cast
            check(evalResult is BuildTaskResult.ValueResult)
            BuildTaskResult.LongRunning {
              handleBuildRuleReturn(result.whenDone(evalResult.value))
            }
          }
        }
      }
    }

    is BuildRuleReturn.GetTypeDetails -> {
      TODO()
    }

    is BuildRuleReturn.WithDirectoryLock -> {
      BuildTaskResult.LongRunning {
        // TODO directory lock for result.directory
        handleBuildRuleReturn(result.withLock())
      }
    }
  }

  private fun createBuildContext(
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
      this.sourceId = sourceIdFrom(callerProjectId)
      this.buildRule = buildRuleData {
        this.buildRuleSourceId = sourceIdFrom(buildRule.projectId)
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

  private fun sourceIdFrom(projectId: Int): BibixIdProto.SourceId {
    // TODO 구현하기
    return sourceId {
      this.bibixVersion = Constants.BIBIX_VERSION
    }
  }
}
