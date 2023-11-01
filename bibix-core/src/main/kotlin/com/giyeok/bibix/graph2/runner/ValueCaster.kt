package com.giyeok.bibix.graph2.runner

import com.giyeok.bibix.base.*
import com.giyeok.bibix.graph2.BibixName
import com.giyeok.bibix.graph2.BibixProjectLocation
import java.nio.file.Path
import kotlin.io.path.absolute
import kotlin.io.path.absolutePathString

class ValueCaster(
  val projectId: Int,
  val projectLocation: BibixProjectLocation?,
  val importInstanceId: Int,
  val exprEvaluatorGen: (ClassInstanceValue) -> ExprEvaluator,
) {
  private fun cannotCast(value: BibixValue, type: BibixType) =
    BuildTaskResult.TypeCastFailResult(value, type)

  // castValue로 오는 ClassInstanceValue는 모두 각 필드가 적당한 타입으로 cast되고, default field와 optional field가 처리된 것으로 본다.
  fun castValue(value: BibixValue, type: BibixType): BuildTaskResult {
    fun fileFromString(path: String): Path {
      checkNotNull(projectLocation)
      return projectLocation.projectRoot.resolve(path).normalize().absolute()
    }

    if (value == NoneValue) {
      return BuildTaskResult.ValueResult(value)
    }
    if (value is ClassInstanceValue) {
      // 사용자가 정의한 custom cast가 있으면 그것부터 시도해보고, 그게 실패하면 기본 캐스팅 시도
      when (type) {
        is DataClassType -> {
          if (type.packageName == value.packageName && type.className == value.className) {
            // type과 일치하고, value가 이미 온전한 것이 확인되었으므로 값을 그대로 반환
            return BuildTaskResult.ValueResult(value)
          }
        }

        is SuperClassType -> {
          if (type.packageName == value.packageName) {
            // TODO value가 type의 sub type의 클래스이면 그대로 반환 - 그 외의 경우엔 계속 진행
            TODO()
          }
        }

        else -> {
          // do nothing - fallthrough
        }
      }
      return tryCustomCast(value, type) { result ->
        when (result) {
          is BuildTaskResult.ValueResult -> result
          is BuildTaskResult.TypeCastFailResult -> {
            when (type) {
              is DataClassType -> {
                if (type.packageName != value.packageName || type.className != value.className) {
                  cannotCast(value, type)
                } else {
                  // TODO field들에 대한 처리
                  //  - 지금은 일단 그냥 넘겨줌
                  BuildTaskResult.ValueResult(value)
                }
              }

              is SuperClassType -> {
                if (type.packageName != value.packageName) {
                  cannotCast(value, type)
                } else {
                  // TODO
                  BuildTaskResult.ValueResult(value)
                }
              }

              is TupleType -> TODO()
              is NamedTupleType -> TODO()
              else -> cannotCast(value, type)
            }
          }

          else -> throw AssertionError()
        }
      }
    }
    return when (type) {
      AnyType -> BuildTaskResult.ValueResult(value)
      BooleanType -> when (value) {
        is BooleanValue -> BuildTaskResult.ValueResult(value)
        else -> cannotCast(value, type)
      }

      StringType -> when (value) {
        is StringValue -> BuildTaskResult.ValueResult(value)
        is PathValue -> BuildTaskResult.ValueResult(StringValue(value.path.absolutePathString()))
        is FileValue -> BuildTaskResult.ValueResult(StringValue(value.file.absolutePathString()))
        is DirectoryValue -> BuildTaskResult.ValueResult(StringValue(value.directory.absolutePathString()))
        else -> cannotCast(value, type)
      }

      PathType -> when (value) {
        is PathValue -> BuildTaskResult.ValueResult(value)
        is FileValue -> BuildTaskResult.ValueResult(PathValue(value.file))
        is DirectoryValue -> BuildTaskResult.ValueResult(PathValue(value.directory))
        is StringValue -> BuildTaskResult.ValueResult(PathValue(fileFromString(value.value)))
        else -> cannotCast(value, type)
      }

      FileType -> when (value) {
        is FileValue -> BuildTaskResult.ValueResult(value)
        // TODO file value, directory value는 build rule에 주기 직전에 존재하는지/타입 확인하기 - 왜 여기서 하면 안되지..?
        is PathValue -> BuildTaskResult.ValueResult(FileValue(value.path))
        is StringValue -> BuildTaskResult.ValueResult(FileValue(fileFromString(value.value)))
        else -> cannotCast(value, type)
      }

      DirectoryType -> when (value) {
        is DirectoryValue -> BuildTaskResult.ValueResult(value)
        is PathValue -> BuildTaskResult.ValueResult(DirectoryValue(value.path))
        is StringValue -> BuildTaskResult.ValueResult(DirectoryValue(fileFromString(value.value)))
        else -> cannotCast(value, type)
      }

      is ListType -> {
        when (value) {
          is ListValue -> castValueList(value.values, type.elemType, ::ListValue)
          is SetValue -> castValueList(value.values, type.elemType, ::SetValue)
          else -> cannotCast(value, type)
        }
      }

      is SetType -> {
        when (value) {
          is ListValue -> castValueList(value.values, type.elemType, ::SetValue)
          is SetValue -> castValueList(value.values, type.elemType, ::SetValue)
          else -> cannotCast(value, type)
        }
      }

      is TupleType -> when (value) {
        is TupleValue -> {
          check(type.elemTypes.size == value.values.size)
          castValuePairs(value.values.zip(type.elemTypes), ::TupleValue)
        }

        is NamedTupleValue -> {
          check(type.elemTypes.size == value.pairs.size)
          castValuePairs(value.values.zip(type.elemTypes), ::TupleValue)
        }

        else -> cannotCast(value, type)
      }

      is NamedTupleType -> when (value) {
        is NamedTupleValue -> {
          check(type.pairs.size == value.pairs.size)
          check(type.names == value.names)
          castValuePairs(value.values.zip(type.valueTypes)) { results ->
            NamedTupleValue(type.names.zip(results))
          }
        }

        is TupleValue -> {
          check(type.pairs.size == value.values.size)
          castValuePairs(value.values.zip(type.valueTypes)) { results ->
            NamedTupleValue(type.names.zip(results))
          }
        }

        else -> cannotCast(value, type)
      }

      is DataClassType -> {
        when (value) {
          is TupleValue -> {
            TODO()
          }

          is NamedTupleValue -> {
            TODO()
          }

          else -> TODO()
        }
      }

      is SuperClassType -> {
        // value가 ClassInstanceValue인 경우는 이미 위에서 처리되었음
        // 그 외의 경우엔 super class type으로 coercion 불가능
        cannotCast(value, type)
      }

      is UnionType -> {
        fun tryCandidateAt(candidateIdx: Int): BuildTaskResult =
          if (candidateIdx >= type.types.size) {
            cannotCast(value, type)
          } else {
            BuildTaskResult.WithResult(
              TypeCastValue(value, type.types[candidateIdx], projectId, importInstanceId)
            ) { result ->
              when (result) {
                is BuildTaskResult.ValueResult -> result
                else -> tryCandidateAt(candidateIdx + 1)
              }
            }
          }
        tryCandidateAt(0)
      }

      NoneType ->
        if (value is NoneValue) BuildTaskResult.ValueResult(value)
        else cannotCast(value, type)

      ActionRuleDefType ->
        if (value is ActionRuleDefValue) BuildTaskResult.ValueResult(value)
        else cannotCast(value, type)

      BuildRuleDefType ->
        if (value is BuildRuleDefValue) BuildTaskResult.ValueResult(value)
        else cannotCast(value, type)

      TypeType ->
        if (value is TypeValue) BuildTaskResult.ValueResult(value)
        else cannotCast(value, type)

      is EnumType ->
        when (value) {
          is EnumValue -> {
            if (value.packageName == type.packageName && value.enumName == type.enumName) {
              BuildTaskResult.ValueResult(value)
            } else {
              cannotCast(value, type)
            }
          }

          is StringValue -> {
            // enum type의 member name 중에 같은 값이 있으면 그 enum 값으로 변경
            TODO()
          }

          else -> cannotCast(value, type)
        }
    }
  }

  private fun castValueList(
    values: List<BibixValue>,
    type: BibixType,
    func: (List<BibixValue>) -> BibixValue
  ): BuildTaskResult =
    BuildTaskResult.WithResultList(values.map {
      TypeCastValue(it, type, projectId, importInstanceId)
    }) { results ->
      check(results.all { it is BuildTaskResult.ValueResult })
      val finalValue = func(results.map { (it as BuildTaskResult.ValueResult).value })
      BuildTaskResult.ValueResult(finalValue)
    }

  private fun castValuePairs(
    valueAndTypes: List<Pair<BibixValue, BibixType>>,
    func: (List<BibixValue>) -> BibixValue
  ): BuildTaskResult =
    BuildTaskResult.WithResultList(valueAndTypes.map { (value, type) ->
      TypeCastValue(value, type, projectId, importInstanceId)
    }) { results ->
      check(results.all { it is BuildTaskResult.ValueResult })
      val finalValue = func(results.map { (it as BuildTaskResult.ValueResult).value })
      BuildTaskResult.ValueResult(finalValue)
    }

  private fun finalizeValueList(
    buildRule: BuildTaskResult.BuildRuleResult,
    values: List<BibixValue>,
    func: (List<BibixValue>) -> BibixValue
  ): BuildTaskResult =
    BuildTaskResult.WithResultList(values.map {
      FinalizeBuildRuleReturnValue(buildRule, it, projectId, importInstanceId)
    }) { results ->
      if (results.all { it is BuildTaskResult.ValueResult }) {
        val finalValue = func(results.map { (it as BuildTaskResult.ValueResult).value })
        BuildTaskResult.ValueResult(finalValue)
      } else {
        BuildTaskResult.ValueFinalizeFailResult()
      }
    }

  private fun finalizeValues(
    values: Map<String, BibixValue>,
    types: Map<String, BibixType>,
    func: (Map<String, BibixValue>) -> BibixValue
  ): BuildTaskResult {
    check(values.keys == types.keys)
    val keys = values.keys.sorted()
    return castValuePairs(keys.map { Pair(values.getValue(it), types.getValue(it)) }) { results ->
      func(keys.zip(results).toMap())
    }
  }

  private fun tryCustomCast(
    value: ClassInstanceValue,
    type: BibixType,
    func: (BuildTaskResult) -> BuildTaskResult
  ): BuildTaskResult {
    // TODO exprEvaluatorGen(value).evaluateExpr()
    return func(BuildTaskResult.TypeCastFailResult(value, type))
  }

  fun finalizeBuildRuleReturnValue(
    buildRule: BuildTaskResult.BuildRuleResult,
    value: BibixValue,
  ): BuildTaskResult =
    when (value) {
      is NClassInstanceValue -> {
        // TODO buildRule 위치를 기준으로 ClassInstanceValue로 변경해서 다시 finalizeBuildRuleReturnValue로 보내기
        TODO()
      }

      is ClassInstanceValue -> {
        // TODO value가 ClassInstanceValue이면 default value, optional value들 채우고 기존 필드들도 목표 타입으로 캐스팅해서 반환
        BuildTaskResult.WithResult(
          EvalDataClassByName(value.packageName, value.className)
        ) { dataClassResult ->
          check(dataClassResult is BuildTaskResult.DataClassResult)

          organizeParamsForDataClass(dataClassResult, listOf(), value.fieldValues)
        }
      }

      is CollectionValue -> {
        finalizeValueList(buildRule, value.values) { elems ->
          value.newCollectionWith(elems)
        }
      }

      is TupleValue -> {
        finalizeValueList(buildRule, value.values) { elems ->
          TupleValue(elems)
        }
      }

      is NamedTupleValue -> {
        finalizeValueList(buildRule, value.values) { elems ->
          NamedTupleValue(value.names.zip(elems))
        }
      }

      else -> BuildTaskResult.ValueResult(value)
    }
}
