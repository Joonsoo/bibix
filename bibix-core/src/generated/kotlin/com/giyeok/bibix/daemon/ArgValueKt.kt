//Generated by the protocol buffer compiler. DO NOT EDIT!
// source: api.proto

package com.giyeok.bibix.daemon;

@kotlin.jvm.JvmName("-initializeargValue")
public inline fun argValue(block: com.giyeok.bibix.daemon.ArgValueKt.Dsl.() -> kotlin.Unit): com.giyeok.bibix.daemon.BibixDaemonApiProto.ArgValue =
  com.giyeok.bibix.daemon.ArgValueKt.Dsl._create(com.giyeok.bibix.daemon.BibixDaemonApiProto.ArgValue.newBuilder()).apply { block() }._build()
public object ArgValueKt {
  @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
  @com.google.protobuf.kotlin.ProtoDslMarker
  public class Dsl private constructor(
    private val _builder: com.giyeok.bibix.daemon.BibixDaemonApiProto.ArgValue.Builder
  ) {
    public companion object {
      @kotlin.jvm.JvmSynthetic
      @kotlin.PublishedApi
      internal fun _create(builder: com.giyeok.bibix.daemon.BibixDaemonApiProto.ArgValue.Builder): Dsl = Dsl(builder)
    }

    @kotlin.jvm.JvmSynthetic
    @kotlin.PublishedApi
    internal fun _build(): com.giyeok.bibix.daemon.BibixDaemonApiProto.ArgValue = _builder.build()

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
     * <code>.com.giyeok.bibix.runner.BibixValue value_inst = 2;</code>
     */
    public var valueInst: com.giyeok.bibix.runner.BibixValueProto.BibixValue
      @JvmName("getValueInst")
      get() = _builder.getValueInst()
      @JvmName("setValueInst")
      set(value) {
        _builder.setValueInst(value)
      }
    /**
     * <code>.com.giyeok.bibix.runner.BibixValue value_inst = 2;</code>
     */
    public fun clearValueInst() {
      _builder.clearValueInst()
    }
    /**
     * <code>.com.giyeok.bibix.runner.BibixValue value_inst = 2;</code>
     * @return Whether the valueInst field is set.
     */
    public fun hasValueInst(): kotlin.Boolean {
      return _builder.hasValueInst()
    }

    /**
     * <code>string value_expr = 3;</code>
     */
    public var valueExpr: kotlin.String
      @JvmName("getValueExpr")
      get() = _builder.getValueExpr()
      @JvmName("setValueExpr")
      set(value) {
        _builder.setValueExpr(value)
      }
    /**
     * <code>string value_expr = 3;</code>
     */
    public fun clearValueExpr() {
      _builder.clearValueExpr()
    }
    /**
     * <code>string value_expr = 3;</code>
     * @return Whether the valueExpr field is set.
     */
    public fun hasValueExpr(): kotlin.Boolean {
      return _builder.hasValueExpr()
    }
    public val valueCase: com.giyeok.bibix.daemon.BibixDaemonApiProto.ArgValue.ValueCase
      @JvmName("getValueCase")
      get() = _builder.getValueCase()

    public fun clearValue() {
      _builder.clearValue()
    }
  }
}
@kotlin.jvm.JvmSynthetic
public inline fun com.giyeok.bibix.daemon.BibixDaemonApiProto.ArgValue.copy(block: com.giyeok.bibix.daemon.ArgValueKt.Dsl.() -> kotlin.Unit): com.giyeok.bibix.daemon.BibixDaemonApiProto.ArgValue =
  com.giyeok.bibix.daemon.ArgValueKt.Dsl._create(this.toBuilder()).apply { block() }._build()

val com.giyeok.bibix.daemon.BibixDaemonApiProto.ArgValueOrBuilder.valueInstOrNull: com.giyeok.bibix.runner.BibixValueProto.BibixValue?
  get() = if (hasValueInst()) getValueInst() else null

