// Generated by the protocol buffer compiler. DO NOT EDIT!
// source: repo.proto

// Generated files should ignore deprecation warnings
@file:Suppress("DEPRECATION")
package com.giyeok.bibix.repo;

@kotlin.jvm.JvmName("-initializetargetState")
public inline fun targetState(block: com.giyeok.bibix.repo.TargetStateKt.Dsl.() -> kotlin.Unit): com.giyeok.bibix.repo.BibixRepoProto.TargetState =
  com.giyeok.bibix.repo.TargetStateKt.Dsl._create(com.giyeok.bibix.repo.BibixRepoProto.TargetState.newBuilder()).apply { block() }._build()
/**
 * Protobuf type `com.giyeok.bibix.repo.TargetState`
 */
public object TargetStateKt {
  @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
  @com.google.protobuf.kotlin.ProtoDslMarker
  public class Dsl private constructor(
    private val _builder: com.giyeok.bibix.repo.BibixRepoProto.TargetState.Builder
  ) {
    public companion object {
      @kotlin.jvm.JvmSynthetic
      @kotlin.PublishedApi
      internal fun _create(builder: com.giyeok.bibix.repo.BibixRepoProto.TargetState.Builder): Dsl = Dsl(builder)
    }

    @kotlin.jvm.JvmSynthetic
    @kotlin.PublishedApi
    internal fun _build(): com.giyeok.bibix.repo.BibixRepoProto.TargetState = _builder.build()

    /**
     * ```
     * unique_run_id는 빌드가 실행될 때마다 랜덤하게 발급되는 고유 ID
     * ```
     *
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
     * ```
     * unique_run_id는 빌드가 실행될 때마다 랜덤하게 발급되는 고유 ID
     * ```
     *
     * `string unique_run_id = 1;`
     */
    public fun clearUniqueRunId() {
      _builder.clearUniqueRunId()
    }

    /**
     * `.google.protobuf.Timestamp build_start_time = 2;`
     */
    public var buildStartTime: com.google.protobuf.Timestamp
      @JvmName("getBuildStartTime")
      get() = _builder.getBuildStartTime()
      @JvmName("setBuildStartTime")
      set(value) {
        _builder.setBuildStartTime(value)
      }
    /**
     * `.google.protobuf.Timestamp build_start_time = 2;`
     */
    public fun clearBuildStartTime() {
      _builder.clearBuildStartTime()
    }
    /**
     * `.google.protobuf.Timestamp build_start_time = 2;`
     * @return Whether the buildStartTime field is set.
     */
    public fun hasBuildStartTime(): kotlin.Boolean {
      return _builder.hasBuildStartTime()
    }

    /**
     * `.com.giyeok.bibix.InputHashes input_hashes = 3;`
     */
    public var inputHashes: com.giyeok.bibix.BibixIdProto.InputHashes
      @JvmName("getInputHashes")
      get() = _builder.getInputHashes()
      @JvmName("setInputHashes")
      set(value) {
        _builder.setInputHashes(value)
      }
    /**
     * `.com.giyeok.bibix.InputHashes input_hashes = 3;`
     */
    public fun clearInputHashes() {
      _builder.clearInputHashes()
    }
    /**
     * `.com.giyeok.bibix.InputHashes input_hashes = 3;`
     * @return Whether the inputHashes field is set.
     */
    public fun hasInputHashes(): kotlin.Boolean {
      return _builder.hasInputHashes()
    }

    /**
     * `bytes input_hash_string = 4;`
     */
    public var inputHashString: com.google.protobuf.ByteString
      @JvmName("getInputHashString")
      get() = _builder.getInputHashString()
      @JvmName("setInputHashString")
      set(value) {
        _builder.setInputHashString(value)
      }
    /**
     * `bytes input_hash_string = 4;`
     */
    public fun clearInputHashString() {
      _builder.clearInputHashString()
    }

    /**
     * `.google.protobuf.Empty build_started = 5;`
     */
    public var buildStarted: com.google.protobuf.Empty
      @JvmName("getBuildStarted")
      get() = _builder.getBuildStarted()
      @JvmName("setBuildStarted")
      set(value) {
        _builder.setBuildStarted(value)
      }
    /**
     * `.google.protobuf.Empty build_started = 5;`
     */
    public fun clearBuildStarted() {
      _builder.clearBuildStarted()
    }
    /**
     * `.google.protobuf.Empty build_started = 5;`
     * @return Whether the buildStarted field is set.
     */
    public fun hasBuildStarted(): kotlin.Boolean {
      return _builder.hasBuildStarted()
    }

    /**
     * `.com.giyeok.bibix.repo.TargetState.BuildSucceeded build_succeeded = 6;`
     */
    public var buildSucceeded: com.giyeok.bibix.repo.BibixRepoProto.TargetState.BuildSucceeded
      @JvmName("getBuildSucceeded")
      get() = _builder.getBuildSucceeded()
      @JvmName("setBuildSucceeded")
      set(value) {
        _builder.setBuildSucceeded(value)
      }
    /**
     * `.com.giyeok.bibix.repo.TargetState.BuildSucceeded build_succeeded = 6;`
     */
    public fun clearBuildSucceeded() {
      _builder.clearBuildSucceeded()
    }
    /**
     * `.com.giyeok.bibix.repo.TargetState.BuildSucceeded build_succeeded = 6;`
     * @return Whether the buildSucceeded field is set.
     */
    public fun hasBuildSucceeded(): kotlin.Boolean {
      return _builder.hasBuildSucceeded()
    }

    /**
     * `.com.giyeok.bibix.repo.TargetState.BuildFailed build_failed = 7;`
     */
    public var buildFailed: com.giyeok.bibix.repo.BibixRepoProto.TargetState.BuildFailed
      @JvmName("getBuildFailed")
      get() = _builder.getBuildFailed()
      @JvmName("setBuildFailed")
      set(value) {
        _builder.setBuildFailed(value)
      }
    /**
     * `.com.giyeok.bibix.repo.TargetState.BuildFailed build_failed = 7;`
     */
    public fun clearBuildFailed() {
      _builder.clearBuildFailed()
    }
    /**
     * `.com.giyeok.bibix.repo.TargetState.BuildFailed build_failed = 7;`
     * @return Whether the buildFailed field is set.
     */
    public fun hasBuildFailed(): kotlin.Boolean {
      return _builder.hasBuildFailed()
    }
    public val stateCase: com.giyeok.bibix.repo.BibixRepoProto.TargetState.StateCase
      @JvmName("getStateCase")
      get() = _builder.getStateCase()

    public fun clearState() {
      _builder.clearState()
    }
  }
  @kotlin.jvm.JvmName("-initializebuildSucceeded")
  public inline fun buildSucceeded(block: com.giyeok.bibix.repo.TargetStateKt.BuildSucceededKt.Dsl.() -> kotlin.Unit): com.giyeok.bibix.repo.BibixRepoProto.TargetState.BuildSucceeded =
    com.giyeok.bibix.repo.TargetStateKt.BuildSucceededKt.Dsl._create(com.giyeok.bibix.repo.BibixRepoProto.TargetState.BuildSucceeded.newBuilder()).apply { block() }._build()
  /**
   * Protobuf type `com.giyeok.bibix.repo.TargetState.BuildSucceeded`
   */
  public object BuildSucceededKt {
    @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
    @com.google.protobuf.kotlin.ProtoDslMarker
    public class Dsl private constructor(
      private val _builder: com.giyeok.bibix.repo.BibixRepoProto.TargetState.BuildSucceeded.Builder
    ) {
      public companion object {
        @kotlin.jvm.JvmSynthetic
        @kotlin.PublishedApi
        internal fun _create(builder: com.giyeok.bibix.repo.BibixRepoProto.TargetState.BuildSucceeded.Builder): Dsl = Dsl(builder)
      }

      @kotlin.jvm.JvmSynthetic
      @kotlin.PublishedApi
      internal fun _build(): com.giyeok.bibix.repo.BibixRepoProto.TargetState.BuildSucceeded = _builder.build()

      /**
       * `.google.protobuf.Timestamp build_end_time = 1;`
       */
      public var buildEndTime: com.google.protobuf.Timestamp
        @JvmName("getBuildEndTime")
        get() = _builder.getBuildEndTime()
        @JvmName("setBuildEndTime")
        set(value) {
          _builder.setBuildEndTime(value)
        }
      /**
       * `.google.protobuf.Timestamp build_end_time = 1;`
       */
      public fun clearBuildEndTime() {
        _builder.clearBuildEndTime()
      }
      /**
       * `.google.protobuf.Timestamp build_end_time = 1;`
       * @return Whether the buildEndTime field is set.
       */
      public fun hasBuildEndTime(): kotlin.Boolean {
        return _builder.hasBuildEndTime()
      }

      /**
       * `.com.giyeok.bibix.BibixValue result_value = 2;`
       */
      public var resultValue: com.giyeok.bibix.BibixValueProto.BibixValue
        @JvmName("getResultValue")
        get() = _builder.getResultValue()
        @JvmName("setResultValue")
        set(value) {
          _builder.setResultValue(value)
        }
      /**
       * `.com.giyeok.bibix.BibixValue result_value = 2;`
       */
      public fun clearResultValue() {
        _builder.clearResultValue()
      }
      /**
       * `.com.giyeok.bibix.BibixValue result_value = 2;`
       * @return Whether the resultValue field is set.
       */
      public fun hasResultValue(): kotlin.Boolean {
        return _builder.hasResultValue()
      }

      /**
       * ```
       * object id는 뭐지 근데..?
       * ```
       *
       * `optional bytes object_id = 3;`
       */
      public var objectId: com.google.protobuf.ByteString
        @JvmName("getObjectId")
        get() = _builder.getObjectId()
        @JvmName("setObjectId")
        set(value) {
          _builder.setObjectId(value)
        }
      /**
       * ```
       * object id는 뭐지 근데..?
       * ```
       *
       * `optional bytes object_id = 3;`
       */
      public fun clearObjectId() {
        _builder.clearObjectId()
      }
      /**
       * ```
       * object id는 뭐지 근데..?
       * ```
       *
       * `optional bytes object_id = 3;`
       * @return Whether the objectId field is set.
       */
      public fun hasObjectId(): kotlin.Boolean {
        return _builder.hasObjectId()
      }
    }
  }
  @kotlin.jvm.JvmName("-initializebuildFailed")
  public inline fun buildFailed(block: com.giyeok.bibix.repo.TargetStateKt.BuildFailedKt.Dsl.() -> kotlin.Unit): com.giyeok.bibix.repo.BibixRepoProto.TargetState.BuildFailed =
    com.giyeok.bibix.repo.TargetStateKt.BuildFailedKt.Dsl._create(com.giyeok.bibix.repo.BibixRepoProto.TargetState.BuildFailed.newBuilder()).apply { block() }._build()
  /**
   * Protobuf type `com.giyeok.bibix.repo.TargetState.BuildFailed`
   */
  public object BuildFailedKt {
    @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
    @com.google.protobuf.kotlin.ProtoDslMarker
    public class Dsl private constructor(
      private val _builder: com.giyeok.bibix.repo.BibixRepoProto.TargetState.BuildFailed.Builder
    ) {
      public companion object {
        @kotlin.jvm.JvmSynthetic
        @kotlin.PublishedApi
        internal fun _create(builder: com.giyeok.bibix.repo.BibixRepoProto.TargetState.BuildFailed.Builder): Dsl = Dsl(builder)
      }

      @kotlin.jvm.JvmSynthetic
      @kotlin.PublishedApi
      internal fun _build(): com.giyeok.bibix.repo.BibixRepoProto.TargetState.BuildFailed = _builder.build()

      /**
       * `.google.protobuf.Timestamp build_fail_time = 1;`
       */
      public var buildFailTime: com.google.protobuf.Timestamp
        @JvmName("getBuildFailTime")
        get() = _builder.getBuildFailTime()
        @JvmName("setBuildFailTime")
        set(value) {
          _builder.setBuildFailTime(value)
        }
      /**
       * `.google.protobuf.Timestamp build_fail_time = 1;`
       */
      public fun clearBuildFailTime() {
        _builder.clearBuildFailTime()
      }
      /**
       * `.google.protobuf.Timestamp build_fail_time = 1;`
       * @return Whether the buildFailTime field is set.
       */
      public fun hasBuildFailTime(): kotlin.Boolean {
        return _builder.hasBuildFailTime()
      }

      /**
       * `string error_message = 2;`
       */
      public var errorMessage: kotlin.String
        @JvmName("getErrorMessage")
        get() = _builder.getErrorMessage()
        @JvmName("setErrorMessage")
        set(value) {
          _builder.setErrorMessage(value)
        }
      /**
       * `string error_message = 2;`
       */
      public fun clearErrorMessage() {
        _builder.clearErrorMessage()
      }
    }
  }
}
@kotlin.jvm.JvmSynthetic
@com.google.errorprone.annotations.CheckReturnValue
public inline fun com.giyeok.bibix.repo.BibixRepoProto.TargetState.copy(block: com.giyeok.bibix.repo.TargetStateKt.Dsl.() -> kotlin.Unit): com.giyeok.bibix.repo.BibixRepoProto.TargetState =
  com.giyeok.bibix.repo.TargetStateKt.Dsl._create(this.toBuilder()).apply { block() }._build()

@kotlin.jvm.JvmSynthetic
@com.google.errorprone.annotations.CheckReturnValue
public inline fun com.giyeok.bibix.repo.BibixRepoProto.TargetState.BuildSucceeded.copy(block: com.giyeok.bibix.repo.TargetStateKt.BuildSucceededKt.Dsl.() -> kotlin.Unit): com.giyeok.bibix.repo.BibixRepoProto.TargetState.BuildSucceeded =
  com.giyeok.bibix.repo.TargetStateKt.BuildSucceededKt.Dsl._create(this.toBuilder()).apply { block() }._build()

public val com.giyeok.bibix.repo.BibixRepoProto.TargetState.BuildSucceededOrBuilder.buildEndTimeOrNull: com.google.protobuf.Timestamp?
  get() = if (hasBuildEndTime()) getBuildEndTime() else null

public val com.giyeok.bibix.repo.BibixRepoProto.TargetState.BuildSucceededOrBuilder.resultValueOrNull: com.giyeok.bibix.BibixValueProto.BibixValue?
  get() = if (hasResultValue()) getResultValue() else null

@kotlin.jvm.JvmSynthetic
@com.google.errorprone.annotations.CheckReturnValue
public inline fun com.giyeok.bibix.repo.BibixRepoProto.TargetState.BuildFailed.copy(block: com.giyeok.bibix.repo.TargetStateKt.BuildFailedKt.Dsl.() -> kotlin.Unit): com.giyeok.bibix.repo.BibixRepoProto.TargetState.BuildFailed =
  com.giyeok.bibix.repo.TargetStateKt.BuildFailedKt.Dsl._create(this.toBuilder()).apply { block() }._build()

public val com.giyeok.bibix.repo.BibixRepoProto.TargetState.BuildFailedOrBuilder.buildFailTimeOrNull: com.google.protobuf.Timestamp?
  get() = if (hasBuildFailTime()) getBuildFailTime() else null

public val com.giyeok.bibix.repo.BibixRepoProto.TargetStateOrBuilder.buildStartTimeOrNull: com.google.protobuf.Timestamp?
  get() = if (hasBuildStartTime()) getBuildStartTime() else null

public val com.giyeok.bibix.repo.BibixRepoProto.TargetStateOrBuilder.inputHashesOrNull: com.giyeok.bibix.BibixIdProto.InputHashes?
  get() = if (hasInputHashes()) getInputHashes() else null

public val com.giyeok.bibix.repo.BibixRepoProto.TargetStateOrBuilder.buildStartedOrNull: com.google.protobuf.Empty?
  get() = if (hasBuildStarted()) getBuildStarted() else null

public val com.giyeok.bibix.repo.BibixRepoProto.TargetStateOrBuilder.buildSucceededOrNull: com.giyeok.bibix.repo.BibixRepoProto.TargetState.BuildSucceeded?
  get() = if (hasBuildSucceeded()) getBuildSucceeded() else null

public val com.giyeok.bibix.repo.BibixRepoProto.TargetStateOrBuilder.buildFailedOrNull: com.giyeok.bibix.repo.BibixRepoProto.TargetState.BuildFailed?
  get() = if (hasBuildFailed()) getBuildFailed() else null

