package com.giyeok.bibix.integtest

import com.giyeok.bibix.base.*
import com.giyeok.bibix.frontend.BuildFrontend
import com.giyeok.bibix.frontend.cli.ProgressConsolePrinter
import com.giyeok.bibix.interpreter.BibixInterpreter
import com.giyeok.bibix.interpreter.BibixProject
import com.giyeok.bibix.interpreter.PluginImplProvider
import com.giyeok.bibix.interpreter.PluginImplProviderImpl
import com.giyeok.bibix.interpreter.coroutine.ProgressIndicator
import com.giyeok.bibix.interpreter.coroutine.ProgressIndicatorContainer
import com.giyeok.bibix.plugins.jvm.ClassPkg
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.Path

class ModuleStructAnalysisTests {
  @Test
  fun test() {
    val javaModulesCollector = ModulesCollector()
    val ktjvmModulesCollector = ModulesCollector()
    val scalaModulesCollector = ModulesCollector()

    val frontend = BuildFrontend(
      mainProject = BibixProject(Path("../testproject"), null),
      buildArgsMap = mapOf(),
      actionArgs = listOf(),
      progressNotifier = ProgressConsolePrinter(),
      pluginImplProvider = OverridingPluginImplProviderImpl(
        mapOf(
          Pair(MainSourceId, "com.giyeok.bibix.plugins.java.Library") to javaModulesCollector,
          Pair(MainSourceId, "com.giyeok.bibix.plugins.ktjvm.Library") to ktjvmModulesCollector,
          Pair(MainSourceId, "com.giyeok.bibix.plugins.scala.Library") to scalaModulesCollector
        )
      ),
      debuggingMode = true
    )
    val interpreter = BibixInterpreter(
      buildEnv = frontend.buildEnv,
      prelude = frontend.prelude,
      preloadedPlugins = frontend.preloadedPlugins,
      pluginImplProvider = frontend.pluginImplProvider,
      mainProject = frontend.mainProject,
      repo = frontend.repo,
      progressIndicatorContainer = object : ProgressIndicatorContainer {
        override fun notifyUpdated(progressIndicator: ProgressIndicator) {
          // Do nothing
        }

        override fun ofCurrentThread(): ProgressIndicator {
          return ProgressIndicator(this, 0)
        }
      },
      actionArgs = listOf()
    )

    val targetNames = listOf("test1")
    runBlocking {
      targetNames.map { targetName ->
        async { targetName to interpreter.userBuildRequest(targetName) }
      }.awaitAll()
    }.toMap()

    println(ktjvmModulesCollector.modules)
    println(ktjvmModulesCollector)

    val test1Hash = "7fe5b2a27ad785f7acf3fb6b6187fc87fea12b68"
    val test2Hash = "4f3cd0f105adf3a8c8e56122b0fdc4794254cd94"
    val unnamedHash = "fff68e8ca3236754a8bb766097be0a4ae711257b"
    assertThat(frontend.repo.repoMeta.objectNamesMap).containsExactly(
      "test1", test1Hash,
      "test2", test2Hash,
    )
    assertThat(ktjvmModulesCollector.modules.keys).containsExactly(
      test1Hash,
      test2Hash,
      unnamedHash
    )
    val test1Module = ktjvmModulesCollector.modules.getValue(test1Hash)
    val test2Module = ktjvmModulesCollector.modules.getValue(test2Hash)
    val unnamedModule = ktjvmModulesCollector.modules.getValue(unnamedHash)
    assertThat(test1Module.sources).containsExactly(
      Path("../testproject/src/main/kotlin/Test2.kt")
    )
    assertThat(test2Module.sources).containsExactly(
      Path("../testproject/src/main/kotlin/Test.kt")
    )
    assertThat(unnamedModule.sources).containsExactly(
      Path("../testproject/src/main/scala/Test3.scala")
    )
    assertThat(test1Module.dependencies).containsExactly(
      ClassInstanceValue(
        "com.giyeok.bibix.plugins.jvm",
        "ClassPkg",
        mapOf(
          "origin" to ClassInstanceValue(
            "com.giyeok.bibix.plugins.jvm",
            "LocalBuilt",
            mapOf(
              "objHash" to StringValue(test2Hash),
              "builderName" to StringValue("ModulesCollector")
            )
          ),
          "cpinfo" to ClassInstanceValue(
            "com.giyeok.bibix.plugins.jvm",
            "ClassesInfo",
            mapOf(
              "classDirs" to SetValue(),
              "resDirs" to SetValue(),
              "srcs" to NoneValue,
            )
          ),
          "deps" to SetValue(),
        )
      ),
      ClassInstanceValue(
        "com.giyeok.bibix.plugins.jvm",
        "ClassPkg",
        mapOf(
          "origin" to ClassInstanceValue(
            "com.giyeok.bibix.plugins.jvm",
            "LocalBuilt",
            mapOf(
              "objHash" to StringValue(unnamedHash),
              "builderName" to StringValue("ModulesCollector")
            )
          ),
          "cpinfo" to ClassInstanceValue(
            "com.giyeok.bibix.plugins.jvm",
            "ClassesInfo",
            mapOf(
              "classDirs" to SetValue(),
              "resDirs" to SetValue(),
              "srcs" to NoneValue,
            )
          ),
          "deps" to SetValue(),
        )
      )
    )
    val test1Pkg = ClassPkg.fromBibix(test1Module.dependencies.first())
    println(test1Pkg)
  }
}

class OverridingPluginImplProviderImpl(val overridings: Map<Pair<SourceId, String>, Any>) :
  PluginImplProvider {
  private val impl = PluginImplProviderImpl()

  override suspend fun getPluginImplInstance(
    callerSourceId: SourceId,
    cpInstance: ClassInstanceValue,
    className: String
  ): Any = overridings[Pair(callerSourceId, className)]
    ?: impl.getPluginImplInstance(callerSourceId, cpInstance, className)
}

class ModulesCollector {
  val modules = mutableMapOf<String, ModuleData>()
  fun build(context: BuildContext): BibixValue {
    val srcs = context.arguments.getValue("srcs") as SetValue
    val deps = context.arguments.getValue("deps") as SetValue
    modules[context.objectIdHash] = ModuleData(
      srcs.values.map { (it as FileValue).file }.toSet(),
      deps.values
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
        "deps" to deps
      )
    )
  }
}

data class ModuleData(val sources: Set<Path>, val dependencies: List<BibixValue>)
