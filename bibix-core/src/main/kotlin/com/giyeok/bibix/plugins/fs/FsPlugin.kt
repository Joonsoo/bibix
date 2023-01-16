package com.giyeok.bibix.plugins.fs

import com.giyeok.bibix.plugins.Classes
import com.giyeok.bibix.plugins.PreloadedPlugin

val fsPlugin = PreloadedPlugin.fromScript(
  "",
  """
    def glob(
      pattern: {string, set<string>}
    ): set<file> = native:com.giyeok.bibix.plugins.fs.Glob
  """.trimIndent(),
  Classes(Glob::class.java)
)
