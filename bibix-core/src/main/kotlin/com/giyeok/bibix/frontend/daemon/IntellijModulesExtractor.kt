//package com.giyeok.bibix.frontend.daemon
//
//import com.giyeok.bibix.base.CName
//import com.giyeok.bibix.base.MainSourceId
//import com.giyeok.bibix.daemon.*
//import com.giyeok.bibix.daemon.BibixDaemonApiProto.IntellijLibraryNode
//import com.giyeok.bibix.daemon.BibixDaemonApiProto.IntellijModuleNode
//import com.giyeok.bibix.daemon.IntellijLibraryNodeKt.intellijLocalLibraryNode
//import com.giyeok.bibix.daemon.IntellijLibraryNodeKt.intellijMavenLibraryNode
//import com.giyeok.bibix.daemon.IntellijLibraryNodeKt.intellijScalaSdkLibraryNode
//import com.giyeok.bibix.frontend.BuildFrontend
//import com.giyeok.bibix.plugins.*
//import com.giyeok.bibix.runner.BuildTask
//import com.giyeok.bibix.repo.hashString
//import com.giyeok.bibix.utils.hexToByteString
//import com.giyeok.bibix.utils.hexToByteStringOrNull
//import com.giyeok.bibix.utils.toHexString
//import com.google.protobuf.ByteString
//import java.nio.file.Path
//import kotlin.io.path.absolute
//import kotlin.io.path.absolutePathString
//import kotlin.io.path.inputStream
//import kotlin.io.path.name
//
//class IntellijModulesExtractor(
//  val frontend: BuildFrontend,
//  val modules: List<IntellijProjectExtractor.ModuleData>,
//) {
//  val modulesByName = modules.associateBy { it.cname }
//
//  val allModulesByObjHash: Map<ByteString, ClassPkg> =
//    modules.flatMap { traverseAllModuleObjHashes(it.deps) }.toMap()
//
//  val moduleObjHashes: Map<ByteString, CName> = modules.filter { it.cname.sourceId == MainSourceId }
//    .mapNotNull { module ->
//      val name = module.cname
//      frontend.buildRunner.getObjectIdOfTask(BuildTask.ResolveName(name))
//        ?.let { it.hashString() to name }
//    }.toMap()
//
//  private fun traverseAllModuleObjHashes(pkgs: List<ClassPkg>): List<Pair<ByteString, ClassPkg>> =
//    pkgs.flatMap { pkg ->
//      if (pkg.origin is LocalBuilt) {
//        listOf(pkg.origin.objHash.hexToByteString() to pkg) +
//          traverseAllModuleObjHashes(pkg.deps)
//      } else listOf()
//    }
//
//  fun moduleCNameByObjHash(objHash: ByteString): CName? = moduleObjHashes[objHash]
//
//  fun moduleDataByObjHash(objHash: ByteString): IntellijProjectExtractor.ModuleData? =
//    moduleCNameByObjHash(objHash)?.let { modulesByName[it] }
//
//  /**
//   * 해시값이 objHash인 모듈에 가장 적당한 이름을 반환.
//   * 만약 사용자가 정의한 모듈이면 사용자가 정의한 이름을 반환하고, 그렇지 않으면 해시의 hex string을 반환
//   */
//  fun moduleNameByObjHash(objHash: ByteString): String {
//    val targetName = moduleCNameByObjHash(objHash)?.tokens?.joinToString(".")
//    if (targetName != null) {
//      return targetName
//    }
//    val task = frontend.buildRunner.getTaskByObjectIdHash(objHash)
//    if (task is BuildTask.ResolveName && task.cname.sourceId == MainSourceId) {
//      return task.cname.tokens.joinToString(".")
//    }
//    return objHash.toHexString()
//  }
//
//  private fun readPackageDecl(srcPath: Path): List<String>? =
//    srcPath.inputStream().bufferedReader().use {
//      SourceCodePackageDeclReader.readPackageDecl(it)
//    }
//
//  private fun sourceRootOf(srcPath: Path, packageTokens: List<String>): Path {
//    return packageTokens.asReversed().fold(srcPath.parent) { dir, token ->
//      if (dir.name == token) dir.parent else null
//    } ?: srcPath.parent
//  }
//
//  /**
//   * srcPath 파일을 읽어서 거기 정의된 package declaration을 확인한 다음 srcPath에서 패키지 경로를 제외한 경로를 반환
//   * 만약 패키지가 정의되어 있지 않거나, 도중에 오류가 발생하면 srcPath를 그대로 반환
//   */
//  private fun sourceRootOf(srcPath: Path): Path {
//    val packageName = readPackageDecl(srcPath)
//    return if (packageName == null) srcPath else sourceRootOf(srcPath, packageName)
//  }
//
//  data class ModuleContent(val source: ModuleSource, val deps: List<ClassPkg>)
//
//  sealed class ModuleSource {
//    data class SubModules(val submodules: List<ModuleName>) : ModuleSource()
//    data class SourceRoot(val path: Path) : ModuleSource()
//    object EmptySource : ModuleSource()
//  }
//
//  sealed class ModuleName {
//    data class SrcModule(val path: Path) : ModuleName()
//    data class BuildModule(val objHash: ByteString) : ModuleName()
//  }
//
//  fun flattenDependencies(
//    queue: List<ClassPkg>,
//    origins: MutableSet<ClassOrigin>,
//    pkgs: MutableList<ClassPkg>
//  ): List<ClassPkg> = if (queue.isEmpty()) {
//    pkgs
//  } else {
//    val head = queue.first()
//    val rest = queue.drop(1)
//    // TODO 중복된 maven dependency 정리
//    if (origins.contains(head.origin)) {
//      flattenDependencies(rest + head.deps, origins, pkgs)
//    } else {
//      origins.add(head.origin)
//      pkgs.add(head)
//      flattenDependencies(rest + head.deps, origins, pkgs)
//    }
//  }
//
//  fun extractModules(): Pair<List<IntellijLibraryNode>, List<IntellijModuleNode>> {
//    // module name -> (sub modules) or (content root directories)
//    // directory -> module name
//
//    val modulesMap = mutableMapOf<ModuleName, ModuleContent>()
//    val directoryToModule = mutableMapOf<Path, ModuleName>()
//    val parentModules = mutableMapOf<ModuleName, MutableList<ModuleName>>()
//
//    allModulesByObjHash.forEach { (objHash, classPkg) ->
//      val moduleData = moduleDataByObjHash(objHash)
//      val srcs = moduleData?.srcs ?: when (classPkg.cpinfo) {
//        is ClassesInfo -> classPkg.cpinfo.srcs ?: listOf()
//        is JarInfo -> listOf()
//      }
//      val srcRoots = srcs.map { it.absolute() }.associateWith { srcPath ->
//        sourceRootOf(srcPath)
//      }
//
//      // rootDirs.values.distinct()에서,
//      val srcRootPaths = srcRoots.values.distinct()
//      // 1. rootDirs 중에 subdirectory가 섞여있으면(/a/와 /a/b/c가 섞여있으면) 그중 최상위 디렉토리만 남김(/a/만 남김)
//      val srcRootDirs = srcRootPaths.filterNot { path ->
//        srcRootPaths.filter { path != it }.any { path.startsWith(it) }
//      }
//      // 2. TODO rootDirs 밑에 포함된 파일 중에 srcRoots.keys에 포함되지 않은 파일이 있으면 경고 발생
//      println(srcRootDirs)
//      // 3. srcRootDirs 디렉토리가 여러개면 모듈에 submodule을 만들어서 각각에 content root로 추가
//      when (srcRootDirs.size) {
//        0 -> {
//          modulesMap[ModuleName.BuildModule(objHash)] =
//            ModuleContent(ModuleSource.EmptySource, classPkg.deps)
//        }
//        1 -> {
//          val rootDir = srcRootDirs.first()
//          val existing = directoryToModule[rootDir]
//          val moduleName = ModuleName.BuildModule(objHash)
//          if (existing == null) {
//            modulesMap[moduleName] = ModuleContent(ModuleSource.SourceRoot(rootDir), classPkg.deps)
//            directoryToModule[rootDir] = moduleName
//          } else {
//            modulesMap[moduleName] =
//              ModuleContent(ModuleSource.SubModules(listOf(existing)), classPkg.deps)
//            parentModules.getOrPut(existing, ::mutableListOf).add(moduleName)
//          }
//        }
//        else -> {
//          val parentModule = ModuleName.BuildModule(objHash)
//          val subs = srcRootDirs.map { srcRoot ->
//            val existing = directoryToModule[srcRoot]
//            if (existing != null) {
//              existing
//            } else {
//              val srcRootHash = if (srcRoot.startsWith(frontend.repo.objectsDirectory)) {
//                srcRoot.getName(frontend.repo.objectsDirectory.nameCount).name.hexToByteStringOrNull()
//              } else null
//              val moduleName = if (srcRootHash != null) {
//                ModuleName.BuildModule(srcRootHash)
//              } else {
//                ModuleName.SrcModule(srcRoot)
//              }
//              // TODO deps?
//              modulesMap[moduleName] =
//                ModuleContent(ModuleSource.SourceRoot(srcRoot), classPkg.deps)
//              directoryToModule[srcRoot] = moduleName
//              moduleName
//            }
//          }
//          // TODO deps?
//          modulesMap[parentModule] = ModuleContent(ModuleSource.SubModules(subs), listOf())
//          subs.forEach { sub ->
//            parentModules.getOrPut(sub, ::mutableListOf).add(parentModule)
//          }
//        }
//      }
//    }
//
//    val moduleNamesMap = modulesMap.keys.associateWith { moduleName ->
//      when (moduleName) {
//        is ModuleName.BuildModule -> moduleNameByObjHash(moduleName.objHash)
//        is ModuleName.SrcModule -> moduleName.path.name
//      }
//    }
//    if (moduleNamesMap.values.distinct().size != moduleNamesMap.size) {
//      // 중복된 이름 발생 -> 수정 요
//      TODO()
//    }
//    val allLibraries =
//      modulesMap.values.flatMap { flattenDependencies(it.deps, mutableSetOf(), mutableListOf()) }
//    val libraryMap = allLibraries.mapNotNull { dep ->
//      fun ClassPkg.getPath(): Path = when (this.cpinfo) {
//        is ClassesInfo -> {
//          if (this.cpinfo.classDirs.size != 1) {
//            throw IllegalStateException("???")
//          }
//          this.cpinfo.classDirs.first()
//        }
//        is JarInfo -> this.cpinfo.jar
//      }
//
//      fun ClassPkg.getSourcePath(): Path? = null
//
//      when (dep.origin) {
//        is MavenDep -> {
//          if (dep.origin.group == "org.scala-lang" && dep.origin.artifact == "scala-library") {
//            // scala dependency인 경우 처리
//            val version = dep.origin.version
//            dep.origin to intellijLibraryNode {
//              this.libraryName = "Scala SDK: $version"
//              this.classpath.add(dep.getPath().absolutePathString())
//              dep.getSourcePath()?.let { this.source.add(it.absolutePathString()) }
//              this.scalaSdkLibrary = intellijScalaSdkLibraryNode {
//                this.scalaVersion = version
//              }
//            }
//          } else {
//            dep.origin to intellijLibraryNode {
//              this.libraryName =
//                "Maven: ${dep.origin.group}:${dep.origin.artifact}:${dep.origin.version}"
//              this.classpath.add(dep.getPath().absolutePathString())
//              dep.getSourcePath()?.let { this.source.add(it.absolutePathString()) }
//              this.mavenLibrary = intellijMavenLibraryNode {
//                this.group = dep.origin.group
//                this.artifact = dep.origin.artifact
//                this.version = dep.origin.version
//              }
//            }
//          }
//        }
//        is LocalLib -> dep.origin to intellijLibraryNode {
//          this.libraryName = "Local: ${dep.origin.path.absolutePathString()}"
//          this.classpath.add(dep.getPath().absolutePathString())
//          dep.getSourcePath()?.let { this.source.add(it.absolutePathString()) }
//          this.localLibrary = intellijLocalLibraryNode {
//            this.path = dep.origin.path.absolutePathString()
//          }
//        }
//        is LocalBuilt -> null
//      }
//    }.toMap()
//    val modules = modulesMap.map { (moduleName, moduleContent) ->
//      val moduleNameString = moduleNamesMap.getValue(moduleName)
//
//      intellijModuleNode {
//        this.name = moduleNameString
//        when (val moduleSource = moduleContent.source) {
//          ModuleSource.EmptySource -> {
//            // do nothing..?
//          }
//          is ModuleSource.SourceRoot ->
//            this.contentRoots.add(intellijContentRootNode {
//              this.path = moduleSource.path.absolutePathString()
//            })
//          is ModuleSource.SubModules -> {
//            this.dependencies.addAll(moduleSource.submodules.map { subModule ->
//              intellijDependencyNode {
//                this.moduleName = moduleNamesMap.getValue(subModule)
//              }
//            })
//          }
//        }
//        // TODO this.path
//        when (moduleName) {
//          is ModuleName.BuildModule -> {
//            val objPath =
//              frontend.repo.objectsDirectory.resolve(moduleName.objHash.toHexString() + "-build")
//            this.objectPath = objPath.absolutePathString()
//          }
//          is ModuleName.SrcModule -> {
//            // TODO 제대로
//            this.objectPath =
//              frontend.repo.objectsDirectory.resolve(moduleName.path.name).absolutePathString()
//          }
//        }
//        this.jdkVersion = "16" // TODO
//        val flattenedDeps = flattenDependencies(moduleContent.deps, mutableSetOf(), mutableListOf())
//        val libraries =
//          flattenedDeps.mapNotNull { dep -> libraryMap[dep.origin]?.let { dep.origin to it } }
//            .toMap()
//        this.libraries.addAll(libraries.values)
//        this.dependencies.addAll(flattenedDeps.map { dep ->
//          when (dep.origin) {
//            is MavenDep, is LocalLib -> intellijDependencyNode {
//              this.libraryName = libraries.getValue(dep.origin).libraryName
//            }
//            is LocalBuilt -> {
//              intellijDependencyNode {
//                this.moduleName =
//                  moduleNamesMap.getValue(ModuleName.BuildModule(dep.origin.objHash.hexToByteString()))
//              }
//            }
//          }
//        })
//      }
//    }
//    return Pair(libraryMap.values.toList(), modules)
//  }
//}
