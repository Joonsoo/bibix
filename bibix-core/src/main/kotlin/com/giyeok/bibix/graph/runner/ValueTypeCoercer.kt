package com.giyeok.bibix.graph.runner

import com.giyeok.bibix.base.*
import com.giyeok.bibix.graph.ClassNameEntry
import com.giyeok.bibix.graph.NameEntryFound
import com.giyeok.bibix.graph.TaskEdgeType
import java.nio.file.Path
import kotlin.io.path.absolutePathString


private fun cannotCoerce(value: BibixValue, type: BibixType): Nothing =
  throw IllegalStateException("Cannot coerce $value to $type")

fun GlobalTaskRunner.withCoercedValue(
  value: BibixValue,
  type: BibixType,
  currentPrjInstanceId: ProjectInstanceId,
  func: (BibixValue) -> TaskRunResult
): TaskRunResult {
  check(value !is NClassInstanceValue)
  val coercionResult = coerceValue(value, type, currentPrjInstanceId)
  return if (coercionResult is TaskRunResult.ImmediateResult) {
    func((coercionResult.result as NodeResult.ValueResult).value)
  } else {
    coercionResult
  }
}

private fun GlobalTaskRunner.withCoercedValues(
  values: Map<String, BibixValue>,
  types: Map<String, BibixType>,
  currentPrjInstanceId: ProjectInstanceId,
  func: (Map<String, BibixValue>) -> TaskRunResult
): TaskRunResult {
  check(types.keys.containsAll(values.keys))
  check(values.all { it.value !is NClassInstanceValue })

  val coerced = mutableMapOf<String, BibixValue>()
  values.forEach { (name, value) ->
    val coercionResult = coerceValue(value, types.getValue(name), currentPrjInstanceId)
    if (coercionResult !is TaskRunResult.ImmediateResult) {
      return coercionResult
    }
    coerced[name] = (coercionResult.result as NodeResult.ValueResult).value
  }
  return func(coerced)
}

private fun GlobalTaskRunner.withCoercedValuesList(
  values: List<BibixValue>,
  type: BibixType,
  currentPrjInstanceId: ProjectInstanceId,
  func: (List<BibixValue>) -> TaskRunResult
): TaskRunResult {
  check(values.all { it !is NClassInstanceValue })

  val coerced = mutableListOf<BibixValue>()
  values.forEach { value ->
    val coercionResult = coerceValue(value, type, currentPrjInstanceId)
    if (coercionResult !is TaskRunResult.ImmediateResult) {
      return coercionResult
    }
    coerced.add((coercionResult.result as NodeResult.ValueResult).value)
  }
  return func(coerced)
}

private fun GlobalTaskRunner.withCoercedValuePairs(
  valueAndTypes: List<Pair<BibixValue, BibixType>>,
  currentPrjInstanceId: ProjectInstanceId,
  func: (List<BibixValue>) -> TaskRunResult
): TaskRunResult {
  check(valueAndTypes.all { it.first !is NClassInstanceValue })

  val coerced = mutableListOf<BibixValue>()
  valueAndTypes.forEach { (value, type) ->
    val coercionResult = coerceValue(value, type, currentPrjInstanceId)
    if (coercionResult !is TaskRunResult.ImmediateResult) {
      return coercionResult
    }
    coerced.add((coercionResult.result as NodeResult.ValueResult).value)
  }
  return func(coerced)
}

private fun GlobalTaskRunner.coerceValue(
  value: BibixValue,
  type: BibixType,
  prjInstanceId: ProjectInstanceId,
): TaskRunResult {
  fun fileFromString(path: String): Path {
    val projectLocation = globalGraph.projectLocations.getValue(prjInstanceId.projectId)
    return projectLocation.projectRoot.resolve(path).normalize()
  }

  if (value == NoneValue) {
    return v(value)
  }
  if (value is ClassInstanceValue) {
    // TODO value의 as element 중에 적용 가능한게 있는지 확인해서 적용해서 반환
  }
  return when (type) {
    AnyType -> v(value)
    BooleanType -> when (value) {
      is BooleanValue -> v(value)
      else -> cannotCoerce(value, type)
    }

    StringType -> when (value) {
      is StringValue -> v(value)
      is PathValue -> v(StringValue(value.path.absolutePathString()))
      is FileValue -> v(StringValue(value.file.absolutePathString()))
      is DirectoryValue -> v(StringValue(value.directory.absolutePathString()))
      else -> cannotCoerce(value, type)
    }

    PathType -> when (value) {
      is PathValue -> v(value)
      is FileValue -> v(PathValue(value.file))
      is DirectoryValue -> v(PathValue(value.directory))
      is StringValue -> v(PathValue(fileFromString(value.value)))
      else -> cannotCoerce(value, type)
    }

    FileType -> when (value) {
      is FileValue -> v(value)
      // TODO file value, directory value는 build rule에 주기 직전에 존재하는지/타입 확인하기
      is PathValue -> v(FileValue(value.path))
      is StringValue -> v(FileValue(fileFromString(value.value)))
      else -> cannotCoerce(value, type)
    }

    DirectoryType -> when (value) {
      is DirectoryValue -> v(value)
      is PathValue -> v(DirectoryValue(value.path))
      is StringValue -> v(DirectoryValue(fileFromString(value.value)))
      else -> cannotCoerce(value, type)
    }

    is ListType -> {
      when (value) {
        is ListValue ->
          withCoercedValuesList(value.values, type.elemType, prjInstanceId) { coerced ->
            // v(ListValue(value.values.map { coerceValue(it, type.elemType, prjInstanceId) }))
            v(ListValue(coerced))
          }

        is SetValue ->
          withCoercedValuesList(value.values, type.elemType, prjInstanceId) { coerced ->
            // v(ListValue(value.values.map { coerceValue(it, type.elemType, prjInstanceId) }))
            v(ListValue(coerced))
          }

        else -> cannotCoerce(value, type)
      }
    }

    is SetType -> {
      when (value) {
        is ListValue ->
          withCoercedValuesList(value.values, type.elemType, prjInstanceId) { coerced ->
            // SetValue(value.values.map { coerceValue(it, type.elemType, prjInstanceId) })
            v(SetValue(coerced))
          }

        is SetValue ->
          withCoercedValuesList(value.values, type.elemType, prjInstanceId) { coerced ->
            //          SetValue(value.values.map { coerceValue(it, type.elemType, prjInstanceId) })
            v(SetValue(coerced))
          }

        else -> cannotCoerce(value, type)
      }
    }

    is TupleType -> when (value) {
      is TupleValue -> {
        check(type.elemTypes.size == value.values.size)
        withCoercedValuePairs(value.values.zip(type.elemTypes), prjInstanceId) { coerced ->
          v(TupleValue(coerced))
        }
      }

      is NamedTupleValue -> {
        check(type.elemTypes.size == value.pairs.size)
        withCoercedValuePairs(value.values().zip(type.elemTypes), prjInstanceId) { coerced ->
          v(TupleValue(coerced))
        }
      }

      else -> cannotCoerce(value, type)
    }

    is NamedTupleType -> when (value) {
      is NamedTupleValue -> {
        check(type.pairs.size == value.pairs.size)
        check(type.names() == value.names())
        withCoercedValuePairs(value.values().zip(type.valueTypes()), prjInstanceId) { coerced ->
          v(NamedTupleValue(type.names().zip(coerced)))
        }
      }

      is TupleValue -> {
        check(type.pairs.size == value.values.size)
        withCoercedValuePairs(value.values.zip(type.valueTypes()), prjInstanceId) { coerced ->
          v(NamedTupleValue(type.names().zip(coerced)))
        }
      }

      else -> cannotCoerce(value, type)
    }

    is DataClassType -> {
      when (value) {
        is ClassInstanceValue -> {
          // TODO class instance coercion은 plugin에서 반환된 값에 대해서만 하면 될듯도 하고..?
          if (type.packageName == value.packageName && type.className == value.className) {
            // TODO field들에 대한 처리
            correctClassValue(value, prjInstanceId)
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
          // TODO field들에 대한 처리
          correctClassValue(value, prjInstanceId)
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
      if (value is NoneValue) v(value) else cannotCoerce(value, type)

    ActionRuleDefType ->
      if (value is ActionRuleDefValue) v(value) else cannotCoerce(value, type)

    BuildRuleDefType ->
      if (value is BuildRuleDefValue) v(value) else cannotCoerce(value, type)

    TypeType ->
      if (value is TypeValue) v(value) else cannotCoerce(value, type)

    is EnumType ->
      if (value is EnumValue && value.packageName == type.packageName && value.enumName == type.enumName) {
        v(value)
      } else {
        cannotCoerce(value, type)
      }
  }
}

fun GlobalTaskRunner.correctClassValue(
  value: ClassInstanceValue,
  prjInstanceId: ProjectInstanceId,
): TaskRunResult {
  val classDefineProjectId = globalGraph.getProjectIdByPackageName(value.packageName)!!
  val classDefineGraph = globalGraph.getProjectGraph(classDefineProjectId)
  val classDefineTask = when (val lookupResult =
    classDefineGraph.nameLookupTable.lookupName(listOf(value.className))) {
    is NameEntryFound -> {
      check(lookupResult.entry is ClassNameEntry)
      lookupResult.entry.id
    }

    else -> {
      TODO()
    }
  }
  // prjInstanceId가 가리키는 프로젝트에서 classDefineProjectId
  return globalWithResult(
    // TODO prjInstanceId
    prjInstanceId = ImportedProjectId(classDefineProjectId, TODO()),
    localTaskId = classDefineTask,
    edgeType = TaskEdgeType.TypeDependency
  ) { typeResult ->
    when (typeResult) {
      is NodeResult.DataClassTypeResult -> {
        finalizeClassInstanceValue(value, typeResult, prjInstanceId)
      }

      else -> TODO()
    }
  }
}

private fun GlobalTaskRunner.finalizeClassInstanceValue(
  value: ClassInstanceValue,
  dataClass: NodeResult.DataClassTypeResult,
  currentPrjInstanceId: ProjectInstanceId
): TaskRunResult {
  // TODO 이 내용은 coerce value 쪽으로 옮겨야 함
  //  -> 지금같은 상태로는 클래스 field로 들어가 있는 class instance에 대해선 이와 같은 보정이 이루어지지 않음
  // dataClassResult.fields와 defaultValues를 보고 value를 수정해서 반환
  //  - 빠져있는 optional 필드에 NoneValue 넣기
  //  - 빠져있는 default value 필드 채우기
  //  - 필드별 type coercion
  val fieldMaps = dataClass.fields.toMap()
  check(fieldMaps.keys.containsAll(value.fieldValues.keys)) { "Unknown fields" }

  val requiredFields = fieldMaps.keys - (dataClass.defaultValues.keys + dataClass.optionalFields)
  check(value.fieldValues.keys.containsAll(requiredFields)) { "Missing required fields" }

  val defaultFieldTasks = dataClass.defaultValues - value.fieldValues.keys

  return globalWithResults(
    prjInstanceId = dataClass.prjInstanceId,
    localTaskIds = defaultFieldTasks.values.toSet(),
    edgeType = TaskEdgeType.ValueDependency
  ) { defaultFieldValues ->
    withCoercedValues(value.fieldValues, fieldMaps, currentPrjInstanceId) { coercedFields ->
      val defaults = defaultFieldTasks.mapValues { (_, taskId) ->
        (defaultFieldValues.getValue(taskId) as NodeResult.ValueResult).value
      }
      val nones = (dataClass.optionalFields - value.fieldValues.keys).associateWith { NoneValue }
      check(value.fieldValues.keys.intersect(defaults.keys).isEmpty())
      check(value.fieldValues.keys.intersect(nones.keys).isEmpty())
      check(defaults.keys.intersect(nones.keys).isEmpty())
      val fieldValues = coercedFields + defaults + nones
      check(fieldValues.keys == fieldMaps.keys)

      v(ClassInstanceValue(value.packageName, value.className, fieldValues))
    }
  }
}
