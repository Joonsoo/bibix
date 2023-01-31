package com.giyeok.bibix.interpreter

import com.giyeok.bibix.base.ClassInstanceValue
import com.giyeok.bibix.base.SourceId

class FakePluginClassLoader(private val provider: (SourceId, ClassInstanceValue, String) -> Any) :
  PluginClassLoader {
  override suspend fun loadPluginInstance(
    sourceId: SourceId,
    cpInstance: ClassInstanceValue,
    className: String
  ): Any = this.provider(sourceId, cpInstance, className)
}
