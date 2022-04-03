package com.giyeok.bibix.buildscript

import com.giyeok.bibix.base.BibixValue
import com.giyeok.bibix.base.CName
import com.giyeok.bibix.base.SourceId

sealed class CNameValue {
  data class LoadedImport(val sourceId: SourceId) : CNameValue()
  data class DeferredImport(val deferredImportId: Int) : CNameValue()
  data class ExprValue(val exprGraphId: Int) : CNameValue()
  data class EvaluatedValue(val value: BibixValue) : CNameValue()
  data class NamespaceValue(val cname: CName, val names: Set<String>) : CNameValue()

  data class ClassType(
    val cname: CName,
    val typeParams: List<Pair<String, BibixType>>,
    val extendings: List<ClassExtending>,
    val reality: BibixType,
    val casts: Map<CName, Int>,
  ) : CNameValue()

  data class EnumType(val cname: CName, val values: List<String>) : CNameValue()

  data class ArgVar(
    val replacing: CName?,
    val type: BibixType,
    val defaultValueId: Int?,
  ) : CNameValue()

  data class BuildRuleValue(
    val params: List<Param>,
    val implName: CName,
    val className: String,
    val methodName: String?,
    val returnType: BibixType,
  ) : CNameValue()

  data class ActionRuleValue(
    val params: List<Param>,
    val implName: CName,
    val className: String,
    val methodName: String?,
  ) : CNameValue()

  data class ActionCallValue(
    val exprGraphId: Int,
  ) : CNameValue()
}

// CustomType은 실제로 클래스인지 enum인지 알 수 없는 경우
sealed class BibixType
object AnyType : BibixType()
object BooleanType : BibixType()
object StringType : BibixType()
object PathType : BibixType()
object FileType : BibixType()
object DirectoryType : BibixType()
data class CustomType(val name: CName) : BibixType()
data class ClassType(val name: CName, val paramExprGraphIds: List<Int>) : BibixType()
data class ListType(val elemType: BibixType) : BibixType()
data class SetType(val elemType: BibixType) : BibixType()
data class TupleType(val elemTypes: List<BibixType>) : BibixType()
data class NamedTupleType(val elemTypes: List<Pair<String, BibixType>>) : BibixType()
data class UnionType(val types: List<BibixType>) : BibixType()

data class ClassExtending(val className: CName, val typeParams: List<ExprGraph>)

data class Param(
  val name: String,
  val optional: Boolean,
  val type: BibixType,
  val defaultValueId: Int?,
)
