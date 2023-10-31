package com.giyeok.bibix.base

sealed class BibixType {
  abstract fun toTypeValue(): TypeValue
}

object AnyType: BibixType() {
  override fun toTypeValue(): TypeValue = TypeValue.AnyTypeValue
}

object BooleanType: BibixType() {
  override fun toTypeValue(): TypeValue = TypeValue.BooleanTypeValue
}

object StringType: BibixType() {
  override fun toTypeValue(): TypeValue = TypeValue.StringTypeValue
}

object PathType: BibixType() {
  override fun toTypeValue(): TypeValue = TypeValue.PathTypeValue
}

object FileType: BibixType() {
  override fun toTypeValue(): TypeValue = TypeValue.FileTypeValue
}

object DirectoryType: BibixType() {
  override fun toTypeValue(): TypeValue = TypeValue.DirectoryTypeValue
}

data class ListType(val elemType: BibixType): BibixType() {
  override fun toTypeValue(): TypeValue = TypeValue.ListTypeValue(elemType.toTypeValue())
}

data class SetType(val elemType: BibixType): BibixType() {
  override fun toTypeValue(): TypeValue = TypeValue.SetTypeValue(elemType.toTypeValue())
}

data class TupleType(val elemTypes: List<BibixType>): BibixType() {
  override fun toTypeValue(): TypeValue =
    TypeValue.TupleTypeValue(elemTypes.map { it.toTypeValue() })
}

data class NamedTupleType(val pairs: List<Pair<String, BibixType>>): BibixType() {
  val names get() = pairs.map { it.first }
  val valueTypes get() = pairs.map { it.second }

  override fun toTypeValue(): TypeValue =
    TypeValue.NamedTupleTypeValue(pairs.map { it.first to it.second.toTypeValue() })
}

data class DataClassType(val packageName: String, val className: String): BibixType() {
  override fun toTypeValue(): TypeValue = TypeValue.DataClassTypeValue(packageName, className)
}

data class SuperClassType(val packageName: String, val className: String): BibixType() {
  override fun toTypeValue(): TypeValue = TypeValue.SuperClassTypeValue(packageName, className)
}

data class EnumType(val packageName: String, val enumName: String): BibixType() {
  override fun toTypeValue(): TypeValue = TypeValue.EnumTypeValue(packageName, enumName)
}

data class UnionType(val types: List<BibixType>): BibixType() {
  override fun toTypeValue(): TypeValue = TypeValue.UnionTypeValue(types.map { it.toTypeValue() })
}

object NoneType: BibixType() {
  override fun toTypeValue(): TypeValue = TypeValue.NoneTypeValue
}

object BuildRuleDefType: BibixType() {
  override fun toTypeValue(): TypeValue = TypeValue.BuildRuleDefTypeValue
}

object ActionRuleDefType: BibixType() {
  override fun toTypeValue(): TypeValue = TypeValue.ActionRuleDefTypeValue
}

object TypeType: BibixType() {
  override fun toTypeValue(): TypeValue = TypeValue.TypeTypeValue
}
