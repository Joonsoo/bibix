package com.giyeok.ccflyer

import java.io.File
import java.util.Stack

data class Include(val isQuoteInclude: Boolean, val includePath: String, val trailing: String) {
  companion object {
    fun parse(macros: Map<String, String?>, line: String): Include {
      val body = line.substringAfter("#include ").trim()
      val (path, trailing) = when (body.first()) {
        '"' ->
          body.substring(1).substringBefore('"') to body.substring(1).substringAfter('"')

        '<' ->
          body.substring(1).substringBefore('>') to body.substring(1).substringAfter('>')

        else -> {
          val macroValue = macros[body]!!
          macroValue to ""
        }
      }

      return Include(body.first() == '"', path, trailing)
    }

    fun collectFromFile(macros: Map<String, String?>, file: File): List<Include> {
      val list = mutableListOf<Include>()
      val ifStack = Stack<Boolean>()
      fun currentState(): Boolean = ifStack.all { it }
      for (line in file.readLines()) {
        val trimmed = line.trim()
        when {
          // TODO #define #undef ??
          trimmed.startsWith("#ifdef") -> {
            val macroName = trimmed.substringAfter("#ifdef").trim()
            println("macro ifdef: $macroName")
            ifStack.push(macroName in macros)
          }

          trimmed.startsWith("#ifndef") -> {
            val macroName = trimmed.substringAfter("#ifndef").trim()
            println("macro ifndef: $macroName")
            ifStack.push(macroName !in macros)
          }

          trimmed.startsWith("#if") -> {
            val macroExpr = trimmed.substringAfter("#if").trim()
            println("macro if: $macroExpr")
            ifStack.push(MacroExprEvaluator(macros).evaluateBoolean(macroExpr))
          }

          trimmed.startsWith("#else") -> {
            val latestStatus = ifStack.pop()
            ifStack.push(!latestStatus)
          }

          trimmed.startsWith("#endif") -> {
            ifStack.pop()
          }

          currentState() -> {
            if (trimmed.startsWith("#include ")) {
              list.add(parse(macros, line))
            }
          }

          else -> {
            // do nothing
          }
        }
      }
      return list
    }
  }
}

data class MacroExprEvaluator(val macros: Map<String, String?>) {
  fun evaluateBoolean(expr: String): Boolean {
    return expr in macros
  }
}

data class HeaderLookupCtx(val includeDirs: List<File>, val systemIncludeDirs: List<File>) {
  fun lookup(file: File, include: Include): File? {
    val fromFile = file.parentFile.resolve(include.includePath)
    if (fromFile.exists()) {
      return fromFile
    }
    val fromIncludeDirs = includeDirs.map { includeDir -> includeDir.resolve(include.includePath) }
      .firstOrNull { it.exists() }
    if (fromIncludeDirs != null) {
      return fromIncludeDirs
    }
    return null
  }
}
