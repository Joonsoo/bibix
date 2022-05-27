package com.giyeok.bibix.plugins.grpc

import com.giyeok.bibix.base.*
import java.io.File

class KotlinPlugin {
  fun url(context: BuildContext): StringValue {
    val version = (context.arguments.getValue("version") as StringValue).value
    val url =
      "https://repo1.maven.org/maven2/io/grpc/protoc-gen-grpc-kotlin/$version/protoc-gen-grpc-kotlin-$version-jdk7.jar"

    return StringValue(url)
  }

  fun createEnv(context: BuildContext): FileValue {
    val envDirectory = context.destDirectory
    val pluginPath = File(envDirectory, "protoc-gen-grpc-kotlin")

    if (context.hashChanged) {
      val pluginJar = (context.arguments.getValue("pluginJar") as FileValue).file

      pluginPath.writeText(
        """
        #!/bin/sh
        java -jar ${pluginJar.absolutePath}
        """.trimIndent()
      )
      pluginPath.setExecutable(true, true)
    }
    return FileValue(pluginPath)
  }
}
