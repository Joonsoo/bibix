package com.giyeok.bibix

import com.giyeok.bibix.ast.BibixAst
import com.giyeok.bibix.base.*
import com.giyeok.bibix.buildscript.BuildGraph
import com.giyeok.bibix.buildscript.CNameValue
import com.giyeok.bibix.buildscript.NameLookupContext
import com.giyeok.bibix.plugins.bibix.bibixPlugin
import com.giyeok.bibix.plugins.java.javaPlugin
import com.giyeok.bibix.plugins.jvm.jvmPlugin
import com.giyeok.bibix.plugins.maven.mavenPlugin
import com.giyeok.bibix.plugins.root.rootScript
import com.giyeok.bibix.runner.BuildRunner
import com.giyeok.bibix.runner.BuildTaskRoutinesManager
import com.giyeok.bibix.runner.Repo
import com.giyeok.bibix.utils.ThreadPool
import com.giyeok.bibix.utils.toKtList
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

object MainCli {
  @JvmStatic
  fun main(args: Array<String>) {
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

    val repo = Repo.load(File("."), debuggingMode = true)

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
          )
        )
      )
    )

    val ast = parsed.left().get()
    val defs = ast.defs().toKtList()
    buildGraph.addDefs(MainSourceId, defs, repo.mainDirectory)

    val buildRunner = BuildRunner(
      buildGraph,
      rootScript,
      mapOf(
        "jvm" to jvmPlugin,
        "java" to javaPlugin,
        "maven" to mavenPlugin,
        "bibix" to bibixPlugin,
      ),
      repo,
      buildArgsMap,
      ListValue(actionArgs.map { StringValue(it) }),
    )
    buildRunner.runTargets(targets)

    val threadPool =
      ThreadPool<BuildTaskRoutinesManager.BuildTaskRoutineId>(repo.runConfig.maxThreads)

    while (true) {
      val nextTask = buildRunner.routinesManager.routinesQueue.poll(10, TimeUnit.SECONDS)
      if (nextTask != null) {
        when (nextTask) {
          is BuildTaskRoutinesManager.NextRoutine.BuildTaskRoutine ->
            threadPool.execute(nextTask.routineId, nextTask.block)
          BuildTaskRoutinesManager.NextRoutine.BuildFinished ->
            break
        }
      }
      threadPool.printProgresses()
    }

    targets.forEach { target ->
      println("$target = ${buildGraph.names.getValue(target)}")
    }
    exitProcess(0)

//    val buildArgsMap: Map<CName, BibixValue> = buildArgs.associate { arg ->
//      val argContent = if (arg.startsWith("--")) {
//        arg.substring(2)
//      } else {
//        arg.substring(1)
//      }
//      check(!argContent.startsWith('-')) { "Argument must start with - or --" }
//      val splitterIdx = argContent.indexOf('=')
//      val (argName, argValue) = if (splitterIdx < 0) Pair(argContent, BooleanValue(true))
//      else Pair(
//        argContent.substring(0, splitterIdx),
//        StringValue(argContent.substring(splitterIdx + 1))
//      )
//      CName(MainSourceId, argName.split('.')) to argValue
//    }
//
//    val repo = Repo.load(File("."))
//    val runner = GraphRunner(
//      repo.runConfig,
//      repo,
//      buildTargets.toSet(),
//      buildGraph,
//      buildArgsMap,
//      actionArgs,
//      mapOf(
//        "jvm" to LoadedBibixPlugin(jvmPlugin),
//        "java" to LoadedBibixPlugin(javaPlugin),
//        "maven" to LoadedBibixPlugin(mavenPlugin),
//      )
//    )

//    runner.start()
//
//    check(runner.isFinished())
//    buildTargets.forEach { buildTarget ->
//      println(runner.getResult(buildTarget))
//    }
//
//    exitProcess(0)
  }
}
