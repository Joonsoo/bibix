package com.giyeok.bibix.plugins

interface ClassProvider {
  fun getClass(name: String): Class<*>
}

class Classes(private val classes: Map<String, Class<*>>) : ClassProvider {
  constructor(vararg classes: Class<*>) : this(classes.associateBy { it.canonicalName })

  override fun getClass(name: String): Class<*> = classes[name]!!
}
