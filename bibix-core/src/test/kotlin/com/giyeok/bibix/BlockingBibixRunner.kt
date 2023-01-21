package com.giyeok.bibix

import com.giyeok.bibix.frontend.BuildFrontend
import com.giyeok.bibix.frontend.cli.ProgressConsolePrinter
import com.giyeok.bibix.interpreter.BibixInterpreter
import com.giyeok.bibix.interpreter.BibixProject
import com.giyeok.bibix.interpreter.RealmProviderImpl
import com.giyeok.bibix.interpreter.coroutine.ProgressIndicator
import com.giyeok.bibix.interpreter.coroutine.ProgressIndicatorContainer
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.io.path.Path

object BlockingBibixRunner {
  @Test
  fun test() {
    val frontend = BuildFrontend(
      mainProject = BibixProject(Path("../testproject"), null),
      buildArgsMap = mapOf(),
      actionArgs = listOf(),
      progressNotifier = ProgressConsolePrinter(),
      debuggingMode = true
    )
    val interpreter = BibixInterpreter(
      buildEnv = frontend.buildEnv,
      prelude = frontend.prelude,
      preloadedPlugins = frontend.preloadedPlugins,
      realmProvider = RealmProviderImpl(),
      mainProject = frontend.mainProject,
      repo = frontend.repo,
      progressIndicatorContainer = object : ProgressIndicatorContainer {
        override fun notifyUpdated(progressIndicator: ProgressIndicator) {
          // Do nothing
        }

        override fun ofCurrentThread(): ProgressIndicator {
          return ProgressIndicator(this, 0)
        }
      },
      actionArgs = listOf()
    )

    val targetNames = listOf("test1")
    runBlocking {
      targetNames.map { targetName ->
        async { targetName to interpreter.userBuildRequest(targetName) }
      }.awaitAll()
    }.toMap()
  }
}
