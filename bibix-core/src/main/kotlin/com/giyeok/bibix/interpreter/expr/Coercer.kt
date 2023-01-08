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
  private fun coercionFailed() = IllegalStateException("Cannot coerce value")

  private fun withNoNull(
    elems: List<BibixValue?>,
    block: (List<BibixValue>) -> BibixValue
  ): BibixValue = if (elems.contains(null)) {
    throw coercionFailed()
  } else {
    block(elems.map { it!! })
  }

  suspend fun coerce(
    task: Task,
    context: NameLookupContext,
    value: BibixValue,
    type: BibixType
  ): BibixValue {
    check(value !is NClassInstanceValue)
    return when (type) {
      AnyType -> value
      BooleanType -> when (value) {
        is BooleanValue -> value
        else -> TODO()
      }

      StringType -> when (value) {
        is StringValue -> value
        else -> throw IllegalStateException("Coercion failed: $value to $type")
      }

      PathType -> when (value) {
        is PathValue -> value
        is FileValue -> PathValue(value.file)
        is DirectoryValue -> PathValue(value.directory)
        is StringValue -> PathValue(fileFromString(context.sourceId, value.value))
        else -> TODO()
      }

      FileType -> when (value) {
        is FileValue -> value
        // TODO file value, directory value는 build rule에 주기 직전에 존재하는지/타입 확인하기
        is PathValue -> FileValue(value.path)
        is StringValue -> FileValue(fileFromString(context.sourceId, value.value))
        else -> TODO()
      }

      DirectoryType -> when (value) {
        is DirectoryValue -> value
        is PathValue -> DirectoryValue(value.path)
        is StringValue -> DirectoryValue(fileFromString(context.sourceId, value.value))
        else -> TODO()
      }

      is ListType -> {
        when (value) {
          is ListValue -> withNoNull(value.values.map {
            coerce(task, context, it, type.elemType)
          }) { ListValue(it) }

          is SetValue -> withNoNull(value.values.map {
            coerce(task, context, it, type.elemType)
          }) { ListValue(it) }

          else -> TODO()
        }
      }

      is SetType -> {
        when (value) {
          is ListValue -> withNoNull(value.values.map {
            coerce(task, context, it, type.elemType)
          }) { SetValue(it) }

          is SetValue -> withNoNull(value.values.map {
            coerce(task, context, it, type.elemType)
          }) { SetValue(it) }

          else -> TODO()
        }
      }

      is TupleType -> {
        suspend fun ifElse(): BibixValue = if (type.elemTypes.size != 1) {
          throw IllegalStateException("Cannot coerce")
        } else {
          // 길이가 1인 tuple이면 그냥 맞춰서 반환해주기
          TupleValue(coerce(task, context, value, type.elemTypes[0]))
        }

        when (value) {
          is TupleValue -> {
            check(type.elemTypes.size == value.values.size)
            withNoNull(
              value.values.zip(type.elemTypes)
                .map {
                  coerce(task, context, it.first, it.second)
                }) { TupleValue(it) }
          }

          is NamedTupleValue -> {
            check(type.elemTypes.size == value.pairs.size)
            withNoNull(value.values().zip(type.elemTypes)
              .map {
                coerce(task, context, it.first, it.second)
              }) { TupleValue(it) }
          }

          else -> ifElse()
        }
      }

      is NamedTupleType -> {
        suspend fun ifElse() =
          if (type.pairs.size != 1) throw coercionFailed() else {
            // 길이가 1인 named tuple이면 그냥 맞춰서 반환해주기
            val value = coerce(task, context, value, type.pairs[0].second)
            NamedTupleValue(type.pairs[0].first to value)
          }

        when (value) {
          is NamedTupleValue -> {
            check(type.pairs.size == value.pairs.size)
            check(type.names() == value.names())
            withNoNull(value.values().zip(type.valueTypes()).map {
              coerce(task, context, it.first, it.second)
            }) { NamedTupleValue(type.names().zip(it)) }
          }

          is TupleValue -> {
            check(type.pairs.size == value.values.size)
            withNoNull(value.values.zip(type.valueTypes())
              .map { coerce(task, context, it.first, it.second) }) {
              NamedTupleValue(type.names().zip(it))
            }
          }

          else -> ifElse()
        }
      }

      is DataClassType -> {
        when (value) {
          is ClassInstanceValue -> {
            check(value.packageName == type.packageName && value.className == type.className)
            value
          }

          else -> TODO()
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
            value
          }

          else -> TODO()
        }
      }

      is UnionType -> {
        val firstMatch = type.types.firstNotNullOfOrNull {
          coerce(task, context, value, it)
        }
        if (firstMatch == null) {
          println("${type.types} $value")
        }
        firstMatch!!
      }

      NoneType -> if (value is NoneValue) value else throw coercionFailed()
      ActionRuleDefType -> if (value is ActionRuleDefValue) value else throw coercionFailed()
      BuildRuleDefType -> if (value is BuildRuleDefValue) value else throw coercionFailed()
      TypeType -> if (value is TypeValue) value else throw coercionFailed()
      is EnumType -> TODO()
    }
  }

  private suspend fun fileFromString(sourceId: SourceId, path: String): Path {
    return sourceManager.getProjectRoot(sourceId).resolve(path).normalize()
  }
}
