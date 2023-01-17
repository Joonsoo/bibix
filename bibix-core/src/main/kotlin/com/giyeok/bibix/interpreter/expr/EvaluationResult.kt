package com.giyeok.bibix.interpreter.expr

import com.giyeok.bibix.ast.BibixAst
import com.giyeok.bibix.base.*
import org.codehaus.plexus.classworlds.realm.ClassRealm

sealed class EvaluationResult {

  fun ensureValue(): BibixValue = tryEnsureValue() ?: throw IllegalStateException()
  open fun tryEnsureValue(): BibixValue? = null

  data class Value(val value: BibixValue) : EvaluationResult() {
    override fun tryEnsureValue(): BibixValue = value
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
    abstract val name: CName
    abstract val className: String
    abstract val cls: Class<*>
    abstract val methodName: String

    sealed class BuildRuleDef : RuleDef() {
      abstract val returnType: BibixType

      data class NativeBuildRuleDef(
        override val name: CName,
        override val context: NameLookupContext,
        override val params: List<Param>,
        override val returnType: BibixType,
        override val cls: Class<*>,
        override val methodName: String
      ) : BuildRuleDef() {
        override val className: String get() = cls.canonicalName
      }

      data class UserBuildRuleDef(
        override val name: CName,
        override val context: NameLookupContext,
        override val params: List<Param>,
        override val returnType: BibixType,
        val implValue: ClassInstanceValue,
        val realm: ClassRealm,
        override val className: String,
        override val methodName: String,
      ) : BuildRuleDef() {
        override val cls: Class<*> get() = realm.loadClass(className)
      }
    }

    sealed class ActionRuleDef : RuleDef() {
      data class PreloadedActionRuleDef(
        override val name: CName,
        override val context: NameLookupContext,
        override val params: List<Param>,
        override val cls: Class<*>,
        override val methodName: String,
      ) : ActionRuleDef() {
        override val className: String get() = cls.canonicalName
      }

      data class UserActionRuleDef(
        override val name: CName,
        override val context: NameLookupContext,
        override val params: List<Param>,
        val realm: ClassRealm,
        override val className: String,
        override val methodName: String
      ) : ActionRuleDef() {
        override val cls: Class<*> get() = realm.loadClass(className)
      }
    }
  }

  data class DataClassDef(
    override val context: NameLookupContext,
    val packageName: String,
    val className: String,
    override val params: List<Param>,
    val bodyElems: List<BibixAst.ClassBodyElem>,
  ) : Callable()

  data class SuperClassDef(
    val context: NameLookupContext,
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
