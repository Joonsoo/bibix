package com.giyeok.bibix.interpreter.expr

import com.giyeok.bibix.base.*
import com.giyeok.bibix.interpreter.*
import com.google.common.jimfs.Jimfs
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.io.path.Path
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

    assertThat(interpreter.userBuildRequest("aaa")).isEqualTo(
      ListValue(StringValue("hello"), StringValue("world"))
    )
    assertThat(interpreter.userBuildRequest("bbb")).isEqualTo(
      TupleValue(StringValue("good"), StringValue("bibix"))
    )
    assertThat(interpreter.userBuildRequest("ccc")).isEqualTo(
      NamedTupleValue("planet" to StringValue("earth"), "person" to StringValue("joonsoo"))
    )
    assertThat(interpreter.userBuildRequest("ddd")).isEqualTo(StringValue("earth"))
    assertThat(interpreter.userBuildRequest("eee")).isEqualTo(StringValue("Hello earth!"))
    assertThat(interpreter.userBuildRequest("fff")).isEqualTo(StringValue("Hello joonsoo!"))
    assertThat(interpreter.userBuildRequest("ggg")).isEqualTo(BooleanValue(true))
    assertThat(interpreter.userBuildRequest("hhh")).isEqualTo(BooleanValue(false))
    assertThat(interpreter.userBuildRequest("iii")).isEqualTo(
      ListValue(BooleanValue(true), BooleanValue(false))
    )
    assertThat(interpreter.userBuildRequest("jjj")).isEqualTo(NoneValue)
    assertThat(interpreter.userBuildRequest("xxx")).isEqualTo(StringValue("hello"))
  }

  @Test
  fun testMergeOp(): Unit = runBlocking {
    val fs = Jimfs.newFileSystem()

    val script = """
      aaa = "hello" + " world!"
      bbb = ["hello", "world"] + ["everyone"]
      ccc = ["hello"] + "world"
      ddd = [] + "hello"
      eee = [("hello", "world")] + ["good"]
      fff = ["hello", "world"] as set<string> + ["everyone"]
      ggg = ["hello", "world"] + (["everyone"] as set<string>)
    """.trimIndent()
    fs.getPath("/build.bbx").writeText(script)

    val interpreter = testInterpreter(fs, "/", mapOf())

    assertThat(interpreter.userBuildRequest("aaa")).isEqualTo(StringValue("hello world!"))
    assertThat(interpreter.userBuildRequest("bbb")).isEqualTo(
      ListValue(StringValue("hello"), StringValue("world"), StringValue("everyone"))
    )
    assertThrows<IllegalStateException> { interpreter.userBuildRequest("ccc") }
    assertThrows<IllegalStateException> { interpreter.userBuildRequest("ddd") }
    assertThat(interpreter.userBuildRequest("eee")).isEqualTo(
      ListValue(TupleValue(StringValue("hello"), StringValue("world")), StringValue("good"))
    )
    assertThat(interpreter.userBuildRequest("fff")).isEqualTo(
      SetValue(StringValue("hello"), StringValue("world"), StringValue("everyone"))
    )
    assertThat(interpreter.userBuildRequest("ggg")).isEqualTo(
      ListValue(StringValue("hello"), StringValue("world"), StringValue("everyone"))
    )
  }

  @Test
  fun testCastOp(): Unit = runBlocking {
    val fs = Jimfs.newFileSystem()

    val d = "$"
    val script = """
      package abc.def
      
      aaa = "hello" as path
      
      class Xyz(message: string) {
        as string = "Xyz says $d{this.message}"
      }
      
      class Abc(name: string) {
        as Xyz = Xyz("Hello ${d}{this.name}")
        as string = "Abc is called ${d}{this.name}"
      }
      
      bbb = Xyz("good") as string
      ccc = Abc("world") as Xyz
      ddd = Abc("world") as string
    """.trimIndent()
    fs.getPath("/build.bbx").writeText(script)

    val interpreter = testInterpreter(fs, "/", mapOf())

    assertThat(interpreter.userBuildRequest("aaa")).isEqualTo(PathValue(fs.getPath("/hello")))
    assertThat(interpreter.userBuildRequest("bbb")).isEqualTo(StringValue("Xyz says good"))
    assertThat(interpreter.userBuildRequest("ccc")).isEqualTo(
      ClassInstanceValue("abc.def", "Xyz", mapOf("message" to StringValue("Hello world")))
    )
    assertThat(interpreter.userBuildRequest("ddd")).isEqualTo(StringValue("Abc is called world"))
  }
}
