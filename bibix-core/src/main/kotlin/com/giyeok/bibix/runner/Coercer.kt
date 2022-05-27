package com.giyeok.bibix.runner

import com.giyeok.bibix.base.*
import com.giyeok.bibix.buildscript.BuildGraph
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import java.io.File

class Coercer(val buildGraph: BuildGraph, val runner: BuildRunner) {
  private fun fileFromString(origin: SourceId, path: String): File {
    val pathFile = File(path)
    if (pathFile.isAbsolute) {
      return pathFile
    }
    val baseDirectory = buildGraph.baseDirectories.getValue(origin)
    return File(baseDirectory, path)
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
    dclassOrigin: SourceId?,
  ): BibixValue? = if (value is NClassInstanceValue) {
    // ClassInstanceValue로 변환
    checkNotNull(dclassOrigin)
    val resolved = runner.runTask(
      task,
      BuildTask.ResolveName(CName(dclassOrigin, value.nameTokens))
    ) as CNameValue.ClassType
    val instanceValue = ClassInstanceValue(resolved.cname, value.value)
    coerce(task, origin, instanceValue, type, dclassOrigin)
  } else {
    // TODO value is ClassInstanceValue 인데 type은 ClassType이 아닌 경우
    // -> class의 cast 함수 시도 -> value.reality를 coerce
    when (type) {
      AnyType -> value
      BooleanType -> when (value) {
        is BooleanValue -> value
        is ClassInstanceValue -> tryCastClassInstance(task, origin, value, type, dclassOrigin)
        else -> null
      }
      StringType -> when (value) {
        is StringValue -> value
        is ClassInstanceValue ->
          tryCastClassInstance(task, origin, value, type, dclassOrigin)
            ?: StringValue(value.stringify())
        else -> StringValue(value.stringify())
      }
      PathType -> when (value) {
        is PathValue -> value
        is FileValue -> PathValue(value.file)
        is DirectoryValue -> PathValue(value.directory)
        is StringValue -> PathValue(fileFromString(origin, value.value))
        is ClassInstanceValue -> tryCastClassInstance(task, origin, value, type, dclassOrigin)
        else -> null
      }
      FileType -> when (value) {
        is FileValue -> value
        // TODO file value, directory value는 build rule에 주기 직전에 존재하는지/타입 확인하기
        is PathValue -> FileValue(value.path)
        is StringValue -> FileValue(fileFromString(origin, value.value))
        is ClassInstanceValue -> tryCastClassInstance(task, origin, value, type, dclassOrigin)
        else -> null
      }
      DirectoryType -> when (value) {
        is DirectoryValue -> value
        is PathValue -> DirectoryValue(value.path)
        is StringValue -> DirectoryValue(fileFromString(origin, value.value))
        is ClassInstanceValue -> tryCastClassInstance(task, origin, value, type, dclassOrigin)
        else -> null
      }
      is CustomType -> {
        when (val actualType = runner.runTask(task, BuildTask.ResolveName(type.name))) {
          is CNameValue.ClassType ->
            when (value) {
              is ClassInstanceValue ->
                if (actualType.cname == value.className) {
                  coerce(task, origin, value.value, actualType.reality, dclassOrigin)?.let {
                    ClassInstanceValue(actualType.cname, it)
                  }
                } else {
                  val tryCast = tryCastClassInstance(task, origin, value, type, dclassOrigin)
                  if (tryCast != null) tryCast else {
                    val valueClassType = runner.runTask(
                      task,
                      BuildTask.ResolveName(value.className)
                    ) as CNameValue.ClassType
                    val castExprId = valueClassType.casts[CustomType(actualType.cname)]
                    suspend fun tryCoerceToReality() =
                      coerce(task, origin, value, actualType.reality, dclassOrigin)?.let {
                        ClassInstanceValue(actualType.cname, it)
                      }
                    if (castExprId != null) {
                      val castExprGraph = buildGraph.exprGraphs[castExprId]
                      val castResult = runner.runTask(
                        task,
                        BuildTask.EvalExpr(origin, castExprId, castExprGraph.mainNode, value)
                      )
                      coerce(task, origin, castResult as BibixValue, type, dclassOrigin)
                        ?: tryCoerceToReality()
                    } else {
                      tryCoerceToReality()
                    }
                  }
                }
              else -> coerce(task, origin, value, actualType.reality, dclassOrigin)?.let {
                ClassInstanceValue(actualType.cname, it)
              }
            }
          is CNameValue.EnumType -> when (value) {
            is EnumValue -> if (actualType.cname == value.enumTypeName) value else null
            is StringValue ->
              if (actualType.values.contains(value.value)) {
                EnumValue(actualType.cname, value.value)
              } else null
            is ClassInstanceValue -> tryCastClassInstance(task, origin, value, type, dclassOrigin)
            else -> null
          }
          else -> null
        }
      }
      is ListType -> {
        when (value) {
          is ListValue -> withNoNull(value.values.map {
            coerce(task, origin, it, type.elemType, dclassOrigin)
          }) { ListValue(it) }
          is SetValue -> withNoNull(value.values.map {
            coerce(task, origin, it, type.elemType, dclassOrigin)
          }) { ListValue(it) }
          is ClassInstanceValue -> tryCastClassInstance(task, origin, value, type, dclassOrigin)
          else -> null
        }
      }
      is SetType -> {
        when (value) {
          is ListValue -> withNoNull(value.values.map {
            coerce(task, origin, it, type.elemType, dclassOrigin)
          }) { SetValue(it) }
          is SetValue -> withNoNull(value.values.map {
            coerce(task, origin, it, type.elemType, dclassOrigin)
          }) { SetValue(it) }
          is ClassInstanceValue -> tryCastClassInstance(task, origin, value, type, dclassOrigin)
          else -> null
        }
      }
      is TupleType -> {
        suspend fun ifElse() = if (type.elemTypes.size != 1) null else {
          // 길이가 1인 tuple이면 그냥 맞춰서 반환해주기
          coerce(task, origin, value, type.elemTypes[0], dclassOrigin)?.let { TupleValue(it) }
        }
        when (value) {
          is TupleValue -> {
            check(type.elemTypes.size == value.values.size)
            withNoNull(
              value.values.zip(type.elemTypes)
                .map { coerce(task, origin, it.first, it.second, dclassOrigin) }) { TupleValue(it) }
          }
          is NamedTupleValue -> {
            check(type.elemTypes.size == value.pairs.size)
            withNoNull(value.values().zip(type.elemTypes)
              .map { coerce(task, origin, it.first, it.second, dclassOrigin) }) { TupleValue(it) }
          }
          is ClassInstanceValue ->
            tryCastClassInstance(task, origin, value, type, dclassOrigin) ?: ifElse()
          else -> ifElse()
        }
      }
      is NamedTupleType -> {
        suspend fun ifElse() = if (type.pairs.size != 1) null else {
          // 길이가 1인 named tuple이면 그냥 맞춰서 반환해주기
          coerce(task, origin, value, type.pairs[0].second, dclassOrigin)?.let {
            NamedTupleValue(type.pairs[0].first to it)
          }
        }

        when (value) {
          is NamedTupleValue -> {
            check(type.pairs.size == value.pairs.size)
            check(type.names() == value.names())
            withNoNull(value.values().zip(type.valueTypes()).map {
              coerce(task, origin, it.first, it.second, dclassOrigin)
            }) { NamedTupleValue(type.names().zip(it)) }
          }
          is TupleValue -> {
            check(type.pairs.size == value.values.size)
            withNoNull(value.values.zip(type.valueTypes())
              .map { coerce(task, origin, it.first, it.second, dclassOrigin) }) {
              NamedTupleValue(type.names().zip(it))
            }
          }
          is ClassInstanceValue ->
            tryCastClassInstance(task, origin, value, type, dclassOrigin) ?: ifElse()
          else -> ifElse()
        }
      }
      is UnionType -> type.types.firstNotNullOf { coerce(task, origin, value, it, dclassOrigin) }
      ActionRuleDefType -> if (value is ActionRuleDefValue) value else null
      BuildRuleDefType -> if (value is BuildRuleDefValue) value else null
      TypeType -> if (value is TypeValue) value else null
    }
  }

  private suspend fun tryCastClassInstance(
    task: BuildTask,
    origin: SourceId,
    value: ClassInstanceValue,
    type: BibixType,
    dclassOrigin: SourceId?
  ): BibixValue? {
    val classType =
      runner.runTask(task, BuildTask.ResolveName(value.className)) as CNameValue.ClassType
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
