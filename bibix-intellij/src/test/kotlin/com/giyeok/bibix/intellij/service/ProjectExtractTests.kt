package com.giyeok.bibix.intellij.service

import com.giyeok.bibix.intellij.loadProjectReq
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class ProjectExtractTests {
  @Test
  fun test(): Unit = runBlocking {
    val impl = BibixIntellijServiceImpl()
    val project = impl.loadProject(loadProjectReq {
      this.projectRoot = "/home/joonsoo/Documents/workspace/jparser"
    })
    println(project)
  }
}
