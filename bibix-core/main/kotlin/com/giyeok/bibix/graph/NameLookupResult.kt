package com.giyeok.bibix.graph

import com.giyeok.bibix.ast.BibixAst

sealed class NameLookupResult

data class NameEntryFound(val entry: NameEntry): NameLookupResult()

data class EnumValueFound(val enum: EnumNameEntry, val enumValueName: String): NameLookupResult()

data class NameFromPrelude(val name: BibixName): NameLookupResult()

data class NameInImport(
  val importEntry: ImportNameEntry,
  val remaining: List<String>
): NameLookupResult()

data class TargetMemberName(
  val targetEntry: TargetNameEntry,
  val remaining: List<String>
): NameLookupResult()

data class NamespaceFound(val name: String, val context: BibixAst.AstNode?): NameLookupResult()

data class NameNotFound(val nameTokens: List<String>, val nameNode: BibixAst.AstNode?):
  NameLookupResult()
