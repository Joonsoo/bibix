// Generated by the protocol buffer compiler. DO NOT EDIT!
// source: api.proto

// Generated files should ignore deprecation warnings
@file:Suppress("DEPRECATION")
package com.giyeok.bibix.intellij;

@kotlin.jvm.JvmName("-initializemodule")
public inline fun module(block: com.giyeok.bibix.intellij.ModuleKt.Dsl.() -> kotlin.Unit): com.giyeok.bibix.intellij.BibixIntellijProto.Module =
  com.giyeok.bibix.intellij.ModuleKt.Dsl._create(com.giyeok.bibix.intellij.BibixIntellijProto.Module.newBuilder()).apply { block() }._build()
/**
 * Protobuf type `com.giyeok.bibix.intellij.Module`
 */
public object ModuleKt {
  @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
  @com.google.protobuf.kotlin.ProtoDslMarker
  public class Dsl private constructor(
    private val _builder: com.giyeok.bibix.intellij.BibixIntellijProto.Module.Builder
  ) {
    public companion object {
      @kotlin.jvm.JvmSynthetic
      @kotlin.PublishedApi
      internal fun _create(builder: com.giyeok.bibix.intellij.BibixIntellijProto.Module.Builder): Dsl = Dsl(builder)
    }

    @kotlin.jvm.JvmSynthetic
    @kotlin.PublishedApi
    internal fun _build(): com.giyeok.bibix.intellij.BibixIntellijProto.Module = _builder.build()

    /**
     * `string module_name = 1;`
     */
    public var moduleName: kotlin.String
      @JvmName("getModuleName")
      get() = _builder.getModuleName()
      @JvmName("setModuleName")
      set(value) {
        _builder.setModuleName(value)
      }
    /**
     * `string module_name = 1;`
     */
    public fun clearModuleName() {
      _builder.clearModuleName()
    }

    /**
     * ```
     * java, ktjvm, scala, ...
     * ```
     *
     * `string module_type = 2;`
     */
    public var moduleType: kotlin.String
      @JvmName("getModuleType")
      get() = _builder.getModuleType()
      @JvmName("setModuleType")
      set(value) {
        _builder.setModuleType(value)
      }
    /**
     * ```
     * java, ktjvm, scala, ...
     * ```
     *
     * `string module_type = 2;`
     */
    public fun clearModuleType() {
      _builder.clearModuleType()
    }

    /**
     * `string module_root_path = 3;`
     */
    public var moduleRootPath: kotlin.String
      @JvmName("getModuleRootPath")
      get() = _builder.getModuleRootPath()
      @JvmName("setModuleRootPath")
      set(value) {
        _builder.setModuleRootPath(value)
      }
    /**
     * `string module_root_path = 3;`
     */
    public fun clearModuleRootPath() {
      _builder.clearModuleRootPath()
    }

    /**
     * An uninstantiable, behaviorless type to represent the field in
     * generics.
     */
    @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
    public class ContentRootsProxy private constructor() : com.google.protobuf.kotlin.DslProxy()
    /**
     * ```
     * module sources
     * ```
     *
     * `repeated .com.giyeok.bibix.intellij.ContentRoot content_roots = 4;`
     */
     public val contentRoots: com.google.protobuf.kotlin.DslList<com.giyeok.bibix.intellij.BibixIntellijProto.ContentRoot, ContentRootsProxy>
      @kotlin.jvm.JvmSynthetic
      get() = com.google.protobuf.kotlin.DslList(
        _builder.getContentRootsList()
      )
    /**
     * ```
     * module sources
     * ```
     *
     * `repeated .com.giyeok.bibix.intellij.ContentRoot content_roots = 4;`
     * @param value The contentRoots to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("addContentRoots")
    public fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.intellij.BibixIntellijProto.ContentRoot, ContentRootsProxy>.add(value: com.giyeok.bibix.intellij.BibixIntellijProto.ContentRoot) {
      _builder.addContentRoots(value)
    }
    /**
     * ```
     * module sources
     * ```
     *
     * `repeated .com.giyeok.bibix.intellij.ContentRoot content_roots = 4;`
     * @param value The contentRoots to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("plusAssignContentRoots")
    @Suppress("NOTHING_TO_INLINE")
    public inline operator fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.intellij.BibixIntellijProto.ContentRoot, ContentRootsProxy>.plusAssign(value: com.giyeok.bibix.intellij.BibixIntellijProto.ContentRoot) {
      add(value)
    }
    /**
     * ```
     * module sources
     * ```
     *
     * `repeated .com.giyeok.bibix.intellij.ContentRoot content_roots = 4;`
     * @param values The contentRoots to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("addAllContentRoots")
    public fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.intellij.BibixIntellijProto.ContentRoot, ContentRootsProxy>.addAll(values: kotlin.collections.Iterable<com.giyeok.bibix.intellij.BibixIntellijProto.ContentRoot>) {
      _builder.addAllContentRoots(values)
    }
    /**
     * ```
     * module sources
     * ```
     *
     * `repeated .com.giyeok.bibix.intellij.ContentRoot content_roots = 4;`
     * @param values The contentRoots to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("plusAssignAllContentRoots")
    @Suppress("NOTHING_TO_INLINE")
    public inline operator fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.intellij.BibixIntellijProto.ContentRoot, ContentRootsProxy>.plusAssign(values: kotlin.collections.Iterable<com.giyeok.bibix.intellij.BibixIntellijProto.ContentRoot>) {
      addAll(values)
    }
    /**
     * ```
     * module sources
     * ```
     *
     * `repeated .com.giyeok.bibix.intellij.ContentRoot content_roots = 4;`
     * @param index The index to set the value at.
     * @param value The contentRoots to set.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("setContentRoots")
    public operator fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.intellij.BibixIntellijProto.ContentRoot, ContentRootsProxy>.set(index: kotlin.Int, value: com.giyeok.bibix.intellij.BibixIntellijProto.ContentRoot) {
      _builder.setContentRoots(index, value)
    }
    /**
     * ```
     * module sources
     * ```
     *
     * `repeated .com.giyeok.bibix.intellij.ContentRoot content_roots = 4;`
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("clearContentRoots")
    public fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.intellij.BibixIntellijProto.ContentRoot, ContentRootsProxy>.clear() {
      _builder.clearContentRoots()
    }


    /**
     * An uninstantiable, behaviorless type to represent the field in
     * generics.
     */
    @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
    public class UsingSdksProxy private constructor() : com.google.protobuf.kotlin.DslProxy()
    /**
     * `repeated .com.giyeok.bibix.intellij.SdkVersion using_sdks = 5;`
     */
     public val usingSdks: com.google.protobuf.kotlin.DslList<com.giyeok.bibix.intellij.BibixIntellijProto.SdkVersion, UsingSdksProxy>
      @kotlin.jvm.JvmSynthetic
      get() = com.google.protobuf.kotlin.DslList(
        _builder.getUsingSdksList()
      )
    /**
     * `repeated .com.giyeok.bibix.intellij.SdkVersion using_sdks = 5;`
     * @param value The usingSdks to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("addUsingSdks")
    public fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.intellij.BibixIntellijProto.SdkVersion, UsingSdksProxy>.add(value: com.giyeok.bibix.intellij.BibixIntellijProto.SdkVersion) {
      _builder.addUsingSdks(value)
    }
    /**
     * `repeated .com.giyeok.bibix.intellij.SdkVersion using_sdks = 5;`
     * @param value The usingSdks to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("plusAssignUsingSdks")
    @Suppress("NOTHING_TO_INLINE")
    public inline operator fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.intellij.BibixIntellijProto.SdkVersion, UsingSdksProxy>.plusAssign(value: com.giyeok.bibix.intellij.BibixIntellijProto.SdkVersion) {
      add(value)
    }
    /**
     * `repeated .com.giyeok.bibix.intellij.SdkVersion using_sdks = 5;`
     * @param values The usingSdks to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("addAllUsingSdks")
    public fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.intellij.BibixIntellijProto.SdkVersion, UsingSdksProxy>.addAll(values: kotlin.collections.Iterable<com.giyeok.bibix.intellij.BibixIntellijProto.SdkVersion>) {
      _builder.addAllUsingSdks(values)
    }
    /**
     * `repeated .com.giyeok.bibix.intellij.SdkVersion using_sdks = 5;`
     * @param values The usingSdks to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("plusAssignAllUsingSdks")
    @Suppress("NOTHING_TO_INLINE")
    public inline operator fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.intellij.BibixIntellijProto.SdkVersion, UsingSdksProxy>.plusAssign(values: kotlin.collections.Iterable<com.giyeok.bibix.intellij.BibixIntellijProto.SdkVersion>) {
      addAll(values)
    }
    /**
     * `repeated .com.giyeok.bibix.intellij.SdkVersion using_sdks = 5;`
     * @param index The index to set the value at.
     * @param value The usingSdks to set.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("setUsingSdks")
    public operator fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.intellij.BibixIntellijProto.SdkVersion, UsingSdksProxy>.set(index: kotlin.Int, value: com.giyeok.bibix.intellij.BibixIntellijProto.SdkVersion) {
      _builder.setUsingSdks(index, value)
    }
    /**
     * `repeated .com.giyeok.bibix.intellij.SdkVersion using_sdks = 5;`
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
    public class ModuleDepsProxy private constructor() : com.google.protobuf.kotlin.DslProxy()
    /**
     * ```
     * dependent한 module 이름 목록
     * ```
     *
     * `repeated string module_deps = 6;`
     * @return A list containing the moduleDeps.
     */
    public val moduleDeps: com.google.protobuf.kotlin.DslList<kotlin.String, ModuleDepsProxy>
      @kotlin.jvm.JvmSynthetic
      get() = com.google.protobuf.kotlin.DslList(
        _builder.getModuleDepsList()
      )
    /**
     * ```
     * dependent한 module 이름 목록
     * ```
     *
     * `repeated string module_deps = 6;`
     * @param value The moduleDeps to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("addModuleDeps")
    public fun com.google.protobuf.kotlin.DslList<kotlin.String, ModuleDepsProxy>.add(value: kotlin.String) {
      _builder.addModuleDeps(value)
    }
    /**
     * ```
     * dependent한 module 이름 목록
     * ```
     *
     * `repeated string module_deps = 6;`
     * @param value The moduleDeps to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("plusAssignModuleDeps")
    @Suppress("NOTHING_TO_INLINE")
    public inline operator fun com.google.protobuf.kotlin.DslList<kotlin.String, ModuleDepsProxy>.plusAssign(value: kotlin.String) {
      add(value)
    }
    /**
     * ```
     * dependent한 module 이름 목록
     * ```
     *
     * `repeated string module_deps = 6;`
     * @param values The moduleDeps to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("addAllModuleDeps")
    public fun com.google.protobuf.kotlin.DslList<kotlin.String, ModuleDepsProxy>.addAll(values: kotlin.collections.Iterable<kotlin.String>) {
      _builder.addAllModuleDeps(values)
    }
    /**
     * ```
     * dependent한 module 이름 목록
     * ```
     *
     * `repeated string module_deps = 6;`
     * @param values The moduleDeps to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("plusAssignAllModuleDeps")
    @Suppress("NOTHING_TO_INLINE")
    public inline operator fun com.google.protobuf.kotlin.DslList<kotlin.String, ModuleDepsProxy>.plusAssign(values: kotlin.collections.Iterable<kotlin.String>) {
      addAll(values)
    }
    /**
     * ```
     * dependent한 module 이름 목록
     * ```
     *
     * `repeated string module_deps = 6;`
     * @param index The index to set the value at.
     * @param value The moduleDeps to set.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("setModuleDeps")
    public operator fun com.google.protobuf.kotlin.DslList<kotlin.String, ModuleDepsProxy>.set(index: kotlin.Int, value: kotlin.String) {
      _builder.setModuleDeps(index, value)
    }/**
     * ```
     * dependent한 module 이름 목록
     * ```
     *
     * `repeated string module_deps = 6;`
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("clearModuleDeps")
    public fun com.google.protobuf.kotlin.DslList<kotlin.String, ModuleDepsProxy>.clear() {
      _builder.clearModuleDeps()
    }
    /**
     * An uninstantiable, behaviorless type to represent the field in
     * generics.
     */
    @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
    public class LibraryDepsProxy private constructor() : com.google.protobuf.kotlin.DslProxy()
    /**
     * ```
     * dependent한 라이브러리 id 목록
     * ```
     *
     * `repeated string library_deps = 7;`
     * @return A list containing the libraryDeps.
     */
    public val libraryDeps: com.google.protobuf.kotlin.DslList<kotlin.String, LibraryDepsProxy>
      @kotlin.jvm.JvmSynthetic
      get() = com.google.protobuf.kotlin.DslList(
        _builder.getLibraryDepsList()
      )
    /**
     * ```
     * dependent한 라이브러리 id 목록
     * ```
     *
     * `repeated string library_deps = 7;`
     * @param value The libraryDeps to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("addLibraryDeps")
    public fun com.google.protobuf.kotlin.DslList<kotlin.String, LibraryDepsProxy>.add(value: kotlin.String) {
      _builder.addLibraryDeps(value)
    }
    /**
     * ```
     * dependent한 라이브러리 id 목록
     * ```
     *
     * `repeated string library_deps = 7;`
     * @param value The libraryDeps to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("plusAssignLibraryDeps")
    @Suppress("NOTHING_TO_INLINE")
    public inline operator fun com.google.protobuf.kotlin.DslList<kotlin.String, LibraryDepsProxy>.plusAssign(value: kotlin.String) {
      add(value)
    }
    /**
     * ```
     * dependent한 라이브러리 id 목록
     * ```
     *
     * `repeated string library_deps = 7;`
     * @param values The libraryDeps to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("addAllLibraryDeps")
    public fun com.google.protobuf.kotlin.DslList<kotlin.String, LibraryDepsProxy>.addAll(values: kotlin.collections.Iterable<kotlin.String>) {
      _builder.addAllLibraryDeps(values)
    }
    /**
     * ```
     * dependent한 라이브러리 id 목록
     * ```
     *
     * `repeated string library_deps = 7;`
     * @param values The libraryDeps to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("plusAssignAllLibraryDeps")
    @Suppress("NOTHING_TO_INLINE")
    public inline operator fun com.google.protobuf.kotlin.DslList<kotlin.String, LibraryDepsProxy>.plusAssign(values: kotlin.collections.Iterable<kotlin.String>) {
      addAll(values)
    }
    /**
     * ```
     * dependent한 라이브러리 id 목록
     * ```
     *
     * `repeated string library_deps = 7;`
     * @param index The index to set the value at.
     * @param value The libraryDeps to set.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("setLibraryDeps")
    public operator fun com.google.protobuf.kotlin.DslList<kotlin.String, LibraryDepsProxy>.set(index: kotlin.Int, value: kotlin.String) {
      _builder.setLibraryDeps(index, value)
    }/**
     * ```
     * dependent한 라이브러리 id 목록
     * ```
     *
     * `repeated string library_deps = 7;`
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("clearLibraryDeps")
    public fun com.google.protobuf.kotlin.DslList<kotlin.String, LibraryDepsProxy>.clear() {
      _builder.clearLibraryDeps()
    }}
}
@kotlin.jvm.JvmSynthetic
@com.google.errorprone.annotations.CheckReturnValue
public inline fun com.giyeok.bibix.intellij.BibixIntellijProto.Module.copy(block: com.giyeok.bibix.intellij.ModuleKt.Dsl.() -> kotlin.Unit): com.giyeok.bibix.intellij.BibixIntellijProto.Module =
  com.giyeok.bibix.intellij.ModuleKt.Dsl._create(this.toBuilder()).apply { block() }._build()

