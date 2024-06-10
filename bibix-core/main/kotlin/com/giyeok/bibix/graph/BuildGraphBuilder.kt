package com.giyeok.bibix.graph

import com.giyeok.bibix.ast.BibixAst
import com.giyeok.bibix.base.*

class BuildGraphBuilder(
  val packageName: String?,
  val preloadedPluginNames: Set<String>,

  val targets: MutableMap<BibixName, ExprNodeId> = mutableMapOf(),
  val buildRules: MutableMap<BibixName, BuildRuleDef> = mutableMapOf(),
  val vars: MutableMap<BibixName, VarDef> = mutableMapOf(),
  val dataClasses: MutableMap<BibixName, DataClassDef> = mutableMapOf(),
  val superClasses: MutableMap<BibixName, SuperClassDef> = mutableMapOf(),
  val enums: MutableMap<BibixName, EnumDef> = mutableMapOf(),
  val actions: MutableMap<BibixName, ActionDef> = mutableMapOf(),
  val actionRules: MutableMap<BibixName, ActionRuleDef> = mutableMapOf(),

  val importAlls: MutableMap<BibixName, ImportAllDef> = mutableMapOf(),
  val importFroms: MutableMap<BibixName, ImportFromDef> = mutableMapOf(),
  // import name -> (var name -> redef expr node id)
  val varRedefs: MutableMap<BibixName, MutableMap<BibixName, ExprNodeId>> = mutableMapOf(),

  val exprNodes: MutableMap<ExprNodeId, ExprGraphNode> = mutableMapOf(),
  val exprEdges: MutableSet<ExprGraphEdge> = mutableSetOf(),
  val exprTypeEdges: MutableSet<ExprTypeEdge> = mutableSetOf(),
  val typeNodes: MutableMap<TypeNodeId, TypeGraphNode> = mutableMapOf(),
  val typeEdges: MutableSet<TypeGraphEdge> = mutableSetOf(),
) {
  fun addDefs(defs: List<BibixAst.Def>, ctx: GraphBuildContext, isRoot: Boolean) {
    defs.forEach { def ->
      when (def) {
        is BibixAst.ActionDef -> {
          var localCtx = def.argsName?.let { ctx.withLocalLetName(it) } ?: ctx
          val actionStmts = def.body.stmts.map { stmt ->
            when (stmt) {
              is BibixAst.LetStmt -> {
                val letExpr = addExpr(stmt.expr, localCtx)
                localCtx = localCtx.withLocalLetName(stmt.name)
                ActionDef.LetStmt(stmt.name, letExpr)
              }

              is BibixAst.CallExpr -> {
                val (callee, posParams, namedParams) = addCallExpr(stmt, localCtx)
                ActionDef.CallStmt(callee, posParams, namedParams)
              }
            }
          }
          actions[localCtx.bibixName(def.name)] = ActionDef(def, actionStmts)
        }

        is BibixAst.ActionRuleDef -> {
          val implTarget =
            if (ctx.nativeAllowed && def.impl.targetName.tokens == listOf("native")) {
              null
            } else {
              lookupExprName(def.impl.targetName.tokens, ctx)
            }
          val paramTypes = def.params.associate { it.name to addType(it.typ!!, ctx) }
          actionRules[ctx.bibixName(def.name)] = ActionRuleDef(
            def = def,
            params = paramTypes,
            paramDefaultValues = defaultValuesMap(def.params, paramTypes, ctx),
            implTarget = implTarget,
            implClassName = def.impl.className.tokens.joinToString("."),
            implMethodNameOpt = def.impl.methodName
          )
        }

        is BibixAst.BuildRuleDef -> {
          val implTarget =
            if (ctx.nativeAllowed && def.impl.targetName.tokens == listOf("native")) {
              null
            } else {
              lookupExprName(def.impl.targetName.tokens, ctx)
            }
          val paramTypes = def.params.associate { it.name to addType(it.typ!!, ctx) }

          buildRules[ctx.bibixName(def.name)] = BuildRuleDef(
            def = def,
            params = paramTypes,
            paramDefaultValues = defaultValuesMap(def.params, paramTypes, ctx),
            returnType = addType(def.returnType, ctx),
            implTarget = implTarget,
            implClassName = def.impl.className.tokens.joinToString("."),
            implMethodNameOpt = def.impl.methodName
          )
        }

        is BibixAst.ImportAll -> {
          val importName = def.rename ?: def.source.getDefaultImportName()
          val source = importSource(def.source, ctx)
          importAlls[ctx.bibixName(importName)] = ImportAllDef(source)
        }

        is BibixAst.ImportFrom -> {
          val importName = def.rename ?: def.importing.tokens.last()
          val source = importSource(def.source, ctx)
          importFroms[ctx.bibixName(importName)] = ImportFromDef(source, def.importing.tokens)
        }

        is BibixAst.NamespaceDef -> {
          addDefs(def.body, ctx.innerNamespace(def.name), false)
        }

        is BibixAst.TargetDef -> {
          targets[ctx.bibixName(def.name)] = addExpr(def.value, ctx)
        }

        is BibixAst.DataClassDef -> {
          val fieldTypes = def.fields.associate { it.name to addType(it.typ!!, ctx) }
          val customAutoCasts = mutableListOf<Pair<TypeNodeId, ExprNodeId>>()
          val ctxWithThis = ctx.withThisRefAllowed()
          def.body.forEach { elem ->
            when (elem) {
              is BibixAst.ActionRuleDef -> TODO()
              is BibixAst.ClassCastDef -> {
                val targetType = addType(elem.castTo, ctxWithThis)
                val convertExpr = addExpr(elem.expr, ctxWithThis)
                customAutoCasts.add(Pair(targetType, convertExpr))
              }
            }
          }
          val dataClassDef = DataClassDef(
            def = def,
            fields = fieldTypes,
            fieldDefaultValues = defaultValuesMap(def.fields, fieldTypes, ctx),
            customAutoCasts = customAutoCasts
          )
          dataClasses[ctx.bibixName(def.name)] = dataClassDef
        }

        is BibixAst.SuperClassDef -> {
          // subs가 모두 같은 플러그인, 같은 네임 스페이스 안에 위치한 data class 혹은 super class인지 확인
          val subs = def.subs.toSet()
          check(subs.isNotEmpty()) { "Super class does not have sub types" }
          val currentScopeNames = ctx.nameLookupCtx.currentScope.table.names
          subs.forEach { sub ->
            val subType = checkNotNull(currentScopeNames[sub]) { "$sub type does not exist" }
            check(subType is DataClassNameEntry || subType is SuperClassNameEntry)
          }
          superClasses[ctx.bibixName(def.name)] = SuperClassDef(def, subs)
        }

        is BibixAst.EnumDef -> {
          enums[ctx.bibixName(def.name)] = EnumDef(def)
        }

        is BibixAst.VarDef -> {
          val varType = when (val specifiedType = def.typ) {
            null -> {
              val defaultValue = def.defaultValue!!
              // TODO 제대로 type inference?
              val inferredType = when (defaultValue) {
                is BibixAst.StringLiteral -> StringType
                else -> TODO()
              }
              VarDef.VarType.FixedType(inferredType)
            }

            else -> VarDef.VarType.TypeNode(addType(specifiedType, ctx))
          }
          vars[ctx.bibixName(def.name)] = VarDef(
            def = def,
            type = varType,
            defaultValue = def.defaultValue?.let { addExpr(it, ctx) }
          )
        }

        is BibixAst.VarRedefs -> {
          def.redefs.forEach { redef ->
            when (val lookupResult = ctx.nameLookupCtx.lookupName(redef.nameTokens)) {
              is NameInImport -> {
                check(isRoot) { "var redef only can be placed in the root" }
                val redefs = varRedefs.getOrPut(lookupResult.importEntry.name) { mutableMapOf() }
                val varName = BibixName(lookupResult.remaining)
                check(varName !in redefs) { "Duplicate var redef: $varName" }
                redefs[varName] = addExpr(redef.redefValue, ctx)
              }

              else -> throw IllegalStateException()
            }
          }
        }
      }
    }
  }

  fun checkNoDuplicateNames() {
    val allNames = mutableSetOf<BibixName>()

    check(allNames.intersect(targets.keys).isEmpty())
    allNames.addAll(targets.keys)
    check(allNames.intersect(buildRules.keys).isEmpty())
    allNames.addAll(buildRules.keys)
    check(allNames.intersect(vars.keys).isEmpty())
    allNames.addAll(vars.keys)
    check(allNames.intersect(dataClasses.keys).isEmpty())
    allNames.addAll(dataClasses.keys)
    check(allNames.intersect(superClasses.keys).isEmpty())
    allNames.addAll(superClasses.keys)
    check(allNames.intersect(enums.keys).isEmpty())
    allNames.addAll(enums.keys)
    check(allNames.intersect(actions.keys).isEmpty())
    allNames.addAll(actions.keys)
    check(allNames.intersect(actionRules.keys).isEmpty())
    allNames.addAll(actionRules.keys)
    check(allNames.intersect(importAlls.keys).isEmpty())
    allNames.addAll(importAlls.keys)
    check(allNames.intersect(importFroms.keys).isEmpty())
  }

  private fun defaultValuesMap(
    params: List<BibixAst.ParamDef>,
    paramTypes: Map<String, TypeNodeId>,
    ctx: GraphBuildContext
  ) = params.filter { it.defaultValue != null }.associate {
    val defaultValue = it.defaultValue!!
    val value = addExpr(defaultValue, ctx)
    it.name to addNode(ValueCastNode(value, paramTypes.getValue(it.name)))
  }

  private fun importSource(
    source: BibixAst.Expr,
    ctx: GraphBuildContext
  ): ImportSource {
    when (source) {
      is BibixAst.NameRef -> {
        if (source.name in preloadedPluginNames) {
          return ImportSource.PreloadedPlugin(source.name)
        }
        when (val lookupResult = ctx.nameLookupCtx.lookupName(listOf(source.name), source)) {
          is NameEntryFound -> {
            when (val entry = lookupResult.entry) {
              is ImportNameEntry -> {
                return ImportSource.AnotherImport(entry.name)
              }

              else -> {}
            }
          }

          else -> {}
        }
      }

      else -> {
        // do nothing
      }
    }
    return ImportSource.Expr(addExpr(source, ctx))
  }

  private fun addNode(node: ExprGraphNode): ExprNodeId {
    val existing = exprNodes[node.id]
    if (existing != null) {
      check(existing == node)
    }
    exprNodes[node.id] = node
    return node.id
  }

  private fun addEdge(start: ExprNodeId, end: ExprNodeId) {
    exprEdges.add(ExprGraphEdge(start, end))
  }

  private fun addEdge(start: ExprNodeId, end: Callee) {
    // TODO
  }

  private fun addEdge(start: ExprNodeId, end: TypeNodeId) {
    exprTypeEdges.add(ExprTypeEdge(start, end))
  }

  private fun lookupExprName(nameTokens: List<String>, ctx: GraphBuildContext): ExprNodeId =
    when (val lookupResult = ctx.nameLookupCtx.lookupName(nameTokens)) {
      is EnumValueFound ->
        addNode(LocalEnumValue(lookupResult.enum.name, lookupResult.enumValueName))

      is NameEntryFound -> {
        val node = when (val entry = lookupResult.entry) {
          is BuildRuleNameEntry -> LocalBuildRuleRef(entry.name, entry.def)
          is DataClassNameEntry -> LocalDataClassRef(entry.name, entry.def)
          is TargetNameEntry -> LocalTargetRef(entry.name, entry.def)
          is VarNameEntry -> LocalVarRef(entry.name, entry.def)
          is ActionNameEntry -> LocalActionRef(entry.name, entry.def)
          is ActionRuleNameEntry -> LocalActionRuleRef(entry.name, entry.def)
          // TODO local import ref
          else -> throw IllegalStateException("Name not found ${entry.name.tokens}")
        }
        addNode(node)
      }

      is NameFromPrelude ->
        addNode(ImportedExprFromPrelude(lookupResult.name))

//      is NameOfPreloadedPlugin ->
//        addNode(ImportedExprFromPreloaded(lookupResult.name, BibixName(lookupResult.remaining)))

      is NameInImport ->
        addNode(ImportedExpr(lookupResult.importEntry.name, BibixName(lookupResult.remaining)))

      is TargetMemberName -> {
        val target =
          addNode(LocalTargetRef(lookupResult.targetEntry.name, lookupResult.targetEntry.def))
        val access = addNode(MemberAccessNode(target, lookupResult.remaining))
        addEdge(access, target)
        access
      }

      is NameNotFound, is NamespaceFound -> throw NameNotFoundException(nameTokens, null)
    }

  data class CallExprAddResult(
    val callee: Callee,
    val posParams: List<ExprNodeId>,
    val namedParams: Map<String, ExprNodeId>
  )

  private fun lookupCallee(nameTokens: List<String>, ctx: GraphBuildContext): Callee =
    when (val lookupResult = ctx.nameLookupCtx.lookupName(nameTokens)) {
      is NameEntryFound -> {
        when (val entry = lookupResult.entry) {
          is BuildRuleNameEntry -> Callee.LocalBuildRule(entry.name)
          is ActionRuleNameEntry -> Callee.LocalActionRule(entry.name)
          is DataClassNameEntry -> Callee.LocalDataClass(entry.name)
          is ImportNameEntry -> Callee.ImportedCallee(entry.name)
          is ActionNameEntry -> Callee.LocalAction(entry.name)
          else -> throw IllegalStateException("Invalid callee: $nameTokens")
        }
      }

      is NameInImport ->
        Callee.ImportedMemberCallee(lookupResult.importEntry.name, lookupResult.remaining)

      is NameFromPrelude ->
        Callee.PreludeMember(lookupResult.name)

      else -> throw IllegalStateException("Invalid callee: $nameTokens")
    }

  private fun addCallExpr(expr: BibixAst.CallExpr, ctx: GraphBuildContext): CallExprAddResult {
    val callee = lookupCallee(expr.name.tokens, ctx)
    val posParams = expr.params.posParams.map { addExpr(it, ctx) }
    val namedParams = expr.params.namedParams.associate { (name, argExpr) ->
      name to addExpr(argExpr, ctx)
    }
    return CallExprAddResult(callee, posParams, namedParams)
  }

  private fun addExpr(expr: BibixAst.Expr, ctx: GraphBuildContext): ExprNodeId = when (expr) {
    is BibixAst.CastExpr -> {
      val valueNode = addExpr(expr.expr, ctx)
      val typeNode = addType(expr.castTo, ctx)
      val node = addNode(ValueCastNode(valueNode, typeNode))
      addEdge(node, valueNode)
      addEdge(node, typeNode)
      node
    }

    is BibixAst.MergeOp -> {
      val lhsNode = addExpr(expr.lhs, ctx)
      val rhsNode = addExpr(expr.rhs, ctx)
      val node = addNode(MergeExprNode(expr, lhsNode, rhsNode))
      addEdge(node, lhsNode)
      addEdge(node, rhsNode)
      node
    }

    is BibixAst.CallExpr -> {
      val (callee, posParams, namedParams) = addCallExpr(expr, ctx)
      val call = addNode(CallExprNode(expr, callee, posParams, namedParams))
      addEdge(call, callee)
      posParams.forEach { addEdge(call, it) }
      namedParams.forEach { addEdge(call, it.value) }
      call
    }

    is BibixAst.ListExpr -> {
      val elems = expr.elems.map { listElem ->
        when (listElem) {
          is BibixAst.EllipsisElem -> ListElem(addExpr(listElem.value, ctx), true)
          is BibixAst.Expr -> ListElem(addExpr(listElem, ctx), false)
        }
      }
      val list = addNode(ListExprNode(expr, elems))
      elems.forEach { addEdge(list, it.value) }
      list
    }

    is BibixAst.BooleanLiteral -> addNode(BooleanLiteralNode(expr))
    is BibixAst.NoneLiteral -> addNode(NoneLiteralNode(expr))

    is BibixAst.StringLiteral -> {
      val stringTypeNode = addNode(BasicTypeNode(StringType))
      val elems = expr.elems.mapNotNull { elem ->
        val value = when (elem) {
          is BibixAst.EscapeChar, is BibixAst.JustChar -> null
          is BibixAst.ComplexExpr -> addExpr(elem.expr, ctx)
          is BibixAst.SimpleExpr -> lookupExprName(listOf(elem.name), ctx)
        }
        value?.let {
          val cast = addNode(ValueCastNode(value, stringTypeNode))
          addEdge(cast, value)
          cast
        }
      }
      val exprNode = addNode(StringNode(expr, elems))
      elems.forEach { addEdge(exprNode, it) }
      exprNode
    }

    is BibixAst.MemberAccess -> {
      val (firstTarget, memberNames) = expr.firstNonName()
      if (firstTarget == null) {
        lookupExprName(memberNames, ctx)
      } else {
        val target = addExpr(firstTarget, ctx)
        val access = addNode(MemberAccessNode(target, memberNames))
        addEdge(access, target)
        access
      }
    }

    is BibixAst.NameRef -> {
      if (expr.name in ctx.localLetNames) {
        addNode(ActionLocalLetNode(expr))
      } else {
        lookupExprName(listOf(expr.name), ctx)
      }
    }

    is BibixAst.Paren -> addExpr(expr.expr, ctx)
    is BibixAst.This -> {
      check(ctx.thisRefAllowed)
      addNode(ThisRefNode(expr))
    }

    is BibixAst.TupleExpr -> {
      val elems = expr.elems.map { addExpr(it, ctx) }
      val tuple = addNode(TupleNode(expr, elems))
      elems.forEach { addEdge(tuple, it) }
      tuple
    }

    is BibixAst.NamedTupleExpr -> {
      val elems = expr.elems.map { (name, elem) ->
        name to addExpr(elem, ctx)
      }
      val tuple = addNode(NamedTupleNode(expr, elems))
      elems.forEach { addEdge(tuple, it.second) }
      tuple
    }
  }

  private fun addNode(node: TypeGraphNode): TypeNodeId {
    val existing = typeNodes[node.id]
    if (existing != null) {
      check(existing == node)
    }
    typeNodes[node.id] = node
    return node.id
  }

  private fun addEdge(start: TypeNodeId, end: TypeNodeId) {
    typeEdges.add(TypeGraphEdge(start, end))
  }

  private fun addType(type: BibixAst.TypeExpr, ctx: GraphBuildContext): TypeNodeId = when (type) {
    is BibixAst.CollectionType -> {
      when (type.name) {
        "list" -> {
          check(type.typeParams.params.size == 1)
          val elemType = addType(type.typeParams.params.first(), ctx)
          val set = addNode(ListTypeNode(type, elemType))
          addEdge(set, elemType)
          set
        }

        "set" -> {
          check(type.typeParams.params.size == 1)
          val elemType = addType(type.typeParams.params.first(), ctx)
          val set = addNode(SetTypeNode(type, elemType))
          addEdge(set, elemType)
          set
        }

        else -> throw IllegalStateException("Unknown collection type")
      }
    }

    is BibixAst.Name -> {
      val basicType = when (type.tokens) {
        listOf("any") -> AnyType
        listOf("boolean") -> BooleanType
        listOf("string") -> StringType
        listOf("file") -> FileType
        listOf("directory") -> DirectoryType
        listOf("path") -> PathType
        listOf("buildrule") -> BuildRuleDefType
        listOf("actionrule") -> ActionRuleDefType
        listOf("type") -> TypeType
        else -> null
      }
      if (basicType != null) {
        addNode(BasicTypeNode(basicType))
      } else {
        // name lookup해서 로컬에서 알고 있는 data, super, enum 이면 LocalDataClassTypeRef, LocalSuperClassTypeRef, LocalEnumTypeRef로,
        // import하는 이름이면 ImportedType으로 반환
        val node: TypeGraphNode =
          when (val lookupResult = ctx.nameLookupCtx.lookupName(type.tokens, type)) {
            is NameEntryFound -> {
              when (val entry = lookupResult.entry) {
                is DataClassNameEntry -> LocalDataClassTypeRef(entry.name, entry.def)
                is SuperClassNameEntry -> LocalSuperClassTypeRef(entry.name, entry.def)
                is EnumNameEntry -> LocalEnumTypeRef(entry.name, entry.def)
                else -> throw IllegalStateException()
              }
            }

            is NameFromPrelude -> {
              ImportedTypeFromPrelude(lookupResult.name)
            }

//            is NameOfPreloadedPlugin -> {
//              ImportedTypeFromPreloaded(lookupResult.name, BibixName(lookupResult.remaining))
//            }

            is NameInImport -> {
              ImportedType(lookupResult.importEntry.name, BibixName(lookupResult.remaining))
            }

            else -> throw NameNotFoundException(type.tokens, type)
          }
        addNode(node)
      }
    }

    is BibixAst.NoneType -> addNode(BasicTypeNode(NoneType))

    is BibixAst.TupleType -> {
      val elemTypes = type.elems.map { addType(it, ctx) }
      val tupleType = addNode(TupleTypeNode(type, elemTypes))
      elemTypes.forEach { addEdge(tupleType, it) }
      tupleType
    }

    is BibixAst.NamedTupleType -> {
      val elemTypes = type.elems.associate { it.name to addType(it.typ, ctx) }
      val tupleType = addNode(NamedTupleTypeNode(type, elemTypes))
      elemTypes.forEach { addEdge(tupleType, it.value) }
      tupleType
    }

    is BibixAst.UnionType -> {
      val elemTypes = type.elems.map { addType(it, ctx) }
      val unionType = addNode(UnionTypeNode(type, elemTypes))
      elemTypes.forEach { addEdge(unionType, it) }
      unionType
    }
  }

  fun build(): BuildGraph = BuildGraph(
    packageName = packageName,
    targets = targets,
    buildRules = buildRules,
    vars = vars,
    dataClasses = dataClasses,
    superClasses = superClasses,
    enums = enums,
    actions = actions,
    actionRules = actionRules,
    importAlls = importAlls,
    importFroms = importFroms,
    varRedefs = varRedefs,
    exprGraph = ExprGraph(exprNodes, exprEdges),
    typeGraph = TypeGraph(typeNodes, typeEdges),
    exprTypeEdges = exprTypeEdges
  )
}

data class GraphBuildContext(
  val nameLookupCtx: NameLookupContext,
  val thisRefAllowed: Boolean,
  val nativeAllowed: Boolean,
  val localLetNames: Set<String>
) {
  fun innerNamespace(namespaceName: String) =
    copy(nameLookupCtx = nameLookupCtx.innerNamespace(namespaceName))

  fun withThisRefAllowed() =
    copy(thisRefAllowed = true)

  fun bibixName(name: String) = BibixName(nameLookupCtx.currentScope.namePath + name)

  fun withLocalLetName(newName: String) = copy(localLetNames = localLetNames + newName)
}

fun BibixAst.Primary.getDefaultImportName(): String = when (this) {
  is BibixAst.NameRef -> this.name
  is BibixAst.MemberAccess -> this.name
  else -> throw IllegalStateException("Cannot infer the default name for import")
}

fun BibixAst.MemberAccess.firstNonName(): Pair<BibixAst.Expr?, List<String>> =
  when (val target = this.target) {
    is BibixAst.MemberAccess -> {
      val (firstTarget, memberNames) = target.firstNonName()
      Pair(firstTarget, memberNames + this.name)
    }

    is BibixAst.NameRef -> Pair(null, listOf(target.name, this.name))
    else -> Pair(target, listOf(this.name))
  }

fun BibixAst.ImportDef.importName() = when (this) {
  is BibixAst.ImportAll -> this.rename ?: this.source.getDefaultImportName()
  is BibixAst.ImportFrom -> this.rename ?: this.importing.tokens.last()
}
