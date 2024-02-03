package com.giyeok.bibix.plugins

class PluginInstanceProvider(private val instances: Map<String, Any>) {
  constructor(vararg classes: Class<*>) : this(
    classes.associate { it.canonicalName to it.getDeclaredConstructor().newInstance() })

  fun add(className: String, instance: Any) =
    PluginInstanceProvider(instances + (className to instance))

  fun getInstance(name: String): Any =
    instances[name] ?: throw IllegalStateException("Cannot find class $name")
}
