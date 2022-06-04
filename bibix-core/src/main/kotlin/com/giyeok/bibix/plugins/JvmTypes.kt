package com.giyeok.bibix.plugins

import com.giyeok.bibix.base.*
import java.nio.file.Path

data class ClassPaths(
  val cps: List<Path>,
) {
  companion object {
    fun fromBibix(value: BibixValue): ClassPaths {
      value as DataClassInstanceValue
      check(value.className.tokens == listOf("ClassPaths"))
      return ClassPaths(
        cps=(value["cps"]!! as SetValue).values.map { (it as PathValue).path },
      )
    }
  }
  fun toBibix(): NDataClassInstanceValue = NDataClassInstanceValue(
    "jvm.ClassPaths",
    mapOf(
      "cps" to SetValue(this.cps.map { PathValue(it) }),
    )
  )
}

data class ClassPkg(
  val origin: ClassOrigin,
  val cpinfo: CpInfo,
  val deps: List<ClassPkg>,
) {
  companion object {
    fun fromBibix(value: BibixValue): ClassPkg {
      value as DataClassInstanceValue
      check(value.className.tokens == listOf("ClassPkg"))
      return ClassPkg(
        origin=ClassOrigin.fromBibix(value["origin"]!!),
        cpinfo=CpInfo.fromBibix(value["cpinfo"]!!),
        deps=(value["deps"]!! as SetValue).values.map { ClassPkg.fromBibix(it) },
      )
    }
  }
  fun toBibix(): NDataClassInstanceValue = NDataClassInstanceValue(
    "jvm.ClassPkg",
    mapOf(
      "origin" to this.origin.toBibix(),
      "cpinfo" to this.cpinfo.toBibix(),
      "deps" to SetValue(this.deps.map { it.toBibix() }),
    )
  )
}

sealed class CpInfo {
  companion object {
    fun fromBibix(value: BibixValue): CpInfo {
      value as DataClassInstanceValue
      check(value.className.tokens == listOf("CpInfo"))
      return when (value.className.tokens) {
        listOf("JarInfo") -> JarInfo.fromBibix(value)
        listOf("ClassesInfo") -> ClassesInfo.fromBibix(value)
        else -> throw IllegalStateException("Unknown subclass of CpInfo: ${value.className}")
      }
    }
  }
  abstract fun toBibix(): NDataClassInstanceValue
}

data class JarInfo(
  val jar: Path,
  val sourceJar: Path?,
): CpInfo() {
  companion object {
    fun fromBibix(value: BibixValue): JarInfo {
      value as DataClassInstanceValue
      check(value.className.tokens == listOf("JarInfo"))
      return JarInfo(
        jar=(value["jar"]!! as FileValue).file,
        sourceJar=value["sourceJar"]?.let { (it as FileValue).file },
      )
    }
  }
  override fun toBibix(): NDataClassInstanceValue = NDataClassInstanceValue(
    "jvm.JarInfo",
    listOfNotNull(
      "jar" to FileValue(this.jar),
      this.sourceJar?.let { "sourceJar" to FileValue(it) },
    ).toMap()
  )
}

data class ClassesInfo(
  val classDirs: List<Path>,
  val resDirs: List<Path>,
  val srcs: List<Path>?,
): CpInfo() {
  companion object {
    fun fromBibix(value: BibixValue): ClassesInfo {
      value as DataClassInstanceValue
      check(value.className.tokens == listOf("ClassesInfo"))
      return ClassesInfo(
        classDirs=(value["classDirs"]!! as SetValue).values.map { (it as DirectoryValue).directory },
        resDirs=(value["resDirs"]!! as SetValue).values.map { (it as DirectoryValue).directory },
        srcs=value["srcs"]?.let { (it as SetValue).values.map { (it as FileValue).file } },
      )
    }
  }
  override fun toBibix(): NDataClassInstanceValue = NDataClassInstanceValue(
    "jvm.ClassesInfo",
    listOfNotNull(
      "classDirs" to SetValue(this.classDirs.map { DirectoryValue(it) }),
      "resDirs" to SetValue(this.resDirs.map { DirectoryValue(it) }),
      this.srcs?.let { "srcs" to SetValue(it.map { FileValue(it) }) },
    ).toMap()
  )
}

sealed class ClassOrigin {
  companion object {
    fun fromBibix(value: BibixValue): ClassOrigin {
      value as DataClassInstanceValue
      check(value.className.tokens == listOf("ClassOrigin"))
      return when (value.className.tokens) {
        listOf("MavenDep") -> MavenDep.fromBibix(value)
        listOf("LocalLib") -> LocalLib.fromBibix(value)
        listOf("LocalBuilt") -> LocalBuilt.fromBibix(value)
        else -> throw IllegalStateException("Unknown subclass of ClassOrigin: ${value.className}")
      }
    }
  }
  abstract fun toBibix(): NDataClassInstanceValue
}

data class MavenDep(
  val repo: String,
  val group: String,
  val artifact: String,
  val version: String,
): ClassOrigin() {
  companion object {
    fun fromBibix(value: BibixValue): MavenDep {
      value as DataClassInstanceValue
      check(value.className.tokens == listOf("MavenDep"))
      return MavenDep(
        repo=(value["repo"]!! as StringValue).value,
        group=(value["group"]!! as StringValue).value,
        artifact=(value["artifact"]!! as StringValue).value,
        version=(value["version"]!! as StringValue).value,
      )
    }
  }
  override fun toBibix(): NDataClassInstanceValue = NDataClassInstanceValue(
    "jvm.MavenDep",
    mapOf(
      "repo" to StringValue(this.repo),
      "group" to StringValue(this.group),
      "artifact" to StringValue(this.artifact),
      "version" to StringValue(this.version),
    )
  )
}

data class LocalBuilt(
  val desc: String,
): ClassOrigin() {
  companion object {
    fun fromBibix(value: BibixValue): LocalBuilt {
      value as DataClassInstanceValue
      check(value.className.tokens == listOf("LocalBuilt"))
      return LocalBuilt(
        desc=(value["desc"]!! as StringValue).value,
      )
    }
  }
  override fun toBibix(): NDataClassInstanceValue = NDataClassInstanceValue(
    "jvm.LocalBuilt",
    mapOf(
      "desc" to StringValue(this.desc),
    )
  )
}

data class LocalLib(
  val path: Path,
): ClassOrigin() {
  companion object {
    fun fromBibix(value: BibixValue): LocalLib {
      value as DataClassInstanceValue
      check(value.className.tokens == listOf("LocalLib"))
      return LocalLib(
        path=(value["path"]!! as PathValue).path,
      )
    }
  }
  override fun toBibix(): NDataClassInstanceValue = NDataClassInstanceValue(
    "jvm.LocalLib",
    mapOf(
      "path" to PathValue(this.path),
    )
  )
}
