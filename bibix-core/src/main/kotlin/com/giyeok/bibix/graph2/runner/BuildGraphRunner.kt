package com.giyeok.bibix.graph2.runner

import com.giyeok.bibix.ast.BibixParser
import com.giyeok.bibix.base.BuildEnv
import com.giyeok.bibix.graph.NameLookupTable
import com.giyeok.bibix.graph.TaskGraph
import com.giyeok.bibix.graph2.BibixProjectLocation
import com.giyeok.bibix.graph2.BuildGraph
import com.giyeok.bibix.plugins.PluginInstanceProvider
import com.giyeok.bibix.plugins.PreloadedPlugin
import com.giyeok.bibix.repo.BibixRepo
import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import org.codehaus.plexus.classworlds.ClassWorld

class BuildGraphRunner(
  val multiGraph: MultiBuildGraph,
  val preloadedPluginIds: BiMap<String, Int>,
  val preloadedPluginInstanceProviders: Map<Int, PluginInstanceProvider>,
  val preludeNames: Set<String>,
  // TODO importInstances
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
        preloadedPluginIds = HashBiMap.create(preloadedPluginIds),
        preloadedPluginInstanceProviders = preloadedPluginInstanceProviders,
        preludeNames = preludeNames,
        buildEnv = buildEnv,
        repo = repo,
        classPkgRunner = ClassPkgRunner(classWorld)
      )
    }
  }
}
