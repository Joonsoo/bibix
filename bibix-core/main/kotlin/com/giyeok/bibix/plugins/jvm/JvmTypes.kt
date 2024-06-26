package com.giyeok.bibix.plugins.jvm

import com.giyeok.bibix.base.*
import java.nio.file.Path

data class ClassPaths(
  val cps: List<Path>,
  val runtimeCps: List<Path>,
) {
  companion object {
    fun fromBibix(value: BibixValue): ClassPaths {
      value as ClassInstanceValue
//      check(value.className.tokens == listOf("ClassPaths"))
      return ClassPaths(
        cps = (value["cps"]!! as SetValue).values.map { (it as PathValue).path },
        runtimeCps = (value["runtimeCps"]!! as SetValue).values.map { (it as PathValue).path },
      )
    }
  }

  fun toBibix(): ClassInstanceValue = ClassInstanceValue(
    "com.giyeok.bibix.plugins.jvm",
    "ClassPaths",
    mapOf(
      "cps" to SetValue(this.cps.map { PathValue(it) }),
      "runtimeCps" to SetValue(this.runtimeCps.map { PathValue(it) }),
    )
  )
}

data class ClassPkg(
  val origin: ClassOrigin,
  val cpinfo: CpInfo,
  val deps: List<ClassPkg>,
  val runtimeDeps: List<ClassPkg>,
  val nativeLibDirs: List<Path>,
) {
  companion object {
    fun fromBibix(value: BibixValue): ClassPkg {
      value as ClassInstanceValue
//      check(value.className.tokens == listOf("ClassPkg"))
      return ClassPkg(
        origin = ClassOrigin.fromBibix(value["origin"]!!),
        cpinfo = CpInfo.fromBibix(value["cpinfo"]!!),
        deps = (value["deps"]!! as SetValue).values.map { fromBibix(it) },
        runtimeDeps = (value["runtimeDeps"]!! as SetValue).values.map { fromBibix(it) },
        nativeLibDirs = (value["nativeLibDirs"]!! as SetValue).values.map { (it as DirectoryValue).directory },
      )
    }
  }

  fun toBibix(): ClassInstanceValue = ClassInstanceValue(
    "com.giyeok.bibix.plugins.jvm",
    "ClassPkg",
    mapOf(
      "origin" to this.origin.toBibix(),
      "cpinfo" to this.cpinfo.toBibix(),
      "deps" to SetValue(this.deps.map { it.toBibix() }),
      "runtimeDeps" to SetValue(this.runtimeDeps.map { it.toBibix() }),
      "nativeLibDirs" to SetValue(this.nativeLibDirs.map { DirectoryValue(it) }),
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

  abstract fun toBibix(): ClassInstanceValue
}

data class JarInfo(
  val jar: Path,
  val sourceJar: Path?,
): CpInfo() {
  companion object {
    fun fromBibix(value: BibixValue): JarInfo {
      value as ClassInstanceValue
//      check(value.className.tokens == listOf("JarInfo"))
      return JarInfo(
        jar = (value["jar"]!! as FileValue).file,
        sourceJar = value.getNullableFieldOf<FileValue>("sourceJar")?.file,
      )
    }
  }

  override fun toBibix(): ClassInstanceValue = ClassInstanceValue(
    "com.giyeok.bibix.plugins.jvm",
    "JarInfo",
    listOfNotNull(
      "jar" to FileValue(this.jar),
      "sourceJar" to (this.sourceJar?.let { FileValue(it) } ?: NoneValue),
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
      value as ClassInstanceValue
//      check(value.className.tokens == listOf("ClassesInfo"))
      return ClassesInfo(
        classDirs = (value["classDirs"]!! as SetValue).values.map { (it as DirectoryValue).directory },
        resDirs = (value["resDirs"]!! as SetValue).values.map { (it as DirectoryValue).directory },
        srcs = value["srcs"]!!.let { v -> if (v == NoneValue) null else (v as SetValue).values.map { (it as FileValue).file } },
      )
    }
  }

  override fun toBibix(): ClassInstanceValue = ClassInstanceValue(
    "com.giyeok.bibix.plugins.jvm",
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

  abstract fun toBibix(): ClassInstanceValue
}

data class MavenDep(
  val repo: String,
  val group: String,
  val artifact: String,
  val version: String,
  val classifier: String,
): ClassOrigin() {
  companion object {
    fun fromBibix(value: BibixValue): MavenDep {
      value as ClassInstanceValue
//      check(value.className.tokens == listOf("MavenDep"))
      return MavenDep(
        repo = (value["repo"]!! as StringValue).value,
        group = (value["group"]!! as StringValue).value,
        artifact = (value["artifact"]!! as StringValue).value,
        version = (value["version"]!! as StringValue).value,
        classifier = (value["classifier"] as StringValue).value,
      )
    }
  }

  override fun toBibix(): ClassInstanceValue = ClassInstanceValue(
    "com.giyeok.bibix.plugins.jvm",
    "MavenDep",
    mapOf(
      "repo" to StringValue(this.repo),
      "group" to StringValue(this.group),
      "artifact" to StringValue(this.artifact),
      "version" to StringValue(this.version),
      "classifier" to StringValue(this.classifier),
    )
  )
}

data class LocalBuilt(
  val objHash: String,
  val builderName: String,
): ClassOrigin() {
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

  override fun toBibix(): ClassInstanceValue = ClassInstanceValue(
    "com.giyeok.bibix.plugins.jvm",
    "LocalBuilt",
    mapOf(
      "objHash" to StringValue(this.objHash),
      "builderName" to StringValue(this.builderName),
    )
  )
}

data class LocalLib(
  val path: Path,
): ClassOrigin() {
  companion object {
    fun fromBibix(value: BibixValue): LocalLib {
      value as ClassInstanceValue
//      check(value.className.tokens == listOf("LocalLib"))
      return LocalLib(
        path = (value["path"]!! as PathValue).path,
      )
    }
  }

  override fun toBibix(): ClassInstanceValue = ClassInstanceValue(
    "com.giyeok.bibix.plugins.jvm",
    "LocalLib",
    mapOf(
      "path" to PathValue(this.path),
    )
  )
}
