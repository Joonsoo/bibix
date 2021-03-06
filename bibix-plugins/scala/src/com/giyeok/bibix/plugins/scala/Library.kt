package com.giyeok.bibix.plugins.scala

import com.giyeok.bibix.base.*
import scala.jdk.`CollectionConverters$`
import scala.tools.nsc.Global
import scala.tools.nsc.Settings
import kotlin.io.path.absolutePathString

class Library {
  fun build(context: BuildContext): BuildRuleReturn {
    val deps = context.arguments.getValue("deps") as SetValue
    val srcsValue = context.arguments.getValue("srcs") as SetValue
    val srcs = srcsValue.values.map { (it as FileValue).file }

    if (!context.hashChanged) {
      return BuildRuleReturn.value(
        ClassPkg(
          LocalBuilt(context.objectIdHash, "scala.library"),
          ClassesInfo(listOf(context.destDirectory), listOf(), srcs),
          deps.values.map { ClassPkg.fromBibix(it) }
        ).toBibix()
      )
    }

    check(srcs.isNotEmpty()) { "srcs must not be empty" }
    return BuildRuleReturn.evalAndThen(
      "jvm.resolveClassPkgs",
      mapOf("classPkgs" to deps)
    ) { classPaths ->
      val settings = Settings()
      val cps = ClassPaths.fromBibix(classPaths)
      cps.cps.forEach { cpPath ->
        settings.classpath().append(cpPath.absolutePathString())
      }
      settings.outputDirs().setSingleOutput(context.destDirectory.absolutePathString())
      // settings.usejavacp().`value_$eq`(true)

      val global = Global(settings)
      val run = global.Run()
      val srcPaths = srcs.map { it.absolutePathString() }
      val srcScala = `CollectionConverters$`.`MODULE$`.ListHasAsScala(srcPaths).asScala().toList()
      run.compile(srcScala)

      // 컴파일 실패시 예외 발생
      if (global.reporter().hasErrors()) {
        throw IllegalStateException(
          "${global.reporter().errorCount()} errors reported from scala compiler"
        )
      }

      BuildRuleReturn.value(
        ClassPkg(
          LocalBuilt(context.objectIdHash, "scala.library"),
          ClassesInfo(listOf(context.destDirectory), listOf(), srcs),
          deps.values.map { ClassPkg.fromBibix(it) }
        ).toBibix()
      )
    }
  }
}
