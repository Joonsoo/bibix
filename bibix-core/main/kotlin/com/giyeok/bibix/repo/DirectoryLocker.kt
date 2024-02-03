package com.giyeok.bibix.repo

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.nio.file.Path
import kotlin.io.path.absolute

interface DirectoryLocker {
  // suspend fun <T> withLock(directory: Path, func: () -> T): T
  suspend fun acquireLock(directory: Path)
  suspend fun releaseLock(directory: Path)
}

// needs to be thread safe
class DirectoryLockerImpl: DirectoryLocker {
  // TODO implement to support global lock(lock between multiple instances of bibix)
  private val mutex = Mutex()
  private val mutexes = mutableMapOf<Path, Mutex>()

  private suspend fun getDirectoryMutex(directory: Path): Mutex {
    val path = directory.absolute().normalize()
    return mutex.withLock {
      val existing = mutexes[path]
      if (existing == null) {
        val newMutex = Mutex()
        mutexes[path] = newMutex
        newMutex
      } else {
        existing
      }
    }
  }

  override suspend fun acquireLock(directory: Path) {
    getDirectoryMutex(directory).lock()
  }

  override suspend fun releaseLock(directory: Path) {
    getDirectoryMutex(directory).unlock()
  }
}
