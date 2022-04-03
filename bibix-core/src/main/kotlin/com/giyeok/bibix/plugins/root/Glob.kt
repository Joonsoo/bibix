package com.giyeok.bibix.plugins.root

import com.giyeok.bibix.base.*
import java.nio.file.FileSystems
import java.nio.file.Files

class Glob {
  fun build(context: BuildContext): SetValue {
    val fileSystem = FileSystems.getDefault()
    val matcher = when (val pattern = context.arguments.getValue("pattern")) {
      is StringValue -> {
        val matcherPattern =
          "glob:" + context.callerBaseDirectory.canonicalPath + "/" + pattern.value
        fileSystem.getPathMatcher(matcherPattern)
      }
      is ListValue ->
        TODO()
      else -> throw AssertionError()
    }

    val matched =
      Files.walk(fileSystem.getPath(context.callerBaseDirectory.canonicalPath)).filter { path ->
        matcher.matches(path)
      }
    val matchedList = matched.map { PathValue(it.toFile()) }.toList()
    return SetValue(matchedList)
  }
}
