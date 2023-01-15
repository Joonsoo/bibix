package com.giyeok.bibix.interpreter

import com.giyeok.bibix.base.ClassInstanceValue
import com.giyeok.bibix.base.PathValue
import com.giyeok.bibix.base.SetValue
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.codehaus.plexus.classworlds.ClassWorld
import org.codehaus.plexus.classworlds.realm.ClassRealm
import kotlin.io.path.absolute

interface RealmProvider {
  suspend fun prepareRealm(cpInstance: ClassInstanceValue): ClassRealm
}

class RealmProviderImpl : RealmProvider {
  private val classWorld = ClassWorld()
  private var realmIdCounter = 0
  private val mutex = Mutex()

  private suspend fun newRealm(): ClassRealm = mutex.withLock {
    realmIdCounter += 1
    val newRealm = classWorld.newRealm("realm-$realmIdCounter")
    newRealm
  }

  override suspend fun prepareRealm(cpInstance: ClassInstanceValue): ClassRealm {
    val realm = newRealm()
    ((cpInstance.fieldValues.getValue("cps")) as SetValue).values.forEach {
      realm.addURL((it as PathValue).path.absolute().toUri().toURL())
    }
    return realm
  }
}
