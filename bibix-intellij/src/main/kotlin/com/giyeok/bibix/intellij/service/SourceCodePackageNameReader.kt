package com.giyeok.bibix.intellij.service

import java.io.Reader

class SourceCodePackageNameReader(val reader: Reader) {
  companion object {
    fun readPackageName(srcReaderStream: Reader): List<String>? {
      val reader = SourceCodePackageNameReader(srcReaderStream)
      return try {
        reader.skipWhitespace()
        var nextToken = reader.nextToken()
        if (nextToken == "@") {
          reader.skipWhitespace()
          TODO()
        } else if (nextToken == "package") {
          reader.skipWhitespace()
          val tokens = mutableListOf<String>()
          tokens.add(reader.nextNameToken())
          while (reader.nextToken() == ".") {
            tokens.add(reader.nextNameToken())
          }
          tokens
        } else {
          null
        }
      } catch (e: Exception) {
        null
      }
    }
  }

  private val whitespaces = setOf(' ', '\b', '\n', '\r', '\t')
  private val ZERO = 0.toChar()

  private var lastChar: Char? = null
  private var lastCharWasUnconsumed: Boolean = false

  private fun nextChar(): Char =
    if (lastChar != null && lastCharWasUnconsumed) {
      val c = lastChar!!
      lastCharWasUnconsumed = false
      c
    } else {
      lastChar = reader.read().toChar()
      lastCharWasUnconsumed = false
      lastChar!!
    }

  private fun unconsumeLastChar() {
    checkNotNull(lastChar)
    // 두번은 안됨
    check(!lastCharWasUnconsumed)
    lastCharWasUnconsumed = true
  }

  fun nextLineCommentBody(): String {
    val builder = StringBuilder()
    var char = nextChar()
    while (char >= ZERO && char != '\n' && char != '\r') {
      builder.append(char)
      char = nextChar()
    }
    builder.append(char)
    return builder.toString()
  }

  fun nextBlockCommentBody(): String {
    val builder = StringBuilder()
    var char = nextChar()
    while (char >= ZERO && char != '\n' && char != '\r') {
      builder.append(char)
      if (char == '*') {
        char = nextChar()
        if (char == '/') {
          builder.append(char)
          return builder.toString()
        }
      }
      char = nextChar()
    }
    throw IllegalStateException("Unexpected end of block comment")
  }

  fun skipWhitespace() {
    var char = nextChar()
    while (whitespaces.contains(char) || char == '/') {
      if (char == '/') {
        when (nextChar()) {
          '/' -> nextLineCommentBody()
          '*' -> nextBlockCommentBody()
          else -> throw IllegalStateException("Invalid file")
        }
      }
      char = nextChar()
    }
    unconsumeLastChar()
  }

  fun nextToken(): String {
    val char = nextChar()
    return if (('a'..'z').contains(char) || ('A'..'Z').contains(char) || char == '_') {
      unconsumeLastChar()
      nextNameToken()
    } else {
      char.toString()
    }
  }

  fun nextNameToken(): String {
    val builder = StringBuilder()
    var char = nextChar()
    var backticked = false
    if (char == '`') {
      char = nextChar()
      backticked = true
    }
    check(('a'..'z').contains(char) || ('A'..'Z').contains(char) || char == '_')
    while (('a'..'z').contains(char) || ('A'..'Z').contains(char) ||
      char == '_' || ('0'..'9').contains(char)
    ) {
      builder.append(char)
      char = nextChar()
    }
    if (backticked) {
      check(char == '`')
    } else {
      unconsumeLastChar()
    }
    return builder.toString()
  }
}