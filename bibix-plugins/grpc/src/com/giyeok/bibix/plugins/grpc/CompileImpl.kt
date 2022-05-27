package com.giyeok.bibix.plugins.grpc

import com.giyeok.bibix.base.*
import com.giyeok.bibix.plugins.grpc.Compile.ProtoSchema
import com.giyeok.bibix.plugins.grpc.Compile.OS
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

class CompileImpl : CompileInterface {
  private fun callCompiler(context: BuildContext, pluginArgs: List<String>) {
    // TODO Skip compiling if !hashChanged
    val os = (context.arguments.getValue("os") as EnumValue).value
    val protocPath = (context.arguments.getValue("protocPath") as DirectoryValue).directory

    val schema =
      ((context.arguments.getValue("schema") as ClassInstanceValue).value as NamedTupleValue).pairs
    val srcs = (schema[0].second as SetValue).values.map { (it as FileValue).file }
    val includes = (schema[1].second as SetValue).values.map { (it as PathValue).path }

    val srcArgs = mutableListOf<String>()
    srcs.forEach { srcArgs.add(it.canonicalPath) }

    val protoPaths = srcs.map { it.parentFile }.toSet() + includes
    protoPaths.forEach { srcArgs.add("-I${it.canonicalPath}") }

    val executableName = if (os == "windows") "protoc.exe" else "protoc"
    val executableFile = File(File(protocPath, "bin"), executableName)

    executableFile.setExecutable(true)

    val runArgs = listOf(executableFile.canonicalPath) + srcArgs + pluginArgs
    val process = Runtime.getRuntime()
      .exec(runArgs.toTypedArray(), arrayOf(), File(protocPath, "bin").canonicalFile)

    context.progressLogger.logInfo(String(process.inputStream.readAllBytes()))
    context.progressLogger.logError(String(process.errorStream.readAllBytes()))
    process.waitFor()

    check(process.exitValue() == 0)
  }

  private fun getFiles(directory: File): SetValue {
    val files = Files.walk(Path.of(directory.toURI()), 1000).toList()
      .map { it.toFile() }
      .filter { it.isFile }
      .map { FileValue(it) }
    return SetValue(files)
  }

  override fun java(
    context: BuildContext,
    schema: ProtoSchema,
    protocPath: File,
    pluginPath: File,
    os: OS,
  ): BuildRuleReturn {
    val destDirectory = context.destDirectory
    if (context.hashChanged) {
      callCompiler(
        context, listOf(
          "--plugin=${pluginPath.absolutePath}",
          "--grpc-java_out=${destDirectory.absolutePath}"
        )
      )
    }
    return BuildRuleReturn.value(getFiles(destDirectory))
  }

  override fun kotlin(
    context: BuildContext,
    schema: ProtoSchema,
    protocPath: File,
    pluginPath: File,
    os: OS,
  ): BuildRuleReturn {
    val destDirectory = context.destDirectory
    if (context.hashChanged) {
      callCompiler(
        context, listOf(
          "--plugin=${pluginPath.absolutePath}",
          "--grpc-kotlin_out=${destDirectory.absolutePath}"
        )
      )
    }
    return BuildRuleReturn.value(getFiles(destDirectory))
  }

  override fun web(
    context: BuildContext,
    schema: ProtoSchema,
    protocPath: File,
    pluginPath: File,
    os: OS,
  ): BuildRuleReturn {
    TODO()
  }
}
