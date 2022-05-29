//Generated by the protocol buffer compiler. DO NOT EDIT!
// source: api.proto

package com.giyeok.bibix.daemon;

@kotlin.jvm.JvmName("-initializeintellijModuleNode")
public inline fun intellijModuleNode(block: com.giyeok.bibix.daemon.IntellijModuleNodeKt.Dsl.() -> kotlin.Unit): com.giyeok.bibix.daemon.BibixDaemonApiProto.IntellijModuleNode =
  com.giyeok.bibix.daemon.IntellijModuleNodeKt.Dsl._create(com.giyeok.bibix.daemon.BibixDaemonApiProto.IntellijModuleNode.newBuilder()).apply { block() }._build()
public object IntellijModuleNodeKt {
  @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
  @com.google.protobuf.kotlin.ProtoDslMarker
  public class Dsl private constructor(
    private val _builder: com.giyeok.bibix.daemon.BibixDaemonApiProto.IntellijModuleNode.Builder
  ) {
    public companion object {
      @kotlin.jvm.JvmSynthetic
      @kotlin.PublishedApi
      internal fun _create(builder: com.giyeok.bibix.daemon.BibixDaemonApiProto.IntellijModuleNode.Builder): Dsl = Dsl(builder)
    }

    @kotlin.jvm.JvmSynthetic
    @kotlin.PublishedApi
    internal fun _build(): com.giyeok.bibix.daemon.BibixDaemonApiProto.IntellijModuleNode = _builder.build()

    /**
     * <code>string name = 1;</code>
     */
    public var name: kotlin.String
      @JvmName("getName")
      get() = _builder.getName()
      @JvmName("setName")
      set(value) {
        _builder.setName(value)
      }
    /**
     * <code>string name = 1;</code>
     */
    public fun clearName() {
      _builder.clearName()
    }

    /**
     * <code>string path = 2;</code>
     */
    public var path: kotlin.String
      @JvmName("getPath")
      get() = _builder.getPath()
      @JvmName("setPath")
      set(value) {
        _builder.setPath(value)
      }
    /**
     * <code>string path = 2;</code>
     */
    public fun clearPath() {
      _builder.clearPath()
    }

    /**
     * <code>string sdk_name = 3;</code>
     */
    public var sdkName: kotlin.String
      @JvmName("getSdkName")
      get() = _builder.getSdkName()
      @JvmName("setSdkName")
      set(value) {
        _builder.setSdkName(value)
      }
    /**
     * <code>string sdk_name = 3;</code>
     */
    public fun clearSdkName() {
      _builder.clearSdkName()
    }

    /**
     * An uninstantiable, behaviorless type to represent the field in
     * generics.
     */
    @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
    public class ModulesProxy private constructor() : com.google.protobuf.kotlin.DslProxy()
    /**
     * <code>repeated .com.giyeok.bibix.daemon.IntellijModuleNode modules = 4;</code>
     */
     public val modules: com.google.protobuf.kotlin.DslList<com.giyeok.bibix.daemon.BibixDaemonApiProto.IntellijModuleNode, ModulesProxy>
      @kotlin.jvm.JvmSynthetic
      get() = com.google.protobuf.kotlin.DslList(
        _builder.getModulesList()
      )
    /**
     * <code>repeated .com.giyeok.bibix.daemon.IntellijModuleNode modules = 4;</code>
     * @param value The modules to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("addModules")
    public fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.daemon.BibixDaemonApiProto.IntellijModuleNode, ModulesProxy>.add(value: com.giyeok.bibix.daemon.BibixDaemonApiProto.IntellijModuleNode) {
      _builder.addModules(value)
    }
    /**
     * <code>repeated .com.giyeok.bibix.daemon.IntellijModuleNode modules = 4;</code>
     * @param value The modules to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("plusAssignModules")
    @Suppress("NOTHING_TO_INLINE")
    public inline operator fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.daemon.BibixDaemonApiProto.IntellijModuleNode, ModulesProxy>.plusAssign(value: com.giyeok.bibix.daemon.BibixDaemonApiProto.IntellijModuleNode) {
      add(value)
    }
    /**
     * <code>repeated .com.giyeok.bibix.daemon.IntellijModuleNode modules = 4;</code>
     * @param values The modules to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("addAllModules")
    public fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.daemon.BibixDaemonApiProto.IntellijModuleNode, ModulesProxy>.addAll(values: kotlin.collections.Iterable<com.giyeok.bibix.daemon.BibixDaemonApiProto.IntellijModuleNode>) {
      _builder.addAllModules(values)
    }
    /**
     * <code>repeated .com.giyeok.bibix.daemon.IntellijModuleNode modules = 4;</code>
     * @param values The modules to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("plusAssignAllModules")
    @Suppress("NOTHING_TO_INLINE")
    public inline operator fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.daemon.BibixDaemonApiProto.IntellijModuleNode, ModulesProxy>.plusAssign(values: kotlin.collections.Iterable<com.giyeok.bibix.daemon.BibixDaemonApiProto.IntellijModuleNode>) {
      addAll(values)
    }
    /**
     * <code>repeated .com.giyeok.bibix.daemon.IntellijModuleNode modules = 4;</code>
     * @param index The index to set the value at.
     * @param value The modules to set.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("setModules")
    public operator fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.daemon.BibixDaemonApiProto.IntellijModuleNode, ModulesProxy>.set(index: kotlin.Int, value: com.giyeok.bibix.daemon.BibixDaemonApiProto.IntellijModuleNode) {
      _builder.setModules(index, value)
    }
    /**
     * <code>repeated .com.giyeok.bibix.daemon.IntellijModuleNode modules = 4;</code>
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("clearModules")
    public fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.daemon.BibixDaemonApiProto.IntellijModuleNode, ModulesProxy>.clear() {
      _builder.clearModules()
    }


    /**
     * An uninstantiable, behaviorless type to represent the field in
     * generics.
     */
    @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
    public class ContentRootsProxy private constructor() : com.google.protobuf.kotlin.DslProxy()
    /**
     * <code>repeated .com.giyeok.bibix.daemon.IntellijContentRootNode content_roots = 5;</code>
     */
     public val contentRoots: com.google.protobuf.kotlin.DslList<com.giyeok.bibix.daemon.BibixDaemonApiProto.IntellijContentRootNode, ContentRootsProxy>
      @kotlin.jvm.JvmSynthetic
      get() = com.google.protobuf.kotlin.DslList(
        _builder.getContentRootsList()
      )
    /**
     * <code>repeated .com.giyeok.bibix.daemon.IntellijContentRootNode content_roots = 5;</code>
     * @param value The contentRoots to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("addContentRoots")
    public fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.daemon.BibixDaemonApiProto.IntellijContentRootNode, ContentRootsProxy>.add(value: com.giyeok.bibix.daemon.BibixDaemonApiProto.IntellijContentRootNode) {
      _builder.addContentRoots(value)
    }
    /**
     * <code>repeated .com.giyeok.bibix.daemon.IntellijContentRootNode content_roots = 5;</code>
     * @param value The contentRoots to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("plusAssignContentRoots")
    @Suppress("NOTHING_TO_INLINE")
    public inline operator fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.daemon.BibixDaemonApiProto.IntellijContentRootNode, ContentRootsProxy>.plusAssign(value: com.giyeok.bibix.daemon.BibixDaemonApiProto.IntellijContentRootNode) {
      add(value)
    }
    /**
     * <code>repeated .com.giyeok.bibix.daemon.IntellijContentRootNode content_roots = 5;</code>
     * @param values The contentRoots to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("addAllContentRoots")
    public fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.daemon.BibixDaemonApiProto.IntellijContentRootNode, ContentRootsProxy>.addAll(values: kotlin.collections.Iterable<com.giyeok.bibix.daemon.BibixDaemonApiProto.IntellijContentRootNode>) {
      _builder.addAllContentRoots(values)
    }
    /**
     * <code>repeated .com.giyeok.bibix.daemon.IntellijContentRootNode content_roots = 5;</code>
     * @param values The contentRoots to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("plusAssignAllContentRoots")
    @Suppress("NOTHING_TO_INLINE")
    public inline operator fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.daemon.BibixDaemonApiProto.IntellijContentRootNode, ContentRootsProxy>.plusAssign(values: kotlin.collections.Iterable<com.giyeok.bibix.daemon.BibixDaemonApiProto.IntellijContentRootNode>) {
      addAll(values)
    }
    /**
     * <code>repeated .com.giyeok.bibix.daemon.IntellijContentRootNode content_roots = 5;</code>
     * @param index The index to set the value at.
     * @param value The contentRoots to set.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("setContentRoots")
    public operator fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.daemon.BibixDaemonApiProto.IntellijContentRootNode, ContentRootsProxy>.set(index: kotlin.Int, value: com.giyeok.bibix.daemon.BibixDaemonApiProto.IntellijContentRootNode) {
      _builder.setContentRoots(index, value)
    }
    /**
     * <code>repeated .com.giyeok.bibix.daemon.IntellijContentRootNode content_roots = 5;</code>
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("clearContentRoots")
    public fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.daemon.BibixDaemonApiProto.IntellijContentRootNode, ContentRootsProxy>.clear() {
      _builder.clearContentRoots()
    }


    /**
     * An uninstantiable, behaviorless type to represent the field in
     * generics.
     */
    @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
    public class TasksProxy private constructor() : com.google.protobuf.kotlin.DslProxy()
    /**
     * <code>repeated .com.giyeok.bibix.daemon.IntellijTaskNode tasks = 6;</code>
     */
     public val tasks: com.google.protobuf.kotlin.DslList<com.giyeok.bibix.daemon.BibixDaemonApiProto.IntellijTaskNode, TasksProxy>
      @kotlin.jvm.JvmSynthetic
      get() = com.google.protobuf.kotlin.DslList(
        _builder.getTasksList()
      )
    /**
     * <code>repeated .com.giyeok.bibix.daemon.IntellijTaskNode tasks = 6;</code>
     * @param value The tasks to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("addTasks")
    public fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.daemon.BibixDaemonApiProto.IntellijTaskNode, TasksProxy>.add(value: com.giyeok.bibix.daemon.BibixDaemonApiProto.IntellijTaskNode) {
      _builder.addTasks(value)
    }
    /**
     * <code>repeated .com.giyeok.bibix.daemon.IntellijTaskNode tasks = 6;</code>
     * @param value The tasks to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("plusAssignTasks")
    @Suppress("NOTHING_TO_INLINE")
    public inline operator fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.daemon.BibixDaemonApiProto.IntellijTaskNode, TasksProxy>.plusAssign(value: com.giyeok.bibix.daemon.BibixDaemonApiProto.IntellijTaskNode) {
      add(value)
    }
    /**
     * <code>repeated .com.giyeok.bibix.daemon.IntellijTaskNode tasks = 6;</code>
     * @param values The tasks to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("addAllTasks")
    public fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.daemon.BibixDaemonApiProto.IntellijTaskNode, TasksProxy>.addAll(values: kotlin.collections.Iterable<com.giyeok.bibix.daemon.BibixDaemonApiProto.IntellijTaskNode>) {
      _builder.addAllTasks(values)
    }
    /**
     * <code>repeated .com.giyeok.bibix.daemon.IntellijTaskNode tasks = 6;</code>
     * @param values The tasks to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("plusAssignAllTasks")
    @Suppress("NOTHING_TO_INLINE")
    public inline operator fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.daemon.BibixDaemonApiProto.IntellijTaskNode, TasksProxy>.plusAssign(values: kotlin.collections.Iterable<com.giyeok.bibix.daemon.BibixDaemonApiProto.IntellijTaskNode>) {
      addAll(values)
    }
    /**
     * <code>repeated .com.giyeok.bibix.daemon.IntellijTaskNode tasks = 6;</code>
     * @param index The index to set the value at.
     * @param value The tasks to set.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("setTasks")
    public operator fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.daemon.BibixDaemonApiProto.IntellijTaskNode, TasksProxy>.set(index: kotlin.Int, value: com.giyeok.bibix.daemon.BibixDaemonApiProto.IntellijTaskNode) {
      _builder.setTasks(index, value)
    }
    /**
     * <code>repeated .com.giyeok.bibix.daemon.IntellijTaskNode tasks = 6;</code>
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("clearTasks")
    public fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.daemon.BibixDaemonApiProto.IntellijTaskNode, TasksProxy>.clear() {
      _builder.clearTasks()
    }

  }
}
@kotlin.jvm.JvmSynthetic
public inline fun com.giyeok.bibix.daemon.BibixDaemonApiProto.IntellijModuleNode.copy(block: com.giyeok.bibix.daemon.IntellijModuleNodeKt.Dsl.() -> kotlin.Unit): com.giyeok.bibix.daemon.BibixDaemonApiProto.IntellijModuleNode =
  com.giyeok.bibix.daemon.IntellijModuleNodeKt.Dsl._create(this.toBuilder()).apply { block() }._build()

