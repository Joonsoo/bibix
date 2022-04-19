package com.giyeok.bibix.plugins.scala

import com.giyeok.bibix.base.*
import scala.jdk.`CollectionConverters$`
import scala.tools.nsc.Global
import scala.tools.nsc.Settings

class Library {
  fun build(context: BuildContext): BuildRuleReturn {
    val srcs =
      (context.arguments.getValue("srcs") as SetValue).values.map { (it as FileValue).file }
    check(srcs.isNotEmpty()) { "srcs must not be empty" }
    val deps = context.arguments.getValue("deps") as SetValue
    // TODO context.hashChanged
    return BuildRuleReturn.evalAndThen(
      "jvm.resolveClassPkgs",
      mapOf("classPkgs" to deps)
    ) { classPaths ->
      val settings = Settings()
      val cps = (classPaths as ClassInstanceValue).value as SetValue
      cps.values.forEach { cp ->
        val cpPath = (cp as PathValue).path
        settings.classpath().append(cpPath.absolutePath)
      }
      settings.outputDirs().setSingleOutput(context.destDirectory.absolutePath)
      settings.usejavacp().`value_$eq`(true)

      val global = Global(settings)
      val run = global.Run()
      val srcPaths = srcs.map { it.absolutePath }
      val srcScala = `CollectionConverters$`.`MODULE$`.ListHasAsScala(srcPaths).asScala().toList()
      // TODO 컴파일 실패시 오류 반환
      run.compile(srcScala)

      if (global.reporter().hasErrors()) {
        throw IllegalStateException("Errors from scala compiler")
      }

      BuildRuleReturn.value(
        TupleValue(
          StringValue("built by scala.library: " + context.objectIdHash),
          SetValue(PathValue(context.destDirectory)),
          deps
        )
      )
    }
  }
}
