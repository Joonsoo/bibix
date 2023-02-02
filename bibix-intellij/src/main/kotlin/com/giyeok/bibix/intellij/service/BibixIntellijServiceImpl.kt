package com.giyeok.bibix.intellij.service

import com.giyeok.bibix.base.MainSourceId
import com.giyeok.bibix.frontend.BuildFrontend
import com.giyeok.bibix.frontend.NoopProgressNotifier
import com.giyeok.bibix.intellij.BibixIntellijProto.BibixProjectInfo
import com.giyeok.bibix.intellij.BibixIntellijProto.LoadProjectReq
import com.giyeok.bibix.intellij.BibixIntellijServiceGrpcKt.BibixIntellijServiceCoroutineImplBase
import com.giyeok.bibix.intellij.bibixProjectInfo
import com.giyeok.bibix.intellij.module
import com.giyeok.bibix.interpreter.BibixProject
import com.giyeok.bibix.interpreter.expr.Definition
import com.google.common.collect.HashBiMap
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Path
import kotlin.io.path.absolute
import kotlin.io.path.name

class BibixIntellijServiceImpl(
  private val fileSystem: FileSystem = FileSystems.getDefault(),
) : BibixIntellijServiceCoroutineImplBase() {
  private fun loadProject(projectRoot: Path, scriptName: String?): BibixProjectInfo {
    val javaModulesCollector = ModulesCollector("java")
    val ktjvmModulesCollector = ModulesCollector("ktjvm")
    val scalaModulesCollector = ModulesCollector("scala")
    val overridingPluginImplProvider = OverridingPluginImplProviderImpl(
      mapOf(
        Pair(MainSourceId, "com.giyeok.bibix.plugins.java.Library") to javaModulesCollector,
        Pair(MainSourceId, "com.giyeok.bibix.plugins.ktjvm.Library") to ktjvmModulesCollector,
        Pair(MainSourceId, "com.giyeok.bibix.plugins.scala.Library") to scalaModulesCollector
      )
    )
    val buildFrontend = BuildFrontend(
      BibixProject(projectRoot, scriptName),
      mapOf(),
      listOf(),
      NoopProgressNotifier(),
      pluginImplProvider = overridingPluginImplProvider
    )

    val mainTargets = buildFrontend.mainScriptDefinitions()
      .filterValues { it is Definition.TargetDef }
    buildFrontend.blockingBuildTargets(mainTargets.keys.toList())

    val modules =
      javaModulesCollector.modules + ktjvmModulesCollector.modules + scalaModulesCollector.modules
    val namesMap = buildFrontend.repo.repoMeta.objectNamesMap

    return bibixProjectInfo {
      this.projectId = "--"
      this.projectName = projectRoot.name
      this.modules.addAll(modules.map { bibixModule ->
        module {
          this.moduleName = namesMap[bibixModule.key] ?: bibixModule.key
        }
      })
    }
  }

  override suspend fun loadProject(request: LoadProjectReq): BibixProjectInfo {
    val projectRoot = fileSystem.getPath(request.projectRoot).normalize().absolute()

    return loadProject(projectRoot, request.scriptName.ifEmpty { null })
  }
}
