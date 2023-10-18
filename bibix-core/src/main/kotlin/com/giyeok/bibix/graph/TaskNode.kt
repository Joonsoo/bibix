package com.giyeok.bibix.graph

import com.giyeok.bibix.ast.BibixAst
import com.giyeok.bibix.base.BibixType

// TasksGraph에서 TaskId는 스크립트 내에서 해당 task가 정의된 위치로 정의된다.
// TasksGraph는 스크립트 내의 def들의 관계를 나타낼 뿐이고
// 실제 해당 task가 어떻게 쓰이고 어떤 값으로 evaluate되는지는 다음 문제.
data class TaskId(val nodeId: Int, val additionalId: Any? = null)

sealed class TaskNode {
  abstract val id: TaskId
}

data class BuildRuleNode(
  val def: BibixAst.BuildRuleDef,
  val paramTypes: Map<String, TaskId>,
  val paramDefaultValues: Map<String, TaskId>,
  val returnType: TaskId,
  val implTarget: TaskId
): TaskNode() {
  override val id: TaskId = TaskId(def.nodeId)
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

data class PreloadedPluginMemberNode(val pluginName: String, val remainingNames: List<String>):
  TaskNode() {
  override val id: TaskId = TaskId(0, this)
}

data class PreludeTaskNode(val name: String): TaskNode() {
  override val id: TaskId = TaskId(0, this)
}

data class PreludeMemberNode(val preludeName: String, val remainingNames: List<String>):
  TaskNode() {
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
  val memberNames: List<String>,
): ExprNode<BibixAst.MemberAccess>(memberAccessExpr)

data class NameRefNode(
  val nameRefExpr: BibixAst.NameRef,
  val valueNode: TaskId
): ExprNode<BibixAst.NameRef>(nameRefExpr)

data class ThisRefNode(val thisExpr: BibixAst.This): ExprNode<BibixAst.This>(thisExpr)

data class TupleNode(
  val tupleExpr: BibixAst.TupleExpr,
  val elemNodes: List<TaskId>,
): ExprNode<BibixAst.TupleExpr>(tupleExpr)

data class NamedTupleNode(
  val namedTupleExpr: BibixAst.NamedTupleExpr,
  val elemNodes: List<Pair<String, TaskId>>,
): ExprNode<BibixAst.NamedTupleExpr>(namedTupleExpr)


sealed class TypeNode<T: BibixAst.TypeExpr>(val typeExpr: T): TaskNode() {
  override val id: TaskId = TaskId(typeExpr.nodeId)
}

data class DataClassTypeNode(
  val defNode: BibixAst.DataClassDef,
  val fieldTypes: Map<String, TaskId>,
  val defaultValues: Map<String, TaskId>,
  val elems: List<TaskId>
): TaskNode() {
  override val id: TaskId = TaskId(defNode.nodeId)
}

data class ClassCastNode(
  val castDef: BibixAst.ClassCastDef,
  val castType: TaskId,
  val castExpr: TaskId
): TaskNode() {
  override val id: TaskId = TaskId(castDef.nodeId)
}

data class SuperClassTypeNode(
  val defNode: BibixAst.SuperClassDef,
  val subClasses: Map<String, TaskId>
): TaskNode() {
  override val id: TaskId = TaskId(defNode.nodeId)
}

data class EnumTypeNode(val defNode: BibixAst.EnumDef): TaskNode() {
  override val id: TaskId = TaskId(defNode.nodeId)
}

data class CollectionTypeNode(
  val collectionType: BibixAst.CollectionType,
  val typeParams: List<TaskId>
): TypeNode<BibixAst.CollectionType>(collectionType)

data class TypeNameNode(val name: BibixAst.Name): TypeNode<BibixAst.Name>(name)

data class BibixTypeNode(val bibixType: BibixType): TaskNode() {
  override val id: TaskId = TaskId(0, bibixType)
}

data class TupleTypeNode(
  val tupleType: BibixAst.TupleType,
  val elems: List<TaskId>
): TypeNode<BibixAst.TupleType>(tupleType)

data class NamedTupleTypeNode(
  val namedTupleType: BibixAst.NamedTupleType,
  val elems: Map<String, TaskId>
): TypeNode<BibixAst.NamedTupleType>(namedTupleType)

data class UnionTypeNode(
  val unionType: BibixAst.UnionType,
  val elemTypes: List<TaskId>
): TypeNode<BibixAst.UnionType>(unionType)

data class TaskEdge(val start: TaskId, val end: TaskId, val edgeType: TaskEdgeType)

enum class TaskEdgeType {
  Definition, ValueDependency, CalleeDependency, Reference, ImportDependency, TypeDependency, ImportInstance,
  ClassInherit,
  ClassMember,

  // default value는 evaluation할 때 빠질 수도 있다는 의미
  DefaultValueDependency,

  // OverridingValueDependency는 global edge에만 올 수 있음
  OverridingValueDependency
}
