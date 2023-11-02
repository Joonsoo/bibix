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
import org.codehaus.plexus.classworlds.ClassWorld
import java.nio.file.FileSystem
import kotlin.io.path.absolute

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
) {
  companion object {
    suspend fun create(
      mainProjectLocation: BibixProjectLocation,
      preludePlugin: PreloadedPlugin,
      preloadedPlugins: Map<String, PreloadedPlugin>,
      buildEnv: BuildEnv,
      fileSystem: FileSystem,
      repo: BibixRepo,
      classWorld: ClassWorld,
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
        classPkgRunner = ClassPkgRunner(classWorld)
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
        when (result) {
          is BuildTaskResult.ValueResult -> result
          is BuildTaskResult.TypeCastFailResult -> throw IllegalStateException()
          else -> throw AssertionError()
        }
      }
    }

    is ExecActionCallExpr -> {
      fun evalTask(exprNodeId: ExprNodeId) = EvalExpr(
        buildTask.projectId,
        exprNodeId,
        buildTask.importInstanceId,
        buildTask.letLocals,
        null
      )

      val callStmt = buildTask.callStmt

      val namedParams = callStmt.namedArgs.entries.toList()

      BuildTaskResult.WithResultList(
        listOf(evalTask(callStmt.calleeNodeId)) +
          callStmt.posArgs.map(::evalTask) +
          namedParams.map { evalTask(it.value) }) { results ->
        check(results.size == 1 + callStmt.posArgs.size + namedParams.size)
        val actionRule = results.first()
        check(actionRule is BuildTaskResult.ActionRuleResult)
        val posArgs = results.drop(1).take(callStmt.posArgs.size).map {
          check(it is BuildTaskResult.ValueResult)
          it.value
        }
        val namedArgs =
          namedParams.zip(results.drop(1 + callStmt.posArgs.size)).associate { (param, result) ->
            check(result is BuildTaskResult.ValueResult)
            param.key to result.value
          }

        val requiredParams = actionRule.actionRuleDef.def.params
          .filter { param -> !param.optional && param.defaultValue == null }
          .map { it.name }.toSet()
        organizeParams(
          actionRule.paramTypes,
          requiredParams,
          actionRule.projectId,
          actionRule.importInstanceId,
          actionRule.actionRuleDef.paramDefaultValues,
          posArgs,
          namedArgs,
        ) { args ->
          TODO()
        }
      }
    }

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
        check(result is BuildTaskResult.ValueResult)
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

    is TypeCastValue -> {
      ValueCaster(this, buildTask.projectId, buildTask.importInstanceId)
        .castValue(buildTask.value, buildTask.type)
    }

    is FinalizeBuildRuleReturnValue -> {
      ValueCaster(this, buildTask.projectId, buildTask.importInstanceId)
        .finalizeBuildRuleReturnValue(buildTask.finalizeCtx, buildTask.value)
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
        ) { implInstance ->
          val method = implInstance::class.java.getDeclaredMethod(
            buildRule.implMethodName,
            BuildContext::class.java
          )
          check(method.trySetAccessible())
          BuildTaskResult.BuildRuleResult(
            buildTask.projectId,
            buildTask.name,
            buildTask.importInstanceId,
            buildRule,
            paramTypes,
            implInstance,
            method
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
        ) { implInstance ->
          val method = implInstance::class.java.getDeclaredMethod(
            actionRule.implMethodName,
            ActionContext::class.java
          )
          BuildTaskResult.ActionRuleResult(
            buildTask.projectId,
            buildTask.name,
            buildTask.importInstanceId,
            actionRule,
            paramTypes,
            implInstance,
            method
          )
        }
      }
    }

    is Import -> handleImportTask(buildTask)

    is ImportFromPrelude -> {
      lookupExprValue(2, BibixName(buildTask.name), 0) { it }
    }

    is ImportPreloaded -> {
      val projectId = preloadedPluginIds.getValue(buildTask.pluginName)
      val graph = multiGraph.getProjectGraph(projectId)
      BuildTaskResult.ImportResult(projectId, graph, listOf())
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
      BuildTaskResult.WithResultList(fieldTasks) { fieldTypeResults ->
        check(fields.size == fieldTypeResults.size)
        val fieldTypes = fieldTypeResults.map {
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
          }
        )
      }
    }

    is EvalDataClassByName -> {
      val projectId = multiGraph.getProjectIdByPackageName(buildTask.packageName)
        ?: throw IllegalStateException()
      BuildTaskResult.WithResult(EvalDataClass(projectId, 0, BibixName(buildTask.className))) { it }
    }

    is EvalSuperClassHierarchyByName -> {
      val projectId = multiGraph.getProjectIdByPackageName(buildTask.packageName)
        ?: throw IllegalStateException()

      val className = BibixName(buildTask.className)
      val buildGraph = multiGraph.getProjectGraph(projectId)
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

      BuildTaskResult.SuperClassHierarchyResult(projectId, buildTask.packageName, className, subs)
    }

    is EvalType -> {
      val buildGraph = multiGraph.getProjectGraph(buildTask.projectId)
      val packageName = multiGraph.projectPackages[buildTask.projectId]
      TypeEvaluator(buildTask.projectId, packageName, buildGraph.typeGraph)
        .evaluateType(buildTask.typeNodeId)
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
    block: (Any) -> BuildTaskResult
  ): BuildTaskResult = if (implTarget == null) {
    val pluginInstanceProvider =
      preloadedPluginInstanceProviders.getValue(projectId)
    val implInstance = pluginInstanceProvider.getInstance(implClassName)
    block(implInstance)
  } else {
    BuildTaskResult.WithResult(
      EvalExpr(projectId, implTarget, importInstanceId, null)
    ) { implTargetResult ->
      check(implTargetResult is BuildTaskResult.ValueResult)

      // implTarget을 ClassPkg로 변환
      BuildTaskResult.WithResult(
        TypeCastValue(
          implTargetResult.value,
          DataClassType("com.giyeok.bibix.plugins.jvm", "ClassPkg"),
          projectId,
          importInstanceId,
        )
      ) { implTarget ->
        check(implTarget is BuildTaskResult.ValueResult)

        val classPkg = ClassPkg.fromBibix(implTarget.value)

        val implInstance = classPkgRunner.getPluginImplInstance(classPkg, implClassName)
        block(implInstance)
      }
    }
  }

  private fun handleImportTask(importTask: Import): BuildTaskResult {
    val buildGraph = multiGraph.getProjectGraph(importTask.projectId)
    val importAll = buildGraph.importAlls[importTask.importName]
    val importFrom = buildGraph.importFroms[importTask.importName]
    check(importAll != null || importFrom != null)

    return if (importAll != null) {
      check(importFrom == null)
      BuildTaskResult.WithResult(
        EvalExpr(importTask.projectId, importAll.source, importTask.importInstanceId, null)
      ) { source ->
        handleImportSource(importTask.projectId, source) { importProjectId, importGraph ->
          BuildTaskResult.ImportResult(importProjectId, importGraph, listOf())
        }
      }
    } else {
      // from bibix.plugins import ktjvm
      // from "../a" import xyz
      // import bibix.plugins as plgs
      check(importFrom != null)
      BuildTaskResult.WithResult(
        EvalExpr(importTask.projectId, importFrom.source, importTask.importInstanceId, null)
      ) { source ->
        handleImportSource(importTask.projectId, source) { importProjectId, importGraph ->
          BuildTaskResult.ImportResult(importProjectId, importGraph, importFrom.importing)
        }
      }
    }
  }

  private fun handleImportSource(
    projectId: Int,
    source: BuildTaskResult,
    block: (importProjectId: Int, importGraph: BuildGraph) -> BuildTaskResult
  ): BuildTaskResult {
    if (source is BuildTaskResult.ImportResult) {
      return source
    }
    check(source is BuildTaskResult.ValueResult)

    suspend fun handleImportLocation(importLocation: BibixProjectLocation): BuildTaskResult {
      val existingProjectId = multiGraph.getProjectIdByLocation(importLocation)
      return if (existingProjectId != null) {
        // 이미 로드된 프로젝트인 경우
        val graph = multiGraph.getProjectGraph(existingProjectId)
        block(existingProjectId, graph)
      } else {
        // 새로 로드해야 하는 프로젝트인 경우
        val importSource = importLocation.readScript()
        val importScript = BibixParser.parse(importSource)
        val importGraph =
          BuildGraph.fromScript(importScript, preloadedPluginIds.keys, preludeNames)

        val importProjectId =
          multiGraph.addProject(importLocation, importGraph, importSource)
        block(importProjectId, importGraph)
      }
    }

    return when (val sourceValue = source.value) {
      is StringValue -> {
        // 상대 경로로 다른 프로젝트 import
        val projectLocation = multiGraph.projectLocations.getValue(projectId)

        val importRoot =
          projectLocation.projectRoot.resolve(sourceValue.value).normalize().absolute()
        val importLocation = BibixProjectLocation(importRoot)

        BuildTaskResult.SuspendLongRunning {
          handleImportLocation(importLocation)
        }
      }

      is ClassInstanceValue -> {
        check(sourceValue.packageName == "com.giyeok.bibix.prelude" && sourceValue.className == "BibixProject")

        // class BibixProject(projectRoot: directory, scriptName?: string)
        val projectRoot = sourceValue.getDirectoryField("projectRoot")
        val scriptName = sourceValue.getNullableStringField("scriptName")

        val importLocation = if (scriptName == null) {
          BibixProjectLocation(projectRoot)
        } else {
          BibixProjectLocation(projectRoot, scriptName)
        }

        BuildTaskResult.SuspendLongRunning {
          handleImportLocation(importLocation)
        }
      }

      else -> TODO()
    }
  }
}
