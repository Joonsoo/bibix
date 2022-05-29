//Generated by the protocol buffer compiler. DO NOT EDIT!
// source: ids.proto

package com.giyeok.bibix.runner;

@kotlin.jvm.JvmName("-initializeargsMap")
public inline fun argsMap(block: com.giyeok.bibix.runner.ArgsMapKt.Dsl.() -> kotlin.Unit): com.giyeok.bibix.runner.BibixIdProto.ArgsMap =
  com.giyeok.bibix.runner.ArgsMapKt.Dsl._create(com.giyeok.bibix.runner.BibixIdProto.ArgsMap.newBuilder()).apply { block() }._build()
public object ArgsMapKt {
  @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
  @com.google.protobuf.kotlin.ProtoDslMarker
  public class Dsl private constructor(
    private val _builder: com.giyeok.bibix.runner.BibixIdProto.ArgsMap.Builder
  ) {
    public companion object {
      @kotlin.jvm.JvmSynthetic
      @kotlin.PublishedApi
      internal fun _create(builder: com.giyeok.bibix.runner.BibixIdProto.ArgsMap.Builder): Dsl = Dsl(builder)
    }

    @kotlin.jvm.JvmSynthetic
    @kotlin.PublishedApi
    internal fun _build(): com.giyeok.bibix.runner.BibixIdProto.ArgsMap = _builder.build()

    /**
     * An uninstantiable, behaviorless type to represent the field in
     * generics.
     */
    @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
    public class PairsProxy private constructor() : com.google.protobuf.kotlin.DslProxy()
    /**
     * <code>repeated .com.giyeok.bibix.runner.ArgPair pairs = 1;</code>
     */
     public val pairs: com.google.protobuf.kotlin.DslList<com.giyeok.bibix.runner.BibixIdProto.ArgPair, PairsProxy>
      @kotlin.jvm.JvmSynthetic
      get() = com.google.protobuf.kotlin.DslList(
        _builder.getPairsList()
      )
    /**
     * <code>repeated .com.giyeok.bibix.runner.ArgPair pairs = 1;</code>
     * @param value The pairs to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("addPairs")
    public fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.runner.BibixIdProto.ArgPair, PairsProxy>.add(value: com.giyeok.bibix.runner.BibixIdProto.ArgPair) {
      _builder.addPairs(value)
    }
    /**
     * <code>repeated .com.giyeok.bibix.runner.ArgPair pairs = 1;</code>
     * @param value The pairs to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("plusAssignPairs")
    @Suppress("NOTHING_TO_INLINE")
    public inline operator fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.runner.BibixIdProto.ArgPair, PairsProxy>.plusAssign(value: com.giyeok.bibix.runner.BibixIdProto.ArgPair) {
      add(value)
    }
    /**
     * <code>repeated .com.giyeok.bibix.runner.ArgPair pairs = 1;</code>
     * @param values The pairs to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("addAllPairs")
    public fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.runner.BibixIdProto.ArgPair, PairsProxy>.addAll(values: kotlin.collections.Iterable<com.giyeok.bibix.runner.BibixIdProto.ArgPair>) {
      _builder.addAllPairs(values)
    }
    /**
     * <code>repeated .com.giyeok.bibix.runner.ArgPair pairs = 1;</code>
     * @param values The pairs to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("plusAssignAllPairs")
    @Suppress("NOTHING_TO_INLINE")
    public inline operator fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.runner.BibixIdProto.ArgPair, PairsProxy>.plusAssign(values: kotlin.collections.Iterable<com.giyeok.bibix.runner.BibixIdProto.ArgPair>) {
      addAll(values)
    }
    /**
     * <code>repeated .com.giyeok.bibix.runner.ArgPair pairs = 1;</code>
     * @param index The index to set the value at.
     * @param value The pairs to set.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("setPairs")
    public operator fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.runner.BibixIdProto.ArgPair, PairsProxy>.set(index: kotlin.Int, value: com.giyeok.bibix.runner.BibixIdProto.ArgPair) {
      _builder.setPairs(index, value)
    }
    /**
     * <code>repeated .com.giyeok.bibix.runner.ArgPair pairs = 1;</code>
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("clearPairs")
    public fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.runner.BibixIdProto.ArgPair, PairsProxy>.clear() {
      _builder.clearPairs()
    }

  }
}
@kotlin.jvm.JvmSynthetic
public inline fun com.giyeok.bibix.runner.BibixIdProto.ArgsMap.copy(block: com.giyeok.bibix.runner.ArgsMapKt.Dsl.() -> kotlin.Unit): com.giyeok.bibix.runner.BibixIdProto.ArgsMap =
  com.giyeok.bibix.runner.ArgsMapKt.Dsl._create(this.toBuilder()).apply { block() }._build()

