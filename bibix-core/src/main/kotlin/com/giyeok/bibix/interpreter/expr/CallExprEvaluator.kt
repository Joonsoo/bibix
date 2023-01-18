package com.giyeok.bibix.interpreter.expr

import com.giyeok.bibix.ast.BibixAst
import com.giyeok.bibix.base.*
import com.giyeok.bibix.interpreter.BibixInterpreter
import com.giyeok.bibix.interpreter.RealmProvider
import com.giyeok.bibix.interpreter.SourceManager
import com.giyeok.bibix.interpreter.expr.EvaluationResult.RuleDef.ActionRuleDef
import com.giyeok.bibix.interpreter.expr.EvaluationResult.RuleDef.BuildRuleDef
import com.giyeok.bibix.interpreter.hash.BibixValueWithObjectHash
import com.giyeok.bibix.interpreter.hash.ObjectHasher
import com.giyeok.bibix.interpreter.task.Task
import com.giyeok.bibix.interpreter.task.TaskRelGraph
import com.giyeok.bibix.utils.await
import com.giyeok.bibix.utils.getOrNull
import com.giyeok.bibix.utils.toKtList
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import java.nio.channels.AsynchronousFileChannel
import java.nio.file.Path
import java.nio.file.StandardOpenOption

class CallExprEvaluator(
  private val interpreter: BibixInterpreter,
  private val g: TaskRelGraph,
  private val sourceManager: SourceManager,
  private val exprEvaluator: ExprEvaluator,
  private val realmProvider: RealmProvider
) {
  private val objectHasher = ObjectHasher(interpreter)

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
  ): EvaluationResult {
    val buildRule = definition.buildRule

    val defName = definition.cname
    val defCtx = NameLookupContext(defName).dropLastToken()

    val params = paramDefs(task, defCtx, buildRule.params().toKtList())
    val returnType = exprEvaluator.evaluateType(task, defCtx, buildRule.returnType())

    val implTarget = buildRule.impl().targetName().tokens().toKtList()
    val clsName = buildRule.impl().className().tokens().mkString(".")
    val methodName = buildRule.impl().methodName().getOrNull() ?: "build"

    if (defName.sourceId is PreloadedSourceId && implTarget == listOf("native")) {
      val cls =
        sourceManager.getPreloadedPluginClass(defName.sourceId as PreloadedSourceId, clsName)
      return BuildRuleDef.NativeBuildRuleDef(defName, defCtx, params, returnType, cls, methodName)
    }
    if (defName.sourceId is PreludeSourceId && implTarget == listOf("native")) {
      val cls = sourceManager.getPreludePluginClass(clsName)
      return BuildRuleDef.NativeBuildRuleDef(defName, defCtx, params, returnType, cls, methodName)
    }

    val impl = exprEvaluator.evaluateName(task, defCtx, implTarget, thisValue).ensureValue()
    val cpInstance = coerceCpInstance(task, defCtx, impl)
    val realm = realmProvider.prepareRealm(cpInstance)
    return BuildRuleDef.UserBuildRuleDef(
      defName,
      defCtx,
      params,
      returnType,
      cpInstance,
      realm,
      clsName,
      methodName
    )
  }

  suspend fun resolveActionRule(
    task: Task,
    thisValue: BibixValue?,
    definition: Definition.ActionRule,
  ): EvaluationResult {
    val actionRule = definition.actionRule
    val defName = definition.cname

    val defContext = NameLookupContext(defName).dropLastToken()

    val params = paramDefs(task, defContext, actionRule.params().toKtList())

    val implTarget = actionRule.impl().targetName().tokens().toKtList()
    val clsName = actionRule.impl().className().tokens().mkString(".")
    val methodName = actionRule.impl().methodName().getOrNull() ?: "run"

    if (defName.sourceId is PreloadedSourceId && implTarget == listOf("native")) {
      val cls =
        sourceManager.getPreloadedPluginClass(defName.sourceId as PreloadedSourceId, clsName)
      return ActionRuleDef.PreloadedActionRuleDef(defName, defContext, params, cls, methodName)
    }
    if (defName.sourceId is PreludeSourceId && implTarget == listOf("native")) {
      val cls = sourceManager.getPreludePluginClass(clsName)
      return ActionRuleDef.PreloadedActionRuleDef(defName, defContext, params, cls, methodName)
    }

    val impl = exprEvaluator.evaluateName(task, defContext, implTarget, thisValue).ensureValue()
    val realm = realmProvider.prepareRealm(coerceCpInstance(task, defContext, impl))
    return ActionRuleDef.UserActionRuleDef(defName, defContext, params, realm, clsName, methodName)
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
      check(unknownParams.isEmpty()) { "Unknown parameters: $unknownParams at $params" }

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

      // Run concurrently
      val paramValues = (posParamsMap + namedParams).mapValues { (_, valueExpr) ->
        async {
          exprEvaluator.evaluateExpr(task, context, valueExpr, thisValue, directBindings)
            .ensureValue()
        }
      }
      val defaultParamValues = defaultParamsMap.mapValues { (_, valueExpr) ->
        async {
          // 여기선 directBindings 사용하지 말아야 함
          exprEvaluator.evaluateExpr(task, callable.context, valueExpr, thisValue).ensureValue()
        }
      }

      val values = (paramValues + defaultParamValues).awaitAllValues()
      val coercedValues = values.mapValues { (name, value) ->
        val type = paramDefsMap.getValue(name).type
        async { exprEvaluator.coerce(task, context, value, type) }
      }.awaitAllValues()
      coercedValues
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
        val name = exprEvaluator.evaluateName(task, context, returnValue.ruleName.split('.'), null)
        check(name is BuildRuleDef) { "" }
        val result = callBuildRule(task, context, name, returnValue.params).value
        return handlePluginReturnValue(task, context, returnValue.whenDone(result))
      }

      is BuildRuleReturn.GetClassTypeDetails -> {
        TODO()
      }

      is BuildRuleReturn.WithDirectoryLock -> {
        val lockFilePath = returnValue.directory.resolve("directory.lock")
        val channel = AsynchronousFileChannel.open(
          lockFilePath,
          StandardOpenOption.WRITE,
          StandardOpenOption.CREATE
        )
        val nextValue = channel.lock().await().use { returnValue.withLock() }
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
      val classType = exprEvaluator.evaluateName(task, context, value.nameTokens, null)
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

  private suspend fun callBuildRule(
    task: Task,
    context: NameLookupContext,
    buildRule: BuildRuleDef,
    params: Map<String, BibixValue>
  ): BibixValueWithObjectHash =
    g.withMemo(objectHasher.hash(context.sourceId, buildRule, params)) { objHash ->
      val pluginClass = buildRule.cls
      val pluginInstance = pluginClass.getDeclaredConstructor().newInstance()
      val method = pluginClass.getMethod(buildRule.methodName, BuildContext::class.java)

      val progressIndicator = interpreter.progressIndicatorContainer.ofCurrentThread()
      val buildContext = BuildContext(
        env = interpreter.buildEnv,
        fileSystem = interpreter.repo.fileSystem,
        mainBaseDirectory = sourceManager.getProjectRoot(MainSourceId),
        callerBaseDirectory = baseDirectoryOf(context.sourceId),
        ruleDefinedDirectory = baseDirectoryOf(buildRule.context.sourceId),
        arguments = params,
        hashChanged = false, // TODO
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

  suspend fun evaluateCallExpr(
    requester: Task,
    context: NameLookupContext,
    expr: BibixAst.CallExpr,
    thisValue: BibixValue?,
  ): BibixValueWithObjectHash =
    g.withTask(requester, Task.EvalCallExpr(context.sourceId, expr.id(), thisValue)) { task ->
      when (val callTarget = exprEvaluator.evaluateName(task, context, expr.name(), null)) {
        is BuildRuleDef -> {
          val params =
            organizeParams(task, context, callTarget, expr.params(), thisValue)
          callBuildRule(task, context, callTarget, params)
        }

        is EvaluationResult.DataClassDef -> {
          val params =
            organizeParams(task, context, callTarget, expr.params(), thisValue)
          val value = ClassInstanceValue(callTarget.packageName, callTarget.className, params)
          BibixValueWithObjectHash(value, null)
        }

        else -> throw IllegalStateException("TODO message")
      }
    }

  suspend fun executeActionCallExpr(
    requester: Task,
    context: NameLookupContext,
    expr: BibixAst.CallExpr,
    args: Pair<String, List<String>>?
  ): Unit = g.withTask(requester, Task.ExecuteActionCall(context.sourceId, expr.id())) { task ->
    // TODO handle actionArgs
    val callTarget = exprEvaluator.evaluateName(task, context, expr.name(), null)

    check(callTarget is ActionRuleDef) { "TODO message" }

    val directBindings =
      if (args == null) mapOf() else mapOf(args.first to ListValue(args.second.map { StringValue(it) }))
    val params = organizeParams(task, context, callTarget, expr.params(), null, directBindings)

    val pluginClass = callTarget.cls
    val pluginInstance = pluginClass.getDeclaredConstructor().newInstance()
    val method = pluginClass.getMethod(callTarget.methodName, ActionContext::class.java)

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
