package com.giyeok.bibix.intellij.service

import com.giyeok.bibix.base.SourceId
import com.giyeok.bibix.interpreter.PluginImplProvider
import com.giyeok.bibix.interpreter.PluginImplProviderImpl
import java.nio.file.Path

class OverridingPluginImplProviderImpl(
  val overridings: Map<Pair<SourceId, String>, Any>
) : PluginImplProvider {
  private val impl = PluginImplProviderImpl()

  override suspend fun getPluginImplInstance(
    callerSourceId: SourceId,
    cps: List<Path>,
    className: String
  ): Any = overridings[Pair(callerSourceId, className)]
    ?: impl.getPluginImplInstance(callerSourceId, cps, className)
}
