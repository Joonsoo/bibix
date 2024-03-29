package com.giyeok.bibix.plugins.bibix

import com.giyeok.bibix.base.Constants
import com.giyeok.bibix.base.*

class Plugins {
  fun build(context: BuildContext): BuildRuleReturn {
    val tag = (context.arguments.getValue("tag") as StringValue).value
    val ref = "refs/tags/$tag"
//    val ref = "HEAD"
    return BuildRuleReturn.eval(
      "git",
      mapOf(
        "url" to StringValue(Constants.BIBIX_GIT_URL),
        "ref" to StringValue(ref),
      )
    )
  }

  fun dev(context: BuildContext): BuildRuleReturn {
    val branch = (context.arguments.getValue("branch") as StringValue).value
    val ref = "refs/heads/$branch"
//    val ref = "HEAD"
    return BuildRuleReturn.eval(
      "git",
      mapOf(
        "url" to StringValue(Constants.BIBIX_GIT_URL),
        "ref" to StringValue(ref),
      )
    )
  }
}
