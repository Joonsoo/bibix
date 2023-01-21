package com.giyeok.bibix.interpreter

import com.giyeok.bibix.ast.BibixAst
import com.giyeok.bibix.base.*
import com.giyeok.bibix.interpreter.expr.NameLookupContext
import com.giyeok.bibix.interpreter.expr.NameLookupTable
import com.giyeok.bibix.plugins.Classes
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
  private lateinit var preludePluginClasses: Classes
  private lateinit var _mainBaseDirectory: Path
  val mainBaseDirectory get() = _mainBaseDirectory
  private val preloadedPluginClasses = mutableMapOf<PreloadedSourceId, Classes>()

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
  ) {
    projectRoots[sourceId] = project.projectRoot
    // script가 sourceId라는 이름으로 등록됨
    // remember that sourceId has the package name
    // TODO Check the package name is unique
    if (buildScript.packageName().isDefined) {
      val packageName = buildScript.packageName().get().tokens().toKtList().joinToString(".")
      sourcePackageName[sourceId] = packageName
    }
    nameLookupTable.add(NameLookupContext(sourceId, listOf()), buildScript.defs().toKtList())
  }

  suspend fun loadPrelude(prelude: PreloadedPlugin, nameLookupTable: NameLookupTable) {
    mutex.withLock {
      nameLookupTable.add(NameLookupContext(PreludeSourceId, listOf()), prelude.defs)
      sourcePackageName[PreludeSourceId] = prelude.packageName
      preludePluginClasses = prelude.classes
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
      registerScript(sourceId, project, buildScript, nameLookupTable)
      sourceId
    }
  }

  suspend fun getProjectRoot(sourceId: SourceId): Path =
    mutex.withLock { projectRoots.getValue(sourceId) }

  fun registerPreloadedPluginClasses(name: String, plugin: PreloadedPlugin) {
    val sourceId = PreloadedSourceId(name)
    preloadedPluginClasses[sourceId] = plugin.classes
    try {
      sourcePackageName[sourceId] = plugin.packageName
    } catch (e: IllegalArgumentException) {
      throw IllegalStateException("Package name conflict: ${plugin.packageName}")
    }
  }

  fun getPreludePluginClass(className: String): Class<*> = preludePluginClasses.getClass(className)

  fun getPreloadedPluginClass(sourceId: PreloadedSourceId, className: String): Class<*> =
    preloadedPluginClasses.getValue(sourceId).getClass(className)

  suspend fun getPackageName(sourceId: SourceId): String? =
    mutex.withLock { sourcePackageName[sourceId] }

  suspend fun getSourceIdFromPackageName(packageName: String): SourceId? =
    mutex.withLock { sourcePackageName.inverse()[packageName] }
}