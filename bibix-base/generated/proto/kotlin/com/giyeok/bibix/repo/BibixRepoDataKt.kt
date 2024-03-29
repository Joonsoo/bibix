// Generated by the protocol buffer compiler. DO NOT EDIT!
// source: repo.proto

// Generated files should ignore deprecation warnings
@file:Suppress("DEPRECATION")
package com.giyeok.bibix.repo;

@kotlin.jvm.JvmName("-initializebibixRepoData")
public inline fun bibixRepoData(block: com.giyeok.bibix.repo.BibixRepoDataKt.Dsl.() -> kotlin.Unit): com.giyeok.bibix.repo.BibixRepoProto.BibixRepoData =
  com.giyeok.bibix.repo.BibixRepoDataKt.Dsl._create(com.giyeok.bibix.repo.BibixRepoProto.BibixRepoData.newBuilder()).apply { block() }._build()
/**
 * ```
 * source id hash -> source id
 * target id hash -> target id(including args map & input hashes)
 * ```
 *
 * Protobuf type `com.giyeok.bibix.repo.BibixRepoData`
 */
public object BibixRepoDataKt {
  @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
  @com.google.protobuf.kotlin.ProtoDslMarker
  public class Dsl private constructor(
    private val _builder: com.giyeok.bibix.repo.BibixRepoProto.BibixRepoData.Builder
  ) {
    public companion object {
      @kotlin.jvm.JvmSynthetic
      @kotlin.PublishedApi
      internal fun _create(builder: com.giyeok.bibix.repo.BibixRepoProto.BibixRepoData.Builder): Dsl = Dsl(builder)
    }

    @kotlin.jvm.JvmSynthetic
    @kotlin.PublishedApi
    internal fun _build(): com.giyeok.bibix.repo.BibixRepoProto.BibixRepoData = _builder.build()

    /**
     * An uninstantiable, behaviorless type to represent the field in
     * generics.
     */
    @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
    public class TargetIdDataProxy private constructor() : com.google.protobuf.kotlin.DslProxy()
    /**
     * ```
     * target id hex -> target data
     * ```
     *
     * `map<string, .com.giyeok.bibix.TargetIdData> target_id_data = 1;`
     */
     public val targetIdData: com.google.protobuf.kotlin.DslMap<kotlin.String, com.giyeok.bibix.BibixIdProto.TargetIdData, TargetIdDataProxy>
      @kotlin.jvm.JvmSynthetic
      @JvmName("getTargetIdDataMap")
      get() = com.google.protobuf.kotlin.DslMap(
        _builder.getTargetIdDataMap()
      )
    /**
     * ```
     * target id hex -> target data
     * ```
     *
     * `map<string, .com.giyeok.bibix.TargetIdData> target_id_data = 1;`
     */
    @JvmName("putTargetIdData")
    public fun com.google.protobuf.kotlin.DslMap<kotlin.String, com.giyeok.bibix.BibixIdProto.TargetIdData, TargetIdDataProxy>
      .put(key: kotlin.String, value: com.giyeok.bibix.BibixIdProto.TargetIdData) {
         _builder.putTargetIdData(key, value)
       }
    /**
     * ```
     * target id hex -> target data
     * ```
     *
     * `map<string, .com.giyeok.bibix.TargetIdData> target_id_data = 1;`
     */
    @kotlin.jvm.JvmSynthetic
    @JvmName("setTargetIdData")
    @Suppress("NOTHING_TO_INLINE")
    public inline operator fun com.google.protobuf.kotlin.DslMap<kotlin.String, com.giyeok.bibix.BibixIdProto.TargetIdData, TargetIdDataProxy>
      .set(key: kotlin.String, value: com.giyeok.bibix.BibixIdProto.TargetIdData) {
         put(key, value)
       }
    /**
     * ```
     * target id hex -> target data
     * ```
     *
     * `map<string, .com.giyeok.bibix.TargetIdData> target_id_data = 1;`
     */
    @kotlin.jvm.JvmSynthetic
    @JvmName("removeTargetIdData")
    public fun com.google.protobuf.kotlin.DslMap<kotlin.String, com.giyeok.bibix.BibixIdProto.TargetIdData, TargetIdDataProxy>
      .remove(key: kotlin.String) {
         _builder.removeTargetIdData(key)
       }
    /**
     * ```
     * target id hex -> target data
     * ```
     *
     * `map<string, .com.giyeok.bibix.TargetIdData> target_id_data = 1;`
     */
    @kotlin.jvm.JvmSynthetic
    @JvmName("putAllTargetIdData")
    public fun com.google.protobuf.kotlin.DslMap<kotlin.String, com.giyeok.bibix.BibixIdProto.TargetIdData, TargetIdDataProxy>
      .putAll(map: kotlin.collections.Map<kotlin.String, com.giyeok.bibix.BibixIdProto.TargetIdData>) {
         _builder.putAllTargetIdData(map)
       }
    /**
     * ```
     * target id hex -> target data
     * ```
     *
     * `map<string, .com.giyeok.bibix.TargetIdData> target_id_data = 1;`
     */
    @kotlin.jvm.JvmSynthetic
    @JvmName("clearTargetIdData")
    public fun com.google.protobuf.kotlin.DslMap<kotlin.String, com.giyeok.bibix.BibixIdProto.TargetIdData, TargetIdDataProxy>
      .clear() {
         _builder.clearTargetIdData()
       }

    /**
     * An uninstantiable, behaviorless type to represent the field in
     * generics.
     */
    @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
    public class TargetStatesProxy private constructor() : com.google.protobuf.kotlin.DslProxy()
    /**
     * `map<string, .com.giyeok.bibix.repo.TargetState> target_states = 2;`
     */
     public val targetStates: com.google.protobuf.kotlin.DslMap<kotlin.String, com.giyeok.bibix.repo.BibixRepoProto.TargetState, TargetStatesProxy>
      @kotlin.jvm.JvmSynthetic
      @JvmName("getTargetStatesMap")
      get() = com.google.protobuf.kotlin.DslMap(
        _builder.getTargetStatesMap()
      )
    /**
     * `map<string, .com.giyeok.bibix.repo.TargetState> target_states = 2;`
     */
    @JvmName("putTargetStates")
    public fun com.google.protobuf.kotlin.DslMap<kotlin.String, com.giyeok.bibix.repo.BibixRepoProto.TargetState, TargetStatesProxy>
      .put(key: kotlin.String, value: com.giyeok.bibix.repo.BibixRepoProto.TargetState) {
         _builder.putTargetStates(key, value)
       }
    /**
     * `map<string, .com.giyeok.bibix.repo.TargetState> target_states = 2;`
     */
    @kotlin.jvm.JvmSynthetic
    @JvmName("setTargetStates")
    @Suppress("NOTHING_TO_INLINE")
    public inline operator fun com.google.protobuf.kotlin.DslMap<kotlin.String, com.giyeok.bibix.repo.BibixRepoProto.TargetState, TargetStatesProxy>
      .set(key: kotlin.String, value: com.giyeok.bibix.repo.BibixRepoProto.TargetState) {
         put(key, value)
       }
    /**
     * `map<string, .com.giyeok.bibix.repo.TargetState> target_states = 2;`
     */
    @kotlin.jvm.JvmSynthetic
    @JvmName("removeTargetStates")
    public fun com.google.protobuf.kotlin.DslMap<kotlin.String, com.giyeok.bibix.repo.BibixRepoProto.TargetState, TargetStatesProxy>
      .remove(key: kotlin.String) {
         _builder.removeTargetStates(key)
       }
    /**
     * `map<string, .com.giyeok.bibix.repo.TargetState> target_states = 2;`
     */
    @kotlin.jvm.JvmSynthetic
    @JvmName("putAllTargetStates")
    public fun com.google.protobuf.kotlin.DslMap<kotlin.String, com.giyeok.bibix.repo.BibixRepoProto.TargetState, TargetStatesProxy>
      .putAll(map: kotlin.collections.Map<kotlin.String, com.giyeok.bibix.repo.BibixRepoProto.TargetState>) {
         _builder.putAllTargetStates(map)
       }
    /**
     * `map<string, .com.giyeok.bibix.repo.TargetState> target_states = 2;`
     */
    @kotlin.jvm.JvmSynthetic
    @JvmName("clearTargetStates")
    public fun com.google.protobuf.kotlin.DslMap<kotlin.String, com.giyeok.bibix.repo.BibixRepoProto.TargetState, TargetStatesProxy>
      .clear() {
         _builder.clearTargetStates()
       }

    /**
     * An uninstantiable, behaviorless type to represent the field in
     * generics.
     */
    @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
    public class OutputNamesProxy private constructor() : com.google.protobuf.kotlin.DslProxy()
    /**
     * ```
     * user defined output name -> target id hex
     * ```
     *
     * `map<string, string> output_names = 3;`
     */
     public val outputNames: com.google.protobuf.kotlin.DslMap<kotlin.String, kotlin.String, OutputNamesProxy>
      @kotlin.jvm.JvmSynthetic
      @JvmName("getOutputNamesMap")
      get() = com.google.protobuf.kotlin.DslMap(
        _builder.getOutputNamesMap()
      )
    /**
     * ```
     * user defined output name -> target id hex
     * ```
     *
     * `map<string, string> output_names = 3;`
     */
    @JvmName("putOutputNames")
    public fun com.google.protobuf.kotlin.DslMap<kotlin.String, kotlin.String, OutputNamesProxy>
      .put(key: kotlin.String, value: kotlin.String) {
         _builder.putOutputNames(key, value)
       }
    /**
     * ```
     * user defined output name -> target id hex
     * ```
     *
     * `map<string, string> output_names = 3;`
     */
    @kotlin.jvm.JvmSynthetic
    @JvmName("setOutputNames")
    @Suppress("NOTHING_TO_INLINE")
    public inline operator fun com.google.protobuf.kotlin.DslMap<kotlin.String, kotlin.String, OutputNamesProxy>
      .set(key: kotlin.String, value: kotlin.String) {
         put(key, value)
       }
    /**
     * ```
     * user defined output name -> target id hex
     * ```
     *
     * `map<string, string> output_names = 3;`
     */
    @kotlin.jvm.JvmSynthetic
    @JvmName("removeOutputNames")
    public fun com.google.protobuf.kotlin.DslMap<kotlin.String, kotlin.String, OutputNamesProxy>
      .remove(key: kotlin.String) {
         _builder.removeOutputNames(key)
       }
    /**
     * ```
     * user defined output name -> target id hex
     * ```
     *
     * `map<string, string> output_names = 3;`
     */
    @kotlin.jvm.JvmSynthetic
    @JvmName("putAllOutputNames")
    public fun com.google.protobuf.kotlin.DslMap<kotlin.String, kotlin.String, OutputNamesProxy>
      .putAll(map: kotlin.collections.Map<kotlin.String, kotlin.String>) {
         _builder.putAllOutputNames(map)
       }
    /**
     * ```
     * user defined output name -> target id hex
     * ```
     *
     * `map<string, string> output_names = 3;`
     */
    @kotlin.jvm.JvmSynthetic
    @JvmName("clearOutputNames")
    public fun com.google.protobuf.kotlin.DslMap<kotlin.String, kotlin.String, OutputNamesProxy>
      .clear() {
         _builder.clearOutputNames()
       }
  }
}
@kotlin.jvm.JvmSynthetic
@com.google.errorprone.annotations.CheckReturnValue
public inline fun com.giyeok.bibix.repo.BibixRepoProto.BibixRepoData.copy(block: com.giyeok.bibix.repo.BibixRepoDataKt.Dsl.() -> kotlin.Unit): com.giyeok.bibix.repo.BibixRepoProto.BibixRepoData =
  com.giyeok.bibix.repo.BibixRepoDataKt.Dsl._create(this.toBuilder()).apply { block() }._build()

