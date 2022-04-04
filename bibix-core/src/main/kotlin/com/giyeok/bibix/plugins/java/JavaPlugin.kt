package com.giyeok.bibix.plugins.java

import com.giyeok.bibix.plugins.Classes
import com.giyeok.bibix.runner.BibixPlugin

val javaPlugin = BibixPlugin.fromScript(
  """
    import jvm
    
    arg srcVersion: string = "16"
    arg outVersion: string = "16"
    
    def library(
      srcs: set<file>,
      deps: set<jvm.ClassPkg> = [],
      compilerPath: string = "javac",
      srcVersion: string = srcVersion,
      outVersion: string = outVersion,
    ): jvm.ClassPkg = native:com.giyeok.bibix.plugins.java.Library
    
    action def test(
      srcs: set<file>,
      deps: set<jvm.ClassPkg> = [],
    ) = native:com.giyeok.bibix.plugins.java.Test
    
    action def execute(
      deps: set<jvm.ClassPkg>,
      mainClass: string,
    ) = native:com.giyeok.bibix.plugins.java.Execute
  """.trimIndent(),
  Classes(
    Library::class.java,
  ),
)

// rule은 build(ctx: PluginRunContext): BibixValue 메소드가 있어야 하고
// action은 run(ctx: PluginRunContext): Unit 메소드가 있어야 함
