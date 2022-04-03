package com.giyeok.bibix.plugins.maven

import com.giyeok.bibix.plugins.Classes
import com.giyeok.bibix.runner.BibixPlugin

val mavenPlugin = BibixPlugin.fromScript(
  """
    import jvm
    
    enum RepoType {
      local, remote
    }
    class Resolver = (repoType: RepoType, url: string)
    
    arg defaultResolvers: list<Resolver> = [
      (RepoType.remote, "http://repo.maven.apache.org/maven2/")
    ]
    
    def dep(
      group: string,
      artifact: string,
      version?: string,
      extension: string = "jar",
      resolvers: list<Resolver> = defaultResolvers,
    ): jvm.Classes = native:com.giyeok.bibix.plugins.maven.Dep
    
    action def deploy(
      target: jvm.Jar,
      group: string,
      artifact: string,
      version: string,
      gpgKey?: file,
    ) = native:com.giyeok.bibix.plugins.maven.Deploy
  """.trimIndent(),
  Classes(
    Dep::class.java,
    Deploy::class.java,
  ),
)
