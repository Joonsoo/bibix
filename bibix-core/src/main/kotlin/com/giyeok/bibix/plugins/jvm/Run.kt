package com.giyeok.bibix.plugins.jvm

import com.giyeok.bibix.base.*

class Run {
  fun run(context: ActionContext): BuildRuleReturn {
    val deps = context.arguments.getValue("deps")
    return BuildRuleReturn.evalAndThen(
      "resolveClassPkgs",
      mapOf("classPkgs" to deps)
    ) { classPaths ->
      val cps = ((classPaths as ClassInstanceValue).value as SetValue).values.map {
        (it as PathValue).path
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
      BuildRuleReturn.done()
    }
  }
}
