package com.giyeok.bibix.base

import java.nio.file.Path

sealed class BuildRuleReturn {
  companion object {
    @JvmStatic
    fun evalAndThen(
      ruleName: String,
      params: Map<String, BibixValue>,
      whenDone: (BibixValue) -> BuildRuleReturn
    ) = EvalAndThen(ruleName, params, whenDone)

    @JvmStatic
    fun eval(
      ruleName: String,
      params: Map<String, BibixValue>,
    ) = EvalAndThen(ruleName, params, ::value)

    @JvmStatic
    fun getTypeDetails(
      typeNames: List<TypeName>,
      relativeNames: List<String>,
      whenDone: (TypeDetailsMap) -> BuildRuleReturn,
    ) = GetTypeDetails(typeNames, relativeNames, whenDone)

    @JvmStatic
    fun value(value: BibixValue) = ValueReturn(value)

    @JvmStatic
    fun failed(exception: Exception) = FailedReturn(exception)

    @JvmStatic
    fun done() = DoneReturn

    @JvmStatic
    fun withDirectoryLock(directory: Path, withLock: () -> BuildRuleReturn) =
      WithDirectoryLock(directory, withLock)
  }

  data class ValueReturn(val value: BibixValue) : BuildRuleReturn()
  data class FailedReturn(val exception: Exception) : BuildRuleReturn()

  // DoneReturn은 액션에서만 반환해야 함
  object DoneReturn : BuildRuleReturn()

  data class EvalAndThen(
    val ruleName: String,
    val params: Map<String, BibixValue>,
    val whenDone: (BibixValue) -> BuildRuleReturn
  ) : BuildRuleReturn()

  data class GetTypeDetails(
    val typeNames: List<TypeName>,
    val relativeNames: List<String>,
    val whenDone: (TypeDetailsMap) -> BuildRuleReturn,
  ) : BuildRuleReturn()

  data class WithDirectoryLock(
    val directory: Path,
    val withLock: () -> BuildRuleReturn
  ) : BuildRuleReturn()
}

data class TypeName(val packageName: String, val typeName: String)

data class TypeDetailsMap(
  val canonicalNamed: Map<TypeName, TypeDetails>,
  val relativeNamed: Map<String, TypeDetails>,
)
