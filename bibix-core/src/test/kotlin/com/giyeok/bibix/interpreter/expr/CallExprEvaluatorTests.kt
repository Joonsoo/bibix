package com.giyeok.bibix.interpreter.expr

import com.giyeok.bibix.base.*
import com.giyeok.bibix.interpreter.*
import com.giyeok.bibix.plugins.Classes
import com.giyeok.bibix.plugins.PreloadedPlugin
import com.google.common.jimfs.Jimfs
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.io.path.writeText

class CallExprEvaluatorTests {
  @Test
  fun testPreloadedPluginsBuildRule(): Unit = runBlocking {
    val fs = Jimfs.newFileSystem()

    val script = """
      import abc
      
      xxx = abc.testRun(helloTo="world")
      yyy = abc.testRun2(adding="again")
    """.trimIndent()
    fs.getPath("/build.bbx").writeText(script)

    val abcPlugin = PreloadedPlugin.fromScript(
      "com.abc.abc",
      """
        def testRun(helloTo: string): string =
          native:com.giyeok.bibix.interpreter.expr.TestPlugin1
         
        def testRun2(adding: string): string =
          native:com.giyeok.bibix.interpreter.expr.TestPlugin2:build1
      """.trimIndent(),
      Classes(
        TestPlugin1::class.java,
        TestPlugin2::class.java
      )
    )

    val interpreter = testInterpreter(fs, "/", mapOf("abc" to abcPlugin))

    assertThat(interpreter.userBuildRequest("xxx")).isEqualTo(StringValue("hello world!"))
    assertThat(interpreter.userBuildRequest("yyy")).isEqualTo(StringValue("hello earth! again!"))
  }

  @Test
  fun testPreloadedPluginsActionRule(): Unit = runBlocking {
    val fs = Jimfs.newFileSystem()

    val script = """
      import abc
      
      action xxx = abc.sayHello(helloTo="world")
    """.trimIndent()
    fs.getPath("/build.bbx").writeText(script)

    val abcPlugin = PreloadedPlugin.fromScript(
      "com.abc.abc",
      """
        action def sayHello(helloTo: string) =
          native:com.giyeok.bibix.interpreter.expr.TestPlugin1
      """.trimIndent(),
      Classes(
        TestPlugin1::class.java,
        TestPlugin2::class.java
      )
    )

    val interpreter = testInterpreter(fs, "/", mapOf("abc" to abcPlugin))

    interpreter.userBuildRequest("xxx")
  }

  @Test
  fun testBuildRuleReturnEvalAndThen(): Unit = runBlocking {
    val fs = Jimfs.newFileSystem()

    val script = """
      import abc
      
      aaa = abc.hello3()
    """.trimIndent()
    fs.getPath("/build.bbx").writeText(script)

    val abcPlugin = PreloadedPlugin.fromScript(
      "com.abc.abc",
      """
        def depend(helloTo: string): string =
          native:com.giyeok.bibix.interpreter.expr.TestPlugin6:depend
        def hello3(): list<string> =
          native:com.giyeok.bibix.interpreter.expr.TestPlugin6
      """.trimIndent(),
      Classes(
        TestPlugin6::class.java
      )
    )

    val interpreter = testInterpreter(fs, "/", mapOf("abc" to abcPlugin))

    assertThat(interpreter.userBuildRequest("aaa")).isEqualTo(
      ListValue(
        StringValue("Hello world!"),
        StringValue("Hello world!"),
        StringValue("Hello world!")
      )
    )
  }
}

class TestPlugin1 {
  fun build(context: BuildContext): BibixValue {
    val helloTo = (context.arguments.getValue("helloTo") as StringValue).value
    return StringValue("hello $helloTo!")
  }

  fun run(context: ActionContext) {
    val helloTo = (context.arguments.getValue("helloTo") as StringValue).value
    println("Hello $helloTo!")
  }
}

class TestPlugin2 {
  fun build1(context: BuildContext): BuildRuleReturn {
    return BuildRuleReturn.evalAndThen(
      "testRun",
      mapOf("helloTo" to StringValue("earth")),
    ) { result ->
      val helloMessage = (result as StringValue).value
      val adding = (context.arguments.getValue("adding") as StringValue).value
      BuildRuleReturn.value(StringValue("$helloMessage $adding!"))
    }
  }
}

class TestPlugin6 {
  fun depend(context: BuildContext): BibixValue {
    val helloTo = (context.arguments.getValue("helloTo") as StringValue).value
    return StringValue("Hello $helloTo!")
  }

  fun build(context: BuildContext): BuildRuleReturn {
    return BuildRuleReturn.evalAndThen(
      "depend",
      mapOf("helloTo" to StringValue("world"))
    ) { result ->
      result as StringValue
      BuildRuleReturn.value(ListValue(result, result, result))
    }
  }
}