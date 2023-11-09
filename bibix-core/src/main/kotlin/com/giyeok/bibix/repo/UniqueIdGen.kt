package com.giyeok.bibix.repo

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.random.Random

object UniqueIdGen {
  fun generate(): String {
    val now = LocalDateTime.now()
      .truncatedTo(ChronoUnit.SECONDS)
      .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    return "$now-${"%05d".format(Random.nextInt(0, 100000))}"
  }
}
