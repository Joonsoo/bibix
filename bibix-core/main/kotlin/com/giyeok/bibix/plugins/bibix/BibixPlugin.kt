package com.giyeok.bibix.plugins.bibix

import com.giyeok.bibix.base.Constants
import com.giyeok.bibix.plugins.PluginInstanceProvider
import com.giyeok.bibix.plugins.PreloadedPlugin

val bibixPlugin = PreloadedPlugin.fromScript(
  "com.giyeok.bibix.plugins.bibix",
  """
    import jvm
    import curl
    
    version = "${Constants.BIBIX_VERSION}"
    
    baseDownload = curl.download("https://github.com/Joonsoo/bibix/releases/download/${Constants.BIBIX_VERSION}/bibix-base-${Constants.BIBIX_VERSION}-all.jar")
    base = jvm.ClassPkg(origin=jvm.LocalLib(baseDownload), cpinfo=jvm.JarInfo(baseDownload, none), deps=[])
    
    plugins = git("${Constants.BIBIX_PLUGINS_GIT_URL}")
    devPlugins = git("${Constants.BIBIX_PLUGINS_GIT_URL}", branch="main")
    
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
      generateRelatedTypes: boolean = true,
      fileName: string = "BibixTypes.kt",
      outerClassName?: string,
    ): file = native:com.giyeok.bibix.plugins.bibix.GenTypesKt
  """.trimIndent(),
  PluginInstanceProvider(
    Base::class.java,
    Plugins::class.java,
    GenRuleImplTemplateKt::class.java,
    GenTypesKt::class.java,
  )
)
