package com.giyeok.ccflyer

import java.io.File

data class CompileCommand(
  val pwd: File,
  val compiler: String,
  val macroDefs: List<Pair<String, String?>>,
  val includeDirs: List<File>,
  val systemIncludeDirs: List<File>,
  val libDirs: List<File>,
  val libs: List<String>,
  val mt: File?,
  val mf: File?,
  val output: File?,
  val mode: CompileMode,
  val inputs: List<File>,
  val otherOptions: List<String>,
  val warnings: List<String>,
) {
  val macroMap = macroDefs.toMap()

  companion object {
    fun parse(pwd: File, tokens: List<String>): CompileCommand {
      fun fileFromPath(path: String): File =
        (if (path.startsWith('/')) File(path) else File(pwd, path)).normalize().absoluteFile

      val compiler = tokens[0]
      val macroDefs = mutableListOf<Pair<String, String?>>()
      val includeDirs = mutableListOf<File>()
      val systemIncludeDirs = mutableListOf<File>()
      val libDirs = mutableListOf<File>()
      val libs = mutableListOf<String>()
      var mt: File? = null
      var mf: File? = null
      var output: File? = null
      var mode: CompileMode? = null
      val inputs = mutableListOf<File>()
      val otherOptions = mutableListOf<String>()
      val warnings = mutableListOf<String>()

      val iter = tokens.drop(1).iterator()
      while (iter.hasNext()) {
        val token = iter.next()
        when {
          token.isEmpty() -> {
            // do nothing
          }

          token.startsWith("-D") -> {
            if ('=' in token) {
              val macroName = token.substring(2).substringBefore('=')
              val macroValue = token.substring(2).substringAfter('=')
              macroDefs.add(macroName to macroValue)
            } else {
              macroDefs.add(token.substring(2) to null)
            }
          }

          token.startsWith("-I") -> {
            includeDirs.add(fileFromPath(token.substring(2)))
          }

          token.startsWith("-L") -> {
            libDirs.add(fileFromPath(token.substring(2)))
          }

          token.startsWith("-l") -> {
            libs.add(token.substring(2))
          }

          token.startsWith("-isystem") -> {
            systemIncludeDirs.add(fileFromPath(token.substringAfter("-isystem")))
          }

          token.startsWith("-W") -> {
            warnings.add(token.substring(2))
          }

          token == "-MT" -> {
            mt = fileFromPath(iter.next())
          }

          token == "-MF" -> {
            mf = fileFromPath(iter.next())
          }

          token == "-o" -> {
            output = fileFromPath(iter.next())
          }

          token == "-c" -> {
            mode = CompileMode.NO_LINKING
          }

          token.startsWith("-") -> {
            otherOptions.add(token)
          }

          else -> {
            inputs.add(fileFromPath(token))
          }
        }
      }

      return CompileCommand(
        pwd = pwd,
        compiler = compiler,
        macroDefs = macroDefs,
        includeDirs = includeDirs,
        systemIncludeDirs = systemIncludeDirs,
        libDirs = libDirs,
        libs = libs,
        mt = mt,
        mf = mf,
        output = output,
        mode = mode ?: CompileMode.LINKING,
        inputs = inputs,
        otherOptions = otherOptions,
        warnings = warnings
      )
    }
  }
}
