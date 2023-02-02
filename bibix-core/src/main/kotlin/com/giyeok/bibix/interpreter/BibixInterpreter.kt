package com.giyeok.bibix.interpreter

import com.giyeok.bibix.base.*
import com.giyeok.bibix.interpreter.coroutine.TaskElement
import com.giyeok.bibix.interpreter.task.Task
import com.giyeok.bibix.interpreter.task.TaskRelGraph
import com.giyeok.bibix.plugins.PreloadedPlugin
import com.giyeok.bibix.repo.Repo
import com.giyeok.bibix.interpreter.coroutine.ProgressIndicatorContainer
import com.giyeok.bibix.interpreter.expr.*
import com.giyeok.bibix.utils.getOrNull
import com.google.common.annotations.VisibleForTesting
import kotlinx.coroutines.*

class BibixInterpreter(
  val buildEnv: BuildEnv,
  val prelude: PreloadedPlugin,
  val preloadedPlugins: Map<String, PreloadedPlugin>,
  val pluginImplProvider: PluginImplProvider,
  val mainProject: BibixProject,
  val repo: Repo,
  val progressIndicatorContainer: ProgressIndicatorContainer,
  val actionArgs: List<String>,
) {
  @VisibleForTesting
  val g = TaskRelGraph()

  private val varsManager = VarsManager()

  @VisibleForTesting
  val nameLookupTable = NameLookupTable(varsManager)

  @VisibleForTesting
  val sourceManager = SourceManager()

  @VisibleForTesting
  val exprEvaluator =
    ExprEvaluator(this, g, sourceManager, varsManager, pluginImplProvider, repo.directoryLocker)

  private val nameLookup =
    NameLookup(g, nameLookupTable, preloadedPlugins, exprEvaluator, sourceManager)

  init {
    runBlocking {
      sourceManager.loadPrelude(prelude, nameLookupTable)
      sourceManager.loadMainSource(mainProject, nameLookupTable)
      preloadedPlugins.forEach { (name, plugin) ->
        sourceManager.registerPreloadedPluginClasses(name, plugin)
      }
    }
  }

  suspend fun userBuildRequest(name: String): BibixValue =
    userBuildRequest(name.split('.').map { it.trim() })

  suspend fun userBuildRequest(nameTokens: List<String>): BibixValue {
    val task = Task.UserBuildRequest(nameTokens)
    return withContext(currentCoroutineContext() + TaskElement(task)) {
      val mainContext = NameLookupContext(MainSourceId, listOf())
      val definition = lookupName(task, mainContext, nameTokens)
      // task가 targetDef이면 evaluateExpr, action def이면 executeAction, 그 외의 다른 것이면 오류

      val defContext = NameLookupContext(definition.cname).dropLastToken()

      when (definition) {
        is Definition.TargetDef ->
          exprEvaluator.evaluateName(task, defContext, definition.cname.tokens, null)
            .ensureValue()

        is Definition.ActionDef -> {
          val actionExpr = definition.action.expr()
          val argName = definition.action.argsName().getOrNull()
          if (argName == null && actionArgs.isNotEmpty()) {
            throw IllegalStateException("action args is not used")
          }
          val args = if (argName == null) null else Pair(argName, actionArgs)
          exprEvaluator.executeAction(task, defContext, actionExpr, args)
          NoneValue
        }

        else -> throw IllegalStateException("${nameTokens.joinToString(".")} is not a target or an action")
      }
    }
  }

  suspend fun lookupName(
    requester: Task,
    context: NameLookupContext,
    name: List<String>
  ): Definition = nameLookup.lookupName(requester, context, name)
}
