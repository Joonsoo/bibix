//Generated by the protocol buffer compiler. DO NOT EDIT!
// source: api.proto

package com.giyeok.bibix.daemon;

@kotlin.jvm.JvmSynthetic
public inline fun namespaceInfo(block: com.giyeok.bibix.daemon.NamespaceInfoKt.Dsl.() -> kotlin.Unit): com.giyeok.bibix.daemon.BibixDaemonApiProto.NamespaceInfo =
  com.giyeok.bibix.daemon.NamespaceInfoKt.Dsl._create(com.giyeok.bibix.daemon.BibixDaemonApiProto.NamespaceInfo.newBuilder()).apply { block() }._build()
public object NamespaceInfoKt {
  @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
  @com.google.protobuf.kotlin.ProtoDslMarker
  public class Dsl private constructor(
    private val _builder: com.giyeok.bibix.daemon.BibixDaemonApiProto.NamespaceInfo.Builder
  ) {
    public companion object {
      @kotlin.jvm.JvmSynthetic
      @kotlin.PublishedApi
      internal fun _create(builder: com.giyeok.bibix.daemon.BibixDaemonApiProto.NamespaceInfo.Builder): Dsl = Dsl(builder)
    }

    @kotlin.jvm.JvmSynthetic
    @kotlin.PublishedApi
    internal fun _build(): com.giyeok.bibix.daemon.BibixDaemonApiProto.NamespaceInfo = _builder.build()

    /**
     * An uninstantiable, behaviorless type to represent the field in
     * generics.
     */
    @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
    public class NamespacesProxy private constructor() : com.google.protobuf.kotlin.DslProxy()
    /**
     * <code>repeated .com.giyeok.bibix.daemon.NamespaceInfo namespaces = 1;</code>
     */
     public val namespaces: com.google.protobuf.kotlin.DslList<com.giyeok.bibix.daemon.BibixDaemonApiProto.NamespaceInfo, NamespacesProxy>
      @kotlin.jvm.JvmSynthetic
      get() = com.google.protobuf.kotlin.DslList(
        _builder.getNamespacesList()
      )
    /**
     * <code>repeated .com.giyeok.bibix.daemon.NamespaceInfo namespaces = 1;</code>
     * @param value The namespaces to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("addNamespaces")
    public fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.daemon.BibixDaemonApiProto.NamespaceInfo, NamespacesProxy>.add(value: com.giyeok.bibix.daemon.BibixDaemonApiProto.NamespaceInfo) {
      _builder.addNamespaces(value)
    }/**
     * <code>repeated .com.giyeok.bibix.daemon.NamespaceInfo namespaces = 1;</code>
     * @param value The namespaces to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("plusAssignNamespaces")
    @Suppress("NOTHING_TO_INLINE")
    public inline operator fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.daemon.BibixDaemonApiProto.NamespaceInfo, NamespacesProxy>.plusAssign(value: com.giyeok.bibix.daemon.BibixDaemonApiProto.NamespaceInfo) {
      add(value)
    }/**
     * <code>repeated .com.giyeok.bibix.daemon.NamespaceInfo namespaces = 1;</code>
     * @param values The namespaces to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("addAllNamespaces")
    public fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.daemon.BibixDaemonApiProto.NamespaceInfo, NamespacesProxy>.addAll(values: kotlin.collections.Iterable<com.giyeok.bibix.daemon.BibixDaemonApiProto.NamespaceInfo>) {
      _builder.addAllNamespaces(values)
    }/**
     * <code>repeated .com.giyeok.bibix.daemon.NamespaceInfo namespaces = 1;</code>
     * @param values The namespaces to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("plusAssignAllNamespaces")
    @Suppress("NOTHING_TO_INLINE")
    public inline operator fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.daemon.BibixDaemonApiProto.NamespaceInfo, NamespacesProxy>.plusAssign(values: kotlin.collections.Iterable<com.giyeok.bibix.daemon.BibixDaemonApiProto.NamespaceInfo>) {
      addAll(values)
    }/**
     * <code>repeated .com.giyeok.bibix.daemon.NamespaceInfo namespaces = 1;</code>
     * @param index The index to set the value at.
     * @param value The namespaces to set.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("setNamespaces")
    public operator fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.daemon.BibixDaemonApiProto.NamespaceInfo, NamespacesProxy>.set(index: kotlin.Int, value: com.giyeok.bibix.daemon.BibixDaemonApiProto.NamespaceInfo) {
      _builder.setNamespaces(index, value)
    }/**
     * <code>repeated .com.giyeok.bibix.daemon.NamespaceInfo namespaces = 1;</code>
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("clearNamespaces")
    public fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.daemon.BibixDaemonApiProto.NamespaceInfo, NamespacesProxy>.clear() {
      _builder.clearNamespaces()
    }
    /**
     * An uninstantiable, behaviorless type to represent the field in
     * generics.
     */
    @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
    public class TargetsProxy private constructor() : com.google.protobuf.kotlin.DslProxy()
    /**
     * <code>repeated .com.giyeok.bibix.daemon.TargetInfo targets = 2;</code>
     */
     public val targets: com.google.protobuf.kotlin.DslList<com.giyeok.bibix.daemon.BibixDaemonApiProto.TargetInfo, TargetsProxy>
      @kotlin.jvm.JvmSynthetic
      get() = com.google.protobuf.kotlin.DslList(
        _builder.getTargetsList()
      )
    /**
     * <code>repeated .com.giyeok.bibix.daemon.TargetInfo targets = 2;</code>
     * @param value The targets to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("addTargets")
    public fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.daemon.BibixDaemonApiProto.TargetInfo, TargetsProxy>.add(value: com.giyeok.bibix.daemon.BibixDaemonApiProto.TargetInfo) {
      _builder.addTargets(value)
    }/**
     * <code>repeated .com.giyeok.bibix.daemon.TargetInfo targets = 2;</code>
     * @param value The targets to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("plusAssignTargets")
    @Suppress("NOTHING_TO_INLINE")
    public inline operator fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.daemon.BibixDaemonApiProto.TargetInfo, TargetsProxy>.plusAssign(value: com.giyeok.bibix.daemon.BibixDaemonApiProto.TargetInfo) {
      add(value)
    }/**
     * <code>repeated .com.giyeok.bibix.daemon.TargetInfo targets = 2;</code>
     * @param values The targets to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("addAllTargets")
    public fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.daemon.BibixDaemonApiProto.TargetInfo, TargetsProxy>.addAll(values: kotlin.collections.Iterable<com.giyeok.bibix.daemon.BibixDaemonApiProto.TargetInfo>) {
      _builder.addAllTargets(values)
    }/**
     * <code>repeated .com.giyeok.bibix.daemon.TargetInfo targets = 2;</code>
     * @param values The targets to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("plusAssignAllTargets")
    @Suppress("NOTHING_TO_INLINE")
    public inline operator fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.daemon.BibixDaemonApiProto.TargetInfo, TargetsProxy>.plusAssign(values: kotlin.collections.Iterable<com.giyeok.bibix.daemon.BibixDaemonApiProto.TargetInfo>) {
      addAll(values)
    }/**
     * <code>repeated .com.giyeok.bibix.daemon.TargetInfo targets = 2;</code>
     * @param index The index to set the value at.
     * @param value The targets to set.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("setTargets")
    public operator fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.daemon.BibixDaemonApiProto.TargetInfo, TargetsProxy>.set(index: kotlin.Int, value: com.giyeok.bibix.daemon.BibixDaemonApiProto.TargetInfo) {
      _builder.setTargets(index, value)
    }/**
     * <code>repeated .com.giyeok.bibix.daemon.TargetInfo targets = 2;</code>
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("clearTargets")
    public fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.daemon.BibixDaemonApiProto.TargetInfo, TargetsProxy>.clear() {
      _builder.clearTargets()
    }
    /**
     * An uninstantiable, behaviorless type to represent the field in
     * generics.
     */
    @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
    public class ActionsProxy private constructor() : com.google.protobuf.kotlin.DslProxy()
    /**
     * <code>repeated .com.giyeok.bibix.daemon.ActionInfo actions = 3;</code>
     */
     public val actions: com.google.protobuf.kotlin.DslList<com.giyeok.bibix.daemon.BibixDaemonApiProto.ActionInfo, ActionsProxy>
      @kotlin.jvm.JvmSynthetic
      get() = com.google.protobuf.kotlin.DslList(
        _builder.getActionsList()
      )
    /**
     * <code>repeated .com.giyeok.bibix.daemon.ActionInfo actions = 3;</code>
     * @param value The actions to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("addActions")
    public fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.daemon.BibixDaemonApiProto.ActionInfo, ActionsProxy>.add(value: com.giyeok.bibix.daemon.BibixDaemonApiProto.ActionInfo) {
      _builder.addActions(value)
    }/**
     * <code>repeated .com.giyeok.bibix.daemon.ActionInfo actions = 3;</code>
     * @param value The actions to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("plusAssignActions")
    @Suppress("NOTHING_TO_INLINE")
    public inline operator fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.daemon.BibixDaemonApiProto.ActionInfo, ActionsProxy>.plusAssign(value: com.giyeok.bibix.daemon.BibixDaemonApiProto.ActionInfo) {
      add(value)
    }/**
     * <code>repeated .com.giyeok.bibix.daemon.ActionInfo actions = 3;</code>
     * @param values The actions to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("addAllActions")
    public fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.daemon.BibixDaemonApiProto.ActionInfo, ActionsProxy>.addAll(values: kotlin.collections.Iterable<com.giyeok.bibix.daemon.BibixDaemonApiProto.ActionInfo>) {
      _builder.addAllActions(values)
    }/**
     * <code>repeated .com.giyeok.bibix.daemon.ActionInfo actions = 3;</code>
     * @param values The actions to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("plusAssignAllActions")
    @Suppress("NOTHING_TO_INLINE")
    public inline operator fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.daemon.BibixDaemonApiProto.ActionInfo, ActionsProxy>.plusAssign(values: kotlin.collections.Iterable<com.giyeok.bibix.daemon.BibixDaemonApiProto.ActionInfo>) {
      addAll(values)
    }/**
     * <code>repeated .com.giyeok.bibix.daemon.ActionInfo actions = 3;</code>
     * @param index The index to set the value at.
     * @param value The actions to set.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("setActions")
    public operator fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.daemon.BibixDaemonApiProto.ActionInfo, ActionsProxy>.set(index: kotlin.Int, value: com.giyeok.bibix.daemon.BibixDaemonApiProto.ActionInfo) {
      _builder.setActions(index, value)
    }/**
     * <code>repeated .com.giyeok.bibix.daemon.ActionInfo actions = 3;</code>
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("clearActions")
    public fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.daemon.BibixDaemonApiProto.ActionInfo, ActionsProxy>.clear() {
      _builder.clearActions()
    }}
}
@kotlin.jvm.JvmSynthetic
public inline fun com.giyeok.bibix.daemon.BibixDaemonApiProto.NamespaceInfo.copy(block: com.giyeok.bibix.daemon.NamespaceInfoKt.Dsl.() -> kotlin.Unit): com.giyeok.bibix.daemon.BibixDaemonApiProto.NamespaceInfo =
  com.giyeok.bibix.daemon.NamespaceInfoKt.Dsl._create(this.toBuilder()).apply { block() }._build()
