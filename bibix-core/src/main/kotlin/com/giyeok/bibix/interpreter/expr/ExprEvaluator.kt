package com.giyeok.bibix.interpreter.expr

import com.giyeok.bibix.ast.BibixAst
import com.giyeok.bibix.base.*
import com.giyeok.bibix.interpreter.*
import com.giyeok.bibix.interpreter.name.Definition
import com.giyeok.bibix.interpreter.name.NameLookupContext
import com.giyeok.bibix.interpreter.task.Task
import com.giyeok.bibix.interpreter.task.TaskRelGraph
import com.giyeok.bibix.utils.toKtList
import java.nio.file.Path

class ExprEvaluator(
  private val interpreter: BibixInterpreter,
  private val g: TaskRelGraph,
  private val sourceManager: SourceManager,
) {
  private val callExprEvaluator = CallExprEvaluator(interpreter, g, sourceManager, this)

  private suspend fun memoize(
    sourceId: SourceId,
    exprId: Int,
    thisValue: BibixValue?,
    eval: suspend () -> EvaluationResult
  ): EvaluationResult {
    return eval()
  }

  private suspend fun fileFromString(sourceId: SourceId, path: String): Path {
    return sourceManager.getProjectRoot(sourceId).resolve(path).normalize()
  }

  suspend fun coerce(
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
        else -> throw IllegalStateException("Coercion failed: $value to $type")
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
        when (value) {
          is ClassInstanceValue -> {
            check(value.packageName == type.packageName && value.className == type.className)
            value
          }

          else -> TODO()
        }
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

  suspend fun evaluateType(
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
            is Definition.ClassDef -> {
              val packageName = sourceManager.getPackageName(definition.cname.sourceId)
                ?: throw IllegalStateException("")
              val className = definition.cname.tokens.joinToString(".")
              when (definition.classDef) {
                is BibixAst.DataClassDef -> DataClassType(packageName, className)
                is BibixAst.SuperClassDef -> SuperClassType(packageName, className)
                else -> throw AssertionError()
              }
            }

            is Definition.EnumDef -> {
              val packageName = sourceManager.getPackageName(definition.cname.sourceId)
                ?: throw IllegalStateException("")
              val className = definition.cname.tokens.joinToString(".")
              EnumType(packageName, className)
            }

            else -> throw AssertionError()
          }
        }
      }
    }

    is BibixAst.NoneType -> NoneType
    is BibixAst.CollectionType -> TODO()
    is BibixAst.TupleType -> TODO()
    is BibixAst.NamedTupleType -> TODO()
    is BibixAst.UnionType -> TODO()
    else -> throw AssertionError()
  }

  suspend fun evaluateName(
    task: Task,
    context: NameLookupContext,
    name: BibixAst.Name,
    thisValue: BibixValue?,
  ): EvaluationResult = evaluateName(task, context, name.tokens().toKtList(), thisValue)

  suspend fun evaluateName(
    task: Task,
    context: NameLookupContext,
    name: List<String>,
    thisValue: BibixValue?,
  ): EvaluationResult =
    // TODO memoize
    when (val definition = interpreter.lookupName(task, context, name)) {
      is Definition.ImportDef -> TODO()
      is Definition.NamespaceDef -> EvaluationResult.Namespace(NameLookupContext(definition.cname))
      is Definition.TargetDef -> evaluateExpr(task, context, definition.target.value(), thisValue)
      is Definition.ActionDef -> TODO()
      is Definition.ClassDef ->
        callExprEvaluator.resolveClassDef(task, context, definition)

      is Definition.EnumDef -> {
        val sourceId = definition.cname.sourceId
        val packageName = sourceManager.getPackageName(sourceId)
          ?: throw IllegalStateException("Package name for enum type ${definition.cname} not specified")
        EvaluationResult.EnumDef(
          sourceId,
          packageName,
          definition.enumDef.name(),
          definition.enumDef.values().toKtList()
        )
      }

      is Definition.ArgDef -> TODO()

      is Definition.ArgRedef -> TODO()

      is Definition.BuildRule ->
        callExprEvaluator.resolveBuildRule(task, context, thisValue, definition)

      is Definition.ActionRule ->
        callExprEvaluator.resolveActionRule(task, context, thisValue, definition)
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
            EvaluationResult.Value(
              callExprEvaluator.evaluateCallExpr(
                task,
                context,
                expr,
                thisValue
              )
            )

          is BibixAst.MemberAccess -> {
            when (val target = evaluateExpr(task, context, expr.target(), thisValue)) {
              is EvaluationResult.Namespace ->
                evaluateName(task, target.context, listOf(expr.name()), thisValue)

              is EvaluationResult.Value ->
                EvaluationResult.Value(findMember(expr.target(), target.value, expr.name()))

              is EvaluationResult.EnumDef -> {
                check(target.enumValues.contains(expr.name())) { "Invalid enum value name" }
                EvaluationResult.Value(EnumValue(target.packageName, target.enumName, expr.name()))
              }

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

  suspend fun executeAction(requester: Task, context: NameLookupContext, expr: BibixAst.Expr) =
    g.withTask(requester, Task.ExecuteAction(context.sourceId, expr.id())) { task ->
      when (expr) {
        is BibixAst.CallExpr ->
          callExprEvaluator.executeActionCallExpr(task, context, expr)

        else -> throw IllegalStateException("")
      }
    }
}

