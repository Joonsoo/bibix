package com.giyeok.bibix.intellij.service

import com.giyeok.bibix.base.SourceId
import com.giyeok.bibix.graph.runner.ClassPkgRunner
import com.giyeok.bibix.interpreter.PluginImplProvider
import com.giyeok.bibix.interpreter.PluginImplProviderImpl
import com.giyeok.bibix.plugins.jvm.ClassPkg
import org.codehaus.plexus.classworlds.ClassWorld
import java.nio.file.Path

class OverridingPluginImplProviderImpl(
  val overridings: Map<Pair<SourceId, String>, Any>
): PluginImplProvider {
  private val impl = PluginImplProviderImpl()

  override suspend fun getPluginImplInstance(
    callerSourceId: SourceId,
    cps: List<Path>,
    className: String
  ): Any = overridings[Pair(callerSourceId, className)]
    ?: impl.getPluginImplInstance(callerSourceId, cps, className)
}

class OverridingClassPkgRunner(
  val overridings: Map<Pair<Int, String>, Any>,
  classWorld: ClassWorld
): ClassPkgRunner(classWorld) {
  // TODO 지금 projectId로 들어오는건 해당 build rule이 있는 project id인듯.. caller쪽을 받아야 할 것 같은데
  override fun getPluginImplInstance(callerProjectId: Int, classPkg: ClassPkg, className: String): Any {
    val overridden = overridings[Pair(callerProjectId, className)]
    if (overridden != null) {
      return overridden
    }
    return super.getPluginImplInstance(callerProjectId, classPkg, className)
  }
}
