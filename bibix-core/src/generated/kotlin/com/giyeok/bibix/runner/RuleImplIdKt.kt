//Generated by the protocol buffer compiler. DO NOT EDIT!
// source: ids.proto

package com.giyeok.bibix.runner;

@kotlin.jvm.JvmName("-initializeruleImplId")
public inline fun ruleImplId(block: com.giyeok.bibix.runner.RuleImplIdKt.Dsl.() -> kotlin.Unit): com.giyeok.bibix.runner.BibixIdProto.RuleImplId =
  com.giyeok.bibix.runner.RuleImplIdKt.Dsl._create(com.giyeok.bibix.runner.BibixIdProto.RuleImplId.newBuilder()).apply { block() }._build()
public object RuleImplIdKt {
  @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
  @com.google.protobuf.kotlin.ProtoDslMarker
  public class Dsl private constructor(
    private val _builder: com.giyeok.bibix.runner.BibixIdProto.RuleImplId.Builder
  ) {
    public companion object {
      @kotlin.jvm.JvmSynthetic
      @kotlin.PublishedApi
      internal fun _create(builder: com.giyeok.bibix.runner.BibixIdProto.RuleImplId.Builder): Dsl = Dsl(builder)
    }

    @kotlin.jvm.JvmSynthetic
    @kotlin.PublishedApi
    internal fun _build(): com.giyeok.bibix.runner.BibixIdProto.RuleImplId = _builder.build()

    /**
     * <pre>
     * bibix version
     * </pre>
     *
     * <code>string native_rule_impl = 1;</code>
     */
    public var nativeRuleImpl: kotlin.String
      @JvmName("getNativeRuleImpl")
      get() = _builder.getNativeRuleImpl()
      @JvmName("setNativeRuleImpl")
      set(value) {
        _builder.setNativeRuleImpl(value)
      }
    /**
     * <pre>
     * bibix version
     * </pre>
     *
     * <code>string native_rule_impl = 1;</code>
     */
    public fun clearNativeRuleImpl() {
      _builder.clearNativeRuleImpl()
    }
    /**
     * <pre>
     * bibix version
     * </pre>
     *
     * <code>string native_rule_impl = 1;</code>
     * @return Whether the nativeRuleImpl field is set.
     */
    public fun hasNativeRuleImpl(): kotlin.Boolean {
      return _builder.hasNativeRuleImpl()
    }

    /**
     * <code>.com.giyeok.bibix.runner.ObjectId rule_impl_object_id = 2;</code>
     */
    public var ruleImplObjectId: com.giyeok.bibix.runner.BibixIdProto.ObjectId
      @JvmName("getRuleImplObjectId")
      get() = _builder.getRuleImplObjectId()
      @JvmName("setRuleImplObjectId")
      set(value) {
        _builder.setRuleImplObjectId(value)
      }
    /**
     * <code>.com.giyeok.bibix.runner.ObjectId rule_impl_object_id = 2;</code>
     */
    public fun clearRuleImplObjectId() {
      _builder.clearRuleImplObjectId()
    }
    /**
     * <code>.com.giyeok.bibix.runner.ObjectId rule_impl_object_id = 2;</code>
     * @return Whether the ruleImplObjectId field is set.
     */
    public fun hasRuleImplObjectId(): kotlin.Boolean {
      return _builder.hasRuleImplObjectId()
    }
    public val implIdCase: com.giyeok.bibix.runner.BibixIdProto.RuleImplId.ImplIdCase
      @JvmName("getImplIdCase")
      get() = _builder.getImplIdCase()

    public fun clearImplId() {
      _builder.clearImplId()
    }
  }
}
@kotlin.jvm.JvmSynthetic
public inline fun com.giyeok.bibix.runner.BibixIdProto.RuleImplId.copy(block: com.giyeok.bibix.runner.RuleImplIdKt.Dsl.() -> kotlin.Unit): com.giyeok.bibix.runner.BibixIdProto.RuleImplId =
  com.giyeok.bibix.runner.RuleImplIdKt.Dsl._create(this.toBuilder()).apply { block() }._build()

val com.giyeok.bibix.runner.BibixIdProto.RuleImplIdOrBuilder.ruleImplObjectIdOrNull: com.giyeok.bibix.runner.BibixIdProto.ObjectId?
  get() = if (hasRuleImplObjectId()) getRuleImplObjectId() else null

