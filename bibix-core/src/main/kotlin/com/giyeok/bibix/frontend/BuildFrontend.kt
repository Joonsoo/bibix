package com.giyeok.bibix.frontend

import com.giyeok.bibix.base.*
import com.giyeok.bibix.interpreter.BibixInterpreter
import com.giyeok.bibix.interpreter.BibixProject
import com.giyeok.bibix.interpreter.RealmProviderImpl
import com.giyeok.bibix.interpreter.coroutine.TaskElement
import com.giyeok.bibix.interpreter.coroutine.ThreadPool
import com.giyeok.bibix.interpreter.task.Task
import com.giyeok.bibix.plugins.PreloadedPlugin
import com.giyeok.bibix.plugins.bibix.bibixPlugin
import com.giyeok.bibix.plugins.curl.curlPlugin
import com.giyeok.bibix.plugins.fs.fsPlugin
import com.giyeok.bibix.plugins.java.javaPlugin
import com.giyeok.bibix.plugins.jvm.jvmPlugin
import com.giyeok.bibix.plugins.maven.mavenPlugin
import com.giyeok.bibix.repo.Repo
import kotlinx.coroutines.*

class BuildFrontend(
  val mainProject: BibixProject,
  val buildArgsMap: Map<String, String>,
  val actionArgs: List<String>,
  val preloadedPlugins: Map<String, PreloadedPlugin> = mapOf(
    "bibix" to bibixPlugin,
    "curl" to curlPlugin,
    "fs" to fsPlugin,
    "java" to javaPlugin,
    "jvm" to jvmPlugin,
    "maven" to mavenPlugin,
  ),
  val debuggingMode: Boolean = false
) {
  val repo = Repo.load(mainProject.projectRoot, debuggingMode = debuggingMode)

  private val buildEnv = BuildEnv(getOsValue(), getArchValue())

  private val threadPool = ThreadPool(getMaxThreads())

  val interpreter = BibixInterpreter(
    buildEnv = buildEnv,
    preloadedPlugins = preloadedPlugins,
    realmProvider = RealmProviderImpl(),
    mainProject = mainProject,
    repo = repo,
    progressIndicatorContainer = threadPool,
    actionArgs = actionArgs
  )

  private fun getMaxThreads(): Int {
    val maxThreads = repo.runConfig.maxThreads
    if (maxThreads <= 0) {
      return 4
    }
    return maxThreads
  }

  private fun getOsValue(): OS {
    val os = System.getProperty("os.name").lowercase()
    return when {
      os.contains("nix") || os.contains("nux") || os.contains("aix") -> OS.Linux("", "")
      os.contains("mac") -> OS.MacOS("", "")
      os.contains("win") -> OS.Windows("", "")
      else -> OS.Unknown
    }
  }

  private fun getArchValue(): Architecture {
    return when (System.getProperty("os.arch").lowercase()) {
      "amd64", "ia64" -> Architecture.X86_64
      "x86" -> Architecture.X86
      "aarch64" -> Architecture.Aarch_64
      else -> Architecture.Unknown
    }
  }

  fun buildTargets(targetNames: List<String>): Map<String, BibixValue> {
    val deferred = CoroutineScope(threadPool + TaskElement(Task.RootTask)).async {
      targetNames.map { targetName ->
        async { targetName to interpreter.userBuildRequest(targetName) }
      }.awaitAll()
    }

    threadPool.processTasks(deferred.job)

    return runBlocking { deferred.await().toMap() }
  }
}
