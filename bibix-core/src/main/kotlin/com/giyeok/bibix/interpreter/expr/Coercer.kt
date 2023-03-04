package com.giyeok.bibix.interpreter.expr

import com.giyeok.bibix.ast.BibixAst
import com.giyeok.bibix.base.*
import com.giyeok.bibix.interpreter.*
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
    ?: throw IllegalStateException(
      "Coercion failed: $value to $type (${sourceManager.descriptionOf(context.sourceId)})"
    )

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
            return SetValue(value.values.map { value ->
              coerce(task, context, value, type.elemType)
            })
          }

          is SetValue -> {
            return SetValue(value.values.map { value ->
              coerce(task, context, value, type.elemType)
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
            // TODO class instance coercion은 plugin에서 반환된 값에 대해서만 하면 될듯도 하고..?
            val coercedClassValue = tryCoerceClassInstanceValue(task, context, value, type)
            if (coercedClassValue != null) {
              return coercedClassValue
            }
          }

          else -> {}
        }
      }

      is SuperClassType -> {
        when (value) {
          is ClassInstanceValue -> {
            if (isValidValueOf(task, value, type)) {
              // 실제 Data class type에 대해서 찾아서 coerce해서 반환
              return coerce(task, context, value, DataClassType(value.packageName, value.className))
            }
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
      is EnumType -> {
        if (value is EnumValue && value.packageName == type.packageName && value.enumName == type.enumName) {
          return value
        } else {
          println(type)
          println(value)
          TODO()
        }
      }
    }
    return tryCoerceFromValue(task, context, value, type)
  }

  private suspend fun fileFromString(sourceId: SourceId, path: String): Path {
    return sourceManager.getProjectRoot(sourceId)!!.resolve(path).normalize()
  }

  private suspend fun findDataClassDef(
    task: Task,
    packageName: String,
    className: String
  ): EvaluationResult.DataClassDef {
    val typeDefinedContext = NameLookupContext(
      sourceManager.getSourceIdFromPackageName(packageName)!!,
      listOf()
    )
    val classDef =
      exprEvaluator.evaluateName(task, typeDefinedContext, className.split('.'), null, setOf())
    check(classDef is EvaluationResult.DataClassDef)
    return classDef
  }

  private suspend fun tryCoerceClassInstanceValue(
    task: Task,
    context: NameLookupContext,
    value: ClassInstanceValue,
    type: DataClassType
  ): ClassInstanceValue? {
    if (value.packageName != type.packageName || value.className != type.className) {
      return null
    }

    val classDef = findDataClassDef(task, type.packageName, type.className)

    val fieldDefs = classDef.params.associateBy { it.name }
    val missingFields = fieldDefs.keys - value.fieldValues.keys
    if (missingFields.any { !fieldDefs.getValue(it).optional }) {
      // optional이 아닌데 빠진 필드가 있으면 실패
      return null
    }
    if ((value.fieldValues.keys - fieldDefs.keys).isNotEmpty()) {
      // 모르는 필드 이름이 있으면 실패
      return null
    }
    val coercedFields = value.fieldValues.mapValues { field ->
      val fieldDef = fieldDefs.getValue(field.key)
      if (fieldDef.optional && field.value == NoneValue) {
        NoneValue
      } else {
        tryCoerce(task, context, field.value, fieldDef.type)
      }
    }
    if (coercedFields.any { it.value == null }) {
      // 필드 중 coerce 실패하는 것이 있으면 실패
      return null
    }
    return ClassInstanceValue(
      value.packageName,
      value.className,
      coercedFields.mapValues { it.value!! } + missingFields.map { it to NoneValue })
  }

  private suspend fun isValidValueOf(
    task: Task,
    value: ClassInstanceValue,
    type: SuperClassType
  ): Boolean {
    if (!isSubClass(task, type, DataClassType(value.packageName, value.className))) {
      return false
    }
    return true
  }

  private suspend fun isSubClass(
    task: Task,
    superClass: SuperClassType,
    dataClass: DataClassType
  ): Boolean {
    // super class와 data class는 반드시 같은 패키지 안에 있어야 함
    if (superClass.packageName != dataClass.packageName) {
      return false
    }
    val superClassName = superClass.className.split('.').map { it.trim() }
    val dataClassName = dataClass.className.split('.').map { it.trim() }
    val superClassNamespace = superClassName.dropLast(1)
    if (superClassNamespace != dataClassName.dropLast(1)) {
      // super class와 data 클래스는 반드시 같은 네임스페이스 안에 있어야 함
      return false
    }

    val typContext = NameLookupContext(
      sourceManager.getSourceIdFromPackageName(superClass.packageName)!!,
      listOf()
    )

    suspend fun traverse(superClassName: String): Boolean {
      val className = superClassNamespace + superClassName
      val superClassDef = exprEvaluator.evaluateName(task, typContext, className, null, setOf())
      if (superClassDef !is EvaluationResult.SuperClassDef) {
        return false
      }
      if (superClassDef.subClasses.contains(dataClassName.last())) {
        return true
      }
      return superClassDef.subClasses.any { traverse(it) }
    }
    return traverse(superClassName.last())
  }

  private suspend fun isSubType(task: Task, superType: BibixType, subType: BibixType): Boolean {
    // TODO
    return superType == subType
  }

  private suspend fun tryCoerceFromValue(
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

    when (value) {
      is ClassInstanceValue -> {
        val classDef = exprEvaluator.findTypeDefinition(task, value.packageName, value.className)
          ?: throw IllegalStateException("Cannot find class def for $value ($task)")
        if (classDef is EvaluationResult.DataClassDef) {
          // 클래스 body에 정의된 cast 중 호환되는 첫번째 타입 캐스트로 반환
          classDef.bodyElems.forEach { bodyElem ->
            when (bodyElem) {
              is BibixAst.ActionRuleDef -> {}
              is BibixAst.ClassCastDef -> {
                val castType =
                  exprEvaluator.evaluateType(task, classDef.context, bodyElem.castTo)
                if (isSubType(task, type, castType)) {
                  val cast =
                    exprEvaluator.evaluateExpr(
                      task,
                      classDef.context,
                      bodyElem.expr,
                      value,
                      setOf()
                    ).ensureValue()
                  return coerce(task, context, cast, type)
                }
              }

              else -> throw AssertionError()
            }
          }
        }
      }

      else -> {}
    }
    return null
  }
}
