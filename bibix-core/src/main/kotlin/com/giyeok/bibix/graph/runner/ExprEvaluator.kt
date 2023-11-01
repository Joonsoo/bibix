package com.giyeok.bibix.graph.runner

import com.giyeok.bibix.*
import com.giyeok.bibix.BibixIdProto.ArgsMap
import com.giyeok.bibix.ast.BibixAst
import com.giyeok.bibix.base.*
import com.giyeok.bibix.graph.*
import com.giyeok.bibix.repo.BibixRepoProto
import com.giyeok.bibix.repo.hashString
import com.giyeok.bibix.utils.toBibix
import com.giyeok.bibix.utils.toHexString
import com.giyeok.bibix.utils.toInstant
import com.giyeok.bibix.utils.toProto
import com.google.protobuf.empty
import kotlinx.coroutines.runBlocking

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

  private val valueCaster: ValueCaster
    get() = ValueCaster(buildGraphRunner, projectId, importInstanceId)

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
              organizeParamsAndRunBuildRule(callee, posArgs, namedArgs) { it }
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
      is LocalDataClassRef ->
        BuildTaskResult.WithResult(EvalDataClass(projectId, importInstanceId, exprNode.name)) { it }

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

          handleImportResult(result, exprNode.import, exprNode.name)
        }

      is PreloadedPluginRef ->
        BuildTaskResult.WithResult(ImportPreloaded(exprNode.pluginName)) { it }

      is ImportedExprFromPreloaded ->
        BuildTaskResult.WithResult(ImportPreloaded(exprNode.pluginName)) { result ->
          check(result is BuildTaskResult.ImportResult)

          handleImportResult(result, BibixName(exprNode.pluginName), exprNode.name)
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

      is ActionLocalLetNode ->
        BuildTaskResult.ValueResult(localLets.getValue(exprNode.name))
    }

  private fun handleImportResult(
    result: BuildTaskResult.ImportResult,
    import: BibixName,
    importedMemberName: BibixName,
  ): BuildTaskResult = buildGraphRunner.handleImportResult(
    result,
    import,
    importedMemberName,
    projectId,
    importInstanceId
  ) { it }

  private fun sourceIdFrom(projectId: Int): BibixIdProto.SourceId {
    // TODO 구현하기
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
      // TODO
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

    // TODO: BibixRepo 기능 정리
    //  - target들의 실행 상태 잘 기록하기
    val repo = buildGraphRunner.repo
    val prevInputHashes = runBlocking { repo.getPrevInputsHashOf(targetId) }
    val hashChanged = if (prevInputHashes == null) true else prevInputHashes != argsMap.hashString()

    // TODO: 같은 run에선 같은 target id는 값을 재활용하자

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

  private fun organizeParamsAndRunBuildRule(
    buildRule: BuildTaskResult.BuildRuleResult,
    posArgs: List<BibixValue>,
    namedArgs: Map<String, BibixValue>,
    block: (BuildTaskResult) -> BuildTaskResult
  ): BuildTaskResult {
    val requiredParams = buildRule.buildRuleDef.def.params
      .filter { param -> !param.optional && param.defaultValue == null }
      .map { it.name }.toSet()
    return organizeParams(
      buildRule.paramTypes,
      requiredParams,
      buildRule.projectId,
      buildRule.importInstanceId,
      buildRule.buildRuleDef.paramDefaultValues,
      posArgs,
      namedArgs,
    ) { args ->
      val buildContext = createBuildContext(projectId, buildRule, args)
      runBuildRule(buildRule, buildContext, block)
    }
  }

  private fun runBuildRule(
    buildRule: BuildTaskResult.BuildRuleResult,
    buildContext: BuildContext,
    block: (BuildTaskResult) -> BuildTaskResult
  ): BuildTaskResult = BuildTaskResult.LongRunning {
    val result = buildRule.implMethod.invoke(buildRule.implInstance, buildContext)

    fun handleBuildRuleReturn(result: BuildRuleReturn): BuildTaskResult = when (result) {
      is BuildRuleReturn.ValueReturn -> block(BuildTaskResult.ValueResult(result.value))
      is BuildRuleReturn.FailedReturn -> throw result.exception
      BuildRuleReturn.DoneReturn -> throw IllegalStateException()

      is BuildRuleReturn.EvalAndThen -> {
        buildGraphRunner.lookupExprValue(
          buildRule.projectId,
          BibixName(result.ruleName),
          buildRule.importInstanceId,
        ) { lookupResult ->
          check(lookupResult is BuildTaskResult.BuildRuleResult)

          val params = result.params.entries
          BuildTaskResult.WithResultList(params.map {
            FinalizeBuildRuleReturnValue(
              buildRule,
              it.value,
              buildRule.projectId,
              buildRule.importInstanceId
            )
          }) { finalized ->
            val finValues = finalized.map {
              check(it is BuildTaskResult.ValueResult)
              it.value
            }
            val finalizedParams = params.map { it.key }.zip(finValues).toMap()
            organizeParamsAndRunBuildRule(lookupResult, listOf(), finalizedParams) { evalResult ->
              // TODO evalResult를 buildRuleResult의 return type으로 cast
              check(evalResult is BuildTaskResult.ValueResult)
              handleBuildRuleReturn(result.whenDone(evalResult.value))
            }
          }
        }
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
      is BibixValue -> block(BuildTaskResult.ValueResult(result))
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

fun BuildGraphRunner.lookupExprValue(
  projectId: Int,
  name: BibixName,
  importInstanceId: Int,
  block: (BuildTaskResult) -> BuildTaskResult,
): BuildTaskResult {
  val buildGraph = multiGraph.getProjectGraph(projectId)

  val target = buildGraph.targets[name]
  val buildRule = buildGraph.buildRules[name]
  val varDef = buildGraph.vars[name]
  val dataClass = buildGraph.dataClasses[name]

  // TODO import도 처리해줘야 할듯..

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

    else -> {
      // graph.imports에서 보고 ImportedExpr task
      buildGraph.importAlls.forEach { (importName, importAll) ->
        if (name.tokens.take(importName.tokens.size) == importName.tokens) {
          return BuildTaskResult.WithResult(
            Import(projectId, importInstanceId, importName)
          ) { result ->
            check(result is BuildTaskResult.ImportResult)

            handleImportResult(
              result,
              importName,
              BibixName(name.tokens.drop(importName.tokens.size)),
              projectId,
              importInstanceId,
              block
            )
          }
        }
      }
      buildGraph.importFroms.forEach { (importName, importFrom) ->
        if (name.tokens.take(importName.tokens.size) == importName.tokens) {
          TODO()
        }
      }
      throw IllegalStateException()
    }
  }
  return BuildTaskResult.WithResult(followingTask, block)
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

fun BuildGraphRunner.handleImportResult(
  result: BuildTaskResult.ImportResult,
  import: BibixName,
  importedMemberName: BibixName,
  projectId: Int,
  importInstanceId: Int,
  block: (BuildTaskResult) -> BuildTaskResult,
): BuildTaskResult {
  val importerGraph = multiGraph.getProjectGraph(projectId)
  val newRedefs = importerGraph.varRedefs[import] ?: mapOf()
  val newGlobalRedefs = newRedefs.mapValues { (_, exprNodeId) ->
    GlobalExprNodeId(projectId, importInstanceId, exprNodeId)
  }

  return BuildTaskResult.WithResult(
    NewImportInstance(result.projectId, newGlobalRedefs)
  ) { instance ->
    check(instance is BuildTaskResult.ImportInstanceResult)
    check(result.projectId == instance.projectId)

    lookupExprValue(
      result.projectId,
      BibixName(result.namePrefix + importedMemberName.tokens),
      instance.importInstanceId,
      block
    )
  }
}