package com.giyeok.bibix.plugins.file

import com.giyeok.bibix.base.ActionContext
import com.giyeok.bibix.base.DirectoryValue
import java.nio.file.Path
import kotlin.io.path.deleteExisting
import kotlin.io.path.listDirectoryEntries

class Clear {
  fun clearDirectory(context: ActionContext) {
    val dest = (context.arguments.getValue("dest") as DirectoryValue).directory

    fun clear(path: Path) {
      path.listDirectoryEntries().forEach {
        clear(it)
        it.deleteExisting()
      }
    }
    clear(dest)
  }
}
