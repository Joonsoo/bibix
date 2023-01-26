package com.giyeok.bibix.repo

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.nio.file.Path
import kotlin.io.path.absolute

interface DirectoryLocker {
  suspend fun <T> withLock(directory: Path, func: () -> T): T
}

class DirectoryLockerImpl : DirectoryLocker {
  // TODO implement to support global lock(lock between multiple instances of bibix)
  private val mutex = Mutex()
  private val mutexes = mutableMapOf<Path, Mutex>()

  override suspend fun <T> withLock(directory: Path, func: () -> T): T {
    val path = directory.absolute().normalize()
    val directoryMutex = mutex.withLock {
      val existing = mutexes[path]
      if (existing == null) {
        val newMutex = Mutex()
        mutexes[path] = newMutex
        newMutex
      } else {
        existing
      }
    }
    return directoryMutex.withLock { func() }
  }
}
