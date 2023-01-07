package com.giyeok.bibix.plugins.root

import com.giyeok.bibix.base.*

class Git {
  // TODO buildscript name 지정할 수 있도록?
  fun build(context: BuildContext): BibixValue {
    val ref = (context.arguments["ref"] as? StringValue)?.value
    val branch = (context.arguments["branch"] as? StringValue)?.value
    val tag = (context.arguments["tag"] as? StringValue)?.value
    val path = context.arguments.getValue("path") as StringValue

    val refSpec = when {
      ref != null -> {
        check(branch == null && tag == null)
        ref
      }

      branch != null -> {
        check(tag == null)
        "refs/heads/$branch"
      }

      tag != null -> {
        "refs/tags/$tag"
      }

      else -> "refs/heads/main"
    }
    return ClassInstanceValue(
      "",
      "GitSource",
      mapOf(
        "url" to context.arguments.getValue("url"),
        "ref" to StringValue(refSpec),
        "path" to path,
      )
    )
  }
}
