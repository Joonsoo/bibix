package com.giyeok.bibix.graph2

import com.giyeok.bibix.ast.BibixAst
import com.giyeok.bibix.base.*

class BuildGraphBuilder(
  val packageName: String?,

  val targets: MutableMap<BibixName, ExprNodeId> = mutableMapOf(),
  val buildRules: MutableMap<BibixName, BuildRuleDef> = mutableMapOf(),
  val vars: MutableMap<BibixName, VarDef> = mutableMapOf(),
  val dataClasses: MutableMap<BibixName, DataClassDef> = mutableMapOf(),
  val superClasses: MutableMap<BibixName, SuperClassDef> = mutableMapOf(),
  val enums: MutableMap<BibixName, EnumDef> = mutableMapOf(),

  val importAlls: MutableMap<BibixName, ImportAllDef> = mutableMapOf(),
  val importFroms: MutableMap<BibixName, ImportFromDef> = mutableMapOf(),
  // import name -> import instace id (0 혹은 DefsWithVarRedefs의 nodeId) -> VarCtxBuilder(var name -> redef expr node id)
  val importInstances: MutableMap<BibixName, MutableMap<Int, VarCtxBuilder>> = mutableMapOf(),

  val exprNodes: MutableMap<ExprNodeId, ExprGraphNode> = mutableMapOf(),
  val exprEdges: MutableSet<ExprGraphEdge> = mutableSetOf(),
  val exprTypeEdges: MutableSet<ExprTypeEdge> = mutableSetOf(),
  val typeNodes: MutableMap<TypeNodeId, TypeGraphNode> = mutableMapOf(),
  val typeEdges: MutableSet<TypeGraphEdge> = mutableSetOf(),
) {
  class VarCtxBuilder(val parentCtxId: Int, val redefs: MutableMap<BibixName, ExprNodeId>)

  fun addDefs(defs: List<BibixAst.Def>, ctx: GraphBuildContext, isRoot: Boolean) {
    defs.forEach { def ->
      when (def) {
        is BibixAst.ActionDef -> {
          // TODO
        }

        is BibixAst.ActionRuleDef -> {
          // TODO
        }

        is BibixAst.BuildRuleDef -> {
          val implTarget =
            if (ctx.nativeAllowed && def.impl.targetName.tokens == listOf("native")) {
              null
            } else {
              val implNode = lookupExprName(def.impl.targetName.tokens, ctx)
              implNode
            }
          buildRules[ctx.bibixName(def.name)] = BuildRuleDef(
            def = def,
            params = def.params.associate { it.name to addType(it.typ!!, ctx) },
            paramDefaultValues = def.params.filter { it.defaultValue != null }
              .associate { it.name to addExpr(it.defaultValue!!, ctx) },
            returnType = addType(def.returnType, ctx),
            implTarget = implTarget,
            implClassName = def.impl.className.tokens.joinToString("."),
            implMethodName = def.impl.methodName
          )
        }

        is BibixAst.ImportAll -> {
          val importName = def.rename ?: def.source.getDefaultImportName()
          val source = addExpr(def.source, ctx)
          importAlls[ctx.bibixName(importName)] = ImportAllDef(source)
        }

        is BibixAst.ImportFrom -> {
          val importName = def.rename ?: def.importing.tokens.last()
          val source = addExpr(def.source, ctx)
          importFroms[ctx.bibixName(importName)] = ImportFromDef(source, def.importing.tokens)
        }

        is BibixAst.NamespaceDef -> {
          addDefs(def.body, ctx.innerNamespace(def.name), false)
        }

        is BibixAst.TargetDef -> {
          targets[ctx.bibixName(def.name)] = addExpr(def.value, ctx)
        }

        is BibixAst.DataClassDef -> {
          dataClasses[ctx.bibixName(def.name)] =
            DataClassDef(
              def = def,
              fields = def.fields.associate { it.name to addType(it.typ!!, ctx) },
              fieldDefaultValues = def.fields.filter { it.defaultValue != null }
                .associate { it.name to addExpr(it.defaultValue!!, ctx) }
            )
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
          vars[ctx.bibixName(def.name)] = VarDef(
            def = def,
            type = addType(def.typ!!, ctx),
            defaultValue = def.defaultValue?.let { addExpr(it, ctx) }
          )
        }

        is BibixAst.VarRedefs -> {
          def.redefs.forEach { redef ->
            when (val lookupResult = ctx.nameLookupCtx.lookupName(redef.nameTokens)) {
              is NameInImport -> {
                check(isRoot) { "var redef only can be placed in the root. Consider using with statement instead" }
                val redefsCtx =
                  importInstances.getOrPut(lookupResult.importEntry.name) { mutableMapOf() }
                    .getOrPut(0) { VarCtxBuilder(0, mutableMapOf()) }
                val redefsMap = redefsCtx.redefs
                val varName = BibixName(lookupResult.remaining)
                check(varName !in redefsMap) { "Duplicate var redef: $varName" }
                redefsMap[varName] = addExpr(redef.redefValue, ctx)
              }

              else -> throw IllegalStateException()
            }
          }
        }

        is BibixAst.DefsWithVarRedefs -> {
          val contextId = def.nodeId
          val redefed = mutableMapOf<BibixName, Int>()
          def.varRedefs.forEach { redef ->
            when (val lookupResult = ctx.nameLookupCtx.lookupName(redef.nameTokens)) {
              // TODO NameOfPreloadedPlugin 처리
              is NameInImport -> {
                val importName = lookupResult.importEntry.name
                val currentVarCtxId = ctx.importInstances[importName] ?: 0

                val redefsCtx =
                  importInstances.getOrPut(importName) { mutableMapOf() }
                    .getOrPut(contextId) { VarCtxBuilder(currentVarCtxId, mutableMapOf()) }
                val redefsMap = redefsCtx.redefs

                val varName = BibixName(lookupResult.remaining)
                check(varName !in redefsMap) { "Duplicate var redef: $varName" }
                redefsMap[varName] = addExpr(redef.redefValue, ctx)

                // ctx.importInstances의 내용을 반영하도록

                redefed[importName] = contextId
              }

              else -> throw IllegalStateException()
            }
          }

          addDefs(def.defs, ctx.withImportInstances(redefed), false)
        }
      }
    }
  }

  fun addNode(node: ExprGraphNode): ExprNodeId {
    val existing = exprNodes[node.id]
    if (existing != null) {
      check(existing == node)
    }
    exprNodes[node.id] = node
    return node.id
  }

  fun addEdge(start: ExprNodeId, end: ExprNodeId) {
    exprEdges.add(ExprGraphEdge(start, end))
  }

  fun addEdge(start: ExprNodeId, end: TypeNodeId) {
    exprTypeEdges.add(ExprTypeEdge(start, end))
  }

  fun lookupExprName(nameTokens: List<String>, ctx: GraphBuildContext): ExprNodeId {
    val node = when (val lookupResult = ctx.nameLookupCtx.lookupName(nameTokens)) {
      is EnumValueFound ->
        LocalEnumValue(lookupResult.enum.name, lookupResult.enumValueName)

      is NameEntryFound -> {
        when (val entry = lookupResult.entry) {
          is BuildRuleNameEntry -> LocalBuildRuleRef(entry.name, entry.def)
          is DataClassNameEntry -> LocalDataClassRef(entry.name, entry.def)
          is TargetNameEntry -> LocalTargetRef(entry.name, entry.def)
          is VarNameEntry -> LocalVarRef(entry.name, entry.def)
          else -> throw IllegalStateException()
        }
      }

      is NameFromPrelude ->
        ImportedExprFromPrelude(lookupResult.name, lookupResult.remaining)

      is NameOfPreloadedPlugin ->
        ImportedExprFromPreloaded(
          lookupResult.name,
          ctx.importInstances[BibixName(lookupResult.name)] ?: 0,
          BibixName(lookupResult.remaining)
        )

      is NameInImport -> {
        ImportedExpr(
          lookupResult.importEntry.name,
          ctx.importInstances[lookupResult.importEntry.name] ?: 0,
          BibixName(lookupResult.remaining)
        )
      }

      is NameNotFound, is NamespaceFound -> throw NameNotFoundException(nameTokens, null)
    }
    return addNode(node)
  }

  fun addExpr(expr: BibixAst.Expr, ctx: GraphBuildContext): ExprNodeId = when (expr) {
    is BibixAst.CastExpr -> {
      val valueNode = addExpr(expr.expr, ctx)
      val typeNode = addType(expr.castTo, ctx)
      val node = addNode(ValueCastNode(expr, valueNode, typeNode))
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
      val callee = lookupExprName(expr.name.tokens, ctx)
      val posParams = expr.params.posParams.mapIndexed { index, argExpr ->
        val arg = addExpr(argExpr, ctx)
        val coercion =
          addNode(CallExprParamCoercionNode(arg, callee, ParamLocation.PosParam(index)))
        addEdge(coercion, callee)
        addEdge(coercion, arg)
        coercion
      }
      val namedParams = expr.params.namedParams.associate { (name, argExpr) ->
        val arg = addExpr(argExpr, ctx)
        val coercion =
          addNode(CallExprParamCoercionNode(arg, callee, ParamLocation.NamedParam(name)))
        addEdge(coercion, callee)
        addEdge(coercion, arg)
        name to coercion
      }
      val call = addNode(CallExprCallNode(expr, callee, posParams, namedParams))
      addEdge(call, callee)
      posParams.forEach { addEdge(call, it) }
      namedParams.forEach { addEdge(call, it.value) }
      val coercion = addNode(CallExprNode(expr, call, callee))
      addEdge(coercion, call)
      coercion
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
          val cast = addNode(ValueCastNode(elem, value, stringTypeNode))
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
        val access = addNode(MemberAccessNode(expr, target, memberNames))
        addEdge(access, target)
        access
      }
    }

    is BibixAst.NameRef -> lookupExprName(listOf(expr.name), ctx)
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

  fun addNode(node: TypeGraphNode): TypeNodeId {
    val existing = typeNodes[node.id]
    if (existing != null) {
      check(existing == node)
    }
    typeNodes[node.id] = node
    return node.id
  }

  fun addEdge(start: TypeNodeId, end: TypeNodeId) {
    typeEdges.add(TypeGraphEdge(start, end))
  }

  fun addType(type: BibixAst.TypeExpr, ctx: GraphBuildContext): TypeNodeId = when (type) {
    is BibixAst.CollectionType -> {
      when (type.name) {
        "set" -> {
          check(type.typeParams.params.size == 1)
          val elemType = addType(type.typeParams.params.first(), ctx)
          val set = addNode(SetTypeNode(type, elemType))
          addEdge(set, elemType)
          set
        }

        "list" -> {
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
        listOf("none") -> NoneType
        else -> null
      }
      if (basicType != null) {
        addNode(BasicTypeNode(basicType))
      } else {
        // TODO name lookup해서 로컬에서 알고 있는 data, super, enum 이면 LocalDataClassTypeRef, LocalSuperClassTypeRef, LocalEnumTypeRef
        //      import하는 이름이면 ImportedType으로 반환
        val node: TypeGraphNode =
          when (val lookupResult = ctx.nameLookupCtx.lookupName(type.tokens, type)) {
            is NameEntryFound -> {
              when (val entry = lookupResult.entry) {
                is DataClassNameEntry -> LocalDataClassTypeRef(entry.name, entry.def)
                is SuperClassNameEntry -> LocalSuperClassTypeRef(entry.name, entry.def)
                is EnumNameEntry -> LocalEnumTypeRef(entry.name, entry.def)
                else -> TODO()
              }
            }

            is NameFromPrelude -> {
              ImportedTypeFromPrelude(lookupResult.name, lookupResult.remaining)
            }

            is NameOfPreloadedPlugin -> {
              ImportedTypeFromPreloaded(lookupResult.name, BibixName(lookupResult.remaining))
            }

            is NameInImport -> {
              ImportedType(
                lookupResult.importEntry.name,
                ctx.importInstances[lookupResult.importEntry.name] ?: 0,
                BibixName(lookupResult.remaining)
              )
            }

            else -> throw NameNotFoundException(type.tokens, type)
          }
        addNode(node)
      }
    }

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
    importAlls = importAlls,
    importFroms = importFroms,
    varRedefs = importInstances.mapValues { (_, ctxMap) ->
      ctxMap.mapValues { (_, ctx) ->
        BuildGraph.VarCtx(ctx.parentCtxId, ctx.redefs)
      }
    },
    exprGraph = ExprGraph(exprNodes, exprEdges),
    typeGraph = TypeGraph(typeNodes, typeEdges),
    exprTypeEdges = exprTypeEdges
  )
}

data class GraphBuildContext(
  val nameLookupCtx: NameLookupContext,
  val importInstances: Map<BibixName, Int>,
  val thisRefAllowed: Boolean,
  val nativeAllowed: Boolean,
) {
  fun innerNamespace(namespaceName: String) =
    copy(nameLookupCtx = nameLookupCtx.innerNamespace(namespaceName))

  fun withThisRefAllowed() =
    copy(thisRefAllowed = true)

  fun bibixName(name: String) = BibixName(nameLookupCtx.currentScope.namePath + name)

  fun withImportInstances(importInstances: Map<BibixName, Int>): GraphBuildContext =
    copy(importInstances = this.importInstances + importInstances)
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
    else -> Pair(this, listOf(this.name))
  }

fun BibixAst.ImportDef.importName() = when (this) {
  is BibixAst.ImportAll -> this.rename ?: this.source.getDefaultImportName()
  is BibixAst.ImportFrom -> this.rename ?: this.importing.tokens.last()
}
