package com.giyeok.bibix.interpreter

import com.giyeok.bibix.base.Architecture
import com.giyeok.bibix.base.BuildEnv
import com.giyeok.bibix.base.OS
import com.giyeok.bibix.frontend.ProgressNotifier
import com.giyeok.bibix.frontend.ThreadState
import com.giyeok.bibix.interpreter.coroutine.FakeProgressIndicatorContainer
import com.giyeok.bibix.plugins.Classes
import com.giyeok.bibix.plugins.PreloadedPlugin
import com.giyeok.bibix.plugins.prelude.preludePlugin
import com.giyeok.bibix.repo.BibixRepoProto
import com.giyeok.bibix.repo.Repo
import com.giyeok.bibix.runner.RunConfigProto
import java.nio.file.FileSystem

fun testRepo(fs: FileSystem): Repo = Repo.load(fs.getPath("/"))

fun testInterpreter(
  fs: FileSystem,
  mainPath: String,
  preloadedPlugins: Map<String, PreloadedPlugin>,
  preludePlugin: PreloadedPlugin = PreloadedPlugin("", listOf(), Classes()),
  realmProvider: RealmProvider = FakeRealmProvider { throw NotImplementedError() },
  actionArgs: List<String> = listOf()
) = BibixInterpreter(
  buildEnv = BuildEnv(OS.Linux("", ""), Architecture.X86_64),
  prelude = preludePlugin,
  preloadedPlugins = preloadedPlugins,
  realmProvider = realmProvider,
  mainProject = BibixProject(fs.getPath(mainPath), null),
  repo = testRepo(fs),
  progressIndicatorContainer = FakeProgressIndicatorContainer(),
  actionArgs = actionArgs
)

class NopeProgressNotifier : ProgressNotifier {
  override fun notifyProgresses(progresses: () -> List<ThreadState?>) {
    // do nothing
  }
}
