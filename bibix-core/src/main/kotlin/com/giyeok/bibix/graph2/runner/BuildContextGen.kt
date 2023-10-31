package com.giyeok.bibix.graph2.runner

import com.giyeok.bibix.base.BibixValue
import com.giyeok.bibix.base.BuildContext
import com.giyeok.bibix.base.BuildEnv
import com.giyeok.bibix.base.ProgressLogger
import com.giyeok.bibix.repo.BibixRepo
import com.giyeok.bibix.targetIdData
import java.nio.file.FileSystem
import java.nio.file.Path

class BuildContextGen(
  val multiGraph: MultiBuildGraph,
  val buildEnv: BuildEnv,
  val fileSystem: FileSystem,
  val repo: BibixRepo
) {
  fun generate(
    callerProjectId: Int,
    ruleDefinedProjectId: Int,
    args: Map<String, BibixValue>
  ): BuildContext {
    val mainLocation = multiGraph.projectLocations.getValue(1)
    val callerLocation = multiGraph.projectLocations[callerProjectId]
    val ruleDefinedLocation = multiGraph.projectLocations[ruleDefinedProjectId]
    return BuildContext(
      buildEnv = buildEnv,
      fileSystem = fileSystem,
      mainBaseDirectory = mainLocation.projectRoot,
      callerBaseDirectory = callerLocation?.projectRoot,
      ruleDefinedDirectory = ruleDefinedLocation?.projectRoot,
      arguments = args,
      targetIdData = targetIdData { },
      targetId = "",
      hashChanged = true,
      prevBuildTime = null,
      prevResult = null,
      destDirectoryPath = Path.of(""),
      progressLogger = object: ProgressLogger {
        override fun logInfo(message: String) {
          TODO("Not yet implemented")
        }

        override fun logError(message: String) {
          TODO("Not yet implemented")
        }
      },
      repo = repo
    )
  }
}
