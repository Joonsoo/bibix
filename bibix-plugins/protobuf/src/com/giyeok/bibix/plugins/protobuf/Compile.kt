package com.giyeok.bibix.plugins.protobuf

import com.giyeok.bibix.base.*
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

class Compile {
  fun schema(context: BuildContext): BibixValue {
    val srcs = context.arguments.getValue("srcs") as SetValue
    val protocPath = (context.arguments.getValue("protocPath") as DirectoryValue).directory

    return ClassInstanceValue(
      CName(context.sourceId, "ProtoSchema"),
      NamedTupleValue(
        "schemaFiles" to srcs,
        "includes" to SetValue(PathValue(File(protocPath, "include")))
      )
    )
  }

  private fun callCompiler(context: BuildContext, outArgs: List<String>) {
    val os = (context.arguments.getValue("os") as EnumValue).value
    val protocPath = (context.arguments.getValue("protocPath") as DirectoryValue).directory

    val schema =
      ((context.arguments.getValue("schema") as ClassInstanceValue).value as NamedTupleValue).values
    val srcs = (schema[0].second as SetValue).values.map { (it as FileValue).file }
    val includes = (schema[1].second as SetValue).values.map { (it as PathValue).path }

    val srcArgs = mutableListOf<String>()
    srcs.forEach { srcArgs.add(it.canonicalPath) }

    val protoPaths = srcs.map { it.parentFile }.toSet() + includes
    protoPaths.forEach { srcArgs.add("-I${it.canonicalPath}") }

    val executableName = if (os == "windows") "protoc.exe" else "protoc"
    val executableFile = File(File(protocPath, "bin"), executableName)

    executableFile.setExecutable(true)

    val runArgs = listOf(executableFile.canonicalPath) + srcArgs + outArgs
    val process = Runtime.getRuntime()
      .exec(runArgs.toTypedArray(), arrayOf(), File(protocPath, "bin").canonicalFile)

    context.progressIndicator.infoLog(String(process.inputStream.readAllBytes()))
    context.progressIndicator.errorLog(String(process.errorStream.readAllBytes()))
    process.waitFor()

    check(process.exitValue() == 0)
  }

  fun protoset(context: BuildContext): BibixValue {
    val outputFileName =
      (context.arguments["outputFileName"] as? StringValue)?.value ?: "descriptor.protoset"

    val destFile = File(context.destDirectory, outputFileName)

    if (context.hashChanged) {
      callCompiler(context, listOf("--descriptor_set_out=${destFile.canonicalPath}"))
    }

    return FileValue(destFile)
  }

  fun cpp(context: BuildContext): BibixValue {
    TODO()
  }

  fun csharp(context: BuildContext): BibixValue {
    TODO()
  }

  private fun getFiles(directory: File): SetValue {
    val files = Files.walk(Path.of(directory.toURI()), 1000).toList()
      .map { it.toFile() }
      .filter { it.isFile }
      .map { FileValue(it) }
    return SetValue(files)
  }

  fun java(context: BuildContext): BibixValue {
    val destDirectory = context.destDirectory
    if (context.hashChanged) {
      callCompiler(context, listOf("--java_out=${destDirectory.canonicalPath}"))
    }
    return getFiles(destDirectory)
  }

  fun javascript(context: BuildContext): BibixValue {
    val destDirectory = context.destDirectory
    if (context.hashChanged) {
      callCompiler(context, listOf("--js_out=${destDirectory.canonicalPath}"))
    }
    return getFiles(destDirectory)
  }

  fun kotlin(context: BuildContext): BibixValue {
    val destDirectory = context.destDirectory
    if (context.hashChanged) {
      callCompiler(context, listOf("--kotlin_out=${destDirectory.canonicalPath}"))
    }
    return getFiles(destDirectory)
  }

  fun objc(context: BuildContext): BibixValue {
    TODO()
  }

  fun php(context: BuildContext): BibixValue {
    TODO()
  }

  fun python(context: BuildContext): BibixValue {
    TODO()
  }

  fun ruby(context: BuildContext): BibixValue {
    TODO()
  }

  fun dart(context: BuildContext): BibixValue {
    TODO()
  }
}
