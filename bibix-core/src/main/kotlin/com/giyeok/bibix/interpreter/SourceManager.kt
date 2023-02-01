package com.giyeok.bibix.interpreter

import com.giyeok.bibix.ast.BibixAst
import com.giyeok.bibix.base.*
import com.giyeok.bibix.interpreter.expr.NameLookupContext
import com.giyeok.bibix.interpreter.expr.NameLookupTable
import com.giyeok.bibix.plugins.PluginInstanceProvider
import com.giyeok.bibix.plugins.PreloadedPlugin
import com.giyeok.bibix.utils.toKtList
import com.google.common.annotations.VisibleForTesting
import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.lang.IllegalArgumentException
import java.nio.file.Path
import kotlin.io.path.readText

class SourceManager {
  private val projectRoots = mutableMapOf<SourceId, Path>()

  @VisibleForTesting
  val sourcePackageName: BiMap<SourceId, String> = HashBiMap.create<SourceId, String>()
  private val externSources = mutableMapOf<BibixProject, ExternSourceId>()
  private var externSourceIdCounter = 0
  private val mutex = Mutex()
  private lateinit var preludePluginPluginInstanceProvider: PluginInstanceProvider
  private lateinit var _mainBaseDirectory: Path
  val mainBaseDirectory get() = _mainBaseDirectory
  private val preloadedPluginPluginInstanceProvider =
    mutableMapOf<PreloadedSourceId, PluginInstanceProvider>()

  private fun parseScript(project: BibixProject): BibixAst.BuildScript {
    val scriptPath = project.projectRoot.resolve(project.scriptName ?: "build.bbx")

    val script = BibixAst.parseAst(scriptPath.readText())
    if (script.isRight) {
      throw IllegalStateException("Failed to parse script: $project (${script.right().get()})")
    }

    return script.left().get()
  }

  private fun registerScript(
    sourceId: SourceId,
    project: BibixProject,
    buildScript: BibixAst.BuildScript,
    nameLookupTable: NameLookupTable
  ): SourceId {
    // script가 sourceId라는 이름으로 등록됨
    // remember that sourceId has the package name
    // It is checked by sourcePackageName whether the package name is unique
    val usingSourceId = if (buildScript.packageName().isDefined) {
      val packageName = buildScript.packageName().get().tokens().toKtList().joinToString(".")
      val conflict = sourcePackageName.inverse()[packageName]
      if (conflict != null && projectRoots[conflict] == project.projectRoot) {
        conflict
      } else {
        sourcePackageName[sourceId] = packageName
        sourceId
      }
    } else {
      sourceId
    }
    projectRoots[usingSourceId] = project.projectRoot
    nameLookupTable.add(NameLookupContext(usingSourceId, listOf()), buildScript.defs().toKtList())
    return usingSourceId
  }

  suspend fun loadPrelude(prelude: PreloadedPlugin, nameLookupTable: NameLookupTable) {
    mutex.withLock {
      nameLookupTable.add(NameLookupContext(PreludeSourceId, listOf()), prelude.defs)
      sourcePackageName[PreludeSourceId] = prelude.packageName
      preludePluginPluginInstanceProvider = prelude.pluginInstanceProvider
    }
  }

  suspend fun loadMainSource(project: BibixProject, nameLookupTable: NameLookupTable) {
    val buildScript = parseScript(project)

    mutex.withLock {
      _mainBaseDirectory = project.projectRoot
      registerScript(MainSourceId, project, buildScript, nameLookupTable)
    }
  }

  suspend fun loadSource(project: BibixProject, nameLookupTable: NameLookupTable): ExternSourceId {
    val existing = externSources[project]
    if (existing != null) {
      return existing
    }

    val buildScript = parseScript(project)

    return mutex.withLock {
      externSourceIdCounter += 1
      val sourceId = ExternSourceId(externSourceIdCounter)
      externSources[project] = sourceId
      registerScript(sourceId, project, buildScript, nameLookupTable) as ExternSourceId
    }
  }

  suspend fun getProjectRoot(sourceId: SourceId): Path =
    mutex.withLock { projectRoots.getValue(sourceId) }

  fun registerPreloadedPluginClasses(name: String, plugin: PreloadedPlugin) {
    val sourceId = PreloadedSourceId(name)
    preloadedPluginPluginInstanceProvider[sourceId] = plugin.pluginInstanceProvider
    try {
      sourcePackageName[sourceId] = plugin.packageName
    } catch (e: IllegalArgumentException) {
      throw IllegalStateException("Package name conflict: ${plugin.packageName}")
    }
  }

  fun getPreludePluginInstance(className: String): Any =
    preludePluginPluginInstanceProvider.getInstance(className)

  fun getPreloadedPluginInstance(sourceId: PreloadedSourceId, className: String): Any =
    preloadedPluginPluginInstanceProvider.getValue(sourceId).getInstance(className)

  suspend fun getPackageName(sourceId: SourceId): String? =
    mutex.withLock { sourcePackageName[sourceId] }

  suspend fun getSourceIdFromPackageName(packageName: String): SourceId? =
    mutex.withLock { sourcePackageName.inverse()[packageName] }

  fun descriptionOf(sourceId: SourceId): String = when (sourceId) {
    is ExternSourceId -> {
      val projectRoot = projectRoots[sourceId]
      "external project at $projectRoot"
    }

    else -> "$sourceId"
  }
}
