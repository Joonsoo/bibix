package com.giyeok.bibix.interpreter.expr

import com.giyeok.bibix.ast.BibixAst
import com.giyeok.bibix.base.*
import com.giyeok.bibix.interpreter.BibixInterpreter
import com.giyeok.bibix.interpreter.PluginImplProvider
import com.giyeok.bibix.interpreter.SourceManager
import com.giyeok.bibix.interpreter.task.Task
import com.giyeok.bibix.interpreter.task.TaskRelGraph
import com.giyeok.bibix.repo.DirectoryLocker
import com.giyeok.bibix.utils.getOrNull
import com.giyeok.bibix.utils.toKtList
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

class ExprEvaluator(
  private val interpreter: BibixInterpreter,
  private val g: TaskRelGraph,
  private val sourceManager: SourceManager,
  private val varsManager: VarsManager,
  pluginImplProvider: PluginImplProvider,
  directoryLocker: DirectoryLocker,
) {
  private val callExprEvaluator =
    CallExprEvaluator(interpreter, g, sourceManager, this, pluginImplProvider, directoryLocker)

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
                ?: throw IllegalStateException(
                  "package name was not set for ${sourceManager.descriptionOf(definition.cname.sourceId)}"
                )
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
    evaluateDefinition(task, interpreter.lookupName(task, context, name), thisValue)

  suspend fun evaluateDefinition(
    requester: Task,
    definition: Definition,
    thisValue: BibixValue?
  ): EvaluationResult = when (definition) {
    is Definition.ImportDef -> TODO()
    is Definition.NamespaceDef -> EvaluationResult.Namespace(NameLookupContext(definition.cname))
    is Definition.TargetDef -> {
      val defContext = NameLookupContext(definition.cname).dropLastToken()
      val result = evaluateExpr(requester, defContext, definition.target.value(), thisValue)

      if (result is EvaluationResult.ValueWithObjectHash && definition.cname.sourceId == MainSourceId) {
        interpreter.repo.linkNameToObject(definition.cname.tokens, result.objectHash)
      }
      result
    }

    is Definition.ActionDef -> TODO()
    is Definition.ClassDef -> callExprEvaluator.resolveClassDef(requester, definition)

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
        evaluateExpr(requester, redefine.redefContext, redefine.def.redefValue(), null)
      } else {
        val defaultValueExpr =
          varDef.def.defaultValue().getOrNull() ?: throw IllegalStateException("???")
        evaluateExpr(requester, varDef.defContext, defaultValueExpr, null)
      }
    }

    is Definition.VarRedef -> TODO()

    is Definition.BuildRule ->
      callExprEvaluator.resolveBuildRule(requester, thisValue, definition)

    is Definition.ActionRule ->
      callExprEvaluator.resolveActionRule(requester, thisValue, definition)
  }

  suspend fun findTypeDefinition(
    task: Task,
    packageName: String,
    typeName: String
  ): EvaluationResult? {
    val sourceId = sourceManager.getSourceIdFromPackageName(packageName) ?: return null
    return evaluateName(task, NameLookupContext(sourceId, listOf()), typeName.split('.'), null)
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

      is ListValue -> when (rhs) {
        is ListValue -> return ListValue(lhs.values + rhs.values)
        is SetValue -> return ListValue(lhs.values + rhs.values)
        else -> {}
      }

      is SetValue -> when (rhs) {
        is ListValue -> return SetValue(lhs.values + rhs.values)
        is SetValue -> return SetValue(lhs.values + rhs.values)
        else -> {}
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
  ): EvaluationResult = evaluateExpr(requester, context, expr, thisValue, mapOf())

  // directBindings는 action의 args처럼 가장 가까운 스코프에서 값을 직접 할당하려는 경우 사용
  suspend fun evaluateExpr(
    requester: Task,
    context: NameLookupContext,
    expr: BibixAst.Expr,
    thisValue: BibixValue?,
    directBindings: Map<String, BibixValue>
  ): EvaluationResult =
    g.withTask(requester, Task.EvalExpr(context.sourceId, expr.id(), thisValue)) { task ->
      when (expr) {
        is BibixAst.CastExpr -> {
          // Run concurrently
          val value = async {
            evaluateExpr(task, context, expr.expr(), thisValue, directBindings).ensureValue()
          }
          val type = async { evaluateType(task, context, expr.castTo()) }
          EvaluationResult.Value(coerce(task, context, value.await(), type.await()))
        }

        is BibixAst.MergeOp -> {
          // Run concurrently
          val lhs = async {
            evaluateExpr(task, context, expr.lhs(), thisValue, directBindings).ensureValue()
          }
          val rhs = async {
            evaluateExpr(task, context, expr.rhs(), thisValue, directBindings).ensureValue()
          }
          val mergedValue = mergeValue(lhs.await(), rhs.await())
          EvaluationResult.Value(mergedValue)
        }

        is BibixAst.CallExpr ->
          callExprEvaluator.evaluateCallExpr(task, context, expr, thisValue).toEvaluationResult()

        is BibixAst.MemberAccess -> {
          when (val target =
            evaluateExpr(task, context, expr.target(), thisValue, directBindings)) {
            is EvaluationResult.Namespace ->
              evaluateName(task, target.context, listOf(expr.name()), thisValue)

            is EvaluationResult.Value ->
              EvaluationResult.Value(findMember(expr.target(), target.value, expr.name()))

            is EvaluationResult.EnumDef -> {
              check(target.enumValues.contains(expr.name())) { "Invalid enum value name" }
              EvaluationResult.Value(EnumValue(target.packageName, target.enumName, expr.name()))
            }

            else -> throw IllegalStateException(
              "Invalid access to ${expr.name()} on ${expr.target()} at ${
                sourceManager.descriptionOf(
                  context.sourceId
                )
              }, target=$target"
            )
          }
        }

        is BibixAst.NameRef -> {
          val directBinding = directBindings[expr.name()]
          if (directBinding != null) {
            EvaluationResult.Value(directBinding)
          } else {
            evaluateName(task, context, listOf(expr.name()), thisValue)
          }
        }

        is BibixAst.ListExpr -> {
          // Run concurrently
          val elemValues = expr.elems().toKtList().map { elemExpr ->
            when (elemExpr) {
              is BibixAst.EllipsisElem -> {
                val expr = elemExpr.value()
                async {
                  val evaluated =
                    evaluateExpr(task, context, expr, thisValue, directBindings).ensureValue()
                  when (evaluated) {
                    is ListValue -> evaluated.values
                    is SetValue -> evaluated.values
                    else -> throw IllegalStateException("Cannot expand $evaluated")
                  }
                }
              }

              is BibixAst.Expr -> {
                async {
                  val value =
                    evaluateExpr(task, context, elemExpr, thisValue, directBindings).ensureValue()
                  listOf(value)
                }
              }

              else -> throw AssertionError()
            }
          }.awaitAll()
          EvaluationResult.Value(ListValue(elemValues.flatten()))
        }

        is BibixAst.TupleExpr -> {
          // Run concurrently
          val elemValues = expr.elems().toKtList().map { elemExpr ->
            async { evaluateExpr(task, context, elemExpr, thisValue, directBindings).ensureValue() }
          }.awaitAll()
          EvaluationResult.Value(TupleValue(elemValues))
        }

        is BibixAst.NamedTupleExpr -> {
          // Run concurrently
          val elemValues = expr.elems().toKtList().map { pair ->
            pair.name() to async {
              evaluateExpr(task, context, pair.expr(), thisValue, directBindings).ensureValue()
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
                  val value = evaluateExpr(task, context, elem.expr(), thisValue, directBindings)
                    .ensureValue()
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

        is BibixAst.Paren -> evaluateExpr(task, context, expr.expr(), thisValue, directBindings)

        else -> throw AssertionError()
      }
    }

  suspend fun executeAction(
    requester: Task,
    context: NameLookupContext,
    expr: BibixAst.Expr,
    args: Pair<String, List<String>>?,
  ) = g.withTask(requester, Task.ExecuteAction(context.sourceId, expr.id())) { task ->
    when (expr) {
      is BibixAst.CallExpr ->
        callExprEvaluator.executeActionCallExpr(task, context, expr, args)

      else -> throw IllegalStateException("")
    }
  }
}
