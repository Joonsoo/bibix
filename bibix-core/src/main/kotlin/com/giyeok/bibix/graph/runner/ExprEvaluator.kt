package com.giyeok.bibix.graph.runner

import com.giyeok.bibix.BibixIdProto.ArgsMap
import com.giyeok.bibix.argPair
import com.giyeok.bibix.argsMap
import com.giyeok.bibix.ast.BibixAst
import com.giyeok.bibix.base.*
import com.giyeok.bibix.graph.*
import com.giyeok.bibix.utils.toProto

class ExprEvaluator(
  private val buildGraphRunner: BuildGraphRunner,
  private val projectId: Int,
  private val importInstanceId: Int,
  private val localLets: Map<String, BibixValue>,
  private val thisValue: ClassInstanceValue?,
) {
  private val multiGraph get() = buildGraphRunner.multiGraph
  private val projectPackageName: String? get() = multiGraph.projectPackages[projectId]

  private val buildGraph get() = multiGraph.getProjectGraph(projectId)
  private val exprGraph: ExprGraph get() = buildGraph.exprGraph

  private val valueCaster: ValueCaster get() = ValueCaster(buildGraphRunner, projectId)

  private fun evalTask(exprNodeId: ExprNodeId) =
    // isRunningActionStmt는 action stmt에서도 가장 바깥의 call expr에만 적용되면 됨
    EvalExpr(projectId, exprNodeId, importInstanceId, localLets, thisValue)

  fun evaluateExpr(exprNodeId: ExprNodeId): BuildTaskResult =
    when (val exprNode = exprGraph.nodes.getValue(exprNodeId)) {
      is NoneLiteralNode -> BuildTaskResult.ValueResult(NoneValue)
      is BooleanLiteralNode -> BuildTaskResult.ValueResult(BooleanValue(exprNode.expr.value))
      is ThisRefNode -> BuildTaskResult.ValueResult(checkNotNull(thisValue))

      is MemberAccessNode ->
        BuildTaskResult.WithResult(evalTask(exprNode.target)) { target ->
          when (target) {
            is BuildTaskResult.ResultWithValue -> {
              val memberValue = target.value.followMemberNames(exprNode.memberNames)
              BuildTaskResult.ValueResult(memberValue)
            }

            else -> throw IllegalStateException()
          }
        }

      is LocalEnumValue ->
        BuildTaskResult.ValueResult(
          EnumValue(projectPackageName!!, exprNode.enumType.toString(), exprNode.enumValue)
        )

      is ListExprNode ->
        BuildTaskResult.WithResultList(exprNode.elems.map { evalTask(it.value) }) { elems ->
          check(elems.size == exprNode.elems.size)
          val elemValues = elems.map { elemResult ->
            elemResult.toValue()
          }
          val list = mutableListOf<BibixValue>()
          elemValues.zip(exprNode.elems).forEach { (value, elemInfo) ->
            if (elemInfo.isEllipsis) {
              check(value is CollectionValue)
              list.addAll(value.values)
            } else {
              list.add(value)
            }
          }
          BuildTaskResult.ValueResult(ListValue(list))
        }

      is TupleNode ->
        BuildTaskResult.WithResultList(exprNode.elems.map { evalTask(it) }) { elems ->
          check(elems.size == exprNode.elems.size)
          val elemValues = elems.map { elemResult ->
            elemResult.toValue()
          }
          BuildTaskResult.ValueResult(TupleValue(elemValues))
        }

      is NamedTupleNode ->
        BuildTaskResult.WithResultList(exprNode.elems.map { evalTask(it.second) }) { elems ->
          check(elems.size == exprNode.elems.size)
          val elemValues = exprNode.elems.zip(elems).map { (pair, elemResult) ->
            pair.first to elemResult.toValue()
          }
          BuildTaskResult.ValueResult(NamedTupleValue(elemValues))
        }

      is MergeExprNode ->
        BuildTaskResult.WithResultList(
          listOf(evalTask(exprNode.lhs), evalTask(exprNode.rhs))
        ) { operands ->
          check(operands.size == 2)
          val lhsResult = operands[0]
          val rhsResult = operands[1]
          check(lhsResult is BuildTaskResult.ResultWithValue && rhsResult is BuildTaskResult.ResultWithValue)
          val lhs = lhsResult.value
          val rhs = rhsResult.value
          val merged = when {
            lhs is StringValue && rhs is StringValue -> StringValue(lhs.value + rhs.value)
            lhs is ListValue && rhs is CollectionValue -> ListValue(lhs.values + rhs.values)
            lhs is SetValue && rhs is CollectionValue -> SetValue(lhs.values + rhs.values)
            else -> throw IllegalStateException()
          }
          BuildTaskResult.ValueResult(merged)
        }

      is StringNode -> {
        val exprElems = exprNode.elems.map { elem -> evalTask(elem) }
        BuildTaskResult.WithResultList(exprElems) { elems ->
          val builder = StringBuilder()
          var elemIdx = 0
          exprNode.expr.elems.forEach { stringElem ->
            when (stringElem) {
              is BibixAst.JustChar -> builder.append(stringElem.chr)
              is BibixAst.EscapeChar -> {
                val chr = when (stringElem.code) {
                  'n' -> '\n'
                  else -> throw AssertionError()
                }
                builder.append(chr)
              }

              is BibixAst.SimpleExpr, is BibixAst.ComplexExpr -> {
                val value = elems[elemIdx++].toValue()
                check(value is StringValue)
                builder.append(value.value)
              }
            }
          }
          check(elemIdx == elems.size)
          BuildTaskResult.ValueResult(StringValue(builder.toString()))
        }
      }

      is CallExprNode -> {
        val namedParams = exprNode.namedParams.entries.toList()
        BuildTaskResult.WithResultList(
          listOf(EvalCallee(projectId, importInstanceId, exprNode.callee)) +
            exprNode.posParams.map { evalTask(it) } +
            namedParams.map { evalTask(it.value) }
        ) { results ->
          check(results.size == 1 + exprNode.posParams.size + namedParams.size)
          val callee = results.first()

          val posArgs = results.drop(1).take(exprNode.posParams.size).map { argResult ->
            argResult.toValue()
          }
          val namedArgs = namedParams.zip(results.drop(1 + exprNode.posParams.size))
            .associate { (param, argResult) ->
              param.key to argResult.toValue()
            }

          when (callee) {
            is BuildTaskResult.BuildRuleResult -> {
              organizeParamsAndRunBuildRule(
                buildGraphRunner,
                projectId,
                importInstanceId,
                callee,
                posArgs,
                namedArgs
              ) { callResult ->
                check(callResult is BuildTaskResult.ValueOfTargetResult)
                castBuildRuleResult(callee, callResult.targetId, callResult.value)
              }
            }

            is BuildTaskResult.DataClassResult -> {
              val classType = DataClassType(callee.packageName, callee.name.toString())
              organizeParamsForDataClass(projectId, callee, posArgs, namedArgs) { value ->
                BuildTaskResult.WithResult(
                  TypeCastValue(value, classType, projectId)
                ) { casted ->
                  check(casted is BuildTaskResult.ResultWithValue)
                  BuildTaskResult.ValueResult(casted.value)
                }
              }
            }

            else -> throw IllegalStateException()
          }
        }
      }

//      is CallExprParamCoercionNode -> {
//        BuildTaskResult.WithResultList(
//          listOf(evalTask(exprNode.value), EvalCallee(projectId, importInstanceId, exprNode.callee))
//        ) { results ->
//          val value = results[0].toValue()
//
//          val calleeProjectId: Int
//          val paramTypeNodeId: TypeNodeId
//          when (val callee = results[1]) {
//            is BuildTaskResult.BuildRuleResult -> {
//              calleeProjectId = callee.projectId
//              paramTypeNodeId = when (val loc = exprNode.paramLocation) {
//                is ParamLocation.NamedParam ->
//                  callee.buildRuleDef.params.getValue(loc.name)
//
//                is ParamLocation.PosParam ->
//                  callee.buildRuleDef.params.getValue(callee.buildRuleDef.def.params[loc.idx].name)
//              }
//            }
//
//            is BuildTaskResult.ActionRuleResult -> {
//              calleeProjectId = callee.projectId
//              paramTypeNodeId = when (val loc = exprNode.paramLocation) {
//                is ParamLocation.NamedParam ->
//                  callee.actionRuleDef.params.getValue(loc.name)
//
//                is ParamLocation.PosParam ->
//                  callee.actionRuleDef.params.getValue(callee.actionRuleDef.def.params[loc.idx].name)
//              }
//            }
//
//            is BuildTaskResult.DataClassResult -> {
//              calleeProjectId = callee.projectId
//              paramTypeNodeId = when (val loc = exprNode.paramLocation) {
//                is ParamLocation.NamedParam ->
//                  callee.dataClassDef.fields.getValue(loc.name)
//
//                is ParamLocation.PosParam ->
//                  callee.dataClassDef.fields.getValue(callee.dataClassDef.def.fields[loc.idx].name)
//              }
//            }
//
//            else -> throw IllegalStateException()
//          }
//
//          BuildTaskResult.WithResult(EvalType(calleeProjectId, paramTypeNodeId)) { result ->
//            check(result is BuildTaskResult.TypeResult)
//            valueCaster.castValue(value, result.type)
//          }
//        }
//      }

      is LocalBuildRuleRef ->
        BuildTaskResult.WithResult(
          EvalBuildRuleMeta(projectId, importInstanceId, exprNode.name)
        ) { result ->
          check(result is BuildTaskResult.BuildRuleMetaResult)

          val source = when (result.projectId) {
            1 -> MainSourceId
            2 -> PreludeSourceId
            else -> ExternSourceId(result.projectId)
          }

          val paramTypes = result.paramTypes.toMap()
          BuildTaskResult.ValueResult(
            BuildRuleDefValue(
              CName(source, result.name.tokens),
              result.buildRuleDef.def.params.map { param ->
                RuleParam(param.name, paramTypes.getValue(param.name).toTypeValue(), param.optional)
              },
              result.buildRuleDef.implClassName,
              result.buildRuleDef.implMethodName,
            )
          )
        }

      is LocalDataClassRef ->
        BuildTaskResult.WithResult(
          EvalDataClass(projectId, importInstanceId, exprNode.name)
        ) { result ->
          check(result is BuildTaskResult.DataClassResult)
          // TODO DataClassTypeValue 값을 반환하도록 수정
          result
        }

      is LocalTargetRef ->
        BuildTaskResult.WithResult(
          EvalTarget(projectId, importInstanceId, exprNode.name)
        ) { result ->
          check(result is BuildTaskResult.ResultWithValue)
          result
        }

      is LocalVarRef ->
        BuildTaskResult.WithResult(EvalVar(projectId, importInstanceId, exprNode.name)) { result ->
          check(result is BuildTaskResult.ResultWithValue)
          result
        }

      is LocalActionRef ->
        // TODO 이건 어떻게 해야되지?
        BuildTaskResult.WithResult(
          EvalAction(projectId, importInstanceId, exprNode.name)
        ) { result ->
          check(result is BuildTaskResult.ActionResult)
          result
        }

      is LocalActionRuleRef ->
        // TODO ActionRuleDefValue 값을 반환하도록 수정
        BuildTaskResult.WithResult(
          EvalActionRule(projectId, importInstanceId, exprNode.name)
        ) { result ->
          check(result is BuildTaskResult.ActionRuleResult)
          result
        }

      is ImportedExpr ->
        BuildTaskResult.WithResult(Import(projectId, importInstanceId, exprNode.import)) { result ->
          when (result) {
            is BuildTaskResult.ResultWithValue -> {
              BuildTaskResult.ValueResult(result.value.followMemberNames(exprNode.name.tokens))
            }

            is BuildTaskResult.ImportInstanceResult -> {
              val graph = multiGraph.getProjectGraph(result.projectId)
              when (val entity = graph.findName(exprNode.name)) {
                is BuildGraphEntity.Target ->
                  BuildTaskResult.WithResult(
                    EvalExpr(result.projectId, entity.exprNodeId, result.importInstanceId, null)
                  ) { it }

                is BuildGraphEntity.Variable ->
                  BuildTaskResult.WithResult(
                    EvalVar(result.projectId, result.importInstanceId, exprNode.name)
                  ) { it }

                is BuildGraphEntity.BuildRule ->
                  BuildTaskResult.WithResult(
                    EvalBuildRule(result.projectId, result.importInstanceId, exprNode.name)
                  ) { buildRule ->
                    check(buildRule is BuildTaskResult.BuildRuleResult)

                    val paramTypes = buildRule.paramTypes.toMap()
                    val params = buildRule.buildRuleDef.def.params.map { param ->
                      RuleParam(
                        param.name,
                        paramTypes.getValue(param.name).toTypeValue(),
                        param.optional
                      )
                    }
                    BuildTaskResult.ValueResult(
                      BuildRuleDefValue(
                        // TODO cname source 제대로 주기
                        CName(MainSourceId, exprNode.name.tokens),
                        params,
                        when (val impl = buildRule.impl) {
                          is BuildTaskResult.BuildRuleImpl.NativeImpl -> impl.implInstance::class.java.canonicalName
                          is BuildTaskResult.BuildRuleImpl.NonNativeImpl -> impl.implClassName
                        },
                        buildRule.implMethodName
                      )
                    )
                  }

                is BuildGraphEntity.DataClass ->
                  BuildTaskResult.ValueResult(
                    TypeValue.DataClassTypeValue(
                      checkNotNull(graph.packageName),
                      exprNode.name.toString()
                    )
                  )

                is BuildGraphEntity.SuperClass ->
                  BuildTaskResult.ValueResult(
                    TypeValue.SuperClassTypeValue(
                      checkNotNull(graph.packageName),
                      exprNode.name.toString()
                    )
                  )

                is BuildGraphEntity.Enum ->
                  BuildTaskResult.ValueResult(
                    TypeValue.EnumTypeValue(
                      checkNotNull(graph.packageName),
                      exprNode.name.toString()
                    )
                  )

                is BuildGraphEntity.ActionRule -> TODO()

                is BuildGraphEntity.ImportAll, is BuildGraphEntity.ImportFrom,
                is BuildGraphEntity.Action, null -> throw IllegalStateException("Invalid expression")
              }
            }

            else -> throw IllegalStateException("Invalid expression")
          }
        }

      is ImportedExprFromPrelude -> {
        val preludeGraph = multiGraph.getProjectGraph(2)

        val target = preludeGraph.targets[BibixName(exprNode.name.tokens.first())]
        if (target != null) {
          BuildTaskResult.WithResult(EvalExpr(2, target, 0, null)) { targetResult ->
            check(targetResult is BuildTaskResult.ResultWithValue)
            BuildTaskResult.ValueResult(
              targetResult.value.followMemberNames(exprNode.name.tokens.drop(1))
            )
          }
        } else {
          BuildTaskResult.WithResult(ImportFromPrelude(exprNode.name)) { it }
        }
      }

      is ValueCastNode ->
        BuildTaskResult.WithResultList(
          listOf(evalTask(exprNode.value), EvalType(projectId, exprNode.type))
        ) { results ->
          val valueResult = results[0]
          check(valueResult is BuildTaskResult.ResultWithValue)
          val typeResult = results[1]
          check(typeResult is BuildTaskResult.TypeResult)

          valueCaster.castValue(valueResult.value, typeResult.type)
        }

      is ActionLocalLetNode ->
        BuildTaskResult.ValueResult(localLets.getValue(exprNode.name))
    }

  private fun castBuildRuleResult(
    buildRule: BuildTaskResult.BuildRuleResult,
    targetId: String,
    value: BibixValue
  ): BuildTaskResult = BuildTaskResult.WithResult(
    EvalType(buildRule.projectId, buildRule.buildRuleDef.returnType)
  ) { typeResult ->
    check(typeResult is BuildTaskResult.TypeResult)

    BuildTaskResult.WithResult(
      FinalizeBuildRuleReturnValue(BuildRuleDefContext.from(buildRule), value, projectId)
    ) { finalized ->
      // finalized가 ValueFinalizeFailResult 이면 안됨
      check(finalized is BuildTaskResult.ValueResult)

      BuildTaskResult.WithResult(
        TypeCastValue(finalized.value, typeResult.type, projectId)
      ) { casted ->
        check(casted is BuildTaskResult.ValueResult)
        buildGraphRunner.repo.targetSucceeded(targetId, casted.value)
        BuildTaskResult.ValueOfTargetResult(casted.value, targetId)
      }
    }
  }

  fun evaluateCallExpr(buildTask: EvalCallExpr): BuildTaskResult {
    val graph = multiGraph.getProjectGraph(buildTask.buildRuleDefCtx.projectId)

    fun call(buildRule: BuildTaskResult.BuildRuleResult): BuildTaskResult =
      organizeParamsAndRunBuildRule(
        buildGraphRunner,
        // TODO 여기서 주는 projectId와 importInstanceId가 이게 맞나?
        buildTask.buildRuleDefCtx.projectId,
        buildTask.buildRuleDefCtx.importInstanceId,
        buildRule,
        listOf(),
        buildTask.params
      ) { evalResult ->
        check(evalResult is BuildTaskResult.ValueOfTargetResult)

        castBuildRuleResult(buildRule, evalResult.targetId, evalResult.value)
      }

    return when (val entity = graph.findName(buildTask.ruleName)) {
      is BuildGraphEntity.BuildRule -> {
        BuildTaskResult.WithResult(
          EvalBuildRule(
            buildTask.buildRuleDefCtx.projectId,
            buildTask.buildRuleDefCtx.importInstanceId,
            buildTask.ruleName
          )
        ) { buildRule ->
          check(buildRule is BuildTaskResult.BuildRuleResult)
          call(buildRule)
        }
      }

      null -> {
        buildGraphRunner.lookupFromImport(
          graph,
          buildTask.buildRuleDefCtx.projectId,
          buildTask.buildRuleDefCtx.importInstanceId,
          buildTask.ruleName
        ) { buildRule ->
          check(buildRule is BuildTaskResult.BuildRuleResult)
          call(buildRule)
        }
      }

      else -> throw IllegalStateException("Invalid rule name: $entity")
    }
  }
}

fun BibixValue.followMemberNames(memberNames: List<String>): BibixValue {
  if (memberNames.isEmpty()) {
    return this
  }
  val firstName = memberNames.first()
  return when (this) {
    is ClassInstanceValue ->
      checkNotNull(this.fieldValues[firstName])
        .followMemberNames(memberNames.drop(1))

    is NamedTupleValue ->
      this.getValue(firstName)
        .followMemberNames(memberNames.drop(1))

    else -> throw IllegalStateException()
  }
}

fun BuildGraphRunner.lookupDataClass(
  projectId: Int,
  importInstanceId: Int,
  dataClassName: BibixName,
  block: (BuildTaskResult.DataClassResult) -> BuildTaskResult
): BuildTaskResult {
  // TODO namespace에서 마지막에서 하나씩 빼면서 이름 찾기 시도
  val buildGraph = multiGraph.getProjectGraph(projectId)

  if (dataClassName in buildGraph.dataClasses) {
    return BuildTaskResult.WithResult(
      EvalDataClass(projectId, importInstanceId, dataClassName)
    ) { dataClass ->
      check(dataClass is BuildTaskResult.DataClassResult)
      block(dataClass)
    }
  }

  return lookupFromImport(buildGraph, projectId, importInstanceId, dataClassName) { result ->
    check(result is BuildTaskResult.DataClassResult) { "Invalid data class name" }
    block(result)
  }
}

fun BuildGraphRunner.lookupFromImport(
  buildGraph: BuildGraph,
  projectId: Int,
  importInstanceId: Int,
  name: BibixName,
  block: (BuildTaskResult.FinalResult) -> BuildTaskResult
): BuildTaskResult {
  buildGraph.importAlls.forEach { (importName, importAll) ->
    if (name.tokens.take(importName.tokens.size) == importName.tokens) {
      return BuildTaskResult.WithResult(
        Import(projectId, importInstanceId, importName)
      ) { result ->
        check(result is BuildTaskResult.ImportInstanceResult)

        importResultFrom(result, BibixName(name.tokens.drop(importName.tokens.size)), block)
      }
    }
  }
  buildGraph.importFroms.forEach { (importName, importFrom) ->
    if (name.tokens.take(importName.tokens.size) == importName.tokens) {
      // TODO 이런 경우는 언제 생기는거지?
      TODO()
    }
  }
  throw IllegalStateException("Name not found")
}

fun organizeParamsForDataClass(
  callerProjectId: Int,
  dataClass: BuildTaskResult.DataClassResult,
  posArgs: List<BibixValue>,
  namedArgs: Map<String, BibixValue>,
  func: (ClassInstanceValue) -> BuildTaskResult
): BuildTaskResult {
  return organizeParams(
    callerProjectId,
    dataClass.fieldTypes,
    dataClass.dataClassDef.def.fields.requiredParamNames(),
    dataClass.projectId,
    dataClass.importInstanceId,
    dataClass.dataClassDef.fieldDefaultValues,
    posArgs,
    namedArgs,
  ) { args ->
    func(ClassInstanceValue(dataClass.packageName, dataClass.name.toString(), args))
  }
}

fun organizeParams(
  callerProjectId: Int,
  paramTypes: List<Pair<String, BibixType>>,
  requiredParamNames: Set<String>,
  calleeProjectId: Int,
  calleeImportInstanceId: Int,
  defaultValues: Map<String, ExprNodeId>,
  posArgs: List<BibixValue>,
  namedArgs: Map<String, BibixValue>,
  whenReady: (args: Map<String, BibixValue>) -> BuildTaskResult
): BuildTaskResult {
  val paramTypesMap = paramTypes.toMap()
  val paramNames = paramTypes.map { it.first }

  // callee의 parameter 목록을 보고 posParams와 namedParams와 맞춰본다
  val posArgCastTasks = paramNames.zip(posArgs) { name, arg ->
    TypeCastValue(arg, paramTypesMap.getValue(name), callerProjectId)
  }
  val namedArgPairs = namedArgs.entries.toList()
  val namedArgCastTasks = namedArgPairs.map { (name, arg) ->
    TypeCastValue(arg, paramTypesMap.getValue(name), callerProjectId)
  }

  val remainingParamNames = paramNames.drop(posArgs.size).toSet()
  check(remainingParamNames.containsAll(namedArgs.keys)) { "Unknown parameters" }

  val unspecifiedParamNames = remainingParamNames - namedArgs.keys
  check(unspecifiedParamNames.all { it !in requiredParamNames }) { "Required parameters are not specified" }

  val defaultArgTasks = unspecifiedParamNames.mapNotNull { paramName ->
    // default arg는 cast가 완료된 상태로 넘어온다
    defaultValues[paramName]?.let {
      paramName to EvalExpr(calleeProjectId, it, calleeImportInstanceId, null)
    }
  }
  val noneArgs = (unspecifiedParamNames - (defaultArgTasks.map { it.first }.toSet()))
    .associateWith { NoneValue }

  // TODO default arg tasks도 합치기
  return BuildTaskResult.WithResultList(posArgCastTasks + namedArgCastTasks) { results ->
    val posArgResults = results.take(posArgCastTasks.size)
    val namedArgResults = results.drop(posArgCastTasks.size)

    val posArgValues = posArgResults.map {
      check(it is BuildTaskResult.ResultWithValue)
      it.value
    }
    val namedArgValues = namedArgResults.map {
      check(it is BuildTaskResult.ResultWithValue)
      it.value
    }
    val posArgsMap = paramNames.zip(posArgValues).toMap()
    val namedArgsMap = namedArgPairs.zip(namedArgValues).map { (param, casted) ->
      param.key to casted
    }.toMap()

    if (defaultArgTasks.isEmpty()) {
      whenReady(posArgsMap + namedArgsMap + noneArgs)
    } else {
      BuildTaskResult.WithResultList(defaultArgTasks.map { it.second }) { results ->
        check(results.size == defaultArgTasks.size)
        val defaultArgs = defaultArgTasks.zip(results).associate { (pair, result) ->
          check(result is BuildTaskResult.ResultWithValue)
          pair.first to result.value
        }
        whenReady(posArgsMap + namedArgsMap + defaultArgs + noneArgs)
      }
    }
  }
}

fun argsMapFrom(args: Map<String, BibixValue>): ArgsMap =
  argsMap {
    args.entries.toList()
      .sortedBy { it.key }
      .forEach { (name, value) ->
        this.pairs.add(argPair {
          this.name = name
          this.value = value.toProto()
        })
      }
  }

fun BuildTaskResult.FinalResult.toValue(): BibixValue = when (this) {
  is BuildTaskResult.ResultWithValue -> this.value

  is BuildTaskResult.DataClassResult ->
    TypeValue.DataClassTypeValue(this.packageName, this.name.toString())

  else -> throw IllegalStateException()
}