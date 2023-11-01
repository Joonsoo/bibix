package com.giyeok.bibix.graph

import com.giyeok.bibix.ast.BibixAst

class BuildGraph(
  val packageName: String?,

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
  val varRedefs: Map<BibixName, Map<Int, VarCtx>>,

  val exprGraph: ExprGraph,
  val typeGraph: TypeGraph,
  val exprTypeEdges: Set<ExprTypeEdge>,
) {
  data class VarCtx(val parentCtxId: Int, val redefs: Map<BibixName, ExprNodeId>)
  companion object {
    fun fromScript(
      script: BibixAst.BuildScript,
      preloadedPluginNames: Set<String>,
      preludeNames: Set<String>
    ): BuildGraph = fromDefs(
      packageName = script.packageName?.tokens?.joinToString(".") { it.trim() },
      defs = script.defs,
      preloadedPluginNames = preloadedPluginNames,
      preludeNames = preludeNames,
      nativeAllowed = false
    )

    fun fromDefs(
      packageName: String?,
      defs: List<BibixAst.Def>,
      preloadedPluginNames: Set<String>,
      preludeNames: Set<String>,
      nativeAllowed: Boolean,
    ): BuildGraph {
      val builder = BuildGraphBuilder(packageName, preloadedPluginNames)
      val nameLookupTable = NameLookupTable.fromDefs(defs)
      val ctx = GraphBuildContext(
        NameLookupContext(
          table = nameLookupTable,
          preloadedPluginNames = preloadedPluginNames,
          preludeNames = preludeNames,
          currentScope = ScopedNameLookupTable(listOf(), nameLookupTable, null)
        ),
        mapOf(),
        thisRefAllowed = false,
        nativeAllowed = nativeAllowed,
      )
      builder.addDefs(defs, ctx, true)

      return builder.build()
    }
  }
}

data class BibixName(val tokens: List<String>) {
  constructor(name: String): this(name.split('.').map { it.trim() })

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
  val implMethodNameOpt: String?
) {
  val implMethodName: String = implMethodNameOpt ?: "build"
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
