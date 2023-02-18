package com.giyeok.bibix.interpreter

import com.giyeok.bibix.base.*
import com.giyeok.bibix.plugins.PluginInstanceProvider
import com.giyeok.bibix.plugins.PreloadedPlugin
import com.google.common.jimfs.Jimfs
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.io.path.writeText

class ActionTests {
  @Test
  fun testNoArgs(): Unit = runBlocking {
    val fs = Jimfs.newFileSystem()

    val script = """
      import abc
      
      action runMyAction = abc.myAction("hello")
    """.trimIndent()
    fs.getPath("/build.bbx").writeText(script)

    val abcPlugin = PreloadedPlugin.fromScript(
      "com.abc",
      """
        action def myAction(msg: string)
          = native:com.giyeok.bibix.interpreter.ActionTestPlugin1
      """.trimIndent(),
      PluginInstanceProvider(ActionTestPlugin1::class.java)
    )

    val interpreter = testInterpreter(fs, "/", mapOf("abc" to abcPlugin))

    assertThat(interpreter.userBuildRequest("runMyAction")).isEqualTo(NoneValue)
    assertThat(ActionTestPlugin1.history).containsExactly(
      mapOf("msg" to StringValue("hello"))
    ).inOrder()
  }

  @Test
  fun testWithArgs(): Unit = runBlocking {
    val fs = Jimfs.newFileSystem()

    val script = """
      import abc
      
      action runMyAction(args) = abc.myAction("hello", args)
      action runYourAction(params) = abc.myAction("world", params)
    """.trimIndent()
    fs.getPath("/build.bbx").writeText(script)

    val abcPlugin = PreloadedPlugin.fromScript(
      "com.abc",
      """
        action def myAction(msg: string, args: list<string>)
          = native:com.giyeok.bibix.interpreter.ActionTestPlugin2
      """.trimIndent(),
      PluginInstanceProvider(ActionTestPlugin2::class.java)
    )

    val interpreter = testInterpreter(fs, "/", mapOf("abc" to abcPlugin))
    assertThat(interpreter.userBuildRequest("runMyAction")).isEqualTo(NoneValue)

    val interpreter2 = testInterpreter(
      fs,
      "/",
      mapOf("abc" to abcPlugin),
      actionArgs = listOf("This", "is", "action", "args")
    )
    assertThat(interpreter2.userBuildRequest("runMyAction")).isEqualTo(NoneValue)

    assertThat(ActionTestPlugin2.history).containsExactly(
      mapOf("msg" to StringValue("hello"), "args" to ListValue()),
      mapOf(
        "msg" to StringValue("hello"),
        "args" to ListValue(listOf("This", "is", "action", "args").map { StringValue(it) })
      )
    ).inOrder()

    val interpreter3 = testInterpreter(
      fs,
      "/",
      mapOf("abc" to abcPlugin),
      actionArgs = listOf("interpreter3", "parameters")
    )
    assertThat(interpreter3.userBuildRequest("runYourAction")).isEqualTo(NoneValue)

    assertThat(ActionTestPlugin2.history).containsExactly(
      mapOf("msg" to StringValue("hello"), "args" to ListValue()),
      mapOf(
        "msg" to StringValue("hello"),
        "args" to ListValue(listOf("This", "is", "action", "args").map { StringValue(it) })
      ),
      mapOf(
        "msg" to StringValue("world"),
        "args" to ListValue(listOf("interpreter3", "parameters").map { StringValue(it) })
      )
    ).inOrder()
  }

  @Test
  fun testReturn(): Unit = runBlocking {
    // TODO action impl이 BuildRuleReturn을 반환하는 경우 테스트
    TODO()
  }

  @Test
  fun testMultiCall(): Unit = runBlocking {
    // TODO multi call action 테스트
    TODO()
  }
}

class ActionTestPlugin1 {
  companion object {
    val history = mutableListOf<Map<String, BibixValue>>()

    @JvmStatic
    fun called(args: Map<String, BibixValue>) {
      history.add(args)
    }
  }

  fun run(actionContext: ActionContext) {
    called(actionContext.arguments)
  }
}

class ActionTestPlugin2 {
  companion object {
    val history = mutableListOf<Map<String, BibixValue>>()

    @JvmStatic
    fun called(args: Map<String, BibixValue>) {
      history.add(args)
    }
  }

  fun run(actionContext: ActionContext) {
    called(actionContext.arguments)
  }
}
