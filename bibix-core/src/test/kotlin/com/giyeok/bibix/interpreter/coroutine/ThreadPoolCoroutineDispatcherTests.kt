package com.giyeok.bibix.interpreter.coroutine

import com.giyeok.bibix.base.ListValue
import com.giyeok.bibix.base.StringValue
import com.giyeok.bibix.interpreter.task.Task
import com.giyeok.bibix.interpreter.testInterpreter
import com.google.common.jimfs.Jimfs
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.*
import org.junit.jupiter.api.Test
import kotlin.io.path.writeText

class ThreadPoolCoroutineDispatcherTests {
  @Test
  fun test(): Unit = runBlocking {
    val fs = Jimfs.newFileSystem()

    val script = """
      abc = [aaa, bbb, ccc]
      aaa = "hello"
      bbb = "world"
      ccc = "again!"
    """.trimIndent()
    fs.getPath("/build.bbx").writeText(script)

    val interpreter = testInterpreter(fs, "/", mapOf())

    val threadPoolEvents = mutableListOf<ThreadPoolEvent>()

    val threadPool = ThreadPool(4) { event ->
      synchronized(threadPoolEvents) {
        threadPoolEvents.add(event)
      }
    }

    val deferred = CoroutineScope(threadPool + TaskElement(Task.RootTask)).async {
      interpreter.userBuildRequest("abc")
    }

    threadPool.processTasks(deferred.job)

    assertThat(deferred.await()).isEqualTo(
      ListValue(StringValue("hello"), StringValue("world"), StringValue("again!"))
    )

    delay(100)

    assertThat(threadPool.queue).isEmpty()
    threadPoolEvents.forEach { println(it) }
  }
}
