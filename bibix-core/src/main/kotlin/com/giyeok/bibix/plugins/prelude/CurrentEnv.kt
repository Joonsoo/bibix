package com.giyeok.bibix.plugins.prelude

import com.giyeok.bibix.base.*

class CurrentEnv {
  fun build(context: BuildContext): BibixValue {
    val env = context.buildEnv
    // TODO
    return NClassInstanceValue(
      "Env", mapOf(
        "os" to NClassInstanceValue("Linux", mapOf()),
        "arch" to EnumValue("com.giyeok.bibix.prelude", "Arch", "x86_64"),
      )
    )
  }
}
