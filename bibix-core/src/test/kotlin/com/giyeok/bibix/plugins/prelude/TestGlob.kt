package com.giyeok.bibix.plugins.prelude

import com.giyeok.bibix.base.FileValue
import com.giyeok.bibix.base.SetValue
import com.giyeok.bibix.interpreter.testInterpreter
import com.google.common.jimfs.Jimfs
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.io.path.createDirectories
import kotlin.io.path.createDirectory
import kotlin.io.path.writeText

class TestGlob {
  @Test
  fun testGlobFunction(): Unit = runBlocking {
    val fs = Jimfs.newFileSystem()

    fs.getPath("/projectRoot").createDirectory()

    val script = """
      xx = glob("subdir")
      yy = glob("subdir/**/*.*")
    """.trimIndent()
    fs.getPath("/projectRoot/build.bbx").writeText(script)

    fs.getPath("/projectRoot/subdir/1/2/3/4").createDirectories()
    fs.getPath("/projectRoot/subdir/1/2/3/4/5.txt").writeText("Hello")
    fs.getPath("/projectRoot/subdir2/5/6/7/8").createDirectories()
    fs.getPath("/projectRoot/subdir2/5/6/7/8/9.txt").writeText("World")

    val interpreter = testInterpreter(fs, "/projectRoot", mapOf(), preludePlugin = preludePlugin)

    assertThat(interpreter.userBuildRequest("xx"))
      .isEqualTo(SetValue(FileValue(fs.getPath("/projectRoot/subdir"))))
    assertThat(interpreter.userBuildRequest("yy"))
      .isEqualTo(SetValue(FileValue(fs.getPath("/projectRoot/subdir/1/2/3/4/5.txt"))))
  }
}