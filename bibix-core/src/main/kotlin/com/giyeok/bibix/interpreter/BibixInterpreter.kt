package com.giyeok.bibix.interpreter

import com.giyeok.bibix.base.BibixValue
import com.giyeok.bibix.base.BuildEnv
import com.giyeok.bibix.base.MainSourceId
import com.giyeok.bibix.base.NoneValue
import com.giyeok.bibix.interpreter.coroutine.TaskElement
import com.giyeok.bibix.interpreter.task.Task
import com.giyeok.bibix.interpreter.task.TaskRelGraph
import com.giyeok.bibix.plugins.PreloadedPlugin
import com.giyeok.bibix.repo.Repo
import com.giyeok.bibix.interpreter.coroutine.ProgressIndicatorContainer
import com.giyeok.bibix.interpreter.expr.*
import com.google.common.annotations.VisibleForTesting
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

class BibixInterpreter(
  val buildEnv: BuildEnv,
  val preloadedPlugins: Map<String, PreloadedPlugin>,
  val mainProject: BibixProject,
  val repo: Repo,
  val progressIndicatorContainer: ProgressIndicatorContainer,
  val actionArgs: List<String>,
) {
  private val g = TaskRelGraph()

  val taskRelGraph get() = g

  private val varsManager = VarsManager()

  @VisibleForTesting
  val nameLookupTable = NameLookupTable(varsManager)

  @VisibleForTesting
  val sourceManager = SourceManager()

  private val exprEvaluator = ExprEvaluator(this, g, sourceManager, varsManager)

  private val nameLookup =
    NameLookup(g, nameLookupTable, preloadedPlugins, exprEvaluator, sourceManager)

  init {
    runBlocking {
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
          exprEvaluator.evaluateExpr(task, defContext, definition.target.value(), null)
            .ensureValue()

        is Definition.ActionDef -> {
          exprEvaluator.executeAction(task, defContext, definition.action.expr())
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
