package com.giyeok.bibix.base

import java.io.File

sealed class BibixValue

data class BooleanValue(val value: Boolean) : BibixValue() {
  override fun toString(): String = "$value"
}

data class StringValue(val value: String) : BibixValue() {
  override fun toString(): String = "\"$value\""
}

data class PathValue(val path: File) : BibixValue() {
  override fun toString(): String = "path($path)"
}

data class FileValue(val file: File) : BibixValue() {
  override fun toString(): String = "file($file)"
}

data class DirectoryValue(val directory: File) : BibixValue() {
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

data class ClassInstanceValue(val className: CName, val value: BibixValue) : BibixValue() {
  override fun toString(): String = "$className($value)"
}

data class NClassInstanceValue(val nameTokens: List<String>, val value: BibixValue) : BibixValue() {
  constructor(name: String, value: BibixValue) : this(name.split('.'), value)

  override fun toString(): String = "${nameTokens.joinToString(".")}($value)"
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
    val extendings: List<CName>,
    val bodyType: TypeValue
  ) {
    override fun toString(): String = "class $className"
  }

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
  is NClassInstanceValue -> this.toString()
  is ClassInstanceValue -> this.toString()
  is BooleanValue -> value.toString()
  is DirectoryValue -> directory.canonicalPath
  is EnumValue -> value
  is FileValue -> file.canonicalPath
  is ListValue -> "[${values.joinToString { it.stringify() }}]"
  is NamedTupleValue -> "(${pairs.joinToString { "${it.first}=${it.second.stringify()}" }})"
  is PathValue -> path.canonicalPath
  is SetValue -> "[${values.joinToString { it.stringify() }}]"
  is StringValue -> value
  is TupleValue -> "(${values.joinToString { it.stringify() }})"
  NoneValue -> "none"
  is BuildRuleDefValue -> this.toString()
  is ActionRuleDefValue -> this.toString()
  is TypeValue -> this.toString()
}
