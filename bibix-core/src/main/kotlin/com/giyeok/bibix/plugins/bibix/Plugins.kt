package com.giyeok.bibix.plugins.bibix

import com.giyeok.bibix.base.Constants
import com.giyeok.bibix.base.*

class Plugins {
  fun build(context: BuildContext): BibixValue {
    val tag = (context.arguments.getValue("tag") as StringValue).value
    val ref = "refs/tags/$tag"
//    val ref = "HEAD"
    return ClassInstanceValue(
      "",
      "GitSource",
      mapOf(
        "url" to StringValue(Constants.BIBIX_GIT_URL),
        "ref" to StringValue(ref),
        "path" to StringValue("bibix-plugins"),
      )
    )
  }

  fun dev(context: BuildContext): BibixValue {
    val branch = (context.arguments.getValue("branch") as StringValue).value
    val ref = "refs/heads/$branch"
//    val ref = "HEAD"
    return ClassInstanceValue(
      "",
      "GitSource",
      mapOf(
        "url" to StringValue(Constants.BIBIX_GIT_URL),
        "ref" to StringValue(ref),
        "path" to StringValue("bibix-plugins"),
      )
    )
  }
}
