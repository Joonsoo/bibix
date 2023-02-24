package com.giyeok.bibix.utils

import com.giyeok.bibix.*
import com.giyeok.bibix.base.*
import com.google.protobuf.ByteString
import com.google.protobuf.kotlin.get
import java.nio.file.Path
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

fun BibixValue.toProto(): BibixValueProto.BibixValue = when (val value = this) {
  is BooleanValue -> bibixValue { this.booleanValue = value.value }
  is StringValue -> bibixValue { this.stringValue = value.value }
  is PathValue -> bibixValue { this.pathValue = value.path.absolutePathString() }
  is FileValue -> bibixValue { this.fileValue = value.file.absolutePathString() }
  is DirectoryValue -> bibixValue { this.directoryValue = value.directory.absolutePathString() }
  is EnumValue -> bibixValue {
    this.enumValue = enumValue {
      this.enumType = value.enumName.toString()
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
