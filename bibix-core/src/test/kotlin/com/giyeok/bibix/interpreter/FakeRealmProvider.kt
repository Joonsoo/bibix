package com.giyeok.bibix.interpreter

import com.giyeok.bibix.base.ClassInstanceValue
import org.codehaus.plexus.classworlds.realm.ClassRealm

class FakeRealmProvider(private val provider: (ClassInstanceValue) -> ClassRealm) : RealmProvider {
  override suspend fun prepareRealm(cpInstance: ClassInstanceValue): ClassRealm =
    this.provider(cpInstance)
}
