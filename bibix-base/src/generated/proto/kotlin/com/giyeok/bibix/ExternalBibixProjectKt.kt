//Generated by the protocol buffer compiler. DO NOT EDIT!
// source: ids.proto

package com.giyeok.bibix;

@kotlin.jvm.JvmSynthetic
public inline fun externalBibixProject(block: com.giyeok.bibix.ExternalBibixProjectKt.Dsl.() -> kotlin.Unit): com.giyeok.bibix.BibixIdProto.ExternalBibixProject =
  com.giyeok.bibix.ExternalBibixProjectKt.Dsl._create(com.giyeok.bibix.BibixIdProto.ExternalBibixProject.newBuilder()).apply { block() }._build()
public object ExternalBibixProjectKt {
  @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
  @com.google.protobuf.kotlin.ProtoDslMarker
  public class Dsl private constructor(
    private val _builder: com.giyeok.bibix.BibixIdProto.ExternalBibixProject.Builder
  ) {
    public companion object {
      @kotlin.jvm.JvmSynthetic
      @kotlin.PublishedApi
      internal fun _create(builder: com.giyeok.bibix.BibixIdProto.ExternalBibixProject.Builder): Dsl = Dsl(builder)
    }

    @kotlin.jvm.JvmSynthetic
    @kotlin.PublishedApi
    internal fun _build(): com.giyeok.bibix.BibixIdProto.ExternalBibixProject = _builder.build()

    /**
     * <code>string root_directory = 1;</code>
     */
    public var rootDirectory: kotlin.String
      @JvmName("getRootDirectory")
      get() = _builder.getRootDirectory()
      @JvmName("setRootDirectory")
      set(value) {
        _builder.setRootDirectory(value)
      }
    /**
     * <code>string root_directory = 1;</code>
     */
    public fun clearRootDirectory() {
      _builder.clearRootDirectory()
    }

    /**
     * <code>string script_name = 2;</code>
     */
    public var scriptName: kotlin.String
      @JvmName("getScriptName")
      get() = _builder.getScriptName()
      @JvmName("setScriptName")
      set(value) {
        _builder.setScriptName(value)
      }
    /**
     * <code>string script_name = 2;</code>
     */
    public fun clearScriptName() {
      _builder.clearScriptName()
    }

    /**
     * <code>bytes project_obj_hash = 3;</code>
     */
    public var projectObjHash: com.google.protobuf.ByteString
      @JvmName("getProjectObjHash")
      get() = _builder.getProjectObjHash()
      @JvmName("setProjectObjHash")
      set(value) {
        _builder.setProjectObjHash(value)
      }
    /**
     * <code>bytes project_obj_hash = 3;</code>
     */
    public fun clearProjectObjHash() {
      _builder.clearProjectObjHash()
    }

    /**
     * <code>int64 version = 4;</code>
     */
    public var version: kotlin.Long
      @JvmName("getVersion")
      get() = _builder.getVersion()
      @JvmName("setVersion")
      set(value) {
        _builder.setVersion(value)
      }
    /**
     * <code>int64 version = 4;</code>
     */
    public fun clearVersion() {
      _builder.clearVersion()
    }
  }
}
@kotlin.jvm.JvmSynthetic
public inline fun com.giyeok.bibix.BibixIdProto.ExternalBibixProject.copy(block: com.giyeok.bibix.ExternalBibixProjectKt.Dsl.() -> kotlin.Unit): com.giyeok.bibix.BibixIdProto.ExternalBibixProject =
  com.giyeok.bibix.ExternalBibixProjectKt.Dsl._create(this.toBuilder()).apply { block() }._build()
