package com.giyeok.bibix.intellij.service

import com.giyeok.bibix.plugins.jvm.*

class PackageGraph(
  val nodes: Set<ClassOrigin>,
  val modules: Map<LocalBuilt, ModuleData>,
  val nonModulePkgs: Map<ClassOrigin, ClassPkg>,
  val edges: Set<Pair<ClassOrigin, ClassOrigin>>,
  val edgesByStart: Map<ClassOrigin, Set<ClassOrigin>>,
) {
  class Builder(
    private val nodes: MutableSet<ClassOrigin>,
    private val modules: MutableMap<LocalBuilt, ModuleData>,
    private val nonModulePkgs: MutableMap<ClassOrigin, ClassPkg>,
    private val edges: MutableSet<Pair<ClassOrigin, ClassOrigin>>
  ) {
    fun addModule(module: ModuleData) {
      nodes.add(module.origin)
      modules[module.origin] = module
      module.dependencies.forEach { traverseDependency(module.origin, it) }
    }

    private fun traverseDependency(parent: ClassOrigin, pkg: ClassPkg) {
      when (pkg.origin) {
        is LocalBuilt -> {
          // TODO ensure origin in modules
        }

        is LocalLib, is MavenDep -> {
          nonModulePkgs[pkg.origin] = pkg
          pkg.deps.forEach { traverseDependency(pkg.origin, it) }
        }
      }
      edges.add(Pair(parent, pkg.origin))
    }

    fun build(): PackageGraph =
      PackageGraph(
        nodes,
        modules,
        nonModulePkgs,
        edges,
        edges
          .groupBy { it.first }
          .mapValues { (_, edges) ->
            edges.map { edge -> edge.second }.toSet()
          })
  }

  fun dependentNodesOf(node: ClassOrigin): Set<ClassOrigin> {
    val deps = mutableSetOf<ClassOrigin>()

    fun traverse(node: ClassOrigin) {
      val outgoings = edgesByStart[node] ?: setOf()
      deps.addAll(outgoings)
      outgoings.forEach {
        if (it !is LocalBuilt) {
          traverse(it)
        }
      }
    }
    traverse(node)
    return deps
  }

  companion object {
    fun create(modules: Collection<ModuleData>): PackageGraph {
      val builder = Builder(mutableSetOf(), mutableMapOf(), mutableMapOf(), mutableSetOf())
      modules.forEach { builder.addModule(it) }

      return builder.build()
    }
  }
}
