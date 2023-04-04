package com.giyeok.bibix.plugins.file

import com.giyeok.bibix.base.ActionContext
import com.giyeok.bibix.base.PathValue
import kotlin.io.path.createDirectories

class MakeDir {
  fun makeDirectory(context: ActionContext) {
    val dest = (context.arguments.getValue("dest") as PathValue).path

    dest.createDirectories()
  }
}
