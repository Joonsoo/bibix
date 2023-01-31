package com.giyeok.bibix.integtest

import com.giyeok.bibix.base.*
import com.giyeok.bibix.frontend.BuildFrontend
import com.giyeok.bibix.frontend.cli.ProgressConsolePrinter
import com.giyeok.bibix.interpreter.BibixInterpreter
import com.giyeok.bibix.interpreter.BibixProject
import com.giyeok.bibix.interpreter.PluginClassLoader
import com.giyeok.bibix.interpreter.PluginClassLoaderImpl
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
    val modulesCollector = ModulesCollector()

    val frontend = BuildFrontend(
      mainProject = BibixProject(Path("../testproject"), null),
      buildArgsMap = mapOf(),
      actionArgs = listOf(),
      progressNotifier = ProgressConsolePrinter(),
      pluginClassLoader = OverridingPluginClassLoaderImpl(
        mapOf(
          Pair(MainSourceId, "com.giyeok.bibix.plugins.ktjvm.Library") to modulesCollector,
          Pair(MainSourceId, "com.giyeok.bibix.plugins.scala.Library") to modulesCollector
        )
      ),
      debuggingMode = true
    )
    val interpreter = BibixInterpreter(
      buildEnv = frontend.buildEnv,
      prelude = frontend.prelude,
      preloadedPlugins = frontend.preloadedPlugins,
      pluginClassLoader = frontend.pluginClassLoader,
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

    println(modulesCollector.modules)
    println(modulesCollector)

    val test1Hash = "9de16e64c983a7569c9b95b6cae4d14a85d786d9"
    val test2Hash = "f76fe69cfc122475be12f4a0b780402d820b3936"
    val unnamedHash = "5d465df49efc9c5d5db4fc97ce97c7a2c087eff7"
    assertThat(frontend.repo.repoMeta.objectNamesMap).containsExactly(
      "test1", test1Hash,
      "test2", test2Hash,
    )
    assertThat(modulesCollector.modules.keys).containsExactly(test1Hash, test2Hash, unnamedHash)
    val test1Module = modulesCollector.modules.getValue(test1Hash)
    val test2Module = modulesCollector.modules.getValue(test2Hash)
    val unnamedModule = modulesCollector.modules.getValue(unnamedHash)
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

class OverridingPluginClassLoaderImpl(val overridings: Map<Pair<SourceId, String>, Any>) :
  PluginClassLoader {
  private val impl = PluginClassLoaderImpl()

  override suspend fun loadPluginInstance(
    callerSourceId: SourceId,
    cpInstance: ClassInstanceValue,
    className: String
  ): Any = overridings[Pair(callerSourceId, className)]
    ?: impl.loadPluginInstance(callerSourceId, cpInstance, className)
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
        "deps" to SetValue()
      )
    )
  }
}

data class ModuleData(val sources: Set<Path>, val dependencies: List<BibixValue>)
