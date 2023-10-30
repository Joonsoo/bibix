package com.giyeok.bibix.graph

import com.giyeok.bibix.ast.BibixAst
import com.giyeok.bibix.base.BibixType
import com.giyeok.bibix.graph.runner.ProjectInstanceId
import kotlin.reflect.KClass

// TasksGraph에서 TaskId는 스크립트 내에서 해당 task가 정의된 위치로 정의된다.
// TasksGraph는 스크립트 내의 def들의 관계를 나타낼 뿐이고
// 실제 해당 task가 어떻게 쓰이고 어떤 값으로 evaluate되는지는 다음 문제.
data class TaskId(
  val nodeId: Int,
  val nodeType: KClass<out TaskNode>,
  val additionalId: Any? = null
)

sealed class TaskNode {
  abstract val id: TaskId
}

data class BuildRuleNode(
  val def: BibixAst.BuildRuleDef,
  val paramTypes: Map<String, TaskId>,
  val paramDefaultValues: Map<String, TaskId>,
  val returnType: TaskId,
  val implTarget: TaskId,
  val implClassName: String,
  val implMethodName: String?,
): TaskNode() {
  override val id: TaskId = TaskId(def.nodeId, this::class)
}

data class NativeImplNode(
  val implTargetName: BibixAst.Name
): TaskNode() {
  override val id: TaskId = TaskId(implTargetName.nodeId, this::class)
}

data class ActionDefNode(
  val def: BibixAst.ActionDef
): TaskNode() {
  override val id: TaskId = TaskId(def.nodeId, this::class)
}

data class ActionRuleDefNode(
  val def: BibixAst.ActionRuleDef
): TaskNode() {
  override val id: TaskId = TaskId(def.nodeId, this::class)
}

data class ImportInstanceNode(
  val importNode: TaskId,
  val withDef: BibixAst.DefsWithVarRedefs?,
  val varRedefs: Map<String, TaskId>
): TaskNode() {
  override val id: TaskId =
    TaskId(importNode.nodeId, this::class, Pair(importNode.additionalId, withDef?.nodeId ?: 0))
}

data class ImportNode(
  val import: BibixAst.ImportDef,
  val importSource: TaskId,
  val importName: List<String>?
): TaskNode() {
  override val id: TaskId = TaskId(import.nodeId, this::class)
}

data class PreloadedPluginNode(val name: String): TaskNode() {
  override val id: TaskId = TaskId(0, this::class, this.name)
}

data class MemberAccessNode(val target: TaskId, val remainingNames: List<String>):
  TaskNode() {
  override val id: TaskId =
    TaskId(target.nodeId, this::class, Pair(target.additionalId, remainingNames))
}

data class PreludeTaskNode(val name: String): TaskNode() {
  override val id: TaskId = TaskId(0, this::class, this.name)
}

data class PreludeMemberNode(val preludeName: String, val remainingNames: List<String>):
  TaskNode() {
  override val id: TaskId = TaskId(0, this::class, this)
}

data class TargetNode(val def: BibixAst.TargetDef, val valueNode: TaskId): TaskNode() {
  override val id: TaskId = TaskId(def.nodeId, this::class)
}

data class VarNode(
  val def: BibixAst.VarDef,
  val typeNode: TaskId,
  val defaultValueNode: TaskId?
): TaskNode() {
  override val id: TaskId = TaskId(def.nodeId, this::class)

  val name get() = def.name
}

sealed class ExprNode<T: BibixAst.Expr>(val expr: T): TaskNode() {
  override val id: TaskId = TaskId(expr.nodeId, this::class)
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

// callExpr에 대한 call + 결과를 최종 타입으로 coercion
// 어떤 타입으로 coercion 하는지는 현재는 알 수 없음
data class CallExprNode(
  val callExpr: BibixAst.CallExpr,
  val callNode: TaskId,
  // callee는 callNode(CallExprCallNode)의 callee와 같아야 함
  val callee: TaskId,
): ExprNode<BibixAst.CallExpr>(callExpr) {
  override val id: TaskId = TaskId(callExpr.nodeId, this::class)
}

data class CallExprCallNode(
  val callExpr: BibixAst.CallExpr,
  val callee: TaskId,
  val posParams: List<TaskId>,
  val namedParams: Map<String, TaskId>,
): ExprNode<BibixAst.CallExpr>(callExpr)

data class CallExprParamCoercionNode(
  val expr: BibixAst.AstNode,
  val value: TaskId,
  val callee: TaskId,
  val paramPos: Int?,
  val paramName: String?,
): TaskNode() {
  override val id = TaskId(expr.nodeId, this::class, Pair(paramPos, paramName))

  init {
    check((paramPos == null && paramName != null) || (paramPos != null && paramName == null))
  }
}

data class ListExprNode(
  val listExpr: BibixAst.ListExpr,
  val elems: List<ListElem>,
): ExprNode<BibixAst.ListExpr>(listExpr) {
  data class ListElem(val valueNode: TaskId, val isEllipsis: Boolean)
}

data class BooleanLiteralNode(
  val literal: BibixAst.BooleanLiteral
): ExprNode<BibixAst.BooleanLiteral>(literal)

data class NoneLiteralNode(
  val literal: BibixAst.NoneLiteral
): ExprNode<BibixAst.NoneLiteral>(literal)

data class StringNode(
  val stringExpr: BibixAst.StringLiteral,
  val exprElems: List<TaskId>
): ExprNode<BibixAst.StringLiteral>(stringExpr)

data class MemberAccessExprNode(
  val memberAccessExpr: BibixAst.MemberAccess,
  val target: TaskId,
  val memberNames: List<String>,
): ExprNode<BibixAst.MemberAccess>(memberAccessExpr)

data class NameRefNode(
  val nameRefExpr: BibixAst.NameRef,
  val valueNode: TaskId
): ExprNode<BibixAst.NameRef>(nameRefExpr)

data class ParenExprNode(
  val parenExpr: BibixAst.Paren,
  val body: TaskId
): ExprNode<BibixAst.Paren>(parenExpr)

data class ThisRefNode(val thisExpr: BibixAst.This): ExprNode<BibixAst.This>(thisExpr)

data class TupleNode(
  val tupleExpr: BibixAst.TupleExpr,
  val elemNodes: List<TaskId>,
): ExprNode<BibixAst.TupleExpr>(tupleExpr)

data class NamedTupleNode(
  val namedTupleExpr: BibixAst.NamedTupleExpr,
  val elemNodes: List<Pair<String, TaskId>>,
): ExprNode<BibixAst.NamedTupleExpr>(namedTupleExpr)


sealed class TypeNode<T: BibixAst.TypeExpr>(typeExpr: T): TaskNode() {
  override val id: TaskId = TaskId(typeExpr.nodeId, this::class)
}

data class DataClassTypeNode(
  val defNode: BibixAst.DataClassDef,
  val fieldTypes: List<Pair<String, TaskId>>,
  val defaultValues: Map<String, TaskId>,
  val elems: List<TaskId>,
): TaskNode() {
  override val id: TaskId = TaskId(defNode.nodeId, this::class)
}

data class ValueCoercionNode(
  val ast: BibixAst.AstNode,
  val value: TaskId,
  val type: TaskId,
): TaskNode() {
  override val id: TaskId = TaskId(ast.nodeId, this::class, Pair(value, type))
}

data class ClassElemCastNode(
  val castDef: BibixAst.ClassCastDef,
  val castType: TaskId,
  val castExpr: TaskId
): TaskNode() {
  override val id: TaskId = TaskId(castDef.nodeId, this::class)
}

data class SuperClassTypeNode(
  val defNode: BibixAst.SuperClassDef,
  val subClasses: Map<String, TaskId>
): TaskNode() {
  override val id: TaskId = TaskId(defNode.nodeId, this::class)
}

data class EnumTypeNode(val defNode: BibixAst.EnumDef): TaskNode() {
  override val id: TaskId = TaskId(defNode.nodeId, this::class)
}

data class EnumValueNode(
  val enumDef: BibixAst.EnumDef,
  val memberName: String,
  val enumTypeNode: TaskId
): TaskNode() {
  override val id: TaskId = TaskId(enumDef.nodeId, this::class, memberName)
}

data class CollectionTypeNode(
  val collectionType: BibixAst.CollectionType,
  val typeParams: List<TaskId>
): TypeNode<BibixAst.CollectionType>(collectionType)

data class TypeNameNode(val name: BibixAst.Name, val typeNode: TaskId):
  TypeNode<BibixAst.Name>(name)

data class BibixTypeNode(val bibixType: BibixType): TaskNode() {
  override val id: TaskId = TaskId(0, this::class, bibixType)
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

enum class TaskEdgeType(val isRequired: Boolean) {
  Definition(true),
  ValueDependency(true),
  CalleeDependency(true),
  Reference(true),
  ImportDependency(true),
  TypeDependency(true),
  ImportInstance(true),
  ClassInherit(true),

  // default value는 evaluation할 때 빠질 수도 있다는 의미
  DefaultValueDependency(false),
  ClassMember(false),

  // OverridingValueDependency는 global edge에만 올 수 있음
  OverridingValueDependency(true),
}
