package com.giyeok.bibix.intellij.service

import com.giyeok.bibix.base.DummyProgressLogger
import com.giyeok.bibix.base.StringValue
import com.giyeok.bibix.frontend.BuildFrontend
import com.giyeok.bibix.graph.BibixProjectLocation
import com.giyeok.bibix.intellij.*
import com.giyeok.bibix.plugins.jvm.*
import com.giyeok.bibix.plugins.maven.Artifact
import com.giyeok.bibix.repo.BibixRepoProto.BibixRepoData
import com.giyeok.bibix.utils.toBibix
import com.google.common.annotations.VisibleForTesting
import java.nio.file.Path
import kotlin.io.path.*

class ProjectInfoBuilder(
  val projectLocation: BibixProjectLocation,
  val buildResults: Map<String, ClassPkg>,
  val repoData: BibixRepoData.Builder,
) {
  // target id -> name
  val namedTargets: Map<String, String> = buildResults.map {
    (it.value.origin as LocalBuilt).objHash to it.key
  }.toMap()

  val projectRoot = projectLocation.projectRoot
  val rootModuleName = projectRoot.name

  data class ModuleFiles(val srcs: List<Path>, val resDirs: List<Path>)

  private val originMap = mutableMapOf<ClassOrigin, ClassPkg>()
  private val sourceFiles = mutableMapOf<LocalBuilt, ModuleFiles>()

  init {
    buildResults.forEach { (_, classPkg) ->
      traverseClassPkg(classPkg)
    }
  }

  private fun traverseClassPkg(classPkg: ClassPkg) {
    when (val origin = classPkg.origin) {
      is LocalBuilt -> {
        val cpinfo = classPkg.cpinfo
        check(cpinfo is ClassesInfo)
        sourceFiles[origin] = ModuleFiles(cpinfo.srcs!!, cpinfo.resDirs)
      }

      else -> {
        // do nothing
      }
    }
    originMap[classPkg.origin] = classPkg
    classPkg.deps.forEach { traverseClassPkg(it) }
    classPkg.runtimeDeps.forEach { traverseClassPkg(it) }
  }

  fun build(): BibixIntellijProto.BibixProjectInfo {
    val moduleContentRoots = sourceFiles.map { (origin, files) ->
      origin to getContentRoots(files.srcs.toSet(), files.resDirs.toSet())
    }.toMap()
    val moduleRoots = assignModuleRoots(projectRoot, moduleContentRoots)

    return bibixProjectInfo {
      this.projectId = "--"
      this.projectName = rootModuleName
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
      buildResults.forEach { (moduleName, classPkg) ->
        val moduleContentRoot = moduleContentRoots[classPkg.origin]!!
        val moduleRoot = moduleRoots[classPkg.origin]
        this.modules.add(createModule(moduleName, classPkg, moduleContentRoot, moduleRoot))
      }
      usedExternalLibraries.values.forEach { pkg ->
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
//      TODO this.sdkInfo = sdks
    }
  }

  fun moduleNameFrom(targetId: String): String {
    val userName = namedTargets[targetId] ?: targetId
    return "$rootModuleName.$userName"
  }

  private val usedExternalLibraries = mutableMapOf<ClassOrigin, ClassPkg>()

  private fun createModule(
    moduleName: String,
    classPkg: ClassPkg,
    moduleContentRoots: List<BibixIntellijProto.ContentRoot>,
    moduleRoot: Path?
  ): BibixIntellijProto.Module = module {
    this.moduleName = moduleName
    val origin = classPkg.origin as LocalBuilt

    val targetIdData = repoData.getTargetIdDataOrDefault(origin.objHash, null)!!

    when (origin.builderName) {
      "java.library" -> {
        val jdkVersion = targetIdData.argsMap.pairsList.find { it.name == "jdkVersion" }
        this.usingSdks.add(sdkVersion {
          // assuming java
          // TODO val jdkVersion = (module.allArgs["jdkVersion"] as StringValue).value
          this.jdkVersion = (jdkVersion!!.value.toBibix() as StringValue).value
        })
        this.moduleType = "java"
      }

      "ktjvm.library" -> {
        val sdkVersion = targetIdData.argsMap.pairsList.find { it.name == "sdkVersion" }
        val outVersion = targetIdData.argsMap.pairsList.find { it.name == "outVersion" }
        this.usingSdks.add(sdkVersion {
          // TODO val sdkVersion = (module.allArgs["sdkVersion"] as StringValue).value
          this.ktjvmSdkVersion = (sdkVersion!!.value.toBibix() as StringValue).value
        })
        this.usingSdks.add(sdkVersion {
          // TODO val jdkVersion = (module.allArgs["outVersion"] as StringValue).value
          this.jdkVersion = (outVersion!!.value.toBibix() as StringValue).value
        })
        this.moduleType = "ktjvm"
      }

      "scala.library" -> {
        val sdkVersion = targetIdData.argsMap.pairsList.find { it.name == "sdkVersion" }
        val outVersion = targetIdData.argsMap.pairsList.find { it.name == "outVersion" }
        this.usingSdks.add(sdkVersion {
          // TODO val sdkVersion = (module.allArgs["sdkVersion"] as StringValue).value
          this.scalaSdkVersion = (sdkVersion!!.value.toBibix() as StringValue).value
        })
        this.usingSdks.add(sdkVersion {
          // TODO val jdkVersion = (module.allArgs["outVersion"] as StringValue).value
          this.jdkVersion = (outVersion!!.value.toBibix() as StringValue).value
        })
        this.moduleType = "scala"
      }

      else -> throw IllegalStateException()
    }

    this.moduleRootPath = moduleRoot?.absolutePathString() ?: ""
    this.contentRoots.addAll(moduleContentRoots)

    val deps =
      ResolveClassPkgs.flattenClassPkgs(classPkg.deps, null)

    (deps.inputDeps + deps.compileDeps).forEach { (origin, pkg) ->
      if (origin is LocalBuilt && isModule(origin)) {
        this.moduleDeps.add(BibixIntellijProto.Module.ModuleDep.newBuilder().also {
          it.moduleName = moduleNameFrom(origin.objHash)
          it.dependencyType = BibixIntellijProto.DependencyType.COMPILE_DEPENDENCY
        }.build())
      } else {
        usedExternalLibraries[origin] = pkg
        this.libraryDeps.add(BibixIntellijProto.Module.LibraryDep.newBuilder().also {
          it.libraryName = libIdFromOrigin(origin)
          it.dependencyType = BibixIntellijProto.DependencyType.COMPILE_DEPENDENCY
        }.build())
      }
    }
    deps.runtimeDeps.forEach { (origin, pkg) ->
      if (origin is LocalBuilt && isModule(origin)) {
        this.moduleDeps.add(BibixIntellijProto.Module.ModuleDep.newBuilder().also {
          it.moduleName = moduleNameFrom(origin.objHash)
          it.dependencyType = BibixIntellijProto.DependencyType.RUNTIME_DEPENDENCY
        }.build())
      } else {
        usedExternalLibraries[origin] = pkg
        this.libraryDeps.add(BibixIntellijProto.Module.LibraryDep.newBuilder().also {
          it.libraryName = libIdFromOrigin(origin)
          it.dependencyType = BibixIntellijProto.DependencyType.RUNTIME_DEPENDENCY
        }.build())
      }
    }
  }

  @VisibleForTesting
  fun assignModuleRoots(
    projectRoot: Path,
    moduleContentRoots: Map<LocalBuilt, List<BibixIntellijProto.ContentRoot>>
  ): Map<LocalBuilt, Path> {
    val moduleContentRootAncestors = moduleContentRoots
      .filterValues { it.isNotEmpty() }
      .mapValues { (_, contentRoots) ->
        commonAncestorOfPaths(contentRoots.map { Path(it.contentRootPath) }
          .toSet())
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

  fun isModule(origin: ClassOrigin) = when (origin) {
    is LocalBuilt -> namedTargets.contains(origin.objHash)
    else -> false
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

  @VisibleForTesting
  fun getContentRoots(
    sources: Set<Path>,
    resourceDirs: Set<Path>
  ): List<BibixIntellijProto.ContentRoot> {
    // 공통 ancestor directory 및 각 파일의 package 이름을 고려해서 content root
    val sourceCodeRoots = sources.associate { sourcePath ->
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

    val resRoots = resourceDirs.map { resDir ->
      contentRoot {
        this.contentRootName = "Resources"
        this.contentRootType = "res"
        this.contentRootPath = resDir.absolutePathString()
      }
    }

    return srcRoots + resRoots
  }

  companion object {
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
  }
}
