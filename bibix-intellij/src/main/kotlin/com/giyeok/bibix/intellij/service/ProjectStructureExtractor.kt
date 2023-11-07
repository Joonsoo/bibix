package com.giyeok.bibix.intellij.service

import com.giyeok.bibix.base.ClassInstanceValue
import com.giyeok.bibix.base.DummyProgressLogger
import com.giyeok.bibix.base.StringValue
import com.giyeok.bibix.frontend.BuildFrontend
import com.giyeok.bibix.graph.BibixProjectLocation
import com.giyeok.bibix.graph.CallExprNode
import com.giyeok.bibix.graph.runner.BuildTask
import com.giyeok.bibix.graph.runner.BuildTaskResult
import com.giyeok.bibix.graph.runner.EvalCallee
import com.giyeok.bibix.graph.runner.EvalTarget
import com.giyeok.bibix.intellij.*
import com.giyeok.bibix.intellij.BibixIntellijProto.Module.LibraryDep
import com.giyeok.bibix.intellij.BibixIntellijProto.Module.ModuleDep
import com.giyeok.bibix.plugins.jvm.*
import com.giyeok.bibix.plugins.maven.Artifact
import com.google.common.annotations.VisibleForTesting
import kotlinx.coroutines.runBlocking
import java.nio.file.Path
import kotlin.io.path.*

object ProjectStructureExtractor {

  fun loadProject(projectRoot: Path, scriptName: String?): BibixIntellijProto.BibixProjectInfo {
    val buildFrontend = BuildFrontend(
      BibixProjectLocation.of(projectRoot, scriptName),
      mapOf(),
      listOf(),
      targetLogFileName = "ijdaemon-log.pbsuf",
    )

    val mainTargets = buildFrontend.mainScriptDefinitions

    data class Candidate(val name: String, val evalTarget: EvalTarget, val evalCallee: EvalCallee)

    val candidates = mutableListOf<Candidate>()
    mainTargets.forEach { (name, buildTask) ->
      if (buildTask is EvalTarget) {
        val buildGraph = buildFrontend.buildGraphRunner.multiGraph
          .getProjectGraph(buildTask.projectId)
        val target = buildGraph.targets[buildTask.name]
        if (target != null) {
          val node = buildGraph.exprGraph.nodes.getValue(target)
          if (node is CallExprNode) {
            candidates.add(
              Candidate(
                name,
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
      }.toMap()
    )

    return projectInfoBuilder.build()
  }
}
