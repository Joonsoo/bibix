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
      val cps = ((classPaths as ClassInstanceValue).value as SetValue).values.map {
        (it as PathValue).path
      }

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
