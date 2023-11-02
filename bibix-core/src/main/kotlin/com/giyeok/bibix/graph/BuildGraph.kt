package com.giyeok.bibix.graph

import com.giyeok.bibix.ast.BibixAst

class BuildGraph(
  val packageName: String?,

  val targets: Map<BibixName, ExprNodeId>,
  val buildRules: Map<BibixName, BuildRuleDef>,
  val vars: Map<BibixName, VarDef>,
  val dataClasses: Map<BibixName, DataClassDef>,
  val superClasses: Map<BibixName, SuperClassDef>,
  val enums: Map<BibixName, EnumDef>,
  val actions: Map<BibixName, ActionDef>,
  val actionRules: Map<BibixName, ActionRuleDef>,

  val importAlls: Map<BibixName, ImportAllDef>,
  val importFroms: Map<BibixName, ImportFromDef>,
  // import name -> (var name -> redef expr node id)
  val varRedefs: Map<BibixName, Map<BibixName, ExprNodeId>>,

  val exprGraph: ExprGraph,
  val typeGraph: TypeGraph,
  val exprTypeEdges: Set<ExprTypeEdge>,
) {
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
        thisRefAllowed = false,
        nativeAllowed = nativeAllowed,
        localLetNames = setOf(),
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

data class ActionDef(
  val def: BibixAst.ActionDef,
  val stmts: List<ActionStmt>,
) {
  val argsName: String? get() = def.argsName

  sealed class ActionStmt
  data class LetStmt(val name: String, val exprNodeId: ExprNodeId): ActionStmt()
  data class CallStmt(
    val calleeNodeId: ExprNodeId,
    val posArgs: List<ExprNodeId>,
    val namedArgs: Map<String, ExprNodeId>
  ): ActionStmt()
}

data class ActionRuleDef(
  val def: BibixAst.ActionRuleDef,
  val params: Map<String, TypeNodeId>,
  val paramDefaultValues: Map<String, ExprNodeId>,
  // implTarget이 null이면 native
  val implTarget: ExprNodeId?,
  val implClassName: String,
  val implMethodNameOpt: String?
) {
  val implMethodName: String = implMethodNameOpt ?: "run"
}
