package com.giyeok.bibix.plugins.jvm

import com.giyeok.bibix.base.*

class Lib {
  fun build(context: BuildContext): BibixValue {
    val path = (context.arguments.getValue("path") as PathValue).path
    val deps =
      (context.arguments.getValue("deps") as SetValue).values.map { ClassPkg.fromBibix(it) }
    return ClassPkg(LocalLib(path), JarInfo(path, null), deps).toBibix()
  }
}
