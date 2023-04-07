package com.giyeok.bibix.intellij.service

import com.giyeok.bibix.plugins.jvm.*

class PackageGraph(
  val nodes: Set<ClassOrigin>,
  val modules: Map<LocalBuilt, ModuleData>,
  val moduleTargetIds: Set<String>,
  val nonModulePkgs: Map<ClassOrigin, ClassPkg>,
  val edges: Set<Pair<ClassOrigin, ClassOrigin>>,
  val edgesByStart: Map<ClassOrigin, Set<ClassOrigin>>,
) {
  class Builder(val modules: Map<LocalBuilt, ModuleData>, val objectNamesMap: Map<String, String>) {
    private val moduleTargetIds = modules.keys.map { it.objHash }.toSet()
    private val nodes = mutableSetOf<ClassOrigin>()
    private val nonModulePkgs = mutableMapOf<ClassOrigin, ClassPkg>()
    private val edges = mutableSetOf<Pair<ClassOrigin, ClassOrigin>>()

    private fun traverseModule(module: ModuleData) {
      nodes.add(module.origin)
      val flattened = try {
        ResolveClassPkgs.flattenClassPkgs(module.dependencies + listOfNotNull(module.sdk?.second))
      } catch (e: IllegalStateException) {
        throw IllegalStateException(
          "Failed during ${objectNamesMap[module.origin.objHash] ?: module.origin.objHash}", e
        )
      }
      (flattened.compileDeps + flattened.runtimeDeps).forEach { (origin, pkg) ->
        if (!isModule(origin)) {
          nonModulePkgs[origin] = pkg
        }
        edges.add(Pair(module.origin, origin))
      }
    }

    private fun isModule(origin: ClassOrigin) =
      when (origin) {
        is LocalBuilt -> moduleTargetIds.contains(origin.objHash)
        else -> false
      }

    private fun traverseDependency(parent: ClassOrigin, pkg: ClassPkg) {
      if (!isModule(pkg.origin)) {
        nonModulePkgs[pkg.origin] = pkg
        pkg.deps.forEach { traverseDependency(pkg.origin, it) }
      }
      edges.add(Pair(parent, pkg.origin))
    }

    fun build(): PackageGraph {
      modules.forEach { (_, module) ->
        traverseModule(module)
      }
      return PackageGraph(
        nodes,
        modules,
        moduleTargetIds,
        nonModulePkgs,
        edges,
        edges
          .groupBy { it.first }
          .mapValues { (_, edges) ->
            edges.map { edge -> edge.second }.toSet()
          })
    }
  }

  fun isModule(origin: ClassOrigin): Boolean =
    when (origin) {
      is LocalBuilt -> moduleTargetIds.contains(origin.objHash)
      else -> false
    }

  fun dependentNodesOf(node: ClassOrigin): Set<ClassOrigin> {
    val deps = mutableSetOf<ClassOrigin>()

    fun traverse(node: ClassOrigin) {
      val outgoings = edgesByStart[node] ?: setOf()
      deps.addAll(outgoings)
      outgoings.forEach {
        if (it !is LocalBuilt || !moduleTargetIds.contains(it.objHash)) {
          traverse(it)
        }
      }
    }
    traverse(node)
    return deps
  }

  companion object {
    fun create(modules: Collection<ModuleData>, objectNamesMap: Map<String, String>): PackageGraph =
      Builder(modules.associateBy { it.origin }, objectNamesMap).build()
  }
}
