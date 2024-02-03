package com.giyeok.bibix.plugins.prelude

import com.giyeok.bibix.base.*
import com.giyeok.bibix.frontend.BlockingBuildGraphRunner
import com.giyeok.bibix.graph.BibixName
import com.giyeok.bibix.graph.BibixProjectLocation
import com.giyeok.bibix.graph.runner.BuildGraphRunner
import com.giyeok.bibix.graph.runner.BuildTaskResult
import com.giyeok.bibix.graph.runner.ClassPkgRunner
import com.giyeok.bibix.graph.runner.EvalTarget
import com.giyeok.bibix.interpreter.testInterpreter
import com.giyeok.bibix.repo.BibixRepo
import com.google.common.jimfs.Jimfs
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.codehaus.plexus.classworlds.ClassWorld
import org.junit.jupiter.api.Test
import kotlin.io.path.createDirectories
import kotlin.io.path.createDirectory
import kotlin.io.path.writeText

class GlobTest {
  @Test
  fun testGlobFunction(): Unit = runBlocking {
    val fs = Jimfs.newFileSystem()

    fs.getPath("/projectRoot").createDirectory()

    val script = """
      xx = glob("subdir")
      yy = glob("subdir/**/*.*")
    """.trimIndent()
    fs.getPath("/projectRoot/build.bbx").writeText(script)

    fs.getPath("/projectRoot/subdir/1/2/3/4").createDirectories()
    fs.getPath("/projectRoot/subdir/1/2/3/4/5.txt").writeText("Hello")
    fs.getPath("/projectRoot/subdir2/5/6/7/8").createDirectories()
    fs.getPath("/projectRoot/subdir2/5/6/7/8/9.txt").writeText("World")

    val interpreter = testInterpreter(fs, "/projectRoot", mapOf(), preludePlugin = preludePlugin)

    assertThat(interpreter.userBuildRequest("xx"))
      .isEqualTo(SetValue(FileValue(fs.getPath("/projectRoot/subdir"))))
    assertThat(interpreter.userBuildRequest("yy"))
      .isEqualTo(SetValue(FileValue(fs.getPath("/projectRoot/subdir/1/2/3/4/5.txt"))))
  }

  @Test
  fun testGlobStarStart(): Unit = runBlocking {
    val fs = Jimfs.newFileSystem()

    fs.getPath("/projectRoot").createDirectory()

    val script = """
      xx = glob("**.java")
    """.trimIndent()

    fs.getPath("/projectRoot/build.bbx").writeText(script)

    fs.getPath("/projectRoot/A.java").writeText("Hello")
    fs.getPath("/projectRoot/subdir1").createDirectory()
    fs.getPath("/projectRoot/subdir1/B.java").writeText("Hello")

    val runner = BlockingBuildGraphRunner(
      BuildGraphRunner.create(
        mainProjectLocation = BibixProjectLocation(fs.getPath("/projectRoot")),
        preludePlugin = preludePlugin,
        preloadedPlugins = mapOf(),
        buildEnv = BuildEnv(OS.Linux("", ""), Architecture.X86_64),
        fileSystem = fs,
        repo = BibixRepo.load(fs.getPath("/projectRoot")),
        classPkgRunner = ClassPkgRunner(ClassWorld())
      )
    )
    val result = runner.runToFinal(EvalTarget(1, 0, BibixName("xx")))
    check(result is BuildTaskResult.ResultWithValue)
    val resultValue = result.value
    check(resultValue is SetValue)
    assertThat(resultValue.values).containsExactly(
      FileValue(fs.getPath("/projectRoot/A.java")),
      FileValue(fs.getPath("/projectRoot/subdir1/B.java"))
    )
  }
}
