package com.giyeok.bibix.plugins.bibix

import com.giyeok.bibix.plugins.Classes
import com.giyeok.bibix.runner.BibixPlugin

val dollar = "\$"
val bibixPlugin = BibixPlugin.fromScript(
  """
    import jvm
    import curl
    
    arg bibixVersion: string = "0.0.2"
    
    def base(
      classpath: path = curl.download("https://github.com/Joonsoo/bibix/releases/download/${dollar}bibixVersion/bibix-base-${dollar}bibixVersion.jar")
    ): jvm.ClassPkg = native:com.giyeok.bibix.plugins.bibix.Base
    
    // TODO bibix.plugins(tag="1.2.0") => GitSource 반환
    def plugins(
      tag: string = bibixVersion
    ): jvm.ClassPkg = native:com.giyeok.bibix.plugins.bibix.Plugins
    
    def genRuleImplTemplateKt(
      rule: buildrule,
      types: set<{type, (type, string)}>,
      implName: string,
      implInterfaceName: string,
    ): file = native:com.giyeok.bibix.plugins.bibix.GenRuleImplTemplateKt
  """.trimIndent(),
  Classes(
    Base::class.java,
    Plugins::class.java,
    GenRuleImplTemplateKt::class.java,
  )
)
