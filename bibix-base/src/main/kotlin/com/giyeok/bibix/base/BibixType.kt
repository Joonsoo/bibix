package com.giyeok.bibix.base

sealed class BibixType

object AnyType : BibixType()

object BooleanType : BibixType()

object StringType : BibixType()

object PathType : BibixType()

object FileType : BibixType()

object DirectoryType : BibixType()

data class ListType(val elemType: BibixType) : BibixType()

data class SetType(val elemType: BibixType) : BibixType()

data class TupleType(val elemTypes: List<BibixType>) : BibixType()

data class NamedTupleType(val pairs: List<Pair<String, BibixType>>) : BibixType() {
  fun names() = pairs.map { it.first }
  fun valueTypes() = pairs.map { it.second }
}

data class DataClassType(val packageName: String, val className: String) : BibixType()

data class SuperClassType(val packageName: String, val className: String) : BibixType()

data class EnumType(val packageName: String, val enumName: String) : BibixType()

data class UnionType(val types: List<BibixType>) : BibixType()

object NoneType : BibixType()

object BuildRuleDefType : BibixType()

object ActionRuleDefType : BibixType()

object TypeType : BibixType()
