// Generated by the protocol buffer compiler. DO NOT EDIT!
// source: api.proto

// Generated files should ignore deprecation warnings
@file:Suppress("DEPRECATION")
package com.giyeok.bibix.intellij;

@kotlin.jvm.JvmName("-initializesdkInfo")
public inline fun sdkInfo(block: com.giyeok.bibix.intellij.SdkInfoKt.Dsl.() -> kotlin.Unit): com.giyeok.bibix.intellij.BibixIntellijProto.SdkInfo =
  com.giyeok.bibix.intellij.SdkInfoKt.Dsl._create(com.giyeok.bibix.intellij.BibixIntellijProto.SdkInfo.newBuilder()).apply { block() }._build()
/**
 * Protobuf type `com.giyeok.bibix.intellij.SdkInfo`
 */
public object SdkInfoKt {
  @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
  @com.google.protobuf.kotlin.ProtoDslMarker
  public class Dsl private constructor(
    private val _builder: com.giyeok.bibix.intellij.BibixIntellijProto.SdkInfo.Builder
  ) {
    public companion object {
      @kotlin.jvm.JvmSynthetic
      @kotlin.PublishedApi
      internal fun _create(builder: com.giyeok.bibix.intellij.BibixIntellijProto.SdkInfo.Builder): Dsl = Dsl(builder)
    }

    @kotlin.jvm.JvmSynthetic
    @kotlin.PublishedApi
    internal fun _build(): com.giyeok.bibix.intellij.BibixIntellijProto.SdkInfo = _builder.build()

    /**
     * An uninstantiable, behaviorless type to represent the field in
     * generics.
     */
    @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
    public class KtjvmSdksProxy private constructor() : com.google.protobuf.kotlin.DslProxy()
    /**
     * `repeated .com.giyeok.bibix.intellij.KotlinJvmSdk ktjvm_sdks = 1;`
     */
     public val ktjvmSdks: com.google.protobuf.kotlin.DslList<com.giyeok.bibix.intellij.BibixIntellijProto.KotlinJvmSdk, KtjvmSdksProxy>
      @kotlin.jvm.JvmSynthetic
      get() = com.google.protobuf.kotlin.DslList(
        _builder.getKtjvmSdksList()
      )
    /**
     * `repeated .com.giyeok.bibix.intellij.KotlinJvmSdk ktjvm_sdks = 1;`
     * @param value The ktjvmSdks to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("addKtjvmSdks")
    public fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.intellij.BibixIntellijProto.KotlinJvmSdk, KtjvmSdksProxy>.add(value: com.giyeok.bibix.intellij.BibixIntellijProto.KotlinJvmSdk) {
      _builder.addKtjvmSdks(value)
    }
    /**
     * `repeated .com.giyeok.bibix.intellij.KotlinJvmSdk ktjvm_sdks = 1;`
     * @param value The ktjvmSdks to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("plusAssignKtjvmSdks")
    @Suppress("NOTHING_TO_INLINE")
    public inline operator fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.intellij.BibixIntellijProto.KotlinJvmSdk, KtjvmSdksProxy>.plusAssign(value: com.giyeok.bibix.intellij.BibixIntellijProto.KotlinJvmSdk) {
      add(value)
    }
    /**
     * `repeated .com.giyeok.bibix.intellij.KotlinJvmSdk ktjvm_sdks = 1;`
     * @param values The ktjvmSdks to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("addAllKtjvmSdks")
    public fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.intellij.BibixIntellijProto.KotlinJvmSdk, KtjvmSdksProxy>.addAll(values: kotlin.collections.Iterable<com.giyeok.bibix.intellij.BibixIntellijProto.KotlinJvmSdk>) {
      _builder.addAllKtjvmSdks(values)
    }
    /**
     * `repeated .com.giyeok.bibix.intellij.KotlinJvmSdk ktjvm_sdks = 1;`
     * @param values The ktjvmSdks to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("plusAssignAllKtjvmSdks")
    @Suppress("NOTHING_TO_INLINE")
    public inline operator fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.intellij.BibixIntellijProto.KotlinJvmSdk, KtjvmSdksProxy>.plusAssign(values: kotlin.collections.Iterable<com.giyeok.bibix.intellij.BibixIntellijProto.KotlinJvmSdk>) {
      addAll(values)
    }
    /**
     * `repeated .com.giyeok.bibix.intellij.KotlinJvmSdk ktjvm_sdks = 1;`
     * @param index The index to set the value at.
     * @param value The ktjvmSdks to set.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("setKtjvmSdks")
    public operator fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.intellij.BibixIntellijProto.KotlinJvmSdk, KtjvmSdksProxy>.set(index: kotlin.Int, value: com.giyeok.bibix.intellij.BibixIntellijProto.KotlinJvmSdk) {
      _builder.setKtjvmSdks(index, value)
    }
    /**
     * `repeated .com.giyeok.bibix.intellij.KotlinJvmSdk ktjvm_sdks = 1;`
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("clearKtjvmSdks")
    public fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.intellij.BibixIntellijProto.KotlinJvmSdk, KtjvmSdksProxy>.clear() {
      _builder.clearKtjvmSdks()
    }


    /**
     * An uninstantiable, behaviorless type to represent the field in
     * generics.
     */
    @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
    public class ScalaSdksProxy private constructor() : com.google.protobuf.kotlin.DslProxy()
    /**
     * `repeated .com.giyeok.bibix.intellij.ScalaSdk scala_sdks = 2;`
     */
     public val scalaSdks: com.google.protobuf.kotlin.DslList<com.giyeok.bibix.intellij.BibixIntellijProto.ScalaSdk, ScalaSdksProxy>
      @kotlin.jvm.JvmSynthetic
      get() = com.google.protobuf.kotlin.DslList(
        _builder.getScalaSdksList()
      )
    /**
     * `repeated .com.giyeok.bibix.intellij.ScalaSdk scala_sdks = 2;`
     * @param value The scalaSdks to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("addScalaSdks")
    public fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.intellij.BibixIntellijProto.ScalaSdk, ScalaSdksProxy>.add(value: com.giyeok.bibix.intellij.BibixIntellijProto.ScalaSdk) {
      _builder.addScalaSdks(value)
    }
    /**
     * `repeated .com.giyeok.bibix.intellij.ScalaSdk scala_sdks = 2;`
     * @param value The scalaSdks to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("plusAssignScalaSdks")
    @Suppress("NOTHING_TO_INLINE")
    public inline operator fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.intellij.BibixIntellijProto.ScalaSdk, ScalaSdksProxy>.plusAssign(value: com.giyeok.bibix.intellij.BibixIntellijProto.ScalaSdk) {
      add(value)
    }
    /**
     * `repeated .com.giyeok.bibix.intellij.ScalaSdk scala_sdks = 2;`
     * @param values The scalaSdks to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("addAllScalaSdks")
    public fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.intellij.BibixIntellijProto.ScalaSdk, ScalaSdksProxy>.addAll(values: kotlin.collections.Iterable<com.giyeok.bibix.intellij.BibixIntellijProto.ScalaSdk>) {
      _builder.addAllScalaSdks(values)
    }
    /**
     * `repeated .com.giyeok.bibix.intellij.ScalaSdk scala_sdks = 2;`
     * @param values The scalaSdks to add.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("plusAssignAllScalaSdks")
    @Suppress("NOTHING_TO_INLINE")
    public inline operator fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.intellij.BibixIntellijProto.ScalaSdk, ScalaSdksProxy>.plusAssign(values: kotlin.collections.Iterable<com.giyeok.bibix.intellij.BibixIntellijProto.ScalaSdk>) {
      addAll(values)
    }
    /**
     * `repeated .com.giyeok.bibix.intellij.ScalaSdk scala_sdks = 2;`
     * @param index The index to set the value at.
     * @param value The scalaSdks to set.
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("setScalaSdks")
    public operator fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.intellij.BibixIntellijProto.ScalaSdk, ScalaSdksProxy>.set(index: kotlin.Int, value: com.giyeok.bibix.intellij.BibixIntellijProto.ScalaSdk) {
      _builder.setScalaSdks(index, value)
    }
    /**
     * `repeated .com.giyeok.bibix.intellij.ScalaSdk scala_sdks = 2;`
     */
    @kotlin.jvm.JvmSynthetic
    @kotlin.jvm.JvmName("clearScalaSdks")
    public fun com.google.protobuf.kotlin.DslList<com.giyeok.bibix.intellij.BibixIntellijProto.ScalaSdk, ScalaSdksProxy>.clear() {
      _builder.clearScalaSdks()
    }

  }
}
@kotlin.jvm.JvmSynthetic
@com.google.errorprone.annotations.CheckReturnValue
public inline fun com.giyeok.bibix.intellij.BibixIntellijProto.SdkInfo.copy(block: com.giyeok.bibix.intellij.SdkInfoKt.Dsl.() -> kotlin.Unit): com.giyeok.bibix.intellij.BibixIntellijProto.SdkInfo =
  com.giyeok.bibix.intellij.SdkInfoKt.Dsl._create(this.toBuilder()).apply { block() }._build()
