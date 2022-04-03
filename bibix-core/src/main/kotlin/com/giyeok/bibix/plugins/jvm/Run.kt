package com.giyeok.bibix.plugins.jvm

import com.giyeok.bibix.base.*

class Run {
  fun run(context: ActionContext) {
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

    val args = mutableListOf("java")
    if (cps.isNotEmpty()) {
      args.add("-cp")
      args.add(cps.joinToString(":") { it.canonicalPath })
    }

    args.add((context.arguments.getValue("mainClass") as StringValue).value)

    val runArgs =
      (context.arguments.getValue("args") as ListValue).values.map { (it as StringValue).value }

    args.addAll(runArgs)

    val process = Runtime.getRuntime().exec(args.toTypedArray())
    println(String(process.inputStream.readAllBytes()))
    println(String(process.errorStream.readAllBytes()))
    process.waitFor()

    check(process.exitValue() == 0)
  }
}
