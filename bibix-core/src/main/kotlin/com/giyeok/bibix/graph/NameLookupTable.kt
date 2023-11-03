package com.giyeok.bibix.graph

import com.giyeok.bibix.ast.BibixAst

class NameLookupTable(
  val namespaces: Map<String, NameLookupTable>,
  val names: Map<String, NameEntry>
) {
  companion object {
    fun fromScript(script: BibixAst.BuildScript): NameLookupTable =
      fromDefs(script.defs)

    fun fromDefs(defs: List<BibixAst.Def>): NameLookupTable =
      Builder(listOf(), mutableMapOf(), mutableMapOf()).addDefs(defs).build()
  }

  class Builder(
    val nameCtx: List<String>,
    val namespaces: MutableMap<String, NameLookupTable>,
    val names: MutableMap<String, NameEntry>
  ) {
    fun addName(name: String, entry: NameEntry) {
      check(name !in names) { "Duplicate name: \"$name\" at ${entry.def.start}..${entry.def.end}" }
      check(name !in namespaces) { "Duplicate name: \"$name\" at ${entry.def.start}..${entry.def.end}" }
      names[name] = entry
    }

    private fun bibixName(name: String) = BibixName(nameCtx + name)

    fun addDefs(defs: List<BibixAst.Def>): Builder {
      defs.forEach { def ->
        when (def) {
          is BibixAst.ImportAll -> {
            val name = def.importName()
            addName(name, ImportNameEntry(bibixName(name), def))
          }

          is BibixAst.ImportFrom -> {
            val name = def.importName()
            addName(name, ImportNameEntry(bibixName(name), def))
          }

          is BibixAst.NamespaceDef -> {
            val namespace =
              Builder(nameCtx + def.name, mutableMapOf(), mutableMapOf()).addDefs(def.body).build()
            namespaces[def.name] = namespace
          }

          is BibixAst.TargetDef -> addName(def.name, TargetNameEntry(bibixName(def.name), def))
          is BibixAst.ActionDef -> addName(def.name, ActionNameEntry(bibixName(def.name), def))
          is BibixAst.DataClassDef -> addName(
            def.name,
            DataClassNameEntry(bibixName(def.name), def)
          )

          is BibixAst.SuperClassDef -> addName(
            def.name,
            SuperClassNameEntry(bibixName(def.name), def)
          )

          is BibixAst.EnumDef -> addName(def.name, EnumNameEntry(bibixName(def.name), def))
          is BibixAst.VarDef -> addName(def.name, VarNameEntry(bibixName(def.name), def))
          is BibixAst.VarRedefs -> Unit // do nothing
          is BibixAst.BuildRuleDef -> addName(
            def.name,
            BuildRuleNameEntry(bibixName(def.name), def)
          )

          is BibixAst.ActionRuleDef -> addName(
            def.name,
            ActionRuleNameEntry(bibixName(def.name), def)
          )
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
      return when {
        tokens.size == 1 -> {
          NameEntryFound(nameEntry)
        }

        else -> {
          check(tokens.size > 1)
          when {
            nameEntry is ImportNameEntry ->
              NameInImport(nameEntry, tokens.drop(1))

            nameEntry is TargetNameEntry ->
              TargetMemberName(nameEntry, tokens.drop(1))

            tokens.size == 2 && nameEntry is EnumNameEntry -> {
              check(tokens[1] in nameEntry.def.values)
              EnumValueFound(nameEntry, tokens[1])
            }

            else -> NameNotFound(tokens, nameNode)
          }
        }
      }
    }
    val ns = namespaces[firstToken]
    return if (ns != null) {
      if (tokens.size == 1) {
        NamespaceFound(tokens.joinToString("."), nameNode)
      } else {
        ns.lookupName(tokens.drop(1), nameNode)
      }
    } else {
      NameNotFound(tokens, nameNode)
    }
  }
}

data class NameNotFoundException(
  val nameTokens: List<String>,
  val nameNode: BibixAst.AstNode?
): Exception(
  if (nameNode == null) {
    "Name not found: ${nameTokens.joinToString(".")} at $nameNode"
  } else {
    "Name not found: ${nameTokens.joinToString(".")} at ${nameNode.start}..${nameNode.end}"
  }
)
