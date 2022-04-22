package com.giyeok.bibix.plugins.scalatest

import com.giyeok.bibix.base.*
import com.giyeok.bibix.plugins.cputils.ClassCollector
import org.scalatest.ConfigMap
import org.scalatest.`ConfigMap$`
import java.net.URLClassLoader

class Run {
  fun run(context: ActionContext): BuildRuleReturn {
    val deps = context.arguments.getValue("deps") as SetValue
    val targetCps = deps.values.flatMap { dep ->
      val depCps =
        (((dep as ClassInstanceValue).value as NamedTupleValue).getValue("cps") as SetValue)
      depCps.values.map { (it as PathValue).path }
    }

    return BuildRuleReturn.evalAndThen(
      "jvm.resolveClassPkgs",
      mapOf("classPkgs" to deps)
    ) { classPaths ->
      val cps = ((classPaths as ClassInstanceValue).value as SetValue).values.map {
        (it as PathValue).path
      }
      val classCollector = ClassCollector(targetCps, cps)

      // allTargetClasses 중에서 org.scalatest.Suite를 extend하는 것들 찾아서
      // TODO arguments에서 클래스 이름을 적어줬으면 그 클래스들만 실행하도록
      val targetClasses = classCollector.findSubclassesOf("org.scalatest.Suite")

      targetClasses.forEach { targetClass ->
        val instance = targetClass.getDeclaredConstructor().newInstance()
        val nestedSuites = targetClass.getMethod("nestedSuites").invoke(instance)
        // TODO Suite.nestedSuites도 실행해야되는듯?

        // TODO 아마도.. default value 있는 파라메터 전부 써줘야될듯?
        val executeMethod = targetClass.getMethod(
          "execute",
          java.lang.String::class.java,
          classCollector.getClass("org.scalatest.ConfigMap"),
          Boolean::class.java,
          Boolean::class.java,
          Boolean::class.java,
          Boolean::class.java,
          Boolean::class.java,
        )
        executeMethod.invoke(
          instance,
          null, `ConfigMap$`.`MODULE$`.empty(), true, false, false, false, false
        )
      }

      BuildRuleReturn.done()
    }
  }
}
