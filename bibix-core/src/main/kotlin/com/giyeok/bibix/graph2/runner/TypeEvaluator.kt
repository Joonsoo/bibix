package com.giyeok.bibix.graph2.runner

import com.giyeok.bibix.base.DataClassType
import com.giyeok.bibix.base.SetType
import com.giyeok.bibix.base.UnionType
import com.giyeok.bibix.graph2.*

class TypeEvaluator(val projectId: Int, val packageName: String?, val typeGraph: TypeGraph) {
  fun evaluateType(typeNodeId: TypeNodeId): BuildTaskResult =
    when (val typeNode = typeGraph.nodes.getValue(typeNodeId)) {
      is BasicTypeNode -> BuildTaskResult.TypeResult(typeNode.bibixType)

      is ImportedType -> {
        BuildTaskResult.WithResult(Import(projectId, 0, typeNode.name)) { importResult ->
          check(importResult is BuildTaskResult.ImportResult)

          TODO()
        }
      }

      is ImportedTypeFromPreloaded -> TODO()
      is ImportedTypeFromPrelude -> TODO()

      is NamedTupleTypeNode -> TODO()

      is ListTypeNode -> {
        BuildTaskResult.WithResult(EvalType(projectId, typeNode.elemType)) { typeResult ->
          check(typeResult is BuildTaskResult.TypeResult)
          BuildTaskResult.TypeResult(SetType(typeResult.type))
        }
      }

      is SetTypeNode -> {
        BuildTaskResult.WithResult(EvalType(projectId, typeNode.elemType)) { typeResult ->
          check(typeResult is BuildTaskResult.TypeResult)
          BuildTaskResult.TypeResult(SetType(typeResult.type))
        }
      }

      is TupleTypeNode -> TODO()

      is LocalDataClassTypeRef -> {
        BuildTaskResult.TypeResult(
          DataClassType(checkNotNull(packageName), typeNode.name.toString())
        )
      }

      is LocalSuperClassTypeRef -> TODO()
      is LocalEnumTypeRef -> TODO()

      is UnionTypeNode -> {
        BuildTaskResult.WithResultList(typeNode.elemTypes.map {
          EvalType(projectId, it)
        }) { results ->
          check(results.all { it is BuildTaskResult.TypeResult })
          val types = results.map { (it as BuildTaskResult.TypeResult).type }
          BuildTaskResult.TypeResult(UnionType(types))
        }
      }
    }
}
