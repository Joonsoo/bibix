package com.giyeok.bibix.interpreter.expr

import com.giyeok.bibix.base.BuildContext
import com.giyeok.bibix.base.BuildRuleReturn
import com.giyeok.bibix.base.StringValue
import com.giyeok.bibix.interpreter.testInterpreter
import com.giyeok.bibix.plugins.Classes
import com.giyeok.bibix.plugins.PreloadedPlugin
import com.google.common.jimfs.Jimfs
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.io.path.isDirectory
import kotlin.io.path.writeText

class DirectoryLockTests {
  @Test
  fun test(): Unit = runBlocking {
    val fs = Jimfs.newFileSystem()

    val script = """
      import abc
      
      aaa = abc.test1("hello")
      bbb = abc.test1("world")
    """.trimIndent()
    fs.getPath("/build.bbx").writeText(script)

    val abcPlugin = PreloadedPlugin.fromScript(
      "com.abc",
      """
        def test1(key: string): string =
          native:com.giyeok.bibix.interpreter.expr.DirectoryLockTestRule1
      """.trimIndent(),
      Classes(DirectoryLockTestRule1::class.java)
    )

    val interpreter = testInterpreter(fs, "/", mapOf("abc" to abcPlugin))

    val aaa = async { interpreter.userBuildRequest("aaa") }
    val bbb = async { interpreter.userBuildRequest("bbb") }

    aaa.await()
    bbb.await()

    assertThat(fs.getPath("/bbxbuild/shared/test").isDirectory()).isTrue()

    val history = DirectoryLockTestRule1.history
    val keys = history.map { it.first }

    assertThat(keys).containsExactly(
      "init hello", "start hello", "end hello",
      "init world", "start world", "end world"
    )
    assertThat(keys.indexOf("init hello")).isLessThan(keys.indexOf("start hello"))
    assertThat(keys.indexOf("start hello")).isLessThan(keys.indexOf("end hello"))
    assertThat(keys.indexOf("init world")).isLessThan(keys.indexOf("start world"))
    assertThat(keys.indexOf("start world")).isLessThan(keys.indexOf("end world"))
    assertThat(keys.indexOf("start hello") + 1).isEqualTo(keys.indexOf("end hello"))
    assertThat(keys.indexOf("start world") + 1).isEqualTo(keys.indexOf("end world"))
  }
}

class DirectoryLockTestRule1 {
  companion object {
    val history = mutableListOf<Pair<String, Instant>>()

    @JvmStatic
    fun initAt(key: String, time: Instant) {
      history.add(Pair("init $key", time))
    }

    @JvmStatic
    fun startedAt(key: String, time: Instant) {
      history.add(Pair("start $key", time))
    }

    @JvmStatic
    fun endingAt(key: String, time: Instant) {
      history.add(Pair("end $key", time))
    }
  }

  fun build(context: BuildContext): BuildRuleReturn {
    val key = (context.arguments.getValue("key") as StringValue).value
    initAt(key, Instant.now())
    val dir = context.getSharedDirectory("test")
    return BuildRuleReturn.withDirectoryLock(dir) {
      startedAt(key, Instant.now())
      Thread.sleep(500)
      endingAt(key, Instant.now())
      BuildRuleReturn.value(StringValue("Good!"))
    }
  }
}
