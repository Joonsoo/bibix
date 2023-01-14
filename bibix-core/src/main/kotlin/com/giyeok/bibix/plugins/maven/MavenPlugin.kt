package com.giyeok.bibix.plugins.maven

import com.giyeok.bibix.plugins.Classes
import com.giyeok.bibix.plugins.PreloadedPlugin

val mavenPlugin = PreloadedPlugin.fromScript(
  "com.giyeok.bibix.plugins.maven",
  """
    import jvm
    
    enum RepoType {
      local, remote
    }
    enum ScopeType {
      compile, test
    }
    class MavenRepo(repoType: RepoType, url: string)
    
    var defaultRepos: list<MavenRepo> = [
      (RepoType.remote, "http://repo.maven.apache.org/maven2/")
    ]
    
    def dep(
      group: string,
      artifact: string,
      version?: string,
      scope: ScopeType = ScopeType.compile,
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
