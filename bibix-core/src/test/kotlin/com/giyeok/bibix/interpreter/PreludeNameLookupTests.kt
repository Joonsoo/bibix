package com.giyeok.bibix.interpreter

import com.giyeok.bibix.base.BibixValue
import com.giyeok.bibix.base.BuildContext
import com.giyeok.bibix.base.StringValue
import com.giyeok.bibix.plugins.Classes
import com.giyeok.bibix.plugins.PreloadedPlugin
import com.google.common.jimfs.Jimfs
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.io.path.writeText

class PreludeNameLookupTests {
  @Test
  fun test(): Unit = runBlocking {
    val fs = Jimfs.newFileSystem()

    val script = """
      xx = preludeRule("Joonsoo")
      yy = preludeValue
    """.trimIndent()
    fs.getPath("/build.bbx").writeText(script)

    val prelude = PreloadedPlugin.fromScript(
      "com.giyeok.bibix.prelude",
      """
        def preludeRule(hello: string): string
          = native:com.giyeok.bibix.interpreter.TestPreludePlugin
          
        preludeValue = "Hello world!"
      """.trimIndent(),
      Classes(TestPreludePlugin::class.java)
    )

    val interpreter = testInterpreter(fs, "/", mapOf(), preludePlugin = prelude)

    assertThat(interpreter.userBuildRequest("xx")).isEqualTo(StringValue("Joonsoo, Great!"))
    assertThat(interpreter.userBuildRequest("yy")).isEqualTo(StringValue("Hello world!"))
  }
}

class TestPreludePlugin {
  fun build(context: BuildContext): BibixValue {
    val hello = (context.arguments.getValue("hello") as StringValue).value
    return StringValue("$hello, Great!")
  }
}
