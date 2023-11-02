package com.giyeok.bibix.graph

import java.nio.file.Path
import kotlin.io.path.absolute
import kotlin.io.path.readText

data class BibixProjectLocation(val projectRoot: Path, val scriptName: String) {
  constructor(projectRoot: Path): this(projectRoot.normalize().absolute(), "build.bbx")

  init {
    check(projectRoot.normalize() == projectRoot && projectRoot.isAbsolute)
  }

  fun readScript(): String = projectRoot.resolve(scriptName).readText()
}
