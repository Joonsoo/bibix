package com.giyeok.bibix.plugins

class Classes(private val classes: Map<String, Class<*>>) {
  constructor(vararg classes: Class<*>) : this(classes.associateBy { it.canonicalName })

  fun getClass(name: String): Class<*> = classes[name]!!
}
