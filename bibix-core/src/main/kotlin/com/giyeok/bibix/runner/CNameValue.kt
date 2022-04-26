package com.giyeok.bibix.runner

import com.giyeok.bibix.base.BibixValue
import com.giyeok.bibix.base.CName
import com.giyeok.bibix.base.SourceId

sealed class CNameValue {
  data class DeferredImport(val deferredImportId: Int) : CNameValue()
  data class ExprValue(val exprGraphId: Int) : CNameValue()
  data class EvaluatedValue(val value: BibixValue) : CNameValue() // not by BuildGraph
  data class NamespaceValue(val cname: CName, val names: Set<String>) : CNameValue()

  data class ClassType(
    val cname: CName,
    val extendings: List<CName>,
    val reality: BibixType,
    val casts: Map<BibixType, Int>,
  ) : CNameValue()

  data class EnumType(val cname: CName, val values: List<String>) : CNameValue()

  data class ArgVar(
    val replacing: CName?,
    val type: BibixType,
    val defaultValueId: Int?,
  ) : CNameValue()

  data class BuildRuleValue(
    val cname: CName,
    val params: List<Param>,
    val implName: CName,
    val className: String,
    val methodName: String?,
    val returnType: BibixType,
  ) : CNameValue()

  data class ActionRuleValue(
    val cname: CName,
    val params: List<Param>,
    val implName: CName,
    val className: String,
    val methodName: String?,
  ) : CNameValue()

  data class ActionCallValue(
    val exprGraphId: Int,
  ) : CNameValue()
}

data class Param(
  val name: String,
  val optional: Boolean,
  val type: BibixType,
  val defaultValueId: Int?,
)
