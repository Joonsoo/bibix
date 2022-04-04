package com.giyeok.bibix.plugins.java

import com.giyeok.bibix.base.*

class Library {
  fun build(context: BuildContext): BuildRuleReturn {
    return BuildRuleReturn.evalAndThen(
      "import jvm",
      "jvm.resolveClassPkgs",
      mapOf("classPkgs" to context.arguments.getValue("deps"))
    ) { pair ->
      val (cps, pkgSet) = (pair as TupleValue).values

      cps as SetValue // set<path>
      pkgSet as SetValue // set<ClassPkg>

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
          StringValue("local build by java.library"),
          SetValue(listOf(DirectoryValue(dest))),
          pkgSet,
        )
      )
    }
  }

  fun run(context: ActionContext) {
    TODO()
  }
}
