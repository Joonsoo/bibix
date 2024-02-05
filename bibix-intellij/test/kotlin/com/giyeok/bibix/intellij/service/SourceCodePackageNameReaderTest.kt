package com.giyeok.bibix.intellij.service

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import java.io.StringReader
import kotlin.io.path.Path
import kotlin.io.path.bufferedReader

class SourceCodePackageNameReaderTest {
  @Test
  fun testBacktick() {
    val pkgName = SourceCodePackageNameReader.readPackageName(
      StringReader(
        """
      package com.giyeok.autodb.`type`.hello

      import com.giyeok.autodb.defs.{DataClassDef, DefsMap, EntityDef}
    """.trimIndent()
      )
    )
    assertThat(pkgName).containsExactly("com", "giyeok", "autodb", "type", "hello")
  }

  @Test
  fun testAnnotation() {
    val pkgName = SourceCodePackageNameReader.readPackageName(
      StringReader(
        """
          // Generated by the protocol buffer compiler. DO NOT EDIT!
          // source: dao.proto

          // Generated files should ignore deprecation warnings
          @file:Suppress("DEPRECATION")
          package com.giyeok.datclub3.proto;

          @kotlin.jvm.JvmName("-initializearticleAuthors")
        """.trimIndent()
      )
    )
    assertThat(pkgName).containsExactly("com", "giyeok", "datclub3", "proto")
  }

  @Test
  fun test() {
    val pkgName =
      SourceCodePackageNameReader.readPackageName(Path("/Users/joonsoo/Documents/workspace/autodb/test/generated/proto/kotlin/com/giyeok/datclub3/proto/ClubDataKt.kt").bufferedReader())
    println(pkgName)
  }
}