package com.giyeok.bibix.plugins.root

import com.giyeok.bibix.plugins.Classes
import com.giyeok.bibix.plugins.PreloadedPlugin

val rootScript = PreloadedPlugin.fromScript(
  "com.giyeok.bibix.plugins.root",
  """
    class Env(os: OS, arch: Arch)
    
    enum OS {
      unknown,
      linux,
      osx,
      windows,
    }
    
    enum Arch {
      unknown,
      x86,
      x86_64,
      aarch_64,
    }
    
    
    def glob(
      pattern: {string, set<string>}
    ): set<path> = native:com.giyeok.bibix.plugins.root.Glob
  """.trimIndent(),
  Classes(
    Glob::class.java,
  )
)
