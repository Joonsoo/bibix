package com.giyeok.bibix.plugins.maven

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.io.path.Path

class ArtifactTests {
  @Test
  fun test(): Unit = runBlocking {
    val artifact = Artifact.resolveArtifact(
      Path("bbxbuild/shared/com.giyeok.bibix.plugins.maven"),
      "org.apache.maven.resolver",
      "maven-resolver-transport-http",
      "jar",
      "1.9.5",
      "compile",
      "jar",
      listOf(),
      setOf()
    )
  }
}
