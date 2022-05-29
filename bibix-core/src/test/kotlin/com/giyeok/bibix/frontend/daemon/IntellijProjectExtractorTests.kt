package com.giyeok.bibix.frontend.daemon

import com.giyeok.bibix.frontend.BuildFrontend
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.io.File

class IntellijProjectExtractorTests {
  @Test
  fun test() {
    val path = "/home/joonsoo/Documents/workspace/bibix"
    val extractor = IntellijProjectExtractor(BuildFrontend(File(path)))
    val projectStructure = runBlocking {
      extractor.extractIntellijProjectStructure()
    }
    println(projectStructure)
  }
}
