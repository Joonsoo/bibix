//Generated by the protocol buffer compiler. DO NOT EDIT!
// source: values.proto

package com.giyeok.bibix.runner;

@kotlin.jvm.JvmSynthetic
public inline fun dataClassField(block: com.giyeok.bibix.runner.DataClassFieldKt.Dsl.() -> kotlin.Unit): com.giyeok.bibix.runner.BibixValueProto.DataClassField =
  com.giyeok.bibix.runner.DataClassFieldKt.Dsl._create(com.giyeok.bibix.runner.BibixValueProto.DataClassField.newBuilder()).apply { block() }._build()
public object DataClassFieldKt {
  @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
  @com.google.protobuf.kotlin.ProtoDslMarker
  public class Dsl private constructor(
    private val _builder: com.giyeok.bibix.runner.BibixValueProto.DataClassField.Builder
  ) {
    public companion object {
      @kotlin.jvm.JvmSynthetic
      @kotlin.PublishedApi
      internal fun _create(builder: com.giyeok.bibix.runner.BibixValueProto.DataClassField.Builder): Dsl = Dsl(builder)
    }

    @kotlin.jvm.JvmSynthetic
    @kotlin.PublishedApi
    internal fun _build(): com.giyeok.bibix.runner.BibixValueProto.DataClassField = _builder.build()

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
     * <code>.com.giyeok.bibix.runner.BibixValue value = 2;</code>
     */
    public var value: com.giyeok.bibix.runner.BibixValueProto.BibixValue
      @JvmName("getValue")
      get() = _builder.getValue()
      @JvmName("setValue")
      set(value) {
        _builder.setValue(value)
      }
    /**
     * <code>.com.giyeok.bibix.runner.BibixValue value = 2;</code>
     */
    public fun clearValue() {
      _builder.clearValue()
    }
    /**
     * <code>.com.giyeok.bibix.runner.BibixValue value = 2;</code>
     * @return Whether the value field is set.
     */
    public fun hasValue(): kotlin.Boolean {
      return _builder.hasValue()
    }
  }
}
@kotlin.jvm.JvmSynthetic
public inline fun com.giyeok.bibix.runner.BibixValueProto.DataClassField.copy(block: com.giyeok.bibix.runner.DataClassFieldKt.Dsl.() -> kotlin.Unit): com.giyeok.bibix.runner.BibixValueProto.DataClassField =
  com.giyeok.bibix.runner.DataClassFieldKt.Dsl._create(this.toBuilder()).apply { block() }._build()
