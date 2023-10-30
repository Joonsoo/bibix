package com.giyeok.bibix.graph2

import com.giyeok.bibix.ast.BibixAst
import com.giyeok.bibix.graph.nodeIdsMap

class BuildGraph(
  val packageName: String?,
  val astNodes: Map<Int, BibixAst.AstNode>,

  val targets: Map<BibixName, ExprNodeId>,
  val buildRules: Map<BibixName, BuildRuleDef>,
  // TODO action/action rules
  val vars: Map<BibixName, VarDef>,
  val dataClasses: Map<BibixName, DataClassDef>,
  val superClasses: Map<BibixName, SuperClassDef>,
  val enums: Map<BibixName, EnumDef>,

  val importAlls: Map<BibixName, ImportAllDef>,
  val importFroms: Map<BibixName, ImportFromDef>,
  // import name -> import instace id (0 혹은 DefsWithVarRedefs의 nodeId) -> var name -> redef expr node id
  val importInstances: Map<BibixName, Map<Int, Map<BibixName, ExprNodeId>>>,

  val exprGraph: ExprGraph,
  val typeGraph: TypeGraph,
  val exprTypeEdges: Set<ExprTypeEdge>,
) {
  companion object {
    fun fromScript(
      script: BibixAst.BuildScript,
      preloadedPluginNames: Set<String>,
      preludeNames: Set<String>
    ): BuildGraph {
      val builder = BuildGraphBuilder(
        script.packageName?.tokens?.joinToString(".") { it.trim() },
        nodeIdsMap(script),
      )
      val nameLookupTable = NameLookupTable.fromScript(script)
      val ctx = GraphBuildContext(
        NameLookupContext(
          table = nameLookupTable,
          preloadedPluginNames = preloadedPluginNames,
          preludeNames = preludeNames,
          currentScope = ScopedNameLookupTable(listOf(), nameLookupTable, null)
        ),
        mapOf(),
        thisRefAllowed = false,
        nativeAllowed = false,
      )
      builder.addDefs(script.defs, ctx, true)

      return builder.build()
    }
  }
}

data class BibixName(val tokens: List<String>) {
  override fun toString(): String = tokens.joinToString(".")
}

data class BuildRuleDef(
  val def: BibixAst.BuildRuleDef,
  val params: Map<String, TypeNodeId>,
  val paramDefaultValues: Map<String, ExprNodeId>,
  val returnType: TypeNodeId,
  // implTarget이 null이면 native
  val implTarget: ExprNodeId?,
  val implClassName: String,
  val implMethodName: String?
) {
  val paramsMap = params.toMap()
}

data class VarDef(
  val def: BibixAst.VarDef,
  val type: TypeNodeId,
  val defaultValue: ExprNodeId?
)

data class DataClassDef(
  val def: BibixAst.DataClassDef,
  val fields: Map<String, TypeNodeId>,
  val fieldDefaultValues: Map<String, ExprNodeId>,
  // TODO elements
)

data class SuperClassDef(
  val def: BibixAst.SuperClassDef,
  val subTypes: Set<String>,
)

data class EnumDef(
  val def: BibixAst.EnumDef,
)

data class ImportAllDef(
  val source: ExprNodeId,
)

data class ImportFromDef(
  val source: ExprNodeId,
  val importing: List<String>,
)
