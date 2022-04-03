package com.giyeok.bibix.plugins.jvm

import com.giyeok.bibix.base.BibixValue
import com.giyeok.bibix.base.BuildContext
import com.giyeok.bibix.base.PathValue
import com.giyeok.bibix.base.SetValue

class Lib {
  fun build(context: BuildContext): BibixValue {
    val path = context.arguments.getValue("path") as PathValue
    return SetValue(path)
  }
}
