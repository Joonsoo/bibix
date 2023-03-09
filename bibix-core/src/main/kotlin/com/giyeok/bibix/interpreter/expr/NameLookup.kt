package com.giyeok.bibix.interpreter.expr

import com.giyeok.bibix.ast.BibixAst
import com.giyeok.bibix.base.*
import com.giyeok.bibix.interpreter.BibixExecutionException
import com.giyeok.bibix.interpreter.BibixProject
import com.giyeok.bibix.interpreter.ExprEvalContext
import com.giyeok.bibix.interpreter.SourceManager
import com.giyeok.bibix.interpreter.task.Task
import com.giyeok.bibix.interpreter.task.TaskRelGraph
import com.giyeok.bibix.plugins.PreloadedPlugin
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class NameLookup(
  private val g: TaskRelGraph,
  private val nameLookupTable: NameLookupTable,
  private val preloadedPlugins: Map<String, PreloadedPlugin>,
  private val exprEvaluator: ExprEvaluator,
  private val sourceManager: SourceManager,
  private val varsManager: VarsManager,
) {
  suspend fun lookupName(
    requester: Task,
    context: ExprEvalContext,
    name: List<String>
  ): Pair<Definition, VarsContext> {
    check(name.isNotEmpty())

    suspend fun lookup(
      lookupCtx: NameLookupContext,
      varsCtx: VarsContext,
      definition: Definition,
      tokens: List<String>
    ): Pair<Definition, VarsContext> = when (definition) {
      is Definition.ImportDef -> {
        // TODO load import
        val imported = nameLookupTable.getImport(definition.cname)
          ?: loadImport(
            requester,
            ExprEvalContext(lookupCtx, varsCtx),
            definition.cname,
            definition.import
          )
        // update varsCtx
        val redefs = varsManager.redefsInSource(lookupCtx.sourceId)
        // redefs 중 imported의 var를 가리키고 있는 것들을 추려서 newVarsCtx 구성
        val effectiveRedefs = redefs
          .mapNotNull { redef ->
            val redefTargetLookup =
              nameLookupTable.lookupInImport(redef.redefContext, redef.def.nameTokens)

            fun invalidVarRedefException() = IllegalStateException(
              "Invalid variable redefinition: ${redef.def} at ${
                sourceManager.descriptionOf(lookupCtx.sourceId)
              }"
            )
            // 위에서 우리가 관심있는 import는 loadImport가 완료된 상태이기 때문에, ImportRequired가 나온다면 우리가 관심 없는 import라는 의미이므로 제외
            when {
              redefTargetLookup is LookupInImportResult.ImportRequired -> null
              redefTargetLookup !is LookupInImportResult.InsideImport ->
                throw invalidVarRedefException()

              redefTargetLookup.importedSource.sourceId == imported.sourceId -> {
                val internalLookup = redefTargetLookup.lookupResult
                if (internalLookup !is LookupInImportResult.DefinitionFound ||
                  internalLookup.definition !is Definition.VarDef
                ) {
                  throw invalidVarRedefException()
                }
                redefTargetLookup.internalName to redef
              }

              else -> null
            }
          }
        val newVarsCtx =
          varsCtx.push(imported.sourceId, effectiveRedefs.associate { (redefName, redef) ->
            redefName to Pair(
              // TODO 여기서 varsCtx가 맞나?
              ExprEvalContext(redef.redefContext, varsCtx),
              redef.def.redefValue
            )
          })
        when (imported) {
          is ImportedSource.ImportedNames -> {
            if (tokens.isNotEmpty()) {
              lookupName(requester, ExprEvalContext(imported.nameLookupContext, newVarsCtx), tokens)
            } else {
              Pair(Definition.NamespaceDef(imported.nameLookupContext.toCName()), newVarsCtx)
            }
          }

          is ImportedSource.ImportedDefinition -> {
            check(tokens.isEmpty())
            Pair(imported.definition, newVarsCtx)
          }
        }
      }

      is Definition.NamespaceDef -> if (tokens.isEmpty()) {
        Pair(definition, varsCtx)
      } else {
        lookupName(requester, ExprEvalContext(NameLookupContext(definition.cname), varsCtx), tokens)
      }

      else -> {
        check(tokens.isEmpty())
        Pair(definition, varsCtx)
      }
    }

    val firstDef = nameLookupTable.findFirstToken(context.nameLookupContext, name.first())
      ?: throw BibixExecutionException("", g.upstreamPathTo(requester))
    return lookup(context.nameLookupContext, context.varsContext, firstDef, name.drop(1))
  }

  // TODO 여러 군데서 동시에 같은 소스를 loadImport하는 경우에 문제가 있는듯. 수정 필요. StateFlow로 해야될듯?
  suspend fun loadImport(
    task: Task,
    context: ExprEvalContext,
    importName: CName,
    import: BibixAst.ImportDef
  ): ImportedSource {
    check(!nameLookupTable.isImported(context.sourceId, import.scopeName()))

    return when (import) {
      is BibixAst.ImportAll ->
        resolveImportAll(task, context, importName, import)

      is BibixAst.ImportFrom ->
        resolveImportFrom(task, context, importName, import)
    }
  }

  private suspend fun handleLookupResult(
    task: Task,
    context: ExprEvalContext,
    name: List<String>,
    lookupResult: LookupResult
  ): Definition =
    when (lookupResult) {
      is LookupResult.DefinitionFound ->
        lookupResult.definition

      is LookupResult.ImportRequired -> {
        loadImport(task, context, lookupResult.import.cname, lookupResult.import.import)
        val nextLookupResult = nameLookupTable.lookup(context.nameLookupContext, name)
        handleLookupResult(task, context, name, nextLookupResult)
      }

      LookupResult.NameNotFound -> {
        val upstreamPath = g.upstreamPathTo(task)
        throw BibixExecutionException(
          "Name not found: ${name.joinToString(".")} in ${sourceManager.descriptionOf(context.sourceId)}",
          upstreamPath
        )
      }
    }

  private val importMutex = Mutex()
  private val importLoadFlows = mutableMapOf<CName, StateFlow<ImportedSource?>>()

  private suspend fun cachedImport(
    importName: CName,
    body: suspend () -> ImportedSource
  ): ImportedSource {
    val (flow, isNew) = importMutex.withLock {
      val existing = importLoadFlows[importName]
      if (existing == null) {
        val newFlow = MutableStateFlow<ImportedSource?>(null)
        importLoadFlows[importName] = newFlow
        Pair(newFlow, true)
      } else {
        Pair(existing, false)
      }
    }
    if (isNew) {
      val imported = body()
      (flow as MutableStateFlow).emit(imported)
    }
    return flow.filterNotNull().first()
  }

  private suspend fun resolveImportAll(
    requester: Task,
    context: ExprEvalContext,
    importName: CName,
    import: BibixAst.ImportAll
  ): ImportedSource = cachedImport(importName) {
    g.withTask(requester, g.resolveImportTask(context.sourceId, import)) { task ->
      val importSource = resolveImportSource(task, context, importName, import.source)
      nameLookupTable.addImport(importName, NameLookupContext(importSource, listOf()))
    }
  }

  private suspend fun resolveImportFrom(
    requester: Task,
    context: ExprEvalContext,
    importName: CName,
    import: BibixAst.ImportFrom
  ): ImportedSource = cachedImport(importName) {
    g.withTask(requester, g.resolveImportTask(context.sourceId, import)) { task ->
      val importSource = resolveImportSource(task, context, importName, import.source)
      // lookupResult가 임포트하려던 것이 Definition을 직접 가리키고 있으면 그 definition을 등록
      val importedScope = NameLookupContext(importSource, listOf())
      val importingName = import.importing.tokens
      val importLookup = nameLookupTable.lookup(importedScope, importingName)
      val definition = handleLookupResult(
        task,
        ExprEvalContext(importedScope, context.varsContext),
        importingName,
        importLookup
      )
      if (definition is Definition.NamespaceDef) {
        nameLookupTable.addImport(importName, NameLookupContext(definition.cname))
      } else {
        nameLookupTable.addImport(importName, definition)
      }
    }
  }

  // nameLookupContext에서 importSource를 resolve하려고 하는 경우
  // 기본적으로는 evaluateExpr을 하고, 그러면 Bibix
  private suspend fun resolveImportSource(
    requester: Task,
    context: ExprEvalContext,
    importName: CName,
    importSource: BibixAst.Expr
  ): SourceId {
    suspend fun process(task: Task): SourceId {
      if (importSource is BibixAst.NameRef) {
        // TODO .bibixrc같은데서 source를 따로 정의하는 기능을 넣게 된다면 그땐 여기서 처리해야겠지
        // 일단은 기본 import는 SimpleName만 되도록..
        // TODO 이렇게 되면 preloaded plugin이 우선순위가 높아지는데.. 괜찮을까? 보통은 상관 없긴 할텐데
        val plugin = preloadedPlugins[importSource.name]
        if (plugin != null) {
          val sourceId = PreloadedSourceId(importSource.name)
          val importedContext = NameLookupContext(sourceId, listOf())
          nameLookupTable.add(importedContext, plugin.defs)
          nameLookupTable.addImport(importName, importedContext)
          return sourceId
        }
      }

      val sourceValue =
        exprEvaluator.evaluateExpr(task, context, importSource, null, setOf()).ensureValue()
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
      Task.ResolveImportSource(context.sourceId, importSource.nodeId),
    ) { task -> process(task) }
  }
}
