package com.giyeok.bibix.intellij.service

import com.giyeok.bibix.base.ClassInstanceValue
import com.giyeok.bibix.base.SourceId
import com.giyeok.bibix.interpreter.PluginImplProvider
import com.giyeok.bibix.interpreter.PluginImplProviderImpl

class OverridingPluginImplProviderImpl(
  val overridings: Map<Pair<SourceId, String>, Any>
) : PluginImplProvider {
  private val impl = PluginImplProviderImpl()

  override suspend fun getPluginImplInstance(
    callerSourceId: SourceId,
    cpInstance: ClassInstanceValue,
    className: String
  ): Any = overridings[Pair(callerSourceId, className)]
    ?: impl.getPluginImplInstance(callerSourceId, cpInstance, className)
}
