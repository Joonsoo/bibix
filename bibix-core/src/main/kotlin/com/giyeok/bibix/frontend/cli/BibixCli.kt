package com.giyeok.bibix.frontend.cli

import com.giyeok.bibix.base.*
import com.giyeok.bibix.frontend.BuildFrontend
import java.io.File
import java.time.Duration
import java.time.Instant
import kotlin.system.exitProcess

object BibixCli {
  @JvmStatic
  fun main(args: Array<String>) {
    val startTime = Instant.now()

    val splitterIdx = args.indexOf("--")
    val bibixArgs = if (splitterIdx < 0) args.toList() else args.slice(0 until splitterIdx)
    val (buildArgs, buildTargetNames) = bibixArgs.partition { it.startsWith('-') }
    val actionArgs = if (splitterIdx < 0) listOf() else args.drop(splitterIdx)

    check(buildTargetNames.isNotEmpty()) { "Must specify at least one build target" }

    val buildArgsMap = mapOf<CName, BibixValue>()

    val targets = buildTargetNames.map { CName(MainSourceId, it.split('.').toList()) }
    val useDebuggingMode = buildArgs.contains("--debug")

    val buildFrontend = BuildFrontend(
      File(".").absoluteFile,
      buildArgsMap,
      ListValue(actionArgs.map { StringValue(it) })
    )

    val targetResults = buildFrontend.runTargets("build", targets)

    targetResults.forEach { (targetName, value) ->
      println("$targetName = $value")
    }

    if (useDebuggingMode) {
      val endTime = Instant.now()
      println("Build finished in ${Duration.between(startTime, endTime)}")
    }
    exitProcess(0)
  }
}
