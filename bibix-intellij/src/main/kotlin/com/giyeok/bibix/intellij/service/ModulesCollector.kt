package com.giyeok.bibix.intellij.service

import com.giyeok.bibix.base.*
import com.giyeok.bibix.plugins.jvm.ClassPkg
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

class ModulesCollector(val languageType: String) {
  private val _modules = ConcurrentHashMap<String, ModuleData>()

  val modules get() = _modules.toMap()

  fun build(context: BuildContext): BibixValue {
    val srcs = context.arguments.getValue("srcs") as SetValue
    val deps = context.arguments.getValue("deps") as SetValue
    _modules[context.objectIdHash] = ModuleData(
      languageType,
      srcs.values.map { (it as FileValue).file }.toSet(),
      deps.values.map { ClassPkg.fromBibix(it) }
    )
    val origin = ClassInstanceValue(
      "com.giyeok.bibix.plugins.jvm",
      "LocalBuilt",
      mapOf(
        "objHash" to StringValue(context.objectIdHash),
        "builderName" to StringValue("ModulesCollector")
      )
    )
    val cpinfo = ClassInstanceValue(
      "com.giyeok.bibix.plugins.jvm",
      "ClassesInfo",
      mapOf(
        "classDirs" to SetValue(),
        "resDirs" to SetValue(),
        "srcs" to NoneValue
      )
    )
    return ClassInstanceValue(
      "com.giyeok.bibix.plugins.jvm",
      "ClassPkg",
      mapOf(
        "origin" to origin,
        "cpinfo" to cpinfo,
        "deps" to SetValue()
      )
    )
  }
}

data class ModuleData(
  val languageType: String,
  val sources: Set<Path>,
  val dependencies: List<ClassPkg>
)
