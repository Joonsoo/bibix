//Generated by the protocol buffer compiler. DO NOT EDIT!
// source: values.proto

package com.giyeok.bibix.runner;

@kotlin.jvm.JvmName("-initializebibixValue")
public inline fun bibixValue(block: com.giyeok.bibix.runner.BibixValueKt.Dsl.() -> kotlin.Unit): com.giyeok.bibix.runner.BibixValueProto.BibixValue =
  com.giyeok.bibix.runner.BibixValueKt.Dsl._create(com.giyeok.bibix.runner.BibixValueProto.BibixValue.newBuilder()).apply { block() }._build()
public object BibixValueKt {
  @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
  @com.google.protobuf.kotlin.ProtoDslMarker
  public class Dsl private constructor(
    private val _builder: com.giyeok.bibix.runner.BibixValueProto.BibixValue.Builder
  ) {
    public companion object {
      @kotlin.jvm.JvmSynthetic
      @kotlin.PublishedApi
      internal fun _create(builder: com.giyeok.bibix.runner.BibixValueProto.BibixValue.Builder): Dsl = Dsl(builder)
    }

    @kotlin.jvm.JvmSynthetic
    @kotlin.PublishedApi
    internal fun _build(): com.giyeok.bibix.runner.BibixValueProto.BibixValue = _builder.build()

    /**
     * <code>bool boolean_value = 1;</code>
     */
    public var booleanValue: kotlin.Boolean
      @JvmName("getBooleanValue")
      get() = _builder.getBooleanValue()
      @JvmName("setBooleanValue")
      set(value) {
        _builder.setBooleanValue(value)
      }
    /**
     * <code>bool boolean_value = 1;</code>
     */
    public fun clearBooleanValue() {
      _builder.clearBooleanValue()
    }
    /**
     * <code>bool boolean_value = 1;</code>
     * @return Whether the booleanValue field is set.
     */
    public fun hasBooleanValue(): kotlin.Boolean {
      return _builder.hasBooleanValue()
    }

    /**
     * <code>string string_value = 2;</code>
     */
    public var stringValue: kotlin.String
      @JvmName("getStringValue")
      get() = _builder.getStringValue()
      @JvmName("setStringValue")
      set(value) {
        _builder.setStringValue(value)
      }
    /**
     * <code>string string_value = 2;</code>
     */
    public fun clearStringValue() {
      _builder.clearStringValue()
    }
    /**
     * <code>string string_value = 2;</code>
     * @return Whether the stringValue field is set.
     */
    public fun hasStringValue(): kotlin.Boolean {
      return _builder.hasStringValue()
    }

    /**
     * <code>string path_value = 3;</code>
     */
    public var pathValue: kotlin.String
      @JvmName("getPathValue")
      get() = _builder.getPathValue()
      @JvmName("setPathValue")
      set(value) {
        _builder.setPathValue(value)
      }
    /**
     * <code>string path_value = 3;</code>
     */
    public fun clearPathValue() {
      _builder.clearPathValue()
    }
    /**
     * <code>string path_value = 3;</code>
     * @return Whether the pathValue field is set.
     */
    public fun hasPathValue(): kotlin.Boolean {
      return _builder.hasPathValue()
    }

    /**
     * <code>string file_value = 4;</code>
     */
    public var fileValue: kotlin.String
      @JvmName("getFileValue")
      get() = _builder.getFileValue()
      @JvmName("setFileValue")
      set(value) {
        _builder.setFileValue(value)
      }
    /**
     * <code>string file_value = 4;</code>
     */
    public fun clearFileValue() {
      _builder.clearFileValue()
    }
    /**
     * <code>string file_value = 4;</code>
     * @return Whether the fileValue field is set.
     */
    public fun hasFileValue(): kotlin.Boolean {
      return _builder.hasFileValue()
    }

    /**
     * <code>string directory_value = 5;</code>
     */
    public var directoryValue: kotlin.String
      @JvmName("getDirectoryValue")
      get() = _builder.getDirectoryValue()
      @JvmName("setDirectoryValue")
      set(value) {
        _builder.setDirectoryValue(value)
      }
    /**
     * <code>string directory_value = 5;</code>
     */
    public fun clearDirectoryValue() {
      _builder.clearDirectoryValue()
    }
    /**
     * <code>string directory_value = 5;</code>
     * @return Whether the directoryValue field is set.
     */
    public fun hasDirectoryValue(): kotlin.Boolean {
      return _builder.hasDirectoryValue()
    }

    /**
     * <code>.com.giyeok.bibix.runner.EnumValue enum_value = 6;</code>
     */
    public var enumValue: com.giyeok.bibix.runner.BibixValueProto.EnumValue
      @JvmName("getEnumValue")
      get() = _builder.getEnumValue()
      @JvmName("setEnumValue")
      set(value) {
        _builder.setEnumValue(value)
      }
    /**
     * <code>.com.giyeok.bibix.runner.EnumValue enum_value = 6;</code>
     */
    public fun clearEnumValue() {
      _builder.clearEnumValue()
    }
    /**
     * <code>.com.giyeok.bibix.runner.EnumValue enum_value = 6;</code>
     * @return Whether the enumValue field is set.
     */
    public fun hasEnumValue(): kotlin.Boolean {
      return _builder.hasEnumValue()
    }

    /**
     * <code>.com.giyeok.bibix.runner.ListValue list_value = 7;</code>
     */
    public var listValue: com.giyeok.bibix.runner.BibixValueProto.ListValue
      @JvmName("getListValue")
      get() = _builder.getListValue()
      @JvmName("setListValue")
      set(value) {
        _builder.setListValue(value)
      }
    /**
     * <code>.com.giyeok.bibix.runner.ListValue list_value = 7;</code>
     */
    public fun clearListValue() {
      _builder.clearListValue()
    }
    /**
     * <code>.com.giyeok.bibix.runner.ListValue list_value = 7;</code>
     * @return Whether the listValue field is set.
     */
    public fun hasListValue(): kotlin.Boolean {
      return _builder.hasListValue()
    }

    /**
     * <code>.com.giyeok.bibix.runner.SetValue set_value = 8;</code>
     */
    public var setValue: com.giyeok.bibix.runner.BibixValueProto.SetValue
      @JvmName("getSetValue")
      get() = _builder.getSetValue()
      @JvmName("setSetValue")
      set(value) {
        _builder.setSetValue(value)
      }
    /**
     * <code>.com.giyeok.bibix.runner.SetValue set_value = 8;</code>
     */
    public fun clearSetValue() {
      _builder.clearSetValue()
    }
    /**
     * <code>.com.giyeok.bibix.runner.SetValue set_value = 8;</code>
     * @return Whether the setValue field is set.
     */
    public fun hasSetValue(): kotlin.Boolean {
      return _builder.hasSetValue()
    }

    /**
     * <code>.com.giyeok.bibix.runner.TupleValue tuple_value = 9;</code>
     */
    public var tupleValue: com.giyeok.bibix.runner.BibixValueProto.TupleValue
      @JvmName("getTupleValue")
      get() = _builder.getTupleValue()
      @JvmName("setTupleValue")
      set(value) {
        _builder.setTupleValue(value)
      }
    /**
     * <code>.com.giyeok.bibix.runner.TupleValue tuple_value = 9;</code>
     */
    public fun clearTupleValue() {
      _builder.clearTupleValue()
    }
    /**
     * <code>.com.giyeok.bibix.runner.TupleValue tuple_value = 9;</code>
     * @return Whether the tupleValue field is set.
     */
    public fun hasTupleValue(): kotlin.Boolean {
      return _builder.hasTupleValue()
    }

    /**
     * <code>.com.giyeok.bibix.runner.NamedTupleValue named_tuple_value = 10;</code>
     */
    public var namedTupleValue: com.giyeok.bibix.runner.BibixValueProto.NamedTupleValue
      @JvmName("getNamedTupleValue")
      get() = _builder.getNamedTupleValue()
      @JvmName("setNamedTupleValue")
      set(value) {
        _builder.setNamedTupleValue(value)
      }
    /**
     * <code>.com.giyeok.bibix.runner.NamedTupleValue named_tuple_value = 10;</code>
     */
    public fun clearNamedTupleValue() {
      _builder.clearNamedTupleValue()
    }
    /**
     * <code>.com.giyeok.bibix.runner.NamedTupleValue named_tuple_value = 10;</code>
     * @return Whether the namedTupleValue field is set.
     */
    public fun hasNamedTupleValue(): kotlin.Boolean {
      return _builder.hasNamedTupleValue()
    }

    /**
     * <code>.com.giyeok.bibix.runner.ClassInstanceValue class_instance_value = 11;</code>
     */
    public var classInstanceValue: com.giyeok.bibix.runner.BibixValueProto.ClassInstanceValue
      @JvmName("getClassInstanceValue")
      get() = _builder.getClassInstanceValue()
      @JvmName("setClassInstanceValue")
      set(value) {
        _builder.setClassInstanceValue(value)
      }
    /**
     * <code>.com.giyeok.bibix.runner.ClassInstanceValue class_instance_value = 11;</code>
     */
    public fun clearClassInstanceValue() {
      _builder.clearClassInstanceValue()
    }
    /**
     * <code>.com.giyeok.bibix.runner.ClassInstanceValue class_instance_value = 11;</code>
     * @return Whether the classInstanceValue field is set.
     */
    public fun hasClassInstanceValue(): kotlin.Boolean {
      return _builder.hasClassInstanceValue()
    }
    public val valueCase: com.giyeok.bibix.runner.BibixValueProto.BibixValue.ValueCase
      @JvmName("getValueCase")
      get() = _builder.getValueCase()

    public fun clearValue() {
      _builder.clearValue()
    }
  }
}
@kotlin.jvm.JvmSynthetic
public inline fun com.giyeok.bibix.runner.BibixValueProto.BibixValue.copy(block: com.giyeok.bibix.runner.BibixValueKt.Dsl.() -> kotlin.Unit): com.giyeok.bibix.runner.BibixValueProto.BibixValue =
  com.giyeok.bibix.runner.BibixValueKt.Dsl._create(this.toBuilder()).apply { block() }._build()

val com.giyeok.bibix.runner.BibixValueProto.BibixValueOrBuilder.enumValueOrNull: com.giyeok.bibix.runner.BibixValueProto.EnumValue?
  get() = if (hasEnumValue()) getEnumValue() else null

val com.giyeok.bibix.runner.BibixValueProto.BibixValueOrBuilder.listValueOrNull: com.giyeok.bibix.runner.BibixValueProto.ListValue?
  get() = if (hasListValue()) getListValue() else null

val com.giyeok.bibix.runner.BibixValueProto.BibixValueOrBuilder.setValueOrNull: com.giyeok.bibix.runner.BibixValueProto.SetValue?
  get() = if (hasSetValue()) getSetValue() else null

val com.giyeok.bibix.runner.BibixValueProto.BibixValueOrBuilder.tupleValueOrNull: com.giyeok.bibix.runner.BibixValueProto.TupleValue?
  get() = if (hasTupleValue()) getTupleValue() else null

val com.giyeok.bibix.runner.BibixValueProto.BibixValueOrBuilder.namedTupleValueOrNull: com.giyeok.bibix.runner.BibixValueProto.NamedTupleValue?
  get() = if (hasNamedTupleValue()) getNamedTupleValue() else null

val com.giyeok.bibix.runner.BibixValueProto.BibixValueOrBuilder.classInstanceValueOrNull: com.giyeok.bibix.runner.BibixValueProto.ClassInstanceValue?
  get() = if (hasClassInstanceValue()) getClassInstanceValue() else null

