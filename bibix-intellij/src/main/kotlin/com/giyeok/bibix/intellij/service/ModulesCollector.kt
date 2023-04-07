package com.giyeok.bibix.intellij.service

import com.giyeok.bibix.base.*
import com.giyeok.bibix.plugins.jvm.ClassPkg
import com.giyeok.bibix.plugins.jvm.LocalBuilt
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

class ModulesCollector(val languageType: String, val sdkArtifactName: Pair<String, String>?) {
  private val _modules = ConcurrentHashMap<String, ModuleData>()

  val modules get() = _modules.toMap()

  fun withSdkArtifact(context: BuildContext, sdk: Pair<String, ClassPkg>?): BuildRuleReturn {
    val srcs = context.arguments.getValue("srcs") as SetValue
    val resources = context.arguments["resources"] as? SetValue ?: SetValue(listOf())
    val resDirs =
      ResourceDirectoryExtractor.findResourceDirectoriesOf(resources.values.map { (it as FileValue).file })
    val deps = context.arguments.getValue("deps") as SetValue
    val origin = LocalBuilt(context.targetId, "ModulesCollector")

    _modules[context.targetId] = ModuleData(
      languageType,
      origin,
      srcs.values.map { (it as FileValue).file }.toSet(),
      resDirs,
      sdk,
      deps.values.map { ClassPkg.fromBibix(it) },
      context.arguments
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
    return BuildRuleReturn.value(
      ClassInstanceValue(
        "com.giyeok.bibix.plugins.jvm",
        "ClassPkg",
        mapOf(
          "origin" to origin.toBibix(),
          "cpinfo" to cpinfo,
          "deps" to SetValue(listOfNotNull(sdk?.second?.toBibix()) + deps.values)
        )
      )
    )
  }

  fun build(context: BuildContext): BuildRuleReturn {
    val sdkVersion = context.arguments["sdkVersion"] as? StringValue

    return if (sdkVersion == null || sdkArtifactName == null) {
      withSdkArtifact(context, null)
    } else {
      BuildRuleReturn.evalAndThen(
        "maven.artifact",
        mapOf(
          "group" to StringValue(sdkArtifactName.first),
          "artifact" to StringValue(sdkArtifactName.second),
          "version" to sdkVersion
        )
      ) { sdkArtifact ->
        withSdkArtifact(context, Pair(sdkVersion.value, ClassPkg.fromBibix(sdkArtifact)))
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
  val dependencies: List<ClassPkg>,
  val allArgs: Map<String, BibixValue>
)
