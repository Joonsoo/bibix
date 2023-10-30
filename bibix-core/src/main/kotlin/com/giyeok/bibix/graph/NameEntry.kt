package com.giyeok.bibix.graph

import com.giyeok.bibix.ast.BibixAst
import kotlin.reflect.KClass

sealed class NameEntry(private val taskType: KClass<out TaskNode>) {
  abstract val def: BibixAst.Def

  val id: TaskId get() = TaskId(def.nodeId, taskType)
}

data class ActionNameEntry(override val def: BibixAst.ActionDef):
  NameEntry(ActionDefNode::class)

data class ActionRuleNameEntry(override val def: BibixAst.ActionRuleDef):
  NameEntry(ActionRuleDefNode::class)

data class BuildRuleNameEntry(override val def: BibixAst.BuildRuleDef):
  NameEntry(BuildRuleNode::class)

data class DataClassNameEntry(override val def: BibixAst.DataClassDef):
  NameEntry(DataClassTypeNode::class)

data class SuperClassNameEntry(override val def: BibixAst.SuperClassDef):
  NameEntry(SuperClassTypeNode::class)

data class EnumNameEntry(override val def: BibixAst.EnumDef): NameEntry(EnumTypeNode::class)
data class ImportNameEntry(override val def: BibixAst.ImportDef): NameEntry(ImportNode::class)
data class TargetNameEntry(override val def: BibixAst.TargetDef): NameEntry(TargetNode::class)
data class VarNameEntry(override val def: BibixAst.VarDef): NameEntry(VarNode::class)
