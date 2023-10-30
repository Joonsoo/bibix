package com.giyeok.bibix.graph2

import com.giyeok.bibix.ast.BibixAst
import com.giyeok.bibix.base.BibixType

class TypeGraph(val nodes: Map<TypeNodeId, TypeGraphNode>, val edges: Set<TypeGraphEdge>)

data class TypeGraphEdge(val start: TypeNodeId, val end: TypeNodeId)

sealed class TypeNodeId {
  data class TypeAstNodeId(val nodeId: Int): TypeNodeId()

  // 개발용 - TODO 삭제
  data class AnyNodeId(val any: Any): TypeNodeId()
}

sealed class TypeGraphNode {
  abstract val id: TypeNodeId
}

sealed class TypeLocalRefNode(val ast: BibixAst.Def): TypeGraphNode() {
  override val id: TypeNodeId = TypeNodeId.TypeAstNodeId(ast.nodeId)
}

data class LocalDataClassTypeRef(val name: BibixName, val def: BibixAst.DataClassDef):
  TypeLocalRefNode(def)

data class LocalSuperClassTypeRef(val name: BibixName, val def: BibixAst.SuperClassDef):
  TypeLocalRefNode(def)

data class LocalEnumTypeRef(val name: BibixName, val def: BibixAst.EnumDef): TypeLocalRefNode(def)


data class ImportedTypeFromPreloaded(val pluginName: String, val name: BibixName): TypeGraphNode() {
  override val id: TypeNodeId get() = TypeNodeId.AnyNodeId(this)
}

data class ImportedTypeFromPrelude(val name: String, val remaining: List<String>): TypeGraphNode() {
  override val id: TypeNodeId get() = TypeNodeId.AnyNodeId(this)
}

data class ImportedType(
  val import: BibixName,
  val importInstanceId: Int,
  val name: BibixName
): TypeGraphNode() {
  override val id: TypeNodeId get() = TypeNodeId.AnyNodeId(this)
}

data class SetTypeNode(val ast: BibixAst.CollectionType, val elemType: TypeNodeId):
  TypeGraphNode() {
  override val id: TypeNodeId get() = TypeNodeId.AnyNodeId(this)
}

data class ListTypeNode(val ast: BibixAst.CollectionType, val elemType: TypeNodeId):
  TypeGraphNode() {
  override val id: TypeNodeId get() = TypeNodeId.AnyNodeId(this)
}

data class BasicTypeNode(val bibixType: BibixType): TypeGraphNode() {
  override val id: TypeNodeId get() = TypeNodeId.AnyNodeId(this)
}

data class TupleTypeNode(val ast: BibixAst.TupleType, val elemTypes: List<TypeNodeId>):
  TypeGraphNode() {
  override val id: TypeNodeId get() = TypeNodeId.AnyNodeId(this)
}

data class NamedTupleTypeNode(
  val namedTupleType: BibixAst.NamedTupleType,
  val elems: Map<String, TypeNodeId>
): TypeGraphNode() {
  override val id: TypeNodeId get() = TypeNodeId.AnyNodeId(this)
}

data class UnionTypeNode(
  val unionType: BibixAst.UnionType,
  val elemTypes: List<TypeNodeId>
): TypeGraphNode() {
  override val id: TypeNodeId get() = TypeNodeId.AnyNodeId(this)
}
