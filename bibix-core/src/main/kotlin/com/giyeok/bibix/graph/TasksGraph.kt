package com.giyeok.bibix.graph

import com.giyeok.bibix.ast.BibixAst


// TasksGraph는 프로젝트 하나(즉 스크립트 하나)의 내용만 포함한다.
// import해서 사용하는 다른 프로젝트의 def들은 별도의 TasksGraph로 관리한다.
class TasksGraph(
  val nameLookup: NameLookupTable,
  val nodes: Map<TaskId, TaskNode>,
  val edges: Map<TaskEdge, TaskEdgeType>,
  val scriptVars: Map<String, TaskId>,
) {
  val edgesByStart = edges.keys.groupBy { it.start }
  val edgesByEnd = edges.keys.groupBy { it.end }

  companion object {
    fun fromScript(script: BibixAst.BuildScript): TasksGraph {
      val nameLookup = NameLookupTable.fromScript(script)
      val builder = Builder(nameLookup, mutableMapOf(), mutableMapOf(), mutableMapOf())
      val rootNameScope = ScopedNameLookupTable(listOf(), nameLookup, null)
      builder.addDefs(script.defs, NameLookupContext(nameLookup, setOf("glob"), rootNameScope))
      return builder.build()
    }
  }

  class Builder(
    val nameLookup: NameLookupTable,
    val nodes: MutableMap<TaskId, TaskNode>,
    val edges: MutableMap<TaskEdge, TaskEdgeType>,
    val scriptVars: MutableMap<String, TaskId>,
  ) {
    fun build(): TasksGraph = TasksGraph(nameLookup, nodes, edges, scriptVars)

    fun addDefs(defs: List<BibixAst.Def>, nameLookupCtx: NameLookupContext) {
      defs.forEach { def ->
        when (def) {
          is BibixAst.ImportDef -> {
            addNode(ImportNode(def))
          }

          is BibixAst.NamespaceDef -> {
            addDefs(def.body, nameLookupCtx.innerNamespace(def.name))
          }

          is BibixAst.TargetDef -> {
            val targetNode = addNode(TargetNode(def))
            val valueNode = addExpr(def.value, nameLookupCtx)
            addEdge(targetNode, valueNode, DefinitionEdge)
          }

          is BibixAst.ActionDef -> {
            // TODO
          }

          is BibixAst.DataClassDef -> {
            // TODO
          }

          is BibixAst.SuperClassDef -> {
            // TODO
          }

          is BibixAst.EnumDef -> {
            // TODO
          }

          is BibixAst.VarDef -> {
            // TODO
          }

          is BibixAst.VarRedefs -> {
            // TODO
          }

          is BibixAst.BuildRuleDef -> {
            // TODO
          }

          is BibixAst.ActionRuleDef -> {
            // TODO
          }

          is BibixAst.DefsWithVarRedefs -> {
            // TODO
          }
        }
        val id = TaskId.fromAstNode(def)
      }
    }

    fun BibixAst.Name.toContextString(): String =
      "${this.tokens.joinToString(".")} at ${this.start}..${this.end}"

    fun List<String>.hasNoDuplicate(): Boolean =
      this.toSet().size == this.size

    fun addExpr(expr: BibixAst.Expr, nameLookupCtx: NameLookupContext): TaskId = when (expr) {
      is BibixAst.CastExpr -> addExpr(expr.expr, nameLookupCtx)
      is BibixAst.MergeOp -> {
        val mergedNode = addNode(ExprNode(expr))
        val lhsNode = addExpr(expr.lhs, nameLookupCtx)
        val rhsNode = addExpr(expr.rhs, nameLookupCtx)
        addEdge(mergedNode, lhsNode, ValueDependencyEdge)
        addEdge(mergedNode, rhsNode, ValueDependencyEdge)
        mergedNode
      }

      is BibixAst.CallExpr -> {
        val calledNode = addNode(ExprNode(expr))
        when (val lookupResult = nameLookupCtx.lookupName(expr.name)) {
          is NameEntryFound -> {
            val ruleEntry = lookupResult.entry.id
            addEdge(calledNode, ruleEntry, RuleDependencyEdge)
          }

          is NameInImport -> {
            check(lookupResult.remaining.isNotEmpty())
            addEdge(
              calledNode,
              lookupResult.importEntry.id,
              ImportDependencyEdge(lookupResult.remaining)
            )
          }

          is NameFromPrelude -> {
            // TODO
          }

          is NamespaceFound -> throw IllegalStateException("Invalid rule: ${expr.name.toContextString()}")
        }
        expr.params.posParams.forEach { param ->
          val paramNode = addExpr(param, nameLookupCtx)
          addEdge(calledNode, paramNode, ValueDependencyEdge)
        }
        check(expr.params.namedParams.map { it.name }.hasNoDuplicate())
        expr.params.namedParams.forEach { (_, param) ->
          val paramNode = addExpr(param, nameLookupCtx)
          addEdge(calledNode, paramNode, ValueDependencyEdge)
        }
        calledNode
      }

      is BibixAst.ListExpr -> {
        val listNode = addNode(ExprNode(expr))
        expr.elems.forEach { elem ->
          when (elem) {
            is BibixAst.EllipsisElem -> {
              val elemNode = addExpr(elem.value, nameLookupCtx)
              addEdge(listNode, elemNode, ValueDependencyEdge)
            }

            is BibixAst.Expr -> {
              val elemNode = addExpr(elem, nameLookupCtx)
              addEdge(listNode, elemNode, ValueDependencyEdge)
            }
          }
        }
        listNode
      }

      is BibixAst.BooleanLiteral, is BibixAst.NoneLiteral -> addNode(ExprNode(expr))
      is BibixAst.StringLiteral -> {
        val stringNode = addNode(ExprNode(expr))
        expr.elems.forEach { elem ->
          when (elem) {
            is BibixAst.EscapeChar, is BibixAst.JustChar -> {
              // do nothing
            }

            is BibixAst.ComplexExpr -> {
              val exprNode = addExpr(elem.expr, nameLookupCtx)
              addEdge(stringNode, exprNode, ValueDependencyEdge)
            }

            is BibixAst.SimpleExpr -> {
              // TODO lookup
              elem.name
            }
          }
        }
        stringNode
      }

      is BibixAst.MemberAccess -> {
        val accessNode = addNode(ExprNode(expr))
        val targetNode = addExpr(expr.target, nameLookupCtx)
        addEdge(accessNode, targetNode, ValueDependencyEdge)
        accessNode
      }

      is BibixAst.NameRef -> {
        val nameNode = addNode(ExprNode(expr))
        when (val lookupResult = nameLookupCtx.lookupName(expr)) {
          is NameEntryFound -> {
            addEdge(nameNode, lookupResult.entry.id, ReferenceEdge)
          }

          is NameInImport -> {
            // TODO
            println(lookupResult)
          }

          is NamespaceFound -> {
            // TODO
            println(lookupResult)
          }

          is NameFromPrelude -> {
            // TODO
            println(lookupResult)
          }
        }
        nameNode
      }

      is BibixAst.NamedTupleExpr -> {
        val tupleNode = addNode(ExprNode(expr))
        expr.elems.forEach { elem ->
          val elemNode = addExpr(elem.expr, nameLookupCtx)
          addEdge(tupleNode, elemNode, ValueDependencyEdge)
        }
        tupleNode
      }

      is BibixAst.Paren -> addExpr(expr.expr, nameLookupCtx)
      is BibixAst.This -> TODO()
      is BibixAst.TupleExpr -> {
        val tupleNode = addNode(ExprNode(expr))
        expr.elems.forEach { elem ->
          val elemNode = addExpr(elem, nameLookupCtx)
          addEdge(tupleNode, elemNode, ValueDependencyEdge)
        }
        tupleNode
      }
    }

    fun addNode(node: TaskNode): TaskId {
      check(!nodes.contains(node.id))
      nodes[node.id] = node
      return node.id
    }

    fun addEdge(start: TaskId, end: TaskId, edgeType: TaskEdgeType) {
      edges[TaskEdge(start, end)] = edgeType
    }
  }
}

// TasksGraph에서 TaskId는 스크립트 내에서 해당 task가 정의된 위치로 정의된다.
// TasksGraph는 스크립트 내의 def들의 관계를 나타낼 뿐이고
// 실제 해당 task가 어떻게 쓰이고 어떤 값으로 evaluate되는지는 다음 문제.
data class TaskId(val nodeId: Int, val start: Int, val end: Int) {
  companion object {
    fun fromAstNode(astNode: BibixAst.AstNode) = TaskId(astNode.nodeId, astNode.start, astNode.end)
  }
}

sealed class TaskNode {
  abstract val id: TaskId
}

data class ImportNode(val import: BibixAst.ImportDef): TaskNode() {
  override val id: TaskId = TaskId.fromAstNode(import)
}

data class TargetNode(val def: BibixAst.TargetDef): TaskNode() {
  override val id: TaskId = TaskId.fromAstNode(def)
}

data class ExprNode(val expr: BibixAst.Expr): TaskNode() {
  override val id: TaskId = TaskId.fromAstNode(expr)
}

data class TaskEdge(val start: TaskId, val end: TaskId)

// TODO import한 다른 프로젝트의 var redef 정보들 모아서 관리
data class ImportVarRedef(val importDef: TaskId, val varName: String, val redefValue: TaskId)

sealed class TaskEdgeType
data object DefinitionEdge: TaskEdgeType()
data object ValueDependencyEdge: TaskEdgeType()
data object RuleDependencyEdge: TaskEdgeType()
data object ReferenceEdge: TaskEdgeType()
data class ImportDependencyEdge(val remainingName: List<String>): TaskEdgeType()
