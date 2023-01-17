package com.giyeok.bibix.interpreter.expr

import com.giyeok.bibix.base.PathValue
import com.giyeok.bibix.interpreter.testInterpreter
import com.google.common.jimfs.Jimfs
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.io.path.writeText

class ValueCoercionTests {
  @Test
  fun test(): Unit = runBlocking {
    val fs = Jimfs.newFileSystem()

    val script = """
      aaa = "123" as path
    """.trimIndent()
    fs.getPath("/build.bbx").writeText(script)

    val interpreter = testInterpreter(fs, "/", mapOf())

    assertThat(interpreter.userBuildRequest("aaa")).isEqualTo(PathValue(fs.getPath("/123")))
  }
}
