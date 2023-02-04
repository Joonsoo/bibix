package com.giyeok.bibix.intellij.service

import com.giyeok.bibix.base.MainSourceId
import com.giyeok.bibix.frontend.BuildFrontend
import com.giyeok.bibix.frontend.NoopProgressNotifier
import com.giyeok.bibix.intellij.BibixIntellijProto.*
import com.giyeok.bibix.intellij.BibixIntellijServiceGrpcKt.BibixIntellijServiceCoroutineImplBase
import com.giyeok.bibix.intellij.bibixProjectInfo
import com.giyeok.bibix.intellij.contentRoot
import com.giyeok.bibix.intellij.externalLibrary
import com.giyeok.bibix.intellij.module
import com.giyeok.bibix.interpreter.BibixProject
import com.giyeok.bibix.interpreter.expr.Definition
import com.giyeok.bibix.plugins.jvm.*
import com.google.common.annotations.VisibleForTesting
import kotlinx.coroutines.flow.Flow
import java.lang.AssertionError
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Path
import kotlin.io.path.absolute
import kotlin.io.path.absolutePathString
import kotlin.io.path.name

class BibixIntellijServiceImpl(
  private val fileSystem: FileSystem = FileSystems.getDefault(),
) : BibixIntellijServiceCoroutineImplBase() {
  @VisibleForTesting
  fun getContentRoots(sources: Set<Path>): List<ContentRoot> {
    // TODO 공통 ancestor directory 및 각 파일의 package 이름을 고려해서 content root
    return sources.map { source ->
      contentRoot {
        this.contentRootName = "Sources"
        this.contentRootType = "src"
        this.contentRootPath = source.absolutePathString()
      }
    }
  }

  @VisibleForTesting
  fun loadProject(projectRoot: Path, scriptName: String?): BibixProjectInfo {
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

    val objectNamesMap = mutableMapOf<String, String>()
    namesMap.forEach { (objHash, name) ->
      val existingName = objectNamesMap[name]
      if (existingName == null || existingName.length > name.length) {
        objectNamesMap[name] = objHash
      }
    }

    val pkgGraph = PackageGraph.create(modules.values)

    fun libIdFromOrigin(origin: ClassOrigin): String = when (origin) {
      is LocalBuilt -> throw AssertionError()
      is LocalLib -> "local: ${origin.path.absolutePathString()}"
      is MavenDep -> "maven: ${origin.group}:${origin.artifact}:${origin.version}"
    }

    fun moduleName(objHash: String): String = objectNamesMap[objHash] ?: objHash

    val externalLibraries = pkgGraph.nonModulePkgs.values.map { pkg ->
      externalLibrary {
        this.libraryId = libIdFromOrigin(pkg.origin)
        when (val cpinfo = pkg.cpinfo) {
          is ClassesInfo -> {
            this.classpaths.addAll(cpinfo.classDirs.map { it.absolutePathString() })
            // TODO resources?
            this.classpaths.addAll(cpinfo.resDirs.map { it.absolutePathString() })
            cpinfo.srcs?.let { srcs ->
              this.sources.addAll(srcs.map { it.absolutePathString() })
            }
          }

          is JarInfo -> {
            this.classpaths.add(cpinfo.jar.absolutePathString())
            cpinfo.sourceJar?.let { sourceJar ->
              this.sources.add(sourceJar.absolutePathString())
            }
          }
        }
      }
    }

    val projectModules = pkgGraph.modules.values.map { module ->
      module {
        this.moduleName = moduleName(module.origin.objHash)
        this.moduleType = module.languageType
        this.contentRoots.addAll(getContentRoots(module.sources))
        this.sdkVersion = "21"
        val deps = pkgGraph.dependentNodesOf(module.origin)
        this.moduleDeps.addAll(deps.filterIsInstance<LocalBuilt>().map { moduleName(it.objHash) })
        this.libraryDeps.addAll(deps.filter { it !is LocalBuilt }.map { libIdFromOrigin(it) })
      }
    }

    return bibixProjectInfo {
      this.projectId = "--"
      this.projectName = projectRoot.name
      this.modules.addAll(projectModules)
      this.externalLibraries.addAll(externalLibraries)
    }
  }

  override suspend fun loadProject(request: LoadProjectReq): BibixProjectInfo {
    val projectRoot = fileSystem.getPath(request.projectRoot).normalize().absolute()

    return loadProject(projectRoot, request.scriptName.ifEmpty { null })
  }

  override suspend fun buildTargets(request: BuildTargetsReq): BuildTargetsRes {
    return super.buildTargets(request)
  }

  override fun buildTargetsStreaming(request: BuildTargetsReq): Flow<BuildTargetsUpdate> {
    return super.buildTargetsStreaming(request)
  }

  override suspend fun executeActions(request: ExecuteActionsReq): ExecuteActionsRes {
    return super.executeActions(request)
  }

  override fun executeActionsStreaming(request: BuildTargetsReq): Flow<ExecuteActionUpdate> {
    return super.executeActionsStreaming(request)
  }
}
