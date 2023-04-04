package com.giyeok.bibix.plugins.file

import com.giyeok.bibix.plugins.PluginInstanceProvider
import com.giyeok.bibix.plugins.PreloadedPlugin

val filePlugin = PreloadedPlugin.fromScript(
  "com.giyeok.bibix.plugins.file",
  """
    action def copyFile(
      src: file,
      dest: path,
      overwrite: boolean = true,
    ) = native:com.giyeok.bibix.plugins.file.Copy:copyFile

    action def copyFiles(
      srcs: set<file>,
      dest: path,
      overwrite: boolean = true,
    ) = native:com.giyeok.bibix.plugins.file.Copy:copyFiles

    action def copyDirectory(
      src: directory,
      dest: directory,
      createDirectories: boolean = true,
      overwrite: boolean = true,
    ) = native:com.giyeok.bibix.plugins.file.Copy:copyDirectory

    action def clearDirectory(
      dest: directory,
    ) = native:com.giyeok.bibix.plugins.file.Clear:clearDirectory

    action def makeDirectory(
      dest: path,
    ) = native:com.giyeok.bibix.plugins.file.MakeDir:makeDirectory
  """.trimIndent(),
  PluginInstanceProvider(
    Clear::class.java,
    Copy::class.java,
    MakeDir::class.java,
  )
)
