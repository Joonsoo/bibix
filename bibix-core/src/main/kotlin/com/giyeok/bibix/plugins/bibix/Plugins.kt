package com.giyeok.bibix.plugins.bibix

import com.giyeok.bibix.base.Constants
import com.giyeok.bibix.base.*

class Plugins {
  fun build(context: BuildContext): BibixValue {
    val tag = (context.arguments.getValue("tag") as StringValue).value
    return ClassInstanceValue(
      CName(BibixRootSourceId, "GitSource"),
      NamedTupleValue(
        listOf(
          "url" to StringValue(Constants.BIBIX_GIT_URL),
          "ref" to StringValue("refs/tags/$tag"),
          "path" to StringValue("bibix-plugins"),
        )
      )
    )
  }

  fun dev(context: BuildContext): BibixValue {
    val branch = (context.arguments.getValue("branch") as StringValue).value
    return ClassInstanceValue(
      CName(BibixRootSourceId, "GitSource"),
      NamedTupleValue(
        listOf(
          "url" to StringValue(Constants.BIBIX_GIT_URL),
          "ref" to StringValue("refs/heads/$branch"),
          "path" to StringValue("bibix-plugins"),
        )
      )
    )
  }
}
