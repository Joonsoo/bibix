package com.giyeok.bibix.frontend.cli

import com.giyeok.bibix.frontend.BuildFrontend
import com.giyeok.bibix.graph.BibixProjectLocation
import kotlinx.coroutines.runBlocking
import java.nio.file.Paths
import java.time.Duration
import java.time.Instant
import kotlin.system.exitProcess

object BibixCli {
  @JvmStatic
  fun main(args: Array<String>) {
    val startTime = Instant.now()

    val splitterIdx = args.indexOf("--")
    val bibixArgs = if (splitterIdx < 0) args.toList() else args.slice(0 until splitterIdx)
    val (buildArgs, names) = bibixArgs.partition { it.startsWith('-') }
    val actionArgs = if (splitterIdx < 0) listOf() else args.drop(splitterIdx)

    val buildArgsMap = mapOf<String, String>()

    val useDebuggingMode = buildArgs.contains("--debug")

    val buildFrontend = BuildFrontend(
      mainProjectLocation = BibixProjectLocation(Paths.get("")),
      buildArgsMap = buildArgsMap,
      actionArgs = actionArgs,
      debuggingMode = useDebuggingMode
    )

    check(names.isNotEmpty()) {
      val targets = buildFrontend.mainScriptTaskNames()
      "Must specify at least one build target\nAvailable targets:\n$targets"
    }

    val targetResults = runBlocking { buildFrontend.runBuild(names) }

    buildFrontend.repo.shutdown()

    targetResults.forEach { (targetName, value) ->
      println("$targetName = $value")
    }

    val endTime = Instant.now()
    println("Build finished in ${Duration.between(startTime, endTime)}")

    exitProcess(0)
  }
}
