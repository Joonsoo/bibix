package com.giyeok.bibix.plugins.prelude

import com.giyeok.bibix.base.StringValue
import com.giyeok.bibix.interpreter.testInterpreter
import com.giyeok.bibix.plugins.PluginInstanceProvider
import com.giyeok.bibix.plugins.PreloadedPlugin
import com.google.common.jimfs.Jimfs
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.io.path.writeText

class PreludeTests {
  @Test
  fun testDefaultImports(): Unit = runBlocking {
    val fs = Jimfs.newFileSystem()

    val script = """
      xx = abc.abcValue
    """.trimIndent()
    fs.getPath("/build.bbx").writeText(script)

    val abcPlugin = PreloadedPlugin.fromScript(
      "com.abc",
      """
        abcValue = "Hello world!"
      """.trimIndent(),
      PluginInstanceProvider()
    )

    val prelude = PreloadedPlugin.fromScript(
      "prelude",
      """
        import abc
      """.trimIndent(),
      PluginInstanceProvider(),
    )

    val interpreter =
      testInterpreter(fs, "/", mapOf("abc" to abcPlugin), preludePlugin = prelude)

    assertThat(interpreter.userBuildRequest("xx")).isEqualTo(StringValue("Hello world!"))
  }
}
