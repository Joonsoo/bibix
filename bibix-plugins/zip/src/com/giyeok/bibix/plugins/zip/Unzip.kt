package com.giyeok.bibix.plugins.zip

import com.giyeok.bibix.base.BuildContext
import com.giyeok.bibix.base.DirectoryValue
import com.giyeok.bibix.base.FileValue
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

class Unzip {
  fun build(context: BuildContext): DirectoryValue {
    val zipFile = (context.arguments.getValue("zipFile") as FileValue).file
    val destDirectory = context.destDirectory

    ZipInputStream(zipFile.inputStream().buffered()).use { zis ->
      var entry = zis.nextEntry
      while (entry != null) {
        if (entry.isDirectory) {
          File(destDirectory, entry.name).mkdir()
        } else {
          val destFile = File(destDirectory, entry.name)
          FileOutputStream(destFile).use { output ->
            val buffer = ByteArray(1000)
            var count: Int
            while (zis.read(buffer, 0, 1000).also { count = it } >= 0) {
              output.write(buffer, 0, count)
            }
          }
        }

        entry = zis.nextEntry
      }
      zis.closeEntry()
    }
    return DirectoryValue(destDirectory)
  }
}
