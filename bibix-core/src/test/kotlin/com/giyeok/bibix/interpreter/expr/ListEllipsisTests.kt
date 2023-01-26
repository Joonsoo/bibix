package com.giyeok.bibix.interpreter.expr

import com.giyeok.bibix.base.FileValue
import com.giyeok.bibix.base.ListValue
import com.giyeok.bibix.base.SetValue
import com.giyeok.bibix.base.StringValue
import com.giyeok.bibix.interpreter.testInterpreter
import com.giyeok.bibix.plugins.prelude.preludePlugin
import com.google.common.jimfs.Jimfs
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.io.path.createDirectory
import kotlin.io.path.writeText

class ListEllipsisTests {
  @Test
  fun test(): Unit = runBlocking {
    val fs = Jimfs.newFileSystem()

    val script = """
      aaa = [...["hello", "world"]]
      bbb = [...["hello", "world"], "everyone"]
      ccc = [...aaa, ...bbb]
      ddd = [..."hello"]
      eee = ["hello", "world"] as set<string>
      fff = [...eee]
      ggg = [...eee] as set<string>
      hhh = [...glob("/hello/*")]
      iii = [...aaa + bbb]
    """.trimIndent()
    fs.getPath("/build.bbx").writeText(script)

    fs.getPath("/hello").createDirectory()
    fs.getPath("/hello/abc").writeText("hello!")
    fs.getPath("/hello/xyz").writeText("world!")

    val interpreter = testInterpreter(fs, "/", mapOf(), preludePlugin = preludePlugin)

    assertThat(interpreter.userBuildRequest("aaa")).isEqualTo(
      ListValue(StringValue("hello"), StringValue("world"))
    )
    assertThat(interpreter.userBuildRequest("bbb")).isEqualTo(
      ListValue(StringValue("hello"), StringValue("world"), StringValue("everyone"))
    )
    assertThat(interpreter.userBuildRequest("ccc")).isEqualTo(
      ListValue(
        StringValue("hello"),
        StringValue("world"),
        StringValue("hello"),
        StringValue("world"),
        StringValue("everyone")
      )
    )
    val exception = assertThrows<IllegalStateException> { interpreter.userBuildRequest("ddd") }
    assertThat(exception).hasMessageThat().isEqualTo("Cannot expand \"hello\"")
    assertThat(interpreter.userBuildRequest("fff")).isEqualTo(
      ListValue(StringValue("hello"), StringValue("world"))
    )
    assertThat(interpreter.userBuildRequest("ggg")).isEqualTo(
      SetValue(StringValue("hello"), StringValue("world"))
    )
    assertThat(interpreter.userBuildRequest("hhh")).isEqualTo(
      ListValue(FileValue(fs.getPath("/hello/abc")), FileValue(fs.getPath("/hello/xyz")))
    )
    assertThat(interpreter.userBuildRequest("iii")).isEqualTo(
      ListValue(
        StringValue("hello"),
        StringValue("world"),
        StringValue("hello"),
        StringValue("world"),
        StringValue("everyone")
      )
    )
  }
}
