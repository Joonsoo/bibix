package com.giyeok.bibix.interpreter.expr

import com.giyeok.bibix.base.BibixValue
import com.giyeok.bibix.base.CName
import com.giyeok.bibix.base.SourceId
import com.giyeok.bibix.interpreter.name.NameLookupContext
import org.codehaus.plexus.classworlds.realm.ClassRealm

sealed class EvaluationResult {
  open fun ensureValue(): BibixValue = throw IllegalStateException()

  data class Value(val value: BibixValue) : EvaluationResult() {
    override fun ensureValue(): BibixValue = value
  }

  data class Namespace(val context: NameLookupContext) : EvaluationResult()

  data class PreloadedBuildRuleDef(val cls: Class<*>, val methodName: String) : EvaluationResult()

  data class UserBuildRuleDef(
    val realm: ClassRealm,
    val className: String,
    val methodName: String,
    // TODO params and return type
  ) : EvaluationResult()

  data class PreloadedActionRuleDef(val cname: CName) : EvaluationResult()

  data class UserActionRuleDef(
    val realm: ClassRealm,
    val className: String,
    val methodName: String
  ) : EvaluationResult()

  data class DataClassDef(val sourceId: SourceId, val packageName: String, val className: String) :
    EvaluationResult()

  data class SuperClassDef(val cname: CName) : EvaluationResult()

  data class EnumDef(val cname: CName) : EvaluationResult()
}
