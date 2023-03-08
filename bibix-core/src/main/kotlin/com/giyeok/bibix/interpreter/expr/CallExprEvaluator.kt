package com.giyeok.bibix.interpreter.expr

import com.giyeok.bibix.*
import com.giyeok.bibix.BibixIdProto.TargetIdData
import com.giyeok.bibix.ast.BibixAst
import com.giyeok.bibix.base.*
import com.giyeok.bibix.interpreter.BibixInterpreter
import com.giyeok.bibix.interpreter.ExprEvalContext
import com.giyeok.bibix.interpreter.PluginImplProvider
import com.giyeok.bibix.interpreter.SourceManager
import com.giyeok.bibix.interpreter.expr.EvaluationResult.RuleDef.ActionRuleDef
import com.giyeok.bibix.interpreter.expr.EvaluationResult.RuleDef.BuildRuleDef
import com.giyeok.bibix.interpreter.task.Task
import com.giyeok.bibix.interpreter.task.TaskRelGraph
import com.giyeok.bibix.plugins.jvm.ClassPkg
import com.giyeok.bibix.plugins.jvm.LocalLib
import com.giyeok.bibix.plugins.jvm.MavenDep
import com.giyeok.bibix.plugins.jvm.ResolveClassPkgs
import com.giyeok.bibix.plugins.jvm.ResolveClassPkgs.Companion.toPaths
import com.giyeok.bibix.repo.*
import com.giyeok.bibix.utils.toBibix
import com.giyeok.bibix.utils.toInstant
import com.giyeok.bibix.utils.toProto
import com.google.protobuf.empty
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import java.nio.file.Path
import kotlin.io.path.absolute
import kotlin.io.path.absolutePathString
import kotlin.io.path.name

class CallExprEvaluator(
  private val interpreter: BibixInterpreter,
  private val g: TaskRelGraph,
  private val sourceManager: SourceManager,
  private val exprEvaluator: ExprEvaluator,
  private val pluginImplProvider: PluginImplProvider,
  private val directoryLocker: DirectoryLocker,
) {
  private suspend fun paramDefs(
    task: Task,
    context: ExprEvalContext,
    params: List<BibixAst.ParamDef>
  ): List<EvaluationResult.Param> =
    params.map { param ->
      checkNotNull(param.typ) { "$param type not set" }
      EvaluationResult.Param(
        param.name,
        param.optional,
        exprEvaluator.evaluateType(task, context, param.typ!!),
        param.defaultValue
      )
    }

  private suspend fun prepareClassPathsForPlugin(
    task: Task,
    context: ExprEvalContext,
    implValue: BibixValue
  ): List<Path> {
    val cpType = DataClassType("com.giyeok.bibix.plugins.jvm", "ClassPkg")
    val classPkgValue = exprEvaluator.coerce(task, context, implValue, cpType) as ClassInstanceValue
    val classPkg = ClassPkg.fromBibix(classPkgValue)

    // bibix.base 와 및 kotlin sdk 관련 classpath 들은 cpInstance의 것을 사용하지 *않고* bibix runtime의 것을 사용해야 한다
    // 그래서 classPkg에서 그 부분은 빼고 classpath 목록 반환
    val cpsMap = ResolveClassPkgs.cpsMap(listOf(classPkg))
      // TODo bibix.base를 maven artifact로 넣어서 필터링하는게 나을듯
      .filterNot { it.key is LocalLib && (it.key as LocalLib).path.fileName.name.startsWith("bibix-base-") }
    val mavenVersions = ResolveClassPkgs.mavenArtifactVersionsToUse(listOf(classPkg))
      .filterNot {
        (it.key.group == "org.jetbrains.kotlin" && it.key.artifact == "kotlin-stdlib") ||
          (it.key.group == "org.jetbrains.kotlin" && it.key.artifact == "kotlin-stdlib-jdk7") ||
          (it.key.group == "org.jetbrains.kotlin" && it.key.artifact == "kotlin-stdlib-jdk8") ||
          (it.key.group == "org.jetbrains.kotlin" && it.key.artifact == "kotlin-stdlib-common") ||
          (it.key.group == "org.jetbrains.kotlin" && it.key.artifact == "kotlin-reflect") ||
          (it.key.group == "org.jetbrains.kotlinx" && it.key.artifact == "kotlinx-coroutines-core") ||
          (it.key.group == "org.jetbrains.kotlinx" && it.key.artifact == "kotlinx-coroutines-jdk7") ||
          (it.key.group == "org.jetbrains.kotlinx" && it.key.artifact == "kotlinx-coroutines-jdk8")
      }

    val packs =
      cpsMap.filter { it.key !is MavenDep } + mavenVersions.values.associateWith { cpsMap[it] }
    return packs.mapNotNull { it.value?.toPaths() }.flatten().map { it.absolute() }.distinct()
  }

  suspend fun resolveClassDef(
    task: Task,
    varsContext: VarsContext,
    definition: Definition.ClassDef
  ): EvaluationResult {
    val context = ExprEvalContext(NameLookupContext(definition.cname).dropLastToken(), varsContext)
    val sourceId = context.sourceId
    val packageName = sourceManager.getPackageName(sourceId)
      ?: throw IllegalStateException("Package name for class ${definition.cname} not specified")
    val className = definition.cname.tokens.joinToString(".")
    return when (definition.classDef) {
      is BibixAst.DataClassDef -> {
        val fields = paramDefs(task, context, definition.classDef.fields)
        // TODO handle definition.classDef.body()
        val bodyElems = definition.classDef.body
        EvaluationResult.DataClassDef(context, packageName, className, fields, bodyElems)
      }

      is BibixAst.SuperClassDef -> {
        val subTypes = definition.classDef.subs
        EvaluationResult.SuperClassDef(context, packageName, className, subTypes)
      }

      else -> throw AssertionError()
    }
  }

  suspend fun resolveBuildRule(
    task: Task,
    varsContext: VarsContext,
    thisValue: BibixValue?,
    definition: Definition.BuildRule,
  ): BuildRuleDef {
    val buildRule = definition.buildRule

    val defName = definition.cname
    val defCtx = ExprEvalContext(NameLookupContext(defName).dropLastToken(), varsContext)

    val params = paramDefs(task, defCtx, buildRule.params)
    val returnType = exprEvaluator.evaluateType(task, defCtx, buildRule.returnType)

    val implTarget = buildRule.impl.targetName.tokens
    val clsName = buildRule.impl.className.tokens.joinToString(".")
    val methodName = buildRule.impl.methodName ?: "build"

    if (defName.sourceId is PreloadedSourceId && implTarget == listOf("native")) {
      val implInstance =
        sourceManager.getPreloadedPluginInstance(defName.sourceId as PreloadedSourceId, clsName)
      return BuildRuleDef.NativeBuildRuleDef(
        defName,
        defCtx,
        params,
        returnType,
        implInstance,
        methodName
      )
    }
    if (defName.sourceId is PreludeSourceId && implTarget == listOf("native")) {
      val implInstance = sourceManager.getPreludePluginInstance(clsName)
      return BuildRuleDef.NativeBuildRuleDef(
        defName,
        defCtx,
        params,
        returnType,
        implInstance,
        methodName
      )
    }

    return BuildRuleDef.UserBuildRuleDef(
      defName,
      defCtx,
      implTarget,
      thisValue,
      params,
      returnType,
      clsName,
      methodName
    )
  }

  suspend fun resolveActionRule(
    task: Task,
    varsContext: VarsContext,
    thisValue: BibixValue?,
    definition: Definition.ActionRule,
  ): ActionRuleDef {
    val actionRule = definition.actionRule
    val defName = definition.cname

    val defContext = ExprEvalContext(NameLookupContext(defName).dropLastToken(), varsContext)

    val params = paramDefs(task, defContext, actionRule.params)

    val implTarget = actionRule.impl.targetName.tokens
    val clsName = actionRule.impl.className.tokens.joinToString(".")
    val methodName = actionRule.impl.methodName ?: "run"

    if (defName.sourceId is PreloadedSourceId && implTarget == listOf("native")) {
      val implInstance =
        sourceManager.getPreloadedPluginInstance(defName.sourceId as PreloadedSourceId, clsName)
      return ActionRuleDef.PreloadedActionRuleDef(
        defName,
        defContext,
        params,
        implInstance,
        methodName
      )
    }
    if (defName.sourceId is PreludeSourceId && implTarget == listOf("native")) {
      val implInstance = sourceManager.getPreludePluginInstance(clsName)
      return ActionRuleDef.PreloadedActionRuleDef(
        defName,
        defContext,
        params,
        implInstance,
        methodName
      )
    }

    return ActionRuleDef.UserActionRuleDef(
      defName,
      defContext,
      implTarget,
      thisValue,
      params,
      clsName,
      methodName
    )
  }

  suspend fun <K, V> Map<K, Deferred<V>>.awaitAllValues(): Map<K, V> =
    this.mapValues { (_, deferred) -> deferred.await() }

  private suspend fun organizeParams(
    requester: Task,
    context: ExprEvalContext,
    callable: EvaluationResult.Callable,
    params: BibixAst.CallParams,
    thisValue: BibixValue?,
    directBindings: Map<String, BibixValue> = mapOf(),
  ): Map<String, BibixValue> =
    g.withTask(requester, g.evalExprTask(context.sourceId, params, thisValue)) { task ->
      val paramDefs = callable.params
      val paramDefsMap = callable.params.associateBy { it.name }

      val posParams = params.posParams
      val namedParams = params.namedParams.associate { it.name to it.value }

      val posParamsMap = paramDefs.zip(posParams).associate { it.first.name to it.second }
      check(posParams.size <= paramDefs.size) { "Unknown positional parameters" }
      val remainingParamDefs = paramDefs.drop(posParams.size)

      val unknownParams = namedParams.keys - remainingParamDefs.map { it.name }.toSet()
      check(unknownParams.isEmpty()) {
        "Unknown parameters: $unknownParams at $params (${sourceManager.descriptionOf(context.sourceId)})"
      }

      val unspecifiedParams = remainingParamDefs.map { it.name }.toSet() - namedParams.keys
      val missingParams = unspecifiedParams.filter { paramName ->
        val paramDef = paramDefsMap.getValue(paramName)
        !paramDef.optional && paramDef.defaultValue == null
      }
      check(missingParams.isEmpty()) { "Required parameters $missingParams not specified at $params" }

      val defaultParamsMap = unspecifiedParams.mapNotNull { paramName ->
        val paramDef = paramDefsMap.getValue(paramName)
        paramDef.defaultValue?.let { paramName to paramDef.defaultValue }
      }.toMap()
      val noneParams = unspecifiedParams - defaultParamsMap.keys

      // Run concurrently
      val paramValues = (posParamsMap + namedParams).mapValues { (paramName, valueExpr) ->
        val paramDef = paramDefsMap.getValue(paramName)
        async {
          // string -> path로 변환할 때의 기준 디렉토리는 coercion이 일어나는 source id를 기준으로
          val value =
            exprEvaluator.evaluateExpr(task, context, valueExpr, thisValue, directBindings, setOf())
              .ensureValue()
          if (paramDef.optional && value == NoneValue) NoneValue else {
            exprEvaluator.coerce(task, context, value, paramDef.type)
          }
        }
      }
      val defaultParamValues = defaultParamsMap.mapValues { (paramName, valueExpr) ->
        val paramDef = paramDefsMap.getValue(paramName)
        async {
          // 여기선 directBindings 사용하지 말아야 함
          val value =
            exprEvaluator.evaluateExpr(task, callable.context, valueExpr, thisValue, setOf())
              .ensureValue()
          if (paramDef.optional && value == NoneValue) NoneValue else {
            exprEvaluator.coerce(task, callable.context, value, paramDef.type)
          }
        }
      }

      (paramValues + defaultParamValues).awaitAllValues() + noneParams.associateWith { NoneValue }
    }

  // TODO organizeParams와 겹치는 코드 리팩토링
  private suspend fun organizeParamsFromPlugin(
    requester: Task,
    context: ExprEvalContext,
    callable: EvaluationResult.Callable,
    namedParamsMap: Map<String, BibixValue>,
    thisValue: BibixValue?,
  ): Map<String, BibixValue> =
    g.withTask(requester, Task.PluginRequestedCallExpr(context.sourceId, -1)) { task ->
      val paramDefsMap = callable.params.associateBy { it.name }

      val unknownParams = namedParamsMap.keys - paramDefsMap.keys
      check(unknownParams.isEmpty()) { "Unknown parameters: $unknownParams by plugin" }

      val unspecifiedParams = paramDefsMap.keys - namedParamsMap.keys
      val missingParams = unspecifiedParams.filter { paramName ->
        val paramDef = paramDefsMap.getValue(paramName)
        !paramDef.optional && paramDef.defaultValue == null
      }
      check(missingParams.isEmpty()) { "Required parameters $missingParams not specified by plugin" }

      val defaultParamsMap = unspecifiedParams.mapNotNull { paramName ->
        val paramDef = paramDefsMap.getValue(paramName)
        paramDef.defaultValue?.let { paramName to paramDef.defaultValue }
      }.toMap()
      val noneParams = unspecifiedParams - defaultParamsMap.keys

      val paramValues = namedParamsMap.mapValues { (paramName, value) ->
        val paramDef = paramDefsMap.getValue(paramName)
        async {
          if (paramDef.optional && value == NoneValue) NoneValue else {
            exprEvaluator.coerce(task, context, value, paramDef.type)
          }
        }
      }
      val defaultParamValues = defaultParamsMap.mapValues { (paramName, valueExpr) ->
        val paramDef = paramDefsMap.getValue(paramName)
        async {
          // 여기선 directBindings 사용하지 말아야 함
          val value =
            exprEvaluator.evaluateExpr(task, callable.context, valueExpr, thisValue, setOf())
              .ensureValue()
          if (paramDef.optional && value == NoneValue) NoneValue else {
            exprEvaluator.coerce(task, callable.context, value, paramDef.type)
          }
        }
      }

      (paramValues + defaultParamValues).awaitAllValues() + noneParams.associateWith { NoneValue }
    }

  private suspend fun getTypeDetails(
    task: Task,
    context: ExprEvalContext,
    typeNames: List<TypeName>,
    relativeNames: List<String>
  ): TypeDetailsMap {
    fun convertEvaluationResult(result: EvaluationResult): TypeDetails? =
      when (result) {
        is EvaluationResult.DataClassDef -> {
          val fields = result.params.map { it.toRuleParamValue() }
          DataClassTypeDetails(result.packageName, result.className, fields)
        }

        is EvaluationResult.SuperClassDef ->
          SuperClassTypeDetails(result.packageName, result.className, result.subClasses)

        is EvaluationResult.EnumDef ->
          EnumTypeDetails(result.packageName, result.enumName, result.enumValues)

        else -> null
      }

    val types = typeNames.mapNotNull { typeName ->
      val evalResult =
        exprEvaluator.findTypeDefinition(task, typeName.packageName, typeName.typeName)
      evalResult?.let { convertEvaluationResult(evalResult)?.let { typeName to it } }
    }.toMap()
    val relativeNamedTypes = relativeNames.mapNotNull { typeName ->
      val typeNameTokens = typeName.split('.').map { it.trim() }
      val evalResult = exprEvaluator.evaluateName(task, context, typeNameTokens, null, setOf())
      convertEvaluationResult(evalResult)?.let { typeName to it }
    }.toMap()
    return TypeDetailsMap(types, relativeNamedTypes)
  }

  private suspend fun callBuildRuleFromPlugin(
    task: Task,
    context: ExprEvalContext,
    ruleName: String,
    params: Map<String, BibixValue>,
  ): BibixValue {
    val name =
      exprEvaluator.evaluateName(task, context, ruleName.split('.'), null, setOf())
    check(name is BuildRuleDef) { "" }
    val params = organizeParamsFromPlugin(task, context, name, params, null)
    return callBuildRule(task, context, name, params, setOf()).value
  }

  private suspend fun handleBuildRuleReturnValue(
    task: Task,
    context: ExprEvalContext,
    returnValue: Any,
  ): BibixValue {
    when (returnValue) {
      is BibixValue ->
        return handleNClassInstanceValue(task, context, returnValue)

      is BuildRuleReturn.ValueReturn ->
        return handleNClassInstanceValue(task, context, returnValue.value)

      is BuildRuleReturn.FailedReturn ->
        throw returnValue.exception

      is BuildRuleReturn.DoneReturn ->
        throw IllegalStateException("Done is not allowed for build rule")

      is BuildRuleReturn.EvalAndThen -> {
        val result =
          callBuildRuleFromPlugin(task, context, returnValue.ruleName, returnValue.params)
        return handleBuildRuleReturnValue(task, context, returnValue.whenDone(result))
      }

      is BuildRuleReturn.GetTypeDetails -> {
        val typeDetailsMap =
          getTypeDetails(task, context, returnValue.typeNames, returnValue.relativeNames)
        return handleBuildRuleReturnValue(task, context, returnValue.whenDone(typeDetailsMap))
      }

      is BuildRuleReturn.WithDirectoryLock -> {
        val nextValue = directoryLocker.withLock(returnValue.directory, returnValue.withLock)
        return handleBuildRuleReturnValue(task, context, nextValue)
      }

      else -> throw IllegalStateException("Unknown return value: $returnValue")
    }
  }

  private suspend fun handleNClassInstanceValue(
    task: Task,
    context: ExprEvalContext,
    value: BibixValue
  ): BibixValue = when (value) {
    is NClassInstanceValue -> {
      val classType = exprEvaluator.evaluateName(task, context, value.nameTokens, null, setOf())
      check(classType is EvaluationResult.DataClassDef) { "Invalid NClassInstanceValue: $value" }
      val fieldValues = value.fieldValues.mapValues { (_, value) ->
        handleNClassInstanceValue(task, context, value)
      }
      ClassInstanceValue(classType.packageName, classType.className, fieldValues)
    }

    is ClassInstanceValue -> ClassInstanceValue(value.packageName, value.className,
      value.fieldValues.mapValues { (_, value) ->
        handleNClassInstanceValue(task, context, value)
      })

    is ListValue ->
      ListValue(value.values.map { handleNClassInstanceValue(task, context, it) })

    is NamedTupleValue ->
      NamedTupleValue(value.pairs.map { (name, value) ->
        name to handleNClassInstanceValue(task, context, value)
      })

    is SetValue ->
      SetValue(value.values.map { handleNClassInstanceValue(task, context, it) })

    is TupleValue ->
      TupleValue(value.values.map { handleNClassInstanceValue(task, context, it) })

    else -> value
  }

  private suspend fun baseDirectoryOf(sourceId: SourceId): Path =
    when (sourceId) {
      is ExternSourceId -> sourceManager.getProjectRoot(sourceId)!!
      else -> sourceManager.mainBaseDirectory
    }

  private suspend fun protoOf(sourceId: SourceId): BibixIdProto.SourceId = when (sourceId) {
    PreludeSourceId -> sourceId { this.preludeSource = empty {} }
    MainSourceId -> sourceId { this.mainSource = empty { } }
    is PreloadedSourceId -> sourceId { this.preloadedPlugin = sourceId.name }
    is ExternSourceId -> sourceId {
      this.externalPluginObjhash = externalBibixProject {
        this.rootDirectory = sourceManager.getProjectRoot(sourceId)!!.absolutePathString()
        // TODO
      }
    }
  }

  fun Map<String, BibixValue>.toArgsMapProto(): BibixIdProto.ArgsMap {
    val value = this
    return argsMap {
      this.pairs.addAll(value.entries.toList()
        .sortedBy { it.key }
        .map {
          argPair {
            this.name = it.key
            this.value = it.value.toProto()
          }
        })
    }
  }

  private suspend fun callBuildRule(
    task: Task,
    context: ExprEvalContext,
    buildRule: BuildRuleDef,
    params: Map<String, BibixValue>,
    outputNames: Set<CName>,
  ): BibixValueWithObjectHash {
    val argsMap = params.toArgsMapProto()
    val inputHashes = argsMap.extractInputHashes()

    val pluginInstance: Any

    val targetIdDataBuilder = TargetIdData.newBuilder().also {
      it.sourceId = protoOf(context.sourceId)
      it.buildRuleSourceId = protoOf(buildRule.context.sourceId)
      // it.targetName = "??"
      // it.buildRule will be set afterward
      it.buildRuleClassName = buildRule.className
      it.buildRuleMethodName = buildRule.methodName
      it.argsMap = argsMap
    }

    when (buildRule) {
      is BuildRuleDef.NativeBuildRuleDef -> {
        pluginInstance = buildRule.implInstance
        targetIdDataBuilder.nativeImpl = empty {}
      }

      is BuildRuleDef.UserBuildRuleDef -> {
        val impl = exprEvaluator.evaluateName(
          task,
          buildRule.context,
          buildRule.implTarget,
          buildRule.thisValue,
          setOf()
        )

        impl as EvaluationResult.ValueWithTargetId

        pluginInstance = pluginImplProvider.getPluginImplInstance(
          context.sourceId,
          prepareClassPathsForPlugin(task, buildRule.context, impl.ensureValue()),
          buildRule.className
        )

        targetIdDataBuilder.buildRuleImplId = buildRuleImplId {
          this.sourceId = protoOf(buildRule.name.sourceId)
          this.targetId = impl.objectHash.targetId.targetIdBytes
          this.objectId = impl.objectHash.objectId.objectIdBytes
        }
      }
    }

    val targetId = TargetId(targetIdDataBuilder.build())
    val objHash = ObjectHash(targetId, inputHashes)

    val targetIdBytes = objHash.targetId.targetIdBytes
    val prevInputHashes = interpreter.repo.getPrevInputsHashOf(targetIdBytes)
    val prevState = interpreter.repo.getPrevTargetState(targetIdBytes)
    val prevSucceeded = prevState?.let {
      if (it.stateCase != BibixRepoProto.TargetState.StateCase.BUILD_SUCCEEDED) null else {
        it.buildSucceeded
      }
    }

    interpreter.repo.putObjectHash(objHash)

    val result = g.withMemo(objHash) {
      interpreter.repo.startBuildingTarget(targetIdBytes, objHash)

      val method =
        pluginInstance::class.java.getMethod(buildRule.methodName, BuildContext::class.java)

      val hashChanged =
        prevInputHashes == null || prevInputHashes != objHash.inputsHash
      val progressIndicator = interpreter.progressIndicatorContainer.ofCurrentThread()
      val buildContext = BuildContext(
        buildEnv = interpreter.buildEnv,
        fileSystem = interpreter.repo.fileSystem,
        mainBaseDirectory = sourceManager.getProjectRoot(MainSourceId)!!,
        callerBaseDirectory = baseDirectoryOf(context.sourceId),
        ruleDefinedDirectory = baseDirectoryOf(buildRule.context.sourceId),
        arguments = params,
        targetIdData = objHash.targetId.targetIdData,
        targetId = objHash.targetId.targetIdHex,
        hashChanged = hashChanged,
        prevBuildTime = prevSucceeded?.buildEndTime?.toInstant(),
        prevResult = prevSucceeded?.resultValue?.toBibix(),
        destDirectoryPath = interpreter.repo.objectsDirectory.resolve(objHash.targetId.targetIdHex),
        progressLogger = progressIndicator,
        repo = interpreter.repo,
      )

      check(method.trySetAccessible())

      progressIndicator.logInfo("Calling ${buildRule.context}...")
      val returnValue = try {
        method.invoke(pluginInstance, buildContext)
      } catch (e: Exception) {
        interpreter.repo.targetBuildingFailed(targetIdBytes, e.message ?: "")
        throw IllegalStateException("Error from the plugin", e)
      }
      progressIndicator.logInfo("Continuing from ${buildRule.context}...")

      val finalValue = handleBuildRuleReturnValue(task, buildRule.context, returnValue)

      exprEvaluator.coerce(task, buildRule.context, finalValue, buildRule.returnType)
    }

    interpreter.repo.targetBuildingSucceeded(targetIdBytes, result.value)

    outputNames.filter { it.sourceId == MainSourceId }.forEach { outputName ->
      if (g.addOutputMemo(outputName, objHash)) {
        interpreter.repo.linkNameToObject(outputName.tokens, objHash)
      }
    }

    return result
  }

  suspend fun evaluateCallExpr(
    requester: Task,
    context: ExprEvalContext,
    expr: BibixAst.CallExpr,
    thisValue: BibixValue?,
    outputNames: Set<CName>,
  ): BibixValueWithObjectHash =
    g.withTask(requester, g.evalCallExprTask(context.sourceId, expr, thisValue)) { task ->
      when (val callTarget =
        exprEvaluator.evaluateName(task, context, expr.name, null, outputNames)) {
        is BuildRuleDef -> {
          val params =
            organizeParams(task, context, callTarget, expr.params, thisValue)
          callBuildRule(task, context, callTarget, params, outputNames)
        }

        is EvaluationResult.DataClassDef -> {
          val params =
            organizeParams(task, context, callTarget, expr.params, thisValue)
          val value = ClassInstanceValue(callTarget.packageName, callTarget.className, params)
          BibixValueWithObjectHash(value, null)
        }

        else -> throw IllegalStateException(
          "Invalid call target $expr at ${sourceManager.descriptionOf(context.sourceId)}"
        )
      }
    }

  private suspend fun getActionImplInstance(
    callerSourceId: SourceId,
    task: Task,
    actionRule: ActionRuleDef
  ): Any =
    when (actionRule) {
      is ActionRuleDef.PreloadedActionRuleDef -> actionRule.implInstance
      is ActionRuleDef.UserActionRuleDef -> {
        val impl = exprEvaluator.evaluateName(
          task,
          actionRule.context,
          actionRule.implTarget,
          actionRule.thisValue,
          setOf(),
        ).ensureValue()
        pluginImplProvider.getPluginImplInstance(
          callerSourceId,
          prepareClassPathsForPlugin(task, actionRule.context, impl),
          actionRule.className
        )
      }
    }

  private suspend fun handleActionReturnValue(
    task: Task,
    context: ExprEvalContext,
    returnValue: Any,
  ) {
    when (returnValue) {
      is BibixValue, is BuildRuleReturn.ValueReturn ->
        throw IllegalStateException("Value return is not allowed for action rule")

      is BuildRuleReturn.FailedReturn ->
        throw returnValue.exception

      is BuildRuleReturn.DoneReturn -> {
        // Do nothing
      }

      is BuildRuleReturn.EvalAndThen -> {
        val result =
          callBuildRuleFromPlugin(task, context, returnValue.ruleName, returnValue.params)
        handleActionReturnValue(task, context, returnValue.whenDone(result))
      }

      is BuildRuleReturn.GetTypeDetails -> {
        val typeDetailsMap =
          getTypeDetails(task, context, returnValue.typeNames, returnValue.relativeNames)
        handleActionReturnValue(task, context, returnValue.whenDone(typeDetailsMap))
      }

      is BuildRuleReturn.WithDirectoryLock -> {
        val nextValue = directoryLocker.withLock(returnValue.directory, returnValue.withLock)
        handleActionReturnValue(task, context, nextValue)
      }

      else -> throw IllegalStateException("Unknown return value: $returnValue")
    }
  }

  suspend fun executeActionCallExpr(
    requester: Task,
    context: ExprEvalContext,
    expr: BibixAst.CallExpr,
    args: Pair<String, List<String>>?
  ): Unit = g.withTask(requester, Task.ExecuteActionCall(context.sourceId, expr.nodeId)) { task ->
    // TODO handle actionArgs
    val callTarget = exprEvaluator.evaluateName(task, context, expr.name, null, setOf())

    check(callTarget is ActionRuleDef) { "TODO message" }

    val directBindings =
      if (args == null) mapOf() else mapOf(args.first to ListValue(args.second.map { StringValue(it) }))
    val params = organizeParams(task, context, callTarget, expr.params, null, directBindings)

    val pluginInstance = getActionImplInstance(context.sourceId, task, callTarget)
    val method =
      pluginInstance::class.java.getMethod(callTarget.methodName, ActionContext::class.java)

    val progressIndicator = interpreter.progressIndicatorContainer.ofCurrentThread()
    val actionContext = ActionContext(interpreter.buildEnv, params, progressIndicator)

    progressIndicator.logInfo("Calling ${callTarget.context}...")

    if (method.returnType == Void.TYPE) {
      try {
        method.invoke(pluginInstance, actionContext)
      } catch (e: Exception) {
        throw IllegalStateException("Error from the plugin", e)
      }
    } else {
      val returnValue = try {
        method.invoke(pluginInstance, actionContext)
      } catch (e: Exception) {
        throw IllegalStateException("Error from the plugin", e)
      }
      handleActionReturnValue(task, callTarget.context, returnValue)
    }

    progressIndicator.logInfo("Continuing from ${callTarget.context}...")
  }
}
