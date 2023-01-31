package com.giyeok.bibix.interpreter

import com.giyeok.bibix.base.ClassInstanceValue

class FakePluginClassLoader(private val provider: (ClassInstanceValue, String) -> Any) :
  PluginClassLoader {
  override suspend fun loadPluginInstance(
    cpInstance: ClassInstanceValue,
    className: String
  ): Any = this.provider(cpInstance, className)
}
