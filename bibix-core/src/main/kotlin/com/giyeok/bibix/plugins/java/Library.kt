package com.giyeok.bibix.plugins.java

import com.giyeok.bibix.base.*

class Library {
  fun build(context: BuildContext): BuildRuleReturn {
    val deps = context.arguments.getValue("deps")
    return BuildRuleReturn.evalAndThen(
      "jvm.resolveClassPkgs",
      mapOf("classPkgs" to deps)
    ) { classPaths ->
      val cps = (classPaths as ClassInstanceValue).value as SetValue // set<path>

      val dest = context.destDirectory
      if (context.hashChanged) {
        val srcs = (context.arguments["srcs"]!! as SetValue).values.map { src ->
          (src as FileValue).file
        }

        val args = mutableListOf("javac")
        if (cps.values.isNotEmpty()) {
          args.add("-cp")
          args.add(cps.values.joinToString(":") { (it as PathValue).path.canonicalPath })
        }
        args.add("-d")
        args.add(dest.canonicalPath)
        args.addAll(srcs.map { it.canonicalPath })

        val process = Runtime.getRuntime().exec(args.toTypedArray())
        println(String(process.errorStream.readAllBytes()))
        process.waitFor()

        check(process.exitValue() == 0)
      }
      // ClassPkg = (origin: ClassOrigin, cps: set<path>, deps: set<ClassPkg>)
      BuildRuleReturn.value(
        TupleValue(
          StringValue("built by java.library: ${context.objectIdHash}"),
          SetValue(listOf(DirectoryValue(dest))),
          deps,
        )
      )
    }
  }

  fun run(context: ActionContext) {
    TODO()
  }
}
