package com.giyeok.bibix.interpreter.expr

import com.giyeok.bibix.ast.BibixAst
import com.giyeok.bibix.base.*
import com.giyeok.bibix.interpreter.BibixProject
import com.giyeok.bibix.base.DirectoryType
import com.giyeok.bibix.base.PreloadedSourceId
import com.giyeok.bibix.interpreter.SourceManager
import com.giyeok.bibix.interpreter.task.Task
import com.giyeok.bibix.interpreter.task.TaskRelGraph
import com.giyeok.bibix.plugins.PreloadedPlugin
import com.giyeok.bibix.utils.toKtList

class NameLookup(
  private val g: TaskRelGraph,
  private val nameLookupTable: NameLookupTable,
  private val preloadedPlugins: Map<String, PreloadedPlugin>,
  private val exprEvaluator: ExprEvaluator,
  private val sourceManager: SourceManager,
) {
  suspend fun lookupName(
    requester: Task,
    context: NameLookupContext,
    name: List<String>
  ): Definition {
    val lookupResult = nameLookupTable.lookup(context, name)
    return handleLookupResult(requester, context, name, lookupResult)
  }

  private suspend fun handleLookupResult(
    requester: Task,
    context: NameLookupContext,
    name: List<String>,
    lookupResult: LookupResult
  ): Definition =
    when (lookupResult) {
      is LookupResult.DefinitionFound ->
        lookupResult.definition

      is LookupResult.ImportRequired -> {
        val import = lookupResult.import.import

        check(!nameLookupTable.isImported(context.sourceId, import.scopeName()))

        when (import) {
          is BibixAst.ImportAll -> resolveImportAll(requester, context, lookupResult, import)
          is BibixAst.ImportFrom -> resolveImportFrom(requester, context, lookupResult, import)
          else -> TODO()
        }

        lookupName(requester, context, name)
      }

      LookupResult.NameNotFound ->
        throw IllegalStateException(
          "Name not found: ${name.joinToString(".")} in ${sourceManager.descriptionOf(context.sourceId)}"
        )
    }

  private suspend fun resolveImportAll(
    requester: Task,
    context: NameLookupContext,
    lookupResult: LookupResult.ImportRequired,
    import: BibixAst.ImportAll
  ): Unit = g.withTask(requester, Task.ResolveImport(context.sourceId, import.id())) { task ->
    val importSource = resolveImportSource(task, context, lookupResult, import.source())
    nameLookupTable.addImport(lookupResult.import.cname, NameLookupContext(importSource, listOf()))
  }

  private suspend fun resolveImportFrom(
    requester: Task,
    context: NameLookupContext,
    lookupResult: LookupResult.ImportRequired,
    import: BibixAst.ImportFrom
  ): Unit = g.withTask(requester, Task.ResolveImport(context.sourceId, import.id())) { task ->
    val importSource = resolveImportSource(task, context, lookupResult, import.source())
    // lookupResult가 임포트하려던 것이 Definition을 직접 가리키고 있으면 그 definition을 등록
    val importedScope = NameLookupContext(importSource, listOf())
    val importingName = import.importing().tokens().toKtList()
    val importLookup = nameLookupTable.lookup(importedScope, importingName)
    val definition = handleLookupResult(requester, importedScope, importingName, importLookup)
    if (definition is Definition.NamespaceDef) {
      nameLookupTable.addImport(lookupResult.import.cname, NameLookupContext(definition.cname))
    } else {
      nameLookupTable.addImport(lookupResult.import.cname, definition)
    }
  }

  // nameLookupContext에서 importSource를 resolve하려고 하는 경우
  // 기본적으로는 evaluateExpr을 하고, 그러면 Bibix
  private suspend fun resolveImportSource(
    requester: Task,
    context: NameLookupContext,
    lookupResult: LookupResult.ImportRequired,
    importSource: BibixAst.Expr
  ): SourceId {
    suspend fun process(task: Task): SourceId {
      if (importSource is BibixAst.NameRef) {
        // TODO .bibixrc같은데서 source를 따로 정의하는 기능을 넣게 된다면 그땐 여기서 처리해야겠지
        // 일단은 기본 import는 SimpleName만 되도록..
        // TODO 이렇게 되면 preloaded plugin이 우선순위가 높아지는데.. 괜찮을까? 보통은 상관 없긴 할텐데
        val plugin = preloadedPlugins[importSource.name()]
        if (plugin != null) {
          val sourceId = PreloadedSourceId(importSource.name())
          val importedContext = NameLookupContext(sourceId, listOf())
          nameLookupTable.add(importedContext, plugin.defs)
          nameLookupTable.addImport(lookupResult.import.cname, importedContext)
          return sourceId
        }
      }

      val sourceValue = exprEvaluator.evaluateExpr(task, context, importSource, null).ensureValue()
      val importedProject = if (sourceValue is ClassInstanceValue) {
        BibixProject.fromBibixValue(sourceValue)
          ?: throw IllegalStateException("import source was not a BibixProject class value: $sourceValue")
      } else {
        val sourceRootDirectory = exprEvaluator.coerce(task, context, sourceValue, DirectoryType)
        BibixProject(
          projectRoot = (sourceRootDirectory as DirectoryValue).directory,
          scriptName = null,
        )
      }
      return sourceManager.loadSource(importedProject, nameLookupTable)
    }

    return g.withTask(
      requester,
      Task.ResolveImportSource(context.sourceId, importSource.id()),
    ) { task -> process(task) }
  }
}
