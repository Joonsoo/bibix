//Generated by the protocol buffer compiler. DO NOT EDIT!
// source: api.proto

package com.giyeok.bibix.daemon;

@kotlin.jvm.JvmSynthetic
public inline fun actionResult(block: com.giyeok.bibix.daemon.ActionResultKt.Dsl.() -> kotlin.Unit): com.giyeok.bibix.daemon.BibixDaemonApiProto.ActionResult =
  com.giyeok.bibix.daemon.ActionResultKt.Dsl._create(com.giyeok.bibix.daemon.BibixDaemonApiProto.ActionResult.newBuilder()).apply { block() }._build()
public object ActionResultKt {
  @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
  @com.google.protobuf.kotlin.ProtoDslMarker
  public class Dsl private constructor(
    private val _builder: com.giyeok.bibix.daemon.BibixDaemonApiProto.ActionResult.Builder
  ) {
    public companion object {
      @kotlin.jvm.JvmSynthetic
      @kotlin.PublishedApi
      internal fun _create(builder: com.giyeok.bibix.daemon.BibixDaemonApiProto.ActionResult.Builder): Dsl = Dsl(builder)
    }

    @kotlin.jvm.JvmSynthetic
    @kotlin.PublishedApi
    internal fun _build(): com.giyeok.bibix.daemon.BibixDaemonApiProto.ActionResult = _builder.build()
  }
}
@kotlin.jvm.JvmSynthetic
public inline fun com.giyeok.bibix.daemon.BibixDaemonApiProto.ActionResult.copy(block: com.giyeok.bibix.daemon.ActionResultKt.Dsl.() -> kotlin.Unit): com.giyeok.bibix.daemon.BibixDaemonApiProto.ActionResult =
  com.giyeok.bibix.daemon.ActionResultKt.Dsl._create(this.toBuilder()).apply { block() }._build()
