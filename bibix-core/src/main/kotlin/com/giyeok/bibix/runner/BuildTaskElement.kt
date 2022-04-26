package com.giyeok.bibix.runner

import kotlin.coroutines.CoroutineContext

data class BuildTaskElement(val buildTask: BuildTask) : CoroutineContext.Element {
  object Key : CoroutineContext.Key<BuildTaskElement>

  override val key: CoroutineContext.Key<*> get() = Key
}
