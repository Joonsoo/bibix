//Generated by the protocol buffer compiler. DO NOT EDIT!
// source: values.proto

package com.giyeok.bibix.runner;

@kotlin.jvm.JvmSynthetic
public inline fun classInstanceValue(block: com.giyeok.bibix.runner.ClassInstanceValueKt.Dsl.() -> kotlin.Unit): com.giyeok.bibix.runner.BibixValueProto.ClassInstanceValue =
  com.giyeok.bibix.runner.ClassInstanceValueKt.Dsl._create(com.giyeok.bibix.runner.BibixValueProto.ClassInstanceValue.newBuilder()).apply { block() }._build()
public object ClassInstanceValueKt {
  @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
  @com.google.protobuf.kotlin.ProtoDslMarker
  public class Dsl private constructor(
    private val _builder: com.giyeok.bibix.runner.BibixValueProto.ClassInstanceValue.Builder
  ) {
    public companion object {
      @kotlin.jvm.JvmSynthetic
      @kotlin.PublishedApi
      internal fun _create(builder: com.giyeok.bibix.runner.BibixValueProto.ClassInstanceValue.Builder): Dsl = Dsl(builder)
    }

    @kotlin.jvm.JvmSynthetic
    @kotlin.PublishedApi
    internal fun _build(): com.giyeok.bibix.runner.BibixValueProto.ClassInstanceValue = _builder.build()

    /**
     * <code>string class_cname = 1;</code>
     */
    public var classCname: kotlin.String
      @JvmName("getClassCname")
      get() = _builder.getClassCname()
      @JvmName("setClassCname")
      set(value) {
        _builder.setClassCname(value)
      }
    /**
     * <code>string class_cname = 1;</code>
     */
    public fun clearClassCname() {
      _builder.clearClassCname()
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
public inline fun com.giyeok.bibix.runner.BibixValueProto.ClassInstanceValue.copy(block: com.giyeok.bibix.runner.ClassInstanceValueKt.Dsl.() -> kotlin.Unit): com.giyeok.bibix.runner.BibixValueProto.ClassInstanceValue =
  com.giyeok.bibix.runner.ClassInstanceValueKt.Dsl._create(this.toBuilder()).apply { block() }._build()
