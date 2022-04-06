package com.giyeok.bibix.plugins.jvm

import com.giyeok.bibix.base.*

class Lib {
  fun build(context: BuildContext): BibixValue {
    val path = context.arguments.getValue("path") as PathValue
    return TupleValue(
      path,
      SetValue(path),
      SetValue()
    )
  }
}
