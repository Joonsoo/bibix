package com.giyeok.bibix.plugins.bibix

import com.giyeok.bibix.base.*
import com.giyeok.bibix.frontend.BuildFrontend
import com.google.common.jimfs.Jimfs
import com.google.common.truth.Truth
import org.junit.jupiter.api.Test
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.io.path.writeText

class GenRuleImplTemplateKtTest {
  @Test
  fun test() {
    val fs = Jimfs.newFileSystem()
    fs.getPath("/build.bbx").writeText(
      """
        import bibix
        import jvm
        import java
        
        gen = bibix.genRuleImplTemplateKt(
          rules = [rule1, rule2, rule3],
          types = [
            jvm.ClassPkg,
            jvm.ClassOrigin,
            jvm.MavenDep,
            jvm.LocalLib,
            jvm.LocalBuilt,
            jvm.CpInfo,
            jvm.JarInfo,
            jvm.ClassesInfo,
          ],
          implName = "com.giyeok.bibix.test.TestImpl",
          implInterfaceName = "com.giyeok.bibix.test.TestInterface",
        )
        
        impl = java.library(
          srcs = (gen as list<file>) + glob("src/**/*.kt"),
          deps = [],
        )
        
        def rule1(
          deps: set<jvm.ClassPkg>,
        ): set<file> = impl:com.giyeok.bibix.test.Test:rule1
        
        def rule2(
          deps: set<jvm.ClassPkg>,
        ): set<file> = impl:com.giyeok.bibix.test.Test:rule2

        def rule3(
          deps: set<jvm.ClassPkg>,
        ): set<file> = impl:com.giyeok.bibix.test.Test:rule3
      """.trimIndent()
    )
    val frontend = BuildFrontend(fs.getPath("/"))
    val target = CName(MainSourceId, "gen")
    val results = frontend.runTargets("build", listOf(target))

    val targetResult = results.getValue(target) as DataClassInstanceValue
    val implClassFile = (targetResult["implClass"] as FileValue).file
    val interfaceClassFile = (targetResult["interfaceClass"] as FileValue).file
    Truth.assertThat(implClassFile.name).isEqualTo("Test.kt")
    Truth.assertThat(interfaceClassFile.name).isEqualTo("TestInterface.kt")

    val dollar = "$"
    Truth.assertThat(implClassFile.readText().trim()).isEqualTo(
      """
        package com.giyeok.bibix.test

        import com.giyeok.bibix.base.*
        import java.nio.file.Path

        class Test(val impl: TestInterface) {
          constructor() : this(TestImpl())

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

          sealed class ClassOrigin {
            companion object {
              fun fromBibix(value: BibixValue): ClassOrigin {
                value as DataClassInstanceValue
                return when (value.className.tokens) {
                  listOf("MavenDep") -> MavenDep.fromBibix(value)
                  listOf("LocalLib") -> LocalLib.fromBibix(value)
                  listOf("LocalBuilt") -> LocalBuilt.fromBibix(value)
                  else -> throw IllegalStateException("Unknown subclass of ClassOrigin: $dollar{value.className}")
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

          data class LocalBuilt(
            val objHash: String,
            val builderName: String,
          ): ClassOrigin() {
            companion object {
              fun fromBibix(value: BibixValue): LocalBuilt {
                value as DataClassInstanceValue
                check(value.className.tokens == listOf("LocalBuilt"))
                return LocalBuilt(
                  objHash=(value["objHash"]!! as StringValue).value,
                  builderName=(value["builderName"]!! as StringValue).value,
                )
              }
            }
            override fun toBibix(): NDataClassInstanceValue = NDataClassInstanceValue(
              "jvm.LocalBuilt",
              mapOf(
                "objHash" to StringValue(this.objHash),
                "builderName" to StringValue(this.builderName),
              )
            )
          }

          sealed class CpInfo {
            companion object {
              fun fromBibix(value: BibixValue): CpInfo {
                value as DataClassInstanceValue
                return when (value.className.tokens) {
                  listOf("JarInfo") -> JarInfo.fromBibix(value)
                  listOf("ClassesInfo") -> ClassesInfo.fromBibix(value)
                  else -> throw IllegalStateException("Unknown subclass of CpInfo: $dollar{value.className}")
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

          fun rule1(context: BuildContext): BuildRuleReturn {
            val deps = (context.arguments.getValue("deps") as SetValue).values.map { ClassPkg.fromBibix(it) }
            return impl.rule1(context, deps)
          }

          fun rule2(context: BuildContext): BuildRuleReturn {
            val deps = (context.arguments.getValue("deps") as SetValue).values.map { ClassPkg.fromBibix(it) }
            return impl.rule2(context, deps)
          }

          fun rule3(context: BuildContext): BuildRuleReturn {
            val deps = (context.arguments.getValue("deps") as SetValue).values.map { ClassPkg.fromBibix(it) }
            return impl.rule3(context, deps)
          }
        }
      """.trimIndent()
    )
    Truth.assertThat(interfaceClassFile.readText().trim()).isEqualTo(
      """
        package com.giyeok.bibix.test

        import com.giyeok.bibix.base.*
        import java.nio.file.Path
        import com.giyeok.bibix.test.Test.*

        interface TestInterface {
          fun rule1(
            context: BuildContext,
            deps: List<ClassPkg>,
          ): BuildRuleReturn

          fun rule2(
            context: BuildContext,
            deps: List<ClassPkg>,
          ): BuildRuleReturn

          fun rule3(
            context: BuildContext,
            deps: List<ClassPkg>,
          ): BuildRuleReturn
        }
      """.trimIndent()
    )
  }
}
