package com.giyeok.bibix.interpreter

import com.giyeok.bibix.base.Architecture
import com.giyeok.bibix.base.BuildEnv
import com.giyeok.bibix.base.OS
import com.giyeok.bibix.frontend.ProgressNotifier
import com.giyeok.bibix.frontend.ThreadState
import com.giyeok.bibix.interpreter.coroutine.FakeProgressIndicatorContainer
import com.giyeok.bibix.plugins.PluginInstanceProvider
import com.giyeok.bibix.plugins.PreloadedPlugin
import com.giyeok.bibix.repo.BibixRepo
import java.nio.file.FileSystem

fun testRepo(fs: FileSystem): BibixRepo = BibixRepo.load(fs.getPath("/"))

fun testInterpreter(
  fs: FileSystem,
  mainPath: String,
  preloadedPlugins: Map<String, PreloadedPlugin>,
  preludePlugin: PreloadedPlugin = PreloadedPlugin("", listOf(), PluginInstanceProvider(), ""),
  pluginImplProvider: PluginImplProvider = FakePluginImplProvider { _, _, _ -> throw NotImplementedError() },
  actionArgs: List<String> = listOf()
) = BibixInterpreter(
  buildEnv = BuildEnv(OS.Linux("", ""), Architecture.X86_64),
  prelude = preludePlugin,
  preloadedPlugins = preloadedPlugins,
  pluginImplProvider = pluginImplProvider,
  mainProject = BibixProject(fs.getPath(mainPath), null),
  repo = testRepo(fs),
  progressIndicatorContainer = FakeProgressIndicatorContainer(),
  actionArgs = actionArgs
)

class NopeProgressNotifier : ProgressNotifier {
  override fun setInterpreter(interpreter: BibixInterpreter) {
    // do nothing
  }

  override fun notifyProgresses(progresses: () -> List<ThreadState?>) {
    // do nothing
  }
}
