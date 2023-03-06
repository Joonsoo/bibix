package com.giyeok.bibix.interpreter

import com.giyeok.bibix.base.ClassInstanceValue
import com.giyeok.bibix.base.SourceId
import java.nio.file.Path

class FakePluginImplProvider(private val provider: (SourceId, List<Path>, String) -> Any) :
  PluginImplProvider {
  override suspend fun getPluginImplInstance(
    sourceId: SourceId,
    cps: List<Path>,
    className: String
  ): Any = this.provider(sourceId, cps, className)
}
