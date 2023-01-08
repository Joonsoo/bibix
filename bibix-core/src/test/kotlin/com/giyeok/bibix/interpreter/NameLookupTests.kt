package com.giyeok.bibix.interpreter

import com.giyeok.bibix.base.Architecture
import com.giyeok.bibix.base.BuildEnv
import com.giyeok.bibix.base.OS
import com.giyeok.bibix.base.StringValue
import com.giyeok.bibix.repo.BibixRepoProto.BibixRepo
import com.giyeok.bibix.repo.Repo
import com.giyeok.bibix.runner.RunConfigProto
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
      
      namespace qwe {
        rty = xyz
        namespace uio {
          zxc = "world!"
        }
      }
      
      asd = qwe.rty
      fgh = qwe.uio.zxc
      
      str = "hello ${d}fgh"
    """.trimIndent()
    fs.getPath("/build.bbx").writeText(script)

    val interpreter = testInterpreter(fs, "/", mapOf())

    assertThat(interpreter.userBuildRequest(listOf("abc"))).isEqualTo(StringValue("hello!"))
    assertThat(interpreter.userBuildRequest(listOf("xyz"))).isEqualTo(StringValue("hello!"))
    assertThat(interpreter.userBuildRequest(listOf("qwe", "rty"))).isEqualTo(StringValue("hello!"))
    assertThat(interpreter.userBuildRequest(listOf("asd"))).isEqualTo(StringValue("hello!"))
    assertThat(interpreter.userBuildRequest(listOf("fgh"))).isEqualTo(StringValue("world!"))
    assertThat(interpreter.userBuildRequest(listOf("str"))).isEqualTo(StringValue("hello world!"))
  }
}
