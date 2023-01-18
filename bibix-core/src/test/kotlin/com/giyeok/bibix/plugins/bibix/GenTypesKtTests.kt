package com.giyeok.bibix.plugins.bibix

import com.giyeok.bibix.base.FileValue
import com.giyeok.bibix.interpreter.testInterpreter
import com.giyeok.bibix.plugins.prelude.preludePlugin
import com.google.common.jimfs.Jimfs
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.io.path.readText
import kotlin.io.path.writeText

class GenTypesKtTests {
  @Test
  fun test(): Unit = runBlocking {
    val fs = Jimfs.newFileSystem()

    val script = """
      package testing.gentypes
      import jvm

      super class MySupers { MyData1, MyData2 }
      class MyData1(anotherData: MyData2, message: string)
      class MyData2(from: string, to: string)
      enum MyEnums { bibix, is, great }

      aaa = bibix.genTypesKt(
        types = [MySupers, MyData1, MyData2, MyEnums],
        packageName = "testing.package",
      )
    """.trimIndent()
    fs.getPath("/build.bbx").writeText(script)

    val interpreter = testInterpreter(
      fs, "/",
      preloadedPlugins = mapOf("bibix" to bibixPlugin),
      preludePlugin = preludePlugin
    )

    val genFile = (interpreter.userBuildRequest("aaa") as FileValue).file.readText()

    println(genFile)
  }
}
