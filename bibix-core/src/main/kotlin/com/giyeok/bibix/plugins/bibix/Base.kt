package com.giyeok.bibix.plugins.bibix

import com.giyeok.bibix.base.*
import java.io.File

class Base {
  fun build(context: BuildContext): BibixValue {
    // TODO jar로 묶었으면 그 jar가 cp로 들어가면 될듯?
    return TupleValue(
      SetValue(PathValue(File("bibix-core/build/classes/kotlin/main"))),
      SetValue()
    )
  }
}
