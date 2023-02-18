package com.giyeok.bibix.plugins.jvm

import com.giyeok.bibix.base.*
import java.nio.file.Path

class ResolveClassPkgs {
  data class MavenArtifact(val repo: String, val group: String, val artifact: String)

  companion object {
    fun MavenDep.toMavenArtifact() = MavenArtifact(repo, group, artifact)

    fun CpInfo.toPaths(): List<Path> = when (this) {
      is ClassesInfo -> this.classDirs + this.resDirs
      is JarInfo -> listOf(this.jar)
    }

    fun resolveClassPkgs(classPkgs: List<ClassPkg>): Set<Path> {
      val pkgToPaths = mutableMapOf<MavenArtifact, MutableList<List<ClassOrigin>>>()
      val mavenDepsMap = mutableMapOf<MavenDep, ClassPkg>()
      val cps = mutableSetOf<Path>()

      fun traversePkg(pkg: ClassPkg, path: List<ClassOrigin>) {
        if (pkg.origin is MavenDep) {
          mavenDepsMap[pkg.origin] = pkg
          val mavenArtifact = pkg.origin.toMavenArtifact()
          pkgToPaths.putIfAbsent(mavenArtifact, mutableListOf())
          pkgToPaths.getValue(mavenArtifact).add(path)
        } else {
          cps.addAll(pkg.cpinfo.toPaths())
        }
        pkg.deps.forEach { dep ->
          traversePkg(dep, path + dep.origin)
        }
      }
      classPkgs.forEach {
        traversePkg(it, listOf(it.origin))
      }

      pkgToPaths.keys.forEach { mavenArtifact ->
        val paths = pkgToPaths.getValue(mavenArtifact)
        val shortestPath = paths.minByOrNull { it.size }!!
        val versionToUse = shortestPath.last()
        versionToUse as MavenDep
        check(versionToUse.toMavenArtifact() == mavenArtifact)
        cps.addAll(mavenDepsMap.getValue(versionToUse).cpinfo.toPaths())
      }

      return cps
    }
  }

  fun build(context: BuildContext): BibixValue {
    val classPkgBibixValues = context.arguments.getValue("classPkgs") as SetValue
    // TODO MavenDep인 pkg들 중에 버전이 충돌하는 경우, root에서 가까운 것들만 살리고 나머지는 삭제
    // 여기서는 클래스 이름이 충돌하는 것까지 확인하진 않는다.
    val classPkgs = classPkgBibixValues.values.map { ClassPkg.fromBibix(it) }

    val cps = resolveClassPkgs(classPkgs)
    return ClassPaths(cps.toList()).toBibix()
  }
}
