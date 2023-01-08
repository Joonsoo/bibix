package com.giyeok.bibix.interpreter.expr

import com.giyeok.bibix.ast.BibixAst
import com.giyeok.bibix.base.BibixValue
import com.giyeok.bibix.base.CName
import com.giyeok.bibix.base.SourceId
import com.giyeok.bibix.interpreter.BibixType
import com.giyeok.bibix.interpreter.name.NameLookupContext
import org.codehaus.plexus.classworlds.realm.ClassRealm

sealed class EvaluationResult {
  open fun ensureValue(): BibixValue = throw IllegalStateException()

  data class Value(val value: BibixValue) : EvaluationResult() {
    override fun ensureValue(): BibixValue = value
  }

  data class Namespace(val context: NameLookupContext) : EvaluationResult()

  data class Param(
    val name: String,
    val optional: Boolean,
    val type: BibixType,
    val defaultValue: BibixAst.Expr?,
  )

  sealed class Callable : EvaluationResult() {
    abstract val context: NameLookupContext
    abstract val params: List<Param>
  }

  sealed class RuleDef : Callable() {
    abstract val cls: Class<*>
    abstract val methodName: String

    sealed class BuildRuleDef : RuleDef() {
      abstract val returnType: BibixType

      data class PreloadedBuildRuleDef(
        override val context: NameLookupContext,
        override val params: List<Param>,
        override val returnType: BibixType,
        override val cls: Class<*>,
        override val methodName: String
      ) : BuildRuleDef()

      data class UserBuildRuleDef(
        override val context: NameLookupContext,
        override val params: List<Param>,
        override val returnType: BibixType,
        val realm: ClassRealm,
        val className: String,
        override val methodName: String,
      ) : BuildRuleDef() {
        override val cls: Class<*>
          get() = realm.loadClass(className)
      }
    }

    sealed class ActionRuleDef : RuleDef() {
      data class PreloadedActionRuleDef(
        override val context: NameLookupContext,
        override val params: List<Param>,
        override val cls: Class<*>,
        override val methodName: String,
      ) : ActionRuleDef()

      data class UserActionRuleDef(
        override val context: NameLookupContext,
        override val params: List<Param>,
        val realm: ClassRealm,
        val className: String,
        override val methodName: String
      ) : ActionRuleDef() {
        override val cls: Class<*>
          get() = realm.loadClass(className)
      }
    }
  }

  data class DataClassDef(
    override val context: NameLookupContext,
    val sourceId: SourceId,
    val packageName: String,
    val className: String,
    override val params: List<Param>,
  ) : Callable()

  data class SuperClassDef(
    val context: NameLookupContext,
    val sourceId: SourceId,
    val packageName: String,
    val className: String,
    val subClasses: List<String>,
  ) : EvaluationResult()

  data class EnumDef(
    val sourceId: SourceId,
    val packageName: String,
    val enumName: String,
    val enumValues: List<String>,
  ) : EvaluationResult()
}
