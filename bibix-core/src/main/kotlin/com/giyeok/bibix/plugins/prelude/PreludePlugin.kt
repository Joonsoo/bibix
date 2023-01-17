package com.giyeok.bibix.plugins.prelude

import com.giyeok.bibix.plugins.Classes
import com.giyeok.bibix.plugins.PreloadedPlugin

val preludePlugin = PreloadedPlugin.fromScript(
  "com.giyeok.bibix.prelude",
  """
    import bibix
    
    def glob(
      pattern: {string, set<string>}
    ): set<file> = native:com.giyeok.bibix.plugins.prelude.Glob
  """.trimIndent(),
  Classes(Glob::class.java)
)
