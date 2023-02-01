package com.giyeok.bibix.interpreter.expr

import com.giyeok.bibix.base.BibixValue
import com.giyeok.bibix.base.BuildContext
import com.giyeok.bibix.base.StringValue
import com.giyeok.bibix.interpreter.testInterpreter
import com.giyeok.bibix.plugins.PluginInstanceProvider
import com.giyeok.bibix.plugins.PreloadedPlugin
import com.google.common.jimfs.Jimfs
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.io.path.*

class ObjectLinkTests {
  @Test
  fun test(): Unit = runBlocking {
    val fs = Jimfs.newFileSystem()

    val script = """
      import abc
      
      aaa = abc.rule1("hello!")
    """.trimIndent()
    fs.getPath("/build.bbx").writeText(script)

    val abcPlugin = PreloadedPlugin.fromScript(
      "com.abc",
      """
        def rule1(message: string): string =
          native:com.giyeok.bibix.interpreter.expr.ObjectLinkTestsPlugin1
      """.trimIndent(),
      PluginInstanceProvider(ObjectLinkTestsPlugin1::class.java)
    )

    val interpreter = testInterpreter(fs, "/", mapOf("abc" to abcPlugin))

    assertThat(interpreter.userBuildRequest("aaa")).isEqualTo(StringValue("Great!"))

    assertThat(fs.getPath("/bbxbuild/outputs").listDirectoryEntries()).containsExactly(
      fs.getPath("/bbxbuild/outputs/aaa")
    )

    assertThat(fs.getPath("/bbxbuild/outputs/aaa").isSymbolicLink()).isTrue()
    assertThat(fs.getPath("/bbxbuild/outputs/aaa").readSymbolicLink())
      .isEqualTo(fs.getPath("/bbxbuild/objects/079dba5cf200d8844b755ca72108eedcf5fe98dc"))
    assertThat(fs.getPath("/bbxbuild/outputs/aaa/output.txt").readText()).isEqualTo("hello!")
  }

  @Test
  fun testDouble(): Unit = runBlocking {
    val fs = Jimfs.newFileSystem()

    val script = """
      import abc
      
      aaa = bbb
      bbb = abc.rule1("hi!")
      ccc = abc.xxx
      ddd = abc.yyy
    """.trimIndent()
    fs.getPath("/build.bbx").writeText(script)

    val abcPlugin = PreloadedPlugin.fromScript(
      "com.abc",
      """
        def rule1(message: string): string =
          native:com.giyeok.bibix.interpreter.expr.ObjectLinkTestsPlugin1
        
        xxx = rule1("Good!")
        yyy = "Hello!"
      """.trimIndent(),
      PluginInstanceProvider(ObjectLinkTestsPlugin1::class.java)
    )

    val interpreter = testInterpreter(fs, "/", mapOf("abc" to abcPlugin))

    assertThat(interpreter.userBuildRequest("aaa")).isEqualTo(StringValue("Great!"))
    assertThat(interpreter.userBuildRequest("ccc")).isEqualTo(StringValue("Great!"))
    assertThat(interpreter.userBuildRequest("ddd")).isEqualTo(StringValue("Hello!"))

    val outputsEntries = fs.getPath("/bbxbuild/outputs").listDirectoryEntries()
    assertThat(outputsEntries).containsExactly(
      fs.getPath("/bbxbuild/outputs/aaa"),
      fs.getPath("/bbxbuild/outputs/bbb"),
      fs.getPath("/bbxbuild/outputs/ccc"),
    )

    assertThat(fs.getPath("/bbxbuild/outputs/aaa").isSymbolicLink()).isTrue()
    assertThat(fs.getPath("/bbxbuild/outputs/aaa").readSymbolicLink())
      .isEqualTo(fs.getPath("/bbxbuild/objects/ea3c6e081b32ed5462295912548e341b90653b9e"))
    assertThat(fs.getPath("/bbxbuild/outputs/aaa/output.txt").readText()).isEqualTo("hi!")

    assertThat(fs.getPath("/bbxbuild/outputs/bbb").isSymbolicLink()).isTrue()
    assertThat(fs.getPath("/bbxbuild/outputs/bbb").readSymbolicLink())
      .isEqualTo(fs.getPath("/bbxbuild/objects/ea3c6e081b32ed5462295912548e341b90653b9e"))

    assertThat(fs.getPath("/bbxbuild/outputs/ccc").isSymbolicLink()).isTrue()
    assertThat(fs.getPath("/bbxbuild/outputs/ccc").readSymbolicLink())
      .isEqualTo(fs.getPath("/bbxbuild/objects/632fce9116b3be154564e1f08fe8c5e6755f4221"))
    assertThat(fs.getPath("/bbxbuild/outputs/ccc/output.txt").readText()).isEqualTo("Good!")
  }
}

class ObjectLinkTestsPlugin1 {
  fun build(context: BuildContext): BibixValue {
    val message = (context.arguments.getValue("message") as StringValue).value
    context.destDirectory.resolve("output.txt").writeText(message)
    return StringValue("Great!")
  }
}
