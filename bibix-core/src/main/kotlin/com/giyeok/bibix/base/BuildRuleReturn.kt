package com.giyeok.bibix.base

sealed class BuildRuleReturn {
  companion object {
    @JvmStatic
    fun evalAndThen(
      ruleName: String,
      params: Map<String, BibixValue>,
      whenDone: (BibixValue) -> BuildRuleReturn
    ) = EvalAndThen(ruleName, params, whenDone)

    @JvmStatic
    fun value(value: BibixValue) = ValueReturn(value)

    @JvmStatic
    fun failed(exception: Exception) = FailedReturn(exception)

    @JvmStatic
    fun done() = DoneReturn
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
}
