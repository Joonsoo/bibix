package com.giyeok.bibix.graph.runner

import com.giyeok.bibix.base.*
import java.nio.file.Path
import kotlin.io.path.absolutePathString


private fun cannotCoerce(value: BibixValue, type: BibixType): Nothing =
  throw IllegalStateException("Cannot coerce $value to $type")

fun GlobalTaskRunner.coerceValue(
  value: BibixValue,
  type: BibixType,
  prjInstanceId: ProjectInstanceId
): BibixValue {
  fun fileFromString(path: String): Path {
    val projectLocation = globalGraph.projectLocations.getValue(prjInstanceId.projectId)
    return projectLocation.projectRoot.resolve(path).normalize()
  }

  if (value == NoneValue) {
    return value
  }
  if (value is ClassInstanceValue) {
    // TODO value의 as element 중에 적용 가능한게 있는지 확인해서 적용해서 반환
  }
  return when (type) {
    AnyType -> value
    BooleanType -> when (value) {
      is BooleanValue -> value
      else -> cannotCoerce(value, type)
    }

    StringType -> when (value) {
      is StringValue -> value
      is PathValue -> StringValue(value.path.absolutePathString())
      is FileValue -> StringValue(value.file.absolutePathString())
      is DirectoryValue -> StringValue(value.directory.absolutePathString())
      else -> cannotCoerce(value, type)
    }

    PathType -> when (value) {
      is PathValue -> value
      is FileValue -> PathValue(value.file)
      is DirectoryValue -> PathValue(value.directory)
      is StringValue -> PathValue(fileFromString(value.value))
      else -> cannotCoerce(value, type)
    }

    FileType -> when (value) {
      is FileValue -> value
      // TODO file value, directory value는 build rule에 주기 직전에 존재하는지/타입 확인하기
      is PathValue -> FileValue(value.path)
      is StringValue -> FileValue(fileFromString(value.value))
      else -> cannotCoerce(value, type)
    }

    DirectoryType -> when (value) {
      is DirectoryValue -> value
      is PathValue -> DirectoryValue(value.path)
      is StringValue -> DirectoryValue(fileFromString(value.value))
      else -> cannotCoerce(value, type)
    }

    is ListType -> {
      when (value) {
        is ListValue ->
          ListValue(value.values.map { coerceValue(it, type.elemType, prjInstanceId) })

        is SetValue ->
          ListValue(value.values.map { coerceValue(it, type.elemType, prjInstanceId) })

        else -> cannotCoerce(value, type)
      }
    }

    is SetType -> {
      when (value) {
        is ListValue ->
          SetValue(value.values.map { coerceValue(it, type.elemType, prjInstanceId) })

        is SetValue ->
          SetValue(value.values.map { coerceValue(it, type.elemType, prjInstanceId) })

        else -> cannotCoerce(value, type)
      }
    }

    is TupleType -> when (value) {
      is TupleValue -> {
        check(type.elemTypes.size == value.values.size)
        val elems = value.values.zip(type.elemTypes).map { (elemValue, elemType) ->
          coerceValue(elemValue, elemType, prjInstanceId)
        }
        TupleValue(elems)
      }

      is NamedTupleValue -> {
        check(type.elemTypes.size == value.pairs.size)
        val elems = value.values().zip(type.elemTypes).map { (elemValue, elemType) ->
          coerceValue(elemValue, elemType, prjInstanceId)
        }
        TupleValue(elems)
      }

      else -> cannotCoerce(value, type)
    }

    is NamedTupleType -> when (value) {
      is NamedTupleValue -> {
        check(type.pairs.size == value.pairs.size)
        check(type.names() == value.names())
        val elems = value.values().zip(type.valueTypes()).map { (elemValue, elemType) ->
          coerceValue(elemValue, elemType, prjInstanceId)
        }
        NamedTupleValue(type.names().zip(elems.requireNoNulls()))
      }

      is TupleValue -> {
        check(type.pairs.size == value.values.size)
        val elems = value.values.zip(type.valueTypes()).map { (elemValue, elemType) ->
          coerceValue(elemValue, elemType, prjInstanceId)
        }
        NamedTupleValue(type.names().zip(elems.requireNoNulls()))
      }

      else -> cannotCoerce(value, type)
    }

    is DataClassType -> {
      when (value) {
        is ClassInstanceValue -> {
          // TODO class instance coercion은 plugin에서 반환된 값에 대해서만 하면 될듯도 하고..?
          if (type.packageName == value.packageName && type.className == value.className) {
            value
          } else {
            // TODO
            cannotCoerce(value, type)
          }
        }

        else -> TODO()
      }
    }

    is SuperClassType -> {
      when (value) {
        is ClassInstanceValue -> {
          if (type.packageName != value.packageName) {
            cannotCoerce(value, type)
          }
          // TODO value의 타입이 super type의 sub type이 맞는지 확인
          value
        }

        else -> TODO()
      }
    }

    is UnionType -> {
      val firstMatch = type.types.firstNotNullOfOrNull { typeCandidate ->
        try {
          coerceValue(value, typeCandidate, prjInstanceId)
        } catch (e: Exception) {
          null
        }
      }
      firstMatch ?: cannotCoerce(value, type)
    }

    NoneType ->
      if (value is NoneValue) value else cannotCoerce(value, type)

    ActionRuleDefType ->
      if (value is ActionRuleDefValue) value else cannotCoerce(value, type)

    BuildRuleDefType ->
      if (value is BuildRuleDefValue) value else cannotCoerce(value, type)

    TypeType ->
      if (value is TypeValue) value else cannotCoerce(value, type)

    is EnumType ->
      if (value is EnumValue && value.packageName == type.packageName && value.enumName == type.enumName) {
        value
      } else {
        cannotCoerce(value, type)
      }
  }
}
