package com.giyeok.bibix.interpreter

import com.giyeok.bibix.base.ClassInstanceValue
import com.giyeok.bibix.base.PathValue
import com.giyeok.bibix.base.SetValue
import com.giyeok.bibix.base.SourceId
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.codehaus.plexus.classworlds.ClassWorld
import org.codehaus.plexus.classworlds.realm.ClassRealm
import kotlin.io.path.absolute

interface PluginClassLoader {
  suspend fun loadPluginInstance(
    callerSourceId: SourceId,
    cpInstance: ClassInstanceValue,
    className: String
  ): Any
}

class PluginClassLoaderImpl : PluginClassLoader {
  private val classWorld = ClassWorld()
  private var realmIdCounter = 0
  private val mutex = Mutex()

  private suspend fun newRealm(): ClassRealm = mutex.withLock {
    realmIdCounter += 1
    val newRealm = classWorld.newRealm("realm-$realmIdCounter")
    newRealm
  }

  override suspend fun loadPluginInstance(
    callerSourceId: SourceId,
    cpInstance: ClassInstanceValue,
    className: String
  ): Any {
    val realm = newRealm()
    ((cpInstance.fieldValues.getValue("cps")) as SetValue).values.forEach {
      realm.addURL((it as PathValue).path.absolute().toUri().toURL())
    }
    val cls = realm.loadClass(className)
    return cls.getDeclaredConstructor().newInstance()
  }
}
