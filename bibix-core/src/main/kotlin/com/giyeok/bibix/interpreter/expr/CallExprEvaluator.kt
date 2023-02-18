package com.giyeok.bibix.interpreter.expr

import com.giyeok.bibix.*
import com.giyeok.bibix.ast.BibixAst
import com.giyeok.bibix.base.*
import com.giyeok.bibix.interpreter.BibixInterpreter
import com.giyeok.bibix.interpreter.PluginImplProvider
import com.giyeok.bibix.interpreter.SourceManager
import com.giyeok.bibix.interpreter.expr.EvaluationResult.RuleDef.ActionRuleDef
import com.giyeok.bibix.interpreter.expr.EvaluationResult.RuleDef.BuildRuleDef
import com.giyeok.bibix.interpreter.task.Task
import com.giyeok.bibix.interpreter.task.TaskRelGraph
import com.giyeok.bibix.repo.*
import com.giyeok.bibix.utils.getOrNull
import com.giyeok.bibix.utils.toKtList
import com.giyeok.bibix.utils.toProto
import com.google.protobuf.empty
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import java.nio.file.Path

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
    context: NameLookupContext,
    params: List<BibixAst.ParamDef>
  ): List<EvaluationResult.Param> =
    params.map { param ->
      check(param.typ().isDefined) { "$param type not set" }
      EvaluationResult.Param(
        param.name(),
        param.optional(),
        exprEvaluator.evaluateType(task, context, param.typ().get()),
        param.defaultValue().getOrNull()
      )
    }

  private suspend fun coerceCpInstance(
    task: Task,
    context: NameLookupContext,
    implValue: BibixValue
  ): ClassInstanceValue {
    val cpType = DataClassType("com.giyeok.bibix.plugins.jvm", "ClassPaths")
    return exprEvaluator.coerce(task, context, implValue, cpType) as ClassInstanceValue
  }

  suspend fun resolveClassDef(
    task: Task,
    definition: Definition.ClassDef
  ): EvaluationResult {
    val context = NameLookupContext(definition.cname).dropLastToken()
    val sourceId = context.sourceId
    val packageName = sourceManager.getPackageName(sourceId)
      ?: throw IllegalStateException("Package name for class ${definition.cname} not specified")
    val className = definition.cname.tokens.joinToString(".")
    return when (definition.classDef) {
      is BibixAst.DataClassDef -> {
        val fields = paramDefs(task, context, definition.classDef.fields().toKtList())
        // TODO handle definition.classDef.body()
        val bodyElems = definition.classDef.body().toKtList()
        EvaluationResult.DataClassDef(context, packageName, className, fields, bodyElems)
      }

      is BibixAst.SuperClassDef -> {
        val subTypes = definition.classDef.subs().toKtList()
        EvaluationResult.SuperClassDef(context, packageName, className, subTypes)
      }

      else -> throw AssertionError()
    }
  }

  suspend fun resolveBuildRule(
    task: Task,
    thisValue: BibixValue?,
    definition: Definition.BuildRule,
  ): BuildRuleDef {
    val buildRule = definition.buildRule

    val defName = definition.cname
    val defCtx = NameLookupContext(defName).dropLastToken()

    val params = paramDefs(task, defCtx, buildRule.params().toKtList())
    val returnType = exprEvaluator.evaluateType(task, defCtx, buildRule.returnType())

    val implTarget = buildRule.impl().targetName().tokens().toKtList()
    val clsName = buildRule.impl().className().tokens().mkString(".")
    val methodName = buildRule.impl().methodName().getOrNull() ?: "build"

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
    thisValue: BibixValue?,
    definition: Definition.ActionRule,
  ): ActionRuleDef {
    val actionRule = definition.actionRule
    val defName = definition.cname

    val defContext = NameLookupContext(defName).dropLastToken()

    val params = paramDefs(task, defContext, actionRule.params().toKtList())

    val implTarget = actionRule.impl().targetName().tokens().toKtList()
    val clsName = actionRule.impl().className().tokens().mkString(".")
    val methodName = actionRule.impl().methodName().getOrNull() ?: "run"

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
    context: NameLookupContext,
    callable: EvaluationResult.Callable,
    params: BibixAst.CallParams,
    thisValue: BibixValue?,
    directBindings: Map<String, BibixValue> = mapOf(),
  ): Map<String, BibixValue> =
    g.withTask(requester, Task.EvalExpr(context.sourceId, params.id(), thisValue)) { task ->
      val paramDefs = callable.params
      val paramDefsMap = callable.params.associateBy { it.name }

      val posParams = params.posParams().toKtList()
      val namedParams = params.namedParams().toKtList().associate { it.name() to it.value() }

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
    context: NameLookupContext,
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

  private suspend fun handlePluginReturnValue(
    task: Task,
    context: NameLookupContext,
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
        val name =
          exprEvaluator.evaluateName(task, context, returnValue.ruleName.split('.'), null, setOf())
        check(name is BuildRuleDef) { "" }
        val params = organizeParamsFromPlugin(task, context, name, returnValue.params, null)
        val result = callBuildRule(task, context, name, params, setOf()).value
        return handlePluginReturnValue(task, context, returnValue.whenDone(result))
      }

      is BuildRuleReturn.GetTypeDetails -> {
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

        val types = returnValue.typeNames.mapNotNull { typeName ->
          val evalResult =
            exprEvaluator.findTypeDefinition(task, typeName.packageName, typeName.typeName)
          evalResult?.let { convertEvaluationResult(evalResult)?.let { typeName to it } }
        }.toMap()
        val relativeNamedTypes = returnValue.relativeNames.mapNotNull { typeName ->
          val typeNameTokens = typeName.split('.').map { it.trim() }
          val evalResult = exprEvaluator.evaluateName(task, context, typeNameTokens, null, setOf())
          convertEvaluationResult(evalResult)?.let { typeName to it }
        }.toMap()
        val typeDetailsMap = TypeDetailsMap(types, relativeNamedTypes)
        return handlePluginReturnValue(task, context, returnValue.whenDone(typeDetailsMap))
      }

      is BuildRuleReturn.WithDirectoryLock -> {
        val nextValue = directoryLocker.withLock(returnValue.directory, returnValue.withLock)
        return handlePluginReturnValue(task, context, nextValue)
      }

      else -> throw IllegalStateException("Unknown return value: $returnValue")
    }
  }

  private suspend fun handleNClassInstanceValue(
    task: Task,
    context: NameLookupContext,
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
      is ExternSourceId -> sourceManager.getProjectRoot(sourceId)
      else -> sourceManager.mainBaseDirectory
    }

  private suspend fun getRuleImplValue(
    task: Task,
    buildRule: BuildRuleDef.UserBuildRuleDef
  ): ClassInstanceValue {
    val impl = exprEvaluator.evaluateName(
      task,
      buildRule.context,
      buildRule.implTarget,
      buildRule.thisValue,
      setOf()
    ).ensureValue()
    return coerceCpInstance(task, buildRule.context, impl)
  }

  private suspend fun getRuleImplInstance(
    callerSourceId: SourceId,
    task: Task,
    buildRule: BuildRuleDef
  ): Any =
    when (buildRule) {
      is BuildRuleDef.NativeBuildRuleDef -> buildRule.implInstance
      is BuildRuleDef.UserBuildRuleDef -> {
        val cpInstance = getRuleImplValue(task, buildRule)
        pluginImplProvider.getPluginImplInstance(callerSourceId, cpInstance, buildRule.className)
      }
    }

  suspend fun hash(
    task: Task,
    callingSourceId: SourceId,
    buildRule: BuildRuleDef,
    params: Map<String, BibixValue>
  ): ObjectHash {
    val argsMap = params.toArgsMapProto()
    val inputHashes = argsMap.extractInputHashes()
    val objectId = objectId {
      this.callingSourceId = protoOf(callingSourceId)
      when (buildRule) {
        is BuildRuleDef.NativeBuildRuleDef ->
          this.bibixVersion = Constants.BIBIX_VERSION

        is BuildRuleDef.UserBuildRuleDef ->
          this.ruleImplObjhash = getRuleImplValue(task, buildRule).hashString()
      }
      this.ruleSourceId = protoOf(buildRule.name.sourceId)
      this.ruleName = buildRule.name.tokens.joinToString(".")
      this.className = buildRule.className
      this.methodName = buildRule.methodName
      this.argsMap = argsMap
    }
    return ObjectHash(objectId, inputHashes, objectId.hashString())
  }

  private fun protoOf(sourceId: SourceId): BibixIdProto.SourceId = when (sourceId) {
    PreludeSourceId -> sourceId { this.preloadedPlugin = "" }
    MainSourceId -> sourceId { this.mainSource = empty { } }
    is PreloadedSourceId -> sourceId { this.preloadedPlugin = sourceId.name }
    is ExternSourceId -> sourceId {
      this.externPluginObjhash = externalBibixProject {
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
    context: NameLookupContext,
    buildRule: BuildRuleDef,
    params: Map<String, BibixValue>,
    outputNames: Set<CName>,
  ): BibixValueWithObjectHash {
    val objHash = hash(task, context.sourceId, buildRule, params)

    val result = g.withMemo(objHash) { objHash ->
      val pluginInstance = getRuleImplInstance(context.sourceId, task, buildRule)
      val method =
        pluginInstance::class.java.getMethod(buildRule.methodName, BuildContext::class.java)

      val progressIndicator = interpreter.progressIndicatorContainer.ofCurrentThread()
      val buildContext = BuildContext(
        buildEnv = interpreter.buildEnv,
        fileSystem = interpreter.repo.fileSystem,
        mainBaseDirectory = sourceManager.getProjectRoot(MainSourceId),
        callerBaseDirectory = baseDirectoryOf(context.sourceId),
        ruleDefinedDirectory = baseDirectoryOf(buildRule.context.sourceId),
        arguments = params,
        hashChanged = true, // TODO
        objectId = objHash.objectId,
        objectIdHash = objHash.hashHexString,
        destDirectoryPath = interpreter.repo.objectsDirectory.resolve(objHash.hashHexString),
        progressLogger = progressIndicator,
        repo = interpreter.repo,
      )

      check(method.trySetAccessible())

      progressIndicator.logInfo("Calling ${buildRule.context}...")
      val returnValue = try {
        method.invoke(pluginInstance, buildContext)
      } catch (e: Exception) {
        throw IllegalStateException("Error from the plugin", e)
      }
      progressIndicator.logInfo("Continuing from ${buildRule.context}...")

      val finalValue = handlePluginReturnValue(task, buildRule.context, returnValue)

      exprEvaluator.coerce(task, buildRule.context, finalValue, buildRule.returnType)
    }

    outputNames.filter { it.sourceId == MainSourceId }.forEach { outputName ->
      if (g.addOutputMemo(outputName, objHash)) {
        interpreter.repo.linkNameToObject(outputName.tokens, objHash)
      }
    }

    return result
  }

  suspend fun evaluateCallExpr(
    requester: Task,
    context: NameLookupContext,
    expr: BibixAst.CallExpr,
    thisValue: BibixValue?,
    outputNames: Set<CName>,
  ): BibixValueWithObjectHash =
    g.withTask(requester, Task.EvalCallExpr(context.sourceId, expr.id(), thisValue)) { task ->
      when (val callTarget =
        exprEvaluator.evaluateName(task, context, expr.name(), null, outputNames)) {
        is BuildRuleDef -> {
          val params =
            organizeParams(task, context, callTarget, expr.params(), thisValue)
          callBuildRule(task, context, callTarget, params, outputNames)
        }

        is EvaluationResult.DataClassDef -> {
          val params =
            organizeParams(task, context, callTarget, expr.params(), thisValue)
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
          coerceCpInstance(task, actionRule.context, impl),
          actionRule.className
        )
      }
    }

  suspend fun executeActionCallExpr(
    requester: Task,
    context: NameLookupContext,
    expr: BibixAst.CallExpr,
    args: Pair<String, List<String>>?
  ): Unit = g.withTask(requester, Task.ExecuteActionCall(context.sourceId, expr.id())) { task ->
    // TODO handle actionArgs
    val callTarget = exprEvaluator.evaluateName(task, context, expr.name(), null, setOf())

    check(callTarget is ActionRuleDef) { "TODO message" }

    val directBindings =
      if (args == null) mapOf() else mapOf(args.first to ListValue(args.second.map { StringValue(it) }))
    val params = organizeParams(task, context, callTarget, expr.params(), null, directBindings)

    val pluginInstance = getActionImplInstance(context.sourceId, task, callTarget)
    val method =
      pluginInstance::class.java.getMethod(callTarget.methodName, ActionContext::class.java)

    val progressIndicator = interpreter.progressIndicatorContainer.ofCurrentThread()
    val actionContext = ActionContext(interpreter.buildEnv, params, progressIndicator)

    progressIndicator.logInfo("Calling ${callTarget.context}...")
    try {
      method.invoke(pluginInstance, actionContext)
    } catch (e: Exception) {
      throw IllegalStateException("Error from the plugin", e)
    }
    progressIndicator.logInfo("Continuing from ${callTarget.context}...")
  }
}
