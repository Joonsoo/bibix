package com.giyeok.bibix.utils

import com.giyeok.bibix.base.*
import com.giyeok.bibix.runner.*
import com.giyeok.bibix.runner.BibixIdProto.ArgsMap
import com.google.protobuf.ByteString
import com.google.protobuf.kotlin.get
import kotlin.io.path.absolutePathString

fun ByteString.toHexString(): String {
  val chars = "0123456789abcdef"
  val builder = StringBuilder()
  (0 until this.size()).map { i ->
    val v = this[i].toInt() and 0xff
    builder.append(chars[v / 16])
    builder.append(chars[v % 16])
  }
  return builder.toString()
}

fun String.hexToByteString(): ByteString {
  fun hexChar(c: Char): Int =
    when (c) {
      in '0'..'9' -> c - '0'
      in 'A'..'F' -> c - 'A' + 10
      in 'a'..'f' -> c - 'a' + 10
      else -> throw IllegalArgumentException("Not a hex string: $this")
    }

  val bytes = this.windowed(2, 2)
  return ByteString.copyFrom(
    ByteArray(bytes.size) { idx ->
      val b = bytes[idx]
      val f = b[0]
      val s = b[1]
      (((hexChar(f) shl 4) and 0xf0) or (hexChar(s) and 0xf)).toByte()
    })
}

fun BibixValue.toProto(): BibixValueProto.BibixValue = when (val value = this) {
  is BooleanValue -> bibixValue { this.booleanValue = value.value }
  is StringValue -> bibixValue { this.stringValue = value.value }
  is PathValue -> bibixValue { this.pathValue = value.path.absolutePathString() }
  is FileValue -> bibixValue { this.fileValue = value.file.absolutePathString() }
  is DirectoryValue -> bibixValue { this.directoryValue = value.directory.absolutePathString() }
  is EnumValue -> bibixValue {
    this.enumValue = enumValue {
      this.enumType = value.enumTypeName.toString()
      this.value = value.value
    }
  }
  is ListValue -> bibixValue {
    this.listValue = listValue {
      this.values.addAll(value.values.map { it.toProto() })
    }
  }
  is SetValue -> bibixValue {
    this.setValue = setValue {
      this.values.addAll(value.values.map { it.toProto() })
    }
  }
  is TupleValue -> bibixValue {
    this.tupleValue = tupleValue {
      this.values.addAll(value.values.map { it.toProto() })
    }
  }
  is NamedTupleValue -> bibixValue {
    this.namedTupleValue = namedTupleValue {
      this.values.addAll(value.pairs.map {
        namedValue {
          this.name = it.first
          this.value = it.second.toProto()
        }
      })
    }
  }
  is DataClassInstanceValue -> bibixValue {
    this.dataClassInstanceValue = dataClassInstanceValue {
      this.classCname = value.className.toString()
      value.fieldValues.entries.sortedBy { it.key }.forEach { entry ->
        this.fields.add(dataClassField {
          this.name = entry.key
          this.value = entry.value.toProto()
        })
      }
    }
  }
  is SuperClassInstanceValue -> bibixValue {
    this.superClassInstanceValue = superClassInstanceValue {
      this.classCname = value.className.toString()
      this.value = value.value.toProto()
    }
  }
  is NDataClassInstanceValue -> throw AssertionError("N(ame)ClassInstnaceValue cannot be marshalled")
  is NoneValue -> bibixValue { }
  is ActionRuleDefValue ->
    // TODO
    bibixValue {}
  is BuildRuleDefValue ->
    // TODO
    bibixValue {}
  is TypeValue ->
    // TODO
    bibixValue {}
}

fun Map<String, BibixValue>.toArgsMap(): ArgsMap {
  val value = this
  return argsMap {
    this.pairs.addAll(value.entries.toList()
      .sortedBy { it.key }
      .map {
        argPair {
          this.name = it.key
          this.value = it.value.toProto()
        }
      })
  }
}