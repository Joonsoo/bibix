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
    fun getClassInfos(
      cnames: List<CName>,
      unames: List<String> = listOf(),
      whenDone: (List<TypeValue.ClassTypeDetail>) -> BuildRuleReturn,
    ) = GetClassTypeDetails(cnames, unames.map { it.split('.') }, whenDone)

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

  data class GetClassTypeDetails(
    val cnames: List<CName>,
    val unames: List<List<String>>,
    val whenDone: (List<TypeValue.ClassTypeDetail>) -> BuildRuleReturn,
  ) : BuildRuleReturn()

  data class WithDirectoryLock(
    val directory: Path,
    val withLock: () -> BuildRuleReturn
  ) : BuildRuleReturn()
}
