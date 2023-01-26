package com.giyeok.bibix.plugins.prelude

import com.giyeok.bibix.plugins.Classes
import com.giyeok.bibix.plugins.PreloadedPlugin

val preludePlugin = PreloadedPlugin.fromScript(
  "com.giyeok.bibix.prelude",
  // import bibix는 main script에서 별도로 "import bibix" 하지 않아도 bibix를 쓸 수 있도록 하기 위한 것
  """
    import bibix

    class Env(os: OS, arch: Arch)
    
    super class OS { Linux, OSX, Windows }
    class Linux()
    class OSX()
    class Windows()
    
    enum Arch {
      unknown,
      x86,
      x86_64,
      aarch_64,
    }
    
    class BibixProject(projectRoot: directory, scriptName?: string)
    
    def git(
      url: string,
      tag?: string,
      branch?: string,
      ref?: string,
      path: string = "",
      scriptName?: string,
    ): BibixProject = native:com.giyeok.bibix.plugins.prelude.Git

    def glob(
      pattern: {string, set<string>}
    ): set<file> = native:com.giyeok.bibix.plugins.prelude.Glob
    
    env = currentEnv()
    
    def currentEnv(): Env = native:com.giyeok.bibix.plugins.prelude.CurrentEnv
  """.trimIndent(),
  Classes(
    Glob::class.java,
    Git::class.java,
    CurrentEnv::class.java,
  )
)
