package com.giyeok.bibix.intellij.service

import com.giyeok.bibix.base.ClassInstanceValue
import com.giyeok.bibix.frontend.BuildFrontend
import com.giyeok.bibix.graph.BibixProjectLocation
import com.giyeok.bibix.graph.CallExprNode
import com.giyeok.bibix.graph.ExprNodeId
import com.giyeok.bibix.graph.runner.*
import com.giyeok.bibix.intellij.*
import com.giyeok.bibix.plugins.jvm.*
import kotlinx.coroutines.runBlocking
import java.nio.file.Path
import kotlin.io.path.*

object ProjectStructureExtractor {

  fun loadProject(projectRoot: Path, scriptName: String?): BibixIntellijProto.BibixProjectInfo {
    val buildFrontend = BuildFrontend(
      BibixProjectLocation.of(projectRoot, scriptName),
      mapOf(),
      listOf(),
    )

    val mainTargets = buildFrontend.mainScriptDefinitions
    val mainBuildGraph = buildFrontend.buildGraphRunner.multiGraph.getProjectGraph(1)

    data class Candidate(
      val name: String,
      val callExprNodeId: ExprNodeId,
      val evalTarget: EvalTarget,
      val evalCallee: EvalCallee
    )

    val candidates = mutableListOf<Candidate>()
    mainTargets.forEach { (name, buildTask) ->
      if (buildTask is EvalTarget) {
        val target = mainBuildGraph.targets[buildTask.name]
        if (target != null) {
          val node = mainBuildGraph.exprGraph.nodes.getValue(target)
          if (node is CallExprNode) {
            candidates.add(
              Candidate(
                name,
                target,
                buildTask,
                EvalCallee(buildTask.projectId, buildTask.importInstanceId, node.callee)
              )
            )
          }
        }
      }
    }

    val callees = runBlocking {
      buildFrontend.parallelRunner.runTasks(candidates.map { it.evalCallee }.distinct())
    }

    val supportedModuleClasses = setOf(
      "com.giyeok.bibix.plugins.java.Library",
      "com.giyeok.bibix.plugins.ktjvm.Library",
      "com.giyeok.bibix.plugins.scala.Library"
    )

    val availableTargets = candidates.filter { candidate ->
      val callee = callees[candidate.evalCallee]
      if (callee != null && callee is BuildTaskResult.BuildRuleResult) {
        val impl = callee.impl
        impl is BuildTaskResult.BuildRuleImpl.NonNativeImpl && impl.implClassName in supportedModuleClasses
      } else {
        false
      }
    }

    val results = runBlocking {
      buildFrontend.runBuildTasks(availableTargets.map { it.evalTarget })
    }

    val projectInfoBuilder = ProjectInfoBuilder(
      buildFrontend.mainProjectLocation,
      availableTargets.mapNotNull { target ->
        val result = results[target.evalTarget]
        (result as? BuildTaskResult.ResultWithValue)?.let {
          if (result.value is ClassInstanceValue) {
            val classPkg = ClassPkg.fromBibix(it.value)
            if (classPkg.origin is LocalBuilt) {
              target.name to classPkg
            } else null
          } else null
        }
      }.toMap(),
      buildFrontend.repo.repoData
    )

    return projectInfoBuilder.build()
  }
}
