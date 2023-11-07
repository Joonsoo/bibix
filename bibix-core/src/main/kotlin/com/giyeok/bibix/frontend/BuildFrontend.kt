package com.giyeok.bibix.frontend

import com.giyeok.bibix.base.Architecture
import com.giyeok.bibix.base.BibixValue
import com.giyeok.bibix.base.BuildEnv
import com.giyeok.bibix.base.OS
import com.giyeok.bibix.graph.BibixProjectLocation
import com.giyeok.bibix.graph.runner.*
import com.giyeok.bibix.plugins.PreloadedPlugin
import com.giyeok.bibix.plugins.bibix.bibixPlugin
import com.giyeok.bibix.plugins.curl.curlPlugin
import com.giyeok.bibix.plugins.file.filePlugin
import com.giyeok.bibix.plugins.jvm.jvmPlugin
import com.giyeok.bibix.plugins.maven.mavenPlugin
import com.giyeok.bibix.plugins.prelude.preludePlugin
import com.giyeok.bibix.repo.BibixRepo
import org.codehaus.plexus.classworlds.ClassWorld
import java.lang.StringBuilder
import java.nio.file.FileSystems

class BuildFrontend(
  val mainProjectLocation: BibixProjectLocation,
  // build args는 어떻게 쓰지? 원래 어쩔 생각이었더라..
  val buildArgsMap: Map<String, String>,
  val actionArgs: List<String>,
  prelude: PreloadedPlugin = preludePlugin,
  preloadedPlugins: Map<String, PreloadedPlugin> = defaultPreloadedPlugins,
  classPkgRunner: ClassPkgRunner = ClassPkgRunner(ClassWorld()),
  pluginOverrides: PluginOverrides = NoPluginOverrides,
  targetLogFileName: String = "log.json",
  val debuggingMode: Boolean = false
) {
  companion object {
    val defaultPreloadedPlugins = mapOf(
      "bibix" to bibixPlugin,
      "file" to filePlugin,
      "curl" to curlPlugin,
      "jvm" to jvmPlugin,
      "maven" to mavenPlugin,
    )
  }

  val repo = BibixRepo.load(
    mainProjectLocation.projectRoot,
    targetLogFileName = targetLogFileName,
    debuggingMode = debuggingMode
  )

  val buildEnv = BuildEnv(getOsValue(), getArchValue())

  val buildGraphRunner = BuildGraphRunner.create(
    mainProjectLocation = mainProjectLocation,
    preludePlugin = prelude,
    preloadedPlugins = preloadedPlugins,
    buildEnv = buildEnv,
    fileSystem = FileSystems.getDefault(),
    repo = repo,
    classPkgRunner = classPkgRunner,
    pluginOverride = pluginOverrides,
  )

  val jobExecutorTracker = ExecutorTracker(getMaxThreads())

  val parallelRunner = ParallelGraphRunner(
    runner = buildGraphRunner,
    longRunningJobExecutor = jobExecutorTracker.executor,
    jobExecutorTracker = jobExecutorTracker
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
      os.contains("mac") -> OS.MacOSX("", "")
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

  // Returns the list of the name definitions in the main script
  val mainScriptDefinitions: Map<String, BuildTask> by lazy {
    val mainBuildGraph = buildGraphRunner.multiGraph.getProjectGraph(1)

    val targets = mainBuildGraph.targets.keys.associate { name ->
      name.toString() to EvalTarget(1, 0, name)
    }
    val actions = mainBuildGraph.actions.keys.associate { name ->
      name.toString() to ExecAction(1, 0, name, 0)
    }

    targets + actions
  }

  fun mainScriptTaskNames(lineIndent: String = "  "): String {
    val targets = mainScriptDefinitions.entries.filter { it.value is EvalTarget }.map { it.key }
    val actions = mainScriptDefinitions.entries.filter { it.value is ExecAction }.map { it.key }

    return if (targets.isEmpty() && actions.isEmpty()) {
      "${lineIndent}No targets or actions"
    } else {
      val builder = StringBuilder()
      if (targets.isNotEmpty()) {
        builder.append("$lineIndent  === targets ===\n")
        targets.sorted().forEach { target ->
          builder.append("$lineIndent$target\n")
        }
      }
      if (actions.isNotEmpty()) {
        builder.append("$lineIndent  === actions ===\n")
        actions.sorted().forEach { action ->
          builder.append("$lineIndent$action\n")
        }
      }
      builder.toString()
    }
  }

  suspend fun runBuildTasks(buildTasks: List<BuildTask>): Map<BuildTask, BuildTaskResult.FinalResult?> =
//    buildTasks.associateWith { BlockingBuildGraphRunner(buildGraphRunner).runToFinal(it) }
    parallelRunner.runTasks(buildTasks)

  suspend fun runBuild(names: List<String>): Map<String, BibixValue> {
    check(mainScriptDefinitions.keys.containsAll(names)) {
      val targets = mainScriptTaskNames()
      "Unknown name: ${names - mainScriptDefinitions.keys}\nAvailable targets:\n$targets"
    }

    val tasks = names.associateWith { mainScriptDefinitions.getValue(it) }

    val results = runBuildTasks(tasks.values.toList())

    return results.mapNotNull { (task, result) ->
      if (result != null && result is BuildTaskResult.ResultWithValue) {
        val name = when (task) {
          is EvalTarget -> task.name.toString()
          is ExecAction -> task.actionName.toString()
          else -> throw AssertionError()
        }
        name to result.value
      } else {
        null
      }
    }.toMap()
  }
}
