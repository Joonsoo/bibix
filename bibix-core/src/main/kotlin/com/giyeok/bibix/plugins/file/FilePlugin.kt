package com.giyeok.bibix.plugins.file

import com.giyeok.bibix.plugins.PluginInstanceProvider
import com.giyeok.bibix.plugins.PreloadedPlugin

val filePlugin = PreloadedPlugin.fromScript(
  "com.giyeok.bibix.plugins.file",
  """
    action def clearDirectory(directory: directory)
      = native:com.giyeok.bibix.plugins.file.ClearDirectory
      
    action def copy(
      src: {file, set<file>, directory},
      dest: path,
    ) = native:com.giyeok.bibix.plugins.file.Copy
  """.trimIndent(),
  PluginInstanceProvider(
    ClearDirectory::class.java,
    Copy::class.java,
  )
)
