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
  }

  data class ValueReturn(val value: BibixValue) : BuildRuleReturn()
  data class FailedReturn(val exception: Exception) : BuildRuleReturn()
  data class EvalAndThen(
    val ruleName: String,
    val params: Map<String, BibixValue>,
    val whenDone: (BibixValue) -> BuildRuleReturn
  ) : BuildRuleReturn()
}