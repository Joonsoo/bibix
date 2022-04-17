package com.giyeok.bibix.plugins.bibix

import com.giyeok.bibix.base.*
import java.io.File

class Base {
  fun build(context: BuildContext): BibixValue {
    // TODO jar로 묶었으면 그 jar가 cp로 들어가면 될듯?
    val path = context.arguments.getValue("jar") as FileValue
    return TupleValue(
      ClassInstanceValue(
        CName(BibixInternalSourceId("jvm"), "LocalLib"),
        path,
      ),
      SetValue(path),
      SetValue()
    )
  }
}
