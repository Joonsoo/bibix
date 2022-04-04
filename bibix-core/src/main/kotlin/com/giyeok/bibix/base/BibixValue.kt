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
  val values = elems.distinct().sortedWith(BibixValueComparator)

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

object BibixValueComparator : Comparator<BibixValue> {
  override fun compare(p0: BibixValue, p1: BibixValue): Int {
    // TODO 제대로 다시 구현
    return p0.stringify().compareTo(p1.stringify())
  }
}

data class TupleValue(val values: List<BibixValue>) : BibixValue() {
  constructor(vararg values: BibixValue) : this(values.toList())

  override fun toString(): String = "(${values.joinToString()})"
}

data class NamedTupleValue(val values: List<Pair<String, BibixValue>>) : BibixValue() {
  constructor(vararg values: Pair<String, BibixValue>) : this(values.toList())

  fun getValue(name: String) = values.find { it.first == name }!!.second

  override fun toString(): String =
    "(${values.joinToString() { p -> "${p.first}: ${p.second}" }})"
}

data class ClassInstanceValue(val className: CName, val value: BibixValue) : BibixValue() {
  override fun toString(): String =
    "$className($value)"
}

object NoneValue : BibixValue()

fun BibixValue.stringify(): String = when (this) {
  is ClassInstanceValue -> this.toString()
  is BooleanValue -> value.toString()
  is DirectoryValue -> directory.path
  is EnumValue -> value
  is FileValue -> file.path
  is ListValue -> "[${values.joinToString { it.stringify() }}]"
  is NamedTupleValue -> "(${values.joinToString { "${it.first}=${it.second.stringify()}" }})"
  is PathValue -> path.path
  is SetValue -> "[${values.joinToString { it.stringify() }}]"
  is StringValue -> value
  is TupleValue -> "(${values.joinToString { it.stringify() }})"
  NoneValue -> "none"
}
