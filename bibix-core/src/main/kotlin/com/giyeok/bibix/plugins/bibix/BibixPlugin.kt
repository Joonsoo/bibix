package com.giyeok.bibix.plugins.bibix

import com.giyeok.bibix.plugins.Classes
import com.giyeok.bibix.runner.BibixPlugin

val dollar = "\$"
val bibixPlugin = BibixPlugin.fromScript(
  """
    import jvm
    import curl
    
    arg bibixVersion: string = "0.0.2"
    
    def base(
      ref: string = curl.download("https://github.com/Joonsoo/bibix/releases/download/${dollar}bibixVersion/bibix-${dollar}bibixVersion.jar")
    ): jvm.ClassPkg = native:com.giyeok.bibix.plugins.bibix.Base
  """.trimIndent(),
  Classes(
    Base::class.java
  )
)
