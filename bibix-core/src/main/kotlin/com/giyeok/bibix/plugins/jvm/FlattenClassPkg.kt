package com.giyeok.bibix.plugins.jvm

import com.giyeok.bibix.base.*

class FlattenClassPkg {
  private fun traversePkgs(
    classPkg: ClassInstanceValue,
    cc: MutableList<PathValue>
  ): List<PathValue> {
    val (_, cps, deps) = (classPkg.value as NamedTupleValue).values

    val paths = (cps.second as SetValue).values.map { it as PathValue }
    cc.addAll(paths - cc.toSet())
    for (dep in (deps.second as SetValue).values) {
      traversePkgs(dep as ClassInstanceValue, cc)
    }
    return cc
  }

  fun build(context: BuildContext): BibixValue {
    val classPkg = context.arguments.getValue("classPkg") as ClassInstanceValue

    return SetValue(traversePkgs(classPkg, mutableListOf()))
  }
}
