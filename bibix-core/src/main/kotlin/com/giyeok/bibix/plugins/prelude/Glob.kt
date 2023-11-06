package com.giyeok.bibix.plugins.prelude

import com.giyeok.bibix.base.BuildContext
import com.giyeok.bibix.base.PathValue
import com.giyeok.bibix.base.SetValue
import com.giyeok.bibix.base.StringValue
import java.nio.file.Files
import kotlin.io.path.absolute
import kotlin.io.path.absolutePathString
import kotlin.io.path.pathString

class Glob {
  fun build(context: BuildContext): SetValue {
    val fileSystem = context.fileSystem
    val matcher = when (val pattern = context.arguments.getValue("pattern")) {
      is StringValue -> {
        val matcherPattern = if (pattern.value.startsWith('/')) {
          "glob:${pattern.value}"
        } else {
          "glob:" + (context.callerBaseDirectory!!.absolutePathString() + "/" + pattern.value)
        }
        fileSystem.getPathMatcher(matcherPattern)
      }

      is SetValue -> {
        val patterns = pattern.values.map { (it as StringValue).value }
        TODO()
      }

      else -> throw AssertionError()
    }

    val matched = Files.walk(context.callerBaseDirectory).filter { path ->
      matcher.matches(path.absolute())
    }
    val matchedList = matched.map { PathValue(it) }.toList()
    return SetValue(matchedList)
  }
}
