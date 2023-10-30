package com.giyeok.bibix.graph.runner2

import com.giyeok.bibix.base.*
import com.giyeok.bibix.graph.DataClassNameEntry
import com.giyeok.bibix.graph.NameEntryFound
import java.nio.file.Path
import kotlin.io.path.absolutePathString


private fun cannotCast(value: BibixValue, type: BibixType): Nothing =
  throw IllegalStateException("Cannot cast $value to $type")

fun GlobalTaskRunner.withCastedValue(
  value: BibixValue,
  type: BibixType,
  ctxId: TaskContextId,
  func: (BibixValue) -> TaskRunResult
): TaskRunResult {
  check(value !is NClassInstanceValue)
  val coercionResult = castValue(value, type, ctxId)
  return if (coercionResult is TaskRunResult.ImmediateResult) {
    func((coercionResult.result as NodeResult.ValueResult).value)
  } else {
    coercionResult
  }
}

private fun GlobalTaskRunner.withCastedValues(
  values: Map<String, BibixValue>,
  types: Map<String, BibixType>,
  ctxId: TaskContextId,
  func: (Map<String, BibixValue>) -> TaskRunResult
): TaskRunResult {
  check(types.keys.containsAll(values.keys))
  check(values.all { it.value !is NClassInstanceValue })

  val coerced = mutableMapOf<String, BibixValue>()
  values.forEach { (name, value) ->
    val coercionResult = castValue(value, types.getValue(name), ctxId)
    if (coercionResult !is TaskRunResult.ImmediateResult) {
      return coercionResult
    }
    coerced[name] = (coercionResult.result as NodeResult.ValueResult).value
  }
  return func(coerced)
}

private fun GlobalTaskRunner.withCastedValuesList(
  values: List<BibixValue>,
  type: BibixType,
  ctxId: TaskContextId,
  func: (List<BibixValue>) -> TaskRunResult
): TaskRunResult {
  check(values.all { it !is NClassInstanceValue })

  val coerced = mutableListOf<BibixValue>()
  values.forEach { value ->
    val coercionResult = castValue(value, type, ctxId)
    if (coercionResult !is TaskRunResult.ImmediateResult) {
      return coercionResult
    }
    coerced.add((coercionResult.result as NodeResult.ValueResult).value)
  }
  return func(coerced)
}

private fun GlobalTaskRunner.withCastedValuePairs(
  valueAndTypes: List<Pair<BibixValue, BibixType>>,
  ctxId: TaskContextId,
  func: (List<BibixValue>) -> TaskRunResult
): TaskRunResult {
  check(valueAndTypes.all { it.first !is NClassInstanceValue })

  val coerced = mutableListOf<BibixValue>()
  valueAndTypes.forEach { (value, type) ->
    val coercionResult = castValue(value, type, ctxId)
    if (coercionResult !is TaskRunResult.ImmediateResult) {
      return coercionResult
    }
    coerced.add((coercionResult.result as NodeResult.ValueResult).value)
  }
  return func(coerced)
}

private fun v(value: BibixValue) =
  TaskRunResult.ImmediateResult(NodeResult.ValueResult(value, true))

private fun GlobalTaskRunner.castValue(
  value: BibixValue,
  type: BibixType,
  ctxId: TaskContextId
): TaskRunResult {
  fun fileFromString(path: String): Path {
    val projectLocation = globalGraph.projectLocations.getValue(ctxId.projectId)
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
      else -> cannotCast(value, type)
    }

    StringType -> when (value) {
      is StringValue -> v(value)
      is PathValue -> v(StringValue(value.path.absolutePathString()))
      is FileValue -> v(StringValue(value.file.absolutePathString()))
      is DirectoryValue -> v(StringValue(value.directory.absolutePathString()))
      else -> cannotCast(value, type)
    }

    PathType -> when (value) {
      is PathValue -> v(value)
      is FileValue -> v(PathValue(value.file))
      is DirectoryValue -> v(PathValue(value.directory))
      is StringValue -> v(PathValue(fileFromString(value.value)))
      else -> cannotCast(value, type)
    }

    FileType -> when (value) {
      is FileValue -> v(value)
      // TODO file value, directory value는 build rule에 주기 직전에 존재하는지/타입 확인하기
      is PathValue -> v(FileValue(value.path))
      is StringValue -> v(FileValue(fileFromString(value.value)))
      else -> cannotCast(value, type)
    }

    DirectoryType -> when (value) {
      is DirectoryValue -> v(value)
      is PathValue -> v(DirectoryValue(value.path))
      is StringValue -> v(DirectoryValue(fileFromString(value.value)))
      else -> cannotCast(value, type)
    }

    is ListType -> {
      when (value) {
        is ListValue ->
          withCastedValuesList(value.values, type.elemType, ctxId) { coerced ->
            // v(ListValue(value.values.map { coerceValue(it, type.elemType, prjInstanceId) }))
            v(ListValue(coerced))
          }

        is SetValue ->
          withCastedValuesList(value.values, type.elemType, ctxId) { coerced ->
            // v(ListValue(value.values.map { coerceValue(it, type.elemType, prjInstanceId) }))
            v(ListValue(coerced))
          }

        else -> cannotCast(value, type)
      }
    }

    is SetType -> {
      when (value) {
        is ListValue ->
          withCastedValuesList(value.values, type.elemType, ctxId) { coerced ->
            // SetValue(value.values.map { coerceValue(it, type.elemType, prjInstanceId) })
            v(SetValue(coerced))
          }

        is SetValue ->
          withCastedValuesList(value.values, type.elemType, ctxId) { coerced ->
            //          SetValue(value.values.map { coerceValue(it, type.elemType, prjInstanceId) })
            v(SetValue(coerced))
          }

        else -> cannotCast(value, type)
      }
    }

    is TupleType -> when (value) {
      is TupleValue -> {
        check(type.elemTypes.size == value.values.size)
        withCastedValuePairs(value.values.zip(type.elemTypes), ctxId) { coerced ->
          v(TupleValue(coerced))
        }
      }

      is NamedTupleValue -> {
        check(type.elemTypes.size == value.pairs.size)
        withCastedValuePairs(value.values().zip(type.elemTypes), ctxId) { coerced ->
          v(TupleValue(coerced))
        }
      }

      else -> cannotCast(value, type)
    }

    is NamedTupleType -> when (value) {
      is NamedTupleValue -> {
        check(type.pairs.size == value.pairs.size)
        check(type.names() == value.names())
        withCastedValuePairs(value.values().zip(type.valueTypes()), ctxId) { coerced ->
          v(NamedTupleValue(type.names().zip(coerced)))
        }
      }

      is TupleValue -> {
        check(type.pairs.size == value.values.size)
        withCastedValuePairs(value.values.zip(type.valueTypes()), ctxId) { coerced ->
          v(NamedTupleValue(type.names().zip(coerced)))
        }
      }

      else -> cannotCast(value, type)
    }

    is DataClassType -> {
      when (value) {
        is ClassInstanceValue -> {
          // TODO class instance coercion은 plugin에서 반환된 값에 대해서만 하면 될듯도 하고..?
          if (type.packageName == value.packageName && type.className == value.className) {
            // TODO field들에 대한 처리
            correctClassValue(value, ctxId)
          } else {
            // TODO
            cannotCast(value, type)
          }
        }

        else -> TODO()
      }
    }

    is SuperClassType -> {
      when (value) {
        is ClassInstanceValue -> {
          if (type.packageName != value.packageName) {
            cannotCast(value, type)
          }
          // TODO value의 타입이 super type의 sub type이 맞는지 확인
          // TODO field들에 대한 처리
          correctClassValue(value, ctxId)
        }

        else -> TODO()
      }
    }

    is UnionType -> {
      val firstMatch = type.types.firstNotNullOfOrNull { typeCandidate ->
        try {
          castValue(value, typeCandidate, ctxId)
        } catch (e: Exception) {
          null
        }
      }
      firstMatch ?: cannotCast(value, type)
    }

    NoneType ->
      if (value is NoneValue) v(value) else cannotCast(value, type)

    ActionRuleDefType ->
      if (value is ActionRuleDefValue) v(value) else cannotCast(value, type)

    BuildRuleDefType ->
      if (value is BuildRuleDefValue) v(value) else cannotCast(value, type)

    TypeType ->
      if (value is TypeValue) v(value) else cannotCast(value, type)

    is EnumType ->
      if (value is EnumValue && value.packageName == type.packageName && value.enumName == type.enumName) {
        v(value)
      } else {
        cannotCast(value, type)
      }
  }
}

fun GlobalTaskRunner.correctClassValue(
  value: ClassInstanceValue,
  ctxId: TaskContextId,
): TaskRunResult {
  val classDefineProjectId = globalGraph.getProjectIdByPackageName(value.packageName)!!
  val classDefineGraph = globalGraph.getProjectGraph(classDefineProjectId)
  val classDefineTask = when (val lookupResult =
    classDefineGraph.nameLookupTable.lookupName(listOf(value.className))) {
    is NameEntryFound -> {
      check(lookupResult.entry is DataClassNameEntry)
      lookupResult.entry.id
    }

    else -> {
      TODO()
    }
  }

  // TODO 일단은 임시 땜빵..
//
//  when (classDefineProjectId) {
//    MainProjectId.projectId -> {
//      return globalWithResult(
//        prjInstanceId = MainProjectId,
//        localTaskId = classDefineTask,
//        edgeType = TaskEdgeType.TypeDependency
//      ) { typeResult ->
//        when (typeResult) {
//          is NodeResult.DataClassTypeResult -> {
//            finalizeClassInstanceValue(value, typeResult, prjInstanceId)
//          }
//
//          else -> TODO()
//        }
//      }
//    }
//
//    PreludeProjectId.projectId -> {
//      return globalWithResult(
//        prjInstanceId = PreludeProjectId,
//        localTaskId = classDefineTask,
//        edgeType = TaskEdgeType.TypeDependency
//      ) { typeResult ->
//        when (typeResult) {
//          is NodeResult.DataClassTypeResult -> {
//            finalizeClassInstanceValue(value, typeResult, prjInstanceId)
//          }
//
//          else -> TODO()
//        }
//      }
//    }
//  }
//
//  val classDefineResults = this.results.filter {
////    when (val prj = it.key.projectInstanceId) {
////      MainProjectId -> TODO()
////      PreludeProjectId -> TODO()
////      is ImportedProjectId -> {
////        if (prj.importer.projectInstanceId == prjInstanceId) {
////          TODO()
////        }
////      }
////    }
//    it.key.projectInstanceId.projectId == classDefineProjectId && it.key.taskId == classDefineTask
//  }
//
//  val classTypeResult = classDefineResults.entries.first().value
//  check(classTypeResult is NodeResult.DataClassTypeResult)
//
//  // TODO 생각해보니 DataClassTypeResult 는 prjInstanceId를 제외하고 나머지 내용은 ProjectInstanceId가 뭐든간에 똑같은데
//  val defines = classDefineResults.values.toList()
//  val d = defines.first()
//  check(d is NodeResult.DataClassTypeResult)
//  defines.drop(1).forEach { c ->
//    check(c is NodeResult.DataClassTypeResult)
//    check(d.fields == c.fields)
//    check(d.optionalFields == c.optionalFields)
//    check(d.defaultValues == c.defaultValues)
//  }
//
//  return finalizeClassInstanceValue(value, classTypeResult, prjInstanceId)
  TODO()
}

private fun GlobalTaskRunner.finalizeClassInstanceValue(
  value: ClassInstanceValue,
  dataClass: NodeResult.DataClassTypeResult,
  ctxId: TaskContextId
): TaskRunResult {
  // dataClassResult.fields와 defaultValues를 보고 value를 수정해서 반환
  //  - 빠져있는 optional 필드에 NoneValue 넣기
  //  - 빠져있는 default value 필드 채우기
  //  - 필드별 type coercion
  val fieldMaps = dataClass.fields.toMap()
  check(fieldMaps.keys.containsAll(value.fieldValues.keys)) { "Unknown fields" }

  val requiredFields = fieldMaps.keys - (dataClass.defaultValues.keys + dataClass.optionalFields)
  check(value.fieldValues.keys.containsAll(requiredFields)) { "Missing required fields" }

  val defaultFieldTasks = dataClass.defaultValues - value.fieldValues.keys

  return withResults(ctxId, defaultFieldTasks.values.toSet()) { defaultFieldValues ->
    withCastedValues(value.fieldValues, fieldMaps, ctxId) { coercedFields ->
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
