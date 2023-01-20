package com.giyeok.bibix.plugins.bibix

import com.giyeok.bibix.base.FileValue
import com.giyeok.bibix.interpreter.testInterpreter
import com.giyeok.bibix.plugins.prelude.preludePlugin
import com.google.common.jimfs.Jimfs
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.lang.IllegalStateException
import kotlin.io.path.readText
import kotlin.io.path.writeText

class GenTypesKtTests {
  @Test
  fun test(): Unit = runBlocking {
    val fs = Jimfs.newFileSystem()

    val script = """
      package testing.gentypes
      import jvm

      super class GrandSupers { MySupers }
      super class MySupers { MyData1, MyData2 }
      class MyData1(anotherData: MyData2, message: string)
      class MyData2(from: string, to: string)
      enum MyEnums { great, bibix }

      aaa = bibix.genTypesKt(
        types = [GrandSupers, MySupers, MyData1, MyData2, MyEnums],
        packageName = "testing.pkgpkg",
      )
    """.trimIndent()
    fs.getPath("/build.bbx").writeText(script)

    val interpreter = testInterpreter(
      fs, "/",
      preloadedPlugins = mapOf("bibix" to bibixPlugin),
      preludePlugin = preludePlugin
    )

    val genFile = (interpreter.userBuildRequest("aaa") as FileValue).file.readText()

    val d = "$"
    assertThat(genFile).isEqualTo(
      """
      package testing.pkgpkg

      import com.giyeok.bibix.base.*
      import java.nio.file.Path

      sealed class GrandSupers {
        companion object {
          fun fromBibix(value: BibixValue): GrandSupers {
            value as ClassInstanceValue
            check(value.packageName == "testing.gentypes")
            return when (value.className) {
              "MyData1" -> MyData1.fromBibix(value)
              "MyData2" -> MyData2.fromBibix(value)
              else -> throw IllegalStateException("Unknown subclass of GrandSupers: $d{value.className}")
            }
          }
        }
        abstract fun toBibix(): ClassInstanceValue
      }

      sealed class MySupers: GrandSupers() {
        companion object {
          fun fromBibix(value: BibixValue): MySupers {
            value as ClassInstanceValue
            check(value.packageName == "testing.gentypes")
            return when (value.className) {
              "MyData1" -> MyData1.fromBibix(value)
              "MyData2" -> MyData2.fromBibix(value)
              else -> throw IllegalStateException("Unknown subclass of MySupers: $d{value.className}")
            }
          }
        }
      }

      data class MyData1(
        val anotherData: MyData2,
        val message: String,
      ): MySupers() {
        companion object {
          fun fromBibix(value: BibixValue): MyData1 {
            value as ClassInstanceValue
            check(value.packageName == "testing.gentypes")
            check(value.className == "MyData1")
            return MyData1(
              anotherData=MyData2.fromBibix(value["anotherData"]!!),
              message=(value["message"]!! as StringValue).value,
            )
          }
        }
        override fun toBibix(): ClassInstanceValue = ClassInstanceValue(
          "testing.gentypes",
          "MyData1",
          mapOf(
            "anotherData" to this.anotherData.toBibix(),
            "message" to StringValue(this.message),
          )
        )
      }

      data class MyData2(
        val from: String,
        val to: String,
      ): MySupers() {
        companion object {
          fun fromBibix(value: BibixValue): MyData2 {
            value as ClassInstanceValue
            check(value.packageName == "testing.gentypes")
            check(value.className == "MyData2")
            return MyData2(
              from=(value["from"]!! as StringValue).value,
              to=(value["to"]!! as StringValue).value,
            )
          }
        }
        override fun toBibix(): ClassInstanceValue = ClassInstanceValue(
          "testing.gentypes",
          "MyData2",
          mapOf(
            "from" to StringValue(this.from),
            "to" to StringValue(this.to),
          )
        )
      }

      enum class MyEnums {
        great,
        bibix,
      }
      
    """.trimIndent()
    )
  }

  @Test
  fun testDoubleSuperClasses(): Unit = runBlocking {
    val fs = Jimfs.newFileSystem()

    val script = """
      package testing.gentypes
      import jvm

      super class GrandSupers { MySupers, MyData2 }
      super class MySupers { MyData1, MyData2 }
      class MyData1(anotherData: MyData2, message: string)
      class MyData2(from: string, to: string)
      enum MyEnums { great, bibix }

      aaa = bibix.genTypesKt(
        types = [GrandSupers, MySupers, MyData1, MyData2, MyEnums],
        packageName = "testing.pkgpkg",
      )
    """.trimIndent()
    fs.getPath("/build.bbx").writeText(script)

    val interpreter = testInterpreter(
      fs, "/",
      preloadedPlugins = mapOf("bibix" to bibixPlugin),
      preludePlugin = preludePlugin
    )

    assertThrows<IllegalStateException> {
      interpreter.userBuildRequest("aaa")
    }
  }
}
