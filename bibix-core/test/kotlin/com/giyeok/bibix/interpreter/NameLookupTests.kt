package com.giyeok.bibix.interpreter

import com.giyeok.bibix.base.StringValue
import com.google.common.jimfs.Jimfs
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.io.path.writeText

class NameLookupTests {
  @Test
  fun test() = runBlocking {
    val fs = Jimfs.newFileSystem()

    val d = "$"
    val script = """
      abc = "hello!"
      
      xyz = abc
      
      qwe {
        rty = xyz
        uio {
          zxc = "world!"
        }
      }
      
      asd = qwe.rty
      fgh = qwe.uio.zxc
      
      str = "hello ${d}fgh"
    """.trimIndent()
    fs.getPath("/build.bbx").writeText(script)

    val interpreter = testInterpreter(fs, "/", mapOf())

    assertThat(interpreter.userBuildRequest("abc")).isEqualTo(StringValue("hello!"))
    assertThat(interpreter.userBuildRequest("xyz")).isEqualTo(StringValue("hello!"))
    assertThat(interpreter.userBuildRequest("qwe.rty")).isEqualTo(StringValue("hello!"))
    assertThat(interpreter.userBuildRequest("asd")).isEqualTo(StringValue("hello!"))
    assertThat(interpreter.userBuildRequest("fgh")).isEqualTo(StringValue("world!"))
    assertThat(interpreter.userBuildRequest("str")).isEqualTo(StringValue("hello world!"))
  }
}
