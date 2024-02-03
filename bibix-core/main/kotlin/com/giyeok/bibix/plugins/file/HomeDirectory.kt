package com.giyeok.bibix.plugins.file

import com.giyeok.bibix.base.BuildContext
import com.giyeok.bibix.base.DirectoryValue
import kotlin.io.path.Path
import kotlin.io.path.absolute

class HomeDirectory {
  fun build(context: BuildContext): DirectoryValue {
    val homeDirectory = System.getProperty("user.home")!!
    return DirectoryValue(Path(homeDirectory).absolute())
  }
}
