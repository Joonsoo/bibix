package com.giyeok.bibix.graph.runner

interface PluginOverrides {
  fun getOverrideInstance(callerProjectId: Int, impl: BuildTaskResult.BuildRuleImpl): Any?
}

object NoPluginOverrides: PluginOverrides {
  override fun getOverrideInstance(
    callerProjectId: Int,
    impl: BuildTaskResult.BuildRuleImpl
  ): Any? = null
}

class PluginOverridesImpl(val overrides: Map<Pair<Int, String>, Any>): PluginOverrides {
  override fun getOverrideInstance(
    callerProjectId: Int,
    impl: BuildTaskResult.BuildRuleImpl
  ): Any? =
    when (impl) {
      is BuildTaskResult.BuildRuleImpl.NativeImpl -> null
      is BuildTaskResult.BuildRuleImpl.NonNativeImpl -> {
        val overriding = overrides[Pair(callerProjectId, impl.implClassName)]
        overriding
      }
    }
}
