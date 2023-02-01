package com.giyeok.bibix.interpreter.expr

import com.giyeok.bibix.ast.BibixAst
import com.giyeok.bibix.base.*
import com.giyeok.bibix.repo.ObjectHash

sealed class EvaluationResult {

  fun ensureValue(): BibixValue = tryEnsureValue()
    ?: throw IllegalStateException("$this cannot be converted to a value")

  open fun tryEnsureValue(): BibixValue? = null

  data class Value(val value: BibixValue) : EvaluationResult() {
    override fun tryEnsureValue(): BibixValue = value
  }

  data class ValueWithObjectHash(val value: BibixValue, val objectHash: ObjectHash) :
    EvaluationResult() {
    override fun tryEnsureValue(): BibixValue = value
  }

  data class Namespace(val context: NameLookupContext) : EvaluationResult()

  data class Param(
    val name: String,
    val optional: Boolean,
    val type: BibixType,
    val defaultValue: BibixAst.Expr?,
  ) {
    fun toRuleParamValue(): RuleParam = RuleParam(name, type.toTypeValue(), optional)
  }

  sealed class Callable : EvaluationResult() {
    abstract val context: NameLookupContext
    abstract val params: List<Param>
  }

  sealed class RuleDef : Callable() {
    abstract val name: CName
    abstract val className: String
    abstract val methodName: String

    sealed class BuildRuleDef : RuleDef() {
      abstract val returnType: BibixType

      data class NativeBuildRuleDef(
        override val name: CName,
        override val context: NameLookupContext,
        override val params: List<Param>,
        override val returnType: BibixType,
        val implInstance: Any,
        override val methodName: String
      ) : BuildRuleDef() {
        override val className: String get() = implInstance::class.java.canonicalName
      }

      data class UserBuildRuleDef(
        override val name: CName,
        override val context: NameLookupContext,
        val implTarget: List<String>,
        val thisValue: BibixValue?,
        override val params: List<Param>,
        override val returnType: BibixType,
        override val className: String,
        override val methodName: String,
      ) : BuildRuleDef() {
        override fun tryEnsureValue(): BibixValue {
          val paramsValue = params.map { it.toRuleParamValue() }
          return BuildRuleDefValue(name, paramsValue, className, methodName)
        }
      }
    }

    sealed class ActionRuleDef : RuleDef() {
      data class PreloadedActionRuleDef(
        override val name: CName,
        override val context: NameLookupContext,
        override val params: List<Param>,
        val implInstance: Any,
        override val methodName: String,
      ) : ActionRuleDef() {
        override val className: String get() = implInstance::class.java.canonicalName
      }

      data class UserActionRuleDef(
        override val name: CName,
        override val context: NameLookupContext,
        val implTarget: List<String>,
        val thisValue: BibixValue?,
        override val params: List<Param>,
        override val className: String,
        override val methodName: String
      ) : ActionRuleDef() {
        override fun tryEnsureValue(): BibixValue {
          val paramsValue = params.map { it.toRuleParamValue() }
          return ActionRuleDefValue(name, paramsValue, className, methodName)
        }
      }
    }
  }

  data class DataClassDef(
    override val context: NameLookupContext,
    val packageName: String,
    val className: String,
    override val params: List<Param>,
    val bodyElems: List<BibixAst.ClassBodyElem>,
  ) : Callable() {
    override fun tryEnsureValue(): BibixValue =
      TypeValue.DataClassTypeValue(packageName, className)
  }

  data class SuperClassDef(
    val context: NameLookupContext,
    val packageName: String,
    val className: String,
    val subClasses: List<String>,
  ) : EvaluationResult() {
    override fun tryEnsureValue(): BibixValue =
      TypeValue.SuperClassTypeValue(packageName, className)
  }

  data class EnumDef(
    val sourceId: SourceId,
    val packageName: String,
    val enumName: String,
    val enumValues: List<String>,
  ) : EvaluationResult() {
    override fun tryEnsureValue(): BibixValue =
      TypeValue.EnumTypeValue(packageName, enumName)
  }
}
