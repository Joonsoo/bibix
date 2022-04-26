package com.giyeok.bibix.plugins.maven

import com.giyeok.bibix.plugins.Classes
import com.giyeok.bibix.plugins.BibixPlugin

val mavenPlugin = BibixPlugin.fromScript(
  """
    import jvm
    
    enum RepoType {
      local, remote
    }
    class MavenRepo = (repoType: RepoType, url: string)
    
    arg defaultRepos: list<MavenRepo> = [
      (RepoType.remote, "http://repo.maven.apache.org/maven2/")
    ]
    
    def dep(
      group: string,
      artifact: string,
      version?: string,
      extension: string = "jar",
      repos: list<MavenRepo> = defaultRepos,
    ): jvm.ClassPkg = native:com.giyeok.bibix.plugins.maven.Dep
    
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
