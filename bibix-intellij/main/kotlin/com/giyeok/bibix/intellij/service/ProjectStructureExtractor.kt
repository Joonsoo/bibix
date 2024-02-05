package com.giyeok.bibix.intellij.service

import com.giyeok.bibix.base.BibixValue
import com.giyeok.bibix.base.BuildContext
import com.giyeok.bibix.base.FileValue
import com.giyeok.bibix.base.ListValue
import com.giyeok.bibix.base.SetValue
import com.giyeok.bibix.frontend.BuildFrontend
import com.giyeok.bibix.graph.BibixProjectLocation
import com.giyeok.bibix.graph.CallExprNode
import com.giyeok.bibix.graph.ExprNodeId
import com.giyeok.bibix.graph.runner.BuildTask
import com.giyeok.bibix.graph.runner.BuildTaskResult
import com.giyeok.bibix.graph.runner.EvalCallee
import com.giyeok.bibix.graph.runner.EvalTarget
import com.giyeok.bibix.graph.runner.ExprEvaluator
import com.giyeok.bibix.graph.runner.organizeParams
import com.giyeok.bibix.graph.runner.requiredParamNames
import com.giyeok.bibix.graph.runner.toValue
import com.giyeok.bibix.graph.runner.withBuildContext
import com.giyeok.bibix.intellij.BibixIntellijProto
import com.giyeok.bibix.plugins.jvm.ClassPkg
import com.giyeok.bibix.plugins.jvm.ClassesInfo
import com.giyeok.bibix.plugins.jvm.LocalBuilt
import kotlinx.coroutines.runBlocking
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger

class ProjectStructureExtractor(projectLocation: BibixProjectLocation) {
  val buildFrontend = BuildFrontend(
    projectLocation,
    mapOf(),
    listOf(),
    taskInterceptor = ::interceptTask
  )

  private data class Candidate(
    val name: String,
    val callExprNodeId: ExprNodeId,
    val evalTarget: EvalTarget,
    val evalCallee: EvalCallee,
    var callee: BuildTaskResult.BuildRuleResult? = null,
    var moduleType: JVMModules? = null,
    var buildContext: BuildContext? = null,
    var result: ClassPkg? = null,
  )

  private var targets: List<Candidate> = listOf()
  private var evalTargets: Map<EvalTarget, Candidate> = mapOf()

  fun loadProject(): BibixIntellijProto.BibixProjectInfo {
    val mainTargets = buildFrontend.mainScriptDefinitions
    val mainBuildGraph = buildFrontend.buildGraphRunner.multiGraph.getProjectGraph(1)

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
      val evalCallees = candidates.map { it.evalCallee }.distinct()
      buildFrontend.parallelRunner.runTasks(evalCallees)
    }

    val supportedModuleClasses = JVMModules.entries.associateBy { it.className }

    targets = candidates.filter { candidate ->
      val callee = callees[candidate.evalCallee]
      if (callee != null && callee is BuildTaskResult.BuildRuleResult) {
        candidate.callee = callee
        val impl = callee.impl
        if (impl is BuildTaskResult.BuildRuleImpl.NonNativeImpl) {
          val moduleType = supportedModuleClasses[impl.implClassName]
          if (moduleType != null) {
            candidate.moduleType = moduleType
          }
          moduleType != null
        } else {
          false
        }
      } else {
        false
      }
    }
    evalTargets = targets.associateBy { it.evalTarget }

    val results = runBlocking {
      buildFrontend.parallelRunner.runTasksOrFailure(targets.map { it.evalTarget })
    }

    results.forEach { (key, result) ->
      println("$key: ${result.toString().substring(0, 100)}")
    }

    val projectInfoBuilder = ProjectInfoBuilder(
      buildFrontend.mainProjectLocation,
      targets.mapNotNull { target ->
        target.result?.let { classPkg ->
          if (classPkg.origin is LocalBuilt) {
            target.name to classPkg
          } else null
        }
      }.toMap(),
      buildFrontend.repo.repoData
    )

    return projectInfoBuilder.build()
  }

  private fun interceptTask(task: BuildTask): BuildTaskResult? {
    val candidate = evalTargets[task]
    if (candidate != null) {
      check(task is EvalTarget)
      val result = candidate.result
      if (result != null) {
        return BuildTaskResult.ValueOfTargetResult(
          result.toBibix(),
          candidate.buildContext!!.targetId
        )
      }

      return processCandidate(candidate)
    }
    return null
  }

  private val counter = AtomicInteger(0)

  private fun processCandidate(candidate: Candidate): BuildTaskResult {
    println("processCandidate: ${counter.incrementAndGet()} ${candidate.evalTarget.name}")
    val task = candidate.evalTarget
    val exprNodeId =
      buildFrontend.buildGraphRunner.multiGraph.getProjectGraph(task.projectId).targets.getValue(
        task.name
      )
    val evaluator = ExprEvaluator(
      buildFrontend.buildGraphRunner,
      task.projectId,
      task.importInstanceId,
      mapOf(),
      null
    )

    val exprNode = evaluator.exprGraph.nodes.getValue(exprNodeId)
    check(exprNode is CallExprNode)

    val namedParams = exprNode.namedParams.entries.toList()

    return BuildTaskResult.WithResultList(
      listOf(EvalCallee(task.projectId, task.importInstanceId, exprNode.callee)) +
        exprNode.posParams.map { evaluator.evalTask(it) } +
        namedParams.map { evaluator.evalTask(it.value) }
    ) { results ->
      check(results.size == 1 + exprNode.posParams.size + exprNode.namedParams.size)
      val callee = results.first()
      val buildRule = callee as BuildTaskResult.BuildRuleResult
      check(buildRule == candidate.callee)

      val posArgs = results.drop(1).take(exprNode.posParams.size).map { argResult ->
        argResult.toValue()
      }
      val namedArgs = namedParams.zip(results.drop(1 + exprNode.posParams.size))
        .associate { (param, argResult) ->
          param.key to argResult.toValue()
        }

      organizeParams(
        task.projectId,
        buildRule.paramTypes,
        buildRule.buildRuleDef.def.params.requiredParamNames(),
        buildRule.projectId,
        buildRule.importInstanceId,
        buildRule.buildRuleDef.paramDefaultValues,
        posArgs,
        namedArgs,
      ) { args ->
        withBuildContext(
          buildFrontend.buildGraphRunner,
          task.projectId,
          buildRule,
          args,
          noReuse = true,
        ) { context ->
          val srcs = context.arguments.getValue("srcs").toCollectionOfFile()
          val deps = context.arguments.getValue("deps").toCollectionOfPkgs()
          val runtimeDeps = context.arguments.getValue("runtimeDeps").toCollectionOfPkgs()
          val resultValue = ClassPkg(
            origin = LocalBuilt(
              context.targetId,
              candidate.moduleType!!.builderName
            ),
            cpinfo = ClassesInfo(listOf(), listOf(), srcs),
            deps = deps,
            runtimeDeps = runtimeDeps,
          )
          synchronized(candidate) {
            candidate.buildContext = context
            candidate.result = resultValue
          }
          BuildTaskResult.ValueOfTargetResult(resultValue.toBibix(), context.targetId)
        }
      }
    }
  }
}

enum class JVMModules(val builderName: String, val className: String) {
  JAVA("java.library", "com.giyeok.bibix.plugins.java.Library"),
  KTJVM("ktjvm.library", "com.giyeok.bibix.plugins.ktjvm.Library"),
  SCALA("scala.library", "com.giyeok.bibix.plugins.scala.Library");
}

fun BibixValue.toCollectionOfFile(): List<Path> = when (this) {
  is ListValue -> this.values.map { (it as FileValue).file }
  is SetValue -> this.values.map { (it as FileValue).file }
  else -> throw IllegalStateException()
}

fun BibixValue.toCollectionOfPkgs(): List<ClassPkg> = when (this) {
  is ListValue -> this.values.map { ClassPkg.fromBibix(it) }
  is SetValue -> this.values.map { ClassPkg.fromBibix(it) }
  else -> throw IllegalStateException()
}