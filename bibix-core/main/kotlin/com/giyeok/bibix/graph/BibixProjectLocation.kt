package com.giyeok.bibix.graph

import java.nio.file.Path
import kotlin.io.path.absolute
import kotlin.io.path.readText

data class BibixProjectLocation(val projectRoot: Path, val scriptName: String) {
  constructor(projectRoot: Path): this(projectRoot.normalize().absolute(), "build.bbx")

  companion object {
    fun of(projectRoot: Path, scriptName: String?) =
      if (scriptName == null) {
        BibixProjectLocation(projectRoot)
      } else {
        BibixProjectLocation(projectRoot, scriptName)
      }
  }

  init {
    check(projectRoot.normalize() == projectRoot && projectRoot.isAbsolute)
  }

  fun readScript(): String = projectRoot.resolve(scriptName).readText()
}
