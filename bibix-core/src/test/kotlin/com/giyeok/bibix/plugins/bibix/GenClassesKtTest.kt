package com.giyeok.bibix.plugins.bibix

import com.giyeok.bibix.base.CName
import com.giyeok.bibix.base.FileValue
import com.giyeok.bibix.base.MainSourceId
import com.giyeok.bibix.frontend.BuildFrontend
import com.google.common.jimfs.Jimfs
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.io.path.writeText

class GenClassesKtTest {
  @Test
  fun test() {
    val fs = Jimfs.newFileSystem()
    fs.getPath("/build.bbx").writeText(
      """
        import bibix
        import jvm
        
        xxx = bibix.genClassesKt(
          types=[
            jvm.ClassPaths,
            jvm.ClassPkg,
            jvm.CpInfo,
            jvm.JarInfo,
            jvm.ClassesInfo,
            jvm.ClassOrigin,
            jvm.MavenDep,
            jvm.LocalBuilt,
            jvm.LocalLib,
          ],
          packageName="com.giyeok.bibix.test",
          fileName="JvmTypes.kt",
        )
      """.trimIndent()
    )
    val frontend = BuildFrontend(fs.getPath("/"))
    val target = CName(MainSourceId, "xxx")
    val results = frontend.runTargets("build", listOf(target))

    val targetResult = results.getValue(target) as FileValue
    assertThat(targetResult.file.name).isEqualTo("JvmTypes.kt")
    val generated = targetResult.file.readText()

    assertThat(generated.trim()).isEqualTo(
      """
        package com.giyeok.bibix.test
        
        data class ClassPaths(
          val cps: List<Path>,
          val res: List<Path>,
        ) {
          companion object {
            fun fromBibix(value: BibixValue): ClassPaths {
              value as ClassInstanceValue
              check(value.className.tokens == listOf("ClassPaths"))
              val body = value.value as NamedTupleValue
              val cps = (body.pairs[0].second as SetValue).values.map { (it as PathValue).path }
              val res = (body.pairs[1].second as SetValue).values.map { (it as PathValue).path }
              return ClassPaths(cps, res)
            }
          }
          fun toBibix() = NClassInstanceValue(
            "jvm.ClassPaths",
            NamedTupleValue(
              "cps" to SetValue(cps.map { PathValue(it) }),
              "res" to SetValue(res.map { PathValue(it) }),
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
              check(value.className.tokens == listOf("ClassPkg"))
              val body = value.value as NamedTupleValue
              val origin = ClassOrigin.fromBibix(body.pairs[0].second)
              val cpinfo = CpInfo.fromBibix(body.pairs[1].second)
              val deps = (body.pairs[2].second as SetValue).values.map { ClassPkg.fromBibix(it) }
              return ClassPkg(origin, cpinfo, deps)
            }
          }
          fun toBibix() = NClassInstanceValue(
            "jvm.ClassPkg",
            NamedTupleValue(
              "origin" to origin.toBibix(),
              "cpinfo" to cpinfo.toBibix(),
              "deps" to SetValue(deps.map { it.toBibix() }),
            )
          )
        }
        
        sealed class CpInfo
        
        data class JarInfo(
          val jar: Path,
          val sourceJar: Path?,
        ): CpInfo() {
          companion object {
            fun fromBibix(value: BibixValue): JarInfo {
              value as ClassInstanceValue
              check(value.className.tokens == listOf("JarInfo"))
              val body = value.value as NamedTupleValue
              val jar = (body.pairs[0].second as FileValue).file
              val sourceJar = body.pairs[1].second.let { if (it == NoneValue) null else (it as FileValue).file }
              return JarInfo(jar, sourceJar)
            }
          }
          fun toBibix() = NClassInstanceValue(
            "jvm.JarInfo",
            NamedTupleValue(
              "jar" to FileValue(jar),
              "sourceJar" to sourceJar?.let { FileValue(it) },
            )
          )
        }
        
        data class ClassesInfo(
          val classDirs: List<Path>,
          val resourceDirs: List<Path>,
          val srcs: List<Path>?,
        ): CpInfo() {
          companion object {
            fun fromBibix(value: BibixValue): ClassesInfo {
              value as ClassInstanceValue
              check(value.className.tokens == listOf("ClassesInfo"))
              val body = value.value as NamedTupleValue
              val classDirs = (body.pairs[0].second as SetValue).values.map { (it as DirectoryValue).directory }
              val resourceDirs = (body.pairs[1].second as SetValue).values.map { (it as DirectoryValue).directory }
              val srcs = body.pairs[2].second.let { if (it == NoneValue) null else (it as SetValue).values.map { (it as FileValue).file } }
              return ClassesInfo(classDirs, resourceDirs, srcs)
            }
          }
          fun toBibix() = NClassInstanceValue(
            "jvm.ClassesInfo",
            NamedTupleValue(
              "classDirs" to SetValue(classDirs.map { DirectoryValue(it) }),
              "resDirs" to SetValue(resourceDirs.map { DirectoryValue(it) }),
              "srcs" to srcs?.let { SetValue(it.map { FileValue(it) }) },
            )
          )
        }
        
        sealed class ClassOrigin
        
        data class MavenDep(
          val repo: String,
          val group: String,
          val artifact: String,
          val version: String,
        ): ClassOrigin() {
          companion object {
            fun fromBibix(value: BibixValue): MavenDep {
              value as ClassInstanceValue
              check(value.className.tokens == listOf("MavenDep"))
              val body = value.value as NamedTupleValue
              val repo = (body.pairs[0].second as StringValue).value
              val group = (body.pairs[1].second as StringValue).value
              val artifact = (body.pairs[2].second as StringValue).value
              val version = (body.pairs[3].second as StringValue).value
              return MavenDep(repo, group, artifact, version)
            }
          }
          fun toBibix() = NClassInstanceValue(
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
        ): ClassOrigin() {
          companion object {
            fun fromBibix(value: BibixValue): LocalBuilt {
              value as ClassInstanceValue
              check(value.className.tokens == listOf("LocalBuilt"))
              val body = value.value as NamedTupleValue
              val desc = (body.pairs[0].second as StringValue).value
              return LocalBuilt(desc)
            }
          }
          fun toBibix() = NClassInstanceValue(
            "jvm.LocalBuilt",
            NamedTupleValue(
              "desc" to StringValue(desc),
            )
          )
        }
        
        data class LocalLib(
          val path: Path,
        ): ClassOrigin() {
          companion object {
            fun fromBibix(value: BibixValue): LocalLib {
              value as ClassInstanceValue
              check(value.className.tokens == listOf("LocalLib"))
              val body = value.value as NamedTupleValue
              val path = (body.pairs[0].second as PathValue).path
              return LocalLib(path)
            }
          }
          fun toBibix() = NClassInstanceValue(
            "jvm.LocalLib",
            NamedTupleValue(
              "path" to PathValue(path),
            )
          )
        }
        """.trimIndent()
    )
  }
}
