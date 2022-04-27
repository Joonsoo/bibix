package com.giyeok.bibix.runner

import com.giyeok.bibix.base.CName
import com.giyeok.bibix.base.SourceId

// TODO build rule impl info를 native impl용이랑 아닌걸로 분리할 것.
// native impl용은 user가 buildrule로 받을 수 없음(impl name에 접근이 불가하기 때문)
sealed interface BuildRuleImplInfo {
  val origin: SourceId
  val methodName: String
  val params: List<Param>
  val returnType: BibixType

  data class NativeBuildRuleImplInfo(
    val buildRuleValue: CNameValue.BuildRuleValue,
    override val origin: SourceId,
    val implObjectIdHash: BibixIdProto.ObjectIdHash,
    val cls: Class<*>,
    override val methodName: String,
    override val params: List<Param>,
    override val returnType: BibixType
  ) : BuildRuleImplInfo

  data class UserBuildRuleImplInfo(
    val buildRuleValue: CNameValue.BuildRuleValue,
    override val origin: SourceId,
    val implName: CName,
    val className: String,
    override val methodName: String,
    override val params: List<Param>,
    override val returnType: BibixType
  ) : BuildRuleImplInfo
}

data class ActionRuleImplInfo(
  val actionRuleValue: CNameValue.ActionRuleValue,
  val origin: SourceId,
  val cls: Class<*>,
  val methodName: String?,
  val params: List<Param>,
)
