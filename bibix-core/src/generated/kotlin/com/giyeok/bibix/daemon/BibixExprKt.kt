//Generated by the protocol buffer compiler. DO NOT EDIT!
// source: api.proto

package com.giyeok.bibix.daemon;

@kotlin.jvm.JvmSynthetic
public inline fun bibixExpr(block: com.giyeok.bibix.daemon.BibixExprKt.Dsl.() -> kotlin.Unit): com.giyeok.bibix.daemon.BibixDaemonApiProto.BibixExpr =
  com.giyeok.bibix.daemon.BibixExprKt.Dsl._create(com.giyeok.bibix.daemon.BibixDaemonApiProto.BibixExpr.newBuilder()).apply { block() }._build()
public object BibixExprKt {
  @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
  @com.google.protobuf.kotlin.ProtoDslMarker
  public class Dsl private constructor(
    private val _builder: com.giyeok.bibix.daemon.BibixDaemonApiProto.BibixExpr.Builder
  ) {
    public companion object {
      @kotlin.jvm.JvmSynthetic
      @kotlin.PublishedApi
      internal fun _create(builder: com.giyeok.bibix.daemon.BibixDaemonApiProto.BibixExpr.Builder): Dsl = Dsl(builder)
    }

    @kotlin.jvm.JvmSynthetic
    @kotlin.PublishedApi
    internal fun _build(): com.giyeok.bibix.daemon.BibixDaemonApiProto.BibixExpr = _builder.build()

    /**
     * <pre>
     * Optional. evaluate된 결과가 있으면 반환.
     * </pre>
     *
     * <code>.com.giyeok.bibix.runner.BibixValue value = 1;</code>
     */
    public var value: com.giyeok.bibix.runner.BibixValueProto.BibixValue
      @JvmName("getValue")
      get() = _builder.getValue()
      @JvmName("setValue")
      set(value) {
        _builder.setValue(value)
      }
    /**
     * <pre>
     * Optional. evaluate된 결과가 있으면 반환.
     * </pre>
     *
     * <code>.com.giyeok.bibix.runner.BibixValue value = 1;</code>
     */
    public fun clearValue() {
      _builder.clearValue()
    }
    /**
     * <pre>
     * Optional. evaluate된 결과가 있으면 반환.
     * </pre>
     *
     * <code>.com.giyeok.bibix.runner.BibixValue value = 1;</code>
     * @return Whether the value field is set.
     */
    public fun hasValue(): kotlin.Boolean {
      return _builder.hasValue()
    }

    /**
     * <code>.com.giyeok.bibix.daemon.BibixExpr.SeqExpr seq = 2;</code>
     */
    public var seq: com.giyeok.bibix.daemon.BibixDaemonApiProto.BibixExpr.SeqExpr
      @JvmName("getSeq")
      get() = _builder.getSeq()
      @JvmName("setSeq")
      set(value) {
        _builder.setSeq(value)
      }
    /**
     * <code>.com.giyeok.bibix.daemon.BibixExpr.SeqExpr seq = 2;</code>
     */
    public fun clearSeq() {
      _builder.clearSeq()
    }
    /**
     * <code>.com.giyeok.bibix.daemon.BibixExpr.SeqExpr seq = 2;</code>
     * @return Whether the seq field is set.
     */
    public fun hasSeq(): kotlin.Boolean {
      return _builder.hasSeq()
    }

    /**
     * <code>.com.giyeok.bibix.daemon.BibixExpr.Glob glob = 3;</code>
     */
    public var glob: com.giyeok.bibix.daemon.BibixDaemonApiProto.BibixExpr.Glob
      @JvmName("getGlob")
      get() = _builder.getGlob()
      @JvmName("setGlob")
      set(value) {
        _builder.setGlob(value)
      }
    /**
     * <code>.com.giyeok.bibix.daemon.BibixExpr.Glob glob = 3;</code>
     */
    public fun clearGlob() {
      _builder.clearGlob()
    }
    /**
     * <code>.com.giyeok.bibix.daemon.BibixExpr.Glob glob = 3;</code>
     * @return Whether the glob field is set.
     */
    public fun hasGlob(): kotlin.Boolean {
      return _builder.hasGlob()
    }

    /**
     * <code>.com.giyeok.bibix.daemon.BibixExpr.MavenDep maven_dep = 4;</code>
     */
    public var mavenDep: com.giyeok.bibix.daemon.BibixDaemonApiProto.BibixExpr.MavenDep
      @JvmName("getMavenDep")
      get() = _builder.getMavenDep()
      @JvmName("setMavenDep")
      set(value) {
        _builder.setMavenDep(value)
      }
    /**
     * <code>.com.giyeok.bibix.daemon.BibixExpr.MavenDep maven_dep = 4;</code>
     */
    public fun clearMavenDep() {
      _builder.clearMavenDep()
    }
    /**
     * <code>.com.giyeok.bibix.daemon.BibixExpr.MavenDep maven_dep = 4;</code>
     * @return Whether the mavenDep field is set.
     */
    public fun hasMavenDep(): kotlin.Boolean {
      return _builder.hasMavenDep()
    }

    /**
     * <code>string jar_path = 5;</code>
     */
    public var jarPath: kotlin.String
      @JvmName("getJarPath")
      get() = _builder.getJarPath()
      @JvmName("setJarPath")
      set(value) {
        _builder.setJarPath(value)
      }
    /**
     * <code>string jar_path = 5;</code>
     */
    public fun clearJarPath() {
      _builder.clearJarPath()
    }

    /**
     * <code>string jvm_lib_path = 6;</code>
     */
    public var jvmLibPath: kotlin.String
      @JvmName("getJvmLibPath")
      get() = _builder.getJvmLibPath()
      @JvmName("setJvmLibPath")
      set(value) {
        _builder.setJvmLibPath(value)
      }
    /**
     * <code>string jvm_lib_path = 6;</code>
     */
    public fun clearJvmLibPath() {
      _builder.clearJvmLibPath()
    }

    /**
     * <code>.com.giyeok.bibix.daemon.TargetInfo target = 7;</code>
     */
    public var target: com.giyeok.bibix.daemon.BibixDaemonApiProto.TargetInfo
      @JvmName("getTarget")
      get() = _builder.getTarget()
      @JvmName("setTarget")
      set(value) {
        _builder.setTarget(value)
      }
    /**
     * <code>.com.giyeok.bibix.daemon.TargetInfo target = 7;</code>
     */
    public fun clearTarget() {
      _builder.clearTarget()
    }
    /**
     * <code>.com.giyeok.bibix.daemon.TargetInfo target = 7;</code>
     * @return Whether the target field is set.
     */
    public fun hasTarget(): kotlin.Boolean {
      return _builder.hasTarget()
    }
  }
  @kotlin.jvm.JvmSynthetic
  public inline fun seqExpr(block: com.giyeok.bibix.daemon.BibixExprKt.SeqExprKt.Dsl.() -> kotlin.Unit): com.giyeok.bibix.daemon.BibixDaemonApiProto.BibixExpr.SeqExpr =
    com.giyeok.bibix.daemon.BibixExprKt.SeqExprKt.Dsl._create(com.giyeok.bibix.daemon.BibixDaemonApiProto.BibixExpr.SeqExpr.newBuilder()).apply { block() }._build()
  public object SeqExprKt {
    @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
    @com.google.protobuf.kotlin.ProtoDslMarker
    public class Dsl private constructor(
      private val _builder: com.giyeok.bibix.daemon.BibixDaemonApiProto.BibixExpr.SeqExpr.Builder
    ) {
      public companion object {
        @kotlin.jvm.JvmSynthetic
        @kotlin.PublishedApi
        internal fun _create(builder: com.giyeok.bibix.daemon.BibixDaemonApiProto.BibixExpr.SeqExpr.Builder): Dsl = Dsl(builder)
      }

      @kotlin.jvm.JvmSynthetic
      @kotlin.PublishedApi
      internal fun _build(): com.giyeok.bibix.daemon.BibixDaemonApiProto.BibixExpr.SeqExpr = _builder.build()

      /**
       * An uninstantiable, behaviorless type to represent the field in
       * generics.
       */
      @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
      public class ElemsProxy private constructor() : com.google.protobuf.kotlin.DslProxy()
      /**
       * <code>repeated .com.giyeok.bibix.daemon.BibixExpr elems = 1;</code>
       */
       public val elems: com.google.protobuf.kotlin.DslList<com.giyeok.bibix.daemon.BibixDaemonApiProto.BibixExpr, ElemsProxy>
        @kotlin.jvm.JvmSynthetic
        get() = com.google.protobuf.kotlin.DslList(
          _builder.getElemsList()
        )
      /**
       * <code>repeated .com.giyeok.bibix.daemon.BibixExpr elems = 1;</code>
       * @param value The elems to add.
       */
      @kotlin.jvm.JvmSynthetic
      @kotlin.jvm.JvmName("addElems")
      public fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.daemon.BibixDaemonApiProto.BibixExpr, ElemsProxy>.add(value: com.giyeok.bibix.daemon.BibixDaemonApiProto.BibixExpr) {
        _builder.addElems(value)
      }/**
       * <code>repeated .com.giyeok.bibix.daemon.BibixExpr elems = 1;</code>
       * @param value The elems to add.
       */
      @kotlin.jvm.JvmSynthetic
      @kotlin.jvm.JvmName("plusAssignElems")
      @Suppress("NOTHING_TO_INLINE")
      public inline operator fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.daemon.BibixDaemonApiProto.BibixExpr, ElemsProxy>.plusAssign(value: com.giyeok.bibix.daemon.BibixDaemonApiProto.BibixExpr) {
        add(value)
      }/**
       * <code>repeated .com.giyeok.bibix.daemon.BibixExpr elems = 1;</code>
       * @param values The elems to add.
       */
      @kotlin.jvm.JvmSynthetic
      @kotlin.jvm.JvmName("addAllElems")
      public fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.daemon.BibixDaemonApiProto.BibixExpr, ElemsProxy>.addAll(values: kotlin.collections.Iterable<com.giyeok.bibix.daemon.BibixDaemonApiProto.BibixExpr>) {
        _builder.addAllElems(values)
      }/**
       * <code>repeated .com.giyeok.bibix.daemon.BibixExpr elems = 1;</code>
       * @param values The elems to add.
       */
      @kotlin.jvm.JvmSynthetic
      @kotlin.jvm.JvmName("plusAssignAllElems")
      @Suppress("NOTHING_TO_INLINE")
      public inline operator fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.daemon.BibixDaemonApiProto.BibixExpr, ElemsProxy>.plusAssign(values: kotlin.collections.Iterable<com.giyeok.bibix.daemon.BibixDaemonApiProto.BibixExpr>) {
        addAll(values)
      }/**
       * <code>repeated .com.giyeok.bibix.daemon.BibixExpr elems = 1;</code>
       * @param index The index to set the value at.
       * @param value The elems to set.
       */
      @kotlin.jvm.JvmSynthetic
      @kotlin.jvm.JvmName("setElems")
      public operator fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.daemon.BibixDaemonApiProto.BibixExpr, ElemsProxy>.set(index: kotlin.Int, value: com.giyeok.bibix.daemon.BibixDaemonApiProto.BibixExpr) {
        _builder.setElems(index, value)
      }/**
       * <code>repeated .com.giyeok.bibix.daemon.BibixExpr elems = 1;</code>
       */
      @kotlin.jvm.JvmSynthetic
      @kotlin.jvm.JvmName("clearElems")
      public fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.daemon.BibixDaemonApiProto.BibixExpr, ElemsProxy>.clear() {
        _builder.clearElems()
      }}
  }
  @kotlin.jvm.JvmSynthetic
  public inline fun glob(block: com.giyeok.bibix.daemon.BibixExprKt.GlobKt.Dsl.() -> kotlin.Unit): com.giyeok.bibix.daemon.BibixDaemonApiProto.BibixExpr.Glob =
    com.giyeok.bibix.daemon.BibixExprKt.GlobKt.Dsl._create(com.giyeok.bibix.daemon.BibixDaemonApiProto.BibixExpr.Glob.newBuilder()).apply { block() }._build()
  public object GlobKt {
    @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
    @com.google.protobuf.kotlin.ProtoDslMarker
    public class Dsl private constructor(
      private val _builder: com.giyeok.bibix.daemon.BibixDaemonApiProto.BibixExpr.Glob.Builder
    ) {
      public companion object {
        @kotlin.jvm.JvmSynthetic
        @kotlin.PublishedApi
        internal fun _create(builder: com.giyeok.bibix.daemon.BibixDaemonApiProto.BibixExpr.Glob.Builder): Dsl = Dsl(builder)
      }

      @kotlin.jvm.JvmSynthetic
      @kotlin.PublishedApi
      internal fun _build(): com.giyeok.bibix.daemon.BibixDaemonApiProto.BibixExpr.Glob = _builder.build()

      /**
       * An uninstantiable, behaviorless type to represent the field in
       * generics.
       */
      @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
      public class PatternsProxy private constructor() : com.google.protobuf.kotlin.DslProxy()
      /**
       * <code>repeated string patterns = 1;</code>
       * @return A list containing the patterns.
       */
      public val patterns: com.google.protobuf.kotlin.DslList<kotlin.String, PatternsProxy>
        @kotlin.jvm.JvmSynthetic
        get() = com.google.protobuf.kotlin.DslList(
          _builder.getPatternsList()
        )
      /**
       * <code>repeated string patterns = 1;</code>
       * @param value The patterns to add.
       */
      @kotlin.jvm.JvmSynthetic
      @kotlin.jvm.JvmName("addPatterns")
      public fun com.google.protobuf.kotlin.DslList<kotlin.String, PatternsProxy>.add(value: kotlin.String) {
        _builder.addPatterns(value)
      }
      /**
       * <code>repeated string patterns = 1;</code>
       * @param value The patterns to add.
       */
      @kotlin.jvm.JvmSynthetic
      @kotlin.jvm.JvmName("plusAssignPatterns")
      @Suppress("NOTHING_TO_INLINE")
      public inline operator fun com.google.protobuf.kotlin.DslList<kotlin.String, PatternsProxy>.plusAssign(value: kotlin.String) {
        add(value)
      }
      /**
       * <code>repeated string patterns = 1;</code>
       * @param values The patterns to add.
       */
      @kotlin.jvm.JvmSynthetic
      @kotlin.jvm.JvmName("addAllPatterns")
      public fun com.google.protobuf.kotlin.DslList<kotlin.String, PatternsProxy>.addAll(values: kotlin.collections.Iterable<kotlin.String>) {
        _builder.addAllPatterns(values)
      }
      /**
       * <code>repeated string patterns = 1;</code>
       * @param values The patterns to add.
       */
      @kotlin.jvm.JvmSynthetic
      @kotlin.jvm.JvmName("plusAssignAllPatterns")
      @Suppress("NOTHING_TO_INLINE")
      public inline operator fun com.google.protobuf.kotlin.DslList<kotlin.String, PatternsProxy>.plusAssign(values: kotlin.collections.Iterable<kotlin.String>) {
        addAll(values)
      }
      /**
       * <code>repeated string patterns = 1;</code>
       * @param index The index to set the value at.
       * @param value The patterns to set.
       */
      @kotlin.jvm.JvmSynthetic
      @kotlin.jvm.JvmName("setPatterns")
      public operator fun com.google.protobuf.kotlin.DslList<kotlin.String, PatternsProxy>.set(index: kotlin.Int, value: kotlin.String) {
        _builder.setPatterns(index, value)
      }/**
       * <code>repeated string patterns = 1;</code>
       */
      @kotlin.jvm.JvmSynthetic
      @kotlin.jvm.JvmName("clearPatterns")
      public fun com.google.protobuf.kotlin.DslList<kotlin.String, PatternsProxy>.clear() {
        _builder.clearPatterns()
      }
      /**
       * An uninstantiable, behaviorless type to represent the field in
       * generics.
       */
      @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
      public class FilesProxy private constructor() : com.google.protobuf.kotlin.DslProxy()
      /**
       * <code>repeated string files = 2;</code>
       * @return A list containing the files.
       */
      public val files: com.google.protobuf.kotlin.DslList<kotlin.String, FilesProxy>
        @kotlin.jvm.JvmSynthetic
        get() = com.google.protobuf.kotlin.DslList(
          _builder.getFilesList()
        )
      /**
       * <code>repeated string files = 2;</code>
       * @param value The files to add.
       */
      @kotlin.jvm.JvmSynthetic
      @kotlin.jvm.JvmName("addFiles")
      public fun com.google.protobuf.kotlin.DslList<kotlin.String, FilesProxy>.add(value: kotlin.String) {
        _builder.addFiles(value)
      }
      /**
       * <code>repeated string files = 2;</code>
       * @param value The files to add.
       */
      @kotlin.jvm.JvmSynthetic
      @kotlin.jvm.JvmName("plusAssignFiles")
      @Suppress("NOTHING_TO_INLINE")
      public inline operator fun com.google.protobuf.kotlin.DslList<kotlin.String, FilesProxy>.plusAssign(value: kotlin.String) {
        add(value)
      }
      /**
       * <code>repeated string files = 2;</code>
       * @param values The files to add.
       */
      @kotlin.jvm.JvmSynthetic
      @kotlin.jvm.JvmName("addAllFiles")
      public fun com.google.protobuf.kotlin.DslList<kotlin.String, FilesProxy>.addAll(values: kotlin.collections.Iterable<kotlin.String>) {
        _builder.addAllFiles(values)
      }
      /**
       * <code>repeated string files = 2;</code>
       * @param values The files to add.
       */
      @kotlin.jvm.JvmSynthetic
      @kotlin.jvm.JvmName("plusAssignAllFiles")
      @Suppress("NOTHING_TO_INLINE")
      public inline operator fun com.google.protobuf.kotlin.DslList<kotlin.String, FilesProxy>.plusAssign(values: kotlin.collections.Iterable<kotlin.String>) {
        addAll(values)
      }
      /**
       * <code>repeated string files = 2;</code>
       * @param index The index to set the value at.
       * @param value The files to set.
       */
      @kotlin.jvm.JvmSynthetic
      @kotlin.jvm.JvmName("setFiles")
      public operator fun com.google.protobuf.kotlin.DslList<kotlin.String, FilesProxy>.set(index: kotlin.Int, value: kotlin.String) {
        _builder.setFiles(index, value)
      }/**
       * <code>repeated string files = 2;</code>
       */
      @kotlin.jvm.JvmSynthetic
      @kotlin.jvm.JvmName("clearFiles")
      public fun com.google.protobuf.kotlin.DslList<kotlin.String, FilesProxy>.clear() {
        _builder.clearFiles()
      }}
  }
  @kotlin.jvm.JvmSynthetic
  public inline fun mavenDep(block: com.giyeok.bibix.daemon.BibixExprKt.MavenDepKt.Dsl.() -> kotlin.Unit): com.giyeok.bibix.daemon.BibixDaemonApiProto.BibixExpr.MavenDep =
    com.giyeok.bibix.daemon.BibixExprKt.MavenDepKt.Dsl._create(com.giyeok.bibix.daemon.BibixDaemonApiProto.BibixExpr.MavenDep.newBuilder()).apply { block() }._build()
  public object MavenDepKt {
    @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
    @com.google.protobuf.kotlin.ProtoDslMarker
    public class Dsl private constructor(
      private val _builder: com.giyeok.bibix.daemon.BibixDaemonApiProto.BibixExpr.MavenDep.Builder
    ) {
      public companion object {
        @kotlin.jvm.JvmSynthetic
        @kotlin.PublishedApi
        internal fun _create(builder: com.giyeok.bibix.daemon.BibixDaemonApiProto.BibixExpr.MavenDep.Builder): Dsl = Dsl(builder)
      }

      @kotlin.jvm.JvmSynthetic
      @kotlin.PublishedApi
      internal fun _build(): com.giyeok.bibix.daemon.BibixDaemonApiProto.BibixExpr.MavenDep = _builder.build()

      /**
       * <code>string group_id = 1;</code>
       */
      public var groupId: kotlin.String
        @JvmName("getGroupId")
        get() = _builder.getGroupId()
        @JvmName("setGroupId")
        set(value) {
          _builder.setGroupId(value)
        }
      /**
       * <code>string group_id = 1;</code>
       */
      public fun clearGroupId() {
        _builder.clearGroupId()
      }

      /**
       * <code>string artifact_id = 2;</code>
       */
      public var artifactId: kotlin.String
        @JvmName("getArtifactId")
        get() = _builder.getArtifactId()
        @JvmName("setArtifactId")
        set(value) {
          _builder.setArtifactId(value)
        }
      /**
       * <code>string artifact_id = 2;</code>
       */
      public fun clearArtifactId() {
        _builder.clearArtifactId()
      }

      /**
       * <code>string version = 3;</code>
       */
      public var version: kotlin.String
        @JvmName("getVersion")
        get() = _builder.getVersion()
        @JvmName("setVersion")
        set(value) {
          _builder.setVersion(value)
        }
      /**
       * <code>string version = 3;</code>
       */
      public fun clearVersion() {
        _builder.clearVersion()
      }
    }
  }
}
@kotlin.jvm.JvmSynthetic
public inline fun com.giyeok.bibix.daemon.BibixDaemonApiProto.BibixExpr.copy(block: com.giyeok.bibix.daemon.BibixExprKt.Dsl.() -> kotlin.Unit): com.giyeok.bibix.daemon.BibixDaemonApiProto.BibixExpr =
  com.giyeok.bibix.daemon.BibixExprKt.Dsl._create(this.toBuilder()).apply { block() }._build()
@kotlin.jvm.JvmSynthetic
public inline fun com.giyeok.bibix.daemon.BibixDaemonApiProto.BibixExpr.SeqExpr.copy(block: com.giyeok.bibix.daemon.BibixExprKt.SeqExprKt.Dsl.() -> kotlin.Unit): com.giyeok.bibix.daemon.BibixDaemonApiProto.BibixExpr.SeqExpr =
  com.giyeok.bibix.daemon.BibixExprKt.SeqExprKt.Dsl._create(this.toBuilder()).apply { block() }._build()
@kotlin.jvm.JvmSynthetic
public inline fun com.giyeok.bibix.daemon.BibixDaemonApiProto.BibixExpr.Glob.copy(block: com.giyeok.bibix.daemon.BibixExprKt.GlobKt.Dsl.() -> kotlin.Unit): com.giyeok.bibix.daemon.BibixDaemonApiProto.BibixExpr.Glob =
  com.giyeok.bibix.daemon.BibixExprKt.GlobKt.Dsl._create(this.toBuilder()).apply { block() }._build()
@kotlin.jvm.JvmSynthetic
public inline fun com.giyeok.bibix.daemon.BibixDaemonApiProto.BibixExpr.MavenDep.copy(block: com.giyeok.bibix.daemon.BibixExprKt.MavenDepKt.Dsl.() -> kotlin.Unit): com.giyeok.bibix.daemon.BibixDaemonApiProto.BibixExpr.MavenDep =
  com.giyeok.bibix.daemon.BibixExprKt.MavenDepKt.Dsl._create(this.toBuilder()).apply { block() }._build()