package com.giyeok.bibix.interpreter.expr

import com.giyeok.bibix.interpreter.testInterpreter
import com.google.common.jimfs.Jimfs
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.io.path.writeText
import com.giyeok.bibix.base.EnumValue

class EnumValueTests {
  @Test
  fun test(): Unit = runBlocking {
    val fs = Jimfs.newFileSystem()

    val script = """
      package abc.def
      
      enum MyEnum { Hello, World }
      
      xxx = MyEnum.Hello
      yyy = MyEnum.NotExist
    """.trimIndent()
    fs.getPath("/build.bbx").writeText(script)

    val interpreter = testInterpreter(fs, "/", mapOf())

    assertThat(interpreter.userBuildRequest("xxx")).isEqualTo(
      EnumValue("abc.def", "MyEnum", "Hello")
    )
    assertThrows<IllegalStateException>("Invalid enum value name") {
      interpreter.userBuildRequest("yyy")
    }
  }

  @Test
  fun testNoPackageName(): Unit = runBlocking {
    val fs = Jimfs.newFileSystem()

    val script = """
      enum MyEnum { Hello, World }
      
      xxx = MyEnum.Hello
    """.trimIndent()
    fs.getPath("/build.bbx").writeText(script)

    val interpreter = testInterpreter(fs, "/", mapOf())

    assertThrows<IllegalStateException>("Package name for enum type main:MyEnum not specified") {
      interpreter.userBuildRequest("xxx")
    }
  }
}
