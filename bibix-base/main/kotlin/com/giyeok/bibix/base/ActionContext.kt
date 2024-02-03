package com.giyeok.bibix.base

data class ActionContext(
  val env: BuildEnv,
  val arguments: Map<String, BibixValue>,
  val progressLogger: ProgressLogger,
)
