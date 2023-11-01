package com.giyeok.bibix.base

import java.nio.file.Path

sealed class BibixValue {
  inline fun <reified T> nullOr(): T? =
    if (this == NoneValue) null else this as T
}

data class BooleanValue(val value: Boolean): BibixValue() {
  override fun toString(): String = "$value"
}

data class StringValue(val value: String): BibixValue() {
  override fun toString(): String = "\"$value\""
}

data class PathValue(val path: Path): BibixValue() {
  override fun toString(): String = "path($path)"
}

data class FileValue(val file: Path): BibixValue() {
  override fun toString(): String = "file($file)"
}

data class DirectoryValue(val directory: Path): BibixValue() {
  override fun toString(): String = "dir($directory)"
}

data class EnumValue(
  val packageName: String,
  // className은 class가 namespace 안에 정의되어 있는 경우 "abc.def.Class"같은 형태가 될 수 있음
  val enumName: String,
  val value: String
): BibixValue() {
  override fun toString(): String = "enum $packageName:$enumName($value)"
}

sealed class CollectionValue: BibixValue() {
  abstract val values: List<BibixValue>

  abstract fun newCollectionWith(elems: List<BibixValue>): CollectionValue
}

data class ListValue(override val values: List<BibixValue>): CollectionValue() {
  constructor(vararg values: BibixValue): this(values.toList())

  override fun toString(): String = "[${values.joinToString()}]"

  override fun newCollectionWith(elems: List<BibixValue>): CollectionValue =
    ListValue(elems)
}

class SetValue(elems: List<BibixValue>): CollectionValue() {
  override val values = elems.distinct()

  constructor(vararg values: BibixValue): this(values.toList())

  override fun toString(): String = "{${values.joinToString()}}"

  override fun newCollectionWith(elems: List<BibixValue>): CollectionValue =
    SetValue(elems)

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as SetValue

    if (values != other.values) return false

    return true
  }

  override fun hashCode(): Int {
    return values.hashCode()
  }
}

data class TupleValue(val values: List<BibixValue>): BibixValue() {
  constructor(vararg values: BibixValue): this(values.toList())

  override fun toString(): String = "(${values.joinToString()})"
}

data class NamedTupleValue(val pairs: List<Pair<String, BibixValue>>): BibixValue() {
  constructor(vararg values: Pair<String, BibixValue>): this(values.toList())

  val valuesMap = pairs.toMap()

  fun getValue(name: String) = valuesMap.getValue(name)

  val names get() = pairs.map { it.first }
  val values get() = pairs.map { it.second }

  override fun toString(): String =
    "(${pairs.joinToString() { p -> "${p.first}: ${p.second}" }})"
}

data class ClassInstanceValue(
  val packageName: String,
  // className은 class가 namespace 안에 정의되어 있는 경우 "abc.def.Class"같은 형태가 될 수 있음
  val className: String,
  val fieldValues: Map<String, BibixValue>
): BibixValue() {
  operator fun get(fieldName: String): BibixValue? = fieldValues[fieldName]

  override fun toString(): String {
    val fieldValuesText = fieldValues.toList()
      .sortedBy { it.first }
      .joinToString(", ") { (key, value) ->
        "$key=$value"
      }
    return "$packageName::$className($fieldValuesText)"
  }

  fun getField(fieldName: String): BibixValue =
    fieldValues[fieldName] ?: throw IllegalStateException("No such field: $fieldName")

  fun getStringField(fieldName: String): String =
    (getField(fieldName) as StringValue).value

  fun getFileField(fieldName: String): Path =
    (getField(fieldName) as FileValue).file

  fun getDirectoryField(fieldName: String): Path =
    (getField(fieldName) as DirectoryValue).directory

  fun getNullableField(fieldName: String): BibixValue? {
    val value = getField(fieldName)
    return if (value == NoneValue) null else value
  }

  inline fun <reified T: BibixValue> getNullableFieldOf(fieldName: String): T? =
    getNullableField(fieldName) as? T

  fun getNullableStringField(fieldName: String) =
    getNullableFieldOf<StringValue>(fieldName)?.value

  fun getNullableFileField(fieldName: String) =
    getNullableFieldOf<FileValue>(fieldName)?.file

  fun getNullableDirectoryField(fieldName: String) =
    getNullableFieldOf<DirectoryValue>(fieldName)?.directory
}

// Non-canonical named class
// 플러그인이 클래스 값을 반환할 때는 NClassInstanceValue를 사용할 수 있다
data class NClassInstanceValue(
  val nameTokens: List<String>,
  val fieldValues: Map<String, BibixValue>
): BibixValue() {
  constructor(name: String, fieldValues: Map<String, BibixValue>):
    this(name.split('.').map { it.trim() }, fieldValues)

  override fun toString(): String = "${nameTokens.joinToString(".")}($fieldValues)"
}

object NoneValue: BibixValue() {
  override fun toString(): String = "none"
}

data class BuildRuleDefValue(
  val name: CName,
  val params: List<RuleParam>,
  val implClassName: String,
  val implMethodName: String,
): BibixValue() {
  override fun toString(): String = "def $name"
}

data class ActionRuleDefValue(
  val name: CName,
  val params: List<RuleParam>,
  val implClassName: String,
  val implMethodName: String,
): BibixValue() {
  override fun toString(): String = "action def $name"
}

data class RuleParam(
  val name: String,
  val type: TypeValue,
  val optional: Boolean,
)

sealed class TypeValue: BibixValue() {
  object AnyTypeValue: TypeValue() {
    override fun toString(): String = "any"
  }

  object BooleanTypeValue: TypeValue() {
    override fun toString(): String = "boolean"
  }

  object StringTypeValue: TypeValue() {
    override fun toString(): String = "string"
  }

  object PathTypeValue: TypeValue() {
    override fun toString(): String = "path"
  }

  object FileTypeValue: TypeValue() {
    override fun toString(): String = "file"
  }

  object DirectoryTypeValue: TypeValue() {
    override fun toString(): String = "directory"
  }

  sealed class PackageNamed: TypeValue() {
    abstract val typeName: TypeName
  }

  data class DataClassTypeValue(val packageName: String, val className: String): PackageNamed() {
    override val typeName: TypeName get() = TypeName(packageName, className)

    override fun toString(): String = "class $packageName:$className"
  }

  data class SuperClassTypeValue(val packageName: String, val className: String): PackageNamed() {
    override val typeName: TypeName get() = TypeName(packageName, className)

    override fun toString(): String = "super class $packageName:$className"
  }

  data class EnumTypeValue(val packageName: String, val enumName: String): PackageNamed() {
    override val typeName: TypeName get() = TypeName(packageName, enumName)

    override fun toString(): String = "enum $packageName:$enumName"
  }

  data class ListTypeValue(val elemType: TypeValue): TypeValue() {
    override fun toString(): String = "list<${elemType}>"
  }

  data class SetTypeValue(val elemType: TypeValue): TypeValue() {
    override fun toString(): String = "set<${elemType}>"
  }

  data class TupleTypeValue(val elemTypes: List<TypeValue>): TypeValue() {
    override fun toString(): String = "(${elemTypes.joinToString()})"
  }

  data class NamedTupleTypeValue(val elemTypes: List<Pair<String, TypeValue>>): TypeValue() {
    override fun toString(): String = "(${elemTypes.joinToString { "${it.first}: ${it.second}" }})"
  }

  data class UnionTypeValue(val types: List<TypeValue>): TypeValue() {
    override fun toString(): String = "{${types.joinToString()}}"
  }

  object NoneTypeValue: TypeValue() {
    override fun toString(): String = "none"
  }

  object BuildRuleDefTypeValue: TypeValue() {
    override fun toString(): String = "buildrule"
  }

  object ActionRuleDefTypeValue: TypeValue() {
    override fun toString(): String = "actionrule"
  }

  object TypeTypeValue: TypeValue() {
    override fun toString(): String = "type"
  }
}

fun BibixValue.stringify(): String = when (this) {
  is NClassInstanceValue -> this.toString()
  is ClassInstanceValue -> this.toString()
  is BooleanValue -> value.toString()
  is DirectoryValue -> directory.normalize().toString()
  is EnumValue -> value
  is FileValue -> file.normalize().toString()
  is ListValue -> "[${values.joinToString { it.stringify() }}]"
  is NamedTupleValue -> "(${pairs.joinToString { "${it.first}=${it.second.stringify()}" }})"
  is PathValue -> path.normalize().toString()
  is SetValue -> "[${values.joinToString { it.stringify() }}]"
  is StringValue -> value
  is TupleValue -> "(${values.joinToString { it.stringify() }})"
  NoneValue -> "none"
  is BuildRuleDefValue -> this.toString()
  is ActionRuleDefValue -> this.toString()
  is TypeValue -> this.toString()
}
