package com.giyeok.bibix.graph

import com.giyeok.bibix.ast.BibixAst

sealed class TaskNode {
  abstract val id: TaskId
}

data class ImportInstanceNode(val importNode: TaskId, val varRedefs: Map<String, TaskId>):
  TaskNode() {
  override val id: TaskId = TaskId(importNode.nodeId, varRedefs)
}

data class ImportNode(val import: BibixAst.ImportDef): TaskNode() {
  override val id: TaskId = TaskId(import.nodeId)
}

data class ImportedTaskNode(val importNode: TaskId, val remainingNames: List<String>): TaskNode() {
  override val id: TaskId = TaskId(importNode.nodeId, Pair(importNode, remainingNames))
}

data class PreloadedPluginNode(val name: String): TaskNode() {
  override val id: TaskId get() = TaskId(0, this)
}

data class PreludeTaskNode(val name: String): TaskNode() {
  override val id: TaskId = TaskId(0, this)
}

data class TargetNode(val def: BibixAst.TargetDef): TaskNode() {
  override val id: TaskId = TaskId(def.nodeId)
}

data class VarNode(val def: BibixAst.VarDef): TaskNode() {
  override val id: TaskId = TaskId(def.nodeId)
}

sealed class ExprNode<T: BibixAst.Expr>(val expr: T): TaskNode() {
  override val id: TaskId = TaskId(expr.nodeId)
}

data class CastExprNode(
  val castExpr: BibixAst.CastExpr,
  val value: TaskId,
  val type: TaskId
): ExprNode<BibixAst.CastExpr>(castExpr)

data class MergeExprNode(
  val mergeExpr: BibixAst.MergeOp,
  val lhs: TaskId,
  val rhs: TaskId
): ExprNode<BibixAst.MergeOp>(mergeExpr)

data class CallExprNode(
  val callExpr: BibixAst.CallExpr,
  val rule: TaskId,
  val posParams: List<TaskId>,
  val namedParams: Map<String, TaskId>
): ExprNode<BibixAst.CallExpr>(callExpr)

data class ListExprNode(
  val listExpr: BibixAst.ListExpr,
  val elems: List<ListElem>,
): ExprNode<BibixAst.ListExpr>(listExpr) {
  data class ListElem(val valueNode: TaskId, val isEllipsis: Boolean)
}

data class LiteralNode(val literal: BibixAst.Literal): ExprNode<BibixAst.Literal>(literal)

data class StringNode(
  val stringExpr: BibixAst.StringLiteral,
  val exprElems: List<TaskId>
): ExprNode<BibixAst.StringLiteral>(stringExpr)

data class MemberAccessNode(
  val memberAccessExpr: BibixAst.MemberAccess,
  val target: TaskId,
): ExprNode<BibixAst.MemberAccess>(memberAccessExpr)

data class NameRefNode(
  val nameRefExpr: BibixAst.NameRef,
  val valueNode: TaskId
): ExprNode<BibixAst.NameRef>(nameRefExpr)

data class TupleNode(
  val tupleExpr: BibixAst.TupleExpr,
  val elemNodes: List<TaskId>,
): ExprNode<BibixAst.TupleExpr>(tupleExpr)

data class NamedTupleNode(
  val namedTupleExpr: BibixAst.NamedTupleExpr,
  val elemNodes: List<Pair<String, TaskId>>,
): ExprNode<BibixAst.NamedTupleExpr>(namedTupleExpr)

data class EtcExprNode(val etcExpr: BibixAst.Expr): ExprNode<BibixAst.Expr>(etcExpr) {
  init {
    check(etcExpr !is BibixAst.CallExpr)
  }
}


data class TaskEdge(val start: TaskId, val end: TaskId, val edgeType: TaskEdgeType)

enum class TaskEdgeType {
  Definition, ValueDependency, RuleDependency, Reference, ImportDependency, TypeDependency, ImportInstance,

  // default value는 evaluation할 때 빠질 수도 있다는 의미
  DefaultValueDependency
}
