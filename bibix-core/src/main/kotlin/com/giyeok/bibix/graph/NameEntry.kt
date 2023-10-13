package com.giyeok.bibix.graph

import com.giyeok.bibix.ast.BibixAst

sealed class NameEntry {
  abstract val def: BibixAst.Def

  val id: TaskId get() = TaskId(def.nodeId)
}

data class ActionNameEntry(override val def: BibixAst.ActionDef): NameEntry()
data class ActionRuleNameEntry(override val def: BibixAst.ActionRuleDef): NameEntry()
data class BuildRuleNameEntry(override val def: BibixAst.BuildRuleDef): NameEntry()
data class ClassNameEntry(override val def: BibixAst.ClassDef): NameEntry()
data class EnumNameEntry(override val def: BibixAst.EnumDef): NameEntry()
data class ImportNameEntry(override val def: BibixAst.ImportDef): NameEntry()
data class TargetNameEntry(override val def: BibixAst.TargetDef): NameEntry()
data class VarNameEntry(override val def: BibixAst.VarDef): NameEntry()
