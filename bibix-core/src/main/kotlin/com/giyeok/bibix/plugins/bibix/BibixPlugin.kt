package com.giyeok.bibix.plugins.bibix

import com.giyeok.bibix.plugins.Classes
import com.giyeok.bibix.runner.BibixPlugin

val bibixPlugin = BibixPlugin.fromScript(
  """
    import jvm
    
    def base(
      ref: string = "HEAD"
    ): jvm.Classes = native:com.giyeok.bibix.plugins.bibix.Base
  """.trimIndent(),
  Classes(
    Base::class.java
  )
)
