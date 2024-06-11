package com.giyeok.bibix.plugins.bibix

import com.giyeok.bibix.base.*
import com.giyeok.bibix.plugins.jvm.ClassPkg
import com.giyeok.bibix.plugins.jvm.JarInfo
import com.giyeok.bibix.plugins.jvm.LocalLib

class Base {
  fun build(context: BuildContext): BibixValue {
    // TODO jar로 묶었으면 그 jar가 cp로 들어가면 될듯?
    val classpath = (context.arguments.getValue("classpath") as PathValue).path
//    val classpath = context.fileSystem.getPath("/home/joonsoo/Documents/workspace/bibix/bibix-base-0.0.4.jar")
    return ClassPkg(
      LocalLib(classpath),
      JarInfo(classpath, null),
      listOf(),
      listOf(),
      listOf()
    ).toBibix()
  }
}
