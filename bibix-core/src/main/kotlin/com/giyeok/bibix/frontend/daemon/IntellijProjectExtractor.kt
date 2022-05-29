package com.giyeok.bibix.frontend.daemon

import com.giyeok.bibix.ast.BibixAst
import com.giyeok.bibix.base.BibixValue
import com.giyeok.bibix.base.CName
import com.giyeok.bibix.base.MainSourceId
import com.giyeok.bibix.base.SourceId
import com.giyeok.bibix.buildscript.ExprNode
import com.giyeok.bibix.buildscript.ParamNodes
import com.giyeok.bibix.daemon.BibixDaemonApiProto
import com.giyeok.bibix.daemon.intellijProjectNode
import com.giyeok.bibix.daemon.intellijProjectStructure
import com.giyeok.bibix.frontend.BuildFrontend
import com.giyeok.bibix.runner.*
import com.giyeok.bibix.utils.toKtList
import com.google.protobuf.ByteString

class IntellijProjectExtractor(val frontend: BuildFrontend) {
  val javaLibPlugin = Pair("com.giyeok.bibix.plugins.java.Library", "build")
  val kotlinLibPlugin = Pair("com.giyeok.bibix.plugins.ktjvm.Library", "build")
  val scalaLibPlugin = Pair("com.giyeok.bibix.plugins.scala.Library", "build")
  val protobufSchemaPlugin = Pair("com.giyeok.bibix.plugins.protobuf.Compile", "schema")
  val mavenDepPlugin = Pair("com.giyeok.bibix.plugins.maven.Dep", "build")

  val jvmRunActionPlugin = Pair("com.giyeok.bibix.plugins.scala.Library", "build")

  suspend fun traverseIntellijNestedNode(
    defs: List<BibixAst.Def>,
    cname: CName
  ): List<BibixDaemonApiProto.IntellijModuleNode> {
    val modules = mutableListOf<BibixDaemonApiProto.IntellijModuleNode>()
    defs.forEach { def ->
      when (def) {
        is BibixAst.NameDef ->
          intellijModuleNode(cname.append(def.name()))?.let { moduleNode ->
            modules.add(moduleNode)
          }
        is BibixAst.NamespaceDef -> {
          // TODO add parent module node
          traverseIntellijNestedNode(def.body().defs().toKtList(), cname.append(def.name()))
        }
        else -> {
          // do nothing
        }
      }
    }
    return modules
  }

  fun paramExprFrom(paramName: String, params: List<Param>, paramNodes: ParamNodes): ExprNode {
    val paramIdx = params.indexOfFirst { it.name == paramName }
    return if (paramIdx < paramNodes.posParams.size) {
      paramNodes.posParams[paramIdx]
    } else {
      paramNodes.namedParams.getValue(paramName)
    }
  }

  suspend fun convertNameToModule(
    origin: SourceId,
    exprGraphId: Int,
    callTarget: Any,
    paramNodes: ParamNodes
  ): BibixDaemonApiProto.IntellijModuleNode? {
    when (callTarget) {
      is BuildRuleImplInfo.UserBuildRuleImplInfo -> {
        if (callTarget.className == kotlinLibPlugin.first && callTarget.methodName == kotlinLibPlugin.second) {
          println(callTarget)
          val srcsExpr = paramExprFrom("srcs", callTarget.params, paramNodes)
          val depsExpr = paramExprFrom("deps", callTarget.params, paramNodes)
          println(srcsExpr)
          println(depsExpr)
          val srcsTask = BuildTask.EvalExpr(origin, exprGraphId, srcsExpr, null)
          val depsTask = BuildTask.EvalExpr(origin, exprGraphId, depsExpr, null)
          val values = frontend.runTasks(frontend.nextBuildId(), listOf(srcsTask, depsTask))
          println(values)
          val srcs = frontend.coercer.coerce(
            srcsTask,
            origin,
            values[0] as BibixValue,
            SetType(FileType),
            null
          )
          println(srcs)
        }
      }
      is BuildRuleImplInfo.NativeBuildRuleImplInfo -> {
        if (callTarget.javaClass.canonicalName == mavenDepPlugin.first && callTarget.methodName == mavenDepPlugin.second) {
          println(callTarget)
          val srcsExpr = paramExprFrom("srcs", callTarget.params, paramNodes)
          val depsExpr = paramExprFrom("deps", callTarget.params, paramNodes)
          println(srcsExpr)
          println(depsExpr)
        }
      }
    }
    return null
  }

  suspend fun intellijModuleNode(cname: CName): BibixDaemonApiProto.IntellijModuleNode? {
    val value = frontend.buildRunner.buildGraph.names[cname] ?: return null
    when (value) {
//      is CNameValue.ActionCallValue -> TODO()
//      is CNameValue.ActionRuleValue -> TODO()
//      is CNameValue.ArgVar -> TODO()
//      is CNameValue.BuildRuleValue -> TODO()
//      is CNameValue.ClassType -> TODO()
//      is CNameValue.DeferredImport -> TODO()
//      is CNameValue.EnumType -> TODO()
//      is CNameValue.EvaluatedValue -> TODO()
      is CNameValue.ExprValue -> {
        val expr = frontend.buildGraph.exprGraphs[value.exprGraphId]
        when (expr.mainNode) {
          is ExprNode.CallExprNode -> {
            val callExpr = expr.callExprs[expr.mainNode.callExprId]
            if (callExpr.target is ExprNode.NameNode) {
              val callTargetName = callExpr.target.name
              frontend.runTargets(frontend.nextBuildId(), listOf(callTargetName))
              val callTarget = frontend.buildRunner.routineManager.getTaskResult(
                BuildTask.ResolveName(callExpr.target.name)
              )
              if (callTarget != null) {
                return convertNameToModule(
                  cname.sourceId,
                  value.exprGraphId,
                  callTarget,
                  callExpr.params
                )
              }
            }
            println(callExpr)
          }
          else -> {}
        }
      }
//      is CNameValue.NamespaceValue -> TODO()
      else -> {}
    }
    return null
  }

  suspend fun extractIntellijProjectStructure(): BibixDaemonApiProto.IntellijProjectStructure {
    val projectBaseDir = frontend.projectDir

    return intellijProjectStructure {
      this.project = intellijProjectNode {
        this.name = projectBaseDir.name
        this.path = projectBaseDir.absolutePath
        this.sdkName = "1.8" // ??

        this.modules.addAll(
          traverseIntellijNestedNode(frontend.ast.defs().toKtList(), CName(MainSourceId))
        )
      }
    }
  }
}
