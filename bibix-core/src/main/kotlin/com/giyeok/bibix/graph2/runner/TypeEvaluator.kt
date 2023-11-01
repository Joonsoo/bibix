package com.giyeok.bibix.graph2.runner

import com.giyeok.bibix.base.*
import com.giyeok.bibix.graph2.*
import com.google.common.collect.ImmutableBiMap

class TypeEvaluator(
  val projectId: Int,
  val packageName: String?,
  val typeGraph: TypeGraph,
) {
  fun evaluateType(typeNodeId: TypeNodeId): BuildTaskResult =
    when (val typeNode = typeGraph.nodes.getValue(typeNodeId)) {
      is BasicTypeNode -> BuildTaskResult.TypeResult(typeNode.bibixType)

      is ImportedType -> {
        BuildTaskResult.WithResult(Import(projectId, 0, typeNode.import)) { importResult ->
          check(importResult is BuildTaskResult.ImportResult)

          val dataClass = importResult.graph.dataClasses[typeNode.name]
          val superClass = importResult.graph.superClasses[typeNode.name]

          when {
            dataClass != null -> {
              check(superClass == null)
              checkNotNull(importResult.graph.packageName)
              BuildTaskResult.TypeResult(
                DataClassType(importResult.graph.packageName, typeNode.name.toString())
              )
            }

            superClass != null -> {
              checkNotNull(importResult.graph.packageName)
              BuildTaskResult.TypeResult(
                SuperClassType(importResult.graph.packageName, typeNode.name.toString())
              )
            }

            else -> throw IllegalStateException()
          }
        }
      }

      is ImportedTypeFromPreloaded -> TODO()
      is ImportedTypeFromPrelude -> TODO()

      is NamedTupleTypeNode -> TODO()

      is ListTypeNode -> {
        BuildTaskResult.WithResult(EvalType(projectId, typeNode.elemType)) { typeResult ->
          check(typeResult is BuildTaskResult.TypeResult)
          BuildTaskResult.TypeResult(ListType(typeResult.type))
        }
      }

      is SetTypeNode -> {
        BuildTaskResult.WithResult(EvalType(projectId, typeNode.elemType)) { typeResult ->
          check(typeResult is BuildTaskResult.TypeResult)
          BuildTaskResult.TypeResult(SetType(typeResult.type))
        }
      }

      is TupleTypeNode -> TODO()

      is LocalDataClassTypeRef ->
        BuildTaskResult.TypeResult(
          DataClassType(checkNotNull(packageName), typeNode.name.toString())
        )

      is LocalSuperClassTypeRef ->
        BuildTaskResult.TypeResult(
          SuperClassType(checkNotNull(packageName), typeNode.name.toString())
        )

      is LocalEnumTypeRef ->
        BuildTaskResult.TypeResult(EnumType(checkNotNull(packageName), typeNode.name.toString()))

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
