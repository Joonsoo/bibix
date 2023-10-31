package com.giyeok.bibix.graph2.runner

import com.giyeok.bibix.ast.BibixParser
import com.giyeok.bibix.base.BuildEnv
import com.giyeok.bibix.base.StringValue
import com.giyeok.bibix.graph.NameLookupTable
import com.giyeok.bibix.graph2.BibixName
import com.giyeok.bibix.graph2.BibixProjectLocation
import com.giyeok.bibix.graph2.BuildGraph
import com.giyeok.bibix.plugins.PluginInstanceProvider
import com.giyeok.bibix.plugins.PreloadedPlugin
import com.giyeok.bibix.repo.BibixRepo
import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import com.google.common.collect.ImmutableBiMap
import org.codehaus.plexus.classworlds.ClassWorld
import kotlin.io.path.absolute

class BuildGraphRunner(
  val multiGraph: MultiBuildGraph,
  // project id -> import instance id -> {var name -> var expr node id}
  val importInstances: MutableMap<Int, BiMap<Int, Map<BibixName, GlobalExprNodeId>>>,
  val preloadedPluginIds: ImmutableBiMap<String, Int>,
  val preloadedPluginInstanceProviders: Map<Int, PluginInstanceProvider>,
  val preludeNames: Set<String>,
  val buildEnv: BuildEnv,
  val repo: BibixRepo,
  val classPkgRunner: ClassPkgRunner,
) {
  companion object {
    suspend fun create(
      mainProjectLocation: BibixProjectLocation,
      preludePlugin: PreloadedPlugin,
      preloadedPlugins: Map<String, PreloadedPlugin>,
      buildEnv: BuildEnv,
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
        when (result) {
          is BuildTaskResult.ValueResult ->
            BuildTaskResult.ValueResult(result.value)

          else -> throw AssertionError()
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
        EvalExpr(varExpr.projectId, varExpr.exprNodeId, varExpr.varCtxId, null)
      ) { result ->
        check(result is BuildTaskResult.ValueResult)
        result
      }
    }

    is EvalExpr -> {
      val buildGraph = multiGraph.getProjectGraph(buildTask.projectId)

      val evaluator = ExprEvaluator(
        projectId = buildTask.projectId,
        projectPackageName = multiGraph.projectPackages[buildTask.projectId],
        varRedefs = buildGraph.varRedefs,
        exprGraph = buildGraph.exprGraph,
        importInstanceId = buildTask.importInstanceId,
        thisValue = buildTask.thisValue
      )
      evaluator.evaluateExpr(buildTask.exprNodeId)
    }

    is Import -> {
      val buildGraph = multiGraph.getProjectGraph(buildTask.projectId)
      val importAll = buildGraph.importAlls[buildTask.importName]
      val importFrom = buildGraph.importFroms[buildTask.importName]
      check(importAll != null || importFrom != null)
      if (importAll != null) {
        check(importFrom == null)
        BuildTaskResult.WithResult(
          EvalExpr(buildTask.projectId, importAll.source, buildTask.varCtxId, null)
        ) { source ->
          check(source is BuildTaskResult.ValueResult)
          when (val sourceValue = source.value) {
            is StringValue -> BuildTaskResult.SuspendLongRunning {
              val projectLocation = multiGraph.projectLocations.getValue(buildTask.projectId)

              val importRoot =
                projectLocation.projectRoot.resolve(sourceValue.value).normalize().absolute()
              val importLocation = BibixProjectLocation(importRoot)

              val existingProjectId = multiGraph.getProjectIdByLocation(importLocation)
              if (existingProjectId != null) {
                val graph = multiGraph.getProjectGraph(existingProjectId)
                BuildTaskResult.ImportResult(existingProjectId, graph)
              } else {

                val importSource = importLocation.readScript()
                val importScript = BibixParser.parse(importSource)
                val importGraph =
                  BuildGraph.fromScript(importScript, preloadedPluginIds.keys, preludeNames)

                val importProjectId =
                  multiGraph.addProject(importLocation, importGraph, importSource)
                BuildTaskResult.ImportResult(importProjectId, importGraph)
              }
            }

            else -> TODO()
          }
        }
      } else {
        check(importFrom != null)
        BuildTaskResult.WithResult(
          EvalExpr(buildTask.projectId, importFrom.source, buildTask.varCtxId, null)
        ) { source ->
          TODO()
        }
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
      val dataClassDef = checkNotNull(buildGraph.dataClasses[buildTask.name])
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
          name = buildTask.name,
          varCtxId = buildTask.varCtxId,
          def = dataClassDef,
          fieldTypes = dataClassDef.def.fields.map { field ->
            Pair(field.name, fieldTypeMap.getValue(field.name))
          }
        )
      }
    }

    is EvalType -> {
      val buildGraph = multiGraph.getProjectGraph(buildTask.projectId)
      TypeEvaluator(buildGraph.typeGraph).evaluateType(buildTask.typeNodeId)
    }

    is RunBuildRule -> TODO()
  }

  private fun getVarCtx(projectId: Int, varCtxId: Int): Map<BibixName, GlobalExprNodeId> {
//    val varCtxOpt = varRedefs[projectId]?.get(varCtxId)
//    check(varCtxId == 0 || varCtxOpt != null)
//    return varCtxOpt ?: mapOf()
    return mapOf()
  }
}
