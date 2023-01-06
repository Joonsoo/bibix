package com.giyeok.bibix.interpreter.name

import com.giyeok.bibix.ast.BibixAst
import com.giyeok.bibix.base.CName
import com.giyeok.bibix.base.SourceId
import com.giyeok.bibix.utils.getOrNull
import com.giyeok.bibix.utils.toKtList

class NameLookupTable {
  fun add(sourceId: SourceId, script: BibixAst.BuildScript) {
    // script가 sourceId라는 이름으로 등록됨
    val packageName = script.packageName().getOrNull()
    if (packageName != null) {
      // TODO remember that sourceId has the package name
    }

    fun traverse(defs: List<BibixAst.Def>, scopePath: List<String>) {
      defs.forEach { def ->
        when (def) {
          is BibixAst.ArgDef -> TODO()
          is BibixAst.ArgRedef -> TODO()
          is BibixAst.NameDef -> TODO()
          is BibixAst.NamespaceDef -> {
            check(def.body().packageName().isEmpty) { "namespace cannot have package name" }
            traverse(def.body().defs().toKtList(), scopePath + def.name())
          }

          is BibixAst.EnumDef -> TODO()
          is BibixAst.ImportDef -> when (def) {
            is BibixAst.ImportName -> TODO()
            is BibixAst.ImportAll -> TODO()
            is BibixAst.ImportFrom -> TODO()
            else -> TODO()
          }

          is BibixAst.ActionRuleDef -> TODO()
          is BibixAst.ActionDef -> TODO()
          is BibixAst.BuildRuleDef -> TODO()
          is BibixAst.ClassDef -> when (def) {
            is BibixAst.SuperClassDef -> TODO()
            is BibixAst.DataClassDef -> TODO()
            else -> TODO()
          }

          else -> {}
        }
      }
    }
    traverse(script.defs().toKtList(), listOf())
  }

  fun lookup(context: NameLookupContext, name: List<String>): CName? {
    TODO()
  }
}

// sourceId 스크립트에서 namespace path 밑에서 이름 검색중
// rule impl에서 요청한 name lookup인 경우, rule impl target이 정의된 위치로 지정
data class NameLookupContext(val sourceId: SourceId, val scopePath: List<String>)
