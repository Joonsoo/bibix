//Generated by the protocol buffer compiler. DO NOT EDIT!
// source: api.proto

package com.giyeok.bibix.intellij;

@kotlin.jvm.JvmSynthetic
public inline fun executeActionsReq(block: com.giyeok.bibix.intellij.ExecuteActionsReqKt.Dsl.() -> kotlin.Unit): com.giyeok.bibix.intellij.BibixIntellijProto.ExecuteActionsReq =
  com.giyeok.bibix.intellij.ExecuteActionsReqKt.Dsl._create(com.giyeok.bibix.intellij.BibixIntellijProto.ExecuteActionsReq.newBuilder()).apply { block() }._build()
public object ExecuteActionsReqKt {
  @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
  @com.google.protobuf.kotlin.ProtoDslMarker
  public class Dsl private constructor(
    private val _builder: com.giyeok.bibix.intellij.BibixIntellijProto.ExecuteActionsReq.Builder
  ) {
    public companion object {
      @kotlin.jvm.JvmSynthetic
      @kotlin.PublishedApi
      internal fun _create(builder: com.giyeok.bibix.intellij.BibixIntellijProto.ExecuteActionsReq.Builder): Dsl = Dsl(builder)
    }

    @kotlin.jvm.JvmSynthetic
    @kotlin.PublishedApi
    internal fun _build(): com.giyeok.bibix.intellij.BibixIntellijProto.ExecuteActionsReq = _builder.build()

    /**
     * <code>string project_id = 1;</code>
     */
    public var projectId: kotlin.String
      @JvmName("getProjectId")
      get() = _builder.getProjectId()
      @JvmName("setProjectId")
      set(value) {
        _builder.setProjectId(value)
      }
    /**
     * <code>string project_id = 1;</code>
     */
    public fun clearProjectId() {
      _builder.clearProjectId()
    }

    /**
     * An uninstantiable, behaviorless type to represent the field in
     * generics.
     */
    @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
    public class ActionsProxy private constructor() : com.google.protobuf.kotlin.DslProxy()
    /**
     * <code>repeated string actions = 2;</code>
     * @return A list containing the actions.
     */
    public val actions: com.google.protobuf.kotlin.DslList<kotlin.String, ActionsProxy>
      @kotlin.jvm.JvmSynthetic
      get() = com.google.protobuf.kotlin.DslList(
        _builder.getActionsList()
      )
    /**
     * <code>repeated string actions = 2;</code>
     * @param value The actions to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("addActions")
    public fun com.google.protobuf.kotlin.DslList<kotlin.String, ActionsProxy>.add(value: kotlin.String) {
      _builder.addActions(value)
    }
    /**
     * <code>repeated string actions = 2;</code>
     * @param value The actions to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("plusAssignActions")
    @Suppress("NOTHING_TO_INLINE")
    public inline operator fun com.google.protobuf.kotlin.DslList<kotlin.String, ActionsProxy>.plusAssign(value: kotlin.String) {
      add(value)
    }
    /**
     * <code>repeated string actions = 2;</code>
     * @param values The actions to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("addAllActions")
    public fun com.google.protobuf.kotlin.DslList<kotlin.String, ActionsProxy>.addAll(values: kotlin.collections.Iterable<kotlin.String>) {
      _builder.addAllActions(values)
    }
    /**
     * <code>repeated string actions = 2;</code>
     * @param values The actions to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("plusAssignAllActions")
    @Suppress("NOTHING_TO_INLINE")
    public inline operator fun com.google.protobuf.kotlin.DslList<kotlin.String, ActionsProxy>.plusAssign(values: kotlin.collections.Iterable<kotlin.String>) {
      addAll(values)
    }
    /**
     * <code>repeated string actions = 2;</code>
     * @param index The index to set the value at.
     * @param value The actions to set.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("setActions")
    public operator fun com.google.protobuf.kotlin.DslList<kotlin.String, ActionsProxy>.set(index: kotlin.Int, value: kotlin.String) {
      _builder.setActions(index, value)
    }/**
     * <code>repeated string actions = 2;</code>
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("clearActions")
    public fun com.google.protobuf.kotlin.DslList<kotlin.String, ActionsProxy>.clear() {
      _builder.clearActions()
    }}
}
@kotlin.jvm.JvmSynthetic
public inline fun com.giyeok.bibix.intellij.BibixIntellijProto.ExecuteActionsReq.copy(block: com.giyeok.bibix.intellij.ExecuteActionsReqKt.Dsl.() -> kotlin.Unit): com.giyeok.bibix.intellij.BibixIntellijProto.ExecuteActionsReq =
  com.giyeok.bibix.intellij.ExecuteActionsReqKt.Dsl._create(this.toBuilder()).apply { block() }._build()
