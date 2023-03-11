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
        addClasspathToJar(classPkg.cpinfo, zos) { false }
      }
    }

    return FileValue(destFile)
  }

  // TODO filter를 parameter로 사용자에게 노출
  private val skipFiles = { path: String ->
    // "META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA"
    path == "META-INF/MANIFEST.MF" ||
      (path.startsWith("META-INF/") && (
        path.endsWith(".SF") || path.endsWith(".DSA") || path.endsWith(".RSA")))
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
            addAnotherJarToJar(cp, zos, skipFiles)
          } else {
            check(cp.isDirectory())
            addDirectoryToJar(cp, zos, skipFiles)
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
            addAnotherJarToJar(cp, zos, skipFiles)
          } else {
            check(cp.isDirectory())
            addDirectoryToJar(cp, zos, skipFiles)
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

  fun addClasspathToJar(cp: CpInfo, dest: ZipOutputStream, skipEntry: (String) -> Boolean) {
    when (cp) {
      is ClassesInfo -> {
        cp.classDirs.forEach { classDir ->
          addDirectoryToJar(classDir, dest, skipEntry)
        }
        cp.resDirs.forEach { resDir ->
          addDirectoryToJar(resDir, dest, skipEntry)
        }
      }

      is JarInfo -> {
        addAnotherJarToJar(cp.jar, dest, skipEntry)
      }
    }
  }

  fun addAnotherJarToJar(inputJar: Path, dest: ZipOutputStream, skipEntry: (String) -> Boolean) {
    ZipInputStream(inputJar.inputStream().buffered()).use { zis ->
      var entry = zis.nextEntry
      while (entry != null) {
        if (!entry.isDirectory && !skipEntry(entry.name)) {
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

  fun addDirectoryToJar(inputDir: Path, dest: ZipOutputStream, skipEntry: (String) -> Boolean) {
    Files.walk(inputDir).toList().forEach { path ->
      if (path.isRegularFile()) {
        val filePath = path.absolute().relativeTo(inputDir.absolute()).pathString
        if (!skipEntry(filePath)) {
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
