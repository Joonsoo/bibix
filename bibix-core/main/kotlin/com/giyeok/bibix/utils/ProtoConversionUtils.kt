package com.giyeok.bibix.utils

import com.giyeok.bibix.*
import com.giyeok.bibix.base.*
import com.google.protobuf.ByteString
import com.google.protobuf.Timestamp
import com.google.protobuf.empty
import com.google.protobuf.kotlin.get
import com.google.protobuf.util.Timestamps
import java.time.Instant
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString

fun Timestamp.toInstant(): Instant =
  Instant.ofEpochMilli(Timestamps.toMillis(this))

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
      else -> throw IllegalStateException("Not a hex string: $this")
    }

  check(this.length % 2 == 0) { "Not a hex string: $this" }
  val bytes = this.windowed(2, 2)
  return ByteString.copyFrom(
    ByteArray(bytes.size) { idx ->
      val b = bytes[idx]
      val f = b[0]
      val s = b[1]
      (((hexChar(f) shl 4) and 0xf0) or (hexChar(s) and 0xf)).toByte()
    })
}

fun String.hexToByteStringOrNull(): ByteString? = try {
  this.hexToByteString()
} catch (e: IllegalStateException) {
  null
}

object BibixValueComparator : Comparator<BibixValueProto.BibixValue> {
  override fun compare(p0: BibixValueProto.BibixValue, p1: BibixValueProto.BibixValue): Int =
    p0.toString().compareTo(p1.toString())
}

fun BibixValueProto.BibixValue.toBibix(): BibixValue = when (this.valueCase) {
  BibixValueProto.BibixValue.ValueCase.NONE_VALUE -> NoneValue
  BibixValueProto.BibixValue.ValueCase.BOOLEAN_VALUE -> BooleanValue(this.booleanValue)
  BibixValueProto.BibixValue.ValueCase.STRING_VALUE -> StringValue(this.stringValue)
  BibixValueProto.BibixValue.ValueCase.PATH_VALUE -> PathValue(Path(this.pathValue))
  BibixValueProto.BibixValue.ValueCase.FILE_VALUE -> FileValue(Path(this.fileValue))
  BibixValueProto.BibixValue.ValueCase.DIRECTORY_VALUE -> DirectoryValue(Path(this.directoryValue))
  BibixValueProto.BibixValue.ValueCase.ENUM_VALUE -> {
    val clsName = this.enumValue.enumType
    EnumValue(clsName.substringBefore(':'), clsName.substringAfter(':'), this.enumValue.value)
  }

  BibixValueProto.BibixValue.ValueCase.LIST_VALUE ->
    ListValue(this.listValue.valuesList.map { it.toBibix() })

  BibixValueProto.BibixValue.ValueCase.SET_VALUE ->
    SetValue(this.setValue.valuesList.map { it.toBibix() })

  BibixValueProto.BibixValue.ValueCase.TUPLE_VALUE ->
    TupleValue(this.tupleValue.valuesList.map { it.toBibix() })

  BibixValueProto.BibixValue.ValueCase.NAMED_TUPLE_VALUE ->
    NamedTupleValue(this.namedTupleValue.valuesList.map { it.name to it.value.toBibix() })

  BibixValueProto.BibixValue.ValueCase.DATA_CLASS_INSTANCE_VALUE -> {
    val clsName = this.dataClassInstanceValue.classCname
    ClassInstanceValue(
      clsName.substringBefore(':'), clsName.substringAfter(':'),
      this.dataClassInstanceValue.fieldsList.associate {
        it.name to it.value.toBibix()
      }
    )
  }

  BibixValueProto.BibixValue.ValueCase.VALUE_NOT_SET -> {
    TODO()
  }
}

fun BibixValue.toProto(): BibixValueProto.BibixValue = when (val value = this) {
  is BooleanValue -> bibixValue { this.booleanValue = value.value }
  is StringValue -> bibixValue { this.stringValue = value.value }
  is PathValue -> bibixValue { this.pathValue = value.path.absolutePathString() }
  is FileValue -> bibixValue { this.fileValue = value.file.absolutePathString() }
  is DirectoryValue -> bibixValue { this.directoryValue = value.directory.absolutePathString() }
  is EnumValue -> bibixValue {
    this.enumValue = enumValue {
      this.enumType = "${value.packageName}:${value.enumName}"
      this.value = value.value
    }
  }

  is ListValue -> bibixValue {
    this.listValue = listValue {
      this.values.addAll(value.values.map { it.toProto() }.sortedWith(BibixValueComparator))
    }
  }

  is SetValue -> bibixValue {
    this.setValue = setValue {
      this.values.addAll(value.values.map { it.toProto() }.sortedWith(BibixValueComparator))
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

  is ClassInstanceValue -> bibixValue {
    this.dataClassInstanceValue = dataClassInstanceValue {
      this.classCname = "${value.packageName}:${value.className}"
      value.fieldValues.entries.sortedBy { it.key }.forEach { entry ->
        this.fields.add(dataClassField {
          this.name = entry.key
          this.value = entry.value.toProto()
        })
      }
    }
  }

  is NClassInstanceValue -> throw AssertionError("NClassInstnaceValue cannot be marshalled")

  is NoneValue -> bibixValue { this.noneValue = empty {} }

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
