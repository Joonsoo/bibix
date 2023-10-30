package com.giyeok.bibix.graph2.runner

import com.giyeok.bibix.plugins.jvm.ClassPkg
import com.giyeok.bibix.plugins.jvm.LocalLib
import com.giyeok.bibix.plugins.jvm.MavenDep
import com.giyeok.bibix.plugins.jvm.ResolveClassPkgs
import com.giyeok.bibix.plugins.jvm.ResolveClassPkgs.Companion.toPaths
import org.codehaus.plexus.classworlds.ClassWorld
import org.codehaus.plexus.classworlds.realm.ClassRealm
import java.nio.file.Path
import kotlin.io.path.absolute
import kotlin.io.path.name

class ClassPkgRunner(private val classWorld: ClassWorld) {
  private var realmIdCounter = 0

  private val realmCache = mutableMapOf<Set<Path>, ClassRealm>()

  private val baseRealm = classWorld.newRealm("bibix-root-realm")

  fun getPluginImplInstance(classPkg: ClassPkg, className: String): Any =
    getPluginImplInstance(prepareClassPathsForPlugin(classPkg), className)

  fun getPluginImplInstance(cps: List<Path>, className: String): Any {
    val cpsSet = cps.map { it.absolute() }.toSet()
    val cached = realmCache[cpsSet]
    val realm = if (cached != null) cached else {
      realmIdCounter += 1
      val newRealm = baseRealm.createChildRealm("bibix-realm-$realmIdCounter")

      cps.forEach {
        newRealm.addURL(it.absolute().toUri().toURL())
      }
      realmCache[cpsSet] = newRealm
      newRealm
    }
    val cls = realm.loadClass(className)
    return cls.getDeclaredConstructor().newInstance()
  }

  fun prepareClassPathsForPlugin(classPkg: ClassPkg): List<Path> {
    // bibix.base 와 및 kotlin sdk 관련 classpath 들은 cpInstance의 것을 사용하지 *않고* bibix runtime의 것을 사용해야 한다
    // 그래서 classPkg에서 그 부분은 빼고 classpath 목록 반환
    // 그래서 ClassPaths가 아니고 ClassPkg를 받아야 함
    val cpsMap = ResolveClassPkgs.cpsMap(listOf(classPkg))
      // TODo bibix.base를 maven artifact로 넣어서 필터링하는게 나을듯
      .filterNot { it.key is LocalLib && (it.key as LocalLib).path.fileName.name.startsWith("bibix-base-") }
    val mavenVersions = ResolveClassPkgs.mavenArtifactVersionsToUse(listOf(classPkg))
      .filterNot {
        (it.key.group == "org.jetbrains.kotlin" && it.key.artifact == "kotlin-stdlib") ||
          (it.key.group == "org.jetbrains.kotlin" && it.key.artifact == "kotlin-stdlib-jdk7") ||
          (it.key.group == "org.jetbrains.kotlin" && it.key.artifact == "kotlin-stdlib-jdk8") ||
          (it.key.group == "org.jetbrains.kotlin" && it.key.artifact == "kotlin-stdlib-common") ||
          (it.key.group == "org.jetbrains.kotlin" && it.key.artifact == "kotlin-reflect") ||
          (it.key.group == "org.jetbrains.kotlinx" && it.key.artifact == "kotlinx-coroutines-core") ||
          (it.key.group == "org.jetbrains.kotlinx" && it.key.artifact == "kotlinx-coroutines-jdk7") ||
          (it.key.group == "org.jetbrains.kotlinx" && it.key.artifact == "kotlinx-coroutines-jdk8")
      }

    val packs =
      cpsMap.filter { it.key !is MavenDep } + mavenVersions.values.associateWith { cpsMap[it] }
    return packs.mapNotNull { it.value?.toPaths() }.flatten().map { it.absolute() }.distinct()
  }
}
