//package com.giyeok.bibix.integtest
//
//import com.giyeok.bibix.base.*
//import com.giyeok.bibix.frontend.BuildFrontend
//import com.giyeok.bibix.frontend.NoopProgressNotifier
//import com.giyeok.bibix.interpreter.BibixProject
//import com.giyeok.bibix.interpreter.PluginImplProvider
//import com.giyeok.bibix.interpreter.PluginImplProviderImpl
//import com.giyeok.bibix.plugins.jvm.ClassPkg
//import com.google.common.truth.Truth.assertThat
//import org.junit.jupiter.api.Test
//import java.nio.file.Path
//import kotlin.io.path.Path
//
//class ModuleStructAnalysisTests {
//  @Test
//  fun test() {
//    val javaModulesCollector = ModulesCollector()
//    val ktjvmModulesCollector = ModulesCollector()
//    val scalaModulesCollector = ModulesCollector()
//
//    val frontend = BuildFrontend(
//      mainProject = BibixProject(Path("../testproject"), null),
//      buildArgsMap = mapOf(),
//      actionArgs = listOf(),
//      progressNotifier = NoopProgressNotifier(),
//      pluginImplProvider = OverridingPluginImplProviderImpl(
//        mapOf(
//          Pair(MainSourceId, "com.giyeok.bibix.plugins.java.Library") to javaModulesCollector,
//          Pair(MainSourceId, "com.giyeok.bibix.plugins.ktjvm.Library") to ktjvmModulesCollector,
//          Pair(MainSourceId, "com.giyeok.bibix.plugins.scala.Library") to scalaModulesCollector
//        )
//      ),
//      debuggingMode = true
//    )
//
//    val results = frontend.blockingBuildTargets(listOf("test1"))
//
//    println(ktjvmModulesCollector.modules)
//    println(ktjvmModulesCollector)
//
//    val test1Hash = "6fc4cbc1888d29fbd07702bbcfcabff3272cbd40"
//    val test2Hash = "999e8279e9d5b7292876e677cbf00e82022fba09"
//    val unnamedHash = "79ddffbbe76151febe15febde8c6c66cd7661fd4"
////    assertThat(frontend.repo.targets).containsExactly(
////      "test1", test1Hash,
////      "test2", test2Hash,
////    )
//    assertThat(ktjvmModulesCollector.modules.keys).containsExactly(
//      test1Hash,
//      test2Hash,
//      unnamedHash
//    )
//    val test1Module = ktjvmModulesCollector.modules.getValue(test1Hash)
//    val test2Module = ktjvmModulesCollector.modules.getValue(test2Hash)
//    val unnamedModule = ktjvmModulesCollector.modules.getValue(unnamedHash)
//    assertThat(test1Module.sources).containsExactly(
//      Path("../testproject/src/main/kotlin/Test2.kt")
//    )
//    assertThat(test2Module.sources).containsExactly(
//      Path("../testproject/src/main/kotlin/Test.kt")
//    )
//    assertThat(unnamedModule.sources).containsExactly(
//      Path("../testproject/src/main/scala/Test3.scala")
//    )
//    assertThat(test1Module.dependencies).containsExactly(
//      ClassInstanceValue(
//        "com.giyeok.bibix.plugins.jvm",
//        "ClassPkg",
//        mapOf(
//          "origin" to ClassInstanceValue(
//            "com.giyeok.bibix.plugins.jvm",
//            "LocalBuilt",
//            mapOf(
//              "objHash" to StringValue(test2Hash),
//              "builderName" to StringValue("ModulesCollector")
//            )
//          ),
//          "cpinfo" to ClassInstanceValue(
//            "com.giyeok.bibix.plugins.jvm",
//            "ClassesInfo",
//            mapOf(
//              "classDirs" to SetValue(),
//              "resDirs" to SetValue(),
//              "srcs" to NoneValue,
//            )
//          ),
//          "deps" to SetValue(),
//        )
//      ),
//      ClassInstanceValue(
//        "com.giyeok.bibix.plugins.jvm",
//        "ClassPkg",
//        mapOf(
//          "origin" to ClassInstanceValue(
//            "com.giyeok.bibix.plugins.jvm",
//            "LocalBuilt",
//            mapOf(
//              "objHash" to StringValue(unnamedHash),
//              "builderName" to StringValue("ModulesCollector")
//            )
//          ),
//          "cpinfo" to ClassInstanceValue(
//            "com.giyeok.bibix.plugins.jvm",
//            "ClassesInfo",
//            mapOf(
//              "classDirs" to SetValue(),
//              "resDirs" to SetValue(),
//              "srcs" to NoneValue,
//            )
//          ),
//          "deps" to SetValue(),
//        )
//      )
//    )
//    val test1Pkg = ClassPkg.fromBibix(test1Module.dependencies.first())
//    println(test1Pkg)
//  }
//}
//
//class OverridingPluginImplProviderImpl(val overridings: Map<Pair<SourceId, String>, Any>) :
//  PluginImplProvider {
//  private val impl = PluginImplProviderImpl()
//
//  override suspend fun getPluginImplInstance(
//    callerSourceId: SourceId,
//    cps: List<Path>,
//    className: String
//  ): Any = overridings[Pair(callerSourceId, className)]
//    ?: impl.getPluginImplInstance(callerSourceId, cps, className)
//}
//
//class ModulesCollector {
//  val modules = mutableMapOf<String, ModuleData>()
//  fun build(context: BuildContext): BibixValue {
//    val srcs = context.arguments.getValue("srcs") as SetValue
//    val deps = context.arguments.getValue("deps") as SetValue
//    modules[context.targetId] = ModuleData(
//      srcs.values.map { (it as FileValue).file }.toSet(),
//      deps.values
//    )
//    val origin = ClassInstanceValue(
//      "com.giyeok.bibix.plugins.jvm",
//      "LocalBuilt",
//      mapOf(
//        "objHash" to StringValue(context.targetId),
//        "builderName" to StringValue("ModulesCollector")
//      )
//    )
//    val cpinfo = ClassInstanceValue(
//      "com.giyeok.bibix.plugins.jvm",
//      "ClassesInfo",
//      mapOf(
//        "classDirs" to SetValue(),
//        "resDirs" to SetValue(),
//        "srcs" to NoneValue
//      )
//    )
//    return ClassInstanceValue(
//      "com.giyeok.bibix.plugins.jvm",
//      "ClassPkg",
//      mapOf(
//        "origin" to origin,
//        "cpinfo" to cpinfo,
//        "deps" to deps
//      )
//    )
//  }
//}
//
//data class ModuleData(val sources: Set<Path>, val dependencies: List<BibixValue>)
