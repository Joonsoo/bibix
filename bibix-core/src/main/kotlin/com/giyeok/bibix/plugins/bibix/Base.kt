package com.giyeok.bibix.plugins.bibix

import com.giyeok.bibix.base.*

class Base {
  fun build(context: BuildContext): BibixValue {
    // TODO jar로 묶었으면 그 jar가 cp로 들어가면 될듯?
    val classpath = context.arguments.getValue("classpath") as PathValue
    return TupleValue(
      ClassInstanceValue(
        CName(BibixInternalSourceId("jvm"), "LocalLib"),
        classpath,
      ),
      SetValue(classpath),
      SetValue()
    )
  }
}
