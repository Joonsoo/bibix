package com.giyeok.bibix

import com.giyeok.bibix.ast.BibixAst
import com.giyeok.bibix.base.*
import com.giyeok.bibix.buildscript.BuildGraph
import com.giyeok.bibix.buildscript.NameLookupContext
import com.giyeok.bibix.plugins.bibix.bibixPlugin
import com.giyeok.bibix.plugins.curl.curlPlugin
import com.giyeok.bibix.plugins.java.javaPlugin
import com.giyeok.bibix.plugins.jvm.jvmPlugin
import com.giyeok.bibix.plugins.maven.mavenPlugin
import com.giyeok.bibix.plugins.root.rootScript
import com.giyeok.bibix.runner.*
import com.giyeok.bibix.runner.BuildRunner
import com.giyeok.bibix.runner.RoutinesQueueCoroutineDispatcher
import com.giyeok.bibix.runner.RoutineManager
import com.giyeok.bibix.runner.ThreadPool
import com.giyeok.bibix.utils.toKtList
import java.io.File
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

object MainCli {
  @JvmStatic
  fun main(args: Array<String>) {
    val startTime = Instant.now()
    val splitterIdx = args.indexOf("--")
    val bibixArgs = if (splitterIdx < 0) args.toList() else args.slice(0 until splitterIdx)
    val (buildArgs, buildTargetNames) = bibixArgs.partition { it.startsWith('-') }
    val actionArgs = if (splitterIdx < 0) listOf() else args.drop(splitterIdx)

    check(buildTargetNames.isNotEmpty()) { "Must specify at least one build target" }

    val buildArgsMap = mapOf<CName, BibixValue>()

    val scriptFile = File("build.bbx")
    val scriptSource = scriptFile.readText()
    val parsed = BibixAst.parseAst(scriptSource)

    if (parsed.isRight) {
      println(parsed.right().get().msg())
      exitProcess(1)
    }

    val targets = buildTargetNames.map { CName(MainSourceId, it.split('.').toList()) }
    val useDebuggingMode = buildArgs.contains("--debug")

    val repo = Repo.load(File("."), debuggingMode = useDebuggingMode)

    val buildGraph = BuildGraph()
    buildGraph.addDefs(
      BibixRootSourceId,
      rootScript.defs,
      NameLookupContext(CName(BibixRootSourceId), rootScript.defs).withNative(),
      repo.mainDirectory,
    )
    // TODO 실제 값으로 넣기
    buildGraph.addDef(
      CName(BibixRootSourceId, "env"),
      CNameValue.EvaluatedValue(
        NamedTupleValue(
          listOf(
            "os" to EnumValue(CName(BibixRootSourceId, "OS"), "linux"),
            "arch" to EnumValue(CName(BibixRootSourceId, "Arch"), "x86_64"),
            "bibixVersion" to StringValue(Constants.BIBIX_VERSION),
          )
        )
      )
    )

    val ast = parsed.left().get()
    val defs = ast.defs().toKtList()
    buildGraph.addDefs(MainSourceId, defs, repo.mainDirectory)

    val queueDispatcher = RoutinesQueueCoroutineDispatcher()
    val routineManager = RoutineManager(buildGraph, queueDispatcher)
    val threadPool = ThreadPool(buildGraph, routineManager, repo.runConfig.maxThreads)

    val buildRunner = BuildRunner(
      buildGraph,
      rootScript,
      mapOf(
        "curl" to curlPlugin,
        "jvm" to jvmPlugin,
        "java" to javaPlugin,
        "maven" to mavenPlugin,
        "bibix" to bibixPlugin,
      ),
      repo,
      routineManager,
      threadPool,
      buildArgsMap,
      ListValue(actionArgs.map { StringValue(it) }),
    )
    buildRunner.runTargets("build", targets)

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

    targets.forEach { target ->
      println("$target = ${buildRunner.getResolvedNameValue(target)}")
    }
    if (useDebuggingMode) {
      (buildRunner.routineLogger as? BuildTaskRoutineLoggerImpl)?.printLogs(buildGraph)

      val endTime = Instant.now()
      println("Build finished in ${Duration.between(startTime, endTime)}")
    }
    exitProcess(0)
  }
}
