package com.giyeok.bibix.interpreter

import com.giyeok.bibix.base.ClassInstanceValue
import com.giyeok.bibix.base.SourceId

class FakePluginImplProvider(private val provider: (SourceId, ClassInstanceValue, String) -> Any) :
  PluginImplProvider {
  override suspend fun getPluginImplInstance(
    sourceId: SourceId,
    cpInstance: ClassInstanceValue,
    className: String
  ): Any = this.provider(sourceId, cpInstance, className)
}
