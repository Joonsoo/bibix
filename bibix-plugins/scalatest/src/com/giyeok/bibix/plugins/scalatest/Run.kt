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
      val targetClasses0 = classCollector.findSubclassesOf("org.scalatest.Suite")
      val targetClasses = collectAllSubclasses(classCollector, targetClasses0, mutableMapOf())

      targetClasses.forEach { targetClass ->
        val instance = targetClass.getDeclaredConstructor().newInstance()
        val nestedSuites = targetClass.getMethod("nestedSuites").invoke(instance)
        println(nestedSuites)
        // TODO Suite.nestedSuites도 실행해야되는듯?

        val configMapClass = classCollector.getClass("org.scalatest.ConfigMap")

        val executeMethod = targetClass.getMethod(
          "execute",
          java.lang.String::class.java,
          configMapClass,
          Boolean::class.java,
          Boolean::class.java,
          Boolean::class.java,
          Boolean::class.java,
          Boolean::class.java,
        )

        val configMapDollar = classCollector.getClass("org.scalatest.ConfigMap$")
        val configMapModule = configMapDollar.getDeclaredField("MODULE$").get(null)
        val emptyConfigMap = configMapDollar.getDeclaredMethod("empty").invoke(configMapModule)

        executeMethod.invoke(instance, null, emptyConfigMap, true, false, false, false, false)
      }

      BuildRuleReturn.done()
    }
  }

  private fun collectAllSubclasses(
    classCollector: ClassCollector,
    queue: List<Class<*>>,
    cc: MutableMap<String, Class<*>>
  ): List<Class<*>> {
    if (queue.isEmpty()) {
      return cc.entries.toList().toList().sortedBy { it.key }.map { it.value }
    }
    val cls = queue.first()
    val rest = queue.drop(1)
    if (cls.declaredConstructors.any { it.parameterCount == 0 } &&
      cls.methods.any { it.name == "execute" }) {
      cc[cls.canonicalName] = cls
    }
    val subs = classCollector.findSubclassesOf(cls.canonicalName).filter {
      !cc.containsKey(it.canonicalName)
    }
    return collectAllSubclasses(classCollector, rest + subs, cc)
  }
}
