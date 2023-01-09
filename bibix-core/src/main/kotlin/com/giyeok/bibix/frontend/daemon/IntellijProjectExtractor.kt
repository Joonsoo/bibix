//package com.giyeok.bibix.frontend.daemon
//
//import com.giyeok.bibix.ast.BibixAst
//import com.giyeok.bibix.base.*
//import com.giyeok.bibix.buildscript.ExprNode
//import com.giyeok.bibix.buildscript.ParamNodes
//import com.giyeok.bibix.daemon.BibixDaemonApiProto
//import com.giyeok.bibix.daemon.intellijProjectNode
//import com.giyeok.bibix.daemon.intellijProjectStructure
//import com.giyeok.bibix.base.FileType
//import com.giyeok.bibix.base.SetType
//import com.giyeok.bibix.frontend.BuildFrontend
//import com.giyeok.bibix.plugins.ClassPkg
//import com.giyeok.bibix.runner.*
//import com.giyeok.bibix.utils.toKtList
//import java.nio.file.Path
//import kotlin.io.path.absolutePathString
//import kotlin.io.path.name
//
//class IntellijProjectExtractor(val frontend: BuildFrontend) {
//  val userBuildRules = mapOf(
//    Pair("com.giyeok.bibix.plugins.ktjvm.Library", "build") to ModuleLanguage.KOTLIN,
//    Pair("com.giyeok.bibix.plugins.scala.Library", "build") to ModuleLanguage.SCALA,
//    // Pair("com.giyeok.bibix.plugins.protobuf.Compile", "schema")
//  )
//  val nativeBuildRules = mapOf(
//    Pair("com.giyeok.bibix.plugins.java.Library", "build") to ModuleLanguage.JAVA,
//  )
//
//  val jvmRunActionPlugin = Pair("com.giyeok.bibix.plugins.scala.Library", "build")
//
//  enum class ModuleLanguage {
//    JAVA, KOTLIN, SCALA
//  }
//
//  data class ModuleData(
//    val cname: CName,
//    val language: ModuleLanguage,
//    val srcs: List<Path>,
//    val deps: List<ClassPkg>
//  )
//
//  suspend fun traverseDef(
//    defs: List<BibixAst.Def>,
//    cname: CName
//  ): List<ModuleData> = defs.flatMap { def ->
//    when (def) {
//      is BibixAst.TargetDef ->
//        listOfNotNull(traverseModuleNode(cname.append(def.name())))
//      is BibixAst.NamespaceDef ->
//        traverseDef(def.body().defs().toKtList(), cname.append(def.name()))
//      else -> listOf()
//    }
//  }
//
//  fun paramExprFrom(paramName: String, params: List<Param>, paramNodes: ParamNodes): ExprNode {
//    val paramIdx = params.indexOfFirst { it.name == paramName }
//    return if (paramIdx < paramNodes.posParams.size) {
//      paramNodes.posParams[paramIdx]
//    } else {
//      paramNodes.namedParams.getValue(paramName)
//    }
//  }
//
//  private suspend fun evaluateModuleParams(
//    cname: CName,
//    origin: SourceId,
//    moduleLanguage: ModuleLanguage,
//    exprGraphId: Int,
//    params: List<Param>,
//    paramNodes: ParamNodes
//  ): ModuleData {
//    val srcsExpr = paramExprFrom("srcs", params, paramNodes)
//    val depsExpr = paramExprFrom("deps", params, paramNodes)
//    println(srcsExpr)
//    println(depsExpr)
//    val srcsTask = BuildTask.EvalExpr(origin, exprGraphId, srcsExpr, null)
//    val depsTask = BuildTask.EvalExpr(origin, exprGraphId, depsExpr, null)
//    val values = frontend.runTasks(frontend.nextBuildId(), listOf(srcsTask, depsTask))
//    val srcs = (frontend.coercer.coerce(
//      srcsTask,
//      origin,
//      values[0] as BibixValue,
//      SetType(FileType),
//      null
//    ) as SetValue).values.map { (it as FileValue).file }
//    val deps = (frontend.coercer.coerce(
//      depsTask,
//      origin,
//      values[1] as BibixValue,
//      // SetType(CustomType(CName(PreloadedSourceId("jvm"), "ClassPkg"))),
//      TODO(),
//      null
//    ) as SetValue).values.map { ClassPkg.fromBibix(it) }
//    return ModuleData(cname, moduleLanguage, srcs, deps)
//  }
//
//  suspend fun evaluateModuleNode(
//    cname: CName,
//    origin: SourceId,
//    exprGraphId: Int,
//    callTarget: Any,
//    paramNodes: ParamNodes
//  ): ModuleData? {
//    when (callTarget) {
//      is BuildRuleImplInfo.UserBuildRuleImplInfo -> {
//        val moduleLanguage = userBuildRules[Pair(callTarget.className, callTarget.methodName)]
//        if (moduleLanguage != null) {
//          return evaluateModuleParams(
//            cname,
//            origin,
//            moduleLanguage,
//            exprGraphId,
//            callTarget.params,
//            paramNodes
//          )
//        }
//      }
//      is BuildRuleImplInfo.NativeBuildRuleImplInfo -> {
//        val moduleLanguage =
//          nativeBuildRules[Pair(callTarget.cls.canonicalName, callTarget.methodName)]
//        if (moduleLanguage != null) {
//          return evaluateModuleParams(
//            cname,
//            origin,
//            moduleLanguage,
//            exprGraphId,
//            callTarget.params,
//            paramNodes
//          )
//        }
//      }
//    }
//    return null
//  }
//
//  suspend fun traverseModuleNode(cname: CName): ModuleData? {
//    val value = frontend.buildRunner.buildGraph.names[cname] ?: return null
//    when (value) {
//      is CNameValue.ExprValue -> {
//        val expr = frontend.buildGraph.exprGraphs[value.exprGraphId]
//        when (expr.mainNode) {
//          is ExprNode.CallExprNode -> {
//            val callExpr = expr.callExprs[expr.mainNode.callExprId]
//            if (callExpr.target is ExprNode.NameNode) {
//              val callTargetName = callExpr.target.name
//              frontend.runTargets(frontend.nextBuildId(), listOf(callTargetName))
//              val callTarget = frontend.buildRunner.routineManager.getTaskResult(
//                BuildTask.ResolveName(callExpr.target.name)
//              )
//              if (callTarget != null) {
//                return evaluateModuleNode(
//                  cname,
//                  cname.sourceId,
//                  value.exprGraphId,
//                  callTarget,
//                  callExpr.params
//                )
//              }
//            }
//            println(callExpr)
//          }
//          else -> {}
//        }
//      }
//      else -> {}
//    }
//    return null
//  }
//
//  suspend fun extractIntellijProjectStructure(): BibixDaemonApiProto.IntellijProjectStructure {
//    val projectBaseDir = frontend.projectDir
//
//    val modules = traverseDef(frontend.ast.defs().toKtList(), CName(MainSourceId))
//
//    println(modules)
//    // val allSrcFiles = srcsMap.values.flatten()
//    // check(allSrcFiles.distinct().size == allSrcFiles.size)
//
//    // depsMap의 ClassPkg 중 maven은 maven대로 resolve, local lib은 그냥 library로 등록, local built는 모듈 디펜던시로
//    // local built는 getTaskByObjectIdHash(objHash)로 얻어와서 어떤 모듈인지 확인
//    // val task = frontend.buildRunner.getTaskByObjectIdHash("~~~".hexToByteString())
//
//    val modulesExtractor = IntellijModulesExtractor(frontend, modules)
//    val (intellijLibraries, intellijModules) = modulesExtractor.extractModules()
//
//    return intellijProjectStructure {
//      this.project = intellijProjectNode {
//        this.name = projectBaseDir.name
//        this.path = projectBaseDir.absolutePathString()
//        this.jdkVersion = "16" // TODO
//
//        this.libraries.addAll(intellijLibraries)
//        this.modules.addAll(intellijModules)
//      }
//    }
//  }
//}
