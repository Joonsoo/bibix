package com.giyeok.bibix.graph

import com.giyeok.bibix.ast.BibixAst

class NameLookupTable(
  val namespaces: Map<String, NameLookupTable>,
  val names: Map<String, NameEntry>
) {
  companion object {
    fun fromScript(script: BibixAst.BuildScript): NameLookupTable =
      Builder(mutableMapOf(), mutableMapOf()).addDefs(script.defs).build()
  }

  class Builder(
    val namespaces: MutableMap<String, NameLookupTable>,
    val names: MutableMap<String, NameEntry>
  ) {
    fun addName(name: String, entry: NameEntry) {
      check(name !in names) { "Duplicate name: \"$name\" at ${entry.def.start}..${entry.def.end}" }
      check(name !in namespaces) { "Duplicate name: \"$name\" at ${entry.def.start}..${entry.def.end}" }
      names[name] = entry
    }

    fun addDefs(defs: List<BibixAst.Def>): Builder {
      defs.forEach { def ->
        when (def) {
          is BibixAst.ImportAll -> {
            val name = def.rename ?: when (val source = def.source) {
              is BibixAst.NameRef -> source.name
              is BibixAst.MemberAccess -> source.name
              else -> throw IllegalStateException("Cannot infer the default name for import $def")
            }
            addName(name, ImportNameEntry(def))
          }

          is BibixAst.ImportFrom -> {
            val name = def.rename ?: def.importing.tokens.last()
            addName(name, ImportNameEntry(def))
          }

          is BibixAst.NamespaceDef -> {
            val namespace = Builder(mutableMapOf(), mutableMapOf()).addDefs(def.body).build()
            namespaces[def.name] = namespace
          }

          is BibixAst.TargetDef -> addName(def.name, TargetNameEntry(def))
          is BibixAst.ActionDef -> addName(def.name, ActionNameEntry(def))
          is BibixAst.DataClassDef -> addName(def.name, ClassNameEntry(def))
          is BibixAst.SuperClassDef -> addName(def.name, ClassNameEntry(def))
          is BibixAst.EnumDef -> addName(def.name, EnumNameEntry(def))
          is BibixAst.VarDef -> addName(def.name, VarNameEntry(def))
          is BibixAst.VarRedefs -> Unit // do nothing
          is BibixAst.BuildRuleDef -> addName(def.name, BuildRuleNameEntry(def))
          is BibixAst.ActionRuleDef -> addName(def.name, ActionRuleNameEntry(def))
          is BibixAst.DefsWithVarRedefs -> addDefs(def.defs)
        }
      }
      return this
    }

    fun build(): NameLookupTable = NameLookupTable(namespaces, names)
  }

  fun containsName(name: String) = name in names || name in namespaces

  fun lookupName(tokens: List<String>, nameNode: BibixAst.AstNode? = null): NameLookupResult {
    check(tokens.isNotEmpty())
    val firstToken = tokens.first()
    val nameEntry = names[firstToken]
    if (nameEntry != null) {
      return if (nameEntry is ImportNameEntry) {
        NameInImport(nameEntry, tokens.drop(1))
      } else {
        if (tokens.size != 1) {
          return NameNotFound(tokens, nameNode)
        }
        NameEntryFound(nameEntry)
      }
    }
    val ns = namespaces[firstToken]
    if (ns != null) {
      return if (tokens.size == 1) {
        NamespaceFound(tokens.joinToString("."), nameNode)
      } else {
        ns.lookupName(tokens.drop(1), nameNode)
      }
    }
    return NameNotFound(tokens, nameNode)
  }
}

data class NameNotFoundException(
  val nameTokens: List<String>,
  val nameNode: BibixAst.AstNode?
): Exception(
  if (nameNode == null) {
    "Name not found: ${nameTokens.joinToString(".")} at ${nameNode}"
  } else {
    "Name not found: ${nameTokens.joinToString(".")} at ${nameNode.start}..${nameNode.end}"
  }
) {
  constructor(result: NameNotFound): this(result.nameTokens, result.nameNode)
}
