package com.giyeok.bibix.graph

import com.giyeok.bibix.ast.BibixAst
import com.giyeok.bibix.base.BibixType

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

      builder.checkNoDuplicateNames()

      return builder.build()
    }
  }

  fun findName(name: BibixName): BuildGraphEntity? {
    val target = targets[name]
    if (target != null) {
      return BuildGraphEntity.Target(target)
    }

    val buildRule = buildRules[name]
    if (buildRule != null) {
      return BuildGraphEntity.BuildRule(buildRule)
    }

    val variable = vars[name]
    if (variable != null) {
      return BuildGraphEntity.Variable(variable)
    }

    val dataClass = dataClasses[name]
    if (dataClass != null) {
      return BuildGraphEntity.DataClass(dataClass)
    }

    val superClass = superClasses[name]
    if (superClass != null) {
      return BuildGraphEntity.SuperClass(superClass)
    }

    val enum = enums[name]
    if (enum != null) {
      return BuildGraphEntity.Enum(enum)
    }

    val action = actions[name]
    if (action != null) {
      return BuildGraphEntity.Action(action)
    }

    val actionRule = actionRules[name]
    if (actionRule != null) {
      return BuildGraphEntity.ActionRule(actionRule)
    }


    val importAll = importAlls[name]
    if (importAll != null) {
      return BuildGraphEntity.ImportAll(importAll)
    }

    val importFrom = importFroms[name]
    if (importFrom != null) {
      return BuildGraphEntity.ImportFrom(importFrom)
    }

    return null
  }
}

sealed class BuildGraphEntity {
  data class Target(val exprNodeId: ExprNodeId): BuildGraphEntity()
  data class BuildRule(val def: BuildRuleDef): BuildGraphEntity()
  data class Variable(val def: VarDef): BuildGraphEntity()
  data class DataClass(val def: DataClassDef): BuildGraphEntity()
  data class SuperClass(val def: SuperClassDef): BuildGraphEntity()
  data class Enum(val def: EnumDef): BuildGraphEntity()
  data class Action(val def: ActionDef): BuildGraphEntity()
  data class ActionRule(val def: ActionRuleDef): BuildGraphEntity()
  data class ImportAll(val def: ImportAllDef): BuildGraphEntity()
  data class ImportFrom(val def: ImportFromDef): BuildGraphEntity()
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
  // TODO 지금은 var의 type을 쓰는 데가 없는데.. 실은 var에 지정된 값이나 redef되는 값 coercion 해줘야 할듯
  val type: VarType,
  val defaultValue: ExprNodeId?
) {
  sealed class VarType {
    data class TypeNode(val typeNode: TypeNodeId): VarType()
    data class FixedType(val bibixType: BibixType): VarType()
  }
}

data class DataClassDef(
  val def: BibixAst.DataClassDef,
  val fields: Map<String, TypeNodeId>,
  val fieldDefaultValues: Map<String, ExprNodeId>,
  val customAutoCasts: List<Pair<TypeNodeId, ExprNodeId>>,
  // TODO actions
)

data class SuperClassDef(
  val def: BibixAst.SuperClassDef,
  val subTypes: Set<String>,
)

data class EnumDef(
  val def: BibixAst.EnumDef,
)

data class ImportAllDef(
  val source: ImportSource,
)

data class ImportFromDef(
  val source: ImportSource,
  val importing: List<String>,
)

sealed class ImportSource {
  data class Expr(val exprNodeId: ExprNodeId): ImportSource()
  data class PreloadedPlugin(val pluginName: String): ImportSource()
  data class AnotherImport(val importName: BibixName): ImportSource()
}

data class ActionDef(
  val def: BibixAst.ActionDef,
  val stmts: List<ActionStmt>,
) {
  val argsName: String? get() = def.argsName

  sealed class ActionStmt
  data class LetStmt(val name: String, val exprNodeId: ExprNodeId): ActionStmt()
  data class CallStmt(
    val callee: Callee,
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
