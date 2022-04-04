package com.giyeok.bibix.utils

import com.giyeok.bibix.base.*
import com.giyeok.bibix.runner.*
import com.giyeok.bibix.runner.BibixIdProto.ArgsMap
import com.google.protobuf.ByteString
import com.google.protobuf.kotlin.get

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

fun BibixValue.toProto(): BibixValueProto.BibixValue = when (val value = this) {
  is BooleanValue -> bibixValue { this.booleanValue = value.value }
  is StringValue -> bibixValue { this.stringValue = value.value }
  is PathValue -> bibixValue { this.pathValue = value.path.path }
  is FileValue -> bibixValue { this.fileValue = value.file.path }
  is DirectoryValue -> bibixValue { this.directoryValue = value.directory.path }
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
      this.values.addAll(value.values.map {
        namedValue {
          this.name = it.first
          this.value = it.second.toProto()
        }
      })
    }
  }
  is ClassInstanceValue -> bibixValue {
    this.classInstanceValue = classInstanceValue {
      this.classCname = value.className.toString()
      this.value = value.value.toProto()
    }
  }
  is NoneValue -> bibixValue { }
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