package com.giyeok.bibix.plugins

import com.giyeok.bibix.base.*
import java.nio.file.Path

data class ClassPaths(val cps: List<Path>) {
  companion object {
    fun fromBibix(value: BibixValue): ClassPaths {
      value as DataClassInstanceValue
      check(value.className.tokens == listOf("ClassPaths"))
      val body = (value.value as SetValue).values.map { (it as PathValue).path }
      return ClassPaths(body)
    }
  }

  fun toBibix() = NDataClassInstanceValue("jvm.ClassPaths", SetValue(cps.map { PathValue(it) }))
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
      val body = value.value as NamedTupleValue
      val origin = ClassOrigin.fromBibix(body.pairs[0].second)
      val cpinfo = CpInfo.fromBibix(body.pairs[1].second)
      val deps = (body.pairs[2].second as SetValue).values.map { ClassPkg.fromBibix(it) }
      return ClassPkg(origin, cpinfo, deps)
    }
  }

  fun toBibix(): NDataClassInstanceValue = NDataClassInstanceValue(
    "jvm.ClassPkg",
    NamedTupleValue(
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
      value as DataClassInstanceValue
      val vv = value.value as DataClassInstanceValue
      return when (vv.className.tokens) {
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
      val body = value.value as NamedTupleValue
      val jar = (body.pairs[0].second as FileValue).file
      val sourceJar =
        body.pairs[1].second.let { if (it == NoneValue) null else (it as FileValue).file }
      return JarInfo(jar, sourceJar)
    }
  }

  override fun toBibix() = NDataClassInstanceValue(
    "jvm.JarInfo",
    NamedTupleValue(
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
      val body = value.value as NamedTupleValue
      val classDirs =
        (body.pairs[0].second as SetValue).values.map { (it as DirectoryValue).directory }
      val resourceDirs =
        (body.pairs[1].second as SetValue).values.map { (it as DirectoryValue).directory }
      val srcs =
        body.pairs[2].second.let { if (it == NoneValue) null else (it as SetValue).values.map { (it as FileValue).file } }
      return ClassesInfo(classDirs, resourceDirs, srcs)
    }
  }

  override fun toBibix() = NDataClassInstanceValue(
    "jvm.ClassesInfo",
    NamedTupleValue(
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
      value as DataClassInstanceValue
      val vv = value.value as DataClassInstanceValue
      return when (vv.className.tokens) {
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
      val body = value.value as NamedTupleValue
      val repo = (body.pairs[0].second as StringValue).value
      val group = (body.pairs[1].second as StringValue).value
      val artifact = (body.pairs[2].second as StringValue).value
      val version = (body.pairs[3].second as StringValue).value
      return MavenDep(repo, group, artifact, version)
    }
  }

  override fun toBibix() = NDataClassInstanceValue(
    "jvm.MavenDep",
    NamedTupleValue(
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
      val body = value.value as NamedTupleValue
      val desc = (body.pairs[0].second as StringValue).value
      return LocalBuilt(desc)
    }
  }

  override fun toBibix() = NDataClassInstanceValue(
    "jvm.LocalBuilt",
    NamedTupleValue(
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
      val body = value.value as NamedTupleValue
      val path = (body.pairs[0].second as PathValue).path
      return LocalLib(path)
    }
  }

  override fun toBibix() = NDataClassInstanceValue(
    "jvm.LocalLib",
    NamedTupleValue(
      "path" to PathValue(path),
    )
  )
}
