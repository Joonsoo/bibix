package com.giyeok.bibix.runner

import com.giyeok.bibix.base.SourceId

data class BuildRuleImplInfo(
  val buildRuleValue: CNameValue.BuildRuleValue,
  val origin: SourceId,
  val implObjectIdHash: BibixIdProto.ObjectIdHash,
  val cls: Class<*>,
  val methodName: String?,
  val params: List<Param>,
  val returnType: BibixType
)

data class ActionRuleImplInfo(
  val actionRuleValue: CNameValue.ActionRuleValue,
  val origin: SourceId,
  val cls: Class<*>,
  val methodName: String?,
  val params: List<Param>,
)
