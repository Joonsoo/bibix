package com.giyeok.bibix.interpreter.expr

import com.giyeok.bibix.ast.BibixAst
import com.giyeok.bibix.base.CName
import com.giyeok.bibix.base.PreludeSourceId
import com.giyeok.bibix.base.SourceId
import com.giyeok.bibix.utils.getOrNull
import com.giyeok.bibix.utils.toKtList
import com.google.common.annotations.VisibleForTesting

class NameLookupTable(private val varsManager: VarsManager) {
  @VisibleForTesting
  val definitions = mutableMapOf<CName, Definition>()

  // 메인 스크립트에서 "jparser"란 이름으로 임포트한 것은 RemoteSourceId(1)에서 "asdf"
  // CName(MainSourceId, "jparser") -> NameLookupContext(CName(RemoteSourceId(1), "asdf"), [])
  // source A의 import def expr id X를 임포트하면 source B가 된다 -> imports[(A, X)] = B
  @VisibleForTesting
  val imports = mutableMapOf<CName, ImportedSource>()

  private fun addDefinition(cname: CName, definition: Definition) {
    // TODO check duplicate
    definitions[cname] = definition
  }

  fun add(context: NameLookupContext, defs: List<BibixAst.Def>) {
    fun traverse(scope: List<String>, defs: List<BibixAst.Def>) {
      defs.forEach { def ->
        when (def) {
          is BibixAst.ImportDef -> {
            val name = def.scopeName()
            val cname = CName(context.sourceId, scope + name)
            addDefinition(cname, Definition.ImportDef(cname, def))
          }

          is BibixAst.NamespaceDef -> {
            check(def.body().packageName().isEmpty) { "namespace cannot have package name" }
            val cname = CName(context.sourceId, scope + def.name())
            addDefinition(cname, Definition.NamespaceDef(cname))
            traverse(scope + def.name(), def.body().defs().toKtList())
          }

          is BibixAst.TargetDef -> {
            val cname = CName(context.sourceId, scope + def.name())
            addDefinition(cname, Definition.TargetDef(cname, def))
          }

          is BibixAst.ActionDef -> {
            val cname = CName(context.sourceId, scope + def.name())
            addDefinition(cname, Definition.ActionDef(cname, def))
          }

          is BibixAst.DataClassDef -> {
            val cname = CName(context.sourceId, scope + def.name())
            addDefinition(cname, Definition.ClassDef(cname, def))
          }

          is BibixAst.SuperClassDef -> {
            val cname = CName(context.sourceId, scope + def.name())
            addDefinition(cname, Definition.ClassDef(cname, def))
          }

          is BibixAst.EnumDef -> {
            val cname = CName(context.sourceId, scope + def.name())
            addDefinition(cname, Definition.EnumDef(cname, def))
          }

          is BibixAst.VarDef -> {
            check(scope.isEmpty()) { "var must be in the root scope of the script" }
            val cname = CName(context.sourceId, def.name())
            val varContext = NameLookupContext(context.sourceId, scope)
            varsManager.addVarDef(cname, varContext, def)
            addDefinition(cname, Definition.VarDef(cname, def))
          }

          is BibixAst.VarRedef -> {
            val redefContext = NameLookupContext(context.sourceId, scope)
            varsManager.addVarRedef(redefContext, def)
          }

          is BibixAst.BuildRuleDef -> {
            val cname = CName(context.sourceId, scope + def.name())
            addDefinition(cname, Definition.BuildRule(cname, def))
          }

          is BibixAst.ActionRuleDef -> {
            val cname = CName(context.sourceId, scope + def.name())
            addDefinition(cname, Definition.ActionRule(cname, def))
          }

          else -> {}
        }
      }
    }
    traverse(context.scopePath, defs)
  }

  fun addImport(importCName: CName, context: NameLookupContext) {
    imports[importCName] = ImportedSource.ImportedNames(context)
  }

  fun addImport(importCName: CName, definition: Definition) {
    imports[importCName] = ImportedSource.ImportedDefinition(definition)
  }

  fun isImported(sourceId: SourceId, name: String): Boolean =
    imports.containsKey(CName(sourceId, listOf(name)))

  fun lookup(context: NameLookupContext, name: List<String>): LookupResult {
    fun findFirstToken(firstToken: String): Definition? {
      val scopePath = context.scopePath.toMutableList()
      while (true) {
        val def = definitions[CName(context.sourceId, scopePath + firstToken)]
        if (def != null) return def
        if (scopePath.isEmpty()) break
        scopePath.removeAt(scopePath.size - 1)
      }
      // prelude에서는 마지막에 찾는다
      return definitions[CName(PreludeSourceId, firstToken)]
    }

    check(name.isNotEmpty())
    val firstDefinition = findFirstToken(name.first())
      ?: return LookupResult.NameNotFound
    if (firstDefinition is Definition.ImportDef) {
      val imported = imports[firstDefinition.cname]
        ?: return LookupResult.ImportRequired(firstDefinition, name.drop(1))
      return when (imported) {
        is ImportedSource.ImportedNames ->
          if (name.size > 1) {
            lookup(imported.nameLookupContext, name.drop(1))
          } else {
            LookupResult.DefinitionFound(Definition.NamespaceDef(imported.nameLookupContext.toCName()))
          }

        is ImportedSource.ImportedDefinition ->
          LookupResult.DefinitionFound(imported.definition)
      }
    }
    return if (name.size == 1) {
      LookupResult.DefinitionFound(firstDefinition)
    } else {
      val finalDefinition = definitions[firstDefinition.cname.append(name.drop(1))]
      if (finalDefinition != null) {
        LookupResult.DefinitionFound(finalDefinition)
      } else {
        LookupResult.NameNotFound
      }
    }
  }
}

// sourceId 스크립트에서 namespace path 밑에서 이름 검색중
// rule impl에서 요청한 name lookup인 경우, rule impl target이 정의된 위치로 지정
data class NameLookupContext(val sourceId: SourceId, val scopePath: List<String>) {
  constructor(cname: CName) : this(cname.sourceId, cname.tokens)

  fun toCName(): CName = CName(sourceId, scopePath)

  fun dropLastToken(): NameLookupContext = NameLookupContext(sourceId, scopePath.dropLast(1))
}

sealed class Definition {
  abstract val cname: CName

  data class ImportDef(override val cname: CName, val import: BibixAst.ImportDef) : Definition()
  data class NamespaceDef(override val cname: CName) : Definition()
  data class TargetDef(override val cname: CName, val target: BibixAst.TargetDef) : Definition()
  data class ActionDef(override val cname: CName, val action: BibixAst.ActionDef) : Definition()
  data class ClassDef(override val cname: CName, val classDef: BibixAst.ClassDef) : Definition()
  data class EnumDef(override val cname: CName, val enumDef: BibixAst.EnumDef) : Definition()
  data class VarDef(override val cname: CName, val varDef: BibixAst.VarDef) : Definition()
  data class VarRedef(override val cname: CName, val varRedef: BibixAst.VarRedef) : Definition()

  data class BuildRule(override val cname: CName, val buildRule: BibixAst.BuildRuleDef) :
    Definition()

  data class ActionRule(override val cname: CName, val actionRule: BibixAst.ActionRuleDef) :
    Definition()
}

sealed class LookupResult {
  data class DefinitionFound(val definition: Definition) : LookupResult()
  data class ImportRequired(val import: Definition.ImportDef, val restName: List<String>) :
    LookupResult()

  object NameNotFound : LookupResult()
}

sealed class ImportedSource {
  data class ImportedNames(val nameLookupContext: NameLookupContext) : ImportedSource()
  data class ImportedDefinition(val definition: Definition) : ImportedSource()
}

fun BibixAst.ImportDef.scopeName(): String = when (this) {
  is BibixAst.ImportAll -> {
    val defaultName = when (val importSource = this.source()) {
      is BibixAst.NameRef -> importSource.name()
      is BibixAst.MemberAccess -> importSource.name()
      else -> null
    }
    (this.rename().getOrNull() ?: defaultName)
      ?: throw IllegalStateException("Cannot infer the default name for import $this")
  }

  is BibixAst.ImportFrom ->
    this.rename().getOrNull() ?: this.importing().tokens().last()

  else -> throw AssertionError()
}
