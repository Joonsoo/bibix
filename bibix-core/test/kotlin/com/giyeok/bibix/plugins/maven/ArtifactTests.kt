package com.giyeok.bibix.plugins.maven

import com.giyeok.bibix.base.Architecture
import com.giyeok.bibix.base.BuildEnv
import com.giyeok.bibix.base.DummyProgressLogger
import com.giyeok.bibix.base.OS
import com.giyeok.bibix.plugins.jvm.MavenDep
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.io.path.Path

class ArtifactTests {
  @Test
  fun test(): Unit = runBlocking {
    val artifact = Artifact.resolveArtifact(
      BuildEnv(OS.Linux("debian", "???"), Architecture.X86_64),
      DummyProgressLogger,
      Path("bbxbuild/shared/com.giyeok.bibix.plugins.maven"),
      "org.apache.maven.resolver",
      "maven-resolver-transport-http",
      "jar",
      "1.9.5",
      null,
      "compile",
      "jar",
      listOf(),
      setOf()
    )
  }

  @Test
  fun testFirebaseAdmin(): Unit = runBlocking {
    // TODO ${os.detected.classifier} 때문에 실행 안됨. 수정 필요
    // maven.artifact("com.google.firebase", "firebase-admin", "9.1.1")
    val artifact = Artifact.resolveArtifact(
      BuildEnv(OS.Linux("debian", "???"), Architecture.X86_64),
      DummyProgressLogger,
      Path("bbxbuild/shared/com.giyeok.bibix.plugins.maven"),
      "com.google.firebase",
      "firebase-admin",
      "jar",
      "9.1.1",
      null,
      "compile",
      "jar",
      listOf(),
      setOf()
    )
    println(artifact)
  }

  @Test
  fun testGrpcNettyShaded(): Unit = runBlocking {
    // maven.artifact("io.grpc", "grpc-netty-shaded", grpcVersion)
    val artifact = Artifact.resolveArtifact(
      BuildEnv(OS.Linux("debian", "???"), Architecture.X86_64),
      DummyProgressLogger,
      Path("bbxbuild/shared/com.giyeok.bibix.plugins.maven"),
      "io.grpc",
      "grpc-netty-shaded",
      "jar",
      "1.54.0",
      null,
      "compile",
      "jar",
      listOf(),
      setOf()
    )
    assert(artifact.runtimeDeps.any {
      it.origin == MavenDep("central", "io.perfmark", "perfmark-api", "0.25.0", "")
    })
    println(artifact)
  }

  @Test
  fun testLibGdx(): Unit = runBlocking {
    val artifact = Artifact.resolveArtifact(
      BuildEnv(OS.Linux("debian", "???"), Architecture.X86_64),
      DummyProgressLogger,
      Path("bbxbuild/shared/com.giyeok.bibix.plugins.maven"),
      "com.badlogicgames.gdx",
      "gdx-backend-lwjgl3",
      "jar",
      "1.12.1",
      null,
      "compile",
      "jar",
      listOf(),
      setOf()
    )
    println(artifact)
  }
}
