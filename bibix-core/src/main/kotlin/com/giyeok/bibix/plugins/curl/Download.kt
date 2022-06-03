package com.giyeok.bibix.plugins.curl

import com.giyeok.bibix.base.BuildContext
import com.giyeok.bibix.base.FileValue
import com.giyeok.bibix.base.StringValue
import java.io.File
import java.io.InputStream
import java.net.URL
import java.nio.file.Path
import kotlin.io.path.deleteIfExists
import kotlin.io.path.outputStream

class Download {
  fun build(context: BuildContext): FileValue {
    val url = URL((context.arguments.getValue("url") as StringValue).value)
    val filename = (context.arguments["filename"] as? StringValue)?.value ?: File(url.file).name
    val destFile = context.destDirectory.resolve(filename)
    if (context.hashChanged) {
      destFile.deleteIfExists()
      copyStreamToFile(url.openStream().buffered(), destFile)
    }
    return FileValue(destFile)
  }
}

fun copyStreamToFile(stream: InputStream, dest: Path) {
  stream.use { input ->
    dest.outputStream().buffered().use { output ->
      val buffer = ByteArray(1000)
      var count: Int
      while (input.read(buffer, 0, 1000).also { count = it } >= 0) {
        output.write(buffer, 0, count)
      }
    }
  }
}
