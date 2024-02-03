package com.giyeok.bibix.utils

import kotlinx.coroutines.delay
import java.util.concurrent.CancellationException
import java.util.concurrent.Future
import kotlin.time.Duration.Companion.milliseconds

suspend fun <T> Future<T>.await(): T {
  while (!this.isDone) {
    delay(100.milliseconds)
  }
  try {
    return this.get()
  } catch (e: CancellationException) {
    throw e
  }
}
