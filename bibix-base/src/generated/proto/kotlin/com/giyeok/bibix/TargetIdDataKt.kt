//Generated by the protocol buffer compiler. DO NOT EDIT!
// source: ids.proto

package com.giyeok.bibix;

@kotlin.jvm.JvmName("-initializetargetIdData")
inline fun targetIdData(block: com.giyeok.bibix.TargetIdDataKt.Dsl.() -> kotlin.Unit): com.giyeok.bibix.BibixIdProto.TargetIdData =
  com.giyeok.bibix.TargetIdDataKt.Dsl._create(com.giyeok.bibix.BibixIdProto.TargetIdData.newBuilder()).apply { block() }._build()
object TargetIdDataKt {
  @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
  @com.google.protobuf.kotlin.ProtoDslMarker
  class Dsl private constructor(
    private val _builder: com.giyeok.bibix.BibixIdProto.TargetIdData.Builder
  ) {
    companion object {
      @kotlin.jvm.JvmSynthetic
      @kotlin.PublishedApi
      internal fun _create(builder: com.giyeok.bibix.BibixIdProto.TargetIdData.Builder): Dsl = Dsl(builder)
    }

    @kotlin.jvm.JvmSynthetic
    @kotlin.PublishedApi
    internal fun _build(): com.giyeok.bibix.BibixIdProto.TargetIdData = _builder.build()

    /**
     * <code>.com.giyeok.bibix.SourceId source_id = 1;</code>
     */
    var sourceId: com.giyeok.bibix.BibixIdProto.SourceId
      @JvmName("getSourceId")
      get() = _builder.getSourceId()
      @JvmName("setSourceId")
      set(value) {
        _builder.setSourceId(value)
      }
    /**
     * <code>.com.giyeok.bibix.SourceId source_id = 1;</code>
     */
    fun clearSourceId() {
      _builder.clearSourceId()
    }
    /**
     * <code>.com.giyeok.bibix.SourceId source_id = 1;</code>
     * @return Whether the sourceId field is set.
     */
    fun hasSourceId(): kotlin.Boolean {
      return _builder.hasSourceId()
    }

    /**
     * <pre>
     * string target_name = 2;
     * </pre>
     *
     * <code>.com.giyeok.bibix.SourceId build_rule_source_id = 2;</code>
     */
    var buildRuleSourceId: com.giyeok.bibix.BibixIdProto.SourceId
      @JvmName("getBuildRuleSourceId")
      get() = _builder.getBuildRuleSourceId()
      @JvmName("setBuildRuleSourceId")
      set(value) {
        _builder.setBuildRuleSourceId(value)
      }
    /**
     * <pre>
     * string target_name = 2;
     * </pre>
     *
     * <code>.com.giyeok.bibix.SourceId build_rule_source_id = 2;</code>
     */
    fun clearBuildRuleSourceId() {
      _builder.clearBuildRuleSourceId()
    }
    /**
     * <pre>
     * string target_name = 2;
     * </pre>
     *
     * <code>.com.giyeok.bibix.SourceId build_rule_source_id = 2;</code>
     * @return Whether the buildRuleSourceId field is set.
     */
    fun hasBuildRuleSourceId(): kotlin.Boolean {
      return _builder.hasBuildRuleSourceId()
    }

    /**
     * <code>.google.protobuf.Empty native_impl = 3;</code>
     */
    var nativeImpl: com.google.protobuf.Empty
      @JvmName("getNativeImpl")
      get() = _builder.getNativeImpl()
      @JvmName("setNativeImpl")
      set(value) {
        _builder.setNativeImpl(value)
      }
    /**
     * <code>.google.protobuf.Empty native_impl = 3;</code>
     */
    fun clearNativeImpl() {
      _builder.clearNativeImpl()
    }
    /**
     * <code>.google.protobuf.Empty native_impl = 3;</code>
     * @return Whether the nativeImpl field is set.
     */
    fun hasNativeImpl(): kotlin.Boolean {
      return _builder.hasNativeImpl()
    }

    /**
     * <code>.com.giyeok.bibix.BuildRuleImplId build_rule_impl_id = 4;</code>
     */
    var buildRuleImplId: com.giyeok.bibix.BibixIdProto.BuildRuleImplId
      @JvmName("getBuildRuleImplId")
      get() = _builder.getBuildRuleImplId()
      @JvmName("setBuildRuleImplId")
      set(value) {
        _builder.setBuildRuleImplId(value)
      }
    /**
     * <code>.com.giyeok.bibix.BuildRuleImplId build_rule_impl_id = 4;</code>
     */
    fun clearBuildRuleImplId() {
      _builder.clearBuildRuleImplId()
    }
    /**
     * <code>.com.giyeok.bibix.BuildRuleImplId build_rule_impl_id = 4;</code>
     * @return Whether the buildRuleImplId field is set.
     */
    fun hasBuildRuleImplId(): kotlin.Boolean {
      return _builder.hasBuildRuleImplId()
    }

    /**
     * <code>string build_rule_class_name = 5;</code>
     */
    var buildRuleClassName: kotlin.String
      @JvmName("getBuildRuleClassName")
      get() = _builder.getBuildRuleClassName()
      @JvmName("setBuildRuleClassName")
      set(value) {
        _builder.setBuildRuleClassName(value)
      }
    /**
     * <code>string build_rule_class_name = 5;</code>
     */
    fun clearBuildRuleClassName() {
      _builder.clearBuildRuleClassName()
    }

    /**
     * <code>string build_rule_method_name = 6;</code>
     */
    var buildRuleMethodName: kotlin.String
      @JvmName("getBuildRuleMethodName")
      get() = _builder.getBuildRuleMethodName()
      @JvmName("setBuildRuleMethodName")
      set(value) {
        _builder.setBuildRuleMethodName(value)
      }
    /**
     * <code>string build_rule_method_name = 6;</code>
     */
    fun clearBuildRuleMethodName() {
      _builder.clearBuildRuleMethodName()
    }

    /**
     * <code>.com.giyeok.bibix.ArgsMap args_map = 7;</code>
     */
    var argsMap: com.giyeok.bibix.BibixIdProto.ArgsMap
      @JvmName("getArgsMap")
      get() = _builder.getArgsMap()
      @JvmName("setArgsMap")
      set(value) {
        _builder.setArgsMap(value)
      }
    /**
     * <code>.com.giyeok.bibix.ArgsMap args_map = 7;</code>
     */
    fun clearArgsMap() {
      _builder.clearArgsMap()
    }
    /**
     * <code>.com.giyeok.bibix.ArgsMap args_map = 7;</code>
     * @return Whether the argsMap field is set.
     */
    fun hasArgsMap(): kotlin.Boolean {
      return _builder.hasArgsMap()
    }
    val buildRuleCase: com.giyeok.bibix.BibixIdProto.TargetIdData.BuildRuleCase
      @JvmName("getBuildRuleCase")
      get() = _builder.getBuildRuleCase()

    fun clearBuildRule() {
      _builder.clearBuildRule()
    }
  }
}
@kotlin.jvm.JvmSynthetic
inline fun com.giyeok.bibix.BibixIdProto.TargetIdData.copy(block: com.giyeok.bibix.TargetIdDataKt.Dsl.() -> kotlin.Unit): com.giyeok.bibix.BibixIdProto.TargetIdData =
  com.giyeok.bibix.TargetIdDataKt.Dsl._create(this.toBuilder()).apply { block() }._build()

val com.giyeok.bibix.BibixIdProto.TargetIdDataOrBuilder.sourceIdOrNull: com.giyeok.bibix.BibixIdProto.SourceId?
  get() = if (hasSourceId()) getSourceId() else null

val com.giyeok.bibix.BibixIdProto.TargetIdDataOrBuilder.buildRuleSourceIdOrNull: com.giyeok.bibix.BibixIdProto.SourceId?
  get() = if (hasBuildRuleSourceId()) getBuildRuleSourceId() else null

val com.giyeok.bibix.BibixIdProto.TargetIdDataOrBuilder.nativeImplOrNull: com.google.protobuf.Empty?
  get() = if (hasNativeImpl()) getNativeImpl() else null

val com.giyeok.bibix.BibixIdProto.TargetIdDataOrBuilder.buildRuleImplIdOrNull: com.giyeok.bibix.BibixIdProto.BuildRuleImplId?
  get() = if (hasBuildRuleImplId()) getBuildRuleImplId() else null

val com.giyeok.bibix.BibixIdProto.TargetIdDataOrBuilder.argsMapOrNull: com.giyeok.bibix.BibixIdProto.ArgsMap?
  get() = if (hasArgsMap()) getArgsMap() else null
