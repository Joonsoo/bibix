//Generated by the protocol buffer compiler. DO NOT EDIT!
// source: ids.proto

package com.giyeok.bibix.runner;

@kotlin.jvm.JvmSynthetic
public inline fun objectIdHash(block: com.giyeok.bibix.runner.ObjectIdHashKt.Dsl.() -> kotlin.Unit): com.giyeok.bibix.runner.BibixIdProto.ObjectIdHash =
  com.giyeok.bibix.runner.ObjectIdHashKt.Dsl._create(com.giyeok.bibix.runner.BibixIdProto.ObjectIdHash.newBuilder()).apply { block() }._build()
public object ObjectIdHashKt {
  @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
  @com.google.protobuf.kotlin.ProtoDslMarker
  public class Dsl private constructor(
    private val _builder: com.giyeok.bibix.runner.BibixIdProto.ObjectIdHash.Builder
  ) {
    public companion object {
      @kotlin.jvm.JvmSynthetic
      @kotlin.PublishedApi
      internal fun _create(builder: com.giyeok.bibix.runner.BibixIdProto.ObjectIdHash.Builder): Dsl = Dsl(builder)
    }

    @kotlin.jvm.JvmSynthetic
    @kotlin.PublishedApi
    internal fun _build(): com.giyeok.bibix.runner.BibixIdProto.ObjectIdHash = _builder.build()

    /**
     * <code>string root_source = 1;</code>
     */
    public var rootSource: kotlin.String
      @JvmName("getRootSource")
      get() = _builder.getRootSource()
      @JvmName("setRootSource")
      set(value) {
        _builder.setRootSource(value)
      }
    /**
     * <code>string root_source = 1;</code>
     */
    public fun clearRootSource() {
      _builder.clearRootSource()
    }
    /**
     * <code>string root_source = 1;</code>
     * @return Whether the rootSource field is set.
     */
    public fun hasRootSource(): kotlin.Boolean {
      return _builder.hasRootSource()
    }

    /**
     * <code>string bibix_internal_source = 2;</code>
     */
    public var bibixInternalSource: kotlin.String
      @JvmName("getBibixInternalSource")
      get() = _builder.getBibixInternalSource()
      @JvmName("setBibixInternalSource")
      set(value) {
        _builder.setBibixInternalSource(value)
      }
    /**
     * <code>string bibix_internal_source = 2;</code>
     */
    public fun clearBibixInternalSource() {
      _builder.clearBibixInternalSource()
    }
    /**
     * <code>string bibix_internal_source = 2;</code>
     * @return Whether the bibixInternalSource field is set.
     */
    public fun hasBibixInternalSource(): kotlin.Boolean {
      return _builder.hasBibixInternalSource()
    }

    /**
     * <code>bytes object_id_hash_string = 3;</code>
     */
    public var objectIdHashString: com.google.protobuf.ByteString
      @JvmName("getObjectIdHashString")
      get() = _builder.getObjectIdHashString()
      @JvmName("setObjectIdHashString")
      set(value) {
        _builder.setObjectIdHashString(value)
      }
    /**
     * <code>bytes object_id_hash_string = 3;</code>
     */
    public fun clearObjectIdHashString() {
      _builder.clearObjectIdHashString()
    }
    /**
     * <code>bytes object_id_hash_string = 3;</code>
     * @return Whether the objectIdHashString field is set.
     */
    public fun hasObjectIdHashString(): kotlin.Boolean {
      return _builder.hasObjectIdHashString()
    }
    public val hashCase: com.giyeok.bibix.runner.BibixIdProto.ObjectIdHash.HashCase
      @JvmName("getHashCase")
      get() = _builder.getHashCase()

    public fun clearHash() {
      _builder.clearHash()
    }
  }
}
@kotlin.jvm.JvmSynthetic
public inline fun com.giyeok.bibix.runner.BibixIdProto.ObjectIdHash.copy(block: com.giyeok.bibix.runner.ObjectIdHashKt.Dsl.() -> kotlin.Unit): com.giyeok.bibix.runner.BibixIdProto.ObjectIdHash =
  com.giyeok.bibix.runner.ObjectIdHashKt.Dsl._create(this.toBuilder()).apply { block() }._build()
