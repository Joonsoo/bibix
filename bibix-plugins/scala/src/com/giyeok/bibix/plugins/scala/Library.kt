package com.giyeok.bibix.plugins.scala

import com.giyeok.bibix.base.*
import scala.collection.JavaConverters.AsScala
import scala.concurrent.`JavaConversions$`
import scala.jdk.CollectionConverters
import scala.jdk.`CollectionConverters$`
import scala.tools.nsc.Global
import scala.tools.nsc.Global.Run
import scala.tools.nsc.Settings
import scala.tools.reflect.ToolBox

class Library {
  fun build(context: BuildContext): BuildRuleReturn {
    val srcs =
      (context.arguments.getValue("srcs") as SetValue).values.map { (it as FileValue).file }
    check(srcs.isNotEmpty()) { "srcs must not be empty" }
    val deps = context.arguments.getValue("deps") as SetValue
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
      run.compile(srcScala)
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
