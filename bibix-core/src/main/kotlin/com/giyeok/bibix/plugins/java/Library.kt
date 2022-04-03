package com.giyeok.bibix.plugins.java

import com.giyeok.bibix.base.*

class Library {
  fun build(context: BuildContext): BibixValue {
    val dest = context.destDirectory
    val cps = (context.arguments["deps"]!! as SetValue).values.flatMap { dep ->
      ((dep as ClassInstanceValue).value as SetValue).values.map { fd ->
        when (fd) {
          is FileValue -> fd.file
          is DirectoryValue -> fd.directory
          is PathValue -> fd.path
          else -> throw AssertionError()
        }
      }
    }
    if (context.hashChanged) {
      val srcs = (context.arguments["srcs"]!! as SetValue).values.map { src ->
        (src as FileValue).file
      }

      val args = mutableListOf("javac")
      if (cps.isNotEmpty()) {
        args.add("-cp")
        args.add(cps.joinToString(":") { it.canonicalPath })
      }
      args.add("-d")
      args.add(dest.canonicalPath)
      args.addAll(srcs.map { it.canonicalPath })

      val process = Runtime.getRuntime().exec(args.toTypedArray())
      println(String(process.errorStream.readAllBytes()))
      process.waitFor()

      check(process.exitValue() == 0)
    }
    return TupleValue(SetValue(listOf(DirectoryValue(dest))), SetValue(cps.map { PathValue(it) }))
  }

  fun run(context: ActionContext) {
    TODO()
  }
}
