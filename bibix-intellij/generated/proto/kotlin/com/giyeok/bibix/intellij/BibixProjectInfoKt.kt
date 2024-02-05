// Generated by the protocol buffer compiler. DO NOT EDIT!
// source: api.proto

// Generated files should ignore deprecation warnings
@file:Suppress("DEPRECATION")
package com.giyeok.bibix.intellij;

@kotlin.jvm.JvmName("-initializebibixProjectInfo")
public inline fun bibixProjectInfo(block: com.giyeok.bibix.intellij.BibixProjectInfoKt.Dsl.() -> kotlin.Unit): com.giyeok.bibix.intellij.BibixIntellijProto.BibixProjectInfo =
  com.giyeok.bibix.intellij.BibixProjectInfoKt.Dsl._create(com.giyeok.bibix.intellij.BibixIntellijProto.BibixProjectInfo.newBuilder()).apply { block() }._build()
/**
 * Protobuf type `com.giyeok.bibix.intellij.BibixProjectInfo`
 */
public object BibixProjectInfoKt {
  @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
  @com.google.protobuf.kotlin.ProtoDslMarker
  public class Dsl private constructor(
    private val _builder: com.giyeok.bibix.intellij.BibixIntellijProto.BibixProjectInfo.Builder
  ) {
    public companion object {
      @kotlin.jvm.JvmSynthetic
      @kotlin.PublishedApi
      internal fun _create(builder: com.giyeok.bibix.intellij.BibixIntellijProto.BibixProjectInfo.Builder): Dsl = Dsl(builder)
    }

    @kotlin.jvm.JvmSynthetic
    @kotlin.PublishedApi
    internal fun _build(): com.giyeok.bibix.intellij.BibixIntellijProto.BibixProjectInfo = _builder.build()

    /**
     * `string project_id = 1;`
     */
    public var projectId: kotlin.String
      @JvmName("getProjectId")
      get() = _builder.getProjectId()
      @JvmName("setProjectId")
      set(value) {
        _builder.setProjectId(value)
      }
    /**
     * `string project_id = 1;`
     */
    public fun clearProjectId() {
      _builder.clearProjectId()
    }

    /**
     * `string project_name = 2;`
     */
    public var projectName: kotlin.String
      @JvmName("getProjectName")
      get() = _builder.getProjectName()
      @JvmName("setProjectName")
      set(value) {
        _builder.setProjectName(value)
      }
    /**
     * `string project_name = 2;`
     */
    public fun clearProjectName() {
      _builder.clearProjectName()
    }

    /**
     * An uninstantiable, behaviorless type to represent the field in
     * generics.
     */
    @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
    public class UsingSdksProxy private constructor() : com.google.protobuf.kotlin.DslProxy()
    /**
     * `repeated .com.giyeok.bibix.intellij.SdkVersion using_sdks = 3;`
     */
     public val usingSdks: com.google.protobuf.kotlin.DslList<com.giyeok.bibix.intellij.BibixIntellijProto.SdkVersion, UsingSdksProxy>
      @kotlin.jvm.JvmSynthetic
      get() = com.google.protobuf.kotlin.DslList(
        _builder.getUsingSdksList()
      )
    /**
     * `repeated .com.giyeok.bibix.intellij.SdkVersion using_sdks = 3;`
     * @param value The usingSdks to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("addUsingSdks")
    public fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.intellij.BibixIntellijProto.SdkVersion, UsingSdksProxy>.add(value: com.giyeok.bibix.intellij.BibixIntellijProto.SdkVersion) {
      _builder.addUsingSdks(value)
    }
    /**
     * `repeated .com.giyeok.bibix.intellij.SdkVersion using_sdks = 3;`
     * @param value The usingSdks to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("plusAssignUsingSdks")
    @Suppress("NOTHING_TO_INLINE")
    public inline operator fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.intellij.BibixIntellijProto.SdkVersion, UsingSdksProxy>.plusAssign(value: com.giyeok.bibix.intellij.BibixIntellijProto.SdkVersion) {
      add(value)
    }
    /**
     * `repeated .com.giyeok.bibix.intellij.SdkVersion using_sdks = 3;`
     * @param values The usingSdks to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("addAllUsingSdks")
    public fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.intellij.BibixIntellijProto.SdkVersion, UsingSdksProxy>.addAll(values: kotlin.collections.Iterable<com.giyeok.bibix.intellij.BibixIntellijProto.SdkVersion>) {
      _builder.addAllUsingSdks(values)
    }
    /**
     * `repeated .com.giyeok.bibix.intellij.SdkVersion using_sdks = 3;`
     * @param values The usingSdks to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("plusAssignAllUsingSdks")
    @Suppress("NOTHING_TO_INLINE")
    public inline operator fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.intellij.BibixIntellijProto.SdkVersion, UsingSdksProxy>.plusAssign(values: kotlin.collections.Iterable<com.giyeok.bibix.intellij.BibixIntellijProto.SdkVersion>) {
      addAll(values)
    }
    /**
     * `repeated .com.giyeok.bibix.intellij.SdkVersion using_sdks = 3;`
     * @param index The index to set the value at.
     * @param value The usingSdks to set.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("setUsingSdks")
    public operator fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.intellij.BibixIntellijProto.SdkVersion, UsingSdksProxy>.set(index: kotlin.Int, value: com.giyeok.bibix.intellij.BibixIntellijProto.SdkVersion) {
      _builder.setUsingSdks(index, value)
    }
    /**
     * `repeated .com.giyeok.bibix.intellij.SdkVersion using_sdks = 3;`
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("clearUsingSdks")
    public fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.intellij.BibixIntellijProto.SdkVersion, UsingSdksProxy>.clear() {
      _builder.clearUsingSdks()
    }


    /**
     * An uninstantiable, behaviorless type to represent the field in
     * generics.
     */
    @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
    public class ModulesProxy private constructor() : com.google.protobuf.kotlin.DslProxy()
    /**
     * `repeated .com.giyeok.bibix.intellij.Module modules = 4;`
     */
     public val modules: com.google.protobuf.kotlin.DslList<com.giyeok.bibix.intellij.BibixIntellijProto.Module, ModulesProxy>
      @kotlin.jvm.JvmSynthetic
      get() = com.google.protobuf.kotlin.DslList(
        _builder.getModulesList()
      )
    /**
     * `repeated .com.giyeok.bibix.intellij.Module modules = 4;`
     * @param value The modules to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("addModules")
    public fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.intellij.BibixIntellijProto.Module, ModulesProxy>.add(value: com.giyeok.bibix.intellij.BibixIntellijProto.Module) {
      _builder.addModules(value)
    }
    /**
     * `repeated .com.giyeok.bibix.intellij.Module modules = 4;`
     * @param value The modules to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("plusAssignModules")
    @Suppress("NOTHING_TO_INLINE")
    public inline operator fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.intellij.BibixIntellijProto.Module, ModulesProxy>.plusAssign(value: com.giyeok.bibix.intellij.BibixIntellijProto.Module) {
      add(value)
    }
    /**
     * `repeated .com.giyeok.bibix.intellij.Module modules = 4;`
     * @param values The modules to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("addAllModules")
    public fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.intellij.BibixIntellijProto.Module, ModulesProxy>.addAll(values: kotlin.collections.Iterable<com.giyeok.bibix.intellij.BibixIntellijProto.Module>) {
      _builder.addAllModules(values)
    }
    /**
     * `repeated .com.giyeok.bibix.intellij.Module modules = 4;`
     * @param values The modules to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("plusAssignAllModules")
    @Suppress("NOTHING_TO_INLINE")
    public inline operator fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.intellij.BibixIntellijProto.Module, ModulesProxy>.plusAssign(values: kotlin.collections.Iterable<com.giyeok.bibix.intellij.BibixIntellijProto.Module>) {
      addAll(values)
    }
    /**
     * `repeated .com.giyeok.bibix.intellij.Module modules = 4;`
     * @param index The index to set the value at.
     * @param value The modules to set.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("setModules")
    public operator fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.intellij.BibixIntellijProto.Module, ModulesProxy>.set(index: kotlin.Int, value: com.giyeok.bibix.intellij.BibixIntellijProto.Module) {
      _builder.setModules(index, value)
    }
    /**
     * `repeated .com.giyeok.bibix.intellij.Module modules = 4;`
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("clearModules")
    public fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.intellij.BibixIntellijProto.Module, ModulesProxy>.clear() {
      _builder.clearModules()
    }


    /**
     * An uninstantiable, behaviorless type to represent the field in
     * generics.
     */
    @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
    public class ExternalLibrariesProxy private constructor() : com.google.protobuf.kotlin.DslProxy()
    /**
     * `repeated .com.giyeok.bibix.intellij.ExternalLibrary external_libraries = 5;`
     */
     public val externalLibraries: com.google.protobuf.kotlin.DslList<com.giyeok.bibix.intellij.BibixIntellijProto.ExternalLibrary, ExternalLibrariesProxy>
      @kotlin.jvm.JvmSynthetic
      get() = com.google.protobuf.kotlin.DslList(
        _builder.getExternalLibrariesList()
      )
    /**
     * `repeated .com.giyeok.bibix.intellij.ExternalLibrary external_libraries = 5;`
     * @param value The externalLibraries to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("addExternalLibraries")
    public fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.intellij.BibixIntellijProto.ExternalLibrary, ExternalLibrariesProxy>.add(value: com.giyeok.bibix.intellij.BibixIntellijProto.ExternalLibrary) {
      _builder.addExternalLibraries(value)
    }
    /**
     * `repeated .com.giyeok.bibix.intellij.ExternalLibrary external_libraries = 5;`
     * @param value The externalLibraries to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("plusAssignExternalLibraries")
    @Suppress("NOTHING_TO_INLINE")
    public inline operator fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.intellij.BibixIntellijProto.ExternalLibrary, ExternalLibrariesProxy>.plusAssign(value: com.giyeok.bibix.intellij.BibixIntellijProto.ExternalLibrary) {
      add(value)
    }
    /**
     * `repeated .com.giyeok.bibix.intellij.ExternalLibrary external_libraries = 5;`
     * @param values The externalLibraries to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("addAllExternalLibraries")
    public fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.intellij.BibixIntellijProto.ExternalLibrary, ExternalLibrariesProxy>.addAll(values: kotlin.collections.Iterable<com.giyeok.bibix.intellij.BibixIntellijProto.ExternalLibrary>) {
      _builder.addAllExternalLibraries(values)
    }
    /**
     * `repeated .com.giyeok.bibix.intellij.ExternalLibrary external_libraries = 5;`
     * @param values The externalLibraries to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("plusAssignAllExternalLibraries")
    @Suppress("NOTHING_TO_INLINE")
    public inline operator fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.intellij.BibixIntellijProto.ExternalLibrary, ExternalLibrariesProxy>.plusAssign(values: kotlin.collections.Iterable<com.giyeok.bibix.intellij.BibixIntellijProto.ExternalLibrary>) {
      addAll(values)
    }
    /**
     * `repeated .com.giyeok.bibix.intellij.ExternalLibrary external_libraries = 5;`
     * @param index The index to set the value at.
     * @param value The externalLibraries to set.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("setExternalLibraries")
    public operator fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.intellij.BibixIntellijProto.ExternalLibrary, ExternalLibrariesProxy>.set(index: kotlin.Int, value: com.giyeok.bibix.intellij.BibixIntellijProto.ExternalLibrary) {
      _builder.setExternalLibraries(index, value)
    }
    /**
     * `repeated .com.giyeok.bibix.intellij.ExternalLibrary external_libraries = 5;`
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("clearExternalLibraries")
    public fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.intellij.BibixIntellijProto.ExternalLibrary, ExternalLibrariesProxy>.clear() {
      _builder.clearExternalLibraries()
    }


    /**
     * `.com.giyeok.bibix.intellij.SdkInfo sdk_info = 6;`
     */
    public var sdkInfo: com.giyeok.bibix.intellij.BibixIntellijProto.SdkInfo
      @JvmName("getSdkInfo")
      get() = _builder.getSdkInfo()
      @JvmName("setSdkInfo")
      set(value) {
        _builder.setSdkInfo(value)
      }
    /**
     * `.com.giyeok.bibix.intellij.SdkInfo sdk_info = 6;`
     */
    public fun clearSdkInfo() {
      _builder.clearSdkInfo()
    }
    /**
     * `.com.giyeok.bibix.intellij.SdkInfo sdk_info = 6;`
     * @return Whether the sdkInfo field is set.
     */
    public fun hasSdkInfo(): kotlin.Boolean {
      return _builder.hasSdkInfo()
    }

    /**
     * An uninstantiable, behaviorless type to represent the field in
     * generics.
     */
    @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
    public class ActionsProxy private constructor() : com.google.protobuf.kotlin.DslProxy()
    /**
     * `repeated .com.giyeok.bibix.intellij.Action actions = 7;`
     */
     public val actions: com.google.protobuf.kotlin.DslList<com.giyeok.bibix.intellij.BibixIntellijProto.Action, ActionsProxy>
      @kotlin.jvm.JvmSynthetic
      get() = com.google.protobuf.kotlin.DslList(
        _builder.getActionsList()
      )
    /**
     * `repeated .com.giyeok.bibix.intellij.Action actions = 7;`
     * @param value The actions to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("addActions")
    public fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.intellij.BibixIntellijProto.Action, ActionsProxy>.add(value: com.giyeok.bibix.intellij.BibixIntellijProto.Action) {
      _builder.addActions(value)
    }
    /**
     * `repeated .com.giyeok.bibix.intellij.Action actions = 7;`
     * @param value The actions to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("plusAssignActions")
    @Suppress("NOTHING_TO_INLINE")
    public inline operator fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.intellij.BibixIntellijProto.Action, ActionsProxy>.plusAssign(value: com.giyeok.bibix.intellij.BibixIntellijProto.Action) {
      add(value)
    }
    /**
     * `repeated .com.giyeok.bibix.intellij.Action actions = 7;`
     * @param values The actions to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("addAllActions")
    public fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.intellij.BibixIntellijProto.Action, ActionsProxy>.addAll(values: kotlin.collections.Iterable<com.giyeok.bibix.intellij.BibixIntellijProto.Action>) {
      _builder.addAllActions(values)
    }
    /**
     * `repeated .com.giyeok.bibix.intellij.Action actions = 7;`
     * @param values The actions to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("plusAssignAllActions")
    @Suppress("NOTHING_TO_INLINE")
    public inline operator fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.intellij.BibixIntellijProto.Action, ActionsProxy>.plusAssign(values: kotlin.collections.Iterable<com.giyeok.bibix.intellij.BibixIntellijProto.Action>) {
      addAll(values)
    }
    /**
     * `repeated .com.giyeok.bibix.intellij.Action actions = 7;`
     * @param index The index to set the value at.
     * @param value The actions to set.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("setActions")
    public operator fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.intellij.BibixIntellijProto.Action, ActionsProxy>.set(index: kotlin.Int, value: com.giyeok.bibix.intellij.BibixIntellijProto.Action) {
      _builder.setActions(index, value)
    }
    /**
     * `repeated .com.giyeok.bibix.intellij.Action actions = 7;`
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("clearActions")
    public fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.intellij.BibixIntellijProto.Action, ActionsProxy>.clear() {
      _builder.clearActions()
    }

  }
}
@kotlin.jvm.JvmSynthetic
@com.google.errorprone.annotations.CheckReturnValue
public inline fun com.giyeok.bibix.intellij.BibixIntellijProto.BibixProjectInfo.copy(block: com.giyeok.bibix.intellij.BibixProjectInfoKt.Dsl.() -> kotlin.Unit): com.giyeok.bibix.intellij.BibixIntellijProto.BibixProjectInfo =
  com.giyeok.bibix.intellij.BibixProjectInfoKt.Dsl._create(this.toBuilder()).apply { block() }._build()

public val com.giyeok.bibix.intellij.BibixIntellijProto.BibixProjectInfoOrBuilder.sdkInfoOrNull: com.giyeok.bibix.intellij.BibixIntellijProto.SdkInfo?
  get() = if (hasSdkInfo()) getSdkInfo() else null
