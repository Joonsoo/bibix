package com.giyeok.bibix.base

import java.nio.file.Path

sealed class BibixValue

data class BooleanValue(val value: Boolean) : BibixValue() {
  override fun toString(): String = "$value"
}

data class StringValue(val value: String) : BibixValue() {
  override fun toString(): String = "\"$value\""
}

data class PathValue(val path: Path) : BibixValue() {
  override fun toString(): String = "path($path)"
}

data class FileValue(val file: Path) : BibixValue() {
  override fun toString(): String = "file($file)"
}

data class DirectoryValue(val directory: Path) : BibixValue() {
  override fun toString(): String = "dir($directory)"
}

data class EnumValue(val enumTypeName: CName, val value: String) : BibixValue() {
  override fun toString(): String = "$enumTypeName::$value"
}

data class ListValue(val values: List<BibixValue>) : BibixValue() {
  constructor(vararg values: BibixValue) : this(values.toList())

  override fun toString(): String = "[${values.joinToString()}]"
}

class SetValue(elems: List<BibixValue>) : BibixValue() {
  val values = elems.distinct()

  constructor(vararg values: BibixValue) : this(values.toList())

  override fun toString(): String = "{${values.joinToString()}}"

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

data class TupleValue(val values: List<BibixValue>) : BibixValue() {
  constructor(vararg values: BibixValue) : this(values.toList())

  override fun toString(): String = "(${values.joinToString()})"
}

data class NamedTupleValue(val pairs: List<Pair<String, BibixValue>>) : BibixValue() {
  constructor(vararg values: Pair<String, BibixValue>) : this(values.toList())

  fun getValue(name: String) = pairs.find { it.first == name }!!.second

  fun names() = pairs.map { it.first }
  fun values() = pairs.map { it.second }

  override fun toString(): String =
    "(${pairs.joinToString() { p -> "${p.first}: ${p.second}" }})"
}

data class DataClassInstanceValue(
  val className: CName,
  val fieldValues: Map<String, BibixValue>
) : BibixValue() {
  override fun toString(): String = "$className($fieldValues)"
}

// value는 DataClassInstanceValue이거나 SuperClassInstanceValue일 것
data class SuperClassInstanceValue(val className: CName, val value: BibixValue) : BibixValue() {
  override fun toString(): String = "$className($value)"
}

// 사용자가 super class instance value를 만들 일은 없을듯
data class NDataClassInstanceValue(
  val nameTokens: List<String>,
  val fieldValues: Map<String, BibixValue>
) : BibixValue() {
  constructor(name: String, fieldValues: Map<String, BibixValue>) :
    this(name.split('.'), fieldValues)

  override fun toString(): String = "${nameTokens.joinToString(".")}($fieldValues)"
}

object NoneValue : BibixValue()

data class BuildRuleDefValue(
  val name: CName,
  val params: List<RuleParam>,
  val impl: CName,
  val implClass: String,
  val implMethodName: String,
) : BibixValue() {
  override fun toString(): String = "def $name"
}

data class ActionRuleDefValue(
  val name: CName,
  val params: List<RuleParam>,
  val impl: CName,
  val implClass: String,
  val implMethodName: String,
) : BibixValue() {
  override fun toString(): String = "action def $name"
}

data class RuleParam(
  val name: String,
  val type: TypeValue,
  val optional: Boolean,
)

sealed class TypeValue : BibixValue() {
  object AnyTypeValue : TypeValue() {
    override fun toString(): String = "any"
  }

  object BooleanTypeValue : TypeValue() {
    override fun toString(): String = "boolean"
  }

  object StringTypeValue : TypeValue() {
    override fun toString(): String = "string"
  }

  object PathTypeValue : TypeValue() {
    override fun toString(): String = "path"
  }

  object FileTypeValue : TypeValue() {
    override fun toString(): String = "file"
  }

  object DirectoryTypeValue : TypeValue() {
    override fun toString(): String = "directory"
  }

  data class ClassTypeValue(val className: CName) : TypeValue() {
    override fun toString(): String = "class $className"
  }

  data class ClassTypeDetail(
    val className: CName,
    val relativeName: List<String>,
    val fields: List<ClassFieldValue>
  ) {
    override fun toString(): String = "class $className"
  }

  data class ClassFieldValue(val name: String, val type: TypeValue, val optional: Boolean)

  data class EnumTypeValue(val enumTypeName: CName, val enumValues: List<String>) : TypeValue() {
    override fun toString(): String = "enum $enumTypeName"
  }

  data class ListTypeValue(val elemType: TypeValue) : TypeValue() {
    override fun toString(): String = "list<${elemType}>"
  }

  data class SetTypeValue(val elemType: TypeValue) : TypeValue() {
    override fun toString(): String = "set<${elemType}>"
  }

  data class TupleTypeValue(val elemTypes: List<TypeValue>) : TypeValue() {
    override fun toString(): String = "(${elemTypes.joinToString()})"
  }

  data class NamedTupleTypeValue(val elemTypes: List<Pair<String, TypeValue>>) : TypeValue() {
    override fun toString(): String = "(${elemTypes.joinToString { "${it.first}: ${it.second}" }})"
  }

  data class UnionTypeValue(val types: List<TypeValue>) : TypeValue() {
    override fun toString(): String = "{${types.joinToString()}}"
  }

  object NoneTypeValue : TypeValue() {
    override fun toString(): String = "none"
  }

  object BuildRuleDefTypeValue : TypeValue() {
    override fun toString(): String = "buildrule"
  }

  object ActionRuleDefTypeValue : TypeValue() {
    override fun toString(): String = "actionrule"
  }

  object TypeTypeValue : TypeValue() {
    override fun toString(): String = "type"
  }
}

fun BibixValue.stringify(): String = when (this) {
  is NDataClassInstanceValue -> this.toString()
  is DataClassInstanceValue -> this.toString()
  is SuperClassInstanceValue -> this.toString()
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
