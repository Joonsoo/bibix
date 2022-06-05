package com.giyeok.bibix.frontend.daemon

import com.giyeok.bibix.base.CName
import com.giyeok.bibix.base.MainSourceId
import com.giyeok.bibix.daemon.*
import com.giyeok.bibix.daemon.BibixDaemonApiProto.IntellijModuleNode
import com.giyeok.bibix.daemon.IntellijDependencyNodeKt.intellijLibraryNode
import com.giyeok.bibix.daemon.IntellijDependencyNodeKt.intellijMavenDependencyNode
import com.giyeok.bibix.frontend.BuildFrontend
import com.giyeok.bibix.plugins.*
import com.giyeok.bibix.runner.BuildTask
import com.giyeok.bibix.runner.hashString
import com.giyeok.bibix.utils.hexToByteString
import com.giyeok.bibix.utils.toHexString
import com.google.protobuf.ByteString
import java.nio.file.Path
import kotlin.io.path.absolutePathString

class IntellijModulesExtractor(
  val frontend: BuildFrontend,
  val modules: List<IntellijProjectExtractor.ModuleData>,
) {
  val modulesByName = modules.associateBy { it.cname }

  val allModulesByObjHash: Map<ByteString, ClassPkg> =
    modules.flatMap { traverseAllModuleObjHashes(it.deps) }.toMap()

  val moduleObjHashes: Map<ByteString, CName> = modules.filter { it.cname.sourceId == MainSourceId }
    .mapNotNull { module ->
      val name = module.cname
      frontend.buildRunner.getObjectIdOfTask(BuildTask.ResolveName(name))
        ?.let { it.hashString() to name }
    }.toMap()

  private fun traverseAllModuleObjHashes(pkgs: List<ClassPkg>): List<Pair<ByteString, ClassPkg>> =
    pkgs.flatMap { pkg ->
      if (pkg.origin is LocalBuilt) {
        listOf(pkg.origin.objHash.hexToByteString() to pkg) +
          traverseAllModuleObjHashes(pkg.deps)
      } else listOf()
    }

  fun moduleCNameByObjHash(objHash: ByteString): CName? = moduleObjHashes[objHash]
  fun moduleNameByObjHash(objHash: ByteString): String =
    moduleCNameByObjHash(objHash)?.tokens?.joinToString(".") ?: objHash.toHexString()

  fun convertModule(moduleObjHash: ByteString, classPkg: ClassPkg): IntellijModuleNode {
    val cname = moduleCNameByObjHash(moduleObjHash)
    val moduleData = cname?.let { modulesByName[it] }

    return if (moduleData != null) {
      convertNamedModule(moduleData)
    } else {
      convertUnnamedModule(moduleObjHash, classPkg)
    }
  }

  fun convertNamedModule(moduleData: IntellijProjectExtractor.ModuleData): IntellijModuleNode {
    val contentRoots = moduleData.srcs.map { it.parent }

    return intellijModuleNode {
      this.name = moduleData.cname.tokens.joinToString(".")
      this.path = contentRoots[0].absolutePathString()

      this.dependencies.addAll(moduleData.deps.map { convertDependency(it) })
      this.contentRoots.addAll(contentRoots.map { contentRoot ->
        intellijContentRootNode {
          this.path = contentRoot.absolutePathString()
        }
      })
    }
  }

  // TODO maven transitive dependency resolve해서 전부 넣어줘야될듯?
  private fun convertDependency(dep: ClassPkg): BibixDaemonApiProto.IntellijDependencyNode =
    intellijDependencyNode {
      when (val origin = dep.origin) {
        is LocalBuilt ->
          this.moduleDependency = moduleNameByObjHash(origin.objHash.hexToByteString())
        is LocalLib ->
          this.libraryDependency = intellijLibraryNode {
            this.path = origin.path.absolutePathString()
          }
        is MavenDep ->
          this.mavenDependency = intellijMavenDependencyNode {
            this.group = origin.group
            this.artifact = origin.artifact
            this.version = origin.version
            this.path = (dep.cpinfo as JarInfo).jar.absolutePathString()
          }
      }
    }

  fun convertUnnamedModule(moduleObjHash: ByteString, classPkg: ClassPkg): IntellijModuleNode {
    val srcs: List<Path>? = when (classPkg.origin) {
      is LocalBuilt -> (classPkg.cpinfo as? ClassesInfo)?.srcs
      else -> null
    }
    val contentRoots = srcs

    return intellijModuleNode {
      this.name = moduleObjHash.toHexString()
      srcs?.let { this.path = srcs[0].absolutePathString() }

      this.dependencies.addAll(classPkg.deps.map { convertDependency(it) })
      contentRoots?.forEach { contentRoot ->
        this.contentRoots.add(intellijContentRootNode {
          this.path = contentRoot.absolutePathString()
        })
      }
    }
  }

  fun extractModules(): List<IntellijModuleNode> = allModulesByObjHash.map { (objHash, module) ->
    convertModule(objHash, module)
  }
}
