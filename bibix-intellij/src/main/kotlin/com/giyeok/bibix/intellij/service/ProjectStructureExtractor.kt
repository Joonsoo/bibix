package com.giyeok.bibix.intellij.service

import com.giyeok.bibix.base.DummyProgressLogger
import com.giyeok.bibix.base.StringValue
import com.giyeok.bibix.frontend.BuildFrontend
import com.giyeok.bibix.graph.BibixProjectLocation
import com.giyeok.bibix.graph.runner.EvalTarget
import com.giyeok.bibix.graph.runner.PluginOverridesImpl
import com.giyeok.bibix.intellij.*
import com.giyeok.bibix.intellij.BibixIntellijProto.Module.LibraryDep
import com.giyeok.bibix.intellij.BibixIntellijProto.Module.ModuleDep
import com.giyeok.bibix.plugins.jvm.*
import com.giyeok.bibix.plugins.maven.Artifact
import com.google.common.annotations.VisibleForTesting
import kotlinx.coroutines.runBlocking
import org.codehaus.plexus.classworlds.ClassWorld
import java.nio.file.Path
import kotlin.io.path.*

object ProjectStructureExtractor {
  @VisibleForTesting
  fun getContentRoots(module: ModuleData): List<BibixIntellijProto.ContentRoot> {
    // 공통 ancestor directory 및 각 파일의 package 이름을 고려해서 content root
    val sourceCodeRoots = module.sources.associate { sourcePath ->
      println("sourcePackage: ${sourcePath.absolutePathString()}")
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
        sourcePath to sourcePath.parent
      }
    }

    val srcRoots = sourceCodeRoots.values.distinct().map { source ->
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
    is LocalBuilt -> "built: ${origin.objHash}"
    is LocalLib -> "local: ${origin.path.absolutePathString()}"
    is MavenDep -> "maven: ${origin.group}:${origin.artifact}:${origin.version}"
  }

  fun loadProject(projectRoot: Path, scriptName: String?): BibixIntellijProto.BibixProjectInfo {
    val javaModulesCollector = ModulesCollector("java", null)
    val ktjvmModulesCollector =
      ModulesCollector("ktjvm", Pair("org.jetbrains.kotlin", "kotlin-stdlib"))
    val scalaModulesCollector =
      ModulesCollector("scala", Pair("org.scala-lang", "scala-library"))
    val buildFrontend = BuildFrontend(
      BibixProjectLocation.of(projectRoot, scriptName),
      mapOf(),
      listOf(),
      targetLogFileName = "ijdaemon-log.pbsuf",
      pluginOverrides = PluginOverridesImpl(
        mapOf(
          Pair(1, "com.giyeok.bibix.plugins.java.Library") to javaModulesCollector,
          Pair(1, "com.giyeok.bibix.plugins.ktjvm.Library") to ktjvmModulesCollector,
          Pair(1, "com.giyeok.bibix.plugins.scala.Library") to scalaModulesCollector
        )
      )
    )

    val rootModuleName = projectRoot.name

    val mainTargets = buildFrontend.mainScriptDefinitions

    val moduleTargets = runBlocking {
      mainTargets.filterValues { definition ->
        if (definition is EvalTarget) {
          // TODO EvalTarget이면서 java, ktjvm, scala 플러그인에 대한 call expr인 경우만
          true
//          if (definition.target.value is BibixAst.CallExpr) {
//            val callTarget = buildFrontend.blockingEvaluateName(
//              ExprEvalContext(NameLookupContext(definition.cname).dropLastToken(), VarsContext()),
//              (definition.target.value as BibixAst.CallExpr).name.tokens,
//            )
//            when (callTarget) {
//              is EvaluationResult.RuleDef.BuildRuleDef.UserBuildRuleDef ->
//                callTarget.className == "com.giyeok.bibix.plugins.java.Library" ||
//                  callTarget.className == "com.giyeok.bibix.plugins.ktjvm.Library" ||
//                  callTarget.className == "com.giyeok.bibix.plugins.scala.Library"
//
//              else -> false
//            }
//          } else false
        } else false
      }
    }
    val results = runBlocking {
      buildFrontend.runBuildTasks(moduleTargets.values.toList())
//      buildFrontend.runBuildTasks(listOf(EvalTarget(1, 0, BibixName("phoserver.main"))))
    }
    println(results)

    // repo에 output -> name 지정되어 있는 것들 중에, modules collector에 기록된 것들 추려서 반환
//    moduleTargets.keys.forEachIndexed { index, target ->
//      try {
//        println("${index + 1}/${moduleTargets.keys.size}: Starting $target")
//        val result = buildFrontend.blockingBuildTargets(listOf(target))
//        println(result)
//      } catch (e: Exception) {
//        if (e is BibixExecutionException) {
//          val writer = StringWriter()
//          val pwriter = PrintWriter(writer)
//          pwriter.println("Task trace (size=${e.trace.size}):")
//          val descriptor =
//            TaskDescriptor(buildFrontend.interpreter.g, buildFrontend.interpreter.sourceManager)
//          e.trace.forEach { task ->
//            pwriter.println(task)
//            descriptor.printTaskDescription(task, pwriter)
//          }
//          pwriter.println("===")
//          System.err.println(writer.toString())
//        }
//        e.printStackTrace()
//        println("Failed to build $target. Ignored")
//      }
//    }

    // modules: target id -> module data
    val modules =
      javaModulesCollector.modules + ktjvmModulesCollector.modules + scalaModulesCollector.modules

    modules.forEach { (targetId, module) ->
      check(module.origin.objHash == targetId)
    }

    val targetNamesMap = mutableMapOf<String, String>()
    buildFrontend.repo.repoData.outputNamesMap.forEach { (moduleName, targetId) ->
      val existingName = targetNamesMap[moduleName]
      if (existingName == null || existingName.length > moduleName.length) {
        targetNamesMap[targetId] = moduleName
      }
    }

    val moduleTargetIds = modules.values.map { it.origin.objHash }.toSet()

    fun moduleName(targetId: String): String {
      val userName = targetNamesMap[targetId] ?: targetId
      return "$rootModuleName.$userName"
    }

    fun isModule(origin: ClassOrigin) = when (origin) {
      is LocalBuilt -> moduleTargetIds.contains(origin.objHash)
      else -> false
    }

    val moduleContentRoots = modules.values.associate { module ->
      module.origin to getContentRoots(module)
    }

    val moduleRoots = assignModuleRoots(projectRoot, moduleContentRoots)

    val externalLibraries = mutableMapOf<ClassOrigin, ClassPkg>()

    val projectModules = modules.values.map { module ->
      val moduleId = module.origin
      val moduleRoot = moduleRoots[moduleId]?.absolutePathString()
      module {
        this.moduleName = moduleName(module.origin.objHash)
        this.moduleType = module.languageType
        this.moduleRootPath = moduleRoot ?: ""
        this.contentRoots.addAll(moduleContentRoots.getValue(moduleId))
        when (module.languageType) {
          "ktjvm" -> {
            this.usingSdks.add(sdkVersion {
              val sdkVersion = (module.allArgs["sdkVersion"] as StringValue).value
              this.ktjvmSdkVersion = sdkVersion
            })
            this.usingSdks.add(sdkVersion {
              val jdkVersion = (module.allArgs["outVersion"] as StringValue).value
              this.jdkVersion = jdkVersion
            })
          }

          "scala" -> {
            this.usingSdks.add(sdkVersion {
              val sdkVersion = (module.allArgs["sdkVersion"] as StringValue).value
              this.scalaSdkVersion = sdkVersion
            })
            this.usingSdks.add(sdkVersion {
              val jdkVersion = (module.allArgs["outVersion"] as StringValue).value
              this.jdkVersion = jdkVersion
            })
          }

          else -> this.usingSdks.add(sdkVersion {
            // assuming java
            val jdkVersion = (module.allArgs["jdkVersion"] as StringValue).value
            this.jdkVersion = jdkVersion
          })
        }
        val deps =
          ResolveClassPkgs.flattenClassPkgs(listOfNotNull(module.sdk?.second) + module.deps, null)
        (deps.inputDeps + deps.compileDeps).forEach { (origin, pkg) ->
          if (origin is LocalBuilt && isModule(origin)) {
            this.moduleDeps.add(ModuleDep.newBuilder().also {
              it.moduleName = moduleName(origin.objHash)
              it.dependencyType = BibixIntellijProto.DependencyType.COMPILE_DEPENDENCY
            }.build())
          } else {
            externalLibraries[origin] = pkg
            this.libraryDeps.add(LibraryDep.newBuilder().also {
              it.libraryName = libIdFromOrigin(origin)
              it.dependencyType = BibixIntellijProto.DependencyType.COMPILE_DEPENDENCY
            }.build())
          }
        }
        deps.runtimeDeps.forEach { (origin, pkg) ->
          if (origin is LocalBuilt && isModule(origin)) {
            this.moduleDeps.add(ModuleDep.newBuilder().also {
              it.moduleName = moduleName(origin.objHash)
              it.dependencyType = BibixIntellijProto.DependencyType.RUNTIME_DEPENDENCY
            }.build())
          } else {
            externalLibraries[origin] = pkg
            this.libraryDeps.add(LibraryDep.newBuilder().also {
              it.libraryName = libIdFromOrigin(origin)
              it.dependencyType = BibixIntellijProto.DependencyType.RUNTIME_DEPENDENCY
            }.build())
          }
        }
      }
    }

    val sdks = getSdksForModules(buildFrontend, modules.values)

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
      externalLibraries.values.forEach { pkg ->
        this.externalLibraries.add(externalLibrary {
          this.libraryId = libIdFromOrigin(pkg.origin)
          when (val cpinfo = pkg.cpinfo) {
            is ClassesInfo -> {
              this.classpaths.addAll(cpinfo.classDirs.map { it.absolutePathString() })
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
        )
      }
      this.sdkInfo = sdks
    }
  }

  private fun getSdksForModules(
    buildFrontend: BuildFrontend,
    modules: Collection<ModuleData>
  ): BibixIntellijProto.SdkInfo {
    val ktjvmModules = modules.filter { it.languageType == "ktjvm" }
    val scalaModules = modules.filter { it.languageType == "scala" }

    val kotlinSdkVersions = ktjvmModules.mapNotNull { it.sdk }.distinctBy { it.first }
    val scalaSdkVersions = scalaModules.mapNotNull { it.sdk }.distinctBy { it.first }

    val scalaCompilers = scalaSdkVersions.associate { (scalaVersion, _) ->
      val compiler = Artifact.resolveArtifact(
        buildFrontend.buildEnv,
        DummyProgressLogger,
        buildFrontend.repo.prepareSharedDirectory(Artifact.sharedRepoName),
        groupId = "org.scala-lang",
        artifactId = "scala-compiler",
        extension = "jar",
        version = scalaVersion,
        scope = "compile",
        javaScope = "jar",
        repos = listOf(),
        excludes = setOf(),
      )
      val reflect = Artifact.resolveArtifact(
        buildFrontend.buildEnv,
        DummyProgressLogger,
        buildFrontend.repo.prepareSharedDirectory(Artifact.sharedRepoName),
        groupId = "org.scala-lang",
        artifactId = "scala-reflect",
        extension = "jar",
        version = scalaVersion,
        scope = "compile",
        javaScope = "jar",
        repos = listOf(),
        excludes = setOf(),
      )
      val library = Artifact.resolveArtifact(
        buildFrontend.buildEnv,
        DummyProgressLogger,
        buildFrontend.repo.prepareSharedDirectory(Artifact.sharedRepoName),
        groupId = "org.scala-lang",
        artifactId = "scala-library",
        extension = "jar",
        version = scalaVersion,
        scope = "compile",
        javaScope = "jar",
        repos = listOf(),
        excludes = setOf(),
      )
      scalaVersion to listOf(compiler, reflect, library)
    }
    val scalaCompilerClasspaths = scalaCompilers.mapValues { (_, classPkgs) ->
      ResolveClassPkgs.resolveClassPkgs(classPkgs, null).cps.map { it.absolutePathString() }
    }

    return sdkInfo {
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
