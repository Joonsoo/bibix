//Generated by the protocol buffer compiler. DO NOT EDIT!
// source: api.proto

package com.giyeok.bibix.daemon;

@kotlin.jvm.JvmSynthetic
public inline fun intellijLibraryNode(block: com.giyeok.bibix.daemon.IntellijLibraryNodeKt.Dsl.() -> kotlin.Unit): com.giyeok.bibix.daemon.BibixDaemonApiProto.IntellijLibraryNode =
  com.giyeok.bibix.daemon.IntellijLibraryNodeKt.Dsl._create(com.giyeok.bibix.daemon.BibixDaemonApiProto.IntellijLibraryNode.newBuilder()).apply { block() }._build()
public object IntellijLibraryNodeKt {
  @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
  @com.google.protobuf.kotlin.ProtoDslMarker
  public class Dsl private constructor(
    private val _builder: com.giyeok.bibix.daemon.BibixDaemonApiProto.IntellijLibraryNode.Builder
  ) {
    public companion object {
      @kotlin.jvm.JvmSynthetic
      @kotlin.PublishedApi
      internal fun _create(builder: com.giyeok.bibix.daemon.BibixDaemonApiProto.IntellijLibraryNode.Builder): Dsl = Dsl(builder)
    }

    @kotlin.jvm.JvmSynthetic
    @kotlin.PublishedApi
    internal fun _build(): com.giyeok.bibix.daemon.BibixDaemonApiProto.IntellijLibraryNode = _builder.build()

    /**
     * <code>string library_name = 1;</code>
     */
    public var libraryName: kotlin.String
      @JvmName("getLibraryName")
      get() = _builder.getLibraryName()
      @JvmName("setLibraryName")
      set(value) {
        _builder.setLibraryName(value)
      }
    /**
     * <code>string library_name = 1;</code>
     */
    public fun clearLibraryName() {
      _builder.clearLibraryName()
    }

    /**
     * An uninstantiable, behaviorless type to represent the field in
     * generics.
     */
    @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
    public class ClasspathProxy private constructor() : com.google.protobuf.kotlin.DslProxy()
    /**
     * <code>repeated string classpath = 2;</code>
     * @return A list containing the classpath.
     */
    public val classpath: com.google.protobuf.kotlin.DslList<kotlin.String, ClasspathProxy>
      @kotlin.jvm.JvmSynthetic
      get() = com.google.protobuf.kotlin.DslList(
        _builder.getClasspathList()
      )
    /**
     * <code>repeated string classpath = 2;</code>
     * @param value The classpath to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("addClasspath")
    public fun com.google.protobuf.kotlin.DslList<kotlin.String, ClasspathProxy>.add(value: kotlin.String) {
      _builder.addClasspath(value)
    }
    /**
     * <code>repeated string classpath = 2;</code>
     * @param value The classpath to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("plusAssignClasspath")
    @Suppress("NOTHING_TO_INLINE")
    public inline operator fun com.google.protobuf.kotlin.DslList<kotlin.String, ClasspathProxy>.plusAssign(value: kotlin.String) {
      add(value)
    }
    /**
     * <code>repeated string classpath = 2;</code>
     * @param values The classpath to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("addAllClasspath")
    public fun com.google.protobuf.kotlin.DslList<kotlin.String, ClasspathProxy>.addAll(values: kotlin.collections.Iterable<kotlin.String>) {
      _builder.addAllClasspath(values)
    }
    /**
     * <code>repeated string classpath = 2;</code>
     * @param values The classpath to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("plusAssignAllClasspath")
    @Suppress("NOTHING_TO_INLINE")
    public inline operator fun com.google.protobuf.kotlin.DslList<kotlin.String, ClasspathProxy>.plusAssign(values: kotlin.collections.Iterable<kotlin.String>) {
      addAll(values)
    }
    /**
     * <code>repeated string classpath = 2;</code>
     * @param index The index to set the value at.
     * @param value The classpath to set.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("setClasspath")
    public operator fun com.google.protobuf.kotlin.DslList<kotlin.String, ClasspathProxy>.set(index: kotlin.Int, value: kotlin.String) {
      _builder.setClasspath(index, value)
    }/**
     * <code>repeated string classpath = 2;</code>
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("clearClasspath")
    public fun com.google.protobuf.kotlin.DslList<kotlin.String, ClasspathProxy>.clear() {
      _builder.clearClasspath()
    }
    /**
     * An uninstantiable, behaviorless type to represent the field in
     * generics.
     */
    @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
    public class SourceProxy private constructor() : com.google.protobuf.kotlin.DslProxy()
    /**
     * <code>repeated string source = 3;</code>
     * @return A list containing the source.
     */
    public val source: com.google.protobuf.kotlin.DslList<kotlin.String, SourceProxy>
      @kotlin.jvm.JvmSynthetic
      get() = com.google.protobuf.kotlin.DslList(
        _builder.getSourceList()
      )
    /**
     * <code>repeated string source = 3;</code>
     * @param value The source to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("addSource")
    public fun com.google.protobuf.kotlin.DslList<kotlin.String, SourceProxy>.add(value: kotlin.String) {
      _builder.addSource(value)
    }
    /**
     * <code>repeated string source = 3;</code>
     * @param value The source to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("plusAssignSource")
    @Suppress("NOTHING_TO_INLINE")
    public inline operator fun com.google.protobuf.kotlin.DslList<kotlin.String, SourceProxy>.plusAssign(value: kotlin.String) {
      add(value)
    }
    /**
     * <code>repeated string source = 3;</code>
     * @param values The source to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("addAllSource")
    public fun com.google.protobuf.kotlin.DslList<kotlin.String, SourceProxy>.addAll(values: kotlin.collections.Iterable<kotlin.String>) {
      _builder.addAllSource(values)
    }
    /**
     * <code>repeated string source = 3;</code>
     * @param values The source to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("plusAssignAllSource")
    @Suppress("NOTHING_TO_INLINE")
    public inline operator fun com.google.protobuf.kotlin.DslList<kotlin.String, SourceProxy>.plusAssign(values: kotlin.collections.Iterable<kotlin.String>) {
      addAll(values)
    }
    /**
     * <code>repeated string source = 3;</code>
     * @param index The index to set the value at.
     * @param value The source to set.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("setSource")
    public operator fun com.google.protobuf.kotlin.DslList<kotlin.String, SourceProxy>.set(index: kotlin.Int, value: kotlin.String) {
      _builder.setSource(index, value)
    }/**
     * <code>repeated string source = 3;</code>
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("clearSource")
    public fun com.google.protobuf.kotlin.DslList<kotlin.String, SourceProxy>.clear() {
      _builder.clearSource()
    }
    /**
     * <code>.com.giyeok.bibix.daemon.IntellijLibraryNode.IntellijMavenLibraryNode maven_library = 4;</code>
     */
    public var mavenLibrary: com.giyeok.bibix.daemon.BibixDaemonApiProto.IntellijLibraryNode.IntellijMavenLibraryNode
      @JvmName("getMavenLibrary")
      get() = _builder.getMavenLibrary()
      @JvmName("setMavenLibrary")
      set(value) {
        _builder.setMavenLibrary(value)
      }
    /**
     * <code>.com.giyeok.bibix.daemon.IntellijLibraryNode.IntellijMavenLibraryNode maven_library = 4;</code>
     */
    public fun clearMavenLibrary() {
      _builder.clearMavenLibrary()
    }
    /**
     * <code>.com.giyeok.bibix.daemon.IntellijLibraryNode.IntellijMavenLibraryNode maven_library = 4;</code>
     * @return Whether the mavenLibrary field is set.
     */
    public fun hasMavenLibrary(): kotlin.Boolean {
      return _builder.hasMavenLibrary()
    }

    /**
     * <code>.com.giyeok.bibix.daemon.IntellijLibraryNode.IntellijLocalLibraryNode local_library = 5;</code>
     */
    public var localLibrary: com.giyeok.bibix.daemon.BibixDaemonApiProto.IntellijLibraryNode.IntellijLocalLibraryNode
      @JvmName("getLocalLibrary")
      get() = _builder.getLocalLibrary()
      @JvmName("setLocalLibrary")
      set(value) {
        _builder.setLocalLibrary(value)
      }
    /**
     * <code>.com.giyeok.bibix.daemon.IntellijLibraryNode.IntellijLocalLibraryNode local_library = 5;</code>
     */
    public fun clearLocalLibrary() {
      _builder.clearLocalLibrary()
    }
    /**
     * <code>.com.giyeok.bibix.daemon.IntellijLibraryNode.IntellijLocalLibraryNode local_library = 5;</code>
     * @return Whether the localLibrary field is set.
     */
    public fun hasLocalLibrary(): kotlin.Boolean {
      return _builder.hasLocalLibrary()
    }

    /**
     * <code>.com.giyeok.bibix.daemon.IntellijLibraryNode.IntellijScalaSdkLibraryNode scala_sdk_library = 6;</code>
     */
    public var scalaSdkLibrary: com.giyeok.bibix.daemon.BibixDaemonApiProto.IntellijLibraryNode.IntellijScalaSdkLibraryNode
      @JvmName("getScalaSdkLibrary")
      get() = _builder.getScalaSdkLibrary()
      @JvmName("setScalaSdkLibrary")
      set(value) {
        _builder.setScalaSdkLibrary(value)
      }
    /**
     * <code>.com.giyeok.bibix.daemon.IntellijLibraryNode.IntellijScalaSdkLibraryNode scala_sdk_library = 6;</code>
     */
    public fun clearScalaSdkLibrary() {
      _builder.clearScalaSdkLibrary()
    }
    /**
     * <code>.com.giyeok.bibix.daemon.IntellijLibraryNode.IntellijScalaSdkLibraryNode scala_sdk_library = 6;</code>
     * @return Whether the scalaSdkLibrary field is set.
     */
    public fun hasScalaSdkLibrary(): kotlin.Boolean {
      return _builder.hasScalaSdkLibrary()
    }
    public val libraryCase: com.giyeok.bibix.daemon.BibixDaemonApiProto.IntellijLibraryNode.LibraryCase
      @JvmName("getLibraryCase")
      get() = _builder.getLibraryCase()

    public fun clearLibrary() {
      _builder.clearLibrary()
    }
  }
  @kotlin.jvm.JvmSynthetic
  public inline fun intellijMavenLibraryNode(block: com.giyeok.bibix.daemon.IntellijLibraryNodeKt.IntellijMavenLibraryNodeKt.Dsl.() -> kotlin.Unit): com.giyeok.bibix.daemon.BibixDaemonApiProto.IntellijLibraryNode.IntellijMavenLibraryNode =
    com.giyeok.bibix.daemon.IntellijLibraryNodeKt.IntellijMavenLibraryNodeKt.Dsl._create(com.giyeok.bibix.daemon.BibixDaemonApiProto.IntellijLibraryNode.IntellijMavenLibraryNode.newBuilder()).apply { block() }._build()
  public object IntellijMavenLibraryNodeKt {
    @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
    @com.google.protobuf.kotlin.ProtoDslMarker
    public class Dsl private constructor(
      private val _builder: com.giyeok.bibix.daemon.BibixDaemonApiProto.IntellijLibraryNode.IntellijMavenLibraryNode.Builder
    ) {
      public companion object {
        @kotlin.jvm.JvmSynthetic
        @kotlin.PublishedApi
        internal fun _create(builder: com.giyeok.bibix.daemon.BibixDaemonApiProto.IntellijLibraryNode.IntellijMavenLibraryNode.Builder): Dsl = Dsl(builder)
      }

      @kotlin.jvm.JvmSynthetic
      @kotlin.PublishedApi
      internal fun _build(): com.giyeok.bibix.daemon.BibixDaemonApiProto.IntellijLibraryNode.IntellijMavenLibraryNode = _builder.build()

      /**
       * <code>string group = 1;</code>
       */
      public var group: kotlin.String
        @JvmName("getGroup")
        get() = _builder.getGroup()
        @JvmName("setGroup")
        set(value) {
          _builder.setGroup(value)
        }
      /**
       * <code>string group = 1;</code>
       */
      public fun clearGroup() {
        _builder.clearGroup()
      }

      /**
       * <code>string artifact = 2;</code>
       */
      public var artifact: kotlin.String
        @JvmName("getArtifact")
        get() = _builder.getArtifact()
        @JvmName("setArtifact")
        set(value) {
          _builder.setArtifact(value)
        }
      /**
       * <code>string artifact = 2;</code>
       */
      public fun clearArtifact() {
        _builder.clearArtifact()
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
  @kotlin.jvm.JvmSynthetic
  public inline fun intellijLocalLibraryNode(block: com.giyeok.bibix.daemon.IntellijLibraryNodeKt.IntellijLocalLibraryNodeKt.Dsl.() -> kotlin.Unit): com.giyeok.bibix.daemon.BibixDaemonApiProto.IntellijLibraryNode.IntellijLocalLibraryNode =
    com.giyeok.bibix.daemon.IntellijLibraryNodeKt.IntellijLocalLibraryNodeKt.Dsl._create(com.giyeok.bibix.daemon.BibixDaemonApiProto.IntellijLibraryNode.IntellijLocalLibraryNode.newBuilder()).apply { block() }._build()
  public object IntellijLocalLibraryNodeKt {
    @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
    @com.google.protobuf.kotlin.ProtoDslMarker
    public class Dsl private constructor(
      private val _builder: com.giyeok.bibix.daemon.BibixDaemonApiProto.IntellijLibraryNode.IntellijLocalLibraryNode.Builder
    ) {
      public companion object {
        @kotlin.jvm.JvmSynthetic
        @kotlin.PublishedApi
        internal fun _create(builder: com.giyeok.bibix.daemon.BibixDaemonApiProto.IntellijLibraryNode.IntellijLocalLibraryNode.Builder): Dsl = Dsl(builder)
      }

      @kotlin.jvm.JvmSynthetic
      @kotlin.PublishedApi
      internal fun _build(): com.giyeok.bibix.daemon.BibixDaemonApiProto.IntellijLibraryNode.IntellijLocalLibraryNode = _builder.build()

      /**
       * <code>string path = 1;</code>
       */
      public var path: kotlin.String
        @JvmName("getPath")
        get() = _builder.getPath()
        @JvmName("setPath")
        set(value) {
          _builder.setPath(value)
        }
      /**
       * <code>string path = 1;</code>
       */
      public fun clearPath() {
        _builder.clearPath()
      }
    }
  }
  @kotlin.jvm.JvmSynthetic
  public inline fun intellijScalaSdkLibraryNode(block: com.giyeok.bibix.daemon.IntellijLibraryNodeKt.IntellijScalaSdkLibraryNodeKt.Dsl.() -> kotlin.Unit): com.giyeok.bibix.daemon.BibixDaemonApiProto.IntellijLibraryNode.IntellijScalaSdkLibraryNode =
    com.giyeok.bibix.daemon.IntellijLibraryNodeKt.IntellijScalaSdkLibraryNodeKt.Dsl._create(com.giyeok.bibix.daemon.BibixDaemonApiProto.IntellijLibraryNode.IntellijScalaSdkLibraryNode.newBuilder()).apply { block() }._build()
  public object IntellijScalaSdkLibraryNodeKt {
    @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
    @com.google.protobuf.kotlin.ProtoDslMarker
    public class Dsl private constructor(
      private val _builder: com.giyeok.bibix.daemon.BibixDaemonApiProto.IntellijLibraryNode.IntellijScalaSdkLibraryNode.Builder
    ) {
      public companion object {
        @kotlin.jvm.JvmSynthetic
        @kotlin.PublishedApi
        internal fun _create(builder: com.giyeok.bibix.daemon.BibixDaemonApiProto.IntellijLibraryNode.IntellijScalaSdkLibraryNode.Builder): Dsl = Dsl(builder)
      }

      @kotlin.jvm.JvmSynthetic
      @kotlin.PublishedApi
      internal fun _build(): com.giyeok.bibix.daemon.BibixDaemonApiProto.IntellijLibraryNode.IntellijScalaSdkLibraryNode = _builder.build()

      /**
       * <code>string scala_version = 1;</code>
       */
      public var scalaVersion: kotlin.String
        @JvmName("getScalaVersion")
        get() = _builder.getScalaVersion()
        @JvmName("setScalaVersion")
        set(value) {
          _builder.setScalaVersion(value)
        }
      /**
       * <code>string scala_version = 1;</code>
       */
      public fun clearScalaVersion() {
        _builder.clearScalaVersion()
      }
    }
  }
}
@kotlin.jvm.JvmSynthetic
public inline fun com.giyeok.bibix.daemon.BibixDaemonApiProto.IntellijLibraryNode.copy(block: com.giyeok.bibix.daemon.IntellijLibraryNodeKt.Dsl.() -> kotlin.Unit): com.giyeok.bibix.daemon.BibixDaemonApiProto.IntellijLibraryNode =
  com.giyeok.bibix.daemon.IntellijLibraryNodeKt.Dsl._create(this.toBuilder()).apply { block() }._build()
@kotlin.jvm.JvmSynthetic
public inline fun com.giyeok.bibix.daemon.BibixDaemonApiProto.IntellijLibraryNode.IntellijMavenLibraryNode.copy(block: com.giyeok.bibix.daemon.IntellijLibraryNodeKt.IntellijMavenLibraryNodeKt.Dsl.() -> kotlin.Unit): com.giyeok.bibix.daemon.BibixDaemonApiProto.IntellijLibraryNode.IntellijMavenLibraryNode =
  com.giyeok.bibix.daemon.IntellijLibraryNodeKt.IntellijMavenLibraryNodeKt.Dsl._create(this.toBuilder()).apply { block() }._build()
@kotlin.jvm.JvmSynthetic
public inline fun com.giyeok.bibix.daemon.BibixDaemonApiProto.IntellijLibraryNode.IntellijLocalLibraryNode.copy(block: com.giyeok.bibix.daemon.IntellijLibraryNodeKt.IntellijLocalLibraryNodeKt.Dsl.() -> kotlin.Unit): com.giyeok.bibix.daemon.BibixDaemonApiProto.IntellijLibraryNode.IntellijLocalLibraryNode =
  com.giyeok.bibix.daemon.IntellijLibraryNodeKt.IntellijLocalLibraryNodeKt.Dsl._create(this.toBuilder()).apply { block() }._build()
@kotlin.jvm.JvmSynthetic
public inline fun com.giyeok.bibix.daemon.BibixDaemonApiProto.IntellijLibraryNode.IntellijScalaSdkLibraryNode.copy(block: com.giyeok.bibix.daemon.IntellijLibraryNodeKt.IntellijScalaSdkLibraryNodeKt.Dsl.() -> kotlin.Unit): com.giyeok.bibix.daemon.BibixDaemonApiProto.IntellijLibraryNode.IntellijScalaSdkLibraryNode =
  com.giyeok.bibix.daemon.IntellijLibraryNodeKt.IntellijScalaSdkLibraryNodeKt.Dsl._create(this.toBuilder()).apply { block() }._build()