package com.giyeok.bibix.graph2.runner

import com.giyeok.bibix.base.*
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

  fun castValue(value: BibixValue, type: BibixType): BuildTaskResult {
    fun fileFromString(path: String): Path {
      checkNotNull(projectLocation)
      return projectLocation.projectRoot.resolve(path).normalize().absolute()
    }

    if (value == NoneValue) {
      return BuildTaskResult.ValueResult(value)
    }
    if (value is ClassInstanceValue) {
      // class instance value는 기본 coercion으로는 동일한 클래스,

      // TODO value의 as element 중에 적용 가능한게 있는지 확인해서 적용해서 반환
      // (projectId, importInstanceId)의 import instance
      // value의 class가 항상 projectId 안에서 정의된 것이라고 볼 수 있을까?
      // -> 그렇다면 importInstanceId를 사용하는게 말이 되는데..
      // -> 항상이라고 볼수는 없지만, 대부분 그럴 것이긴 함
      //   -> 예를 들어서, 어떤 build rule이 A 타입을 반환할 거라고 해놓고 B 타입을 반환하면 bibix는 B 값을 A 타입으로 캐스트하려고 시도한다

      // TODO 여기서 사용할 ExprEvaluator를 만들기 위한 정보를 함수 프로퍼티로 갖고 있어야 함

      // 사용자가 정의한 custom cast가 있으면 그것부터 시도해보고,
      // 그게 실패하면
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
          is ListValue ->
            castValueList(value.values, type.elemType, ::ListValue)

          is SetValue ->
            castValueList(value.values, type.elemType, ::SetValue)

          else -> cannotCast(value, type)
        }
      }

      is SetType -> {
        when (value) {
          is ListValue ->
            castValueList(value.values, type.elemType, ::SetValue)

          is SetValue ->
            castValueList(value.values, type.elemType, ::SetValue)

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
        if (value is EnumValue && value.packageName == type.packageName && value.enumName == type.enumName) {
          BuildTaskResult.ValueResult(value)
        } else {
          cannotCast(value, type)
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

  private fun castValues(
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

  fun finalizeClassInstanceValue(
    value: ClassInstanceValue,
    dataClass: BuildTaskResult.DataClassResult
  ): BuildTaskResult {
    TODO()
  }
}
