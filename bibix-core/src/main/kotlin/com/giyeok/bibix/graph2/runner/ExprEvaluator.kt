package com.giyeok.bibix.graph2.runner

import com.giyeok.bibix.*
import com.giyeok.bibix.BibixIdProto.ArgsMap
import com.giyeok.bibix.ast.BibixAst
import com.giyeok.bibix.base.*
import com.giyeok.bibix.graph2.*
import com.giyeok.bibix.repo.BibixRepoProto
import com.giyeok.bibix.repo.TargetId
import com.giyeok.bibix.repo.hashString
import com.giyeok.bibix.utils.toBibix
import com.giyeok.bibix.utils.toHexString
import com.giyeok.bibix.utils.toInstant
import com.giyeok.bibix.utils.toProto
import com.google.protobuf.empty
import kotlinx.coroutines.runBlocking
import java.lang.StringBuilder
import java.nio.file.Path

class ExprEvaluator(
  private val buildGraphRunner: BuildGraphRunner,
  private val projectId: Int,
  private val importInstanceId: Int,
  private val thisValue: ClassInstanceValue?
) {
  private val multiGraph get() = buildGraphRunner.multiGraph
  private val projectPackageName: String? get() = multiGraph.projectPackages[projectId]
  private val projectLocation: BibixProjectLocation? get() = multiGraph.projectLocations[projectId]

  private val buildGraph get() = multiGraph.getProjectGraph(projectId)
  private val varRedefs: Map<BibixName, Map<Int, BuildGraph.VarCtx>> get() = buildGraph.varRedefs
  private val exprGraph: ExprGraph get() = buildGraph.exprGraph

  private val valueCaster: ValueCaster
    get() = ValueCaster(projectId, projectLocation, importInstanceId) { this.copy(it) }

  private fun evalTask(exprNodeId: ExprNodeId) =
    EvalExpr(projectId, exprNodeId, importInstanceId, thisValue)

  private fun copy(thisValue: ClassInstanceValue?) =
    ExprEvaluator(buildGraphRunner, projectId, importInstanceId, thisValue)

  fun evaluateExpr(exprNodeId: ExprNodeId): BuildTaskResult =
    when (val exprNode = exprGraph.nodes.getValue(exprNodeId)) {
      is NoneLiteralNode -> BuildTaskResult.ValueResult(NoneValue)
      is BooleanLiteralNode -> BuildTaskResult.ValueResult(BooleanValue(exprNode.expr.value))
      is ThisRefNode -> BuildTaskResult.ValueResult(checkNotNull(thisValue))

      is MemberAccessNode ->
        BuildTaskResult.WithResult(evalTask(exprNode.target)) { target ->
          when (target) {
            is BuildTaskResult.ValueResult -> {
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
          val elemValues = elems.map { elem ->
            check(elem is BuildTaskResult.ValueResult)
            elem.value
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
          val elemValues = elems.map { elem ->
            check(elem is BuildTaskResult.ValueResult)
            elem.value
          }
          BuildTaskResult.ValueResult(TupleValue(elemValues))
        }

      is NamedTupleNode ->
        BuildTaskResult.WithResultList(exprNode.elems.map { evalTask(it.second) }) { elems ->
          check(elems.size == exprNode.elems.size)
          val elemValues = exprNode.elems.zip(elems).map { (pair, elem) ->
            check(elem is BuildTaskResult.ValueResult)
            pair.first to elem.value
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
          check(lhsResult is BuildTaskResult.ValueResult && rhsResult is BuildTaskResult.ValueResult)
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
                val value = elems[elemIdx++]
                check(value is BuildTaskResult.ValueResult && value.value is StringValue)
                builder.append(value.value)
              }
            }
          }
          check(elemIdx == elems.size)
          BuildTaskResult.ValueResult(StringValue(builder.toString()))
        }
      }

      is CallExprCallNode -> {
        val namedParams = exprNode.namedParams.entries.sortedBy { it.key }
        BuildTaskResult.WithResultList(
          listOf(evalTask(exprNode.callee)) +
            exprNode.posParams.map { evalTask(it) } +
            namedParams.map { evalTask(it.value) }
        ) { results ->
          check(results.size == 1 + exprNode.posParams.size + namedParams.size)
          val callee = results.first()

          val posArgs = results.drop(1).take(exprNode.posParams.size).map { argResult ->
            check(argResult is BuildTaskResult.ValueResult)
            argResult.value
          }
          val namedArgs = namedParams.zip(results.drop(1 + exprNode.posParams.size))
            .associate { (param, argResult) ->
              check(argResult is BuildTaskResult.ValueResult)
              param.key to argResult.value
            }

          when (callee) {
            is BuildTaskResult.BuildRuleResult -> {
              val requiredParams = callee.buildRuleDef.def.params
                .filter { param -> !param.optional && param.defaultValue == null }
                .map { it.name }.toSet()
              organizeParams(
                callee.paramTypes,
                requiredParams,
                callee.projectId,
                callee.importInstanceId,
                callee.buildRuleDef.paramDefaultValues,
                posArgs,
                namedArgs,
              ) { args ->
                val buildContext = createBuildContext(projectId, callee, args)
                runBuildRule(callee, buildContext)
              }
            }

            is BuildTaskResult.DataClassResult -> {
              organizeParamsForDataClass(callee, posArgs, namedArgs)
            }

            else -> throw IllegalStateException()
          }
        }
      }

      is CallExprNode -> {
        BuildTaskResult.WithResultList(
          listOf(evalTask(exprNode.callee), evalTask(exprNode.callNode))
        ) { results ->
          check(results.size == 2)
          val valueResult = results[1]
          check(valueResult is BuildTaskResult.ValueResult)
          val value = valueResult.value

          // value를 callee의 returnType으로 cast
          when (val callee = results[0]) {
            is BuildTaskResult.BuildRuleResult -> {
              BuildTaskResult.WithResult(
                EvalType(callee.projectId, callee.buildRuleDef.returnType)
              ) { typeResult ->
                check(typeResult is BuildTaskResult.TypeResult)

                BuildTaskResult.WithResult(
                  FinalizeBuildRuleReturnValue(callee, value, projectId, importInstanceId)
                ) { finalized ->
                  // finalized가 ValueFinalizeFailResult 이면 안됨
                  check(finalized is BuildTaskResult.ValueResult)
                  valueCaster.castValue(finalized.value, typeResult.type)
                }
              }
            }

            is BuildTaskResult.DataClassResult -> {
              val classType = DataClassType(callee.packageName, callee.name.toString())
              valueCaster.castValue(value, classType)
            }

            else -> throw IllegalStateException()
          }
        }
      }

      is CallExprParamCoercionNode -> {
        BuildTaskResult.WithResultList(
          listOf(evalTask(exprNode.value), evalTask(exprNode.callee))
        ) { results ->
          val valueResult = results[0]
          check(valueResult is BuildTaskResult.ValueResult)
          val value = valueResult.value

          val calleeProjectId: Int
          val paramTypeNodeId: TypeNodeId
          when (val callee = results[1]) {
            is BuildTaskResult.BuildRuleResult -> {
              calleeProjectId = callee.projectId
              paramTypeNodeId = when (val loc = exprNode.paramLocation) {
                is ParamLocation.NamedParam ->
                  callee.buildRuleDef.params.getValue(loc.name)

                is ParamLocation.PosParam ->
                  callee.buildRuleDef.params.getValue(callee.buildRuleDef.def.params[loc.idx].name)
              }
            }

            is BuildTaskResult.DataClassResult -> {
              calleeProjectId = callee.projectId
              paramTypeNodeId = when (val loc = exprNode.paramLocation) {
                is ParamLocation.NamedParam ->
                  callee.dataClassDef.fields.getValue(loc.name)

                is ParamLocation.PosParam ->
                  callee.dataClassDef.fields.getValue(callee.dataClassDef.def.fields[loc.idx].name)
              }
            }

            else -> throw IllegalStateException()
          }

          BuildTaskResult.WithResult(EvalType(calleeProjectId, paramTypeNodeId)) { result ->
            check(result is BuildTaskResult.TypeResult)

            valueCaster.castValue(value, result.type)
          }
        }
      }

      is LocalBuildRuleRef -> TODO()
      is LocalDataClassRef -> TODO()

      is LocalTargetRef ->
        BuildTaskResult.WithResult(
          EvalTarget(projectId, importInstanceId, exprNode.name)
        ) { result ->
          check(result is BuildTaskResult.ValueResult)
          result
        }

      is LocalVarRef ->
        BuildTaskResult.WithResult(EvalVar(projectId, importInstanceId, exprNode.name)) { result ->
          check(result is BuildTaskResult.ValueResult)
          result
        }

      is ImportedExpr ->
        BuildTaskResult.WithResult(Import(projectId, importInstanceId, exprNode.import)) { result ->
          check(result is BuildTaskResult.ImportResult)

          handleImportResult(result, exprNode.import, exprNode.varCtxId, exprNode.name)
        }

      is ImportedExprFromPreloaded ->
        BuildTaskResult.WithResult(ImportPreloaded(exprNode.pluginName)) { result ->
          check(result is BuildTaskResult.ImportResult)

          handleImportResult(
            result,
            BibixName(exprNode.pluginName),
            exprNode.varCtxId,
            exprNode.name
          )
        }

      is ImportedExprFromPrelude -> {
        BuildTaskResult.WithResult(ImportFromPrelude(exprNode.name, exprNode.remaining)) { it }
      }

      is ValueCastNode ->
        BuildTaskResult.WithResultList(
          listOf(evalTask(exprNode.value), EvalType(projectId, exprNode.type))
        ) { results ->
          val valueResult = results[0]
          check(valueResult is BuildTaskResult.ValueResult)
          val typeResult = results[1]
          check(typeResult is BuildTaskResult.TypeResult)

          valueCaster.castValue(valueResult.value, typeResult.type)
        }
    }

  private fun handleImportResult(
    result: BuildTaskResult.ImportResult,
    import: BibixName,
    varCtxId: Int,
    importedMemberName: BibixName
  ): BuildTaskResult {
    fun Map<Int, BuildGraph.VarCtx>.mergeForCtxId(varCtxId: Int): Map<BibixName, ExprNodeId> {
      val varCtx = this[varCtxId]
      if (varCtx == null) {
        check(varCtxId == 0)
        return mapOf()
      }
      val parent = if (varCtxId == 0 && varCtx.parentCtxId == 0) {
        mapOf()
      } else {
        mergeForCtxId(varCtx.parentCtxId)
      }
      return parent + varCtx.redefs
    }

    val newRedefs = varRedefs[import]?.mergeForCtxId(varCtxId) ?: mapOf()
    val newGlobalRedefs = newRedefs.mapValues { (_, exprNodeId) ->
      GlobalExprNodeId(projectId, importInstanceId, exprNodeId)
    }

    return BuildTaskResult.WithResult(
      NewImportInstance(result.projectId, newGlobalRedefs)
    ) { instance ->
      check(instance is BuildTaskResult.ImportInstanceResult)
      check(result.projectId == instance.projectId)

      lookupExprValue(result.graph, importedMemberName, result.projectId, instance.importInstanceId)
    }
  }

  private fun sourceIdFrom(projectId: Int): BibixIdProto.SourceId {
    return sourceId {
      this.bibixVersion = Constants.BIBIX_VERSION
    }
  }

  private fun createBuildContext(
    callerProjectId: Int,
    buildRule: BuildTaskResult.BuildRuleResult,
    args: Map<String, BibixValue>
  ): BuildContext {
    val mainLocation = multiGraph.projectLocations.getValue(1)
    val callerLocation = multiGraph.projectLocations[callerProjectId]
    val ruleDefinedLocation = multiGraph.projectLocations[buildRule.projectId]

    val argsMap = argsMapFrom(args)
    val targetIdData = targetIdData {
      this.sourceId = sourceIdFrom(callerProjectId)
      this.buildRule = buildRuleData {
        this.buildRuleSourceId = sourceIdFrom(buildRule.projectId)
        if (buildRule.buildRuleDef.implTarget == null) {
          this.nativeImpl = empty { }
        } else {
          // TODO
          this.bibixValueHash
        }
        this.buildRuleClassName = buildRule.buildRuleDef.implClassName
        this.buildRuleMethodName = buildRule.buildRuleDef.implMethodName
      }
      this.argsMap = argsMap
    }
    val targetId = targetIdData.hashString()
    val targetIdHex = targetId.toHexString()

    val repo = buildGraphRunner.repo
    val prevInputHashes = runBlocking { repo.getPrevInputsHashOf(targetId) }
    val hashChanged = if (prevInputHashes == null) true else prevInputHashes != argsMap.hashString()

    val targetState = runBlocking { repo.getPrevTargetState(targetId) }
    val prevResult = targetState?.let {
      if (targetState.stateCase == BibixRepoProto.TargetState.StateCase.BUILD_SUCCEEDED) {
        targetState.buildSucceeded.resultValue.toBibix()
      } else {
        null
      }
    }

    return BuildContext(
      buildEnv = buildGraphRunner.buildEnv,
      fileSystem = buildGraphRunner.fileSystem,
      mainBaseDirectory = mainLocation.projectRoot,
      callerBaseDirectory = callerLocation?.projectRoot,
      ruleDefinedDirectory = ruleDefinedLocation?.projectRoot,
      arguments = args,
      targetIdData = targetIdData,
      targetId = targetIdHex,
      hashChanged = hashChanged,
      prevBuildTime = targetState?.buildStartTime?.toInstant(),
      prevResult = prevResult,
      destDirectoryPath = repo.objectsDirectory.resolve(targetIdHex),
      progressLogger = object: ProgressLogger {
        override fun logInfo(message: String) {
          println(message)
        }

        override fun logError(message: String) {
          println(message)
        }
      },
      repo = repo
    )
  }

  private fun runBuildRule(
    buildRule: BuildTaskResult.BuildRuleResult,
    buildContext: BuildContext
  ): BuildTaskResult = BuildTaskResult.LongRunning {
    val result = buildRule.implMethod.invoke(buildRule.implInstance, buildContext)

    fun handleBuildRuleReturn(result: BuildRuleReturn): BuildTaskResult = when (result) {
      is BuildRuleReturn.ValueReturn -> BuildTaskResult.ValueResult(result.value)
      is BuildRuleReturn.FailedReturn -> throw result.exception
      BuildRuleReturn.DoneReturn -> throw IllegalStateException()

      is BuildRuleReturn.EvalAndThen -> {
        TODO()
      }

      is BuildRuleReturn.GetTypeDetails -> {
        TODO()
      }

      is BuildRuleReturn.WithDirectoryLock -> {
        // TODO directory lock for result.directory
        handleBuildRuleReturn(result.withLock())
      }
    }

    when (result) {
      is BibixValue -> BuildTaskResult.ValueResult(result)
      is BuildRuleReturn -> handleBuildRuleReturn(result)
      else -> throw IllegalStateException("Unsupported return value from build rule: $result")
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

fun lookupExprValue(
  graph: BuildGraph,
  name: BibixName,
  projectId: Int,
  importInstanceId: Int
): BuildTaskResult.WithResult {
  val target = graph.targets[name]
  val buildRule = graph.buildRules[name]
  val varDef = graph.vars[name]
  val dataClass = graph.dataClasses[name]

  // TODO import는 어떻게 하지? import까지 밖에서 쓸 수 있게 할 필요는 없지 않을까?

  val followingTask = when {
    target != null -> {
      check(buildRule == null && varDef == null && dataClass == null)
      EvalTarget(projectId, importInstanceId, name)
    }

    buildRule != null -> {
      check(varDef == null && dataClass == null)
      EvalBuildRule(projectId, importInstanceId, name)
    }

    varDef != null -> {
      check(dataClass == null)
      EvalVar(projectId, importInstanceId, name)
    }

    dataClass != null -> {
      EvalDataClass(projectId, importInstanceId, name)
    }

    else -> throw IllegalStateException()
  }
  return BuildTaskResult.WithResult(followingTask) { it }
}

fun organizeParamsForDataClass(
  dataClass: BuildTaskResult.DataClassResult,
  posArgs: List<BibixValue>,
  namedArgs: Map<String, BibixValue>,
): BuildTaskResult {
  val requiredFields = dataClass.dataClassDef.def.fields
    .filter { field -> !field.optional && field.defaultValue == null }
    .map { it.name }.toSet()
  return organizeParams(
    dataClass.fieldTypes,
    requiredFields,
    dataClass.projectId,
    dataClass.importInstanceId,
    dataClass.dataClassDef.fieldDefaultValues,
    posArgs,
    namedArgs
  ) { args ->
    BuildTaskResult.ValueResult(
      ClassInstanceValue(dataClass.packageName, dataClass.name.toString(), args)
    )
  }
}

fun organizeParams(
  paramTypes: List<Pair<String, BibixType>>,
  requiredParamNames: Set<String>,
  defaultValueProjectId: Int,
  defaultValueImportInstanceId: Int,
  defaultValues: Map<String, ExprNodeId>,
  posArgs: List<BibixValue>,
  namedArgs: Map<String, BibixValue>,
  whenReady: (args: Map<String, BibixValue>) -> BuildTaskResult
): BuildTaskResult {
  val paramNames = paramTypes.map { it.first }

  // callee의 parameter 목록을 보고 posParams와 namedParams와 맞춰본다
  val posArgsMap = posArgs.zip(paramNames) { arg, name -> name to arg }.toMap()

  val remainingParamNames = paramNames.drop(posArgs.size).toSet()
  check(remainingParamNames.containsAll(namedArgs.keys)) { "Unknown parameters" }

  val unspecifiedParamNames = remainingParamNames - namedArgs.keys
  check(unspecifiedParamNames.all { it !in requiredParamNames }) { "Required parameters are not specified" }

  val defaultArgTasks = unspecifiedParamNames.mapNotNull { paramName ->
    defaultValues[paramName]?.let {
      paramName to EvalExpr(defaultValueProjectId, it, defaultValueImportInstanceId, null)
    }
  }
  val noneArgs = (unspecifiedParamNames - (defaultArgTasks.map { it.first }.toSet()))
    .associateWith { NoneValue }

  return if (defaultArgTasks.isEmpty()) {
    whenReady(posArgsMap + namedArgs + noneArgs)
  } else {
    BuildTaskResult.WithResultList(defaultArgTasks.map { it.second }) { results ->
      check(results.size == defaultArgTasks.size)
      val defaultArgs = defaultArgTasks.zip(results).associate { (pair, result) ->
        check(result is BuildTaskResult.ValueResult)
        pair.first to result.value
      }
      whenReady(posArgsMap + namedArgs + defaultArgs + noneArgs)
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
