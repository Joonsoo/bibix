package com.giyeok.bibix.interpreter.expr

import com.giyeok.bibix.base.ListValue
import com.giyeok.bibix.base.StringValue
import com.giyeok.bibix.interpreter.testInterpreter
import com.giyeok.bibix.plugins.PluginInstanceProvider
import com.giyeok.bibix.plugins.PreloadedPlugin
import com.google.common.jimfs.Jimfs
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.io.path.writeText

class VarTests {
  @Test
  fun test(): Unit = runBlocking {
    val fs = Jimfs.newFileSystem()

    val script = """
      import xyz
      
      var xyz: string = "var value"
      
      msg = xyz
    """.trimIndent()
    fs.getPath("/build.bbx").writeText(script)

    val interpreter = testInterpreter(fs, "/", mapOf())

    assertThat(interpreter.userBuildRequest("msg")).isEqualTo(StringValue("var value"))
  }

  @Test
  fun testRedef(): Unit = runBlocking {
    val fs = Jimfs.newFileSystem()

    val script = """
      import xyz
      
      var xyz.message = "new value"
      
      msg = xyz.message
    """.trimIndent()
    fs.getPath("/build.bbx").writeText(script)

    val xyz = PreloadedPlugin.fromScript(
      "abc.xyz",
      """
        var message = "old value"
      """.trimIndent(),
      PluginInstanceProvider()
    )

    val interpreter = testInterpreter(fs, "/", mapOf("xyz" to xyz))

    assertThat(interpreter.userBuildRequest("msg")).isEqualTo(StringValue("new value"))
  }

  @Test
  fun testRedefFromImportInNamespace(): Unit = runBlocking {
    val fs = Jimfs.newFileSystem()

    val script = """
      abc {
        import xyz
      }
      
      var abc.xyz.hello.message = "new value"
      
      msg = abc.xyz.hello.message
    """.trimIndent()
    fs.getPath("/build.bbx").writeText(script)

    val xyz = PreloadedPlugin.fromScript(
      "abc.xyz",
      """
        hello {
          var message = "old value"
        }
      """.trimIndent(),
      PluginInstanceProvider()
    )

    val interpreter = testInterpreter(fs, "/", mapOf("xyz" to xyz))

    assertThat(interpreter.userBuildRequest("msg")).isEqualTo(StringValue("new value"))
  }

  @Test
  fun testMultiRedef(): Unit = runBlocking {
    val fs = Jimfs.newFileSystem()

    val script = """
      import xyz
      
      var xyz.message = "new value", xyz.msg2 = "another new value"
      
      msg = [xyz.message, xyz.msg2]
    """.trimIndent()
    fs.getPath("/build.bbx").writeText(script)

    val xyz = PreloadedPlugin.fromScript(
      "abc.xyz",
      """
        var message = "old value"
        var msg2 = "another old value"
      """.trimIndent(),
      PluginInstanceProvider()
    )

    val interpreter = testInterpreter(fs, "/", mapOf("xyz" to xyz))

    assertThat(interpreter.userBuildRequest("msg"))
      .isEqualTo(ListValue(StringValue("new value"), StringValue("another new value")))
  }

  @Test
  fun testVarInNamespace(): Unit = runBlocking {
    val fs = Jimfs.newFileSystem()

    val script = """
      namespace xyz {
        var xyzValue: string = "invalid"
      }
    """.trimIndent()
    fs.getPath("/build.bbx").writeText(script)

    assertThrows<IllegalStateException>("var must be in the root scope of the script") {
      testInterpreter(fs, "/", mapOf())
    }
  }
}
