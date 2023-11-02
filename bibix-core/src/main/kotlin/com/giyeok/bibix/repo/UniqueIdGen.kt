package com.giyeok.bibix.repo

import kotlin.random.Random

object UniqueIdGen {
  fun generate(): String {
    return "${System.currentTimeMillis()}-${"%05d".format(Random.nextInt(0, 100000))}"
  }
}
