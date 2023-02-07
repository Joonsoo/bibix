package com.giyeok.bibix.intellij.service

import com.giyeok.bibix.intellij.contentRoot
import com.google.common.jimfs.Jimfs
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

class ContentRootsTests {
  @Test
  fun test() {
    val fs = Jimfs.newFileSystem()

    fs.getPath("/testproject/src/main/kotlin/com/giyeok/bibix").createDirectories()
    val file1 = fs.getPath("/testproject/src/main/kotlin/com/giyeok/bibix/File1.kt")
    val file2 = fs.getPath("/testproject/src/main/kotlin/com/giyeok/bibix/File2.kt")

    file1.writeText(
      """
      package com.giyeok.bibix
    """.trimIndent()
    )
    file2.writeText(
      """
      package com.giyeok.bibix
    """.trimIndent()
    )

    val root = ProjectStructureExtractor.getContentRoots(setOf(file1, file2))
    assertThat(root).containsExactly(contentRoot {
      this.contentRootName = "Sources"
      this.contentRootType = "src"
      this.contentRootPath = "/testproject/src/main/kotlin"
    })
  }
}
