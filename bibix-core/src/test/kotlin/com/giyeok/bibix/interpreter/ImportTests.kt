package com.giyeok.bibix.interpreter

import com.giyeok.bibix.base.*
import com.giyeok.bibix.interpreter.name.ImportedSource
import com.giyeok.bibix.interpreter.name.NameLookupContext
import com.giyeok.bibix.plugins.Classes
import com.giyeok.bibix.plugins.PreloadedPlugin
import com.google.common.jimfs.Jimfs
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
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

    val repo = testRepo(fs)

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

    val interpreter = BibixInterpreter(
      BuildEnv(OS.Linux("", ""), Architecture.X86_64),
      mapOf("xyz" to xyzPlugin, "yza" to yzaPlugin),
      BibixProject(fs.getPath("/"), null),
      repo,
      mapOf()
    )

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
}

class TestWorldRule {
  fun build(context: BuildContext): BibixValue {
    return StringValue("World!")
  }
}
