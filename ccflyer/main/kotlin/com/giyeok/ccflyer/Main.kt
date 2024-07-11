package com.giyeok.ccflyer

import java.io.File

fun main() {
  val ninjalog = File("/home/joonsoo/Documents/workspace/llvm-project/build/ninjalog").readLines()
    .filter { it.startsWith('[') }

  val compileCommands = mutableListOf<CompileCommand>()
  var pwd = File("/home/joonsoo/Documents/workspace/llvm-project/build")
  for (line in ninjalog) {
    val commandLine = line.substringAfter("] ")
    val commands = commandLine.split("&&")
    for (command in commands) {
      val tokens = splitCommand(command)
      val commandName = tokens.first().substringAfterLast('/')
      when {
        commandName in setOf("c++", "g++", "gcc", "cc") -> {
          compileCommands.add(CompileCommand.parse(pwd, tokens))
        }

        commandName == "cd" -> {
          check(tokens.size == 2)
          pwd = File(tokens[1])
        }

        commandName == ":" || commandName.endsWith("cmake") -> {
          // do nothing
        }

        commandName == "ar" -> {
          // TODO
        }

        commandName == "ranlib" -> {
          // TODO
        }

        commandName in setOf("llvm-tblgen", "llvm-min-tblgen", "mlir-tblgen") -> {
          // TODO
        }

        commandName == "mlir-linalg-ods-yaml-gen" -> {
          // TODO
        }

        commandName == "mlir-src-sharder" -> {
          // TODO
        }

        commandName == "mlir-pdll" -> {
          // TODO
        }

        commandName == "python3.10" -> {
          // TODO
        }

        else -> {
          println(commandName)
        }
      }
    }
  }

  for (command in compileCommands) {
    val deps = command.inputs.mapNotNull { input ->
      compileCommands.find { it.output!! == input }
    }
    if (deps.isNotEmpty()) {
      println("${command.output!!.name} depends on:")
      for (dep in deps) {
        println("  ${dep.output!!}")
      }
      println()
    }
    val srcExts = setOf("c", "cc", "cpp")
    val hdrExts = setOf("h", "hpp")
    val codes = command.inputs.filter { it.extension in srcExts || it.extension in hdrExts }
    if (codes.isNotEmpty()) {
      println("${command.output!!.name} sources:")
      (command.includeDirs + command.systemIncludeDirs).forEach { includeDir ->
        println("  -I $includeDir")
      }
      println()
      val lookupCtx = HeaderLookupCtx(command.includeDirs, command.systemIncludeDirs)
      for (code in codes) {
        println("  ${code.absolutePath} includes:")
        val includes = Include.collectFromFile(command.macroMap, code)
        for (include in includes) {
          val referring = lookupCtx.lookup(code, include)
          println("    ${include.isQuoteInclude} ${include.includePath} -> ${referring?.absolutePath}")
          if (include.isQuoteInclude && referring == null) {
            println("***")
          }
        }
      }
    }
  }

  println(compileCommands.size)
  println(compileCommands.flatMap { it.inputs.map { it.extension } }.distinct().sorted())
}

fun splitCommand(command: String): List<String> =
  command.split(' ').filter { it.isNotEmpty() }

enum class CompileMode {
  COMPILE_ONLY, NO_LINKING, LINKING
}
