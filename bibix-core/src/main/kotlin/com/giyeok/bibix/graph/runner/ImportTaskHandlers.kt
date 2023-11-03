package com.giyeok.bibix.graph.runner

import com.giyeok.bibix.ast.BibixParser
import com.giyeok.bibix.base.ClassInstanceValue
import com.giyeok.bibix.base.StringValue
import com.giyeok.bibix.graph.*
import com.giyeok.jparser.ktlib.ParsingErrorKt
import kotlin.io.path.absolute

fun BuildGraphRunner.handleImportTask(importTask: Import): BuildTaskResult {
  val buildGraph = multiGraph.getProjectGraph(importTask.projectId)
  val importAll = buildGraph.importAlls[importTask.importName]
  val importFrom = buildGraph.importFroms[importTask.importName]
  check((importAll != null && importFrom == null) || (importAll == null && importFrom != null))

  return if (importAll != null) {
    check(importFrom == null)
    BuildTaskResult.WithResult(
      EvalImportSource(
        importTask.projectId,
        importTask.importInstanceId,
        importTask.importName,
        importAll.source
      )
    ) { source ->
      check(source is BuildTaskResult.ImportInstanceResult)
      source
    }
  } else {
    // from bibix.plugins import ktjvm
    // from "../a" import xyz
    // import bibix.plugins as plgs
    // from ktjvm import library is ktjvmlib
    check(importFrom != null)
    BuildTaskResult.WithResult(
      EvalImportSource(
        importTask.projectId,
        importTask.importInstanceId,
        importTask.importName,
        importFrom.source
      )
    ) { source ->
      check(source is BuildTaskResult.ImportInstanceResult)

      importResultFrom(source, BibixName(importFrom.importing)) { it }
    }
  }
}

fun BuildGraphRunner.importResultFrom(
  importResult: BuildTaskResult.ImportInstanceResult,
  importingName: BibixName,
  block: (BuildTaskResult.FinalResult) -> BuildTaskResult
): BuildTaskResult {
  val importGraph = multiGraph.getProjectGraph(importResult.projectId)

  return when (val importEntry = importGraph.findName(importingName)) {
    is BuildGraphEntity.ImportAll -> {
      BuildTaskResult.WithResult(
        Import(importResult.projectId, importResult.importInstanceId, importingName)
      ) { importAllResult ->
        check(importAllResult is BuildTaskResult.ImportInstanceResult)
        block(importAllResult)
      }
    }

    is BuildGraphEntity.Target -> BuildTaskResult.WithResult(
      EvalTarget(importResult.projectId, importResult.importInstanceId, importingName),
      block
    )

    is BuildGraphEntity.Variable -> BuildTaskResult.WithResult(
      EvalVar(importResult.projectId, importResult.importInstanceId, importingName),
      block
    )

    is BuildGraphEntity.BuildRule -> BuildTaskResult.WithResult(
      EvalBuildRule(importResult.projectId, importResult.importInstanceId, importingName),
      block
    )

    is BuildGraphEntity.DataClass -> BuildTaskResult.WithResult(
      EvalDataClass(importResult.projectId, importResult.importInstanceId, importingName),
      block
    )

    is BuildGraphEntity.SuperClass -> BuildTaskResult.WithResult(
      EvalSuperClass(importResult.projectId, importResult.toString()),
      block
    )

    is BuildGraphEntity.Enum -> {
      block(BuildTaskResult.EnumTypeResult(
        importResult.projectId,
        checkNotNull(importGraph.packageName),
        importingName,
        importEntry.def.def.values
      ))
    }

    is BuildGraphEntity.Action -> BuildTaskResult.WithResult(
      EvalAction(importResult.projectId, importResult.importInstanceId, importingName),
      block
    )

    is BuildGraphEntity.ActionRule -> BuildTaskResult.WithResult(
      EvalActionRule(importResult.projectId, importResult.importInstanceId, importingName),
      block
    )

    is BuildGraphEntity.ImportFrom, null -> throw IllegalStateException("Cannot import")
  }
}

fun BuildGraphRunner.handleEvalImportSource(buildTask: EvalImportSource): BuildTaskResult {
  val projectId = buildTask.projectId
  val importInstanceId = buildTask.importInstanceId
  return when (val importSource = buildTask.importSource) {
    is ImportSource.Expr -> BuildTaskResult.WithResult(
      EvalExpr(projectId, importSource.exprNodeId, importInstanceId, null)
    ) { sourceResult ->
      check(sourceResult is BuildTaskResult.ResultWithValue)

      when (val sourceValue = sourceResult.value) {
        is StringValue -> {
          // 상대 경로로 다른 프로젝트 import
          val projectLocation = multiGraph.projectLocations.getValue(projectId)

          val importRoot =
            projectLocation.projectRoot.resolve(sourceValue.value).normalize().absolute()
          val importLocation = BibixProjectLocation(importRoot)

          handleImportLocation(projectId, importInstanceId, buildTask.importName, importLocation)
        }

        is ClassInstanceValue -> {
          check(sourceValue.packageName == "com.giyeok.bibix.prelude" && sourceValue.className == "BibixProject")

          // class BibixProject(projectRoot: directory, scriptName?: string)
          val projectRoot = sourceValue.getDirectoryField("projectRoot")
          val scriptName = sourceValue.getNullableStringField("scriptName")

          val importLocation = if (scriptName == null) {
            BibixProjectLocation(projectRoot)
          } else {
            BibixProjectLocation(projectRoot, scriptName)
          }

          handleImportLocation(projectId, importInstanceId, buildTask.importName, importLocation)
        }

        else -> TODO()
      }
    }

    is ImportSource.PreloadedPlugin -> {
      val importedProjectId = preloadedPluginIds.getValue(importSource.pluginName)

      createImportInstance(projectId, importInstanceId, buildTask.importName, importedProjectId)
    }

    is ImportSource.AnotherImport -> {
      BuildTaskResult.WithResult(
        Import(projectId, importInstanceId, importSource.importName)
      ) { result ->
        check(result is BuildTaskResult.ImportInstanceResult)
        result
      }
    }
  }
}

// NOTE handleImportLocation도 LongRunning 안에서 돌리면 좋겠지만 multiGraph가 thread-safe하지 않기 때문에 그냥 메인 스레드에서 돌도록 한다
// - 스크립트 읽고 파싱하는 부분만 LongRunning으로 뺄 수는 있을 것 같음
// - 그런데 그러면 읽어놓고 addProject 하려고 할 때 이미 같은 location의 프로젝트가 있으면 추가하지 않고 기존 것을 이미 사용하도록하면 되긴 할텐데
//   복잡도에 비해 benefit이 그다지 크지 않을 것 같음
private fun BuildGraphRunner.handleImportLocation(
  importerProjectId: Int,
  importerImportInstanceId: Int,
  importerImportName: BibixName,
  importLocation: BibixProjectLocation,
): BuildTaskResult {
  val importedProjectId: Int

  val existingProjectId = multiGraph.getProjectIdByLocation(importLocation)
  if (existingProjectId != null) {
    // 이미 로드된 프로젝트인 경우
    importedProjectId = existingProjectId
    // importedGraph = multiGraph.getProjectGraph(existingProjectId)
  } else {
    // 새로 로드해야 하는 프로젝트인 경우
    val importSource = importLocation.readScript()
    val importScript = try {
      BibixParser.parse(importSource)
    } catch (e: ParsingErrorKt.UnexpectedInput) {
      throw IllegalStateException(
        "Failed to parse: ${importLocation.projectRoot} ${importLocation.scriptName}", e
      )
    }
    val importedGraph =
      BuildGraph.fromScript(importScript, preloadedPluginIds.keys, preludeNames)

    importedProjectId = multiGraph.addProject(importLocation, importedGraph, importSource)
  }

  return createImportInstance(
    importerProjectId,
    importerImportInstanceId,
    importerImportName,
    importedProjectId,
  )
}

fun BuildGraphRunner.createImportInstance(
  importerProjectId: Int,
  importerImportInstanceId: Int,
  importerImportName: BibixName,
  importedProjectId: Int,
): BuildTaskResult.WithResult {
  val importerGraph = multiGraph.getProjectGraph(importerProjectId)
  val newRedefs = importerGraph.varRedefs[importerImportName] ?: mapOf()

  val newGlobalRedefs = newRedefs.mapValues { (_, exprNodeId) ->
    GlobalExprNodeId(importerProjectId, importerImportInstanceId, exprNodeId)
  }

  return BuildTaskResult.WithResult(
    NewImportInstance(importedProjectId, newGlobalRedefs)
  ) { instance ->
    check(instance is BuildTaskResult.ImportInstanceResult)
    check(importedProjectId == instance.projectId)

    instance
  }
}
