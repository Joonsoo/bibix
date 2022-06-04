package com.giyeok.bibix.plugins.scala

import com.giyeok.bibix.base.*
import java.nio.file.Path

data class ClassPaths(val cps: List<Path>) {
  companion object {
    fun fromBibix(value: BibixValue): ClassPaths {
      value as DataClassInstanceValue
      check(value.className.tokens == listOf("ClassPaths"))
      val body = (value["cps"] as SetValue).values.map { (it as PathValue).path }
      return ClassPaths(body)
    }
  }

  fun toBibix() = NDataClassInstanceValue(
    "jvm.ClassPaths", mapOf(
      "cps" to SetValue(cps.map { PathValue(it) })
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
      val origin = ClassOrigin.fromBibix(value["origin"]!!)
      val cpinfo = CpInfo.fromBibix(value["cpinfo"]!!)
      val deps = (value["deps"] as SetValue).values.map { ClassPkg.fromBibix(it) }
      return ClassPkg(origin, cpinfo, deps)
    }
  }

  fun toBibix(): NDataClassInstanceValue = NDataClassInstanceValue(
    "jvm.ClassPkg",
    mapOf(
      "origin" to origin.toBibix(),
      "cpinfo" to cpinfo.toBibix(),
      "deps" to SetValue(deps.map { it.toBibix() }),
    )
  )
}

sealed class CpInfo {
  abstract fun toBibix(): BibixValue

  companion object {
    fun fromBibix(value: BibixValue): CpInfo {
      value as SuperClassInstanceValue
      return when (value.className.tokens) {
        listOf("JarInfo") -> JarInfo.fromBibix(value.value)
        listOf("ClassesInfo") -> ClassesInfo.fromBibix(value.value)
        else -> TODO()
      }
    }
  }
}

data class JarInfo(
  val jar: Path,
  val sourceJar: Path?,
) : CpInfo() {
  companion object {
    fun fromBibix(value: BibixValue): JarInfo {
      value as DataClassInstanceValue
      check(value.className.tokens == listOf("JarInfo"))
      val jar = (value.fieldValues.getValue("jar") as FileValue).file
      val sourceJar = value.fieldValues["sourceJar"]?.let { (it as FileValue).file }
      return JarInfo(jar, sourceJar)
    }
  }

  override fun toBibix() = NDataClassInstanceValue(
    "jvm.JarInfo",
    mapOf(
      "jar" to FileValue(jar),
      "sourceJar" to (sourceJar?.let { FileValue(it) } ?: NoneValue),
    )
  )
}

data class ClassesInfo(
  val classDirs: List<Path>,
  val resourceDirs: List<Path>,
  val srcs: List<Path>?,
) : CpInfo() {
  companion object {
    fun fromBibix(value: BibixValue): ClassesInfo {
      value as DataClassInstanceValue
      check(value.className.tokens == listOf("ClassesInfo"))
      val classDirs = (value.fieldValues.getValue("classDirs") as SetValue).values.map {
        (it as DirectoryValue).directory
      }
      val resourceDirs =
        (value.fieldValues.getValue("resDirs") as SetValue).values.map { (it as DirectoryValue).directory }
      val srcs =
        value.fieldValues["srcs"]?.let { (it as SetValue).values.map { (it as FileValue).file } }
      return ClassesInfo(classDirs, resourceDirs, srcs)
    }
  }

  override fun toBibix() = NDataClassInstanceValue(
    "jvm.ClassesInfo",
    mapOf(
      "classDirs" to SetValue(classDirs.map { DirectoryValue(it) }),
      "resDirs" to SetValue(resourceDirs.map { DirectoryValue(it) }),
      "srcs" to (srcs?.let { SetValue(it.map { FileValue(it) }) } ?: NoneValue),
    )
  )
}

sealed class ClassOrigin {
  abstract fun toBibix(): BibixValue

  companion object {
    fun fromBibix(value: BibixValue): ClassOrigin {
      value as SuperClassInstanceValue
      return when (value.className.tokens) {
        listOf("MavenDep") -> MavenDep.fromBibix(value.value)
        listOf("LocalBuilt") -> LocalBuilt.fromBibix(value.value)
        listOf("LocalLib") -> LocalLib.fromBibix(value.value)
        else -> TODO()
      }
    }
  }
}

data class MavenDep(
  val repo: String,
  val group: String,
  val artifact: String,
  val version: String,
) : ClassOrigin() {
  companion object {
    fun fromBibix(value: BibixValue): MavenDep {
      value as DataClassInstanceValue
      check(value.className.tokens == listOf("MavenDep"))
      val repo = (value.fieldValues.getValue("repo") as StringValue).value
      val group = (value.fieldValues.getValue("group") as StringValue).value
      val artifact = (value.fieldValues.getValue("artifact") as StringValue).value
      val version = (value.fieldValues.getValue("version") as StringValue).value
      return MavenDep(repo, group, artifact, version)
    }
  }

  override fun toBibix() = NDataClassInstanceValue(
    "jvm.MavenDep",
    mapOf(
      "repo" to StringValue(repo),
      "group" to StringValue(group),
      "artifact" to StringValue(artifact),
      "version" to StringValue(version),
    )
  )
}

data class LocalBuilt(
  val desc: String,
) : ClassOrigin() {
  companion object {
    fun fromBibix(value: BibixValue): LocalBuilt {
      value as DataClassInstanceValue
      check(value.className.tokens == listOf("LocalBuilt"))
      val desc = (value.fieldValues.getValue("desc") as StringValue).value
      return LocalBuilt(desc)
    }
  }

  override fun toBibix() = NDataClassInstanceValue(
    "jvm.LocalBuilt",
    mapOf(
      "desc" to StringValue(desc),
    )
  )
}

data class LocalLib(
  val path: Path,
) : ClassOrigin() {
  companion object {
    fun fromBibix(value: BibixValue): LocalLib {
      value as DataClassInstanceValue
      check(value.className.tokens == listOf("LocalLib"))
      val path = (value.fieldValues.getValue("path") as PathValue).path
      return LocalLib(path)
    }
  }

  override fun toBibix() = NDataClassInstanceValue(
    "jvm.LocalLib",
    mapOf(
      "path" to PathValue(path),
    )
  )
}
