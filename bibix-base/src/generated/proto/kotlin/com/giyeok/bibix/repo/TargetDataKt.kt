//Generated by the protocol buffer compiler. DO NOT EDIT!
// source: repo.proto

package com.giyeok.bibix.repo;

@kotlin.jvm.JvmName("-initializetargetData")
inline fun targetData(block: com.giyeok.bibix.repo.TargetDataKt.Dsl.() -> kotlin.Unit): com.giyeok.bibix.repo.BibixRepoProto.TargetData =
  com.giyeok.bibix.repo.TargetDataKt.Dsl._create(com.giyeok.bibix.repo.BibixRepoProto.TargetData.newBuilder()).apply { block() }._build()
object TargetDataKt {
  @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
  @com.google.protobuf.kotlin.ProtoDslMarker
  class Dsl private constructor(
    private val _builder: com.giyeok.bibix.repo.BibixRepoProto.TargetData.Builder
  ) {
    companion object {
      @kotlin.jvm.JvmSynthetic
      @kotlin.PublishedApi
      internal fun _create(builder: com.giyeok.bibix.repo.BibixRepoProto.TargetData.Builder): Dsl = Dsl(builder)
    }

    @kotlin.jvm.JvmSynthetic
    @kotlin.PublishedApi
    internal fun _build(): com.giyeok.bibix.repo.BibixRepoProto.TargetData = _builder.build()

    /**
     * <code>bytes target_id = 1;</code>
     */
    var targetId: com.google.protobuf.ByteString
      @JvmName("getTargetId")
      get() = _builder.getTargetId()
      @JvmName("setTargetId")
      set(value) {
        _builder.setTargetId(value)
      }
    /**
     * <code>bytes target_id = 1;</code>
     */
    fun clearTargetId() {
      _builder.clearTargetId()
    }

    /**
     * <code>.com.giyeok.bibix.TargetIdData target_id_data = 2;</code>
     */
    var targetIdData: com.giyeok.bibix.BibixIdProto.TargetIdData
      @JvmName("getTargetIdData")
      get() = _builder.getTargetIdData()
      @JvmName("setTargetIdData")
      set(value) {
        _builder.setTargetIdData(value)
      }
    /**
     * <code>.com.giyeok.bibix.TargetIdData target_id_data = 2;</code>
     */
    fun clearTargetIdData() {
      _builder.clearTargetIdData()
    }
    /**
     * <code>.com.giyeok.bibix.TargetIdData target_id_data = 2;</code>
     * @return Whether the targetIdData field is set.
     */
    fun hasTargetIdData(): kotlin.Boolean {
      return _builder.hasTargetIdData()
    }

    /**
     * <code>optional .com.giyeok.bibix.repo.TargetState state = 3;</code>
     */
    var state: com.giyeok.bibix.repo.BibixRepoProto.TargetState
      @JvmName("getState")
      get() = _builder.getState()
      @JvmName("setState")
      set(value) {
        _builder.setState(value)
      }
    /**
     * <code>optional .com.giyeok.bibix.repo.TargetState state = 3;</code>
     */
    fun clearState() {
      _builder.clearState()
    }
    /**
     * <code>optional .com.giyeok.bibix.repo.TargetState state = 3;</code>
     * @return Whether the state field is set.
     */
    fun hasState(): kotlin.Boolean {
      return _builder.hasState()
    }
    val TargetDataKt.Dsl.stateOrNull: com.giyeok.bibix.repo.BibixRepoProto.TargetState?
      get() = _builder.stateOrNull
  }
}
@kotlin.jvm.JvmSynthetic
inline fun com.giyeok.bibix.repo.BibixRepoProto.TargetData.copy(block: com.giyeok.bibix.repo.TargetDataKt.Dsl.() -> kotlin.Unit): com.giyeok.bibix.repo.BibixRepoProto.TargetData =
  com.giyeok.bibix.repo.TargetDataKt.Dsl._create(this.toBuilder()).apply { block() }._build()

val com.giyeok.bibix.repo.BibixRepoProto.TargetDataOrBuilder.targetIdDataOrNull: com.giyeok.bibix.BibixIdProto.TargetIdData?
  get() = if (hasTargetIdData()) getTargetIdData() else null

val com.giyeok.bibix.repo.BibixRepoProto.TargetDataOrBuilder.stateOrNull: com.giyeok.bibix.repo.BibixRepoProto.TargetState?
  get() = if (hasState()) getState() else null
