package com.giyeok.bibix.graph

import com.giyeok.bibix.ast.BibixAst
import com.giyeok.bibix.base.*

class TaskGraphBuilder(
  val astNodes: Map<Int, BibixAst.AstNode>,
  val nameLookupTable: NameLookupTable,
  val nodes: MutableMap<TaskId, TaskNode> = mutableMapOf(),
  val edges: MutableList<TaskEdge> = mutableListOf(),
  val scriptVars: MutableMap<String, TaskId> = mutableMapOf(),
  val varRedefs: MutableMap<TaskId, Map<String, TaskId>> = mutableMapOf(),
) {
  private val edgePairs: MutableMap<Pair<TaskId, TaskId>, TaskEdge> =
    edges.associateBy { Pair(it.start, it.end) }.toMutableMap()

  fun build() = TaskGraph(astNodes, nameLookupTable, nodes, edges, scriptVars, varRedefs)

  fun addImportSource(source: BibixAst.Expr, ctx: GraphBuildContext): TaskId {
    if (source is BibixAst.NameRef) {
      if (source.name in ctx.nameLookupCtx.preloadedPluginNames) {
        return addNode(PreloadedPluginNode(source.name))
//        val nameNode = addNode(NameRefNode(source, ref))
//        addEdge(nameNode, ref, TaskEdgeType.ValueDependency)
//        return nameNode
      }
    }
    return addExpr(source, ctx)
  }

  fun addDefs(defs: List<BibixAst.Def>, ctx: GraphBuildContext, isRoot: Boolean) {
    defs.forEach { def ->
      when (def) {
        is BibixAst.ImportDef -> {
          check(isRoot) { "import must be in the root" }
          val source = when (def) {
            is BibixAst.ImportAll -> addImportSource(def.source, ctx)
            is BibixAst.ImportFrom -> addImportSource(def.source, ctx)
          }
          val importNode = addNode(ImportNode(def, source))
          addEdge(importNode, source, TaskEdgeType.ValueDependency)
          val defaultImportInstanceNode = addNode(ImportInstanceNode(importNode, null, mapOf()))
          addEdge(defaultImportInstanceNode, importNode, TaskEdgeType.ImportInstance)
        }

        is BibixAst.NamespaceDef -> {
          addDefs(def.body, ctx.innerNamespace(def.name), false)
        }

        is BibixAst.TargetDef -> {
          val valueNode = addExpr(def.value, ctx)
          val targetNode = addNode(TargetNode(def, valueNode))
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
                val castExpr = addExpr(classElem.expr, ctx.withThisRefAllowed())
                addNode(ClassElemCastNode(classElem, castTo, castExpr))
              }
            }
          }
          val classNode = addNode(DataClassTypeNode(def, fieldTypes, defaultValues, bodyElems))
          fieldTypes.forEach { addEdge(classNode, it.value, TaskEdgeType.TypeDependency) }
          defaultValues.forEach {
            addEdge(classNode, it.value, TaskEdgeType.DefaultValueDependency)
          }
          bodyElems.forEach { addEdge(classNode, it, TaskEdgeType.ClassMember) }
        }

        is BibixAst.SuperClassDef -> {
          val subClasses = def.subs.associate { subClassName ->
            val entry = ctx.nameLookupCtx.table.names[subClassName]
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
          val implTarget =
            if (ctx.nativeAllowed && def.impl.targetName.tokens == listOf("native")) {
              val className = def.impl.className.tokens.joinToString(".")
              addNode(NativeImplNode(def.impl.targetName, className, def.impl.methodName))
            } else {
              lookupResultToId(
                ctx.nameLookupCtx.lookupName(def.impl.targetName.tokens, def.impl.targetName),
                ctx.importInstances
              )
            }
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
            val importInstanceNode = addNode(ImportInstanceNode(importNodeId, def, varRedefs))
            addEdge(importInstanceNode, importNodeId, TaskEdgeType.ImportInstance)
            varRedefs.values.forEach {
              addEdge(importInstanceNode, it, TaskEdgeType.DefaultValueDependency)
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
          else -> null
        }

        val referred = if (bibixType != null) {
          addNode(BibixTypeNode(bibixType))
        } else {
          val lookupResult = ctx.nameLookupCtx.lookupName(type.tokens, type)
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

      is EnumValueFound -> {
        addNode(EnumValueNode(lookupResult.enum.def, lookupResult.enumMemberName))
      }

      is NameInImport -> {
        val importNode = importInstances[lookupResult.importEntry.id]
          ?: addNode(ImportInstanceNode(lookupResult.importEntry.id, null, mapOf()))

        if (lookupResult.remaining.isEmpty()) {
          importNode
        } else {
          val importedName = addNode(MemberAccessNode(importNode, lookupResult.remaining))
          addEdge(importedName, importNode, TaskEdgeType.ImportDependency)
          importedName
        }
      }

      is NameFromPrelude -> {
        val preludeNode = addNode(PreludeTaskNode(lookupResult.name))
        if (lookupResult.remaining.isEmpty()) {
          preludeNode
        } else {
          val importedName = addNode(PreludeMemberNode(lookupResult.name, lookupResult.remaining))
          addEdge(importedName, preludeNode, TaskEdgeType.ImportDependency)
          importedName
        }
      }

      is NameOfPreloadedPlugin -> {
        val pluginNode = if (lookupResult.isPrelude) {
          addNode(PreloadedPluginNode(lookupResult.name))
        } else {
          throw IllegalStateException("Name not found: ${lookupResult.name}")
        }
        val pluginInstanceNode = importInstances[pluginNode]
          ?: addNode(ImportInstanceNode(pluginNode, null, mapOf()))
        addEdge(pluginInstanceNode, pluginNode, TaskEdgeType.ImportDependency)
        if (lookupResult.remaining.isEmpty()) {
          pluginInstanceNode
        } else {
          val importedName = addNode(MemberAccessNode(pluginInstanceNode, lookupResult.remaining))
          addEdge(importedName, pluginInstanceNode, TaskEdgeType.ImportDependency)
          importedName
        }
      }

      is NamespaceFound -> throw IllegalStateException("Name not found: ${lookupResult.name}")
      is NameNotFound -> throw NameNotFoundException(lookupResult)
    }

  fun addExpr(expr: BibixAst.Expr, ctx: GraphBuildContext): TaskId {
    val exprNode = when (expr) {
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
        // TODO params에 type cast - 그런데 타입을 아직 모르는데..
        //   type cast, type coercion이 문제네..
        //   buildrule resolve할 때 default value랑 같이 type cast 관련된 정보도 그래프에 추가해줘야겠다
        //   -> 그런데 이 때 type cast가 필요한지 아닌지가 실제 파라메터 값에 따라 달라지니까 항상 resolve해야 되는 것은 아님에 유의
        // callee는 rule 혹은 dependency
        val callee =
          lookupResultToId(ctx.nameLookupCtx.lookupName(expr.name), ctx.importInstances)
        val posParams = expr.params.posParams.map { param ->
          addExpr(param, ctx)
        }
        check(expr.params.namedParams.map { it.name }.hasNoDuplicate())
        val namedParams = expr.params.namedParams.associate { (name, param) ->
          name to addExpr(param, ctx)
        }
        val exprNode = addNode(CallExprNode(expr, callee, posParams, namedParams))
        addEdge(exprNode, callee, TaskEdgeType.CalleeDependency)
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

      is BibixAst.BooleanLiteral -> addNode(BooleanLiteralNode(expr))
      is BibixAst.NoneLiteral -> addNode(NoneLiteralNode(expr))

      is BibixAst.StringLiteral -> {
        // TODO CastToString이 들어가야 함
        val exprElems = expr.elems.mapNotNull { elem ->
          when (elem) {
            is BibixAst.EscapeChar, is BibixAst.JustChar -> null

            is BibixAst.ComplexExpr -> addExpr(elem.expr, ctx)

            is BibixAst.SimpleExpr -> lookupResultToId(
              ctx.nameLookupCtx.lookupName(listOf(elem.name), elem),
              ctx.importInstances
            )
          }
        }
        val exprNode = addNode(StringNode(expr, exprElems))
        exprElems.forEach { addEdge(exprNode, it, TaskEdgeType.ValueDependency) }
        exprNode
      }

      is BibixAst.MemberAccess -> {
        fun firstTarget(
          expr: BibixAst.MemberAccess,
          names: List<String>
        ): Pair<BibixAst.Expr?, List<String>> =
          when (val target = expr.target) {
            is BibixAst.MemberAccess -> {
              val (firstTarget, memberNames) = firstTarget(target, names)
              Pair(firstTarget, memberNames + expr.name)
            }

            is BibixAst.NameRef -> {
              Pair(null, listOf(target.name, expr.name))
            }

            else -> Pair(target, listOf(expr.name))
          }

        val (firstTarget, memberNames) = firstTarget(expr, listOf())
        if (firstTarget == null) {
          val lookupResult = ctx.nameLookupCtx.lookupName(memberNames, expr)
          val firstTargetNode = lookupResultToId(lookupResult, ctx.importInstances)
          val exprNode = addNode(MemberAccessExprNode(expr, firstTargetNode, listOf()))
          addEdge(exprNode, firstTargetNode, TaskEdgeType.ValueDependency)
          exprNode
        } else {
          val target = addExpr(firstTarget, ctx)
          val exprNode = addNode(MemberAccessExprNode(expr, target, memberNames))
          addEdge(exprNode, target, TaskEdgeType.ValueDependency)
          exprNode
        }
      }

      is BibixAst.NameRef -> {
        val lookupResult = ctx.nameLookupCtx.lookupName(expr)
        val referedNode = lookupResultToId(lookupResult, ctx.importInstances)
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

      is BibixAst.Paren -> {
        val bodyNode = addExpr(expr.expr, ctx)
        val parenNode = addNode(ParenExprNode(expr, bodyNode))
        addEdge(parenNode, bodyNode, TaskEdgeType.ValueDependency)
        parenNode
      }

      is BibixAst.This -> {
        // ThisRefNode는 실제 실행 시에는 일종의 placeholder로만 동작한다
        check(ctx.thisRefAllowed) { "this is not allowed at ${expr.start}..${expr.end}" }
        addNode(ThisRefNode(expr))
      }

      is BibixAst.TupleExpr -> {
        val elemNodes = expr.elems.map { elem -> addExpr(elem, ctx) }
        val exprNode = addNode(TupleNode(expr, elemNodes))
        elemNodes.forEach { addEdge(exprNode, it, TaskEdgeType.ValueDependency) }
        exprNode
      }
    }
    check(exprNode.nodeId == expr.nodeId)
    return exprNode
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

data class GraphBuildContext(
  val nameLookupCtx: NameLookupContext,
  val importInstances: Map<TaskId, TaskId>,
  val thisRefAllowed: Boolean,
  val nativeAllowed: Boolean,
) {
  fun withVarRedefsCtx(redefsCtx: Map<TaskId, TaskId>) =
    copy(importInstances = importInstances + redefsCtx)

  fun innerNamespace(namespaceName: String) =
    copy(nameLookupCtx = nameLookupCtx.innerNamespace(namespaceName))

  fun withThisRefAllowed() =
    copy(thisRefAllowed = true)
}
