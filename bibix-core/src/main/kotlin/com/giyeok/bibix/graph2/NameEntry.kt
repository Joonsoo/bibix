package com.giyeok.bibix.graph2

import com.giyeok.bibix.ast.BibixAst
import kotlin.reflect.KClass

sealed class NameEntry {
  abstract val name: BibixName
  abstract val def: BibixAst.Def
}

data class ActionNameEntry(
  override val name: BibixName,
  override val def: BibixAst.ActionDef
): NameEntry()

data class ActionRuleNameEntry(
  override val name: BibixName,
  override val def: BibixAst.ActionRuleDef
): NameEntry()

data class BuildRuleNameEntry(
  override val name: BibixName,
  override val def: BibixAst.BuildRuleDef
): NameEntry()

data class DataClassNameEntry(
  override val name: BibixName,
  override val def: BibixAst.DataClassDef
): NameEntry()

data class SuperClassNameEntry(
  override val name: BibixName,
  override val def: BibixAst.SuperClassDef
): NameEntry()

data class EnumNameEntry(
  override val name: BibixName,
  override val def: BibixAst.EnumDef
): NameEntry()

data class ImportNameEntry(
  override val name: BibixName,
  override val def: BibixAst.ImportDef
): NameEntry()

data class TargetNameEntry(
  override val name: BibixName,
  override val def: BibixAst.TargetDef
): NameEntry()

data class VarNameEntry(
  override val name: BibixName,
  override val def: BibixAst.VarDef
): NameEntry()
