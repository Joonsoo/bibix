package com.giyeok.bibix.plugins.java

import com.giyeok.bibix.plugins.PluginInstanceProvider
import com.giyeok.bibix.plugins.PreloadedPlugin

val javaPlugin = PreloadedPlugin.fromScript(
  "com.giyeok.bibix.plugins.java",
  """
    import jvm
    import bibix
    import maven

    var jdkVersion: string = "16"
    var srcVersion: string = "16"
    var outVersion: string = "16"

    def library(
      srcs: set<file>,
      deps: set<jvm.ClassPkg> = [],
      compilerPath: string = "javac",
      jdkVersion: string = jdkVersion,
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
  PluginInstanceProvider(
    Library::class.java
  )
)
