package com.giyeok.bibix.plugins.protobuf

import com.giyeok.bibix.base.*
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

class Compile {
  data class ProtoSchema(
    val schemaFiles: List<File>,
    val includes: List<File>,
  ) {
    // TODO CName 대신 이 rule이 정의된 bbx 스크립트 한정의 Name
    companion object {
      fun fromBibix(value: BibixValue): ProtoSchema {
        value as ClassInstanceValue
        check(value.className.tokens == listOf("ProtoSchema"))
        val body = value.value as NamedTupleValue
        val schemaFiles =
          (body.getValue("schemaFiles") as SetValue).values.map { (it as FileValue).file }
        val includes =
          (body.getValue("includes") as SetValue).values.map { (it as PathValue).path }
        return ProtoSchema(schemaFiles, includes)
      }
    }

    fun toBibix(sourceId: SourceId) = ClassInstanceValue(
      CName(sourceId, "ProtoSchema"),
      NamedTupleValue(
        "schemaFiles" to SetValue(schemaFiles.map { FileValue(it) }),
        "includes" to SetValue(includes.map { PathValue(it) }),
      )
    )
  }

  fun schema(context: BuildContext): BibixValue {
    val srcs = context.arguments.getValue("srcs") as SetValue
    val deps = (context.arguments.getValue("deps") as SetValue).values.map {
      ProtoSchema.fromBibix(it)
    }
    val protocPath = (context.arguments.getValue("protocPath") as DirectoryValue).directory

    val mergedIncludes = listOf(PathValue(File(protocPath, "include"))) +
      deps.flatMap { dep ->
        // schema 파일의 parent를 사용함. protoc가 include는 디렉토리를 받기 때문에..
        // 파일 이름이 충돌하면 문제가 생길텐데 일단은 그냥 이렇게 해둬야지..
        dep.schemaFiles.map { PathValue(it.parentFile) } + dep.includes.map { PathValue(it) }
      }

    return ClassInstanceValue(
      CName(context.sourceId, "ProtoSchema"),
      NamedTupleValue(
        "schemaFiles" to srcs,
        "includes" to SetValue(mergedIncludes)
      )
    )
  }

  private fun callCompiler(context: BuildContext, outArgs: List<String>) {
    // TODO Skip compiling if !hashChanged
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

    context.progressLogger.logInfo(String(process.inputStream.readAllBytes()))
    context.progressLogger.logError(String(process.errorStream.readAllBytes()))
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
