package com.giyeok.bibix.plugins.root

import com.giyeok.bibix.plugins.Classes
import com.giyeok.bibix.runner.BibixPlugin

val rootScript = BibixPlugin.fromScript(
  """
    class GitSource = (url: string, ref: string, path: string)
    
    class Env = (os: OS, arch: Arch)
    
    enum OS {
      linux,
      osx,
      windows,
    }
    
    enum Arch {
      x86,
      x86_64,
      aarch_64,
    }
    
    def git(
      url: string,
      tag?: string,
      branch?: string,
      ref?: string,
      path: string = "/",
    ): GitSource = native:com.giyeok.bibix.plugins.root.Git
    
    def glob(
      pattern: {string, set<string>}
    ): set<path> = native:com.giyeok.bibix.plugins.root.Glob
  """.trimIndent(),
  Classes(
    Git::class.java,
    Glob::class.java,
  )
)
