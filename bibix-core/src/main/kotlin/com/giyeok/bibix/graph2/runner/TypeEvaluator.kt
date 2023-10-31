package com.giyeok.bibix.graph2.runner

import com.giyeok.bibix.graph2.*

class TypeEvaluator(val typeGraph: TypeGraph) {
  fun evaluateType(typeNodeId: TypeNodeId): BuildTaskResult =
    when (val typeNode = typeGraph.nodes.getValue(typeNodeId)) {
      is BasicTypeNode -> BuildTaskResult.TypeResult(typeNode.bibixType)
      is ImportedType -> TODO()
      is ImportedTypeFromPreloaded -> TODO()
      is ImportedTypeFromPrelude -> TODO()
      is ListTypeNode -> TODO()
      is NamedTupleTypeNode -> TODO()
      is SetTypeNode -> TODO()
      is TupleTypeNode -> TODO()
      is LocalDataClassTypeRef -> TODO()
      is LocalEnumTypeRef -> TODO()
      is LocalSuperClassTypeRef -> TODO()
      is UnionTypeNode -> TODO()
    }
}
