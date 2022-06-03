package com.giyeok.bibix.plugins.java

import com.giyeok.bibix.base.*
import com.giyeok.bibix.plugins.ClassPkg
import com.giyeok.bibix.plugins.ClassesInfo
import com.giyeok.bibix.plugins.LocalBuilt
import java.nio.file.Path
import kotlin.io.path.absolutePathString

class Library {
  private fun built(objectIdHash: String, dest: Path, deps: List<ClassPkg>): BuildRuleReturn =
    BuildRuleReturn.value(
      ClassPkg(
        LocalBuilt(objectIdHash /* TODO built by java.library */),
        ClassesInfo(listOf(dest), listOf(), null),
        deps,
      ).toBibix()
    )

  fun build(context: BuildContext): BuildRuleReturn {
    val depsValue = context.arguments.getValue("deps") as SetValue
    val deps = depsValue.values.map { ClassPkg.fromBibix(it) }
    val dest = context.destDirectory

    if (!context.hashChanged) {
      return built(context.objectIdHash, dest, deps)
    }

    return BuildRuleReturn.evalAndThen(
      "jvm.resolveClassPkgs",
      mapOf("classPkgs" to depsValue)
    ) { classPaths ->
      val cps = (classPaths as DataClassInstanceValue).value as SetValue

      val srcs = (context.arguments["srcs"]!! as SetValue).values.map { src ->
        (src as FileValue).file
      }

      val args = mutableListOf("javac")
      if (cps.values.isNotEmpty()) {
        args.add("-cp")
        args.add(cps.values.joinToString(":") { (it as PathValue).path.absolutePathString() })
      }
      args.add("-d")
      args.add(dest.absolutePathString())
      args.addAll(srcs.map { it.absolutePathString() })

      val process = Runtime.getRuntime().exec(args.toTypedArray())
      println(String(process.errorStream.readAllBytes()))
      process.waitFor()

      check(process.exitValue() == 0)

      // ClassPkg = (origin: ClassOrigin, cps: set<path>, deps: set<ClassPkg>)
      built(context.objectIdHash, dest, deps)
    }
  }

  fun run(context: ActionContext) {
    TODO()
  }
}
