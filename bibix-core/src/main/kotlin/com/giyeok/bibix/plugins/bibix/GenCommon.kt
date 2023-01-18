package com.giyeok.bibix.plugins.bibix

import com.giyeok.bibix.base.TypeValue

fun isNullableType(type: TypeValue.UnionTypeValue): TypeValue? =
  if (type.types.contains(TypeValue.NoneTypeValue) && type.types.size == 2) {
    if (type.types[0] == TypeValue.NoneTypeValue) type.types[1] else type.types[0]
  } else null

fun bibixTypeToKtType(bibixType: TypeValue): String =
  when (bibixType) {
    TypeValue.AnyTypeValue -> "BibixValue"
    TypeValue.BooleanTypeValue -> "Boolean"
    TypeValue.StringTypeValue -> "String"
    TypeValue.PathTypeValue, TypeValue.FileTypeValue, TypeValue.DirectoryTypeValue -> "Path"
    is TypeValue.DataClassTypeValue -> bibixType.className
    is TypeValue.SuperClassTypeValue -> bibixType.className
    is TypeValue.EnumTypeValue -> bibixType.enumName
    is TypeValue.ListTypeValue -> "List<${bibixTypeToKtType(bibixType.elemType)}>"
    is TypeValue.SetTypeValue -> "List<${bibixTypeToKtType(bibixType.elemType)}>"
    is TypeValue.TupleTypeValue -> TODO()
    is TypeValue.NamedTupleTypeValue -> TODO()
    is TypeValue.UnionTypeValue -> {
      // nullable인 경우만 처리해줌
      val nullableValueType = isNullableType(bibixType)
      if (nullableValueType != null) {
        "${bibixTypeToKtType(nullableValueType)}?"
      } else {
        TODO()
      }
    }

    TypeValue.NoneTypeValue -> TODO()
    TypeValue.BuildRuleDefTypeValue -> TODO()
    TypeValue.ActionRuleDefTypeValue -> TODO()
    TypeValue.TypeTypeValue -> TODO()
  }

fun bibixValueToKt(expr: String, bibixType: TypeValue): String = when (bibixType) {
  TypeValue.AnyTypeValue -> expr
  TypeValue.BooleanTypeValue -> "($expr as BooleanValue).value"
  TypeValue.StringTypeValue -> "($expr as StringValue).value"
  TypeValue.PathTypeValue -> "($expr as PathValue).path"
  TypeValue.FileTypeValue -> "($expr as FileValue).file"
  TypeValue.DirectoryTypeValue -> "($expr as DirectoryValue).directory"
  is TypeValue.DataClassTypeValue -> "${bibixType.className}.fromBibix($expr)"
  is TypeValue.SuperClassTypeValue -> "${bibixType.className}.fromBibix($expr)"
  is TypeValue.EnumTypeValue -> {
    "${bibixType.enumName}.valueOf((($expr) as EnumValue).value)"
  }

  is TypeValue.ListTypeValue ->
    "($expr as ListValue).values.map { ${bibixValueToKt("it", bibixType.elemType)} }"

  is TypeValue.SetTypeValue ->
    "($expr as SetValue).values.map { ${bibixValueToKt("it", bibixType.elemType)} }"

  is TypeValue.TupleTypeValue -> TODO()
  is TypeValue.NamedTupleTypeValue -> TODO()
  is TypeValue.UnionTypeValue -> {
    val nullableValueType = isNullableType(bibixType)
    if (nullableValueType != null) {
      "$expr.let { if (it == NoneValue) null else ${bibixValueToKt("it", nullableValueType)} }"
    } else {
      TODO()
    }
  }

  TypeValue.NoneTypeValue -> TODO()
  TypeValue.BuildRuleDefTypeValue -> TODO()
  TypeValue.ActionRuleDefTypeValue -> TODO()
  TypeValue.TypeTypeValue -> TODO()
}

fun ktValueToBibix(expr: String, bibixType: TypeValue): String = when (bibixType) {
  TypeValue.AnyTypeValue -> expr
  TypeValue.BooleanTypeValue -> "BooleanValue($expr)"
  TypeValue.StringTypeValue -> "StringValue($expr)"
  TypeValue.PathTypeValue -> "PathValue($expr)"
  TypeValue.FileTypeValue -> "FileValue($expr)"
  TypeValue.DirectoryTypeValue -> "DirectoryValue($expr)"
  is TypeValue.DataClassTypeValue -> "$expr.toBibix()"
  is TypeValue.SuperClassTypeValue -> "$expr.toBibix()"
  is TypeValue.EnumTypeValue -> TODO()
  is TypeValue.ListTypeValue ->
    "ListValue($expr.map { ${ktValueToBibix("it", bibixType.elemType)} })"

  is TypeValue.SetTypeValue ->
    "SetValue($expr.map { ${ktValueToBibix("it", bibixType.elemType)} })"

  is TypeValue.TupleTypeValue -> TODO()
  is TypeValue.NamedTupleTypeValue -> TODO()
  is TypeValue.UnionTypeValue -> {
    val nullableValueType = isNullableType(bibixType)
    if (nullableValueType != null) {
      "$expr?.let { ${ktValueToBibix("it", nullableValueType)} }"
    } else {
      TODO()
    }
  }

  TypeValue.NoneTypeValue -> TODO()
  TypeValue.BuildRuleDefTypeValue -> TODO()
  TypeValue.ActionRuleDefTypeValue -> TODO()
  TypeValue.TypeTypeValue -> TODO()
}
