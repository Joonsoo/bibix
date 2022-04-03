package com.giyeok.bibix.buildscript

class Registry<T>(
  private var idCounter: Int,
  val map: Map<Int, T>,
) {
  class Builder<T>(
    private var idCounter: Int = 0,
    private val map: MutableMap<Int, T> = mutableMapOf()
  ) {
    fun nextId(): Int {
      idCounter += 1
      return idCounter
    }

    fun register(element: T): Int {
      val nextId = nextId()
      map[nextId] = element
      return nextId
    }

    operator fun set(id: Int, element: T) {
      check(!map.containsKey(id))
      map[id] = element
    }

    operator fun get(id: Int) = map.getValue(id)

    fun build(): Registry<T> = Registry(idCounter, map)
  }

  operator fun get(id: Int) = map.getValue(id)

  fun toBuilder() = Builder(idCounter, map.toMutableMap())
}
