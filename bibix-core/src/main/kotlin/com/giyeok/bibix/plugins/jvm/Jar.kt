package com.giyeok.bibix.plugins.jvm

import com.giyeok.bibix.base.*
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.io.path.isRegularFile

class Jar {
  fun jar(context: BuildContext): BibixValue {
    TODO()
  }

  fun uberJar(context: BuildContext): BibixValue {
    TODO()
  }

  fun executableUberJar(context: BuildContext): BibixValue {
    val mainClass = (context.arguments.getValue("mainClass") as StringValue).value
    val deps = (context.arguments.getValue("deps") as SetValue).values.flatMap { dep ->
      ((dep as ClassInstanceValue).value as SetValue).values.map { (it as PathValue).path }
    }
    val jarFileName = (context.arguments.getValue("jarFileName") as StringValue).value
    val destFile = File(context.destDirectory, jarFileName)

    ZipOutputStream(destFile.outputStream().buffered()).use { zos ->
      deps.reversed().forEach { dep ->
        if (dep.isFile) {
          // TODO dep is jar file
          ZipInputStream(dep.inputStream().buffered()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
              if (!entry.isDirectory &&
                !entry.name.startsWith("META-INF/") &&
                entry.name != "module-info.class"
              ) {
                try {
                  zos.putNextEntry(ZipEntry(entry.name))
                  transferFromStreamToStream(zis, zos)
                } catch (e: ZipException) {
                  // TODO 일단 무시하고 진행하도록
                  println(e.message)
                }
              }
              entry = zis.nextEntry
            }
            zis.closeEntry()
          }
        } else {
          check(dep.isDirectory)
          Files.walk(Path.of(dep.canonicalPath)).toList().forEach { path ->
            if (path.isRegularFile()) {
              val filePath = path.toFile().relativeTo(dep.canonicalFile)
              if (!filePath.startsWith("META-INF") && filePath.path != "module-info.class") {
                try {
                  zos.putNextEntry(ZipEntry(filePath.path))
                  transferFromStreamToStream(path.toFile().inputStream().buffered(), zos)
                } catch (e: ZipException) {
                  // TODO 일단 무시하고 진행하도록
                  println(e.message)
                }
              }
            }
          }
        }
      }
      zos.putNextEntry(ZipEntry("META-INF/MANIFEST.MF"))
      transferFromStreamToStream(
        ByteArrayInputStream("Manifest-Version: 1.0\nMain-Class: $mainClass\n".encodeToByteArray()),
        zos
      )
      zos.closeEntry()
    }
    return FileValue(destFile)
  }

  private fun transferFromStreamToStream(input: InputStream, output: OutputStream) {
    val buffer = ByteArray(1000)
    var count: Int
    while (input.read(buffer, 0, 1000).also { count = it } >= 0) {
      output.write(buffer, 0, count)
    }
  }
}
