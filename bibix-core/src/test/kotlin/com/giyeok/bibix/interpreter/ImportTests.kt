package com.giyeok.bibix.interpreter

import com.giyeok.bibix.base.*
import com.giyeok.bibix.interpreter.expr.ImportedSource
import com.giyeok.bibix.interpreter.expr.NameLookupContext
import com.giyeok.bibix.plugins.Classes
import com.giyeok.bibix.plugins.PreloadedPlugin
import com.google.common.jimfs.Jimfs
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.nio.file.Files
import kotlin.io.path.writeText

class ImportTests {
  @Test
  fun testPreloadedPlugins(): Unit = runBlocking {
    val fs = Jimfs.newFileSystem()

    val script = """
      import xyz
      import yza as azy
      from yza import planet
      from yza import satellite
      from git() import jeon
      import git() as joonsoo
      
      abc = xyz.hello
      
      bcd = xyz.world
      
      xxx = azy.planet
      yyy = planet
      zzz = satellite.moon
    """.trimIndent()
    fs.getPath("/build.bbx").writeText(script)

    val xyzPlugin = PreloadedPlugin.fromScript(
      "com.abc.xyz",
      """
        import yza
        
        hello = "good!"
        
        world = yza.planet
        
        def worldRule(): string = native:com.giyeok.bibix.interpreter.TestWorldRule
      """.trimIndent(),
      Classes(TestWorldRule::class.java),
    )

    val yzaPlugin = PreloadedPlugin.fromScript(
      "com.abc.yza",
      """
        package com.giyeok.yza
        
        planet = "earth"
        
        namespace satellite {
          moon = "moon"
        }
      """.trimIndent(),
      Classes()
    )

    val interpreter = testInterpreter(fs, "/", mapOf("xyz" to xyzPlugin, "yza" to yzaPlugin))

    assertThat(interpreter.userBuildRequest(listOf("abc"))).isEqualTo(StringValue("good!"))
    assertThat(interpreter.userBuildRequest(listOf("bcd"))).isEqualTo(StringValue("earth"))
    assertThat(interpreter.userBuildRequest(listOf("xxx"))).isEqualTo(StringValue("earth"))
    assertThat(interpreter.userBuildRequest(listOf("yyy"))).isEqualTo(StringValue("earth"))
    assertThat(interpreter.userBuildRequest(listOf("zzz"))).isEqualTo(StringValue("moon"))
    assertThat(interpreter.nameLookupTable.definitions.keys).containsExactly(
      CName(MainSourceId, "xyz"),
      CName(MainSourceId, "azy"),
      CName(MainSourceId, "planet"),
      CName(MainSourceId, "satellite"),
      CName(MainSourceId, "jeon"),
      CName(MainSourceId, "joonsoo"),
      CName(MainSourceId, "abc"),
      CName(MainSourceId, "bcd"),
      CName(MainSourceId, "xxx"),
      CName(MainSourceId, "yyy"),
      CName(MainSourceId, "zzz"),
      CName(PreloadedSourceId("xyz"), "yza"),
      CName(PreloadedSourceId("xyz"), "hello"),
      CName(PreloadedSourceId("xyz"), "world"),
      CName(PreloadedSourceId("xyz"), "worldRule"),
      CName(PreloadedSourceId("yza"), "planet"),
      CName(PreloadedSourceId("yza"), "satellite"),
      CName(PreloadedSourceId("yza"), "satellite", "moon"),
    )
    assertThat(interpreter.nameLookupTable.imports.keys).containsExactly(
      CName(MainSourceId, "xyz"),
      CName(MainSourceId, "azy"),
      CName(MainSourceId, "planet"),
      CName(MainSourceId, "satellite"),
      CName(PreloadedSourceId("xyz"), "yza"),
    )
    assertThat(interpreter.nameLookupTable.imports).containsAtLeast(
      CName(MainSourceId, "xyz"),
      ImportedSource.ImportedNames(NameLookupContext(PreloadedSourceId("xyz"), listOf())),
      CName(MainSourceId, "azy"),
      ImportedSource.ImportedNames(NameLookupContext(PreloadedSourceId("yza"), listOf())),
      CName(MainSourceId, "satellite"),
      ImportedSource.ImportedNames(
        NameLookupContext(PreloadedSourceId("yza"), listOf("satellite"))
      ),
      CName(PreloadedSourceId("xyz"), "yza"),
      ImportedSource.ImportedNames(NameLookupContext(PreloadedSourceId("yza"), listOf())),
    )
    assertThat(interpreter.nameLookupTable.imports[CName(MainSourceId, "planet")])
      .isInstanceOf(ImportedSource.ImportedDefinition::class.java)
    assertThat(interpreter.sourceManager.sourcePackageName).containsExactly(
      PreloadedSourceId("xyz"), "com.abc.xyz",
      PreloadedSourceId("yza"), "com.abc.yza",
    )
  }

  @Test
  fun testExternalPluginsByPath(): Unit = runBlocking {
    val fs = Jimfs.newFileSystem()

    val script = """
      import "subproject1" as xyz
      from "subproject1" import qqq
      from "subproject1" import qqq.universe as univ
      from "subproject1/subproject2" import earth

      xxx = xyz.planet
      hug = qqq.universe
      ddd = qqq.ccc
      uni = univ
      ear = earth
    """.trimIndent()
    fs.getPath("/build.bbx").writeText(script)

    val subproject1 = """
      package com.abc.xyz
      
      import "subproject2" as yyy
      planet = "earth!"
      
      namespace qqq {
        universe = "huge"
        ccc = yyy.earth
      }
    """.trimIndent()
    Files.createDirectory(fs.getPath("/subproject1"))
    fs.getPath("/subproject1/build.bbx").writeText(subproject1)

    val subproject2 = """
      earth = "has moon"
    """.trimIndent()
    Files.createDirectory(fs.getPath("/subproject1/subproject2"))
    fs.getPath("/subproject1/subproject2/build.bbx").writeText(subproject2)

    val interpreter = testInterpreter(fs, "/", mapOf())

    assertThat(interpreter.userBuildRequest(listOf("xxx"))).isEqualTo(StringValue("earth!"))
    assertThat(interpreter.userBuildRequest(listOf("hug"))).isEqualTo(StringValue("huge"))
    assertThat(interpreter.userBuildRequest(listOf("ddd"))).isEqualTo(StringValue("has moon"))
    assertThat(interpreter.userBuildRequest(listOf("uni"))).isEqualTo(StringValue("huge"))
    assertThat(interpreter.userBuildRequest(listOf("ear"))).isEqualTo(StringValue("has moon"))
    assertThat(interpreter.nameLookupTable.definitions.keys).containsExactly(
      CName(MainSourceId, "xyz"),
      CName(MainSourceId, "qqq"),
      CName(MainSourceId, "univ"),
      CName(MainSourceId, "earth"),
      CName(MainSourceId, "xxx"),
      CName(MainSourceId, "hug"),
      CName(MainSourceId, "ddd"),
      CName(MainSourceId, "uni"),
      CName(MainSourceId, "ear"),
      CName(ExternSourceId(1), "yyy"),
      CName(ExternSourceId(1), "planet"),
      CName(ExternSourceId(1), "qqq"),
      CName(ExternSourceId(1), "qqq", "universe"),
      CName(ExternSourceId(1), "qqq", "ccc"),
      CName(ExternSourceId(2), "earth"),
    )
    assertThat(interpreter.nameLookupTable.imports.keys).containsExactly(
      CName(MainSourceId, "xyz"),
      CName(MainSourceId, "qqq"),
      CName(MainSourceId, "univ"),
      CName(MainSourceId, "earth"),
      CName(ExternSourceId(1), "yyy"),
    )
    assertThat(interpreter.nameLookupTable.imports).containsAtLeast(
      CName(MainSourceId, "xyz"),
      ImportedSource.ImportedNames(NameLookupContext(ExternSourceId(1), listOf())),
      CName(MainSourceId, "qqq"),
      ImportedSource.ImportedNames(NameLookupContext(ExternSourceId(1), listOf("qqq"))),
      CName(ExternSourceId(1), "yyy"),
      ImportedSource.ImportedNames(NameLookupContext(ExternSourceId(2), listOf())),
    )
    assertThat(interpreter.nameLookupTable.imports[CName(MainSourceId, "univ")])
      .isInstanceOf(ImportedSource.ImportedDefinition::class.java)
    assertThat(interpreter.nameLookupTable.imports[CName(MainSourceId, "earth")])
      .isInstanceOf(ImportedSource.ImportedDefinition::class.java)
    assertThat(interpreter.sourceManager.sourcePackageName).containsExactly(
      ExternSourceId(1), "com.abc.xyz",
    )
  }

  @Test
  fun testImportsInNamespace(): Unit = runBlocking {
    val fs = Jimfs.newFileSystem()

    val script = """
      namespace aaa {
        import xyz
        abc = xyz.hello
      }
      
      bcd = aaa.xyz.world
    """.trimIndent()
    fs.getPath("/build.bbx").writeText(script)

    val xyzPlugin = PreloadedPlugin.fromScript(
      "com.abc.xyz",
      """
        import yza
        
        hello = "good!"
        
        world = "hi!"
        
        def worldRule(): string = native:com.giyeok.bibix.interpreter.TestWorldRule
      """.trimIndent(),
      Classes(TestWorldRule::class.java),
    )

    val interpreter = testInterpreter(fs, "/", mapOf("xyz" to xyzPlugin))

    assertThat(interpreter.userBuildRequest(listOf("aaa", "abc"))).isEqualTo(StringValue("good!"))
    assertThat(interpreter.userBuildRequest(listOf("bcd"))).isEqualTo(StringValue("hi!"))
  }
}

class TestWorldRule {
  fun build(context: BuildContext): BibixValue {
    return StringValue("World!")
  }
}
