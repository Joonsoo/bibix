package com.giyeok.bibix.plugins.jvm

import com.giyeok.bibix.base.*
import java.io.File

class ResolveClassPkgs {
  sealed class ClassOrigin {
    data class MavenDep(
      val repo: String,
      val group: String,
      val artifact: String,
      val version: String
    ) : ClassOrigin() {
      fun toMavenArtifact() = MavenArtifact(repo, group, artifact)
    }

    data class LocalBuilt(val desc: String) : ClassOrigin()

    data class LocalLib(val path: File) : ClassOrigin()

    companion object {
      fun fromBibix(value: BibixValue): ClassOrigin {
        value as ClassInstanceValue
        check(value.className.tokens == listOf("ClassOrigin"))
        value.value as ClassInstanceValue

        return when (value.value.className.tokens) {
          listOf("MavenDep") -> {
            val (repo, group, artifact, version) = (value.value.value as NamedTupleValue).values
            MavenDep(
              (repo.second as StringValue).value,
              (group.second as StringValue).value,
              (artifact.second as StringValue).value,
              (version.second as StringValue).value,
            )
          }
          listOf("LocalBuilt") -> {
            LocalBuilt(((value.value.value as NamedTupleValue).values[0].second as StringValue).value)
          }
          listOf("LocalLib") -> {
            LocalLib(((value.value.value as NamedTupleValue).values[0].second as PathValue).path)
          }
          else -> TODO()
        }
      }

      fun toBibix(value: ClassOrigin): BibixValue {
        TODO()
      }
    }
  }

  data class MavenArtifact(val repo: String, val group: String, val artifact: String)

  data class ClassPkg(
    val origin: ClassOrigin,
    val cps: List<File>,
    val deps: List<ClassPkg>,
  )

  fun bibixToKt(value: BibixValue): ClassPkg {
    value as ClassInstanceValue
    check(value.className.tokens == listOf("ClassPkg"))
    val (origin, cps, deps) = (value.value as NamedTupleValue).values

    return ClassPkg(
      ClassOrigin.fromBibix(origin.second),
      (cps.second as SetValue).values.map { (it as PathValue).path },
      (deps.second as SetValue).values.map { bibixToKt(it) }
    )
  }

  fun ktToBibix(value: ClassPkg): ClassInstanceValue {
    TODO()
  }

  fun build(context: BuildContext): BibixValue {
    val classPkgBibixValues = context.arguments.getValue("classPkgs") as SetValue
    // ClassPkg = (origin: ClassOrigin, cps: set<path>, deps: set<ClassPkg>)

    // class ClassOrigin = {MavenDep, LocalBuilt, LocalLib}
    // class MavenDep = (repo: string, group: string, artifact: string, version: string)
    // class LocalBuilt = (desc: string)
    // class LocalLib = (path: path)

    // TODO MavenDep인 pkg들 중에 버전이 충돌하는 경우, root에서 가까운 것들만 살리고 나머지는 삭제
    // 여기서는 클래스 이름이 충돌하는 것까지 확인하진 않는다.
    val classPkgs = classPkgBibixValues.values.map { bibixToKt(it) }

    val pkgToPaths = mutableMapOf<MavenArtifact, MutableList<List<ClassOrigin>>>()
    val mavenDepsMap = mutableMapOf<ClassOrigin.MavenDep, ClassPkg>()
    val cps = mutableSetOf<File>()

    fun traversePkg(pkg: ClassPkg, path: List<ClassOrigin>) {
      if (pkg.origin is ClassOrigin.MavenDep) {
        mavenDepsMap[pkg.origin] = pkg
        val mavenArtifact = pkg.origin.toMavenArtifact()
        pkgToPaths.putIfAbsent(mavenArtifact, mutableListOf())
        pkgToPaths.getValue(mavenArtifact).add(path)
      } else {
        cps.addAll(pkg.cps)
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
      versionToUse as ClassOrigin.MavenDep
      check(versionToUse.toMavenArtifact() == mavenArtifact)
      cps.addAll(mavenDepsMap.getValue(versionToUse).cps)
    }
    return SetValue(cps.map { PathValue(it) })
  }
}
