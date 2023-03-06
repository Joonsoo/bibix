package com.giyeok.bibix.interpreter

import com.giyeok.bibix.base.PathValue
import com.giyeok.bibix.base.SourceId
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.codehaus.plexus.classworlds.ClassWorld
import org.codehaus.plexus.classworlds.realm.ClassRealm
import java.nio.file.Path
import kotlin.io.path.absolute
import kotlin.io.path.name

interface PluginImplProvider {
  suspend fun getPluginImplInstance(
    callerSourceId: SourceId,
    cps: List<Path>,
    className: String
  ): Any
}

class PluginImplProviderImpl(
  private val classWorld: ClassWorld = ClassWorld()
) : PluginImplProvider {
  private var realmIdCounter = 0
  private val mutex = Mutex()

  private val realmCache = mutableMapOf<Set<Path>, ClassRealm>()

  private val baseRealm = classWorld.newRealm("bibix-root-realm")

  override suspend fun getPluginImplInstance(
    callerSourceId: SourceId,
    cps: List<Path>,
    className: String
  ): Any = mutex.withLock {
    val cpsSet = cps.map { it.absolute() }.toSet()
    val cached = realmCache[cpsSet]
    val realm = if (cached != null) cached else {
      realmIdCounter += 1
      val newRealm = baseRealm.createChildRealm("bibix-realm-$realmIdCounter")

      cps.forEach {
        newRealm.addURL(it.absolute().toUri().toURL())
      }
      realmCache[cpsSet] = newRealm
      newRealm
    }
    val cls = realm.loadClass(className)
    return cls.getDeclaredConstructor().newInstance()
  }
}
