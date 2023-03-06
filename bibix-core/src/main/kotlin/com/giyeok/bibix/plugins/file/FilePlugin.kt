package com.giyeok.bibix.plugins.file

import com.giyeok.bibix.plugins.PluginInstanceProvider
import com.giyeok.bibix.plugins.PreloadedPlugin

val filePlugin = PreloadedPlugin.fromScript(
  "com.giyeok.bibix.plugins.file",
  """
    action def copyFile(
      src: {file, set<file>},
      dest: path,
      overwrite: boolean = true,
    ) = native:com.giyeok.bibix.plugins.file.Copy:copyFile

    action def copyDirectory(
      src: directory,
      dest: directory,
      createDirectories: boolean = false,
      overwrite: boolean = true,
    ) = native:com.giyeok.bibix.plugins.file.Copy:copyDirectory

    action def clearDirectory(
      dest: directory,
    ) = native:com.giyeok.bibix.plugins.file.Clear:clearDirectory
  """.trimIndent(),
  PluginInstanceProvider(
    Clear::class.java,
    Copy::class.java,
  )
)
