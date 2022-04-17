package com.giyeok.bibix.plugins.curl

import com.giyeok.bibix.plugins.Classes
import com.giyeok.bibix.runner.BibixPlugin

val curlPlugin = BibixPlugin.fromScript(
  """
    def download(
      url: string,
      filename?: string,
    ): file = native:com.giyeok.bibix.plugins.curl.Download
  """.trimIndent(),
  Classes(
    Download::class.java
  )
)
