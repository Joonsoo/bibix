package com.giyeok.bibix.graph.runner

import com.giyeok.bibix.base.*
import com.giyeok.bibix.graph.BibixName
import com.giyeok.bibix.graph.BibixProjectLocation
import org.apache.tools.ant.types.resources.Union
import java.nio.file.Path
import kotlin.io.path.absolute
import kotlin.io.path.absolutePathString

class ValueCaster(
  val buildGraphRunner: BuildGraphRunner,
  val projectId: Int,
) {
  val projectLocation: BibixProjectLocation? get() = buildGraphRunner.multiGraph.projectLocations[projectId]

  private fun cannotCast(value: BibixValue, type: BibixType) =
    BuildTaskResult.TypeCastFailResult(value, type)

  // castValue로 오는 ClassInstanceValue는 모두 각 필드가 적당한 타입으로 cast되고, default field와 optional field가 처리된 것으로 본다.
  // 최종적으로 ResultWithValue
  fun castValue(value: BibixValue, type: BibixType): BuildTaskResult {
    fun fileFromString(path: String): Path {
      return checkNotNull(projectLocation).projectRoot.resolve(path).normalize().absolute()
    }

    if (value == NoneValue) {
      return BuildTaskResult.ValueResult(value)
    }
    if (value is ClassInstanceValue) {
      // 사용자가 정의한 custom cast가 있으면 그것부터 시도해보고, 그게 실패하면 기본 캐스팅 시도
      when (type) {
        is UnionType ->
          return castToUnionType(value, type)

        is DataClassType -> {
          if (type.packageName == value.packageName && type.className == value.className) {
            // type과 일치하고, value가 이미 온전한 것이 확인되었으므로 값을 그대로 반환
            return BuildTaskResult.ValueResult(value)
          }
        }

        is SuperClassType -> {
          if (type.packageName == value.packageName) {
            // value가 type의 sub type의 클래스이면 그대로 반환 - 그 외의 경우엔 계속 진행
            // TODO 지금은 sub type인지 체크해보고 맞으면 성공, 아니면 exception을 내버려서.. 의도한 동작과는 조금 다름.
            return BuildTaskResult.WithResult(
              EvalSuperClassByName(type.packageName, type.className)
            ) { superClassResult ->
              check(superClassResult is BuildTaskResult.SuperClassHierarchyResult)

              check(BibixName(value.className) in superClassResult.allSubDataClasses) { "Not a subtype" }
              BuildTaskResult.ValueResult(value)
            }
          }
        }

        else -> {
          // do nothing - fallthrough
        }
      }
      return tryCustomCast(value, type) { valueDataClass ->
        // valueDataClass는 value의 DataClassResult
        when (type) {
          is DataClassType -> {
            check(type.packageName != value.packageName || type.className != value.className)
            cannotCast(value, type)
          }

          is SuperClassType -> {
            check(type.packageName != value.packageName)
            cannotCast(value, type)
          }

          is TupleType -> {
            TODO()
          }

          is NamedTupleType -> {
            TODO()
          }

          else -> cannotCast(value, type)
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
            BuildTaskResult.WithResult(
              EvalDataClassByName(type.packageName, type.className)
            ) { dataClass ->
              check(dataClass is BuildTaskResult.DataClassResult)

              // TODO dataClass의 필드 중 optional이거나 default가 있는 경우 처리
              check(dataClass.fieldTypes.size == value.values.size)
              organizeParamsForDataClass(projectId, dataClass, value.values, mapOf()) {
                BuildTaskResult.ValueResult(it)
              }
            }
          }

          is NamedTupleValue -> {
            BuildTaskResult.WithResult(
              EvalDataClassByName(type.packageName, type.className)
            ) { dataClass ->
              check(dataClass is BuildTaskResult.DataClassResult)

              organizeParamsForDataClass(projectId, dataClass, listOf(), value.valuesMap) {
                BuildTaskResult.ValueResult(it)
              }
            }
          }

          else -> cannotCast(value, type)
        }
      }

      is SuperClassType -> {
        // value가 ClassInstanceValue인 경우는 이미 위에서 처리되었음
        // 그 외의 경우엔 super class type으로 coercion 불가능
        cannotCast(value, type)
      }

      is UnionType -> castToUnionType(value, type)

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

  fun castToUnionType(value: BibixValue, type: UnionType): BuildTaskResult {
    fun tryCandidateAt(candidateIdx: Int): BuildTaskResult =
      if (candidateIdx >= type.types.size) {
        cannotCast(value, type)
      } else {
        BuildTaskResult.WithResult(
          TypeCastValue(value, type.types[candidateIdx], projectId)
        ) { result ->
          when (result) {
            is BuildTaskResult.ResultWithValue -> result
            else -> tryCandidateAt(candidateIdx + 1)
          }
        }
      }
    return tryCandidateAt(0)
  }

  private fun tryCustomCast(
    value: ClassInstanceValue,
    type: BibixType,
    whenNotAvailable: (valueDataClass: BuildTaskResult.DataClassResult) -> BuildTaskResult
  ): BuildTaskResult {
    return BuildTaskResult.WithResult(
      EvalDataClassByName(value.packageName, value.className)
    ) { dataClass ->
      check(dataClass is BuildTaskResult.DataClassResult)

      // dataClass에서 cast 중 type하고 일치하는 것이 있으면 evaluate해서 func로 반환
      val customCast = dataClass.customCasts.find { it.first == type }
      if (customCast == null) {
        whenNotAvailable(dataClass)
      } else {
        ExprEvaluator(
          buildGraphRunner,
          dataClass.projectId,
          dataClass.importInstanceId,
          mapOf(),
          value
        ).evaluateExpr(customCast.second)
      }
    }
  }

  private fun castValueList(
    values: List<BibixValue>,
    type: BibixType,
    func: (List<BibixValue>) -> BibixValue
  ): BuildTaskResult =
    BuildTaskResult.WithResultList(values.map {
      TypeCastValue(it, type, projectId)
    }) { results ->
      check(results.all { it is BuildTaskResult.ResultWithValue })
      val finalValue = func(results.map { (it as BuildTaskResult.ResultWithValue).value })
      BuildTaskResult.ValueResult(finalValue)
    }

  private fun castValuePairs(
    valueAndTypes: List<Pair<BibixValue, BibixType>>,
    func: (List<BibixValue>) -> BibixValue
  ): BuildTaskResult =
    BuildTaskResult.WithResultList(valueAndTypes.map { (value, type) ->
      TypeCastValue(value, type, projectId)
    }) { results ->
      check(results.all { it is BuildTaskResult.ResultWithValue })
      val finalValue = func(results.map { (it as BuildTaskResult.ResultWithValue).value })
      BuildTaskResult.ValueResult(finalValue)
    }

  fun finalizeBuildRuleReturnValue(
    finalizeCtx: BuildRuleDefContext,
    value: BibixValue,
  ): BuildTaskResult = when (value) {
    is NClassInstanceValue -> {
      // buildRule 위치를 기준으로 ClassInstanceValue로 변경해서 반환
      buildGraphRunner.lookupDataClass(
        finalizeCtx.projectId,
        finalizeCtx.importInstanceId,
        BibixName(value.nameTokens)
      ) { dataClass ->
        val fieldTypeMaps = dataClass.fieldTypes.toMap()
        finalizeValues(finalizeCtx, value.fieldValues, fieldTypeMaps) { fieldNames, finVals ->
          organizeParamsForDataClass(
            projectId,
            dataClass,
            listOf(),
            fieldNames.zip(finVals).toMap(),
          ) { BuildTaskResult.ValueResult(it) }
        }
      }
    }

    is ClassInstanceValue -> {
      // value가 ClassInstanceValue이면 default value, optional value들 채우고 기존 필드들도 목표 타입으로 캐스팅해서 반환
      BuildTaskResult.WithResult(
        EvalDataClassByName(value.packageName, value.className)
      ) { dataClassResult ->
        check(dataClassResult is BuildTaskResult.DataClassResult)

        val fieldTypeMaps = dataClassResult.fieldTypes.toMap()
        finalizeValues(finalizeCtx, value.fieldValues, fieldTypeMaps) { fieldNames, finVals ->
          val castTasks = fieldNames.zip(finVals).map { (name, value) ->
            val expectedType = fieldTypeMaps.getValue(name)
            TypeCastValue(value, expectedType, projectId)
          }
          BuildTaskResult.WithResultList(castTasks) { cast ->
            check(cast.size == finVals.size)
            val castValues = cast.map {
              check(it is BuildTaskResult.ResultWithValue) { "$it" }
              it.value
            }

            organizeParamsForDataClass(
              projectId,
              dataClassResult,
              listOf(),
              fieldNames.zip(castValues).toMap()
            ) { BuildTaskResult.ValueResult(it) }
          }
        }
      }
    }

    is CollectionValue -> finalizeValueList(finalizeCtx, value.values, value::newCollectionWith)
    is TupleValue -> finalizeValueList(finalizeCtx, value.values, ::TupleValue)
    is NamedTupleValue ->
      finalizeValueList(finalizeCtx, value.values) { elems ->
        NamedTupleValue(value.names.zip(elems))
      }

    else -> BuildTaskResult.ValueResult(value)
  }

  private fun finalizeValueList(
    finalizeCtx: BuildRuleDefContext,
    values: List<BibixValue>,
    func: (List<BibixValue>) -> BibixValue
  ): BuildTaskResult =
    BuildTaskResult.WithResultList(values.map {
      FinalizeBuildRuleReturnValue(finalizeCtx, it, projectId)
    }) { results ->
      if (results.all { it is BuildTaskResult.ResultWithValue }) {
        val finalValue = func(results.map { (it as BuildTaskResult.ResultWithValue).value })
        BuildTaskResult.ValueResult(finalValue)
      } else {
        BuildTaskResult.ValueFinalizeFailResult(values)
      }
    }

  private fun finalizeValues(
    finalizeCtx: BuildRuleDefContext,
    values: Map<String, BibixValue>,
    types: Map<String, BibixType>,
    block: (List<String>, List<BibixValue>) -> BuildTaskResult
  ): BuildTaskResult {
    check(types.keys.containsAll(values.keys)) { "Missing args: ${types.keys - values.keys}" }

    val fieldValues = values.entries.sortedBy { it.key }
    val finalizeTasks = fieldValues.map {
      FinalizeBuildRuleReturnValue(finalizeCtx, it.value, projectId)
    }
    return BuildTaskResult.WithResultList(finalizeTasks) { finalized ->
      check(finalized.size == fieldValues.size)
      val finalizedValues = finalized.map {
        check(it is BuildTaskResult.ResultWithValue)
        it.value
      }
      block(fieldValues.map { it.key }, finalizedValues)
    }
  }
}
