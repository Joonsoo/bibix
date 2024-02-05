package com.giyeok.bibix.graph.runner

import com.giyeok.bibix.ast.BibixParser
import com.giyeok.bibix.graph.BibixProjectLocation
import com.giyeok.bibix.graph.BuildGraph
import com.giyeok.jparser.ktlib.ParsingErrorKt
import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap

class MultiBuildGraph private constructor(
  val projectGraphs: MutableMap<Int, BuildGraph>,
  val projectPackages: BiMap<Int, String>,
  val projectLocations: BiMap<Int, BibixProjectLocation>,
  val projectSources: MutableMap<Int, String>,
) {
  constructor(projects: Map<Int, ProjectInfo>): this(
    projects.mapValues { it.value.graph }.toMutableMap(),
    HashBiMap.create(projects.mapNotNull { (projectId, projectInfo) ->
      projectInfo.graph.packageName?.let { projectId to it }
    }.toMap()),
    HashBiMap.create(projects.filter { it.value.location != null }
      .mapValues { it.value.location!! }),
    projects.mapValues { it.value.scriptSource }.toMutableMap()
  )

  data class ProjectInfo(
    val location: BibixProjectLocation?,
    val scriptSource: String,
    val graph: BuildGraph,
  )

  private fun nextProjectId(): Int = (projectGraphs.keys.maxOrNull() ?: 0) + 1

  fun addProject(
    location: BibixProjectLocation,
    graph: BuildGraph,
    source: String
  ): Int {
    val projectId = nextProjectId()

    check(projectId !in projectGraphs)
    check(!projectLocations.containsValue(location))
    projectGraphs[projectId] = graph
    if (graph.packageName != null) {
      check(graph.packageName !in projectPackages.inverse())
      projectPackages[projectId] = graph.packageName
    }
    projectLocations[projectId] = location
    projectSources[projectId] = source
    return projectId
  }

  fun getProjectGraph(projectId: Int): BuildGraph =
    projectGraphs[projectId] ?: throw IllegalStateException()

  fun getProjectIdByLocation(location: BibixProjectLocation): Int? =
    projectLocations.inverse()[location]

  fun getProjectIdByPackageName(packageName: String): Int? =
    projectPackages.inverse()[packageName]
}
