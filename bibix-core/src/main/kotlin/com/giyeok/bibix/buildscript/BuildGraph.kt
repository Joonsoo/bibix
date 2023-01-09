package com.giyeok.bibix.buildscript

import com.giyeok.bibix.BibixIdProto
import com.giyeok.bibix.ast.BibixAst
import com.giyeok.bibix.base.CName
import com.giyeok.bibix.base.SourceId
import com.giyeok.bibix.interpreter.*
import com.giyeok.bibix.runner.CNameValue
import com.giyeok.bibix.runner.Param
import com.giyeok.bibix.utils.getOrNull
import com.giyeok.bibix.utils.toKtList
import java.nio.file.Path

class BuildGraph(
  val names: MutableMap<CName, CNameValue> = mutableMapOf(),
  val deferredImports: Registry.Builder<DeferredImportDef> = Registry.Builder(),
  val exprGraphs: Registry.Builder<ExprGraph> = Registry.Builder(),
  val loadedSources: MutableSet<SourceId> = mutableSetOf(),
  val remoteSources: Registry.Builder<BibixIdProto.RemoteSourceId> = Registry.Builder(),
  val baseDirectories: MutableMap<SourceId, Path> = mutableMapOf(),
  // TODO class hierarchy 정보 추가
) {
  fun addRemoteSource(remoteSourceId: BibixIdProto.RemoteSourceId): Int =
    // TODO 이미 remoteSources에 있으면?
    remoteSources.register(remoteSourceId)

  fun addDefs(sourceId: SourceId, defs: List<BibixAst.Def>, baseDirectory: Path) {
    addDefs(sourceId, defs, NameLookupContext(CName(sourceId), defs), baseDirectory)
  }

  fun addDefs(
    sourceId: SourceId,
    defs: List<BibixAst.Def>,
    lookup: NameLookupContext,
    baseDirectory: Path
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
//            is BibixAst.ImportName -> {
//              val nameTokens = def.name().tokens().toKtList()
//              val rename = def.rename().getOrNull()
//              if (nameTokens.first() == "main") {
//                check(nameTokens.size >= 2)
//                Pair(
//                  DeferredImportDef.ImportMainSub(nameTokens.drop(1)),
//                  rename ?: nameTokens.last()
//                )
//              } else {
//                Pair(
//                  DeferredImportDef.ImportDefaultPlugin(nameTokens),
//                  rename ?: nameTokens.last()
//                )
//              }
//            }

            is BibixAst.ImportAll -> {
              Pair(
                DeferredImportDef.ImportAll(
                  traverseImportSource(
                    cname.sourceId,
                    def.source(),
                    lookup
                  )
                ),
                def.rename().getOrNull() ?: "???"
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

        is BibixAst.TargetDef -> {
          registerName(
            cname.append(def.name()),
            CNameValue.ExprValue(exprGraphs.register(traverseExpr(def.value(), lookup)))
          )
        }

        is BibixAst.DataClassDef -> {
          val className = cname.append(def.name())
          val fields = def.fields().toKtList().map { field ->
//            CNameValue.ClassField(
//              field.name(),
//              traverseTypeExpr(field.typ(), lookup),
//              field.optional()
//            )
            TODO()
          }
          val casts = def.body().toKtList().filterIsInstance<BibixAst.ClassCastDef>()
            .associate { cast ->
              traverseTypeExpr(cast.castTo(), lookup) to
                exprGraphs.register(
                  traverseExpr(cast.expr(), lookup, className)
                )
            }
          registerName(className, CNameValue.DataClassType(className, fields, casts))
        }

        is BibixAst.SuperClassDef -> {
          val className = cname.append(def.name())
//          val subs = def.subs().toKtList().map { CustomType(lookup.findName(it)) }
//
//          registerName(className, CNameValue.SuperClassType(className, subs))
          TODO()
        }

        is BibixAst.EnumDef -> {
          val enumName = cname.append(def.name())
          registerName(
            enumName,
            CNameValue.EnumType(enumName, def.values().toKtList())
          )
        }

        is BibixAst.VarDef -> {
          registerName(
            cname.append(def.name()),
            CNameValue.ArgVar(
              traverseTypeExpr(def.typ().getOrNull()!!, lookup),
              def.defaultValue().getOrNull()?.let { exprGraphs.register(traverseExpr(it, lookup)) }
            )
          )
        }

        is BibixAst.BuildRuleDef -> {
          registerName(
            cname.append(def.name()),
            CNameValue.BuildRuleValue(
              cname.append(def.name()),
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
              cname.append(def.name()),
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
    importSource: BibixAst.Expr,
    lookup: NameLookupContext
  ): ImportSource =
    when (importSource) {
      is BibixAst.StringLiteral ->
        ImportSource.ImportSourceString(
          sourceId,
          exprGraphs.register(traverseExpr(importSource, lookup))
        )

      is BibixAst.CallExpr -> {
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
  ): ExprGraph = ExprGraph.fromExpr(expr, lookup, thisClassTypeName, this::traverseTypeExpr)

  private fun traverseTypeExpr(typeExpr: BibixAst.TypeExpr, lookup: NameLookupContext): BibixType =
    when (typeExpr) {
      is BibixAst.Name -> {
        when (val name = typeExpr.tokens().toKtList()) {
          listOf("boolean") -> BooleanType
          listOf("string") -> StringType
          listOf("file") -> FileType
          listOf("directory") -> DirectoryType
          listOf("path") -> PathType
          listOf("buildrule") -> BuildRuleDefType
          listOf("actionrule") -> ActionRuleDefType
          listOf("type") -> TypeType
          else -> TODO() // CustomType(lookup.findName(name))
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

      is BibixAst.NoneType -> NoneType
      else -> throw AssertionError()
    }

  private fun reifyParam(param: BibixAst.ParamDef, lookup: NameLookupContext): Param = Param(
    param.name(),
    param.optional(),
    traverseTypeExpr(param.typ().getOrNull()!!, lookup),
    param.defaultValue().getOrNull()?.let { exprGraphs.register(traverseExpr(it, lookup)) })
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
