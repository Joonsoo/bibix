package com.giyeok.bibix.interpreter.expr

import com.giyeok.bibix.base.*
import com.giyeok.bibix.interpreter.*
import com.giyeok.bibix.interpreter.name.NameLookupContext
import com.giyeok.bibix.interpreter.task.Task
import java.nio.file.Path

class Coercer(
  private val sourceManager: SourceManager,
  private val exprEvaluator: ExprEvaluator,
) {
  suspend fun coerce(
    task: Task,
    context: NameLookupContext,
    value: BibixValue,
    type: BibixType
  ): BibixValue = tryCoerce(task, context, value, type)
    ?: throw IllegalStateException("Coercion failed: $value to $type")


  suspend fun tryCoerce(
    task: Task,
    context: NameLookupContext,
    value: BibixValue,
    type: BibixType
  ): BibixValue? {
    check(value !is NClassInstanceValue)
    when (type) {
      AnyType -> return value
      BooleanType -> when (value) {
        is BooleanValue -> return value
        else -> {}
      }

      StringType -> when (value) {
        is StringValue -> return value
        else -> {}
      }

      PathType -> when (value) {
        is PathValue -> return value
        is FileValue -> return PathValue(value.file)
        is DirectoryValue -> return PathValue(value.directory)
        is StringValue -> return PathValue(fileFromString(context.sourceId, value.value))
        else -> {}
      }

      FileType -> when (value) {
        is FileValue -> return value
        // TODO file value, directory value는 build rule에 주기 직전에 존재하는지/타입 확인하기
        is PathValue -> return FileValue(value.path)
        is StringValue -> return FileValue(fileFromString(context.sourceId, value.value))
        else -> {}
      }

      DirectoryType -> when (value) {
        is DirectoryValue -> return value
        is PathValue -> return DirectoryValue(value.path)
        is StringValue -> return DirectoryValue(fileFromString(context.sourceId, value.value))
        else -> {}
      }

      is ListType -> {
        when (value) {
          is ListValue -> {
            return ListValue(value.values.map {
              coerce(task, context, it, type.elemType)
            })
          }

          is SetValue -> {
            return ListValue(value.values.map {
              coerce(task, context, it, type.elemType)
            })
          }

          else -> {}
        }
      }

      is SetType -> {
        when (value) {
          is ListValue -> {
            return SetValue(value.values.map {
              coerce(task, context, it, type.elemType)
            })
          }

          is SetValue -> {
            return SetValue(value.values.map {
              coerce(task, context, it, type.elemType)
            })
          }

          else -> {}
        }
      }

      is TupleType -> {
        when (value) {
          is TupleValue -> {
            check(type.elemTypes.size == value.values.size)
            val elems = value.values.zip(type.elemTypes).map {
              tryCoerce(task, context, it.first, it.second)
            }
            if (!elems.contains(null)) {
              return TupleValue(elems.requireNoNulls())
            }
          }

          is NamedTupleValue -> {
            check(type.elemTypes.size == value.pairs.size)
            val elems = value.values().zip(type.elemTypes).map {
              tryCoerce(task, context, it.first, it.second)
            }
            if (!elems.contains(null)) {
              return TupleValue(elems.requireNoNulls())
            }
          }

          else -> {}
        }
      }

      is NamedTupleType -> {
        when (value) {
          is NamedTupleValue -> {
            check(type.pairs.size == value.pairs.size)
            check(type.names() == value.names())
            val elems = value.values().zip(type.valueTypes()).map {
              tryCoerce(task, context, it.first, it.second)
            }
            if (!elems.contains(null)) {
              return NamedTupleValue(type.names().zip(elems.requireNoNulls()))
            }
          }

          is TupleValue -> {
            check(type.pairs.size == value.values.size)
            val elems = value.values.zip(type.valueTypes()).map {
              tryCoerce(task, context, it.first, it.second)
            }
            if (!elems.contains(null)) {
              return NamedTupleValue(type.names().zip(elems.requireNoNulls()))
            }
          }

          else -> {}
        }
      }

      is DataClassType -> {
        when (value) {
          is ClassInstanceValue -> {
            check(value.packageName == type.packageName && value.className == type.className)
            return value
          }

          else -> {}
        }
      }

      is SuperClassType -> {
        when (value) {
          is ClassInstanceValue -> {
            check(value.packageName == type.packageName)
            val typeDefinedContext = NameLookupContext(
              sourceManager.getSourceIdFromPackageName(type.packageName)!!,
              listOf()
            )
            val valueClassName = value.className.split('.')
            val typeClassName = type.className.split('.')
            check(valueClassName.dropLast(1) == typeClassName.dropLast(1))

            val superClassDef =
              exprEvaluator.evaluateName(task, typeDefinedContext, typeClassName, null)
            check(superClassDef is EvaluationResult.SuperClassDef)
            // TODO recursive - superClass쪽에서 아래로 내려가야 함. data class에는 super class 정보가 없기 때문에
            check(superClassDef.subClasses.contains(valueClassName.last()))
            // TODO check if it is subtype
            return value
          }

          else -> {}
        }
      }

      is UnionType -> {
        val firstMatch = type.types.firstNotNullOfOrNull {
          coerce(task, context, value, it)
        }
        if (firstMatch == null) {
          println("${type.types} $value")
        }
        return firstMatch!!
      }

      NoneType -> return if (value is NoneValue) value else null
      ActionRuleDefType -> return if (value is ActionRuleDefValue) value else null
      BuildRuleDefType -> return if (value is BuildRuleDefValue) value else null
      TypeType -> return if (value is TypeValue) value else null
      is EnumType -> TODO()
    }
    return tryCoerceFromValue(task, context, value, type)
  }

  private suspend fun fileFromString(sourceId: SourceId, path: String): Path {
    return sourceManager.getProjectRoot(sourceId).resolve(path).normalize()
  }

  private fun tryCoerceFromValue(
    task: Task,
    context: NameLookupContext,
    value: BibixValue,
    type: BibixType
  ): BibixValue? {
    // value로부터 type으로 coerce해보기
    // 특히 value가 ClassInstanceValue일 때 호환되는 cast expression이 있으면 사용해서 변환

    // Tuple일 때: 길이가 1인 tuple이면 그냥 맞춰서 반환해주기
    // TupleValue(coerce(task, context, value, type.elemTypes[0]))

    // NamedTuple일 때: 길이가 1인 named tuple이면 그냥 맞춰서 반환해주기
    // val value = coerce(task, context, value, type.pairs[0].second)
    // NamedTupleValue(type.pairs[0].first to value)

    return null
  }
}
