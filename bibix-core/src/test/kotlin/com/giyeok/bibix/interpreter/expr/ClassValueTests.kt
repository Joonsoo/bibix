package com.giyeok.bibix.interpreter.expr

import com.giyeok.bibix.base.ClassInstanceValue
import com.giyeok.bibix.base.StringValue
import com.giyeok.bibix.interpreter.testInterpreter
import com.giyeok.bibix.plugins.Classes
import com.giyeok.bibix.plugins.PreloadedPlugin
import com.google.common.jimfs.Jimfs
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.io.path.writeText

class ClassValueTests {
  @Test
  fun test(): Unit = runBlocking {
    val fs = Jimfs.newFileSystem()

    val script = """
      package abc.def
      
      import abc
      import xyz
      
      class Abc(name: string, value: Def)
      class Def(hello: string)
      
      xxx = Abc("joonsoo", Def("world"))
      aaa = Abc(value=Def("abc"), name="def")
      yyy = abc.Qwe("google")
      zzz = abc.Rty(xyz.Xyz("farewell!"))
    """.trimIndent()
    fs.getPath("/build.bbx").writeText(script)

    val abcPlugin = PreloadedPlugin.fromScript(
      "com.abc",
      """
        import xyz
        
        class Qwe(bye: string)
        class Rty(hi: xyz.Xyz)
      """.trimIndent(),
      Classes()
    )

    val xyzPlugin = PreloadedPlugin.fromScript(
      "com.xyz",
      """
        class Xyz(msg: string)
      """.trimIndent(),
      Classes()
    )

    val interpreter = testInterpreter(fs, "/", mapOf("abc" to abcPlugin, "xyz" to xyzPlugin))

    assertThat(interpreter.userBuildRequest(listOf("xxx"))).isEqualTo(
      ClassInstanceValue(
        "abc.def", "Abc", mapOf(
          "name" to StringValue("joonsoo"),
          "value" to ClassInstanceValue("abc.def", "Def", mapOf("hello" to StringValue("world")))
        )
      )
    )
    assertThat(interpreter.userBuildRequest(listOf("aaa"))).isEqualTo(
      ClassInstanceValue(
        "abc.def", "Abc", mapOf(
          "name" to StringValue("def"),
          "value" to ClassInstanceValue("abc.def", "Def", mapOf("hello" to StringValue("abc")))
        )
      )
    )
    assertThat(interpreter.userBuildRequest(listOf("yyy"))).isEqualTo(
      ClassInstanceValue(
        "com.abc", "Qwe", mapOf(
          "bye" to StringValue("google"),
        )
      )
    )
    assertThat(interpreter.userBuildRequest(listOf("zzz"))).isEqualTo(
      ClassInstanceValue(
        "com.abc", "Rty", mapOf(
          "hi" to ClassInstanceValue("com.xyz", "Xyz", mapOf("msg" to StringValue("farewell!")))
        )
      )
    )
  }

  @Test
  fun testFieldTypeMismatch(): Unit = runBlocking {
    val fs = Jimfs.newFileSystem()

    val script = """
      package abc.def
      
      class Abc(name: string, value: Def)
      class Def(hello: string)
      
      xxx = Abc()
      yyy = Abc(Def("xyz"), "zyx")
      aaa = Abc("a", Def("b"), "c")
      zzz = Abc(name="def", value=Def("fed"), msg="message")
    """.trimIndent()
    fs.getPath("/build.bbx").writeText(script)

    val interpreter = testInterpreter(fs, "/", mapOf())

    assertThrows<IllegalStateException>("Missing parameters") {
      interpreter.userBuildRequest(listOf("xxx"))
    }
    assertThrows<IllegalStateException>("Coercion failed") {
      interpreter.userBuildRequest(listOf("yyy"))
    }
    assertThrows<IllegalStateException>("Unknown positional parameters") {
      interpreter.userBuildRequest(listOf("aaa"))
    }
    assertThrows<IllegalStateException>("Unknown parameters") {
      interpreter.userBuildRequest(listOf("zzz"))
    }
  }

  @Test
  fun testNoPackageName(): Unit = runBlocking {
    val fs = Jimfs.newFileSystem()

    val script = """
      class Abc(name: string, value: Def)
      class Def(hello: string)
      
      xxx = Abc("joonsoo", Def("world"))
    """.trimIndent()
    fs.getPath("/build.bbx").writeText(script)

    val interpreter = testInterpreter(fs, "/", mapOf())

    assertThrows<IllegalStateException>("Package name for class main:Abc not specified") {
      interpreter.userBuildRequest(listOf("xxx"))
    }
  }
}
