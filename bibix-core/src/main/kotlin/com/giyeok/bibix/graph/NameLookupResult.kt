package com.giyeok.bibix.graph

import com.giyeok.bibix.ast.BibixAst

sealed class NameLookupResult
data class NameEntryFound(val entry: NameEntry): NameLookupResult()
data class NameOfPreloadedPlugin(val name: String, val isPrelude: Boolean): NameLookupResult()
data class NameFromPrelude(val name: String): NameLookupResult()
data class NameInImport(val importEntry: NameEntry, val remaining: List<String>): NameLookupResult()
data class NamespaceFound(val name: String, val context: BibixAst.AstNode?): NameLookupResult()
