package com.giyeok.bibix.intellij.service

import com.giyeok.bibix.base.MainSourceId
import com.giyeok.bibix.frontend.BuildFrontend
import com.giyeok.bibix.frontend.NoopProgressNotifier
import com.giyeok.bibix.intellij.BibixIntellijProto
import com.giyeok.bibix.intellij.bibixProjectInfo
import com.giyeok.bibix.intellij.contentRoot
import com.giyeok.bibix.intellij.externalLibrary
import com.giyeok.bibix.interpreter.BibixProject
import com.giyeok.bibix.interpreter.expr.Definition
import com.giyeok.bibix.plugins.jvm.*
import com.google.common.annotations.VisibleForTesting
import java.lang.AssertionError
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.bufferedReader
import kotlin.io.path.isRegularFile
import kotlin.io.path.name

object ProjectStructureExtractor {
  fun commonAncestorsOf(paths: Set<Path>): Set<Path> {
    // TODO implement
    return paths
  }

  @VisibleForTesting
  fun getContentRoots(sources: Set<Path>): List<BibixIntellijProto.ContentRoot> {
    // TODO 공통 ancestor directory 및 각 파일의 package 이름을 고려해서 content root
    val sourceCodeRoots = sources.associate { sourcePath ->
      val sourcePackage = sourcePath.bufferedReader().use {
        SourceCodePackageNameReader.readPackageName(it)
      }
      if (sourcePackage != null) {
        var path = sourcePath.normalize()
        if (path.isRegularFile()) {
          path = path.parent
        }
        for (pkgToken in sourcePackage.asReversed()) {
          if (pkgToken == path.name) {
            path = path.parent
          } else {
            break
          }
        }
        sourcePath to path
      } else {
        sourcePath to sourcePath
      }
    }

    val roots = commonAncestorsOf(sourceCodeRoots.values.toSet())

    return roots.map { source ->
      contentRoot {
        this.contentRootName = "Sources"
        this.contentRootType = "src"
        this.contentRootPath = source.absolutePathString()
      }
    }
  }

  @VisibleForTesting
  fun loadProject(projectRoot: Path, scriptName: String?): BibixIntellijProto.BibixProjectInfo {
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
      com.giyeok.bibix.intellij.module {
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
}
