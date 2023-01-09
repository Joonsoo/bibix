package com.giyeok.bibix.interpreter.expr

import com.giyeok.bibix.ast.BibixAst
import com.giyeok.bibix.base.*
import com.giyeok.bibix.interpreter.BibixInterpreter
import com.giyeok.bibix.interpreter.DataClassType
import com.giyeok.bibix.interpreter.SourceManager
import com.giyeok.bibix.interpreter.expr.EvaluationResult.RuleDef.ActionRuleDef
import com.giyeok.bibix.interpreter.expr.EvaluationResult.RuleDef.BuildRuleDef
import com.giyeok.bibix.interpreter.task.Task
import com.giyeok.bibix.interpreter.task.TaskRelGraph
import com.giyeok.bibix.objectId
import com.giyeok.bibix.repo.hashString
import com.giyeok.bibix.repo.toProto
import com.giyeok.bibix.utils.getOrNull
import com.giyeok.bibix.utils.toArgsMap
import com.giyeok.bibix.utils.toHexString
import com.giyeok.bibix.utils.toKtList
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.codehaus.plexus.classworlds.ClassWorld
import org.codehaus.plexus.classworlds.realm.ClassRealm
import kotlin.io.path.Path
import kotlin.io.path.absolute

class CallExprEvaluator(
  private val interpreter: BibixInterpreter,
  private val g: TaskRelGraph,
  private val sourceManager: SourceManager,
  private val exprEvaluator: ExprEvaluator,
) {
  private val classWorld = ClassWorld()
  private var realmIdCounter = 0
  private val mutex = Mutex()

  private suspend fun newRealm(): ClassRealm = mutex.withLock {
    realmIdCounter += 1
    val newRealm = classWorld.newRealm("realm-$realmIdCounter")
    newRealm
  }

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

  private suspend fun prepareRealm(
    task: Task,
    context: NameLookupContext,
    impl: BibixValue
  ): ClassRealm {
    val cpType = DataClassType("com.giyeok.bibix.plugins.jvm", "ClassPaths")
    val cpInstance = exprEvaluator.coerce(task, context, impl, cpType) as ClassInstanceValue
    val realm = newRealm()
    ((cpInstance.fieldValues.getValue("cps")) as SetValue).values.forEach {
      realm.addURL((it as PathValue).path.absolute().toUri().toURL())
    }
    return realm
  }

  suspend fun resolveClassDef(
    task: Task,
    context: NameLookupContext,
    definition: Definition.ClassDef
  ): EvaluationResult {
    val sourceId = definition.cname.sourceId
    val packageName = sourceManager.getPackageName(sourceId)
      ?: throw IllegalStateException("Package name for class ${definition.cname} not specified")
    val className = definition.cname.tokens.joinToString(".")
    return when (definition.classDef) {
      is BibixAst.DataClassDef -> {
        val fields = paramDefs(task, context, definition.classDef.fields().toKtList())
        // TODO handle definition.classDef.body()
        EvaluationResult.DataClassDef(context, sourceId, packageName, className, fields)
      }

      is BibixAst.SuperClassDef -> {
        val subTypes = definition.classDef.subs().toKtList()
        EvaluationResult.SuperClassDef(context, sourceId, packageName, className, subTypes)
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

    val defContext = NameLookupContext(definition.cname).dropLastToken()

    val params = paramDefs(task, defContext, buildRule.params().toKtList())
    val returnType = exprEvaluator.evaluateType(task, defContext, buildRule.returnType())

    val implTarget = buildRule.impl().targetName().tokens().toKtList()
    val clsName = buildRule.impl().className().tokens().mkString(".")
    val methodName = buildRule.impl().methodName().getOrNull() ?: "build"

    if (definition.cname.sourceId is PreloadedSourceId && implTarget == listOf("native")) {
      val cls = sourceManager.getPreloadedPluginClass(
        definition.cname.sourceId as PreloadedSourceId,
        clsName
      )
      return BuildRuleDef.PreloadedBuildRuleDef(defContext, params, returnType, cls, methodName)
    }

    val impl = exprEvaluator.evaluateName(task, defContext, implTarget, thisValue).ensureValue()
    val realm = prepareRealm(task, defContext, impl)
    return BuildRuleDef.UserBuildRuleDef(defContext, params, returnType, realm, clsName, methodName)
  }

  suspend fun resolveActionRule(
    task: Task,
    thisValue: BibixValue?,
    definition: Definition.ActionRule,
  ): EvaluationResult {
    val actionRule = definition.actionRule

    val defContext = NameLookupContext(definition.cname).dropLastToken()

    val params = paramDefs(task, defContext, actionRule.params().toKtList())

    val implTarget = actionRule.impl().targetName().tokens().toKtList()
    val clsName = actionRule.impl().className().tokens().mkString(".")
    val methodName = actionRule.impl().methodName().getOrNull() ?: "run"

    if (definition.cname.sourceId is PreloadedSourceId && implTarget == listOf("native")) {
      val cls = sourceManager.getPreloadedPluginClass(
        definition.cname.sourceId as PreloadedSourceId,
        clsName
      )
      return ActionRuleDef.PreloadedActionRuleDef(defContext, params, cls, methodName)
    }

    val impl = exprEvaluator.evaluateName(task, defContext, implTarget, thisValue).ensureValue()
    val realm = prepareRealm(task, defContext, impl)
    return ActionRuleDef.UserActionRuleDef(defContext, params, realm, clsName, methodName)
  }

  private suspend fun organizeParams(
    requester: Task,
    context: NameLookupContext,
    callable: EvaluationResult.Callable,
    params: BibixAst.CallParams,
    thisValue: BibixValue?,
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
      check(unspecifiedParams.all { paramDefsMap.getValue(it).optional }) { "Required parameter not specified at $params" }

      val defaultParamsMap =
        unspecifiedParams.associateWith { name -> namedParams.getValue(name) }

      val paramValues = (posParamsMap + namedParams).mapValues { (_, valueExpr) ->
        exprEvaluator.evaluateExpr(task, context, valueExpr, thisValue).ensureValue()
      }
      val defaultParamValues = defaultParamsMap.mapValues { (_, valueExpr) ->
        exprEvaluator.evaluateExpr(task, callable.context, valueExpr, thisValue).ensureValue()
      }

      val values = paramValues + defaultParamValues
      val coercedValues = values.mapValues { (name, value) ->
        val type = paramDefsMap.getValue(name).type
        exprEvaluator.coerce(task, context, value, type)
      }
      coercedValues
    }

  private suspend fun handlePluginReturnValue(
    task: Task,
    context: NameLookupContext,
    returnValue: Any,
  ): BibixValue {
    when (returnValue) {
      is BibixValue -> return returnValue
      is BuildRuleReturn.ValueReturn -> return returnValue.value
      is BuildRuleReturn.FailedReturn -> throw returnValue.exception
      is BuildRuleReturn.DoneReturn ->
        throw IllegalStateException("Done is not allowed for build rule")

      is BuildRuleReturn.EvalAndThen -> {
        val name = exprEvaluator.evaluateName(task, context, returnValue.ruleName.split('.'), null)
        check(name is BuildRuleDef) { "" }
        val result = callBuildRule(task, context, name, returnValue.params)
        return handlePluginReturnValue(task, context, returnValue.whenDone(result))
      }

      is BuildRuleReturn.GetClassTypeDetails -> {
        TODO()
      }

      else -> throw IllegalStateException("Unknown return value: $returnValue")
    }
  }

  suspend fun callBuildRule(
    task: Task,
    context: NameLookupContext,
    buildRule: BuildRuleDef,
    params: Map<String, BibixValue>
  ): BibixValue {
    val pluginClass = buildRule.cls
    val pluginInstance = pluginClass.getDeclaredConstructor().newInstance()
    val method = pluginClass.getMethod(buildRule.methodName, BuildContext::class.java)

    val objectId = objectId {
      this.sourceId = context.sourceId.toProto()
      this.argsMap = params.toArgsMap()
    }
    val objectIdHash = objectId.hashString().toHexString()
    val progressIndicator = interpreter.progressIndicatorContainer.ofCurrentThread()
    val buildContext = BuildContext(
      env = interpreter.buildEnv,
      sourceId = buildRule.context.sourceId,
      fileSystem = interpreter.repo.fileSystem,
      mainBaseDirectory = interpreter.sourceManager.getProjectRoot(MainSourceId),
      callerBaseDirectory = if (context.sourceId is ExternSourceId) {
        interpreter.sourceManager.getProjectRoot(context.sourceId)
      } else {
        Path("/")
      },
      ruleDefinedDirectory = if (context.sourceId is ExternSourceId) {
        interpreter.sourceManager.getProjectRoot(buildRule.context.sourceId)
      } else {
        Path("/")
      },
      arguments = params,
      hashChanged = false, // TODO
      objectId = objectId,
      objectIdHash = objectIdHash,
      destDirectoryPath = interpreter.repo.objectsDirectory.resolve(objectIdHash),
      progressLogger = progressIndicator,
      repo = interpreter.repo,
    )

    check(method.trySetAccessible())

    progressIndicator.updateProgressDescription("Calling ${buildRule.context}...")
    val returnValue = try {
      method.invoke(pluginInstance, buildContext)
    } catch (e: Exception) {
      throw IllegalStateException("Error from the plugin", e)
    }
    progressIndicator.updateProgressDescription("Continuing from ${buildRule.context}...")

    val finalValue = handlePluginReturnValue(task, buildRule.context, returnValue)
    return exprEvaluator.coerce(task, buildRule.context, finalValue, buildRule.returnType)
  }

  suspend fun evaluateCallExpr(
    requester: Task,
    context: NameLookupContext,
    expr: BibixAst.CallExpr,
    thisValue: BibixValue?,
  ): BibixValue =
    g.withTask(requester, Task.EvalExpr(context.sourceId, expr.id(), thisValue)) { task ->
      when (val callTarget = exprEvaluator.evaluateName(task, context, expr.name(), null)) {
        is BuildRuleDef -> {
          val params =
            organizeParams(task, context, callTarget, expr.params(), thisValue)
          callBuildRule(task, context, callTarget, params)
          // val classDef = evaluateName(task, context, value.nameTokens, null)
          // check(classDef is EvaluationResult.DataClassDef) { "Invalid relative class name ${value.nameTokens} in $context" }
          // val newValue = ClassInstanceValue(classDef.packageName, classDef.className, value.fieldValues)
          // return coerce(task, context, newValue, type)
          // TODO 반환값이 NClassInstanceValue인 경우 ClassInstanceValue로 바꾸는건 여기서 해야함(context때문에)
        }

        is EvaluationResult.DataClassDef -> {
          val params =
            organizeParams(task, context, callTarget, expr.params(), thisValue)
          ClassInstanceValue(callTarget.packageName, callTarget.className, params)
        }

        else -> throw IllegalStateException("TODO message")
      }
    }

  suspend fun executeActionCallExpr(
    requester: Task,
    context: NameLookupContext,
    expr: BibixAst.CallExpr,
  ): Unit = g.withTask(requester, Task.ExecuteAction(context.sourceId, expr.id())) { task ->
    val callTarget = exprEvaluator.evaluateName(task, context, expr.name(), null)

    check(callTarget is ActionRuleDef) { "TODO message" }

    val params = organizeParams(task, context, callTarget, expr.params(), null)

    val pluginClass = callTarget.cls
    val pluginInstance = pluginClass.getDeclaredConstructor().newInstance()
    val method = pluginClass.getMethod(callTarget.methodName, ActionContext::class.java)

    val progressIndicator = interpreter.progressIndicatorContainer.ofCurrentThread()
    val actionContext =
      ActionContext(interpreter.buildEnv, callTarget.context.sourceId, params, progressIndicator)

    progressIndicator.updateProgressDescription("Calling ${callTarget.context}...")
    try {
      method.invoke(pluginInstance, actionContext)
    } catch (e: Exception) {
      throw IllegalStateException("Error from the plugin", e)
    }
    progressIndicator.updateProgressDescription("Continuing from ${callTarget.context}...")
  }
}
