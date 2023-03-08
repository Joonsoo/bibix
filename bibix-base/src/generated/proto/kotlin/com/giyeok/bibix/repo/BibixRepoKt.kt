// Generated by the protocol buffer compiler. DO NOT EDIT!
// source: repo.proto

// Generated files should ignore deprecation warnings
@file:Suppress("DEPRECATION")
package com.giyeok.bibix.repo;

@kotlin.jvm.JvmName("-initializebibixRepo")
public inline fun bibixRepo(block: com.giyeok.bibix.repo.BibixRepoKt.Dsl.() -> kotlin.Unit): com.giyeok.bibix.repo.BibixRepoProto.BibixRepo =
  com.giyeok.bibix.repo.BibixRepoKt.Dsl._create(com.giyeok.bibix.repo.BibixRepoProto.BibixRepo.newBuilder()).apply { block() }._build()
/**
 * ```
 * source id hash -> source id
 * target id hash -> target id(including args map & input hashes)
 * ```
 *
 * Protobuf type `com.giyeok.bibix.repo.BibixRepo`
 */
public object BibixRepoKt {
  @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
  @com.google.protobuf.kotlin.ProtoDslMarker
  public class Dsl private constructor(
    private val _builder: com.giyeok.bibix.repo.BibixRepoProto.BibixRepo.Builder
  ) {
    public companion object {
      @kotlin.jvm.JvmSynthetic
      @kotlin.PublishedApi
      internal fun _create(builder: com.giyeok.bibix.repo.BibixRepoProto.BibixRepo.Builder): Dsl = Dsl(builder)
    }

    @kotlin.jvm.JvmSynthetic
    @kotlin.PublishedApi
    internal fun _build(): com.giyeok.bibix.repo.BibixRepoProto.BibixRepo = _builder.build()

    /**
     * An uninstantiable, behaviorless type to represent the field in
     * generics.
     */
    @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
    public class TargetsProxy private constructor() : com.google.protobuf.kotlin.DslProxy()
    /**
     * ```
     * target id hex -> target data
     * ```
     *
     * `repeated .com.giyeok.bibix.repo.TargetData targets = 1;`
     */
     public val targets: com.google.protobuf.kotlin.DslList<com.giyeok.bibix.repo.BibixRepoProto.TargetData, TargetsProxy>
      @kotlin.jvm.JvmSynthetic
      get() = com.google.protobuf.kotlin.DslList(
        _builder.getTargetsList()
      )
    /**
     * ```
     * target id hex -> target data
     * ```
     *
     * `repeated .com.giyeok.bibix.repo.TargetData targets = 1;`
     * @param value The targets to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("addTargets")
    public fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.repo.BibixRepoProto.TargetData, TargetsProxy>.add(value: com.giyeok.bibix.repo.BibixRepoProto.TargetData) {
      _builder.addTargets(value)
    }
    /**
     * ```
     * target id hex -> target data
     * ```
     *
     * `repeated .com.giyeok.bibix.repo.TargetData targets = 1;`
     * @param value The targets to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("plusAssignTargets")
    @Suppress("NOTHING_TO_INLINE")
    public inline operator fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.repo.BibixRepoProto.TargetData, TargetsProxy>.plusAssign(value: com.giyeok.bibix.repo.BibixRepoProto.TargetData) {
      add(value)
    }
    /**
     * ```
     * target id hex -> target data
     * ```
     *
     * `repeated .com.giyeok.bibix.repo.TargetData targets = 1;`
     * @param values The targets to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("addAllTargets")
    public fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.repo.BibixRepoProto.TargetData, TargetsProxy>.addAll(values: kotlin.collections.Iterable<com.giyeok.bibix.repo.BibixRepoProto.TargetData>) {
      _builder.addAllTargets(values)
    }
    /**
     * ```
     * target id hex -> target data
     * ```
     *
     * `repeated .com.giyeok.bibix.repo.TargetData targets = 1;`
     * @param values The targets to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("plusAssignAllTargets")
    @Suppress("NOTHING_TO_INLINE")
    public inline operator fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.repo.BibixRepoProto.TargetData, TargetsProxy>.plusAssign(values: kotlin.collections.Iterable<com.giyeok.bibix.repo.BibixRepoProto.TargetData>) {
      addAll(values)
    }
    /**
     * ```
     * target id hex -> target data
     * ```
     *
     * `repeated .com.giyeok.bibix.repo.TargetData targets = 1;`
     * @param index The index to set the value at.
     * @param value The targets to set.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("setTargets")
    public operator fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.repo.BibixRepoProto.TargetData, TargetsProxy>.set(index: kotlin.Int, value: com.giyeok.bibix.repo.BibixRepoProto.TargetData) {
      _builder.setTargets(index, value)
    }
    /**
     * ```
     * target id hex -> target data
     * ```
     *
     * `repeated .com.giyeok.bibix.repo.TargetData targets = 1;`
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("clearTargets")
    public fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.repo.BibixRepoProto.TargetData, TargetsProxy>.clear() {
      _builder.clearTargets()
    }


    /**
     * An uninstantiable, behaviorless type to represent the field in
     * generics.
     */
    @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
    public class OutputNamesProxy private constructor() : com.google.protobuf.kotlin.DslProxy()
    /**
     * ```
     * user defined output name -> target id
     * ```
     *
     * `repeated .com.giyeok.bibix.repo.OutputName output_names = 2;`
     */
     public val outputNames: com.google.protobuf.kotlin.DslList<com.giyeok.bibix.repo.BibixRepoProto.OutputName, OutputNamesProxy>
      @kotlin.jvm.JvmSynthetic
      get() = com.google.protobuf.kotlin.DslList(
        _builder.getOutputNamesList()
      )
    /**
     * ```
     * user defined output name -> target id
     * ```
     *
     * `repeated .com.giyeok.bibix.repo.OutputName output_names = 2;`
     * @param value The outputNames to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("addOutputNames")
    public fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.repo.BibixRepoProto.OutputName, OutputNamesProxy>.add(value: com.giyeok.bibix.repo.BibixRepoProto.OutputName) {
      _builder.addOutputNames(value)
    }
    /**
     * ```
     * user defined output name -> target id
     * ```
     *
     * `repeated .com.giyeok.bibix.repo.OutputName output_names = 2;`
     * @param value The outputNames to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("plusAssignOutputNames")
    @Suppress("NOTHING_TO_INLINE")
    public inline operator fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.repo.BibixRepoProto.OutputName, OutputNamesProxy>.plusAssign(value: com.giyeok.bibix.repo.BibixRepoProto.OutputName) {
      add(value)
    }
    /**
     * ```
     * user defined output name -> target id
     * ```
     *
     * `repeated .com.giyeok.bibix.repo.OutputName output_names = 2;`
     * @param values The outputNames to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("addAllOutputNames")
    public fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.repo.BibixRepoProto.OutputName, OutputNamesProxy>.addAll(values: kotlin.collections.Iterable<com.giyeok.bibix.repo.BibixRepoProto.OutputName>) {
      _builder.addAllOutputNames(values)
    }
    /**
     * ```
     * user defined output name -> target id
     * ```
     *
     * `repeated .com.giyeok.bibix.repo.OutputName output_names = 2;`
     * @param values The outputNames to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("plusAssignAllOutputNames")
    @Suppress("NOTHING_TO_INLINE")
    public inline operator fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.repo.BibixRepoProto.OutputName, OutputNamesProxy>.plusAssign(values: kotlin.collections.Iterable<com.giyeok.bibix.repo.BibixRepoProto.OutputName>) {
      addAll(values)
    }
    /**
     * ```
     * user defined output name -> target id
     * ```
     *
     * `repeated .com.giyeok.bibix.repo.OutputName output_names = 2;`
     * @param index The index to set the value at.
     * @param value The outputNames to set.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("setOutputNames")
    public operator fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.repo.BibixRepoProto.OutputName, OutputNamesProxy>.set(index: kotlin.Int, value: com.giyeok.bibix.repo.BibixRepoProto.OutputName) {
      _builder.setOutputNames(index, value)
    }
    /**
     * ```
     * user defined output name -> target id
     * ```
     *
     * `repeated .com.giyeok.bibix.repo.OutputName output_names = 2;`
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("clearOutputNames")
    public fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.repo.BibixRepoProto.OutputName, OutputNamesProxy>.clear() {
      _builder.clearOutputNames()
    }

  }
}
@kotlin.jvm.JvmSynthetic
@com.google.errorprone.annotations.CheckReturnValue
public inline fun com.giyeok.bibix.repo.BibixRepoProto.BibixRepo.copy(block: com.giyeok.bibix.repo.BibixRepoKt.Dsl.() -> kotlin.Unit): com.giyeok.bibix.repo.BibixRepoProto.BibixRepo =
  com.giyeok.bibix.repo.BibixRepoKt.Dsl._create(this.toBuilder()).apply { block() }._build()

