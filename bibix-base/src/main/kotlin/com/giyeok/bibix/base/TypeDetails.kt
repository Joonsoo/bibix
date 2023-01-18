package com.giyeok.bibix.base

sealed class TypeDetails

sealed class ClassTypeDetails : TypeDetails()

data class DataClassTypeDetails(
  val packageName: String,
  val className: String,
  val fields: List<RuleParam>
) : ClassTypeDetails() {
  override fun toString(): String = "class $className"
}

data class SuperClassTypeDetails(
  val packageName: String,
  val className: String,
  // subclass들은 모두 같은 패키지(같은 스크립트) 안에 속해야 함
  val subClasses: List<String>
) : ClassTypeDetails() {
  override fun toString(): String = "super class $className"
}

data class EnumTypeDetails(
  val packageName: String,
  val enumName: String,
  val values: List<String>
) : TypeDetails()
