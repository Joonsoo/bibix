package com.giyeok.bibix.intellij.service

import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries

object ResourceDirectoryExtractor {
  fun allFilesOf(directory: Path): Set<Path> =
    directory.listDirectoryEntries().flatMap { sub ->
      if (sub.isDirectory()) {
        allFilesOf(sub)
      } else {
        listOf(sub)
      }
    }.toSet()

  fun findResourceDirectoriesOf(paths: Collection<Path>): Set<Path> {
    val mutPaths = paths.toMutableSet()
    val resDirs = mutableSetOf<Path>()
    while (mutPaths.isNotEmpty()) {
      val path = mutPaths.first()
      val directory = path.parent
      val dirFiles = allFilesOf(directory)
      if (paths.containsAll(dirFiles)) {
        resDirs.removeIf { it.startsWith(directory) }
        resDirs.add(directory)
        mutPaths.removeAll(dirFiles)
      } else {
        resDirs.add(path)
      }
      mutPaths.remove(path)
    }
    return resDirs
  }
}
