package com.giyeok.bibix.test

import com.giyeok.bibix.base.ActionContext

class AA {
  fun run(context: ActionContext) {
    println("Hello!")
    context.progressLogger.logInfo("Hello!! from AA")
  }
}
