package com.giyeok.bibix.frontend.cli

import com.giyeok.bibix.frontend.BuildFrontend
import com.giyeok.bibix.interpreter.BibixProject
import com.giyeok.bibix.plugins.bibix.bibixPlugin
import com.giyeok.bibix.plugins.curl.curlPlugin
import com.giyeok.bibix.plugins.java.javaPlugin
import com.giyeok.bibix.plugins.jvm.jvmPlugin
import com.giyeok.bibix.plugins.maven.mavenPlugin
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
    val (buildArgs, buildTargetNames) = bibixArgs.partition { it.startsWith('-') }
    val actionArgs = if (splitterIdx < 0) listOf() else args.drop(splitterIdx)

    check(buildTargetNames.isNotEmpty()) { "Must specify at least one build target" }

    val buildArgsMap = mapOf<String, String>()

    val useDebuggingMode = buildArgs.contains("--debug")

    val buildFrontend = BuildFrontend(
      mainProject = BibixProject(Paths.get(""), null),
      buildArgsMap = buildArgsMap,
      actionArgs = actionArgs,
      debuggingMode = useDebuggingMode
    )

    val targetResults = buildFrontend.buildTargets(buildTargetNames)

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
