package com.giyeok.bibix.intellij.service

import com.giyeok.bibix.base.*
import com.giyeok.bibix.plugins.jvm.ClassPkg
import com.giyeok.bibix.plugins.jvm.LocalBuilt
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

class ModuleCollector(val languageType: String, val sdkArtifactName: Pair<String, String>?) {
  private val _modules = ConcurrentHashMap<String, ModuleData>()

  val modules get() = _modules.toMap()

  fun withSdkArtifact(
    context: BuildContext,
    sdk: Pair<String, ClassPkg>?,
    thenBlock: () -> BuildRuleReturn
  ): BuildRuleReturn {
    val srcs = context.arguments.getValue("srcs") as SetValue
    val resources = context.arguments["resources"] as? SetValue ?: SetValue(listOf())
    val resDirs =
      ResourceDirectoryExtractor.findResourceDirectoriesOf(resources.values.map { (it as FileValue).file })
    val deps = context.arguments.getValue("deps") as SetValue
    val runtimeDeps = context.arguments.getValue("runtimeDeps") as SetValue
    val origin = LocalBuilt(context.targetId, "ModulesCollector")

    _modules[context.targetId] = ModuleData(
      languageType = languageType,
      origin = origin,
      sources = srcs.values.map { (it as FileValue).file }.toSet(),
      resourceDirs = resDirs,
      sdk = sdk,
      deps = deps.values.map { ClassPkg.fromBibix(it) },
      runtimeDeps = runtimeDeps.values.map { ClassPkg.fromBibix(it) },
      allArgs = context.arguments
    )
    return thenBlock()
//    val cpinfo = ClassInstanceValue(
//      "com.giyeok.bibix.plugins.jvm",
//      "ClassesInfo",
//      mapOf(
//        "classDirs" to SetValue(),
//        "resDirs" to SetValue(),
//        "srcs" to NoneValue
//      )
//    )
//    return BuildRuleReturn.value(
//      ClassInstanceValue(
//        "com.giyeok.bibix.plugins.jvm",
//        "ClassPkg",
//        mapOf(
//          "origin" to origin.toBibix(),
//          "cpinfo" to cpinfo,
//          "deps" to SetValue(listOfNotNull(sdk?.second?.toBibix()) + deps.values),
//          "runtimeDeps" to SetValue(runtimeDeps.values),
//        )
//      )
//    )
  }

  fun processAndThen(context: BuildContext, thenBlock: () -> BuildRuleReturn): BuildRuleReturn {
    val sdkVersion = context.arguments["sdkVersion"] as? StringValue

    return if (sdkVersion == null || sdkArtifactName == null) {
      withSdkArtifact(context, null, thenBlock)
    } else {
      BuildRuleReturn.evalAndThen(
        "maven.artifact",
        mapOf(
          "group" to StringValue(sdkArtifactName.first),
          "artifact" to StringValue(sdkArtifactName.second),
          "version" to sdkVersion
        )
      ) { sdkArtifact ->
        withSdkArtifact(context, Pair(sdkVersion.value, ClassPkg.fromBibix(sdkArtifact)), thenBlock)
      }
    }
  }
}

data class ModuleData(
  val languageType: String,
  val origin: LocalBuilt,
  val sources: Set<Path>,
  val resourceDirs: Set<Path>,
  val sdk: Pair<String, ClassPkg>?,
  val deps: List<ClassPkg>,
  val runtimeDeps: List<ClassPkg>,
  val allArgs: Map<String, BibixValue>
)
