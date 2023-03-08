package com.giyeok.bibix.frontend

import com.giyeok.bibix.ast.BibixAst
import com.giyeok.bibix.base.*
import com.giyeok.bibix.interpreter.*
import com.giyeok.bibix.interpreter.coroutine.TaskElement
import com.giyeok.bibix.interpreter.expr.EvaluationResult
import com.giyeok.bibix.interpreter.expr.NameLookupContext
import com.giyeok.bibix.interpreter.task.Task
import com.giyeok.bibix.plugins.PreloadedPlugin
import com.giyeok.bibix.plugins.bibix.bibixPlugin
import com.giyeok.bibix.plugins.curl.curlPlugin
import com.giyeok.bibix.plugins.file.filePlugin
import com.giyeok.bibix.plugins.jvm.jvmPlugin
import com.giyeok.bibix.plugins.maven.mavenPlugin
import com.giyeok.bibix.plugins.prelude.preludePlugin
import com.giyeok.bibix.repo.Repo
import kotlinx.coroutines.*
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.system.exitProcess

class BuildFrontend(
  val mainProject: BibixProject,
  val buildArgsMap: Map<String, String>,
  val actionArgs: List<String>,
  val progressNotifier: ProgressNotifier,
  val prelude: PreloadedPlugin = preludePlugin,
  val preloadedPlugins: Map<String, PreloadedPlugin> = defaultPreloadedPlugins,
  val pluginImplProvider: PluginImplProvider = PluginImplProviderImpl(),
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

  val repo = Repo.load(mainProject.projectRoot, debuggingMode = debuggingMode)

  val buildEnv = BuildEnv(getOsValue(), getArchValue())

  private val threadPool = ThreadPool(getMaxThreads(), progressNotifier)

  val interpreter = BibixInterpreter(
    buildEnv = buildEnv,
    prelude = prelude,
    preloadedPlugins = preloadedPlugins,
    pluginImplProvider = pluginImplProvider,
    mainProject = mainProject,
    repo = repo,
    progressIndicatorContainer = threadPool,
    actionArgs = actionArgs
  )

  init {
    progressNotifier.setInterpreter(interpreter)
  }

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

  fun buildTargets(targetNames: List<String>): Map<String, BibixValue> {
    val deferred = CoroutineScope(threadPool + TaskElement(Task.RootTask)).async {
      targetNames.map { targetName ->
        async { targetName to interpreter.userBuildRequest(targetName) }
      }.awaitAll()
    }

    threadPool.processTasks(deferred.job)

    return runBlocking {
      interpreter.g.clear()
      try {
        deferred.await().toMap()
      } catch (e: Exception) {
        when (e) {
          is BibixExecutionException -> {
            val writer = StringWriter()
            val pwriter = PrintWriter(writer)
            pwriter.println("Task trace (size=${e.trace.size}):")
            e.trace.forEach { task ->
              pwriter.println(task)
              interpreter.taskDescriptor.printTaskDescription(task, pwriter)
            }
            pwriter.println("===")
            System.err.println(writer.toString())
            e.printStackTrace()
          }

          else -> {
            e.printStackTrace()
          }
        }
        // Shutdown
        repo.shutdown()
        exitProcess(1)
      }
    }
  }

  fun blockingBuildTargets(targetNames: List<String>): Map<String, BibixValue> {
    threadPool.setLocalProgressIndicator()
    return runBlocking {
      interpreter.g.clear()
      targetNames.associateWith { targetName ->
        interpreter.userBuildRequest(targetName)
      }
    }
  }

  fun blockingEvaluateName(
    context: ExprEvalContext,
    nameTokens: List<String>
  ): EvaluationResult {
    threadPool.setLocalProgressIndicator()
    return runBlocking {
      interpreter.g.clear()
      interpreter.exprEvaluator.evaluateName(
        Task.RootTask,
        context,
        nameTokens,
        null,
        setOf()
      )
    }
  }

  // Returns the list of the name definitions in the main script
  fun mainScriptDefinitions() = interpreter.nameLookupTable.definitions
    .filterKeys { cname -> cname.sourceId == MainSourceId }
    .mapKeys { (cname, _) -> cname.tokens.joinToString(".") }
}
