package com.giyeok.bibix.plugins.curl

import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

fun copyStreamToFile(stream: InputStream, dest: File) {
  stream.use { input ->
    FileOutputStream(dest).use { output ->
      val buffer = ByteArray(1000)
      var count: Int
      while (input.read(buffer, 0, 1000).also { count = it } >= 0) {
        output.write(buffer, 0, count)
      }
    }
  }
}
