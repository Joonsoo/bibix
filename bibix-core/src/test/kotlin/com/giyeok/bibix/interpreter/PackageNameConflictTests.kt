package com.giyeok.bibix.interpreter

import com.giyeok.bibix.plugins.PluginInstanceProvider
import com.giyeok.bibix.plugins.PreloadedPlugin
import com.google.common.jimfs.Jimfs
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.io.path.writeText

class PackageNameConflictTests {
  @Test
  fun test(): Unit = runBlocking {
    val fs = Jimfs.newFileSystem()

    val script = """
      import abc
      import xyz
      
      aaa = abc.bbb + xyz.ccc
    """.trimIndent()
    fs.getPath("/build.bbx").writeText(script)

    val abcPlugin = PreloadedPlugin.fromScript(
      "com.abc",
      """
        bbb = "hello "
      """.trimIndent(),
      PluginInstanceProvider()
    )
    val xyzPlugin = PreloadedPlugin.fromScript(
      "com.abc",
      """
        ccc = "world!"
      """.trimIndent(),
      PluginInstanceProvider()
    )

    assertThrows<IllegalStateException> {
      testInterpreter(fs, "/", mapOf("abc" to abcPlugin, "xyz" to xyzPlugin))
    }
  }
}