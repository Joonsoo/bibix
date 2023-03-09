package com.giyeok.bibix.plugins.jvm

import com.giyeok.bibix.base.*
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.io.path.*

class Jar {
  fun jar(context: BuildContext): BibixValue {
    val jarFileName = (context.arguments.getValue("jarFileName") as StringValue).value
    val destFile = context.destDirectory.resolve(jarFileName)

    if (!context.hashChanged) {
      return FileValue(destFile)
    }

    val deps = (context.arguments.getValue("deps") as SetValue).values
      .map { ClassPkg.fromBibix(it) }
    ZipOutputStream(destFile.outputStream().buffered()).use { zos ->
      deps.forEach { classPkg ->
        addClasspathToJar(classPkg.cpinfo, zos)
      }
    }

    return FileValue(destFile)
  }

  fun uberJar(context: BuildContext): BuildRuleReturn {
    val jarFileName = (context.arguments.getValue("jarFileName") as StringValue).value
    val destFile = context.destDirectory.resolve(jarFileName)

    if (!context.hashChanged) {
      return BuildRuleReturn.value(FileValue(destFile))
    }
    val deps = (context.arguments.getValue("deps") as SetValue)
    return BuildRuleReturn.evalAndThen(
      "resolveClassPkgs",
      mapOf("classPkgs" to deps)
    ) { classPaths ->
      val cps = ((classPaths as ClassInstanceValue)["cps"] as SetValue).values
        .map { (it as PathValue).path }

      ZipOutputStream(destFile.outputStream().buffered()).use { zos ->
        cps.forEach { cp ->
          if (cp.isRegularFile()) {
            // TODO dep is jar file
            addAnotherJarToJar(cp, zos)
          } else {
            check(cp.isDirectory())
            addDirectoryToJar(cp, zos)
          }
        }
      }
      BuildRuleReturn.value(FileValue(destFile))
    }
  }

  fun executableUberJar(context: BuildContext): BuildRuleReturn {
    val jarFileName = (context.arguments.getValue("jarFileName") as StringValue).value
    val destFile = context.destDirectory.resolve(jarFileName)

    if (!context.hashChanged) {
      return BuildRuleReturn.value(FileValue(destFile))
    }
    val mainClass = (context.arguments.getValue("mainClass") as StringValue).value
    val deps = (context.arguments.getValue("deps") as SetValue)
    return BuildRuleReturn.evalAndThen(
      "resolveClassPkgs",
      mapOf("classPkgs" to deps)
    ) { classPaths ->
      val cps = ((classPaths as ClassInstanceValue)["cps"] as SetValue).values
        .map { (it as PathValue).path }

      ZipOutputStream(destFile.outputStream().buffered()).use { zos ->
        cps.forEach { cp ->
          if (cp.isRegularFile()) {
            addAnotherJarToJar(cp, zos)
          } else {
            check(cp.isDirectory())
            addDirectoryToJar(cp, zos)
          }
        }

        zos.putNextEntry(ZipEntry("META-INF/MANIFEST.MF"))
        transferFromStreamToStream(
          ByteArrayInputStream("Manifest-Version: 1.0\nMain-Class: $mainClass\n".encodeToByteArray()),
          zos
        )
        zos.closeEntry()
      }
      BuildRuleReturn.value(FileValue(destFile))
    }
  }

  fun addClasspathToJar(cp: CpInfo, dest: ZipOutputStream) {
    when (cp) {
      is ClassesInfo -> {
        cp.classDirs.forEach { classDir ->
          addDirectoryToJar(classDir, dest)
        }
        cp.resDirs.forEach { resDir ->
          addDirectoryToJar(resDir, dest)
        }
      }

      is JarInfo -> {
        addAnotherJarToJar(cp.jar, dest)
      }
    }
  }

  fun addAnotherJarToJar(inputJar: Path, dest: ZipOutputStream) {
    ZipInputStream(inputJar.inputStream().buffered()).use { zis ->
      var entry = zis.nextEntry
      while (entry != null) {
        if (!entry.isDirectory &&
          !entry.name.startsWith("META-INF/") &&
          entry.name != "module-info.class"
        ) {
          try {
            dest.putNextEntry(ZipEntry(entry.name))
            transferFromStreamToStream(zis, dest)
          } catch (e: ZipException) {
            // TODO 일단 무시하고 진행하도록
            println(e.message)
          }
        }
        entry = zis.nextEntry
      }
      zis.closeEntry()
    }
  }

  fun addDirectoryToJar(inputDir: Path, dest: ZipOutputStream) {
    Files.walk(inputDir).toList().forEach { path ->
      if (path.isRegularFile()) {
        val filePath = path.absolute().relativeTo(inputDir.absolute()).pathString
        if (!filePath.startsWith("META-INF") && filePath != "module-info.class") {
          try {
            dest.putNextEntry(ZipEntry(filePath))
            transferFromStreamToStream(path.toFile().inputStream().buffered(), dest)
          } catch (e: ZipException) {
            // TODO 일단 무시하고 진행하도록
            println(e.message)
          }
        }
      }
    }
  }

  private fun transferFromStreamToStream(input: InputStream, output: OutputStream) {
    val buffer = ByteArray(1000)
    var count: Int
    while (input.read(buffer, 0, 1000).also { count = it } >= 0) {
      output.write(buffer, 0, count)
    }
  }
}
