package com.giyeok.bibix.graph.runner

import com.giyeok.bibix.base.*
import com.giyeok.bibix.graph.*

class TypeEvaluator(
  val multiGraph: MultiBuildGraph,
  val projectId: Int,
  val importInstanceId: Int,
  val packageName: String?,
  val typeGraph: TypeGraph,
) {
  fun evaluateType(typeNodeId: TypeNodeId): BuildTaskResult =
    when (val typeNode = typeGraph.nodes.getValue(typeNodeId)) {
      is BasicTypeNode -> BuildTaskResult.TypeResult(typeNode.bibixType)

      is ImportedType -> {
        BuildTaskResult.WithResult(
          Import(projectId, importInstanceId, typeNode.import)
        ) { importResult ->
          check(importResult is BuildTaskResult.ImportInstanceResult)

          val importedGraph = multiGraph.getProjectGraph(importResult.projectId)
          val dataClass = importedGraph.dataClasses[typeNode.name]
          val superClass = importedGraph.superClasses[typeNode.name]

          when {
            dataClass != null -> {
              check(superClass == null)
              checkNotNull(importedGraph.packageName)
              BuildTaskResult.TypeResult(
                DataClassType(importedGraph.packageName, typeNode.name.toString())
              )
            }

            superClass != null -> {
              checkNotNull(importedGraph.packageName)
              BuildTaskResult.TypeResult(
                SuperClassType(importedGraph.packageName, typeNode.name.toString())
              )
            }

            else -> throw IllegalStateException()
          }
        }
      }

      is ImportedTypeFromPreloaded -> {
        TODO()
      }

      is ImportedTypeFromPrelude -> {
        val preludeGraph = multiGraph.getProjectGraph(2)
        val packageName = checkNotNull(preludeGraph.packageName)
        when (preludeGraph.findName(typeNode.name)) {
          is BuildGraphEntity.DataClass ->
            BuildTaskResult.TypeResult(DataClassType(packageName, typeNode.name.toString()))

          is BuildGraphEntity.SuperClass ->
            BuildTaskResult.TypeResult(SuperClassType(packageName, typeNode.name.toString()))

          is BuildGraphEntity.Enum ->
            BuildTaskResult.TypeResult(EnumType(packageName, typeNode.name.toString()))

          else -> throw IllegalStateException()
        }
      }

      is NamedTupleTypeNode -> {
        TODO()
      }

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
