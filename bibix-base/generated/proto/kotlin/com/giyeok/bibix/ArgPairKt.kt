// Generated by the protocol buffer compiler. DO NOT EDIT!
// source: ids.proto

// Generated files should ignore deprecation warnings
@file:Suppress("DEPRECATION")
package com.giyeok.bibix;

@kotlin.jvm.JvmName("-initializeargPair")
public inline fun argPair(block: com.giyeok.bibix.ArgPairKt.Dsl.() -> kotlin.Unit): com.giyeok.bibix.BibixIdProto.ArgPair =
  com.giyeok.bibix.ArgPairKt.Dsl._create(com.giyeok.bibix.BibixIdProto.ArgPair.newBuilder()).apply { block() }._build()
/**
 * Protobuf type `com.giyeok.bibix.ArgPair`
 */
public object ArgPairKt {
  @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
  @com.google.protobuf.kotlin.ProtoDslMarker
  public class Dsl private constructor(
    private val _builder: com.giyeok.bibix.BibixIdProto.ArgPair.Builder
  ) {
    public companion object {
      @kotlin.jvm.JvmSynthetic
      @kotlin.PublishedApi
      internal fun _create(builder: com.giyeok.bibix.BibixIdProto.ArgPair.Builder): Dsl = Dsl(builder)
    }

    @kotlin.jvm.JvmSynthetic
    @kotlin.PublishedApi
    internal fun _build(): com.giyeok.bibix.BibixIdProto.ArgPair = _builder.build()

    /**
     * `string name = 1;`
     */
    public var name: kotlin.String
      @JvmName("getName")
      get() = _builder.getName()
      @JvmName("setName")
      set(value) {
        _builder.setName(value)
      }
    /**
     * `string name = 1;`
     */
    public fun clearName() {
      _builder.clearName()
    }

    /**
     * `.com.giyeok.bibix.BibixValue value = 2;`
     */
    public var value: com.giyeok.bibix.BibixValueProto.BibixValue
      @JvmName("getValue")
      get() = _builder.getValue()
      @JvmName("setValue")
      set(value) {
        _builder.setValue(value)
      }
    /**
     * `.com.giyeok.bibix.BibixValue value = 2;`
     */
    public fun clearValue() {
      _builder.clearValue()
    }
    /**
     * `.com.giyeok.bibix.BibixValue value = 2;`
     * @return Whether the value field is set.
     */
    public fun hasValue(): kotlin.Boolean {
      return _builder.hasValue()
    }
  }
}
@kotlin.jvm.JvmSynthetic
@com.google.errorprone.annotations.CheckReturnValue
public inline fun com.giyeok.bibix.BibixIdProto.ArgPair.copy(block: com.giyeok.bibix.ArgPairKt.Dsl.() -> kotlin.Unit): com.giyeok.bibix.BibixIdProto.ArgPair =
  com.giyeok.bibix.ArgPairKt.Dsl._create(this.toBuilder()).apply { block() }._build()

public val com.giyeok.bibix.BibixIdProto.ArgPairOrBuilder.valueOrNull: com.giyeok.bibix.BibixValueProto.BibixValue?
  get() = if (hasValue()) getValue() else null

