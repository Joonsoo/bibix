package com.giyeok.bibix.runner

import com.giyeok.bibix.base.*
import com.giyeok.bibix.buildscript.BuildGraph
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import java.nio.file.Path

class Coercer(val buildGraph: BuildGraph, val runner: BuildRunner) {
  private fun fileFromString(origin: SourceId, path: String): Path {
    val pathFile = runner.repo.fileSystem.getPath(path)
    if (pathFile.isAbsolute) {
      return pathFile
    }
    val baseDirectory = buildGraph.baseDirectories.getValue(origin)
    return baseDirectory.resolve(path)
  }

  private fun withNoNull(
    elems: List<BibixValue?>,
    block: (List<BibixValue>) -> BibixValue
  ): BibixValue? = if (elems.contains(null)) null else block(elems.map { it!! })

  suspend fun coerce(
    task: BuildTask,
    origin: SourceId,
    value: BibixValue,
    type: BibixType,
    // value가 NClassInstanceValue일 수 있을 때 그 이름을 어디서 찾아야되는지
    nclassOrigin: SourceId?,
  ): BibixValue? {
    suspend fun coerceClassFields(
      fields: List<CNameValue.ClassField>,
      values: Map<String, BibixValue>
    ): Map<String, BibixValue>? {
      return fields.mapNotNull { field ->
        val fieldValue = values[field.name]
        if (fieldValue == null && !field.optional) {
          return null
        }
        fieldValue?.let {
          val coercedValue =
            coerce(task, origin, fieldValue, field.type, nclassOrigin)
              ?: return null
          field.name to coercedValue
        }
      }.toMap()
    }

    return if (value is NDataClassInstanceValue) {
      // ClassInstanceValue로 변환
      checkNotNull(nclassOrigin)
      val resolved = runner.runTask(
        task,
        BuildTask.ResolveName(CName(nclassOrigin, value.nameTokens))
      ) as CNameValue.DataClassType
      val fieldValues: Map<String, BibixValue> =
        coerceClassFields(resolved.fields, value.fieldValues) ?: return null
      val instanceValue = DataClassInstanceValue(resolved.cname, fieldValues)
      coerce(task, origin, instanceValue, type, nclassOrigin)
    } else {
      // TODO value is ClassInstanceValue 인데 type은 ClassType이 아닌 경우
      // -> class의 cast 함수 시도 -> value.reality를 coerce
      when (type) {
        AnyType -> value
        BooleanType -> when (value) {
          is BooleanValue -> value
          is DataClassInstanceValue ->
            tryCastClassInstance(task, origin, value, type, nclassOrigin)
          else -> null
        }
        StringType -> when (value) {
          is StringValue -> value
          is DataClassInstanceValue ->
            tryCastClassInstance(task, origin, value, type, nclassOrigin)
              ?: StringValue(value.stringify())
          else -> StringValue(value.stringify())
        }
        PathType -> when (value) {
          is PathValue -> value
          is FileValue -> PathValue(value.file)
          is DirectoryValue -> PathValue(value.directory)
          is StringValue -> PathValue(fileFromString(origin, value.value))
          is DataClassInstanceValue ->
            tryCastClassInstance(task, origin, value, type, nclassOrigin)
          else -> null
        }
        FileType -> when (value) {
          is FileValue -> value
          // TODO file value, directory value는 build rule에 주기 직전에 존재하는지/타입 확인하기
          is PathValue -> FileValue(value.path)
          is StringValue -> FileValue(fileFromString(origin, value.value))
          is DataClassInstanceValue ->
            tryCastClassInstance(task, origin, value, type, nclassOrigin)
          else -> null
        }
        DirectoryType -> when (value) {
          is DirectoryValue -> value
          is PathValue -> DirectoryValue(value.path)
          is StringValue -> DirectoryValue(fileFromString(origin, value.value))
          is DataClassInstanceValue ->
            tryCastClassInstance(task, origin, value, type, nclassOrigin)
          else -> null
        }
        is CustomType -> {
          when (val actualType = runner.runTask(task, BuildTask.ResolveName(type.name))) {
            is CNameValue.DataClassType -> {
              suspend fun tupleToClass(
                values: List<BibixValue>,
                actualType: CNameValue.DataClassType,
              ): DataClassInstanceValue? {
                val fieldValues = actualType.fields.zip(values)
                  .mapNotNull { (field, value) ->
                    val fieldType = if (field.optional) {
                      if (field.type is UnionType) {
                        UnionType(field.type.types + NoneType)
                      } else {
                        UnionType(listOf(field.type, NoneType))
                      }
                    } else field.type
                    val coerced =
                      coerce(task, origin, value, fieldType, nclassOrigin) ?: return null
                    if (field.optional && coerced == NoneValue) {
                      null
                    } else {
                      field.name to coerced
                    }
                  }.toMap()
                return DataClassInstanceValue(type.name, fieldValues)
              }
              when (value) {
                is DataClassInstanceValue -> {
                  if (value.className != actualType.cname) {
                    tryCastClassInstance(task, origin, value, type, nclassOrigin)
                  } else {
                    val fieldValues: Map<String, BibixValue> =
                      coerceClassFields(actualType.fields, value.fieldValues) ?: return null
                    DataClassInstanceValue(value.className, fieldValues)
                  }
                }
                is NamedTupleValue ->
                  if (value.pairs.map { it.first } != actualType.fields.map { it.name }) {
                    null
                  } else {
                    tupleToClass(value.pairs.map { it.second }, actualType)
                  }
                is TupleValue ->
                  if (value.values.size != actualType.fields.size) {
                    null
                  } else {
                    tupleToClass(value.values, actualType)
                  }
                else -> null
              }
            }
            is CNameValue.SuperClassType ->
              when (value) {
                is DataClassInstanceValue -> {
                  // TODO sub class -> sub class 인 경우 처리
                  val subTypes =
                    runner.runTasks(task, actualType.subs.map { BuildTask.ResolveName(it.name) })
                      .map { it as CNameValue.DataClassType }
                  if (subTypes.any { it.cname == value.className }) value else null
                }
                else -> null
              }
            is CNameValue.EnumType -> when (value) {
              is EnumValue -> if (actualType.cname == value.enumTypeName) value else null
              is StringValue ->
                if (actualType.values.contains(value.value)) {
                  EnumValue(actualType.cname, value.value)
                } else null
              is DataClassInstanceValue ->
                tryCastClassInstance(task, origin, value, type, nclassOrigin)
              else -> null
            }
            else -> null
          }
        }
        is ListType -> {
          when (value) {
            is ListValue -> withNoNull(value.values.map {
              coerce(task, origin, it, type.elemType, nclassOrigin)
            }) { ListValue(it) }
            is SetValue -> withNoNull(value.values.map {
              coerce(task, origin, it, type.elemType, nclassOrigin)
            }) { ListValue(it) }
            is DataClassInstanceValue ->
              tryCastClassInstance(task, origin, value, type, nclassOrigin)
            else -> null
          }
        }
        is SetType -> {
          when (value) {
            is ListValue -> withNoNull(value.values.map {
              coerce(task, origin, it, type.elemType, nclassOrigin)
            }) { SetValue(it) }
            is SetValue -> withNoNull(value.values.map {
              coerce(task, origin, it, type.elemType, nclassOrigin)
            }) { SetValue(it) }
            is DataClassInstanceValue ->
              tryCastClassInstance(task, origin, value, type, nclassOrigin)
            else -> null
          }
        }
        is TupleType -> {
          suspend fun ifElse() = if (type.elemTypes.size != 1) null else {
            // 길이가 1인 tuple이면 그냥 맞춰서 반환해주기
            coerce(task, origin, value, type.elemTypes[0], nclassOrigin)?.let { TupleValue(it) }
          }
          when (value) {
            is TupleValue -> {
              check(type.elemTypes.size == value.values.size)
              withNoNull(
                value.values.zip(type.elemTypes)
                  .map {
                    coerce(task, origin, it.first, it.second, nclassOrigin)
                  }) { TupleValue(it) }
            }
            is NamedTupleValue -> {
              check(type.elemTypes.size == value.pairs.size)
              withNoNull(value.values().zip(type.elemTypes)
                .map {
                  coerce(task, origin, it.first, it.second, nclassOrigin)
                }) { TupleValue(it) }
            }
            is DataClassInstanceValue ->
              tryCastClassInstance(task, origin, value, type, nclassOrigin) ?: ifElse()
            else -> ifElse()
          }
        }
        is NamedTupleType -> {
          suspend fun ifElse() = if (type.pairs.size != 1) null else {
            // 길이가 1인 named tuple이면 그냥 맞춰서 반환해주기
            coerce(task, origin, value, type.pairs[0].second, nclassOrigin)?.let {
              NamedTupleValue(type.pairs[0].first to it)
            }
          }

          when (value) {
            is NamedTupleValue -> {
              check(type.pairs.size == value.pairs.size)
              check(type.names() == value.names())
              withNoNull(value.values().zip(type.valueTypes()).map {
                coerce(task, origin, it.first, it.second, nclassOrigin)
              }) { NamedTupleValue(type.names().zip(it)) }
            }
            is TupleValue -> {
              check(type.pairs.size == value.values.size)
              withNoNull(value.values.zip(type.valueTypes())
                .map { coerce(task, origin, it.first, it.second, nclassOrigin) }) {
                NamedTupleValue(type.names().zip(it))
              }
            }
            is DataClassInstanceValue ->
              tryCastClassInstance(task, origin, value, type, nclassOrigin) ?: ifElse()
            else -> ifElse()
          }
        }
        is UnionType -> {
          val firstMatch = type.types.firstNotNullOfOrNull {
            coerce(task, origin, value, it, nclassOrigin)
          }
          if (firstMatch == null) {
            println("${type.types} $value")
          }
          firstMatch!!
        }
        NoneType -> if (value is NoneValue) value else null
        ActionRuleDefType -> if (value is ActionRuleDefValue) value else null
        BuildRuleDefType -> if (value is BuildRuleDefValue) value else null
        TypeType -> if (value is TypeValue) value else null
      }
    }
  }

  private suspend fun tryCastClassInstance(
    task: BuildTask,
    origin: SourceId,
    value: DataClassInstanceValue,
    type: BibixType,
    dclassOrigin: SourceId?
  ): BibixValue? {
    val classType =
      runner.runTask(task, BuildTask.ResolveName(value.className)) as CNameValue.DataClassType
    val castExprId = classType.casts[type] ?: return null
    val castExprGraph = buildGraph.exprGraphs[castExprId]
    val castResult = runner.runTask(
      task,
      BuildTask.EvalExpr(origin, castExprId, castExprGraph.mainNode, value)
    )
    return coerce(task, origin, castResult as BibixValue, type, dclassOrigin)
  }

  suspend fun toBibixValue(task: BuildTask, value: Any): BibixValue = when (value) {
    is BibixValue -> value
    is CNameValue.ClassType -> TypeValue.ClassTypeValue(value.cname)
    is CNameValue.EnumType -> TypeValue.EnumTypeValue(value.cname, value.values)
    is CNameValue.BuildRuleValue -> {
      val paramTypes = toTypeValues(task, value.params.map { it.type })
      val params = value.params.zip(paramTypes).map { (param, paramType) ->
        RuleParam(param.name, paramType, param.optional)
      }
      BuildRuleDefValue(
        value.cname,
        params,
        value.implName,
        value.className,
        value.methodName ?: "build"
      )
    }
    is BuildRuleImplInfo.UserBuildRuleImplInfo -> toBibixValue(task, value.buildRuleValue)
    is CNameValue.ActionRuleValue -> {
      val paramTypes = toTypeValues(task, value.params.map { it.type })
      val params = value.params.zip(paramTypes).map { (param, paramType) ->
        RuleParam(param.name, paramType, param.optional)
      }
      ActionRuleDefValue(
        value.cname,
        params,
        value.implName,
        value.className,
        value.methodName ?: "run"
      )
    }
    is ActionRuleImplInfo -> toBibixValue(task, value.actionRuleValue)
    else -> TODO()
  }

  suspend fun toBibixValues(task: BuildTask, values: List<Any>): List<BibixValue> =
    coroutineScope {
      values.map {
        async(currentCoroutineContext()) {
          toBibixValue(task, it)
        }
      }
    }.awaitAll()

  suspend fun toTypeValue(task: BuildTask, type: BibixType): TypeValue = when (type) {
    AnyType -> TypeValue.AnyTypeValue
    BooleanType -> TypeValue.BooleanTypeValue
    StringType -> TypeValue.StringTypeValue
    PathType -> TypeValue.PathTypeValue
    FileType -> TypeValue.FileTypeValue
    DirectoryType -> TypeValue.DirectoryTypeValue
    is CustomType -> {
      when (val resolved = runner.runTask(task, BuildTask.ResolveName(type.name))) {
        is CNameValue.ClassType -> TypeValue.ClassTypeValue(resolved.cname)
        is CNameValue.EnumType -> TypeValue.EnumTypeValue(resolved.cname, resolved.values)
        else -> throw BibixBuildException(task, "Failed to find class or enum type ${type.name}")
      }
    }
    is ListType -> TypeValue.ListTypeValue(toTypeValue(task, type.elemType))
    is SetType -> TypeValue.SetTypeValue(toTypeValue(task, type.elemType))
    is TupleType -> TypeValue.TupleTypeValue(toTypeValues(task, type.elemTypes))
    is NamedTupleType -> {
      val elemTypes = toTypeValues(task, type.pairs.map { it.second })
      TypeValue.NamedTupleTypeValue(type.names().zip(elemTypes))
    }
    is UnionType -> TypeValue.UnionTypeValue(toTypeValues(task, type.types))
    NoneType -> TypeValue.NoneTypeValue
    BuildRuleDefType -> TypeValue.BuildRuleDefTypeValue
    ActionRuleDefType -> TypeValue.ActionRuleDefTypeValue
    TypeType -> TypeValue.TypeTypeValue
  }

  suspend fun toTypeValues(task: BuildTask, types: List<BibixType>): List<TypeValue> =
    coroutineScope {
      types.map {
        async(currentCoroutineContext()) { toTypeValue(task, it) }
      }
    }.awaitAll()
}
