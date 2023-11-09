package com.giyeok.bibix.graph.runner

import com.giyeok.bibix.base.BibixValue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

// 원래는 꼭 thread safe할 필요가 없을 것 같은데 어딘가에서 오류가 생기는 것 같아서 thread-safe하게 만듦
class ValueStore {
  private val idCounter = AtomicInteger(0)
  private val values = ConcurrentHashMap<Int, BibixValue>()
  private val valueMaps = ConcurrentHashMap<Int, Map<String, BibixValue>>()

  fun idOf(value: BibixValue): Int {
    val newId = idCounter.incrementAndGet()
    values[newId] = value
    return newId
  }

  fun idOf(valueMap: Map<String, BibixValue>): Int {
    val newId = idCounter.incrementAndGet()
    valueMaps[newId] = valueMap
    return newId
  }

  fun valueOf(id: Int): BibixValue =
    values.getValue(id)

  fun valueMapOf(id: Int): Map<String, BibixValue> =
    valueMaps.getValue(id)
}
