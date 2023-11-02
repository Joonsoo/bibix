package com.giyeok.bibix.graph

import com.giyeok.bibix.ast.BibixAst


class ExprGraph(val nodes: Map<ExprNodeId, ExprGraphNode>, val edges: Set<ExprGraphEdge>)

data class ExprGraphEdge(val start: ExprNodeId, val end: ExprNodeId)

data class ExprTypeEdge(val start: ExprNodeId, val end: TypeNodeId)

sealed class ExprNodeId {
  data class LocalDefRefNodeId(val astNodeId: Int): ExprNodeId()
  data class ExprAstNodeId(val astNodeId: Int): ExprNodeId()

  // 개발용 - TODO 삭제
  data class AnyNodeId(val any: Any): ExprNodeId()
}

sealed class ExprGraphNode {
  abstract val id: ExprNodeId
}

sealed class ExprLocalRefNode(val ast: BibixAst.Def): ExprGraphNode() {
  override val id: ExprNodeId = ExprNodeId.LocalDefRefNodeId(ast.nodeId)
}

data class LocalTargetRef(val name: BibixName, val def: BibixAst.TargetDef): ExprLocalRefNode(def)

data class LocalBuildRuleRef(val name: BibixName, val def: BibixAst.BuildRuleDef):
  ExprLocalRefNode(def)

data class LocalVarRef(val name: BibixName, val def: BibixAst.VarDef): ExprLocalRefNode(def)

data class LocalActionRef(val name: BibixName, val def: BibixAst.ActionDef): ExprLocalRefNode(def)

data class LocalActionRuleRef(
  val name: BibixName,
  val def: BibixAst.ActionRuleDef
): ExprLocalRefNode(def)

// callee에서 사용될 수 있음
data class LocalDataClassRef(val name: BibixName, val def: BibixAst.DataClassDef):
  ExprLocalRefNode(def)

data class LocalEnumValue(val enumType: BibixName, val enumValue: String): ExprGraphNode() {
  override val id: ExprNodeId get() = ExprNodeId.AnyNodeId(this)
}

data class PreloadedPluginRef(val pluginName: String): ExprGraphNode() {
  override val id: ExprNodeId get() = ExprNodeId.AnyNodeId(this)
}

data class ImportedExprFromPreloaded(
  val pluginName: String,
  val name: BibixName
): ExprGraphNode() {
  override val id: ExprNodeId get() = ExprNodeId.AnyNodeId(this)
}

data class ImportedExprFromPrelude(val name: String, val remaining: List<String>): ExprGraphNode() {
  override val id: ExprNodeId get() = ExprNodeId.AnyNodeId(this)
}

data class ImportedExpr(val import: BibixName, val name: BibixName): ExprGraphNode() {
  override val id: ExprNodeId get() = ExprNodeId.AnyNodeId(this)
}

data class ValueCastNode(
  val ast: BibixAst.AstNode,
  val value: ExprNodeId,
  val type: TypeNodeId,
): ExprGraphNode() {
  override val id: ExprNodeId get() = ExprNodeId.AnyNodeId(this)
}

data class CallExprNode(
  val callExpr: BibixAst.CallExpr,
  val callNode: ExprNodeId, // CallExprCallNode,
  val callee: ExprNodeId, // callNode의 callee와 같아야 함
): ExprGraphNode() {
  override val id: ExprNodeId get() = ExprNodeId.AnyNodeId(this)
}

data class CallExprCallNode(
  val callExpr: BibixAst.CallExpr,
  // callee는 build rule 혹은 data class type 이어야 함
  // 그러려면 ImportedExpr, LocalBuildRuleRef, LocalDataClassRef 중 하나여야 함
  val callee: ExprNodeId,
  val posParams: List<ExprNodeId>,
  val namedParams: Map<String, ExprNodeId>,
): ExprGraphNode() {
  override val id: ExprNodeId get() = ExprNodeId.AnyNodeId(this)
}

data class CallExprParamCoercionNode(
  val value: ExprNodeId,
  val callee: ExprNodeId,
  val paramLocation: ParamLocation
): ExprGraphNode() {
  override val id: ExprNodeId get() = ExprNodeId.AnyNodeId(this)
}

sealed class ParamLocation {
  data class PosParam(val idx: Int): ParamLocation()
  data class NamedParam(val name: String): ParamLocation()
}

sealed class ExprAstNode<T: BibixAst.Expr>(val ast: T): ExprGraphNode() {
  override val id: ExprNodeId get() = ExprNodeId.ExprAstNodeId(ast.nodeId)
}

data class MergeExprNode(
  val expr: BibixAst.MergeOp,
  val lhs: ExprNodeId,
  val rhs: ExprNodeId,
): ExprAstNode<BibixAst.MergeOp>(expr)

data class ListExprNode(
  val expr: BibixAst.ListExpr,
  val elems: List<ListElem>
): ExprAstNode<BibixAst.ListExpr>(expr)

data class ListElem(val value: ExprNodeId, val isEllipsis: Boolean)

data class BooleanLiteralNode(
  val expr: BibixAst.BooleanLiteral
): ExprAstNode<BibixAst.BooleanLiteral>(expr)

data class NoneLiteralNode(
  val expr: BibixAst.NoneLiteral
): ExprAstNode<BibixAst.NoneLiteral>(expr)

data class StringNode(
  val expr: BibixAst.StringLiteral,
  val elems: List<ExprNodeId>
): ExprAstNode<BibixAst.StringLiteral>(expr)

data class MemberAccessNode(
  val expr: BibixAst.MemberAccess,
  val target: ExprNodeId,
  val memberNames: List<String>
): ExprAstNode<BibixAst.MemberAccess>(expr)

data class ThisRefNode(
  val expr: BibixAst.This
): ExprAstNode<BibixAst.This>(expr)

data class TupleNode(
  val expr: BibixAst.TupleExpr,
  val elems: List<ExprNodeId>
): ExprAstNode<BibixAst.TupleExpr>(expr)

data class NamedTupleNode(
  val expr: BibixAst.NamedTupleExpr,
  val elems: List<Pair<String, ExprNodeId>>
): ExprAstNode<BibixAst.NamedTupleExpr>(expr)

data class ActionLocalLetNode(val expr: BibixAst.NameRef): ExprAstNode<BibixAst.NameRef>(expr) {
  val name get() = expr.name
}
