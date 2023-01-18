package com.giyeok.bibix.interpreter.expr

import com.giyeok.bibix.ast.BibixAst
import com.giyeok.bibix.base.*
import com.giyeok.bibix.interpreter.BibixInterpreter
import com.giyeok.bibix.interpreter.RealmProvider
import com.giyeok.bibix.interpreter.SourceManager
import com.giyeok.bibix.interpreter.task.Task
import com.giyeok.bibix.interpreter.task.TaskRelGraph
import com.giyeok.bibix.utils.getOrNull
import com.giyeok.bibix.utils.toKtList
import com.google.common.annotations.VisibleForTesting
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

class ExprEvaluator(
  private val interpreter: BibixInterpreter,
  private val g: TaskRelGraph,
  private val sourceManager: SourceManager,
  private val varsManager: VarsManager,
  realmProvider: RealmProvider,
) {
  private val callExprEvaluator =
    CallExprEvaluator(interpreter, g, sourceManager, this, realmProvider)

  private val coercer = Coercer(sourceManager, this)

  suspend fun coerce(
    task: Task,
    context: NameLookupContext,
    value: BibixValue,
    type: BibixType
  ): BibixValue = coercer.coerce(task, context, value, type)

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
    is BibixAst.CollectionType -> {
      val typeParams = type.typeParams().params().toKtList()
      check(typeParams.size == 1) { "Invalid number of type parameters" }
      when (type.name()) {
        "set" -> SetType(evaluateType(task, context, typeParams.first()))
        "list" -> ListType(evaluateType(task, context, typeParams.first()))
        else -> throw IllegalStateException("Unknown colleciton type: ${type.name()}")
      }
    }

    is BibixAst.TupleType -> {
      val elemTypes = type.elems().toKtList().map { elemTypeElem ->
        evaluateType(task, context, elemTypeElem)
      }
      TupleType(elemTypes)
    }

    is BibixAst.NamedTupleType -> {
      val elemTypes = type.elems().toKtList().map { pair ->
        pair.name() to evaluateType(task, context, pair.typ())
      }
      NamedTupleType(elemTypes)
    }

    is BibixAst.UnionType -> {
      val candTypes = type.elems().toKtList().map { evaluateType(task, context, it) }
      UnionType(candTypes)
    }

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
    when (val definition = interpreter.lookupName(task, context, name)) {
      is Definition.ImportDef -> TODO()
      is Definition.NamespaceDef -> EvaluationResult.Namespace(NameLookupContext(definition.cname))
      is Definition.TargetDef -> {
        val result = evaluateExpr(task, context, definition.target.value(), thisValue)
        if (definition.cname.sourceId == MainSourceId && result is EvaluationResult.ValueWithObjectHash) {
          interpreter.repo.linkNameToObject(definition.cname.tokens, result.objectHash)
        }
        result
      }

      is Definition.ActionDef -> TODO()
      is Definition.ClassDef -> callExprEvaluator.resolveClassDef(task, definition)

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

      is Definition.VarDef -> {
        val varDef = varsManager.getVarDef(definition.cname)
        check(varDef.def == definition.varDef)
        // TODO 프로그램 argument 지원
        val redefines = varsManager.redefines(interpreter.nameLookupTable, definition.cname)
        if (redefines.isNotEmpty()) {
          // TODO redefinition이 여러개 발견되면 어떻게 처리하지..?
          check(redefines.size == 1) { "more than one redefinition for ${definition.cname} found" }
          val redefine = redefines.first()
          evaluateExpr(task, redefine.redefContext, redefine.def.redefValue(), null)
        } else {
          val defaultValueExpr =
            varDef.def.defaultValue().getOrNull() ?: throw IllegalStateException("???")
          evaluateExpr(task, varDef.defContext, defaultValueExpr, null)
        }
      }

      is Definition.VarRedef -> TODO()

      is Definition.BuildRule ->
        callExprEvaluator.resolveBuildRule(task, thisValue, definition)

      is Definition.ActionRule ->
        callExprEvaluator.resolveActionRule(task, thisValue, definition)
    }

  private fun findMember(targetExpr: BibixAst.Expr, value: BibixValue, name: String): BibixValue =
    when (value) {
      is ClassInstanceValue -> value.fieldValues[name]
        ?: throw IllegalStateException("Invalid access to $name on $targetExpr")

      is NamedTupleValue -> value.valuesMap[name]
        ?: throw IllegalStateException("Invalid access to $name on $targetExpr")

      else -> throw IllegalStateException("Invalid access to $name on $targetExpr")
    }

  private fun mergeValue(lhs: BibixValue, rhs: BibixValue): BibixValue {
    when (lhs) {
      is StringValue -> when (rhs) {
        is StringValue -> return StringValue(lhs.value + rhs.value)
        else -> {}
      }

      is ListValue -> return when (rhs) {
        is ListValue -> ListValue(lhs.values + rhs.values)
        else -> ListValue(lhs.values + rhs)
      }

      else -> {}
    }
    throw IllegalStateException("Cannot merge $lhs and $rhs")
  }

  suspend fun evaluateExpr(
    requester: Task,
    context: NameLookupContext,
    expr: BibixAst.Expr,
    thisValue: BibixValue?,
  ): EvaluationResult =
    g.withTask(requester, Task.EvalExpr(context.sourceId, expr.id(), thisValue)) { task ->
      when (expr) {
        is BibixAst.CastExpr -> {
          // Run concurrently
          val value = async { evaluateExpr(task, context, expr.expr(), thisValue).ensureValue() }
          val type = async { evaluateType(task, context, expr.castTo()) }
          EvaluationResult.Value(coerce(task, context, value.await(), type.await()))
        }

        is BibixAst.MergeOp -> {
          // Run concurrently
          val lhs = async { evaluateExpr(task, context, expr.lhs(), thisValue).ensureValue() }
          val rhs = async { evaluateExpr(task, context, expr.rhs(), thisValue).ensureValue() }
          val mergedValue = mergeValue(lhs.await(), rhs.await())
          EvaluationResult.Value(mergedValue)
        }

        is BibixAst.CallExpr ->
          callExprEvaluator.evaluateCallExpr(task, context, expr, thisValue).toEvaluationResult()

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

        is BibixAst.NameRef -> evaluateName(task, context, listOf(expr.name()), thisValue)

        is BibixAst.ListExpr -> {
          // Run concurrently
          val elemValues = expr.elems().toKtList().map { elemExpr ->
            async {
              evaluateExpr(task, context, elemExpr, thisValue).ensureValue()
            }
          }.awaitAll()
          EvaluationResult.Value(ListValue(elemValues))
        }

        is BibixAst.TupleExpr -> {
          // Run concurrently
          val elemValues = expr.elems().toKtList().map { elemExpr ->
            async { evaluateExpr(task, context, elemExpr, thisValue).ensureValue() }
          }.awaitAll()
          EvaluationResult.Value(TupleValue(elemValues))
        }

        is BibixAst.NamedTupleExpr -> {
          // Run concurrently
          val elemValues = expr.elems().toKtList().map { pair ->
            pair.name() to async {
              evaluateExpr(task, context, pair.expr(), thisValue).ensureValue()
            }
          }.map { (name, value) -> name to value.await() }
          EvaluationResult.Value(NamedTupleValue(elemValues))
        }

        is BibixAst.StringLiteral -> {
          // Run concurrently
          val elems = expr.elems().toKtList().map { elem ->
            async {
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
          }.awaitAll()
          EvaluationResult.Value(StringValue(elems.joinToString("")))
        }

        is BibixAst.BooleanLiteral ->
          EvaluationResult.Value(BooleanValue(expr.value()))

        is BibixAst.NoneLiteral ->
          EvaluationResult.Value(NoneValue)

        is BibixAst.This ->
          EvaluationResult.Value(checkNotNull(thisValue) { "this is not available" })

        is BibixAst.Paren -> evaluateExpr(task, context, expr.expr(), thisValue)

        else -> throw AssertionError()
      }
    }

  suspend fun executeAction(
    requester: Task,
    context: NameLookupContext,
    expr: BibixAst.Expr,
    actionArgs: List<String>
  ) = g.withTask(requester, Task.ExecuteAction(context.sourceId, expr.id())) { task ->
    when (expr) {
      is BibixAst.CallExpr ->
        callExprEvaluator.executeActionCallExpr(task, context, expr, actionArgs)

      else -> throw IllegalStateException("")
    }
  }
}

