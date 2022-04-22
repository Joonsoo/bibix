package com.giyeok.bibix.buildscript

import com.giyeok.bibix.ast.BibixAst
import com.giyeok.bibix.base.*
import com.giyeok.bibix.runner.BibixIdProto
import com.giyeok.bibix.utils.getOrNull
import com.giyeok.bibix.utils.toKtList
import java.io.File

class BuildGraph(
  val names: MutableMap<CName, CNameValue> = mutableMapOf(),
  val deferredImports: Registry.Builder<DeferredImportDef> = Registry.Builder(),
  val exprGraphs: Registry.Builder<ExprGraph> = Registry.Builder(),
  val loadedSources: MutableSet<SourceId> = mutableSetOf(),
  val remoteSources: Registry.Builder<BibixIdProto.RemoteSourceId> = Registry.Builder(),
  val baseDirectories: MutableMap<SourceId, File> = mutableMapOf(),
) {
  fun addRemoteSource(remoteSourceId: BibixIdProto.RemoteSourceId): Int =
    // TODO 이미 remoteSources에 있으면?
    remoteSources.register(remoteSourceId)

  fun addDefs(sourceId: SourceId, defs: List<BibixAst.Def>, baseDirectory: File) {
    addDefs(sourceId, defs, NameLookupContext(CName(sourceId), defs), baseDirectory)
  }

  fun addDefs(
    sourceId: SourceId,
    defs: List<BibixAst.Def>,
    lookup: NameLookupContext,
    baseDirectory: File
  ) {
    synchronized(this) {
      if (!loadedSources.contains(sourceId)) {
        loadedSources.add(sourceId)
        baseDirectories[sourceId] = baseDirectory
        addDefs(CName(sourceId), defs, lookup)
      }
    }
  }

  fun addDef(cname: CName, value: CNameValue) {
    synchronized(this) {
      registerName(cname, value)
    }
  }

  private fun addDefs(cname: CName, defs: List<BibixAst.Def>, lookup: NameLookupContext) {
    defs.forEach { def ->
      when (def) {
        is BibixAst.NamespaceDef -> {
          val subname = cname.append(def.name())
          val elems = def.body().defs().toKtList()
          val sublookup = lookup.append(subname, elems)
          addDefs(subname, elems, sublookup)
          names[subname] = CNameValue.NamespaceValue(subname, sublookup.chain.scope.names)
        }
        is BibixAst.ImportDef -> {
          val (importDef: DeferredImportDef, importName) = when (def) {
            is BibixAst.ImportName -> {
              val nameTokens = def.name().tokens().toKtList()
              val rename = def.rename().getOrNull()
              if (nameTokens.first() == "main") {
                check(nameTokens.size >= 2)
                Pair(
                  DeferredImportDef.ImportMainSub(nameTokens.drop(1)),
                  rename ?: nameTokens.last()
                )
              } else {
                Pair(
                  DeferredImportDef.ImportDefaultPlugin(nameTokens),
                  rename ?: nameTokens.last()
                )
              }
            }
            is BibixAst.ImportAll -> {
              Pair(
                DeferredImportDef.ImportAll(
                  traverseImportSource(
                    cname.sourceId,
                    def.source(),
                    lookup
                  )
                ),
                def.rename()
              )
            }
            is BibixAst.ImportFrom -> {
              val nameTokens = def.importing().tokens().toKtList()
              Pair(
                DeferredImportDef.ImportFrom(
                  traverseImportSource(cname.sourceId, def.source(), lookup),
                  nameTokens,
                ),
                def.rename().getOrNull() ?: nameTokens.last()
              )
            }
            else -> throw AssertionError()
          }
          registerName(
            cname.append(importName),
            CNameValue.DeferredImport(deferredImports.register(importDef))
          )
        }
        is BibixAst.NameDef -> {
          registerName(
            cname.append(def.name()),
            CNameValue.ExprValue(exprGraphs.register(traverseExpr(def.value(), lookup)))
          )
        }
        is BibixAst.ClassDef -> {
          val className = cname.append(def.name())
          val typeParams = def.typeParams().toKtList().map { tp ->
            tp.name()!! to traverseTypeExpr(tp.typ(), lookup)
          }
          val extendings = def.extendings().toKtList().map { ex -> lookup.findName(ex.name()) }
          val reality = traverseTypeExpr(def.reality(), lookup)
          val casts = def.body().toKtList().filterIsInstance<BibixAst.ClassCastDef>()
            .associate { cast ->
              lookup.findName(cast.castTo()) to
                exprGraphs.register(
                  traverseExpr(cast.expr(), lookup, className)
                )
            }
          registerName(
            className,
            CNameValue.ClassType(className, typeParams, extendings, reality, casts)
          )
        }
        is BibixAst.EnumDef -> {
          val enumName = cname.append(def.name())
          registerName(
            enumName,
            CNameValue.EnumType(enumName, def.values().toKtList())
          )
        }
        is BibixAst.ArgDef -> {
          registerName(
            cname.append(def.name()),
            CNameValue.ArgVar(
              def.replacing().getOrNull()?.let { lookup.findName(it) },
              traverseTypeExpr(def.typ().getOrNull()!!, lookup),
              def.defaultValue().getOrNull()?.let { exprGraphs.register(traverseExpr(it, lookup)) }
            )
          )
        }
        is BibixAst.BuildRuleDef -> {
          registerName(
            cname.append(def.name()),
            CNameValue.BuildRuleValue(
              def.params().toKtList().map { reifyParam(it, lookup) },
              lookup.findName(def.impl().targetName()),
              def.impl().className().tokens().mkString("."),
              def.impl().methodName().getOrNull(),
              traverseTypeExpr(def.returnType(), lookup)
            )
          )
        }
        is BibixAst.ActionRuleDef -> {
          registerName(
            cname.append(def.name()),
            CNameValue.ActionRuleValue(
              def.params().toKtList().map { reifyParam(it, lookup) },
              lookup.findName(def.impl().targetName()),
              def.impl().className().tokens().mkString("."),
              def.impl().methodName().getOrNull(),
            )
          )
        }
        is BibixAst.ActionDef -> {
          val argsName = def.argsName().getOrNull()
          val lookup1 = if (argsName == null) lookup else lookup.withArgs(argsName)
          registerName(
            cname.append(def.name()),
            CNameValue.ActionCallValue(exprGraphs.register(traverseExpr(def.expr(), lookup1)))
          )
        }
        else -> throw AssertionError()
      }
    }
  }

  private fun registerName(cname: CName, cnameValue: CNameValue) {
    check(!names.containsKey(cname)) { "Duplicate name: $cname" }
    names[cname] = cnameValue
  }

  private fun traverseImportSource(
    sourceId: SourceId,
    importSource: BibixAst.ImportSourceExpr,
    lookup: NameLookupContext
  ): ImportSource =
    when (importSource) {
      is BibixAst.StringLiteral ->
        ImportSource.ImportSourceString(
          sourceId,
          exprGraphs.register(traverseExpr(importSource, lookup))
        )
      is BibixAst.CallExpr -> {
        check(importSource.name().tokens().size() == 1)
        ImportSource.ImportSourceCall(
          sourceId,
          exprGraphs.register(traverseExpr(importSource, lookup))
        )
      }
      else -> throw AssertionError()
    }

  private fun traverseExpr(
    expr: BibixAst.Expr,
    lookup: NameLookupContext,
    thisClassTypeName: CName? = null
  ): ExprGraph = ExprGraph.fromExpr(expr, lookup, thisClassTypeName)

  private fun traverseTypeExpr(typeExpr: BibixAst.TypeExpr, lookup: NameLookupContext): BibixType =
    when (typeExpr) {
      is BibixAst.Name -> {
        when (val name = typeExpr.tokens().toKtList()) {
          listOf("boolean") -> BooleanType
          listOf("string") -> StringType
          listOf("file") -> FileType
          listOf("directory") -> DirectoryType
          listOf("path") -> PathType
          else -> CustomType(lookup.findName(name))
        }
      }
      is BibixAst.CollectionType ->
        when (typeExpr.name()) {
          "list" -> {
            check(typeExpr.typeParams().params().size() == 1)
            val typeParam = typeExpr.typeParams().params().head()
            ListType(traverseTypeExpr(typeParam, lookup))
          }
          "set" -> {
            check(typeExpr.typeParams().params().size() == 1)
            val typeParam = typeExpr.typeParams().params().head()
            SetType(traverseTypeExpr(typeParam, lookup))
          }
          else ->
            throw IllegalArgumentException("Unknown type: ${typeExpr.parseNode().sourceText()}")
        }
      is BibixAst.TupleType ->
        TupleType(typeExpr.elems().toKtList().map { traverseTypeExpr(it, lookup) })
      is BibixAst.NamedTupleType ->
        NamedTupleType(typeExpr.elems().toKtList().map {
          it.name() to traverseTypeExpr(it.typ(), lookup)
        })
      is BibixAst.UnionType -> UnionType(
        typeExpr.elems().toKtList().map { traverseTypeExpr(it, lookup) })
      else -> throw AssertionError()
    }

  private fun reifyParam(param: BibixAst.ParamDef, lookup: NameLookupContext): Param = Param(
    param.name(),
    param.optional(),
    traverseTypeExpr(param.typ().getOrNull()!!, lookup),
    param.defaultValue().getOrNull()?.let { exprGraphs.register(traverseExpr(it, lookup)) })

  fun replaceValue(cname: CName, result: CNameValue) {
    names[cname] = result
  }
}

sealed class DeferredImportDef {
  data class ImportDefaultPlugin(val nameTokens: List<String>) : DeferredImportDef()
  data class ImportMainSub(val nameTokens: List<String>) : DeferredImportDef()
  data class ImportAll(val importSource: ImportSource) : DeferredImportDef()
  data class ImportFrom(val importSource: ImportSource, val nameTokens: List<String>) :
    DeferredImportDef()
}

sealed class ImportSource {
  data class ImportSourceString(val origin: SourceId, val stringLiteralExprId: Int) :
    ImportSource()

  data class ImportSourceCall(val origin: SourceId, val sourceExprId: Int) : ImportSource()
}
