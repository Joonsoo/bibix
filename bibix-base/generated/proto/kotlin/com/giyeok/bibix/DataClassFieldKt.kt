// Generated by the protocol buffer compiler. DO NOT EDIT!
// source: values.proto

// Generated files should ignore deprecation warnings
@file:Suppress("DEPRECATION")
package com.giyeok.bibix;

@kotlin.jvm.JvmName("-initializedataClassField")
public inline fun dataClassField(block: com.giyeok.bibix.DataClassFieldKt.Dsl.() -> kotlin.Unit): com.giyeok.bibix.BibixValueProto.DataClassField =
  com.giyeok.bibix.DataClassFieldKt.Dsl._create(com.giyeok.bibix.BibixValueProto.DataClassField.newBuilder()).apply { block() }._build()
/**
 * Protobuf type `com.giyeok.bibix.DataClassField`
 */
public object DataClassFieldKt {
  @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
  @com.google.protobuf.kotlin.ProtoDslMarker
  public class Dsl private constructor(
    private val _builder: com.giyeok.bibix.BibixValueProto.DataClassField.Builder
  ) {
    public companion object {
      @kotlin.jvm.JvmSynthetic
      @kotlin.PublishedApi
      internal fun _create(builder: com.giyeok.bibix.BibixValueProto.DataClassField.Builder): Dsl = Dsl(builder)
    }

    @kotlin.jvm.JvmSynthetic
    @kotlin.PublishedApi
    internal fun _build(): com.giyeok.bibix.BibixValueProto.DataClassField = _builder.build()

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
public inline fun com.giyeok.bibix.BibixValueProto.DataClassField.copy(block: com.giyeok.bibix.DataClassFieldKt.Dsl.() -> kotlin.Unit): com.giyeok.bibix.BibixValueProto.DataClassField =
  com.giyeok.bibix.DataClassFieldKt.Dsl._create(this.toBuilder()).apply { block() }._build()

public val com.giyeok.bibix.BibixValueProto.DataClassFieldOrBuilder.valueOrNull: com.giyeok.bibix.BibixValueProto.BibixValue?
  get() = if (hasValue()) getValue() else null

