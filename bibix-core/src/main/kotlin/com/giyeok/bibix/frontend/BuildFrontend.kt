package com.giyeok.bibix.frontend

import com.giyeok.bibix.ast.BibixAst
import com.giyeok.bibix.base.*
import com.giyeok.bibix.buildscript.BuildGraph
import com.giyeok.bibix.buildscript.NameLookupContext
import com.giyeok.bibix.interpreter.expr.Coercer
import com.giyeok.bibix.plugins.PreloadedPlugin
import com.giyeok.bibix.plugins.bibix.preloadedPlugin
import com.giyeok.bibix.plugins.curl.curlPlugin
import com.giyeok.bibix.plugins.java.javaPlugin
import com.giyeok.bibix.plugins.jvm.jvmPlugin
import com.giyeok.bibix.plugins.maven.mavenPlugin
import com.giyeok.bibix.plugins.root.rootScript
import com.giyeok.bibix.repo.Repo
import com.giyeok.bibix.runner.*
import com.giyeok.bibix.utils.toKtList
import com.giyeok.jparser.ParsingErrors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.readText

class BuildFrontend(
  val projectDir: Path,
  val buildArgsMap: Map<CName, BibixValue> = mapOf(),
  val actionArgs: ListValue? = null,
  val scriptFileName: String = "build.bbx",
  val useDebuggingMode: Boolean = false,
  val rootPlugins: Map<String, PreloadedPlugin> = mapOf(
    "curl" to curlPlugin,
    "jvm" to jvmPlugin,
    "java" to javaPlugin,
    "maven" to mavenPlugin,
    "bibix" to preloadedPlugin,
  )
) {
  val scriptFile = projectDir.resolve(scriptFileName)

  val parsed by lazy {
    val scriptSource = scriptFile.readText()
    BibixAst.parseAst(scriptSource)
  }

  val parseError: ParsingErrors.ParsingError? = if (parsed.isRight) parsed.right().get() else null

  val ast: BibixAst.BuildScript by lazy {
    if (parsed.isRight) {
      throw IllegalStateException("Failed to parse build script: ${parsed.right().get().msg()}")
    } else {
      parsed.left().get()
    }
  }

  val repo = Repo.load(projectDir, debuggingMode = useDebuggingMode)

  fun getOsValue(): String {
    val os = System.getProperty("os.name").lowercase()
    return when {
      os.contains("nix") || os.contains("nux") || os.contains("aix") -> "linux"
      os.contains("mac") -> "osx"
      os.contains("win") -> "windows"
      else -> "unknown"
    }
  }

  fun getArchValue(): String {
    return when (System.getProperty("os.arch").lowercase()) {
      "amd64", "ia64" -> "x86_64"
      "x86" -> "x86"
      "aarch64" -> "aarch_64"
      else -> "unknown"
    }
  }

  val buildGraph by lazy {
    val buildGraph = BuildGraph()

    buildGraph.addDefs(
      BibixRootSourceId,
      rootScript.defs,
      NameLookupContext(CName(BibixRootSourceId), rootScript.defs).withNative(),
      repo.mainDirectory,
    )
    val os = getOsValue()
    val arch = getArchValue()
    // TODO 실제 값으로 넣기
    buildGraph.addDef(
      CName(BibixRootSourceId, "env"),
      CNameValue.EvaluatedValue(
        NamedTupleValue(
          listOf(
            "os" to EnumValue("com.giyeok.bibix", "OS", os),
            "arch" to EnumValue("com.giyeok.bibix", "Arch", arch),
            "bibixVersion" to StringValue(Constants.BIBIX_VERSION),
          )
        )
      )
    )

    val defs = ast.defs().toKtList()
    buildGraph.addDefs(MainSourceId, defs, repo.mainDirectory)

    buildGraph
  }

  val queueDispatcher = RoutinesQueueCoroutineDispatcher()

  val routineManager = RoutineManager(buildGraph, queueDispatcher)

  val threadPool = ThreadPool(buildGraph, routineManager, repo.runConfig.maxThreads)

  val buildEnv = BuildEnv(OS.Linux("ubuntu", "22.04"), Architecture.X86_64)

  val buildRunner = BuildRunner(
    buildEnv,
    buildGraph,
    rootScript,
    rootPlugins,
    repo,
    routineManager,
    threadPool,
    buildArgsMap,
    actionArgs,
  )

  val coercer = Coercer(buildGraph, buildRunner)

  private val idCounter = AtomicInteger()
  fun nextBuildId() = "build${idCounter.incrementAndGet()}"

  fun runTasks(buildRequestName: String, tasks: List<BuildTask>): List<Any?> {
    val rootTask = BuildTask.BuildRequest(buildRequestName)

    val coroutineContext = routineManager.coroutineDispatcher + BuildTaskElement(rootTask)
    CoroutineScope(coroutineContext).launch(coroutineContext) {
      buildRunner.runTasks(rootTask, tasks)
      routineManager.buildFinished(buildRequestName)
    }

    while (true) {
      val nextTask = queueDispatcher.routinesQueue.poll(10, TimeUnit.SECONDS)
      if (nextTask != null) {
        when (nextTask) {
          is RoutinesQueueCoroutineDispatcher.NextRoutine.BuildTaskRoutine ->
            threadPool.execute(nextTask.routineId, nextTask.block)

          is RoutinesQueueCoroutineDispatcher.NextRoutine.BuildFinished ->
            break
        }
      }
      threadPool.printProgresses()
    }

    if (useDebuggingMode) {
      (buildRunner.routineLogger as? BuildTaskRoutineLoggerImpl)?.printLogs(buildGraph)
    }

    return tasks.map { buildRunner.routineManager.getTaskResult(it) }
  }

  fun runTargets(buildRequestName: String, targets: List<CName>): Map<CName, BibixValue> {
    runTasks(buildRequestName, targets.map { BuildTask.ResolveName(it) })

    return targets.mapNotNull { target ->
      buildRunner.getResolvedNameValue(target)?.let { value -> target to value }
    }.toMap()
  }
}
