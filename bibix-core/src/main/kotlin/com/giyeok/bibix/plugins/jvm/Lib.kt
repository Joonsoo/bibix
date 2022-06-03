package com.giyeok.bibix.plugins.jvm

import com.giyeok.bibix.base.*

class Lib {
  fun build(context: BuildContext): BibixValue {
    val path = (context.arguments.getValue("path") as PathValue).path
    return ClassPkg(
      LocalLib(path),
      JarInfo(path, null),
      listOf()
    ).toBibix()
  }
}
