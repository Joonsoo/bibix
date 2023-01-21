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
import java.lang.IllegalStateException
import kotlin.io.path.writeText

class NameAliasTests {
  @Test
  fun test(): Unit = runBlocking {
    val fs = Jimfs.newFileSystem()

    val script = """
      import xyz
      
      MyClass = xyz.XyzClass
      
      aaa = MyClass("hello")
    """.trimIndent()
    fs.getPath("/build.bbx").writeText(script)

    val xyzPlugin = PreloadedPlugin.fromScript(
      "com.xyz",
      """
        class XyzClass(message: string)
      """.trimIndent(),
      Classes()
    )

    val interpreter = testInterpreter(fs, "/", mapOf("xyz" to xyzPlugin))

    assertThat(interpreter.userBuildRequest("aaa")).isEqualTo(
      ClassInstanceValue(
        "com.xyz",
        "XyzClass",
        mapOf("message" to StringValue("hello"))
      )
    )
  }

  @Test
  fun test2(): Unit = runBlocking {
    // TODO 이 경우는 분명히 잘못되었지만 어디서 에러가 나야 하는지가 약간 애매한 경우인듯
    // InvalidSuper와 MyClass가 다른 패키지에 있기 때문에 잘못은 잘못
    val fs = Jimfs.newFileSystem()

    val script = """
      package main
      
      import xyz
      
      super class InvalidSuper { MyClass }
      MyClass = xyz.XyzClass
      
      aaa = MyClass("hello") as InvalidSuper
    """.trimIndent()
    fs.getPath("/build.bbx").writeText(script)

    val xyzPlugin = PreloadedPlugin.fromScript(
      "com.xyz",
      """
        class XyzClass(message: string)
      """.trimIndent(),
      Classes()
    )

    val interpreter = testInterpreter(fs, "/", mapOf("xyz" to xyzPlugin))

    assertThrows<IllegalStateException> { interpreter.userBuildRequest("aaa") }
  }
}
