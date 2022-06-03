package com.giyeok.bibix.plugins.junit5

import com.giyeok.bibix.base.*
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder
import org.junit.platform.launcher.core.LauncherFactory
import org.junit.platform.launcher.listeners.SummaryGeneratingListener

class Run {
  fun run(context: ActionContext): BuildRuleReturn {
    val deps = context.arguments.getValue("deps")
    return BuildRuleReturn.evalAndThen(
      "resolveClassPkgs",
      mapOf("classPkgs" to deps)
    ) { classPaths ->
      val classPathsValue = (classPaths as DataClassInstanceValue).value as NamedTupleValue
      val cps = (classPathsValue.getValue("cps") as SetValue).values.map {
        (it as PathValue).path
      }
      val res = classPathsValue.getValue("res") as SetValue // set<path>

      val launcher = LauncherFactory.create()
      val discoveryRequest = LauncherDiscoveryRequestBuilder.request()
        .selectors().build()
      val testPlan = launcher.discover(discoveryRequest)

      launcher.registerTestExecutionListeners(object : SummaryGeneratingListener() {
      })
      launcher.execute(discoveryRequest)

      BuildRuleReturn.done()
    }
  }
}
