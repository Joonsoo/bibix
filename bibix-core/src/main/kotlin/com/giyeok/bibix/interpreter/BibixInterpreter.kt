package com.giyeok.bibix.interpreter

import com.giyeok.bibix.ast.BibixAst
import com.giyeok.bibix.base.*
import com.giyeok.bibix.interpreter.expr.ExprEvaluator
import com.giyeok.bibix.interpreter.name.*
import com.giyeok.bibix.interpreter.task.Task
import com.giyeok.bibix.interpreter.task.TaskRelGraph
import com.giyeok.bibix.plugins.PreloadedPlugin
import com.giyeok.bibix.repo.Repo
import com.giyeok.bibix.utils.toKtList
import com.google.common.annotations.VisibleForTesting
import kotlinx.coroutines.runBlocking

class BibixInterpreter(
  val buildEnv: BuildEnv,
  val preloadedPlugins: Map<String, PreloadedPlugin>,
  val mainProject: BibixProject,
  val repo: Repo,
  val bibixArgs: Map<List<String>, BibixValue>,
) {
  private val g = TaskRelGraph()

  private val aliasGraph = AliasGraph()

  @VisibleForTesting
  val nameLookupTable = NameLookupTable()

  @VisibleForTesting
  val sourceManager = SourceManager()

  private val exprEvaluator = ExprEvaluator(this, g, sourceManager)

  init {
    runBlocking {
      sourceManager.loadMainSource(mainProject, nameLookupTable)
      preloadedPlugins.forEach { (name, plugin) ->
        sourceManager.registerPreloadedPluginClasses(name, plugin)
      }
    }
  }

  suspend fun argValue(arg: Definition.ArgDef): BibixValue {
    TODO()
  }

  suspend fun userBuildRequest(nameTokens: List<String>): BibixValue {
    val task = Task.UserBuildRequest(nameTokens)
    val mainContext = NameLookupContext(MainSourceId, listOf())
    val definition = lookupName(task, mainContext, nameTokens)
    // task가 targetDef이면 evaluateExpr, action def이면 executeAction, 그 외의 다른 것이면 오류

    when (definition) {
      is Definition.TargetDef ->
        return exprEvaluator.evaluateExpr(task, mainContext, definition.target.value(), null)
          .ensureValue()

      is Definition.ActionDef -> TODO()

      else -> throw IllegalStateException("${nameTokens.joinToString(".")} is not a target or an action")
    }
  }

  suspend fun lookupName(
    requester: Task,
    context: NameLookupContext,
    name: List<String>
  ): Definition =
    handleLookupResult(requester, context, name, nameLookupTable.lookup(context, name))

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
        throw IllegalStateException("Name not found: ${name.joinToString(".")}")
    }


  suspend fun executeAction(requester: Task, sourceId: SourceId, actionDef: BibixAst.ActionDef) {
    val task = g.add(requester, Task.ExecuteAction(sourceId, actionDef.id()))
    TODO()
  }

  suspend fun resolveBuildRuleName(requester: Task, name: CName): BuildRuleDefValue {
    TODO()
  }

  suspend fun resolveActionRuleName(requester: Task, name: CName): ActionRuleDefValue {
    TODO()
  }

  suspend fun resolveArgValue(requester: Task, name: CName): BibixValue {
    TODO()
  }

  private suspend fun resolveImportAll(
    requester: Task,
    context: NameLookupContext,
    lookupResult: LookupResult.ImportRequired,
    import: BibixAst.ImportAll
  ): Unit = g.withTask(requester, Task.ResolveImport(context.sourceId, import.id())) { task ->
    val importSource = resolveImportSource(task, context, lookupResult, import.source())
    check(
      lookupResult.import.cname == CName(
        context.sourceId,
        context.scopePath + import.scopeName()
      )
    )
    nameLookupTable.addImport(lookupResult.import.cname, NameLookupContext(importSource, listOf()))
  }

  private suspend fun resolveImportFrom(
    requester: Task,
    context: NameLookupContext,
    lookupResult: LookupResult.ImportRequired,
    import: BibixAst.ImportFrom
  ): Unit = g.withTask(requester, Task.ResolveImport(context.sourceId, import.id())) { task ->
    val importSource = resolveImportSource(task, context, lookupResult, import.source())
    check(
      lookupResult.import.cname == CName(context.sourceId, context.scopePath + import.scopeName())
    )
    // lookupResult가 임포트하려던 것이 Definition을 직접 가리키고 있으면 그 definition을 등록
    val importedScope = NameLookupContext(importSource, listOf())
    val restName = lookupResult.restName + import.importing().tokens().toKtList()
    val definition = handleLookupResult(
      requester,
      context,
      restName,
      nameLookupTable.lookup(importedScope, restName)
    )
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
      check(sourceValue is ClassInstanceValue) { "import source was not a BibixProject class value" }
      check(sourceValue.packageName == "com.giyeok.bibix" && sourceValue.className == "BibixProject") { "import source was not a BibixProject class value" }
      val importedProject = BibixProject(
        projectRoot = (sourceValue.fieldValues.getValue("projectRoot") as DirectoryValue).directory,
        scriptName = (sourceValue.fieldValues["scriptName"] as? StringValue)?.value
      )
      return sourceManager.loadSource(importedProject, nameLookupTable)
    }

    return g.withTask(
      requester,
      Task.ResolveImportSource(context.sourceId, importSource.id()),
      ::process
    )
  }
}
