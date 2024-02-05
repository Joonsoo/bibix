package com.giyeok.bibix.intellij.service

import com.giyeok.bibix.base.BibixValue
import com.giyeok.bibix.base.FileValue
import com.giyeok.bibix.base.ListValue
import com.giyeok.bibix.base.NoneValue
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
import com.giyeok.bibix.graph.runner.FailureOr
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
import kotlin.io.path.absolute
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries

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

    val projectInfoBuilder = ProjectInfoBuilder(
      buildFrontend.mainProjectLocation,
      targets.mapNotNull { target ->
        results[target.evalTarget]?.let { result ->
          when (result) {
            is FailureOr.Result -> {
              val classPkg = ClassPkg.fromBibix(result.result.toValue())
              if (classPkg.origin is LocalBuilt) {
                target.name to classPkg
              } else null
            }

            is FailureOr.Failure -> {
              println("Failed to process ${target.evalTarget.name}")
              null
            }
          }
        }
      }.toMap(),
      buildFrontend.repo.repoData
    )

    return projectInfoBuilder.build(buildFrontend)
  }

  private fun interceptTask(task: BuildTask): BuildTaskResult? {
    val candidate = evalTargets[task]
    if (candidate != null) {
      check(task is EvalTarget)
      return processCandidate(candidate)
    }
    return null
  }

  private fun processCandidate(candidate: Candidate): BuildTaskResult {
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
      exprNode.posParams.map { evaluator.evalTask(it) } +
        namedParams.map { evaluator.evalTask(it.value) }
    ) { results ->
      check(results.size == exprNode.posParams.size + exprNode.namedParams.size)
      val buildRule = candidate.callee!!

      val posArgs = results.take(exprNode.posParams.size).map { argResult ->
        argResult.toValue()
      }
      val namedArgs = namedParams.zip(results.drop(exprNode.posParams.size))
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
          val resources = context.arguments.getValue("resources").toCollectionOfFile()
          val resDirs = findResourceDirectoriesOf(resources)
          val deps0 = context.arguments.getValue("deps").toCollectionOfPkgs()
          val sdk = context.arguments["sdk"]?.let { value ->
            if (value == NoneValue) null else ClassPkg.fromBibix(value)
          }
          val runtimeDeps = context.arguments.getValue("runtimeDeps").toCollectionOfPkgs()
          // TODO deps에 stdlib dependency 추가
          val deps = when (candidate.moduleType!!) {
            JVMModules.JAVA -> deps0
            JVMModules.KTJVM -> listOfNotNull(sdk) + deps0
            JVMModules.SCALA -> listOfNotNull(sdk) + deps0
          }
          val resultValue = ClassPkg(
            origin = LocalBuilt(
              context.targetId,
              candidate.moduleType!!.builderName
            ),
            cpinfo = ClassesInfo(listOf(), resDirs, srcs),
            deps = deps,
            runtimeDeps = runtimeDeps,
          )
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

fun allFilesOf(directory: Path): Set<Path> =
  directory.listDirectoryEntries().flatMap { sub ->
    if (sub.isDirectory()) {
      allFilesOf(sub)
    } else {
      listOf(sub)
    }
  }.toSet()

fun findResourceDirectoriesOf(paths: Collection<Path>): List<Path> {
  val mutPaths = paths.toMutableSet()
  val resDirs = mutableSetOf<Path>()
  while (mutPaths.isNotEmpty()) {
    val path = mutPaths.first()
    val directory = path.parent
    val dirFiles = allFilesOf(directory)
    if (paths.containsAll(dirFiles)) {
      resDirs.removeIf { it.startsWith(directory) }
      resDirs.add(directory)
      mutPaths.removeAll(dirFiles)
    } else {
      resDirs.add(path)
    }
    mutPaths.remove(path)
  }
  return resDirs.map { it.absolute() }.toList().sorted()
}
