package com.giyeok.bibix.plugins.protobuf

import com.giyeok.bibix.base.BibixValue
import com.giyeok.bibix.base.BuildContext
import com.giyeok.bibix.base.EnumValue
import com.giyeok.bibix.base.StringValue

class ProtocDownloadUrl {
  fun build(context: BuildContext): BibixValue {
    val os = (context.arguments.getValue("os") as EnumValue).value
    val arch = (context.arguments.getValue("arch") as EnumValue).value
    val version = (context.arguments.getValue("protobufVersion") as StringValue).value
    val url = when (os) {
      "linux" -> when (arch) {
        "x86_64" -> "https://github.com/protocolbuffers/protobuf/releases/download/v$version/protoc-$version-linux-x86_64.zip"
        else -> throw IllegalArgumentException("Unsupported arch on linux: $arch")
      }

      "osx" -> when (arch) {
        "aarch_64" -> "https://github.com/protocolbuffers/protobuf/releases/download/v$version/protoc-$version-osx-aarch_64.zip"
        else -> throw IllegalArgumentException("Unsupported arch on osx: $arch")
      }

      else -> throw IllegalArgumentException("Unsupported os: $os")
    }
    return StringValue(url)
  }
}
