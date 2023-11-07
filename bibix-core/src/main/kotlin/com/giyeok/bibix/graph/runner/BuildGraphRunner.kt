package com.giyeok.bibix.graph.runner

import com.giyeok.bibix.ast.BibixAst
import com.giyeok.bibix.ast.BibixParser
import com.giyeok.bibix.base.*
import com.giyeok.bibix.graph.*
import com.giyeok.bibix.plugins.PluginInstanceProvider
import com.giyeok.bibix.plugins.PreloadedPlugin
import com.giyeok.bibix.plugins.jvm.ClassPkg
import com.giyeok.bibix.repo.BibixRepo
import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import com.google.common.collect.ImmutableBiMap
import java.nio.file.FileSystem

class BuildGraphRunner(
  val multiGraph: MultiBuildGraph,
  // project id -> import instance id -> {var name -> var expr node id}
  val importInstances: MutableMap<Int, BiMap<Int, Map<BibixName, GlobalExprNodeId>>>,
  val preloadedPluginIds: ImmutableBiMap<String, Int>,
  val preloadedPluginInstanceProviders: Map<Int, PluginInstanceProvider>,
  val preludeNames: Set<String>,
  val buildEnv: BuildEnv,
  val fileSystem: FileSystem,
  val repo: BibixRepo,
  val classPkgRunner: ClassPkgRunner,
  val fileHashStore: FileHashStore,
) {
  companion object {
    fun create(
      mainProjectLocation: BibixProjectLocation,
      preludePlugin: PreloadedPlugin,
      preloadedPlugins: Map<String, PreloadedPlugin>,
      buildEnv: BuildEnv,
      fileSystem: FileSystem,
      repo: BibixRepo,
      classPkgRunner: ClassPkgRunner,
    ): BuildGraphRunner {
      val preludeNames = NameLookupTable.fromDefs(preludePlugin.defs).names.keys

      val mainScriptSource = mainProjectLocation.readScript()
      val mainScript = BibixParser.parse(mainScriptSource)
      val mainGraph = BuildGraph.fromScript(mainScript, preloadedPlugins.keys, preludeNames)

      fun nativePluginFor(plugin: PreloadedPlugin): BuildGraph = BuildGraph.fromDefs(
        plugin.packageName,
        plugin.defs,
        preloadedPlugins.keys,
        preludeNames,
        true
      )

      val preloadedPluginIds = mutableMapOf<String, Int>()
      var preloadedPluginIdCounter = 3
      val preloadedPluginInfos = mutableMapOf<Int, MultiBuildGraph.ProjectInfo>()
      val preloadedPluginInstanceProviders = mutableMapOf<Int, PluginInstanceProvider>()

      preloadedPluginInstanceProviders[2] = preludePlugin.pluginInstanceProvider
      preloadedPlugins.forEach { (name, plugin) ->
        preloadedPluginIds[name] = preloadedPluginIdCounter
        preloadedPluginInfos[preloadedPluginIdCounter] = MultiBuildGraph.ProjectInfo(
          null,
          plugin.script,
          nativePluginFor(plugin)
        )
        preloadedPluginInstanceProviders[preloadedPluginIdCounter] = plugin.pluginInstanceProvider

        preloadedPluginIdCounter += 1
      }

      val multiGraph = MultiBuildGraph(
        mapOf(
          1 to MultiBuildGraph.ProjectInfo(mainProjectLocation, mainScriptSource, mainGraph),
          2 to MultiBuildGraph.ProjectInfo(
            null,
            preludePlugin.script,
            nativePluginFor(preludePlugin)
          ),
        ) + preloadedPluginInfos
      )

      return BuildGraphRunner(
        multiGraph = multiGraph,
        importInstances = HashBiMap.create(),
        preloadedPluginIds = ImmutableBiMap.copyOf(preloadedPluginIds),
        preloadedPluginInstanceProviders = preloadedPluginInstanceProviders,
        preludeNames = preludeNames,
        buildEnv = buildEnv,
        fileSystem = fileSystem,
        repo = repo,
        classPkgRunner = classPkgRunner,
        fileHashStore = FileHashStore()
      )
    }
  }

  fun runBuildTask(buildTask: BuildTask): BuildTaskResult = when (buildTask) {
    is EvalTarget -> {
      val exprNodeId =
        multiGraph.getProjectGraph(buildTask.projectId).targets.getValue(buildTask.name)
      BuildTaskResult.WithResult(
        EvalExpr(buildTask.projectId, exprNodeId, buildTask.importInstanceId, null)
      ) { result ->
        // TODO if buildTask.projectId == 1이면 outputs 폴더에 링크 만들기
        if (buildTask.projectId == 1) {
          if (result is BuildTaskResult.ValueOfTargetResult) {
            repo.linkNameToObjectIfExists(buildTask.name, result.targetId)
          }
        }
        when (result) {
          is BuildTaskResult.ResultWithValue -> result
          is BuildTaskResult.TypeCastFailResult -> throw IllegalStateException()
          else -> throw AssertionError()
        }
      }
    }

    is EvalAction -> {
      val action = multiGraph.getProjectGraph(buildTask.projectId).actions.getValue(buildTask.name)
      BuildTaskResult.ActionResult(
        buildTask.projectId,
        buildTask.importInstanceId,
        buildTask.name,
        action
      )
    }

    is ExecAction -> executeAction(buildTask)
    is ExecActionCallExpr -> executeActionCallExpr(buildTask)

    is EvalVar -> {
      val buildGraph = multiGraph.getProjectGraph(buildTask.projectId)
      val varDef = buildGraph.vars[buildTask.name]
      checkNotNull(varDef)

      val varRedefs =
        importInstances[buildTask.projectId]?.get(buildTask.importInstanceId) ?: mapOf()
      val varExpr = varRedefs[buildTask.name] ?: varDef.defaultValue?.let {
        GlobalExprNodeId(buildTask.projectId, buildTask.importInstanceId, it)
      }
      checkNotNull(varExpr)
      BuildTaskResult.WithResult(
        EvalExpr(varExpr.projectId, varExpr.exprNodeId, varExpr.importInstanceId, null)
      ) { result ->
        check(result is BuildTaskResult.ResultWithValue)
        result
      }
    }

    is EvalExpr -> {
      val evaluator = ExprEvaluator(
        buildGraphRunner = this,
        projectId = buildTask.projectId,
        importInstanceId = buildTask.importInstanceId,
        localLets = buildTask.localVars,
        thisValue = buildTask.thisValue,
      )
      evaluator.evaluateExpr(buildTask.exprNodeId)
    }

    is EvalCallExpr -> {
      val evaluator = ExprEvaluator(
        buildGraphRunner = this,
        projectId = buildTask.projectId,
        importInstanceId = buildTask.importInstanceId,
        localLets = mapOf(),
        thisValue = null,
      )
      evaluator.evaluateCallExpr(buildTask)
    }

    is EvalCallee -> {
      // -> BuildRuleResult
      // -> ActionRuleResult
      // -> DataClassResult
      // -> ActionResult

      fun processEntity(
        projectId: Int,
        importInstanceId: Int,
        memberName: BibixName,
        entity: BuildGraphEntity?
      ): BuildTaskResult = when (entity) {
        is BuildGraphEntity.BuildRule -> BuildTaskResult.WithResult(
          EvalBuildRule(projectId, importInstanceId, memberName)
        ) { it }

        is BuildGraphEntity.DataClass -> BuildTaskResult.WithResult(
          EvalDataClass(projectId, importInstanceId, memberName)
        ) { it }

        is BuildGraphEntity.ActionRule -> BuildTaskResult.WithResult(
          EvalActionRule(projectId, importInstanceId, memberName)
        ) { it }

        is BuildGraphEntity.Action -> BuildTaskResult.WithResult(
          EvalAction(projectId, importInstanceId, memberName)
        ) { it }

        else -> throw IllegalStateException()
      }

      when (val callee = buildTask.callee) {
        is Callee.ImportedCallee -> BuildTaskResult.WithResult(
          Import(buildTask.projectId, buildTask.importInstanceId, callee.importName)
        ) { result ->
          check(result is BuildTaskResult.BuildRuleResult || result is BuildTaskResult.DataClassResult || result is BuildTaskResult.ActionRuleResult)
          result
        }

        is Callee.ImportedMemberCallee -> {
          BuildTaskResult.WithResult(
            Import(buildTask.projectId, buildTask.importInstanceId, callee.importName)
          ) { importResult ->
            check(importResult is BuildTaskResult.ImportInstanceResult)

            val importedGraph = multiGraph.getProjectGraph(importResult.projectId)
            val memberName = BibixName(callee.memberNames)
            processEntity(
              importResult.projectId,
              importResult.importInstanceId,
              memberName,
              importedGraph.findName(memberName)
            )
          }
        }

        is Callee.LocalBuildRule -> BuildTaskResult.WithResult(
          EvalBuildRule(buildTask.projectId, buildTask.importInstanceId, callee.name)
        ) { it }

        is Callee.LocalDataClass -> BuildTaskResult.WithResult(
          EvalDataClass(buildTask.projectId, buildTask.importInstanceId, callee.name)
        ) { it }

        is Callee.LocalActionRule -> BuildTaskResult.WithResult(
          EvalActionRule(buildTask.projectId, buildTask.importInstanceId, callee.name)
        ) { it }

        is Callee.LocalAction -> BuildTaskResult.WithResult(
          EvalAction(buildTask.projectId, buildTask.importInstanceId, callee.name)
        ) { it }

        is Callee.PreludeMember -> {
          val preludeGraph = multiGraph.getProjectGraph(2)
          val localEntity = preludeGraph.findName(callee.name)
          if (localEntity != null) {
            processEntity(2, 0, callee.name, localEntity)
          } else {
            // prelude에 import bibix 가 있어서..
            lookupFromImport(preludeGraph, 2, 0, callee.name) { it }
          }
        }
      }
    }

    is TypeCastValue -> {
      ValueCaster(this, buildTask.valueProjectId)
        .castValue(buildTask.value, buildTask.type)
    }

    is FinalizeBuildRuleReturnValue -> {
      ValueCaster(this, buildTask.projectId)
        .finalizeBuildRuleReturnValue(
          buildTask.buildRuleDefCtx,
          buildTask.value
        )
    }

    is EvalBuildRuleMeta -> {
      val buildGraph = multiGraph.getProjectGraph(buildTask.projectId)

      val buildRule = buildGraph.buildRules.getValue(buildTask.name)

      withParamTypes(buildTask.projectId, buildRule.def.params, buildRule.params) { paramTypes ->
        BuildTaskResult.BuildRuleMetaResult(
          buildTask.projectId,
          buildTask.name,
          buildTask.importInstanceId,
          buildRule,
          paramTypes,
        )
      }
    }

    is EvalBuildRule -> {
      val buildGraph = multiGraph.getProjectGraph(buildTask.projectId)

      val buildRule = buildGraph.buildRules.getValue(buildTask.name)

      withParamTypes(buildTask.projectId, buildRule.def.params, buildRule.params) { paramTypes ->
        withImplTarget(
          buildTask.projectId,
          buildTask.importInstanceId,
          buildRule.implTarget,
          buildRule.implClassName,
        ) { impl ->
          BuildTaskResult.BuildRuleResult(
            buildTask.projectId,
            buildTask.name,
            buildTask.importInstanceId,
            buildRule,
            paramTypes,
            impl,
            buildRule.implMethodName
          )
        }
      }
    }

    is EvalActionRule -> {
      val buildGraph = multiGraph.getProjectGraph(buildTask.projectId)

      val actionRule = buildGraph.actionRules.getValue(buildTask.name)

      withParamTypes(buildTask.projectId, actionRule.def.params, actionRule.params) { paramTypes ->
        withImplTarget(
          buildTask.projectId,
          buildTask.importInstanceId,
          actionRule.implTarget,
          actionRule.implClassName
        ) { impl ->
          BuildTaskResult.ActionRuleResult(
            buildTask.projectId,
            buildTask.name,
            buildTask.importInstanceId,
            actionRule,
            paramTypes,
            impl,
            actionRule.implMethodName
          )
        }
      }
    }

    is EvalImportSource -> handleEvalImportSource(buildTask)

    is Import -> handleImportTask(buildTask)

    is ImportFromPrelude -> {
      val projectId = 2
      val name = buildTask.name

      val buildGraph = multiGraph.getProjectGraph(projectId)

      val target = buildGraph.targets[name]
      val buildRule = buildGraph.buildRules[name]
      val varDef = buildGraph.vars[name]
      val dataClass = buildGraph.dataClasses[name]
      val superClass = buildGraph.superClasses[name]
      val enum = buildGraph.enums[name]
      val action = buildGraph.actions[name]
      val actionRule = buildGraph.actionRules[name]

      // 중복된 이름이 있으면 안됨
      check(
        listOf(
          target != null,
          buildRule != null,
          varDef != null,
          dataClass != null,
          superClass != null,
          enum != null,
          action != null,
          actionRule != null
        ).count { it } <= 1
      ) { "Duplicate name: $name at $projectId" }

      when {
        target != null ->
          BuildTaskResult.WithResult(EvalTarget(projectId, 0, name)) { it }

        buildRule != null ->
          BuildTaskResult.WithResult(EvalBuildRule(projectId, 0, name)) { it }

        varDef != null ->
          BuildTaskResult.WithResult(EvalVar(projectId, 0, name)) { it }

        dataClass != null ->
          BuildTaskResult.WithResult(EvalDataClass(projectId, 0, name)) { it }

        superClass != null ->
          BuildTaskResult.WithResult(EvalSuperClass(projectId, name.toString())) { it }

        enum != null -> {
          BuildTaskResult.EnumTypeResult(
            projectId,
            checkNotNull(buildGraph.packageName),
            name,
            enum.def.values
          )
        }

        action != null ->
          BuildTaskResult.WithResult(EvalAction(projectId, 0, name)) { it }

        actionRule != null ->
          BuildTaskResult.WithResult(EvalActionRule(projectId, 0, name)) { it }

        else ->
          lookupFromImport(buildGraph, projectId, 0, name) { it }
      }
    }

    is NewImportInstance -> {
      val instances = importInstances.getOrPut(buildTask.projectId) { HashBiMap.create() }
      val existing = instances.inverse()[buildTask.redefs]
      if (existing != null) {
        BuildTaskResult.ImportInstanceResult(buildTask.projectId, existing)
      } else {
        val newImportInstanceId = (instances.keys.maxOrNull() ?: 0) + 1
        instances[newImportInstanceId] = buildTask.redefs
        BuildTaskResult.ImportInstanceResult(buildTask.projectId, newImportInstanceId)
      }
    }

    is EvalDataClass -> {
      val buildGraph = multiGraph.getProjectGraph(buildTask.projectId)
      val dataClassDef = checkNotNull(buildGraph.dataClasses[buildTask.className])
      val fields = dataClassDef.fields.entries.sortedBy { it.key }
      val fieldTasks = fields.map { EvalType(buildTask.projectId, it.value) }
      val customCastTypeTasks =
        dataClassDef.customAutoCasts.map { EvalType(buildTask.projectId, it.first) }

      BuildTaskResult.WithResultList(fieldTasks + customCastTypeTasks) { typeResults ->
        check(fields.size + customCastTypeTasks.size == typeResults.size)

        val fieldTypes = typeResults.take(fields.size).map {
          check(it is BuildTaskResult.TypeResult)
          it.type
        }
        val customCastTypes = typeResults.drop(fields.size).map {
          check(it is BuildTaskResult.TypeResult)
          it.type
        }
        val fieldTypeMap = fields.zip(fieldTypes).associate { (field, type) -> field.key to type }
        BuildTaskResult.DataClassResult(
          projectId = buildTask.projectId,
          packageName = checkNotNull(buildGraph.packageName),
          name = buildTask.className,
          importInstanceId = buildTask.importInstanceId,
          dataClassDef = dataClassDef,
          fieldTypes = dataClassDef.def.fields.map { field ->
            Pair(field.name, fieldTypeMap.getValue(field.name))
          },
          customCasts = customCastTypes.zip(dataClassDef.customAutoCasts.map { it.second })
        )
      }
    }

    is EvalDataClassByName -> {
      val projectId = multiGraph.getProjectIdByPackageName(buildTask.packageName)
        ?: throw IllegalStateException()
      BuildTaskResult.WithResult(EvalDataClass(projectId, 0, BibixName(buildTask.className))) { it }
    }

    is EvalSuperClass -> {
      val className = BibixName(buildTask.className)
      val buildGraph = multiGraph.getProjectGraph(buildTask.projectId)
      val cls = buildGraph.superClasses[className] ?: throw IllegalStateException()

      fun collectSubTypes(name: BibixName): BuildTaskResult.SuperClassHierarchyResult.SubType {
        val dataClass = buildGraph.dataClasses[name]
        val superClass = buildGraph.superClasses[name]

        return when {
          dataClass != null -> {
            check(superClass == null)
            BuildTaskResult.SuperClassHierarchyResult.SubType(name, listOf())
          }

          superClass != null -> {
            val subs = superClass.subTypes.map { collectSubTypes(BibixName(it)) }
            BuildTaskResult.SuperClassHierarchyResult.SubType(name, subs)
          }

          else -> throw IllegalStateException()
        }
      }

      val subs = cls.subTypes.map { collectSubTypes(BibixName(it)) }

      BuildTaskResult.SuperClassHierarchyResult(
        buildTask.projectId,
        checkNotNull(buildGraph.packageName),
        className,
        subs
      )
    }

    is EvalSuperClassByName -> {
      val projectId = multiGraph.getProjectIdByPackageName(buildTask.packageName)
        ?: throw IllegalStateException()

      BuildTaskResult.WithResult(EvalSuperClass(projectId, buildTask.className)) { it }
    }

    is EvalTypeByName -> {
      val projectId = multiGraph.getProjectIdByPackageName(buildTask.packageName)
        ?: throw IllegalStateException()
      val buildGraph = multiGraph.getProjectGraph(projectId)

      val className = BibixName(buildTask.className)
      val dataClass = buildGraph.dataClasses[className]
      val superClass = buildGraph.superClasses[className]
      val enum = buildGraph.enums[className]

      if (dataClass != null) {
        check(superClass == null && enum == null)
        BuildTaskResult.WithResult(EvalDataClass(projectId, 0, className)) { it }
      } else if (superClass != null) {
        check(enum == null)
        BuildTaskResult.WithResult(
          EvalSuperClassByName(buildTask.packageName, buildTask.className)
        ) { it }
      } else {
        check(enum != null)
        BuildTaskResult.EnumTypeResult(projectId, buildTask.packageName, className, enum.def.values)
      }
    }

    is EvalType -> {
      val buildGraph = multiGraph.getProjectGraph(buildTask.projectId)
      val packageName = multiGraph.projectPackages[buildTask.projectId]
      TypeEvaluator(
        multiGraph,
        buildTask.projectId,
        // buildTask.importInstanceId,
        0,
        packageName,
        buildGraph.typeGraph
      ).evaluateType(buildTask.typeNodeId)
    }
  }

  private fun executeAction(buildTask: ExecAction): BuildTaskResult {
    val projectId = buildTask.projectId
    val importInstanceId = buildTask.importInstanceId
    return BuildTaskResult.WithResult(
      EvalAction(projectId, importInstanceId, buildTask.actionName)
    ) { action ->
      check(action is BuildTaskResult.ActionResult)

      val stmts = action.actionDef.stmts
      fun runNextStmt(stmtIdx: Int, letLocals: Map<String, BibixValue>): BuildTaskResult =
        when (val nextStmt = stmts[stmtIdx]) {
          is ActionDef.LetStmt -> {
            BuildTaskResult.WithResult(
              EvalExpr(
                projectId,
                nextStmt.exprNodeId,
                importInstanceId,
                letLocals,
                null
              )
            ) { evalResult ->
              check(evalResult is BuildTaskResult.ResultWithValue)
              if (stmtIdx + 1 == stmts.size) {
                evalResult
              } else {
                runNextStmt(stmtIdx + 1, letLocals + (nextStmt.name to evalResult.value))
              }
            }
          }

          is ActionDef.CallStmt ->
            BuildTaskResult.WithResult(
              ExecActionCallExpr(projectId, importInstanceId, nextStmt, letLocals)
            ) { execResult ->
              check(execResult is BuildTaskResult.ActionRuleDoneResult)
              if (stmtIdx + 1 == stmts.size) {
                execResult
              } else {
                runNextStmt(stmtIdx + 1, letLocals)
              }
            }
        }

      if (stmts.isEmpty()) BuildTaskResult.ActionRuleDoneResult(null) else runNextStmt(0, mapOf())
    }
  }

  private fun executeActionCallExpr(buildTask: ExecActionCallExpr): BuildTaskResult {
    fun evalTask(exprNodeId: ExprNodeId) = EvalExpr(
      buildTask.projectId,
      exprNodeId,
      buildTask.importInstanceId,
      buildTask.letLocals,
      null
    )

    val callStmt = buildTask.callStmt

    val namedParams = callStmt.namedArgs.entries.toList()

    return BuildTaskResult.WithResultList(
      listOf(EvalCallee(buildTask.projectId, buildTask.importInstanceId, callStmt.callee)) +
        callStmt.posArgs.map(::evalTask) +
        namedParams.map { evalTask(it.value) }) { results ->
      check(results.size == 1 + callStmt.posArgs.size + namedParams.size)
      when (val callee = results.first()) {
        is BuildTaskResult.ActionResult -> {
          // 다른 action 호출
          // TODO callStmt에 string list 파라메터 하나는 들어갈 수 있도록(args)
          check(callStmt.posArgs.isEmpty() && namedParams.isEmpty())
          BuildTaskResult.WithResult(
            ExecAction(callee.projectId, callee.importInstanceId, callee.actionName, 0)
          ) { it }
        }

        is BuildTaskResult.ActionRuleResult -> {
          val posArgs = results.drop(1).take(callStmt.posArgs.size).map {
            check(it is BuildTaskResult.ResultWithValue)
            it.value
          }
          val namedArgs = namedParams.zip(results.drop(1 + callStmt.posArgs.size))
            .associate { (param, result) ->
              check(result is BuildTaskResult.ResultWithValue)
              param.key to result.value
            }

          organizeParams(
            buildTask.projectId,
            callee.paramTypes,
            callee.actionRuleDef.def.params.requiredParamNames(),
            callee.projectId,
            callee.importInstanceId,
            callee.actionRuleDef.paramDefaultValues,
            posArgs,
            namedArgs,
          ) { args ->
            val progressLogger =
              repo.progressLoggerForAction(callee.projectId, callee.importInstanceId, callee.name)

            val actionContext = ActionContext(buildEnv, args, progressLogger)

            val runner = BuildRuleRunner(
              repo,
              buildTask.projectId,
              buildTask.importInstanceId,
              BuildRuleDefContext.from(callee)
            ) { it }

            val implInstance = getImplInstance(callee.impl, buildTask.projectId, classPkgRunner)

            val implMethod =
              implInstance::class.java.getMethod(callee.implMethodName, ActionContext::class.java)
            implMethod.trySetAccessible()

            val result = implMethod.invoke(implInstance, actionContext)
            runner.handleActionReturn(result)
          }
        }

        else -> throw IllegalStateException()
      }
    }
  }

  private fun withParamTypes(
    projectId: Int,
    paramDefs: List<BibixAst.ParamDef>,
    paramTypeNodeIds: Map<String, TypeNodeId>,
    block: (List<Pair<String, BibixType>>) -> BuildTaskResult
  ): BuildTaskResult {
    val params = paramDefs.map { param ->
      param.name to paramTypeNodeIds.getValue(param.name)
    }
    return BuildTaskResult.WithResultList(params.map {
      EvalType(projectId, it.second)
    }) { results ->
      check(results.all { it is BuildTaskResult.TypeResult })
      check(params.size == results.size)
      val types = results.map { (it as BuildTaskResult.TypeResult).type }
      val paramTypes = params.map { it.first }.zip(types)

      block(paramTypes)
    }
  }

  private fun withImplTarget(
    projectId: Int,
    importInstanceId: Int,
    implTarget: ExprNodeId?,
    implClassName: String,
    block: (impl: BuildTaskResult.BuildRuleImpl) -> BuildTaskResult
  ): BuildTaskResult = if (implTarget == null) {
    val pluginInstanceProvider =
      preloadedPluginInstanceProviders.getValue(projectId)
    val implInstance = pluginInstanceProvider.getInstance(implClassName)
    block(BuildTaskResult.BuildRuleImpl.NativeImpl(implInstance))
  } else {
    BuildTaskResult.WithResult(
      EvalExpr(projectId, implTarget, importInstanceId, null)
    ) { implTargetResult ->
      check(implTargetResult is BuildTaskResult.ResultWithValue)

      // implTarget을 ClassPkg로 변환
      BuildTaskResult.WithResult(
        TypeCastValue(
          implTargetResult.value,
          DataClassType("com.giyeok.bibix.plugins.jvm", "ClassPkg"),
          projectId,
        )
      ) { implTarget ->
        check(implTarget is BuildTaskResult.ResultWithValue)

        val classPkg = ClassPkg.fromBibix(implTarget.value)

        block(BuildTaskResult.BuildRuleImpl.NonNativeImpl(classPkg, implClassName))
      }
    }
  }
}

fun List<BibixAst.ParamDef>.requiredParamNames() =
  this.filter { param -> !param.optional && param.defaultValue == null }
    .map { it.name }.toSet()

fun getImplInstance(
  impl: BuildTaskResult.BuildRuleImpl,
  callerProjectId: Int,
  classPkgRunner: ClassPkgRunner,
): Any = when (impl) {
  is BuildTaskResult.BuildRuleImpl.NativeImpl -> impl.implInstance
  is BuildTaskResult.BuildRuleImpl.NonNativeImpl -> {
    classPkgRunner.getPluginImplInstance(
      callerProjectId = callerProjectId,
      classPkg = impl.classPkg,
      className = impl.implClassName
    )
  }
}
