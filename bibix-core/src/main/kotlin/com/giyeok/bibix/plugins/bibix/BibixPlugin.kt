package com.giyeok.bibix.plugins.bibix

import com.giyeok.bibix.base.Constants
import com.giyeok.bibix.plugins.Classes
import com.giyeok.bibix.plugins.PreloadedPlugin

val bibixPlugin = PreloadedPlugin.fromScript(
  "com.giyeok.bibix.plugins.bibix",
  """
    import jvm
    import curl
    
    version = "${Constants.BIBIX_VERSION}"
    
    def base(
      classpath: path = curl.download("https://github.com/Joonsoo/bibix/releases/download/${Constants.BIBIX_VERSION}/bibix-base-${Constants.BIBIX_VERSION}.jar")
    ): jvm.ClassPkg = native:com.giyeok.bibix.plugins.bibix.Base
    
    def plugins(
      tag: string = version
    ): BibixProject = native:com.giyeok.bibix.plugins.bibix.Plugins
    
    def devPlugins(
      branch: string = "main"
    ): BibixProject = native:com.giyeok.bibix.plugins.bibix.Plugins:dev
    
    class RuleImplTemplate(implClass: file, interfaceClass: file) {
      as list<file> = [this.implClass, this.interfaceClass]
    }
    
    def genRuleImplTemplateKt(
      rules: set<buildrule>,
      implName: string,
      implInterfaceName: string,
    ): RuleImplTemplate = native:com.giyeok.bibix.plugins.bibix.GenRuleImplTemplateKt
    
    def genTypesKt(
      types: set<type>,
      packageName: string,
      fileName: string = "BibixTypes.kt",
      outerClassName?: string,
    ): file = native:com.giyeok.bibix.plugins.bibix.GenTypesKt
  """.trimIndent(),
  Classes(
    Base::class.java,
    Plugins::class.java,
    GenRuleImplTemplateKt::class.java,
    GenTypesKt::class.java,
  )
)
