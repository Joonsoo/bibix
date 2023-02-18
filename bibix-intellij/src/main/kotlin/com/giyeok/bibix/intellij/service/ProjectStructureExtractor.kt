package com.giyeok.bibix.intellij.service

import com.giyeok.bibix.base.MainSourceId
import com.giyeok.bibix.base.StringValue
import com.giyeok.bibix.frontend.BuildFrontend
import com.giyeok.bibix.frontend.NoopProgressNotifier
import com.giyeok.bibix.intellij.*
import com.giyeok.bibix.interpreter.BibixProject
import com.giyeok.bibix.interpreter.expr.Definition
import com.giyeok.bibix.plugins.jvm.*
import com.giyeok.bibix.plugins.maven.Artifact
import com.google.common.annotations.VisibleForTesting
import java.nio.file.Path
import kotlin.io.path.*

object ProjectStructureExtractor {
  @VisibleForTesting
  fun getContentRoots(module: ModuleData): List<BibixIntellijProto.ContentRoot> {
    // 공통 ancestor directory 및 각 파일의 package 이름을 고려해서 content root
    val sourceCodeRoots = module.sources.associate { sourcePath ->
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

    val srcRoots = sourceCodeRoots.values.map { source ->
      contentRoot {
        this.contentRootName = "Sources"
        this.contentRootType = "src"
        this.contentRootPath = source.absolutePathString()
      }
    }

    val resRoots = module.resourceDirs.map { resDir ->
      contentRoot {
        this.contentRootName = "Resources"
        this.contentRootType = "res"
        this.contentRootPath = resDir.absolutePathString()
      }
    }

    return srcRoots + resRoots
  }

  fun commonAncestorOfPaths(paths: Set<Path>): Path {
    fun parentsOf(path: Path): List<Path> {
      val list = mutableListOf<Path>()
      var path = path
      while (path.nameCount > 0) {
        list.add(path)
        path = path.parent
      }
      list.add(path)
      return list.reversed()
    }

    fun commonAncestorOf(a: Path, b: Path): Path =
      when {
        a.startsWith(b) -> b
        b.startsWith(a) -> a
        else -> {
          val aParents = parentsOf(a)
          val bParents = parentsOf(b)
          check(aParents.first() == bParents.first()) // 모두 루트
          var lastCommon = 0
          while (lastCommon < aParents.size && lastCommon < bParents.size) {
            if (aParents[lastCommon] != bParents[lastCommon]) break
            lastCommon += 1
          }
          aParents[lastCommon - 1]
        }
      }

    fun merge(path: Path, rest: List<Path>): Path =
      if (rest.isEmpty()) {
        path
      } else {
        merge(commonAncestorOf(path, rest.first()), rest.drop(1))
      }

    val pathsList = paths.toList().map { it.absolute().normalize() }
    return merge(pathsList.first(), pathsList.drop(1))
  }

  @VisibleForTesting
  fun assignModuleRoots(
    projectRoot: Path,
    moduleContentRoots: Map<LocalBuilt, List<BibixIntellijProto.ContentRoot>>
  ): Map<LocalBuilt, Path> {
    val moduleContentRootAncestors = moduleContentRoots
      .filterValues { it.isNotEmpty() }
      .mapValues { (_, contentRoots) ->
        commonAncestorOfPaths(contentRoots.map { Path(it.contentRootPath) }.toSet())
      }
    // projectRoot에서부터 아래로 내려가면서 점유된 모듈이 하나뿐인 서브디렉토리를 해당 모듈의 루트로 지정

    val moduleRoots = mutableMapOf<LocalBuilt, Path>()
    val occupants = mutableMapOf<Path, MutableSet<LocalBuilt>>()

    moduleContentRootAncestors.forEach { (moduleId, rootAncestor) ->
      if (!rootAncestor.startsWith(projectRoot)) {
        moduleRoots[moduleId] = rootAncestor
      } else {
        var path = rootAncestor
        while (path.startsWith(projectRoot)) {
          occupants.getOrPut(path) { mutableSetOf() }.add(moduleId)
          path = path.parent
        }
      }
    }

    fun traverse(path: Path) {
      val occup = occupants[path]
      if (occup != null) {
        if (occup.size == 1) {
          moduleRoots[occup.first()] = path
        } else {
          path.listDirectoryEntries().forEach { child ->
            traverse(child)
          }
        }
      }
    }
    traverse(projectRoot)

    // module root가 할당되지 않은 경우엔 그냥 content root ancestor로 설정
    (moduleContentRootAncestors.keys - moduleRoots.keys).forEach { moduleId ->
      moduleRoots[moduleId] = moduleContentRootAncestors.getValue(moduleId)
    }

    return moduleRoots
  }

  fun libIdFromOrigin(origin: ClassOrigin): String = when (origin) {
    is LocalBuilt -> throw AssertionError()
    is LocalLib -> "local: ${origin.path.absolutePathString()}"
    is MavenDep -> "maven: ${origin.group}:${origin.artifact}:${origin.version}"
  }

  @VisibleForTesting
  fun loadProject(projectRoot: Path, scriptName: String?): BibixIntellijProto.BibixProjectInfo {
    val javaModulesCollector = ModulesCollector("java", null)
    val ktjvmModulesCollector =
      ModulesCollector("ktjvm", Pair("org.jetbrains.kotlin", "kotlin-stdlib"))
    val scalaModulesCollector =
      ModulesCollector("scala", Pair("org.scala-lang", "scala-library"))
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

    val rootModuleName = projectRoot.name

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

    fun moduleName(objHash: String): String =
      "$rootModuleName." + (objectNamesMap[objHash] ?: objHash)

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

    val moduleContentRoots = pkgGraph.modules.mapValues { (moduleId, module) ->
      getContentRoots(module)
    }

    val moduleRoots = assignModuleRoots(projectRoot, moduleContentRoots)

    val projectModules = pkgGraph.modules.map { (moduleId, module) ->
      val moduleRoot = moduleRoots[moduleId]?.absolutePathString()
      module {
        this.moduleName = moduleName(module.origin.objHash)
        this.moduleType = module.languageType
        this.moduleRootPath = moduleRoot ?: ""
        this.contentRoots.addAll(moduleContentRoots.getValue(moduleId))
        when (module.languageType) {
          "ktjvm" -> {
            this.moduleSdks.add(moduleSdk {
              val sdkVersion = (module.allArgs["sdkVersion"] as StringValue).value
              this.ktjvmSdkVersion = sdkVersion
            })
            this.moduleSdks.add(moduleSdk {
              val jdkVersion = (module.allArgs["outVersion"] as StringValue).value
              this.jdkVersion = jdkVersion
            })
          }

          "scala" -> {
            this.moduleSdks.add(moduleSdk {
              val sdkVersion = (module.allArgs["sdkVersion"] as StringValue).value
              this.scalaSdkVersion = sdkVersion
            })
            this.moduleSdks.add(moduleSdk {
              val jdkVersion = (module.allArgs["outVersion"] as StringValue).value
              this.jdkVersion = jdkVersion
            })
          }

          else -> this.moduleSdks.add(moduleSdk {
            // assuming java
            val jdkVersion = (module.allArgs["jdkVersion"] as StringValue).value
            this.jdkVersion = jdkVersion
          })
        }
        val deps = pkgGraph.dependentNodesOf(module.origin)
        this.moduleDeps.addAll(deps.filterIsInstance<LocalBuilt>().map { moduleName(it.objHash) })
        this.libraryDeps.addAll(deps.filter { it !is LocalBuilt }.map { libIdFromOrigin(it) })
      }
    }

    val sdks = getSdksForModules(buildFrontend, pkgGraph.modules)

    return bibixProjectInfo {
      this.projectId = "--"
      this.projectName = projectRoot.name
      this.modules.add(module {
        this.moduleName = rootModuleName
        this.moduleType = "root"
        this.moduleRootPath = projectRoot.absolutePathString()
        this.contentRoots.add(contentRoot {
          this.contentRootName = rootModuleName
          this.contentRootType = "root"
          this.contentRootPath = projectRoot.absolutePathString()
        })
        this.contentRoots.add(contentRoot {
          this.contentRootName = "bbxbuild"
          this.contentRootType = "excluded"
          this.contentRootPath = projectRoot.resolve("bbxbuild").absolutePathString()
        })
      })
      this.modules.addAll(projectModules)
      this.externalLibraries.addAll(externalLibraries)
      this.sdks = sdks
    }
  }

  private fun getSdksForModules(
    buildFrontend: BuildFrontend,
    modules: Map<LocalBuilt, ModuleData>
  ): BibixIntellijProto.Sdks {
    val ktjvmModules = modules.filterValues { it.languageType == "ktjvm" }
    val scalaModules = modules.filterValues { it.languageType == "scala" }

    val kotlinSdkVersions = ktjvmModules.values.mapNotNull { it.sdk }.distinctBy { it.first }
    val scalaSdkVersions = scalaModules.values.mapNotNull { it.sdk }.distinctBy { it.first }

    val scalaCompilers = scalaSdkVersions.associate { (scalaVersion, _) ->
      val compiler = Artifact.resolveArtifact(
        buildFrontend.repo.prepareSharedDirectory(Artifact.sharedRepoName),
        groupId = "org.scala-lang",
        artifactId = "scala-compiler",
        extension = "jar",
        version = scalaVersion,
        scope = "compile",
        javaScope = "jar",
        repos = listOf()
      )
      val reflect = Artifact.resolveArtifact(
        buildFrontend.repo.prepareSharedDirectory(Artifact.sharedRepoName),
        groupId = "org.scala-lang",
        artifactId = "scala-reflect",
        extension = "jar",
        version = scalaVersion,
        scope = "compile",
        javaScope = "jar",
        repos = listOf()
      )
      val library = Artifact.resolveArtifact(
        buildFrontend.repo.prepareSharedDirectory(Artifact.sharedRepoName),
        groupId = "org.scala-lang",
        artifactId = "scala-reflect",
        extension = "jar",
        version = scalaVersion,
        scope = "compile",
        javaScope = "jar",
        repos = listOf()
      )
      scalaVersion to listOf(compiler, reflect, library)
    }
    val scalaCompilerClasspaths = scalaCompilers.mapValues { (_, classPkgs) ->
      ResolveClassPkgs.resolveClassPkgs(classPkgs).map { it.absolutePathString() }
    }

    return sdks {
      kotlinSdkVersions.forEach { (sdkVersion, sdkArtifact) ->
        this.ktjvmSdks.add(kotlinJvmSdk {
          this.version = sdkVersion
          this.sdkLibraryIds.add(libIdFromOrigin(sdkArtifact.origin))
        })
      }
      scalaSdkVersions.forEach { (scalaVersion, sdkArtifact) ->
        val scalaLangVersion = scalaVersion.split('.').take(2).joinToString(".")
        this.scalaSdks.add(scalaSdk {
          this.version = scalaVersion
          this.scalaLanguageVersion = scalaLangVersion
          this.compilerClasspaths.addAll(scalaCompilerClasspaths[scalaVersion] ?: setOf())
          this.sdkLibraryIds.add(libIdFromOrigin(sdkArtifact.origin))
        })
      }
    }
  }
}
