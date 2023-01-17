package com.giyeok.bibix.interpreter.expr

import com.giyeok.bibix.base.*
import com.giyeok.bibix.interpreter.testInterpreter
import com.giyeok.bibix.plugins.Classes
import com.giyeok.bibix.plugins.PreloadedPlugin
import com.google.common.jimfs.Jimfs
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.io.path.writeText

class CallExprEvaluatorOrganizeParamsTests {
  @Test
  fun test(): Unit = runBlocking {
    val fs = Jimfs.newFileSystem()

    val script = """
      import abc
      
      aaa = abc.hello("hello", "world")
      bbb = abc.hello(a="hello", b="world")
      ccc = abc.hello(b="world", a="hello")
      ddd = abc.hello("hello", b="world")
    """.trimIndent()
    fs.getPath("/build.bbx").writeText(script)

    val abcPlugin = PreloadedPlugin.fromScript(
      "com.abc",
      """
        import xyz
        
        def hello(a: string, b: path): string = native:com.giyeok.bibix.interpreter.expr.TestPlugin8
      """.trimIndent(),
      Classes(TestPlugin8::class.java)
    )

    val interpreter = testInterpreter(fs, "/", mapOf("abc" to abcPlugin))

    assertThat(interpreter.userBuildRequest("aaa"))
      .isEqualTo(StringValue("a=\"hello\",b=path(/world)"))
    assertThat(interpreter.userBuildRequest("bbb"))
      .isEqualTo(StringValue("a=\"hello\",b=path(/world)"))
    assertThat(interpreter.userBuildRequest("ccc"))
      .isEqualTo(StringValue("a=\"hello\",b=path(/world)"))
    assertThat(interpreter.userBuildRequest("ddd"))
      .isEqualTo(StringValue("a=\"hello\",b=path(/world)"))
  }
}

class TestPlugin8 {
  fun build(context: BuildContext): BibixValue {
    val pairs = context.arguments.toList().sortedBy { it.first }
    val argsString = pairs.joinToString(",") { "${it.first}=${it.second}" }
    return StringValue(argsString)
  }
}
