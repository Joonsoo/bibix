package com.giyeok.bibix.graph

import com.giyeok.bibix.ast.BibixAst
import com.giyeok.bibix.base.*


// TaskGraph는 프로젝트 하나(즉 스크립트 하나)의 내용만 포함한다.
// import해서 사용하는 다른 프로젝트의 def들은 별도의 TaskGraph로 관리한다.
class TaskGraph(
  val astNodes: Map<Int, BibixAst.AstNode>,
  val nameLookup: NameLookupTable,
  val nodes: Map<TaskId, TaskNode>,
  val edges: List<TaskEdge>,
  val scriptVars: Map<String, TaskId>,
  val varRedefs: Map<TaskId, Map<String, TaskId>>,
) {
  val edgesByStart = edges.groupBy { it.start }
  val edgesByEnd = edges.groupBy { it.end }

  companion object {
    fun fromScript(
      script: BibixAst.BuildScript,
      preloadedPluginNames: Set<String>,
      preludeNames: Set<String>
    ): TaskGraph {
      val nodeIdsMap = mutableMapOf<Int, BibixAst.AstNode>()
      traverseAst(script) { nodeIdsMap[it.nodeId] = it }
      val nameLookup = NameLookupTable.fromScript(script)
      val builder = Builder(nodeIdsMap, nameLookup)
      val rootNameScope = ScopedNameLookupTable(listOf(), nameLookup, null)
      val nameLookupCtx =
        NameLookupContext(nameLookup, preloadedPluginNames, preludeNames, rootNameScope)
      val ctx = GraphBuildContext(nameLookupCtx, mapOf())
      builder.addDefs(script.defs, ctx, true)
      return builder.build()
    }
  }

  data class GraphBuildContext(
    val nameLookupCtx: NameLookupContext,
    val importInstances: Map<TaskId, TaskId>,
  ) {
    fun withVarRedefsCtx(redefsCtx: Map<TaskId, TaskId>) =
      copy(importInstances = importInstances + redefsCtx)

    fun innerNamespace(namespaceName: String) =
      copy(nameLookupCtx = nameLookupCtx.innerNamespace(namespaceName))
  }

  class Builder(
    val astNodes: Map<Int, BibixAst.AstNode>,
    val nameLookup: NameLookupTable,
    val nodes: MutableMap<TaskId, TaskNode> = mutableMapOf(),
    val edges: MutableList<TaskEdge> = mutableListOf(),
    val scriptVars: MutableMap<String, TaskId> = mutableMapOf(),
    val varRedefs: MutableMap<TaskId, Map<String, TaskId>> = mutableMapOf(),
  ) {
    private val edgePairs: MutableMap<Pair<TaskId, TaskId>, TaskEdge> =
      edges.associateBy { Pair(it.start, it.end) }.toMutableMap()

    fun build() = TaskGraph(astNodes, nameLookup, nodes, edges, scriptVars, varRedefs)

    fun addImportSource(source: BibixAst.Expr, ctx: GraphBuildContext): TaskId {
      if (source is BibixAst.NameRef) {
        if (source.name in ctx.nameLookupCtx.preloadedPluginNames) {
          return addNode(PreloadedPluginNode(source.name))
        }
      }
      return addExpr(source, ctx)
    }

    fun addDefs(defs: List<BibixAst.Def>, ctx: GraphBuildContext, isRoot: Boolean) {
      defs.forEach { def ->
        when (def) {
          is BibixAst.ImportDef -> {
            check(isRoot) { "import must be in the root" }
            val importNode = addNode(ImportNode(def))
            when (def) {
              is BibixAst.ImportAll -> {
                val source = addImportSource(def.source, ctx)
                addEdge(importNode, source, TaskEdgeType.ValueDependency)
              }

              is BibixAst.ImportFrom -> {
                val source = addImportSource(def.source, ctx)
                addEdge(importNode, source, TaskEdgeType.ValueDependency)
              }
            }
            val defaultImportInstanceNode = addNode(ImportInstanceNode(importNode, mapOf()))
            addEdge(defaultImportInstanceNode, importNode, TaskEdgeType.ImportInstance)
          }

          is BibixAst.NamespaceDef -> {
            addDefs(def.body, ctx.innerNamespace(def.name), false)
          }

          is BibixAst.TargetDef -> {
            val targetNode = addNode(TargetNode(def))
            val valueNode = addExpr(def.value, ctx)
            addEdge(targetNode, valueNode, TaskEdgeType.Definition)
          }

          is BibixAst.ActionDef -> {
            // TODO
          }

          is BibixAst.DataClassDef -> {
            val fieldTypes = def.fields.mapNotNull { field ->
              field.typ?.let { field.name to addType(it, ctx) }
            }.toMap()
            val defaultValues = def.fields.mapNotNull { field ->
              field.defaultValue?.let { field.name to addExpr(it, ctx) }
            }.toMap()
            val bodyElems = def.body.map { classElem ->
              when (classElem) {
                is BibixAst.ActionRuleDef -> TODO()
                is BibixAst.ClassCastDef -> {
                  val castTo = addType(classElem.castTo, ctx)
                  val castExpr = addExpr(classElem.expr, ctx)
                  addNode(ClassCastNode(classElem, castTo, castExpr))
                }
              }
            }
            val classNode = addNode(DataClassTypeNode(def, fieldTypes, defaultValues, bodyElems))
            fieldTypes.forEach { addEdge(classNode, it.value, TaskEdgeType.TypeDependency) }
            defaultValues.forEach { addEdge(classNode, it.value, TaskEdgeType.TypeDependency) }
            bodyElems.forEach { addEdge(classNode, it, TaskEdgeType.ClassMember) }
          }

          is BibixAst.SuperClassDef -> {
            val subClasses = def.subs.associate { subClassName ->
              val entry = nameLookup.names[subClassName]
              check(entry is ClassNameEntry) { "Invalid subclass name" }
              subClassName to TaskId(entry.def.nodeId)
            }
            val classNode = addNode(SuperClassTypeNode(def, subClasses))
            subClasses.forEach { addEdge(classNode, it.value, TaskEdgeType.ClassInherit) }
          }

          is BibixAst.EnumDef -> {
            addNode(EnumTypeNode(def))
          }

          is BibixAst.VarDef -> {
            check(isRoot) { "variable only can be defined in the root" }
            val varNode = addNode(VarNode(def))
            def.typ?.let { typ ->
              val typeNode = addType(typ, ctx)
              addEdge(varNode, typeNode, TaskEdgeType.TypeDependency)
            }
            def.defaultValue?.let { defaultValue ->
              val valueNode = addExpr(defaultValue, ctx)
              addEdge(varNode, valueNode, TaskEdgeType.DefaultValueDependency)
            }
            check(def.name !in scriptVars)
            scriptVars[def.name] = varNode
          }

          is BibixAst.VarRedefs -> {
            check(isRoot) { "variable redefs must be in the root. Consider using with instead" }
            compileVarRedefs(def.redefs, ctx).forEach { (import, redefs) ->
              val existingRedefs = this.varRedefs[import]
              if (existingRedefs == null) {
                this.varRedefs[import] = redefs
              } else {
                val duplicateRedefs = existingRedefs.keys.intersect(redefs.keys)
                check(duplicateRedefs.isEmpty()) {
                  // TODO improve error message
                  val importNode = astNodes.getValue(import.nodeId) as BibixAst.ImportDef
                  val importName = when (importNode) {
                    is BibixAst.ImportAll -> importNode.rename ?: importNode.source.toString()
                    is BibixAst.ImportFrom -> importNode.rename
                      ?: importNode.importing.tokens.joinToString(".")
                  }
                  "Duplicate var redef for $importName $duplicateRedefs"
                }
                this.varRedefs[import] = existingRedefs + redefs
              }
            }
          }

          is BibixAst.BuildRuleDef -> {
            val paramTypes = def.params.associate { it.name to addType(checkNotNull(it.typ), ctx) }
            val paramDefaultValues = def.params.mapNotNull { param ->
              param.defaultValue?.let { param.name to addExpr(it, ctx) }
            }.toMap()
            val returnType = addType(def.returnType, ctx)
            val implTarget = lookupResultToId(
              nameLookup.lookupName(
                def.impl.targetName.tokens,
                def.impl.targetName
              ), ctx.importInstances
            )
            val buildRule =
              addNode(BuildRuleNode(def, paramTypes, paramDefaultValues, returnType, implTarget))
            paramTypes.values.forEach { addEdge(buildRule, it, TaskEdgeType.TypeDependency) }
            paramDefaultValues.values.forEach {
              addEdge(buildRule, it, TaskEdgeType.DefaultValueDependency)
            }
            addEdge(buildRule, returnType, TaskEdgeType.TypeDependency)
            addEdge(buildRule, implTarget, TaskEdgeType.ValueDependency)
          }

          is BibixAst.ActionRuleDef -> {
            // TODO
          }

          is BibixAst.DefsWithVarRedefs -> {
            val varRedefs = compileVarRedefs(def.varRedefs, ctx)
            val importWithRedefs = varRedefs.map { (importNodeId, varRedefs) ->
              val importInstanceNode = addNode(ImportInstanceNode(importNodeId, varRedefs))
              addEdge(importInstanceNode, importNodeId, TaskEdgeType.ImportInstance)
              varRedefs.values.forEach {
                addEdge(importInstanceNode, it, TaskEdgeType.ValueDependency)
              }
              importNodeId to importInstanceNode
            }.toMap()
            println(varRedefs)
            println(importWithRedefs)
            addDefs(def.defs, ctx.withVarRedefsCtx(importWithRedefs), false)
          }
        }
      }
    }

    fun compileVarRedefs(varRedefs: List<BibixAst.VarRedef>, ctx: GraphBuildContext) =
      varRedefs.map { varRedef ->
        val lookupResult = ctx.nameLookupCtx.lookupName(varRedef.nameTokens, varRedef)
        check(lookupResult is NameInImport)
        check(lookupResult.remaining.size == 1)
        val import = lookupResult.importEntry.id
        val redefVarName = lookupResult.remaining.first()
        val redefValue = addExpr(varRedef.redefValue, ctx)
        import to (redefVarName to redefValue)
      }.groupBy { it.first }.mapValues { (_, importAndRedefs) ->
        val redefs = importAndRedefs.map { it.second }
        check(redefs.map { it.first }.hasNoDuplicate())
        redefs.toMap()
      }

    fun List<String>.hasNoDuplicate(): Boolean =
      this.toSet().size == this.size

    fun addType(type: BibixAst.TypeExpr, ctx: GraphBuildContext): TaskId =
      when (type) {
        is BibixAst.CollectionType -> {
          val typeParams = type.typeParams.params.map { addType(it, ctx) }
          val typeNode = addNode(CollectionTypeNode(type, typeParams))
          typeParams.forEach { addEdge(typeNode, it, TaskEdgeType.TypeDependency) }
          typeNode
        }

        is BibixAst.Name -> {
          val lookupResult = nameLookup.lookupName(type.tokens, type)
          val referred = if (lookupResult is NameNotFound) {
            val bibixType = when (type.tokens) {
              listOf("any") -> AnyType
              listOf("boolean") -> BooleanType
              listOf("string") -> StringType
              listOf("file") -> FileType
              listOf("directory") -> DirectoryType
              listOf("path") -> PathType
              listOf("buildrule") -> BuildRuleDefType
              listOf("actionrule") -> ActionRuleDefType
              listOf("type") -> TypeType
              listOf("none") -> NoneType
              else -> throw NameNotFoundException(lookupResult)
            }
            addNode(BibixTypeNode(bibixType))
          } else {
            lookupResultToId(lookupResult, ctx.importInstances)
          }
          val nameNode = addNode(TypeNameNode(type))
          addEdge(nameNode, referred, TaskEdgeType.TypeDependency)
          nameNode
        }

        is BibixAst.TupleType -> {
          val elemTypes = type.elems.map { addType(it, ctx) }
          val tupleType = addNode(TupleTypeNode(type, elemTypes))
          elemTypes.forEach { addEdge(tupleType, it, TaskEdgeType.TypeDependency) }
          tupleType
        }

        is BibixAst.NamedTupleType -> {
          val elemTypes = type.elems.associate { it.name to addType(it.typ, ctx) }
          val namedTupleType = addNode(NamedTupleTypeNode(type, elemTypes))
          elemTypes.forEach { addEdge(namedTupleType, it.value, TaskEdgeType.TypeDependency) }
          namedTupleType
        }

        is BibixAst.UnionType -> {
          val elemTypes = type.elems.map { addType(it, ctx) }
          val unionType = addNode(UnionTypeNode(type, elemTypes))
          elemTypes.forEach { addEdge(unionType, it, TaskEdgeType.TypeDependency) }
          unionType
        }
      }

    fun lookupResultToId(lookupResult: NameLookupResult, importInstances: Map<TaskId, TaskId>) =
      when (lookupResult) {
        is NameEntryFound -> lookupResult.entry.id

        is NameInImport -> {
          val importNode = importInstances[lookupResult.importEntry.id]
            ?: addNode(ImportInstanceNode(lookupResult.importEntry.id, mapOf()))

          if (lookupResult.remaining.isEmpty()) {
            importNode
          } else {
            val importedName = addNode(ImportedTaskNode(importNode, lookupResult.remaining))
            addEdge(importedName, importNode, TaskEdgeType.ImportDependency)
            importedName
          }
        }

        is NameFromPrelude -> addNode(PreludeTaskNode(lookupResult.name))
        is NameOfPreloadedPlugin ->
          if (lookupResult.isPrelude) {
            addNode(PreloadedPluginNode(lookupResult.name))
          } else {
            throw IllegalStateException("Name not found: ${lookupResult.name}")
          }

        is NamespaceFound -> throw IllegalStateException("Name not found: ${lookupResult.name}")
        is NameNotFound -> throw NameNotFoundException(lookupResult)
      }

    fun addExpr(expr: BibixAst.Expr, ctx: GraphBuildContext): TaskId = when (expr) {
      is BibixAst.CastExpr -> {
        val valueNode = addExpr(expr.expr, ctx)
        val typeNode = addType(expr.castTo, ctx)
        val exprNode = addNode(CastExprNode(expr, valueNode, typeNode))
        addEdge(exprNode, valueNode, TaskEdgeType.ValueDependency)
        addEdge(exprNode, typeNode, TaskEdgeType.TypeDependency)
        exprNode
      }

      is BibixAst.MergeOp -> {
        val lhs = addExpr(expr.lhs, ctx)
        val rhs = addExpr(expr.rhs, ctx)
        val exprNode = addNode(MergeExprNode(expr, lhs, rhs))
        addEdge(exprNode, lhs, TaskEdgeType.ValueDependency)
        addEdge(exprNode, rhs, TaskEdgeType.ValueDependency)
        exprNode
      }

      is BibixAst.CallExpr -> {
        val ruleOrClass =
          lookupResultToId(ctx.nameLookupCtx.lookupName(expr.name), ctx.importInstances)
        val posParams = expr.params.posParams.map { param ->
          addExpr(param, ctx)
        }
        check(expr.params.namedParams.map { it.name }.hasNoDuplicate())
        val namedParams = expr.params.namedParams.associate { (name, param) ->
          name to addExpr(param, ctx)
        }
        val exprNode = addNode(CallExprNode(expr, ruleOrClass, posParams, namedParams))
        addEdge(exprNode, ruleOrClass, TaskEdgeType.CalleeDependency)
        posParams.forEach { addEdge(exprNode, it, TaskEdgeType.ValueDependency) }
        namedParams.forEach { addEdge(exprNode, it.value, TaskEdgeType.ValueDependency) }
        exprNode
      }

      is BibixAst.ListExpr -> {
        val elems = expr.elems.map { elem ->
          when (elem) {
            is BibixAst.EllipsisElem -> {
              ListExprNode.ListElem(addExpr(elem.value, ctx), true)
//              addEdge(listNode, elemNode, TaskEdgeType.ValueDependency)
            }

            is BibixAst.Expr -> {
              ListExprNode.ListElem(addExpr(elem, ctx), false)
//              addEdge(listNode, elemNode, TaskEdgeType.ValueDependency)
            }
          }
        }
        val exprNode = addNode(ListExprNode(expr, elems))
        elems.forEach { addEdge(exprNode, it.valueNode, TaskEdgeType.ValueDependency) }
        exprNode
      }

      is BibixAst.BooleanLiteral -> addNode(LiteralNode(expr))
      is BibixAst.NoneLiteral -> addNode(LiteralNode(expr))

      is BibixAst.StringLiteral -> {
        val exprElems = expr.elems.mapNotNull { elem ->
          when (elem) {
            is BibixAst.EscapeChar, is BibixAst.JustChar -> null

            is BibixAst.ComplexExpr -> addExpr(elem.expr, ctx)

            is BibixAst.SimpleExpr ->
              lookupResultToId(nameLookup.lookupName(listOf(elem.name), elem), ctx.importInstances)
          }
        }
        val exprNode = addNode(StringNode(expr, exprElems))
        exprElems.forEach { addEdge(exprNode, it, TaskEdgeType.ValueDependency) }
        exprNode
      }

      is BibixAst.MemberAccess -> {
        val target = addExpr(expr.target, ctx)
        val exprNode = addNode(MemberAccessNode(expr, target))
        addEdge(exprNode, target, TaskEdgeType.ValueDependency)
        exprNode
      }

      is BibixAst.NameRef -> {
        val referedNode =
          lookupResultToId(ctx.nameLookupCtx.lookupName(expr), ctx.importInstances)
        val exprNode = addNode(NameRefNode(expr, referedNode))
        addEdge(exprNode, referedNode, TaskEdgeType.Reference)
        exprNode
      }

      is BibixAst.NamedTupleExpr -> {
        val elemNodes = expr.elems.map { elem ->
          elem.name to addExpr(elem.expr, ctx)
        }
        val exprNode = addNode(NamedTupleNode(expr, elemNodes))
        elemNodes.forEach {
          addEdge(exprNode, it.second, TaskEdgeType.ValueDependency)
        }
        exprNode
      }

      is BibixAst.Paren -> addExpr(expr.expr, ctx)
      is BibixAst.This -> {
        // TODO this는 어떻게 처리하지? context에 뭔가 더 추가돼야될 듯 한데
        addNode(EtcExprNode(expr))
      }

      is BibixAst.TupleExpr -> {
        val elemNodes = expr.elems.map { elem -> addExpr(elem, ctx) }
        val exprNode = addNode(TupleNode(expr, elemNodes))
        elemNodes.forEach { addEdge(exprNode, it, TaskEdgeType.ValueDependency) }
        exprNode
      }
    }

    fun addNode(node: TaskNode): TaskId {
      val existing = nodes[node.id]
      // ktjvm.library 같은 것들 때문에 두번 이상 등록될 수는 있지만 내용은 같아야 함
      check(existing == null || existing == node)
      nodes[node.id] = node
      return node.id
    }

    fun addEdge(start: TaskId, end: TaskId, edgeType: TaskEdgeType) {
      val existing = edgePairs[Pair(start, end)]
      val edge = TaskEdge(start, end, edgeType)
      check(existing == null || existing == edge)
      if (existing == null) {
        edgePairs[Pair(start, end)] = edge
        edges.add(edge)
      }
    }
  }
}
