// Generated by the protocol buffer compiler. DO NOT EDIT!
// source: repo.proto

// Generated files should ignore deprecation warnings
@file:Suppress("DEPRECATION")
package com.giyeok.bibix.repo;

@kotlin.jvm.JvmName("-initializetargetLogs")
public inline fun targetLogs(block: com.giyeok.bibix.repo.TargetLogsKt.Dsl.() -> kotlin.Unit): com.giyeok.bibix.repo.BibixRepoProto.TargetLogs =
  com.giyeok.bibix.repo.TargetLogsKt.Dsl._create(com.giyeok.bibix.repo.BibixRepoProto.TargetLogs.newBuilder()).apply { block() }._build()
/**
 * Protobuf type `com.giyeok.bibix.repo.TargetLogs`
 */
public object TargetLogsKt {
  @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
  @com.google.protobuf.kotlin.ProtoDslMarker
  public class Dsl private constructor(
    private val _builder: com.giyeok.bibix.repo.BibixRepoProto.TargetLogs.Builder
  ) {
    public companion object {
      @kotlin.jvm.JvmSynthetic
      @kotlin.PublishedApi
      internal fun _create(builder: com.giyeok.bibix.repo.BibixRepoProto.TargetLogs.Builder): Dsl = Dsl(builder)
    }

    @kotlin.jvm.JvmSynthetic
    @kotlin.PublishedApi
    internal fun _build(): com.giyeok.bibix.repo.BibixRepoProto.TargetLogs = _builder.build()

    /**
     * `string unique_run_id = 1;`
     */
    public var uniqueRunId: kotlin.String
      @JvmName("getUniqueRunId")
      get() = _builder.getUniqueRunId()
      @JvmName("setUniqueRunId")
      set(value) {
        _builder.setUniqueRunId(value)
      }
    /**
     * `string unique_run_id = 1;`
     */
    public fun clearUniqueRunId() {
      _builder.clearUniqueRunId()
    }

    /**
     * `string target_id = 2;`
     */
    public var targetId: kotlin.String
      @JvmName("getTargetId")
      get() = _builder.getTargetId()
      @JvmName("setTargetId")
      set(value) {
        _builder.setTargetId(value)
      }
    /**
     * `string target_id = 2;`
     */
    public fun clearTargetId() {
      _builder.clearTargetId()
    }

    /**
     * An uninstantiable, behaviorless type to represent the field in
     * generics.
     */
    @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
    public class BlocksProxy private constructor() : com.google.protobuf.kotlin.DslProxy()
    /**
     * `repeated .com.giyeok.bibix.repo.LogBlock blocks = 3;`
     */
     public val blocks: com.google.protobuf.kotlin.DslList<com.giyeok.bibix.repo.BibixRepoProto.LogBlock, BlocksProxy>
      @kotlin.jvm.JvmSynthetic
      get() = com.google.protobuf.kotlin.DslList(
        _builder.getBlocksList()
      )
    /**
     * `repeated .com.giyeok.bibix.repo.LogBlock blocks = 3;`
     * @param value The blocks to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("addBlocks")
    public fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.repo.BibixRepoProto.LogBlock, BlocksProxy>.add(value: com.giyeok.bibix.repo.BibixRepoProto.LogBlock) {
      _builder.addBlocks(value)
    }
    /**
     * `repeated .com.giyeok.bibix.repo.LogBlock blocks = 3;`
     * @param value The blocks to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("plusAssignBlocks")
    @Suppress("NOTHING_TO_INLINE")
    public inline operator fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.repo.BibixRepoProto.LogBlock, BlocksProxy>.plusAssign(value: com.giyeok.bibix.repo.BibixRepoProto.LogBlock) {
      add(value)
    }
    /**
     * `repeated .com.giyeok.bibix.repo.LogBlock blocks = 3;`
     * @param values The blocks to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("addAllBlocks")
    public fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.repo.BibixRepoProto.LogBlock, BlocksProxy>.addAll(values: kotlin.collections.Iterable<com.giyeok.bibix.repo.BibixRepoProto.LogBlock>) {
      _builder.addAllBlocks(values)
    }
    /**
     * `repeated .com.giyeok.bibix.repo.LogBlock blocks = 3;`
     * @param values The blocks to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("plusAssignAllBlocks")
    @Suppress("NOTHING_TO_INLINE")
    public inline operator fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.repo.BibixRepoProto.LogBlock, BlocksProxy>.plusAssign(values: kotlin.collections.Iterable<com.giyeok.bibix.repo.BibixRepoProto.LogBlock>) {
      addAll(values)
    }
    /**
     * `repeated .com.giyeok.bibix.repo.LogBlock blocks = 3;`
     * @param index The index to set the value at.
     * @param value The blocks to set.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("setBlocks")
    public operator fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.repo.BibixRepoProto.LogBlock, BlocksProxy>.set(index: kotlin.Int, value: com.giyeok.bibix.repo.BibixRepoProto.LogBlock) {
      _builder.setBlocks(index, value)
    }
    /**
     * `repeated .com.giyeok.bibix.repo.LogBlock blocks = 3;`
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("clearBlocks")
    public fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.repo.BibixRepoProto.LogBlock, BlocksProxy>.clear() {
      _builder.clearBlocks()
    }

  }
}
@kotlin.jvm.JvmSynthetic
@com.google.errorprone.annotations.CheckReturnValue
public inline fun com.giyeok.bibix.repo.BibixRepoProto.TargetLogs.copy(block: com.giyeok.bibix.repo.TargetLogsKt.Dsl.() -> kotlin.Unit): com.giyeok.bibix.repo.BibixRepoProto.TargetLogs =
  com.giyeok.bibix.repo.TargetLogsKt.Dsl._create(this.toBuilder()).apply { block() }._build()

