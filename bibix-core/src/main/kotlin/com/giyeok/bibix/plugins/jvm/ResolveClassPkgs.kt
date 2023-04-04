package com.giyeok.bibix.plugins.jvm

import com.giyeok.bibix.base.*
import java.nio.file.Path
import java.util.LinkedList

class ResolveClassPkgs {
  data class MavenArtifact(val repo: String, val group: String, val artifact: String)

  companion object {
    fun MavenDep.toMavenArtifact() = MavenArtifact(repo, group, artifact)

    fun CpInfo.toPaths(): List<Path> = when (this) {
      is ClassesInfo -> this.classDirs + this.resDirs
      is JarInfo -> listOf(this.jar)
    }

    fun flattenDeps(classPkgs: List<ClassPkg>): Map<ClassOrigin, ClassPkg> {
      val cpsMap = mutableMapOf<ClassOrigin, ClassPkg>()

      fun traversePkg(pkg: ClassPkg) {
        cpsMap[pkg.origin] = pkg
        pkg.deps.forEach { traversePkg(it) }
        pkg.runtimeDeps.forEach { traversePkg(it) }
      }

      classPkgs.forEach { traversePkg(it) }

      return cpsMap.toMap()
    }

    fun cpsMap(classPkgs: List<ClassPkg>): Map<ClassOrigin, CpInfo> {
      val cpsMap = mutableMapOf<ClassOrigin, CpInfo>()

      fun traversePkg(pkg: ClassPkg) {
        cpsMap[pkg.origin] = pkg.cpinfo
        pkg.deps.forEach { traversePkg(it) }
        pkg.runtimeDeps.forEach { traversePkg(it) }
      }

      classPkgs.forEach { traversePkg(it) }

      return cpsMap.toMap()
    }

    data class MavenVersionDepths(
      val depth: Int,
      val paths: MutableList<List<ClassOrigin>>,
    )

    fun mavenArtifactVersionsToUse(classPkgs: List<ClassPkg>): Map<MavenArtifact, MavenDep> {
      // value는 버젼명 -> root로부터의 거리
      val depthMap = mutableMapOf<MavenArtifact, MutableMap<MavenDep, MavenVersionDepths>>()
      val path = LinkedList<ClassOrigin>()

      fun traversePkg(pkg: ClassPkg, depth: Int) {
        if (pkg.origin is MavenDep) {
          path.push(pkg.origin)
          val key = pkg.origin.toMavenArtifact()
          val depths = depthMap.getOrPut(key) { mutableMapOf() }
          val existingDepth = depths[pkg.origin]
          if (existingDepth == null || depth < existingDepth.depth) {
            depths[pkg.origin] = MavenVersionDepths(depth, mutableListOf(path.toList()))
          } else if (depth == existingDepth.depth) {
            existingDepth.paths.add(path.toList())
          }
          pkg.deps.forEach {
            traversePkg(it, depth + 1)
          }
          path.pop()
        } else {
          pkg.deps.forEach {
            traversePkg(it, depth + 1)
          }
        }
      }
      classPkgs.forEach {
        traversePkg(it, 0)
      }

      val versionsToUse = depthMap.mapValues { (artifactName, depthsMap) ->
        val minDepth = depthsMap.minBy { it.value.depth }.value.depth
        val minDepthVersions = depthsMap.filter { it.value.depth == minDepth }.keys

        if (minDepthVersions.size > 1) {
          val depthsMapString =
            depthsMap.values.sortedBy { it.depth }.joinToString("\n") { pathSet ->
              pathSet.paths.joinToString("\n") { path ->
                "${pathSet.depth}: " + path.reversed().toString()
              }
            }
          throw IllegalStateException("conflicting versions: $artifactName\n$depthsMapString")
        }
        minDepthVersions.first()
      }
      return versionsToUse
    }

    fun compileDeps(classPkgs: List<ClassPkg>): Set<ClassOrigin> {
      val deps = mutableSetOf<ClassOrigin>()

      fun traversePkg(pkg: ClassPkg) {
        deps.add(pkg.origin)
        pkg.deps.forEach { traversePkg(it) }
      }

      classPkgs.forEach {
        traversePkg(it)
      }

      return deps.toSet()
    }

    fun runtimeDeps(classPkgs: List<ClassPkg>): Set<ClassOrigin> {
      val deps = mutableSetOf<ClassOrigin>()

      fun traversePkg(pkg: ClassPkg) {
        deps.add(pkg.origin)
        pkg.runtimeDeps.forEach { traversePkg(it) }
      }

      classPkgs.forEach { pkg ->
        pkg.runtimeDeps.forEach { traversePkg(it) }
      }

      return deps.toSet()
    }

    data class FlattenedClassPkgs(
      val compileDeps: Map<ClassOrigin, ClassPkg>,
      val runtimeDeps: Map<ClassOrigin, ClassPkg>,
    )

    fun flattenClassPkgs(classPkgs: List<ClassPkg>): FlattenedClassPkgs {
      val pkgsMap = flattenDeps(classPkgs)
      val mavenVersions = mavenArtifactVersionsToUse(classPkgs)

      fun filterMavenDeps(origins: Collection<ClassOrigin>): Set<ClassOrigin> =
        origins.mapNotNull { origin ->
          when (origin) {
            is MavenDep -> {
              val chosen = mavenVersions[origin.toMavenArtifact()]!!
              if (chosen == origin) chosen else null
            }

            else -> origin
          }
        }.toSet()

      val compileDeps = filterMavenDeps(compileDeps(classPkgs))
      val runtimeDeps = filterMavenDeps(runtimeDeps(classPkgs))

      return FlattenedClassPkgs(
        compileDeps.associateWith { pkgsMap[it]!! },
        runtimeDeps.associateWith { pkgsMap[it]!! })
    }

    fun resolveClassPkgs(classPkgs: List<ClassPkg>): ClassPaths {
      val flattened = flattenClassPkgs(classPkgs)

      fun depsToPaths(cps: Collection<ClassPkg>): List<Path> =
        cps.flatMap { it.cpinfo.toPaths() }

      return ClassPaths(
        depsToPaths(flattened.compileDeps.values),
        depsToPaths(flattened.runtimeDeps.filterKeys { flattened.compileDeps.contains(it) }.values)
      )
    }
  }

  fun build(context: BuildContext): BibixValue {
    val classPkgBibixValues = context.arguments.getValue("classPkgs") as SetValue
    // TODO MavenDep인 pkg들 중에 버전이 충돌하는 경우, root에서 가까운 것들만 살리고 나머지는 삭제
    // 여기서는 클래스 이름이 충돌하는 것까지 확인하진 않는다.
    val classPkgs = classPkgBibixValues.values.map { ClassPkg.fromBibix(it) }

    val cps = resolveClassPkgs(classPkgs)
    return cps.toBibix()
  }
}
