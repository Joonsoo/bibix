package com.giyeok.bibix.base

data class ActionContext(
  val sourceId: SourceId,
  val arguments: Map<String, BibixValue>,
  val progressLogger: ProgressLogger,
)
