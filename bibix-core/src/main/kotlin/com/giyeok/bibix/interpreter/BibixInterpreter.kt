package com.giyeok.bibix.interpreter

import com.giyeok.bibix.ast.BibixAst
import com.giyeok.bibix.base.*
import com.giyeok.bibix.interpreter.name.AliasGraph
import com.giyeok.bibix.interpreter.name.NameLookupContext
import com.giyeok.bibix.interpreter.name.NameLookupTable
import com.giyeok.bibix.interpreter.task.Task
import com.giyeok.bibix.interpreter.task.TaskRelGraph
import com.giyeok.bibix.plugins.PreloadedPlugin
import com.giyeok.bibix.repo.Repo

class BibixInterpreter(
  val buildEnv: BuildEnv,
  val preloadedPlugins: Map<String, PreloadedPlugin>,
  val repo: Repo,
  val bibixArgs: Map<List<String>, BibixValue>,
) {
  private val taskRelGraph = TaskRelGraph()
  private val aliasGraph = AliasGraph()
  private val nameLookupTable = NameLookupTable()
  private val sourceManager = SourceManager()

  suspend fun userBuildRequest(name: CName): BibixValue {
    val task = Task.UserBuildRequest(name)
    // task가 nameDef이면 evaluateExpr, action def이면 executeAction, 그 외의 다른 것이면 오류
    TODO()
  }

  suspend fun evaluateExpr(
    requester: Task,
    sourceId: SourceId,
    expr: BibixAst.Expr,
    thisValue: BibixValue?,
  ): BibixValue {
    val task = taskRelGraph.add(requester, Task.EvaluateExpr(sourceId, expr.id(), thisValue))
    TODO()
  }

  suspend fun executeAction(requester: Task, sourceId: SourceId, actionDef: BibixAst.ActionDef) {
    val task = taskRelGraph.add(requester, Task.ExecuteAction(sourceId, actionDef.id()))
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

  // TODO ImportName이나 ImportAll인 경우엔 NameLookupContext가 되고
  // ImportFrom인 경우엔 가리키고 있는 것이 NameDef이면 BibixValue가, namespace이면 NameLookupContext가 되어야 할듯?
  suspend fun resolveImport(
    requester: Task,
    sourceId: SourceId,
    importDef: BibixAst.ImportDef
  ): NameLookupContext {
    val task = taskRelGraph.add(requester, Task.ResolveImport(sourceId, importDef.id()))
    TODO()
  }
}
