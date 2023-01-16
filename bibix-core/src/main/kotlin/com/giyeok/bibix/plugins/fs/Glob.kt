package com.giyeok.bibix.plugins.fs

import com.giyeok.bibix.base.*
import java.nio.file.Files
import kotlin.io.path.absolutePathString

class Glob {
  fun build(context: BuildContext): SetValue {
    val fileSystem = context.fileSystem
    val matcher = when (val pattern = context.arguments.getValue("pattern")) {
      is StringValue -> {
        val matcherPattern =
          "glob:" + context.callerBaseDirectory!!.absolutePathString() + "/" + pattern.value
        fileSystem.getPathMatcher(matcherPattern)
      }
      is SetValue -> {
        val patterns = pattern.values.map { (it as StringValue).value }
        TODO()
      }
      else -> throw AssertionError()
    }

    val matched = Files.walk(context.callerBaseDirectory).filter { path ->
      matcher.matches(path)
    }
    val matchedList = matched.map { PathValue(it) }.toList()
    return SetValue(matchedList)
  }
}
