package com.giyeok.bibix.plugins.bibix

import com.giyeok.bibix.base.Constants
import com.giyeok.bibix.plugins.Classes
import com.giyeok.bibix.plugins.PreloadedPlugin

val dollar = "\$"
val bibixPlugin = PreloadedPlugin.fromScript(
  "com.giyeok.bibix.plugins.bibix",
  """
    import jvm
    import curl
    
    class Env(os: OS, arch: Arch)
    
    super class OS { Linux, OSX, Windows }
    class Linux()
    class OSX()
    class Windows()
    
    enum Arch {
      unknown,
      x86,
      x86_64,
      aarch_64,
    }
    
    var buildingBibixVersion: string = "${Constants.BUILDING_BIBIX_VERSION}"
    var bibixVersion: string = "${Constants.BIBIX_VERSION}"
    
    def base(
      classpath: path = curl.download("https://github.com/Joonsoo/bibix/releases/download/${dollar}buildingBibixVersion/bibix-base-${dollar}buildingBibixVersion.jar")
    ): jvm.ClassPkg = native:com.giyeok.bibix.plugins.bibix.Base
    
    class BibixProject(projectRoot: directory, scriptName?: string)

    def git(
      url: string,
      tag?: string,
      branch?: string,
      ref?: string,
      path: string = "",
      scriptName?: string,
    ): BibixProject = native:com.giyeok.bibix.plugins.bibix.Git

    def plugins(
      tag: string = bibixVersion
    ): BibixProject = native:com.giyeok.bibix.plugins.bibix.Plugins
    
    def devPlugins(
      branch: string = "main"
    ): BibixProject = native:com.giyeok.bibix.plugins.bibix.Plugins:dev
    
    class RuleImplTemplate(implClass: file, interfaceClass: file) {
      as list<file> = [this.implClass, this.interfaceClass]
    }
    
    def genRuleImplTemplateKt(
      rules: set<buildrule>,
      // TODO (type, string)은 뭐하려고 했던거지..? rename인가?
      types: set<{type, (type, string)}>,
      implName: string,
      implInterfaceName: string,
    ): RuleImplTemplate = native:com.giyeok.bibix.plugins.bibix.GenRuleImplTemplateKt
    
    def genClassesKt(
      types: set<type>,
      packageName: string,
      fileName: string = "BibixClasses.kt",
      outerClassName?: string,
    ): file = native:com.giyeok.bibix.plugins.bibix.GenClassesKt
  """.trimIndent(),
  Classes(
    Base::class.java,
    Git::class.java,
    Plugins::class.java,
    GenRuleImplTemplateKt::class.java,
    GenClassesKt::class.java,
  )
)
