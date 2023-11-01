package com.giyeok.bibix.interpreter

import com.giyeok.bibix.ast.BibixAst
import com.giyeok.bibix.base.BibixValue
import com.giyeok.bibix.base.BuildEnv
import com.giyeok.bibix.base.MainSourceId
import com.giyeok.bibix.base.NoneValue
import com.giyeok.bibix.interpreter.coroutine.ProgressIndicatorContainer
import com.giyeok.bibix.interpreter.coroutine.TaskElement
import com.giyeok.bibix.interpreter.expr.*
import com.giyeok.bibix.interpreter.task.Task
import com.giyeok.bibix.interpreter.task.TaskRelGraph
import com.giyeok.bibix.plugins.PreloadedPlugin
import com.giyeok.bibix.repo.BibixRepo
import com.google.common.annotations.VisibleForTesting
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class BibixInterpreter(
  val buildEnv: BuildEnv,
  val prelude: PreloadedPlugin,
  val preloadedPlugins: Map<String, PreloadedPlugin>,
  val pluginImplProvider: PluginImplProvider,
  val mainProject: BibixProject,
  val repo: BibixRepo,
  val progressIndicatorContainer: ProgressIndicatorContainer,
  val actionArgs: List<String>,
) {
  val g = TaskRelGraph()

  private val varsManager = VarsManager()

  val nameLookupTable = NameLookupTable(varsManager)

  @VisibleForTesting
  val sourceManager = SourceManager()

  @VisibleForTesting
  val exprEvaluator =
    ExprEvaluator(this, g, sourceManager, varsManager, pluginImplProvider, repo.directoryLocker)

  private val nameLookup =
    NameLookup(g, nameLookupTable, preloadedPlugins, exprEvaluator, sourceManager, varsManager)

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
      val mainContext = ExprEvalContext(NameLookupContext(MainSourceId, listOf()), VarsContext())
      val (definition, varsCtx) = nameLookup.lookupName(task, mainContext, nameTokens)
      // task가 targetDef이면 evaluateExpr, action def이면 executeAction, 그 외의 다른 것이면 오류
      val defName = definition.cname

      val context = ExprEvalContext(NameLookupContext(defName).dropLastToken(), varsCtx)

      when (definition) {
        is Definition.TargetDef ->
          exprEvaluator.evaluateName(task, context, listOf(defName.tokens.last()), null, setOf())
            .ensureValue()

        is Definition.ActionDef -> {
          suspend fun executeActionStmt(actionStmt: BibixAst.ActionStmt) {
            val argName = definition.action.argsName
            if (argName == null && actionArgs.isNotEmpty()) {
              throw IllegalStateException("action args is not used")
            }
            val args = if (argName == null) null else Pair(argName, actionArgs)
            when (actionStmt) {
              is BibixAst.CallExpr -> {
                exprEvaluator.executeAction(task, context, actionStmt, args)
              }

              is BibixAst.LetStmt -> TODO()
            }
          }

          when (val body = definition.action.body) {
            is BibixAst.SingleCallAction -> executeActionStmt(body.expr)
            is BibixAst.MultiCallActions -> body.exprs.forEach { expr ->
              executeActionStmt(expr)
            }

            else -> throw AssertionError()
          }
          NoneValue
        }

        else -> throw IllegalStateException("${nameTokens.joinToString(".")} is not a target or an action")
      }
    }
  }

  suspend fun lookupName(
    requester: Task,
    context: ExprEvalContext,
    name: List<String>
  ): Pair<Definition, VarsContext> = nameLookup.lookupName(requester, context, name)

  val taskDescriptor: TaskDescriptor = TaskDescriptor(g, sourceManager)
}
