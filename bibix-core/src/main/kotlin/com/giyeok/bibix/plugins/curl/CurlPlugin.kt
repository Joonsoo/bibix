package com.giyeok.bibix.plugins.curl

import com.giyeok.bibix.plugins.PluginInstanceProvider
import com.giyeok.bibix.plugins.PreloadedPlugin

val curlPlugin = PreloadedPlugin.fromScript(
  "com.giyeok.bibix.plugins.curl",
  """
    def download(
      url: string,
      filename?: string,
    ): file = native:com.giyeok.bibix.plugins.curl.Download
  """.trimIndent(),
  PluginInstanceProvider(
    Download::class.java
  )
)
