package com.giyeok.bibix.interpreter.expr

import com.giyeok.bibix.base.BibixValue
import com.giyeok.bibix.base.BuildContext
import com.giyeok.bibix.base.NoneValue
import com.giyeok.bibix.base.StringValue
import com.giyeok.bibix.interpreter.testInterpreter
import com.giyeok.bibix.plugins.PluginInstanceProvider
import com.giyeok.bibix.plugins.PreloadedPlugin
import com.google.common.jimfs.Jimfs
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.io.path.writeText

class OptionalParamsTests {
  @Test
  fun test(): Unit = runBlocking {
    val fs = Jimfs.newFileSystem()

    val script = """
      import xyz
      
      aaa = xyz.rule1("hello")
      bbb = xyz.rule1("hello", "world", "everyone")
      ccc = xyz.rule1("hello", noDefault="everyone")
      ddd = xyz.rule1("hello", "everyone")
      eee = xyz.rule1("hello", withDefault="everyone")
    """.trimIndent()
    fs.getPath("/build.bbx").writeText(script)

    val xyzPlugin = PreloadedPlugin.fromScript(
      "abc.xyz",
      """
        def rule1(
          required: string,
          withDefault?: string = "default value",
          noDefault?: string,
        ): string = native:com.giyeok.bibix.interpreter.expr.OptionalParamsTestRule1
      """.trimIndent(),
      PluginInstanceProvider(OptionalParamsTestRule1::class.java)
    )

    val interpreter = testInterpreter(fs, "/", mapOf("xyz" to xyzPlugin))

    OptionalParamsTestRule1.args = null
    assertThat(interpreter.userBuildRequest("aaa"))
      .isEqualTo(StringValue("\"hello\" \"default value\" none"))
    assertThat(OptionalParamsTestRule1.args).containsExactly(
      "required", StringValue("hello"),
      "withDefault", StringValue("default value"),
      "noDefault", NoneValue
    )

    OptionalParamsTestRule1.args = null
    assertThat(interpreter.userBuildRequest("bbb"))
      .isEqualTo(StringValue("\"hello\" \"world\" \"everyone\""))
    assertThat(OptionalParamsTestRule1.args).containsExactly(
      "required", StringValue("hello"),
      "withDefault", StringValue("world"),
      "noDefault", StringValue("everyone")
    )

    OptionalParamsTestRule1.args = null
    assertThat(interpreter.userBuildRequest("ccc"))
      .isEqualTo(StringValue("\"hello\" \"default value\" \"everyone\""))
    assertThat(OptionalParamsTestRule1.args).containsExactly(
      "required", StringValue("hello"),
      "withDefault", StringValue("default value"),
      "noDefault", StringValue("everyone")
    )

    OptionalParamsTestRule1.args = null
    assertThat(interpreter.userBuildRequest("ddd"))
      .isEqualTo(StringValue("\"hello\" \"everyone\" none"))
    assertThat(OptionalParamsTestRule1.args).containsExactly(
      "required", StringValue("hello"),
      "withDefault", StringValue("everyone"),
      "noDefault", NoneValue
    )

    // evaluation result is cached, so plugin won't be called
    assertThat(interpreter.userBuildRequest("eee"))
      .isEqualTo(StringValue("\"hello\" \"everyone\" none"))
  }
}

class OptionalParamsTestRule1 {
  companion object {
    @JvmStatic
    var args: Map<String, BibixValue>? = null
  }

  fun build(context: BuildContext): BibixValue {
    assertThat(context.arguments.keys).containsExactly("required", "withDefault", "noDefault")
    args = context.arguments
    val required = context.arguments.getValue("required") as StringValue
    val withDefault = context.arguments.getValue("withDefault") as StringValue
    val noDefault = context.arguments.getValue("noDefault")
    return StringValue("$required $withDefault $noDefault")
  }
}
