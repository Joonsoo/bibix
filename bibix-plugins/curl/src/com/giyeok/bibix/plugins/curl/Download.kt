package com.giyeok.bibix.plugins.curl

import com.giyeok.bibix.base.BuildContext
import com.giyeok.bibix.base.FileValue
import com.giyeok.bibix.base.StringValue
import java.io.File
import java.net.URL

class Download {
  fun build(context: BuildContext): FileValue {
    val url = URL((context.arguments.getValue("url") as StringValue).value)
    val filename = (context.arguments["filename"] as? StringValue)?.value ?: File(url.file).name
    val destFile = File(context.destDirectory, filename)
    if (context.hashChanged) {
      copyStreamToFile(url.openStream().buffered(), destFile)
    }
    return FileValue(destFile)
  }
}
