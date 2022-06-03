package com.giyeok.bibix.plugins.zip

import com.giyeok.bibix.base.BuildContext
import com.giyeok.bibix.base.DirectoryValue
import com.giyeok.bibix.base.FileValue
import java.util.zip.ZipInputStream
import kotlin.io.path.createDirectory
import kotlin.io.path.inputStream
import kotlin.io.path.notExists
import kotlin.io.path.outputStream

class Unzip {
  fun build(context: BuildContext): DirectoryValue {
    val zipFile = (context.arguments.getValue("zipFile") as FileValue).file
    val destDirectory = context.destDirectory

    if (context.hashChanged) {
      ZipInputStream(zipFile.inputStream().buffered()).use { zis ->
        var entry = zis.nextEntry
        while (entry != null) {
          val entryPath = destDirectory.resolve(entry.name)
          if (entry.isDirectory) {
            if (entryPath.notExists()) {
              entryPath.createDirectory()
            }
          } else {
            entryPath.outputStream().buffered().use { output ->
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
    }
    return DirectoryValue(destDirectory)
  }
}
