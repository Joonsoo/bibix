package com.giyeok.bibix.buildscript

import com.giyeok.bibix.ast.BibixAst
import com.giyeok.bibix.base.BibixRootSourceId
import com.giyeok.bibix.base.CName
import com.giyeok.bibix.utils.getOrNull
import com.giyeok.bibix.utils.toKtList

data class NameLookupContext(
  val chain: NameLookupChain,
  val allowNative: Boolean,
  val argsName: String?,
) {
  constructor(cname: CName, defs: List<BibixAst.Def>) :
    this(
      NameLookupChain.LookupChain(
        NameLookupChain.RootLookupChain(
          NameLookupChain.NameScope(
            CName(BibixRootSourceId),
            // TODO 이렇게 하지 말고 실제 스크립트에서 읽어오고 싶다..
            setOf("git", "glob", "env", "OS", "Arch")
          )
        ),
        NameLookupChain.NameScope.fromDefs(cname, defs)
      ),
      false, null
    )

  fun findName(name: BibixAst.Name): CName = findName(name.tokens().toKtList())

  fun findName(nameTokens: List<String>): CName =
    findName(nameTokens.first()).append(nameTokens.drop(1))

  fun findName(nameToken: String): CName =
    if (allowNative && nameToken == "native") {
      CName(chain.scope.cname.sourceId, "$$")
    } else {
      chain.findName(nameToken)
    }

  fun append(subname: CName, defs: List<BibixAst.Def>): NameLookupContext = copy(
    chain = NameLookupChain.LookupChain(
      chain,
      NameLookupChain.NameScope.fromDefs(subname, defs)
    )
  )

  fun withNative() = copy(allowNative = true)

  fun withArgs(argsName: String) = copy(argsName = argsName)
}

sealed class NameLookupChain {
  data class NameScope(val cname: CName, val names: Set<String>) {
    companion object {
      fun fromDefs(cname: CName, defs: List<BibixAst.Def>): NameScope {
        val names = defs.mapNotNull { def ->
          when (def) {
            is BibixAst.NamespaceDef -> def.name()
//            is BibixAst.ImportName -> def.rename().getOrNull() ?: def.name().tokens().last()
            is BibixAst.ImportAll -> def.rename().getOrNull() ?: "???"
            is BibixAst.ImportFrom -> def.rename().getOrNull() ?: def.importing().tokens().last()
            is BibixAst.TargetDef -> def.name()
            is BibixAst.DataClassDef -> def.name()
            is BibixAst.SuperClassDef -> def.name()
            is BibixAst.ArgDef -> def.name()
            is BibixAst.ArgRedef -> null
            is BibixAst.BuildRuleDef -> def.name()
            is BibixAst.ActionDef -> def.name()
            is BibixAst.ActionRuleDef -> def.name()
            is BibixAst.EnumDef -> def.name()
            else -> throw AssertionError()
          }
        }.toSet()
        return NameScope(cname, names)
      }
    }
  }

  abstract val scope: NameScope
  abstract fun findName(name: String): CName

  data class RootLookupChain(override val scope: NameScope) : NameLookupChain() {
    override fun findName(name: String): CName {
      check(scope.names.contains(name)) { "Cannot find name: $name" }
      return scope.cname.append(name)
    }
  }

  data class LookupChain(val parent: NameLookupChain, override val scope: NameScope) :
    NameLookupChain() {
    override fun findName(name: String): CName =
      if (scope.names.contains(name)) {
        scope.cname.append(name)
      } else {
        parent.findName(name)
      }
  }
}
