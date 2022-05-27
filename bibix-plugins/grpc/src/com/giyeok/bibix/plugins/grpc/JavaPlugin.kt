package com.giyeok.bibix.plugins.grpc

import com.giyeok.bibix.base.BuildContext
import com.giyeok.bibix.base.EnumValue
import com.giyeok.bibix.base.FileValue
import com.giyeok.bibix.base.StringValue
import java.io.File
import java.nio.file.Files

class JavaPlugin {
  fun url(context: BuildContext): StringValue {
    val version = (context.arguments.getValue("version") as StringValue).value
    val os = (context.arguments.getValue("os") as EnumValue).value
    val arch = (context.arguments.getValue("arch") as EnumValue).value

    val url = when (os) {
      "linux" -> when (arch) {
        "x86_64" -> "https://repo1.maven.org/maven2/io/grpc/protoc-gen-grpc-java/$version/protoc-gen-grpc-java-$version-linux-x86_64.exe"
        else -> throw IllegalArgumentException("Unsupported arch: $arch")
      }
      else -> throw IllegalArgumentException("Unsupported os: $os")
    }
    return StringValue(url)
  }

  fun createEnv(context: BuildContext): FileValue {
    val envDirectory = context.destDirectory
    val pluginPath = File(envDirectory, "protoc-gen-grpc-java")

    if (context.hashChanged) {
      val pluginExe = (context.arguments.getValue("pluginExe") as FileValue).file
      pluginExe.setExecutable(true, true)

      Files.createSymbolicLink(pluginPath.toPath(), pluginExe.absoluteFile.toPath())
    }
    return FileValue(pluginPath)
  }
}
