package com.giyeok.bibix.plugins.jvm

import com.giyeok.bibix.base.*

class Lib {
  fun build(context: BuildContext): BibixValue {
    val path = (context.arguments.getValue("path") as PathValue).path
    val deps =
      (context.arguments.getValue("deps") as SetValue).values.map { ClassPkg.fromBibix(it) }
    val runtimeDeps =
      (context.arguments.getValue("runtimeDeps") as SetValue).values.map { ClassPkg.fromBibix(it) }
    val sourceJar = context.arguments.getValue("srcJar").let { srcs ->
      if (srcs == NoneValue) null else (srcs as FileValue).file
    }
    val nativeLibDirs = (context.arguments.getValue("nativeLibDirs") as SetValue).values.map {
      (it as DirectoryValue).directory
    }
    return ClassPkg(
      origin = LocalLib(path),
      cpinfo = JarInfo(path, sourceJar),
      deps = deps,
      runtimeDeps = runtimeDeps,
      nativeLibDirs = nativeLibDirs
    ).toBibix()
  }
}
