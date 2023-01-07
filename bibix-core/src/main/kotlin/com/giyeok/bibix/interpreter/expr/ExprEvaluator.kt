package com.giyeok.bibix.interpreter.expr

import com.giyeok.bibix.ast.BibixAst
import com.giyeok.bibix.base.*
import com.giyeok.bibix.interpreter.*
import com.giyeok.bibix.interpreter.name.Definition
import com.giyeok.bibix.interpreter.name.NameLookupContext
import com.giyeok.bibix.interpreter.task.Task
import com.giyeok.bibix.interpreter.task.TaskRelGraph
import com.giyeok.bibix.runner.Param
import com.giyeok.bibix.utils.getOrNull
import com.giyeok.bibix.utils.toKtList
import java.lang.reflect.Method
import java.nio.file.Path

class ExprEvaluator(
  private val interpreter: BibixInterpreter,
  private val g: TaskRelGraph,
  private val sourceManager: SourceManager,
) {
  // expr -> list of dependent tasks -> evaluator

  private suspend fun memoize(
    sourceId: SourceId,
    exprId: Int,
    thisValue: BibixValue?,
    eval: suspend () -> EvaluationResult
  ): EvaluationResult {
    return eval()
  }

  private fun fileFromString(sourceId: SourceId, path: String): Path {
    TODO()
  }

  private suspend fun coerce(
    task: Task,
    context: NameLookupContext,
    value: BibixValue,
    type: BibixType
  ): BibixValue {
    fun coercionFailed() = IllegalStateException("Cannot coerce value")

    fun withNoNull(
      elems: List<BibixValue?>,
      block: (List<BibixValue>) -> BibixValue
    ): BibixValue = if (elems.contains(null)) {
      throw coercionFailed()
    } else {
      block(elems.map { it!! })
    }

    check(value !is NClassInstanceValue)
    return when (type) {
      AnyType -> value
      BooleanType -> when (value) {
        is BooleanValue -> value
        else -> TODO()
      }

      StringType -> when (value) {
        is StringValue -> value
        else -> TODO()
      }

      PathType -> when (value) {
        is PathValue -> value
        is FileValue -> PathValue(value.file)
        is DirectoryValue -> PathValue(value.directory)
        is StringValue -> PathValue(fileFromString(context.sourceId, value.value))
        else -> TODO()
      }

      FileType -> when (value) {
        is FileValue -> value
        // TODO file value, directory value는 build rule에 주기 직전에 존재하는지/타입 확인하기
        is PathValue -> FileValue(value.path)
        is StringValue -> FileValue(fileFromString(context.sourceId, value.value))
        else -> TODO()
      }

      DirectoryType -> when (value) {
        is DirectoryValue -> value
        is PathValue -> DirectoryValue(value.path)
        is StringValue -> DirectoryValue(fileFromString(context.sourceId, value.value))
        else -> TODO()
      }

      is CustomType -> {
        TODO()
      }

      is ListType -> {
        when (value) {
          is ListValue -> withNoNull(value.values.map {
            coerce(task, context, it, type.elemType)
          }) { ListValue(it) }

          is SetValue -> withNoNull(value.values.map {
            coerce(task, context, it, type.elemType)
          }) { ListValue(it) }

          else -> TODO()
        }
      }

      is SetType -> {
        when (value) {
          is ListValue -> withNoNull(value.values.map {
            coerce(task, context, it, type.elemType)
          }) { SetValue(it) }

          is SetValue -> withNoNull(value.values.map {
            coerce(task, context, it, type.elemType)
          }) { SetValue(it) }

          else -> TODO()
        }
      }

      is TupleType -> {
        suspend fun ifElse(): BibixValue = if (type.elemTypes.size != 1) {
          throw IllegalStateException("Cannot coerce")
        } else {
          // 길이가 1인 tuple이면 그냥 맞춰서 반환해주기
          TupleValue(coerce(task, context, value, type.elemTypes[0]))
        }

        when (value) {
          is TupleValue -> {
            check(type.elemTypes.size == value.values.size)
            withNoNull(
              value.values.zip(type.elemTypes)
                .map {
                  coerce(task, context, it.first, it.second)
                }) { TupleValue(it) }
          }

          is NamedTupleValue -> {
            check(type.elemTypes.size == value.pairs.size)
            withNoNull(value.values().zip(type.elemTypes)
              .map {
                coerce(task, context, it.first, it.second)
              }) { TupleValue(it) }
          }

          else -> ifElse()
        }
      }

      is NamedTupleType -> {
        suspend fun ifElse() =
          if (type.pairs.size != 1) throw coercionFailed() else {
            // 길이가 1인 named tuple이면 그냥 맞춰서 반환해주기
            val value = coerce(task, context, value, type.pairs[0].second)
            NamedTupleValue(type.pairs[0].first to value)
          }

        when (value) {
          is NamedTupleValue -> {
            check(type.pairs.size == value.pairs.size)
            check(type.names() == value.names())
            withNoNull(value.values().zip(type.valueTypes()).map {
              coerce(task, context, it.first, it.second)
            }) { NamedTupleValue(type.names().zip(it)) }
          }

          is TupleValue -> {
            check(type.pairs.size == value.values.size)
            withNoNull(value.values.zip(type.valueTypes())
              .map { coerce(task, context, it.first, it.second) }) {
              NamedTupleValue(type.names().zip(it))
            }
          }

          else -> ifElse()
        }
      }

      is DataClassType -> {
        TODO()
      }

      is UnionType -> {
        val firstMatch = type.types.firstNotNullOfOrNull {
          coerce(task, context, value, it)
        }
        if (firstMatch == null) {
          println("${type.types} $value")
        }
        firstMatch!!
      }

      NoneType -> if (value is NoneValue) value else throw coercionFailed()
      ActionRuleDefType -> if (value is ActionRuleDefValue) value else throw coercionFailed()
      BuildRuleDefType -> if (value is BuildRuleDefValue) value else throw coercionFailed()
      TypeType -> if (value is TypeValue) value else throw coercionFailed()
      is EnumType -> TODO()
      is SuperClassType -> TODO()
    }
  }

  private suspend fun evaluateType(
    task: Task,
    context: NameLookupContext,
    type: BibixAst.TypeExpr,
  ): BibixType = when (type) {
    is BibixAst.Name -> {
      when (val name = type.tokens().toKtList()) {
        listOf("boolean") -> BooleanType
        listOf("string") -> StringType
        listOf("file") -> FileType
        listOf("directory") -> DirectoryType
        listOf("path") -> PathType
        listOf("buildrule") -> BuildRuleDefType
        listOf("actionrule") -> ActionRuleDefType
        listOf("type") -> TypeType
        else -> {
          when (val definition = interpreter.lookupName(task, context, name)) {
            is Definition.ClassDef -> TODO()
            is Definition.EnumDef -> TODO()
            else -> throw AssertionError()
          }
        }
      }
    }

    is BibixAst.CanonicalName -> TODO()
    is BibixAst.NoneType -> NoneType
    is BibixAst.CollectionType -> TODO()
    is BibixAst.TupleType -> TODO()
    is BibixAst.NamedTupleType -> TODO()
    is BibixAst.UnionType -> TODO()
    else -> throw AssertionError()
  }

  private suspend fun evaluateName(
    task: Task,
    context: NameLookupContext,
    name: BibixAst.Name,
    thisValue: BibixValue?,
  ): EvaluationResult = evaluateName(task, context, name.tokens().toKtList(), thisValue)

  private suspend fun evaluateName(
    task: Task,
    context: NameLookupContext,
    name: List<String>,
    thisValue: BibixValue?,
  ): EvaluationResult =
    when (val definition = interpreter.lookupName(task, context, name)) {
      is Definition.ImportDef -> TODO()
      is Definition.NamespaceDef -> EvaluationResult.Namespace(NameLookupContext(definition.cname))
      is Definition.TargetDef -> evaluateExpr(task, context, definition.target.value(), thisValue)
      is Definition.ActionDef -> TODO()
      is Definition.ClassDef -> TODO()
      is Definition.EnumDef -> TODO()
      is Definition.ArgDef -> TODO()
      is Definition.ArgRedef -> TODO()

      is Definition.BuildRule -> {
        val buildRule = definition.buildRule
        println(definition)
        println(name)

        val methodName = buildRule.impl().methodName().getOrNull() ?: "build"
        val params = buildRule.params().toKtList().map { param ->
          check(param.typ().isDefined) { "$param type not set" }
          val paramType = evaluateType(task, context, param.typ().get())
          param.optional()
          param.name()
          param.defaultValue().getOrNull()
        }
        val returnType = evaluateType(task, context, buildRule.returnType())

        if (definition.cname.sourceId is PreloadedSourceId &&
          buildRule.impl().targetName().tokens().toKtList() == listOf("native")
        ) {
          val cls = sourceManager.getPreloadedPluginClass(
            definition.cname.sourceId as PreloadedSourceId,
            buildRule.impl().className().tokens().mkString(".")
          )
          EvaluationResult.PreloadedBuildRuleDef(cls, methodName)
        } else {
          val impl =
            evaluateName(
              task,
              NameLookupContext(definition.cname),
              buildRule.impl().targetName(),
              thisValue
            ).ensureValue()
          val cps =
            coerce(task, context, impl, DataClassType("com.giyeok.bibix.plugins.jvm", "ClassPaths"))
              as ClassInstanceValue
          TODO()
        }
      }

      is Definition.ActionRule -> TODO()
    }

  private fun findMember(targetExpr: BibixAst.Expr, value: BibixValue, name: String): BibixValue =
    when (value) {
      is ClassInstanceValue -> value.fieldValues[name]
        ?: throw IllegalStateException("Invalid access to $name on $targetExpr")

      is NamedTupleValue -> value.valuesMap[name]
        ?: throw IllegalStateException("Invalid access to $name on $targetExpr")

      else -> throw IllegalStateException("Invalid access to $name on $targetExpr")
    }

  suspend fun evaluateExpr(
    requester: Task,
    context: NameLookupContext,
    expr: BibixAst.Expr,
    thisValue: BibixValue?,
  ): EvaluationResult =
    g.withTask(requester, Task.EvalExpr(context.sourceId, expr.id(), thisValue)) { task ->
      memoize(context.sourceId, expr.id(), thisValue) {
        when (expr) {
          is BibixAst.CastExpr -> TODO()

          is BibixAst.MergeOp -> {
            val lhs = evaluateExpr(task, context, expr.lhs(), thisValue).ensureValue()
            val rhs = evaluateExpr(task, context, expr.rhs(), thisValue).ensureValue()
            EvaluationResult.Value(ListValue())
            TODO()
          }

          is BibixAst.CallExpr ->
            EvaluationResult.Value(evaluateCallExpr(task, context, expr, thisValue))

          is BibixAst.MemberAccess -> {
            when (val target = evaluateExpr(task, context, expr.target(), thisValue)) {
              is EvaluationResult.Namespace ->
                evaluateName(task, target.context, listOf(expr.name()), thisValue)

              is EvaluationResult.Value ->
                EvaluationResult.Value(findMember(expr.target(), target.value, expr.name()))

              else -> throw IllegalStateException("Invalid access to ${expr.name()} on ${expr.target()}")
            }
          }

          is BibixAst.NameRef ->
            evaluateName(task, context, listOf(expr.name()), thisValue)

          is BibixAst.ListExpr -> {
            val elemValues = expr.elems().toKtList()
              .map { elemExpr -> evaluateExpr(task, context, elemExpr, thisValue).ensureValue() }
            EvaluationResult.Value(ListValue(elemValues))
          }

          is BibixAst.TupleExpr -> {
            val elemValues = expr.elems().toKtList()
              .map { elemExpr -> evaluateExpr(task, context, elemExpr, thisValue).ensureValue() }
            EvaluationResult.Value(TupleValue(elemValues))
          }

          is BibixAst.NamedTupleExpr -> {
            val elemValues = expr.elems().toKtList()
              .map { pair ->
                pair.name() to evaluateExpr(task, context, pair.expr(), thisValue).ensureValue()
              }
            EvaluationResult.Value(NamedTupleValue(elemValues))
          }

          is BibixAst.StringLiteral -> {
            val elems = expr.elems().toKtList().map { elem ->
              when (elem) {
                is BibixAst.JustChar -> elem.chr().toString()

                is BibixAst.EscapeChar -> when (elem.code()) {
                  'n' -> "\n"
                  'b' -> "\b"
                  'r' -> "\r"
                  't' -> "\t"
                  '$' -> "$"
                  '\\' -> "\\"
                  '"' -> "\""
                  else -> throw AssertionError()
                }

                is BibixAst.SimpleExpr -> {
                  val value =
                    evaluateName(task, context, listOf(elem.name()), thisValue).ensureValue()
                  (coerce(task, context, value, StringType) as StringValue).value
                }

                is BibixAst.ComplexExpr -> {
                  val value = evaluateExpr(task, context, elem.expr(), thisValue).ensureValue()
                  (coerce(task, context, value, StringType) as StringValue).value
                }

                else -> throw AssertionError()
              }
            }
            EvaluationResult.Value(StringValue(elems.joinToString("")))
          }

          is BibixAst.BooleanLiteral ->
            EvaluationResult.Value(BooleanValue(expr.value()))

          is BibixAst.NoneLiteral ->
            EvaluationResult.Value(NoneValue)

          is BibixAst.Paren -> evaluateExpr(task, context, expr.expr(), thisValue)

          else -> throw AssertionError()
        }
      }
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
      val callTarget = evaluateName(task, context, expr.name(), null)

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

