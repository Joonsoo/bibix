package com.giyeok.bibix.graph.runner

import com.giyeok.bibix.*
import com.giyeok.bibix.ast.BibixAst
import com.giyeok.bibix.base.*
import com.giyeok.bibix.graph.BibixName
import com.giyeok.bibix.repo.BibixRepo
import com.giyeok.bibix.repo.BibixRepoProto
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
    val implInstance =
      getImplInstance(buildRule.impl, callerProjectId, buildGraphRunner.classPkgRunner)

    withBuildContext(buildGraphRunner, callerProjectId, buildRule, args) { buildContext ->
      // TODO target이 실패한 경우에 repo에 업데이트
      val runner = BuildRuleRunner(
        buildGraphRunner.repo,
        callerProjectId,
        callerImportInstanceId,
        BuildRuleDefContext.from(buildRule)
      ) { result ->
        val finalResult = if (result is BuildTaskResult.ResultWithValue) {
          BuildTaskResult.ValueOfTargetResult(result.value, buildContext.targetId)
        } else {
          result
        }
        block(finalResult)
      }


      val implMethod =
        implInstance::class.java.getMethod(buildRule.implMethodName, BuildContext::class.java)
      implMethod.trySetAccessible()

      BuildTaskResult.LongRunning(
        body = { implMethod.invoke(implInstance, buildContext) },
        after = { runner.handleBuildReturn(it) },
      )
    }
  }
}

fun withBuildContext(
  buildGraphRunner: BuildGraphRunner,
  callerProjectId: Int,
  buildRule: BuildTaskResult.BuildRuleResult,
  args: Map<String, BibixValue>,
  noReuse: Boolean = false,
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
      when (buildRule.impl) {
        is BuildTaskResult.BuildRuleImpl.NativeImpl ->
          this.nativeImpl = empty { }

        is BuildTaskResult.BuildRuleImpl.NonNativeImpl -> {
          this.bibixValueHash = buildRuleImplValueHash {
            val implValueProto = buildRule.impl.classPkg.toBibix().toProto()
            this.implValue = implValueProto
            // impl value에 들어있는 file/direcotry 값들의 해시도 포함
            this.implValueFilesHash =
              buildGraphRunner.fileHashStore.extractInputHashes(implValueProto)
          }
        }
      }
      this.buildRuleClassName = buildRule.buildRuleDef.implClassName
      this.buildRuleMethodName = buildRule.buildRuleDef.implMethodName
    }
    this.argsMap = argsMap
  }
  val targetId = targetIdData.hashString()
  val targetIdHex = targetId.toHexString()

  val inputHashes = buildGraphRunner.fileHashStore.extractInputHashes(argsMap)
  val inputHashString = inputHashes.hashString()

  val repo = buildGraphRunner.repo

  val noReuseModifier = buildRule.buildRuleDef.def.mods.contains(BibixAst.BuildRuleMod.NoReuse)

  val (reuse, prevState) =
    repo.targetStarted(targetIdHex, targetIdData, inputHashes, inputHashString) { prevState ->
      if (noReuse || noReuseModifier) null else {
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
  val repo: BibixRepo,
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
      FinalizeBuildRuleReturnValue(buildRuleDefCtx, it.value, callerProjectId)
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
        BuildTaskResult.LongRunning(
          body = { result.whenDone(evalResult.value) },
          after = {
            check(it is BuildRuleReturn)
            afterThen(it)
          }
        )
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
      val typeDetailsMap = TypeDetailsMap(typeDetails, mapOf())
      BuildTaskResult.LongRunning(
        body = { result.whenDone(typeDetailsMap) },
        after = {
          check(it is BuildRuleReturn)
          afterThen(it)
        }
      )
    }
  }

  private fun handleWithDirectoryLock(
    result: BuildRuleReturn.WithDirectoryLock,
    afterThen: (BuildRuleReturn) -> BuildTaskResult
  ): BuildTaskResult {
    // TODO lock result.directory
    //  지금은 동일 프로세스 내에서만 락을 잡는데.. 제대로 잡는 방법을 찾아보자
    //  build rule에 synchronized 기능 구현하면 별로 필요 없을 지도..
    val directoryLocker = repo.directoryLocker
    return BuildTaskResult.LongRunning(
      preCondition = {
        directoryLocker.acquireLock(result.directory)
      },
      body = { result.withLock() },
      postCondition = {
        directoryLocker.releaseLock(result.directory)
      },
      after = {
        check(it is BuildRuleReturn)
        afterThen(it)
      })
  }
}
