package com.giyeok.bibix.interpreter.expr

import com.giyeok.bibix.base.*
import com.giyeok.bibix.interpreter.*
import com.google.common.jimfs.Jimfs
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.io.path.writeText

class ExprEvaluatorTests {
  @Test
  fun test(): Unit = runBlocking {
    val fs = Jimfs.newFileSystem()

    val d = "$"
    val script = """
      aaa = ["hello", "world"]
      bbb = ("good", "bibix")
      ccc = (planet: "earth", person: "joonsoo")
      ddd = ccc.planet
      eee = "Hello ${d}ddd!"
      fff = "Hello ${d}{ccc.person}!"
      ggg = true
      hhh = false
      iii = [ggg, hhh]
      jjj = none
      xxx = ("hello")
    """.trimIndent()
    fs.getPath("/build.bbx").writeText(script)

    val interpreter = testInterpreter(fs, "/", mapOf())

    assertThat(interpreter.userBuildRequest(listOf("aaa"))).isEqualTo(
      ListValue(StringValue("hello"), StringValue("world"))
    )
    assertThat(interpreter.userBuildRequest(listOf("bbb"))).isEqualTo(
      TupleValue(StringValue("good"), StringValue("bibix"))
    )
    assertThat(interpreter.userBuildRequest(listOf("ccc"))).isEqualTo(
      NamedTupleValue("planet" to StringValue("earth"), "person" to StringValue("joonsoo"))
    )
    assertThat(interpreter.userBuildRequest(listOf("ddd"))).isEqualTo(StringValue("earth"))
    assertThat(interpreter.userBuildRequest(listOf("eee"))).isEqualTo(StringValue("Hello earth!"))
    assertThat(interpreter.userBuildRequest(listOf("fff"))).isEqualTo(StringValue("Hello joonsoo!"))
    assertThat(interpreter.userBuildRequest(listOf("ggg"))).isEqualTo(BooleanValue(true))
    assertThat(interpreter.userBuildRequest(listOf("hhh"))).isEqualTo(BooleanValue(false))
    assertThat(interpreter.userBuildRequest(listOf("iii"))).isEqualTo(
      ListValue(BooleanValue(true), BooleanValue(false))
    )
    assertThat(interpreter.userBuildRequest(listOf("jjj"))).isEqualTo(NoneValue)
    assertThat(interpreter.userBuildRequest(listOf("xxx"))).isEqualTo(StringValue("hello"))
  }
}
