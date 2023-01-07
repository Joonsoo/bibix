package com.giyeok.bibix.plugins.jvm

import com.giyeok.bibix.base.*
import java.nio.file.Path

data class ClassPaths(
  val cps: List<Path>,
) {
  companion object {
    fun fromBibix(value: BibixValue): ClassPaths {
      value as ClassInstanceValue
//      check(value.className.tokens == listOf("ClassPaths"))
      return ClassPaths(
        cps = (value["cps"]!! as SetValue).values.map { (it as PathValue).path },
      )
    }
  }

  fun toBibix(): NClassInstanceValue = NClassInstanceValue(
    "ClassPaths",
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
      value as ClassInstanceValue
//      check(value.className.tokens == listOf("ClassPkg"))
      return ClassPkg(
        origin = ClassOrigin.fromBibix(value["origin"]!!),
        cpinfo = CpInfo.fromBibix(value["cpinfo"]!!),
        deps = (value["deps"]!! as SetValue).values.map { ClassPkg.fromBibix(it) },
      )
    }
  }

  fun toBibix(): NClassInstanceValue = NClassInstanceValue(
    "ClassPkg",
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
      value as ClassInstanceValue
      return when (value.className) {
        "JarInfo" -> JarInfo.fromBibix(value)
        "ClassesInfo" -> ClassesInfo.fromBibix(value)
        else -> throw IllegalStateException("Unknown subclass of CpInfo: ${value.className}")
      }
    }
  }

  abstract fun toBibix(): NClassInstanceValue
}

data class JarInfo(
  val jar: Path,
  val sourceJar: Path?,
) : CpInfo() {
  companion object {
    fun fromBibix(value: BibixValue): JarInfo {
      value as ClassInstanceValue
//      check(value.className.tokens == listOf("JarInfo"))
      return JarInfo(
        jar = (value["jar"]!! as FileValue).file,
        sourceJar = value["sourceJar"]?.let { (it as FileValue).file },
      )
    }
  }

  override fun toBibix(): NClassInstanceValue = NClassInstanceValue(
    "JarInfo",
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
) : CpInfo() {
  companion object {
    fun fromBibix(value: BibixValue): ClassesInfo {
      value as ClassInstanceValue
//      check(value.className.tokens == listOf("ClassesInfo"))
      return ClassesInfo(
        classDirs = (value["classDirs"]!! as SetValue).values.map { (it as DirectoryValue).directory },
        resDirs = (value["resDirs"]!! as SetValue).values.map { (it as DirectoryValue).directory },
        srcs = value["srcs"]?.let { (it as SetValue).values.map { (it as FileValue).file } },
      )
    }
  }

  override fun toBibix(): NClassInstanceValue = NClassInstanceValue(
    "ClassesInfo",
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
      value as ClassInstanceValue
      return when (value.className) {
        "MavenDep" -> MavenDep.fromBibix(value)
        "LocalLib" -> LocalLib.fromBibix(value)
        "LocalBuilt" -> LocalBuilt.fromBibix(value)
        else -> throw IllegalStateException("Unknown subclass of ClassOrigin: ${value.className}")
      }
    }
  }

  abstract fun toBibix(): NClassInstanceValue
}

data class MavenDep(
  val repo: String,
  val group: String,
  val artifact: String,
  val version: String,
) : ClassOrigin() {
  companion object {
    fun fromBibix(value: BibixValue): MavenDep {
      value as ClassInstanceValue
//      check(value.className.tokens == listOf("MavenDep"))
      return MavenDep(
        repo = (value["repo"]!! as StringValue).value,
        group = (value["group"]!! as StringValue).value,
        artifact = (value["artifact"]!! as StringValue).value,
        version = (value["version"]!! as StringValue).value,
      )
    }
  }

  override fun toBibix(): NClassInstanceValue = NClassInstanceValue(
    "MavenDep",
    mapOf(
      "repo" to StringValue(this.repo),
      "group" to StringValue(this.group),
      "artifact" to StringValue(this.artifact),
      "version" to StringValue(this.version),
    )
  )
}

data class LocalBuilt(
  val objHash: String,
  val builderName: String,
) : ClassOrigin() {
  companion object {
    fun fromBibix(value: BibixValue): LocalBuilt {
      value as ClassInstanceValue
//      check(value.className.tokens == listOf("LocalBuilt"))
      return LocalBuilt(
        objHash = (value["objHash"]!! as StringValue).value,
        builderName = (value["builderName"]!! as StringValue).value,
      )
    }
  }

  override fun toBibix(): NClassInstanceValue = NClassInstanceValue(
    "LocalBuilt",
    mapOf(
      "objHash" to StringValue(this.objHash),
      "builderName" to StringValue(this.builderName),
    )
  )
}

data class LocalLib(
  val path: Path,
) : ClassOrigin() {
  companion object {
    fun fromBibix(value: BibixValue): LocalLib {
      value as ClassInstanceValue
//      check(value.className.tokens == listOf("LocalLib"))
      return LocalLib(
        path = (value["path"]!! as PathValue).path,
      )
    }
  }

  override fun toBibix(): NClassInstanceValue = NClassInstanceValue(
    "LocalLib",
    mapOf(
      "path" to PathValue(this.path),
    )
  )
}
