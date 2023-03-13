package com.giyeok.bibix.intellij.service

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import java.io.StringReader

class SourceCodePackageNameReaderTest {
  @Test
  fun test() {
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
}
