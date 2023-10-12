package com.giyeok.bibix.graph

import com.giyeok.bibix.ast.BibixAst

data class NameLookupContext(
  val table: NameLookupTable,
  val preludeNames: Set<String>,
  val currentScope: ScopedNameLookupTable
) {
  fun innerNamespace(namespaceName: String): NameLookupContext {
    // 현재 namespace scope 안에 `namespaceName`가 존재하는지 확인
    val innerScope = currentScope.table.namespaces[namespaceName]
    checkNotNull(innerScope)
    return NameLookupContext(
      table,
      preludeNames,
      ScopedNameLookupTable(currentScope.namePath + namespaceName, innerScope, currentScope)
    )
  }

  fun lookupName(tokens: List<String>, nameNode: BibixAst.AstNode? = null): NameLookupResult {
    check(tokens.isNotEmpty())
    var current: ScopedNameLookupTable? = currentScope
    val firstToken = tokens.first()
    while (current != null && !current.table.containsName(firstToken)) {
      current = current.parent
    }
    if (current == null) {
      // 가장 상위 scope까지 확인해 봤는데도 이름이 없으면 prelude 이름에서 찾아봄
      if (tokens.size == 1 && preludeNames.contains(tokens.first())) {
        return NameFromPrelude(tokens.first())
      } else {
        throw NameNotFoundException(tokens, nameNode)
      }
    }
    return current.table.lookupName(tokens, nameNode)
  }

  fun lookupName(name: BibixAst.Name): NameLookupResult =
    lookupName(name.tokens, name)

  fun lookupName(name: BibixAst.NameRef): NameLookupResult =
    lookupName(listOf(name.name), name)
}

data class ScopedNameLookupTable(
  val namePath: List<String>,
  val table: NameLookupTable,
  val parent: ScopedNameLookupTable?
)