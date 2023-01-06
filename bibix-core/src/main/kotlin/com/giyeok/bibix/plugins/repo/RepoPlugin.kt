package com.giyeok.bibix.plugins.repo

import com.giyeok.bibix.plugins.Classes
import com.giyeok.bibix.plugins.PreloadedPlugin

val repoPlugin = PreloadedPlugin.fromScript(
  "",
  """
    def git(
      url: string,
      tag?: string,
      branch?: string,
      ref?: string,
      path: string = "",
    ): com.giyeok.bibix.plugins.root:BibixPackage = native:com.giyeok.bibix.plugins.root.Git
  """.trimIndent(),
  Classes()
)
