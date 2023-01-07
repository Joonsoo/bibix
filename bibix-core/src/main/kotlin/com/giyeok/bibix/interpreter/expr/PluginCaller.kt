package com.giyeok.bibix.interpreter.expr

import com.giyeok.bibix.ast.BibixAst
import com.giyeok.bibix.base.BibixValue
import com.giyeok.bibix.base.BuildContext
import com.giyeok.bibix.base.ClassInstanceValue
import com.giyeok.bibix.base.PreloadedSourceId
import com.giyeok.bibix.interpreter.DataClassType
import com.giyeok.bibix.interpreter.SourceManager
import com.giyeok.bibix.interpreter.name.Definition
import com.giyeok.bibix.interpreter.name.NameLookupContext
import com.giyeok.bibix.interpreter.task.Task
import com.giyeok.bibix.interpreter.task.TaskRelGraph
import com.giyeok.bibix.runner.Param
import com.giyeok.bibix.utils.getOrNull
import com.giyeok.bibix.utils.toKtList
import java.lang.reflect.Method

class PluginCaller(
  private val g: TaskRelGraph,
  private val sourceManager: SourceManager,
  private val exprEvaluator: ExprEvaluator,
) {
  suspend fun resolveBuildRule(
    task: Task,
    context: NameLookupContext,
    name: List<String>,
    thisValue: BibixValue?,
    definition: Definition.BuildRule,
  ): EvaluationResult {
    val buildRule = definition.buildRule
    println(definition)
    println(name)

    val methodName = buildRule.impl().methodName().getOrNull() ?: "build"
    val params = buildRule.params().toKtList().map { param ->
      check(param.typ().isDefined) { "$param type not set" }
      EvaluationResult.Param(
        param.name(),
        param.optional(),
        exprEvaluator.evaluateType(task, context, param.typ().get()),
        param.defaultValue().getOrNull()
      )
    }
    val returnType = exprEvaluator.evaluateType(task, context, buildRule.returnType())

    if (definition.cname.sourceId is PreloadedSourceId &&
      buildRule.impl().targetName().tokens().toKtList() == listOf("native")
    ) {
      val cls = sourceManager.getPreloadedPluginClass(
        definition.cname.sourceId as PreloadedSourceId,
        buildRule.impl().className().tokens().mkString(".")
      )
      return EvaluationResult.PreloadedBuildRuleDef(context, params, returnType, cls, methodName)
    }

    val impl =
      exprEvaluator.evaluateName(
        task,
        NameLookupContext(definition.cname),
        buildRule.impl().targetName(),
        thisValue
      ).ensureValue()
    val cps =
      exprEvaluator.coerce(
        task,
        context,
        impl,
        DataClassType("com.giyeok.bibix.plugins.jvm", "ClassPaths")
      ) as ClassInstanceValue
    TODO()
  }

  suspend fun resolveActionRule(
    task: Task,
    context: NameLookupContext,
    name: List<String>,
    thisValue: BibixValue?,
    definition: Definition.ActionRule,
  ): EvaluationResult {
    TODO()
  }

  suspend fun evaluateCallExpr(
    requester: Task,
    context: NameLookupContext,
    expr: BibixAst.CallExpr,
    thisValue: BibixValue?,
  ): BibixValue =
    g.withTask(requester, Task.EvalExpr(context.sourceId, expr.id(), thisValue)) { task ->
      // val classDef = evaluateName(task, context, value.nameTokens, null)
      // check(classDef is EvaluationResult.DataClassDef) { "Invalid relative class name ${value.nameTokens} in $context" }
      // val newValue = ClassInstanceValue(classDef.packageName, classDef.className, value.fieldValues)
      // return coerce(task, context, newValue, type)
      // 반환값이 NClassInstanceValue인 경우 ClassInstanceValue로 바꾸는건 여기서 해야함(context때문에)
      val callTarget = exprEvaluator.evaluateName(task, context, expr.name(), null)

      val instance: Any
      val method: Method
      val params: List<Param>

      when (callTarget) {
        is EvaluationResult.PreloadedBuildRuleDef -> {
          instance = callTarget.cls.getDeclaredConstructor().newInstance()
          method = callTarget.cls.getMethod(callTarget.methodName, BuildContext::class.java)
        }

        is EvaluationResult.UserBuildRuleDef ->
          TODO()

        else ->
          throw IllegalStateException("Expected build rule, but $callTarget was given for $expr")
      }

      // val buildContext = BuildContext(interpreter.buildEnv)

      // method.invoke(instance, buildContext)

      TODO()
    }
}
