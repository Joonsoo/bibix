package com.giyeok.bibix.graph.runner

import com.giyeok.bibix.base.BibixValue

class ValueStore {
  private var idCounter = 0
  val bibixValues: MutableMap<Int, BibixValue> = mutableMapOf()
  val bibixValueMaps: MutableMap<Int, Map<String, BibixValue>> = mutableMapOf()

  fun idOf(value: BibixValue): Int {
    idCounter += 1
    bibixValues[idCounter] = value
    return idCounter
  }

  fun valueOf(valueId: Int) = bibixValues.getValue(valueId)

  fun idOf(valueMap: Map<String, BibixValue>): Int =
    if (valueMap.isEmpty()) 0 else {
      idCounter += 1
      bibixValueMaps[idCounter] = valueMap
      idCounter
    }

  fun valueMapOf(valueMapId: Int) =
    if (valueMapId == 0) mapOf() else bibixValueMaps.getValue(valueMapId)
}
