package com.giyeok.bibix.plugins.maven

import com.giyeok.bibix.plugins.PluginInstanceProvider
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
    class MavenRepo(name: string, repoType: RepoType, url: string)
    
    class MavenArtifactName(group: string, artifact: string, version?: string, classifier?: string)
    var excludes: set<MavenArtifactName> = []
    
    var defaultRepos: list<MavenRepo> = [
      MavenRepo("Maven central", RepoType.remote, "http://repo.maven.apache.org/maven2/")
    ]
    
    def artifact(
      group: string,
      artifact: string,
      version?: string,
      classifier?: string,
      scope: ScopeType = ScopeType.compile,
      extension: string = "jar",
      repos: list<MavenRepo> = defaultRepos,
      excludes: set<MavenArtifactName> = excludes,
    ): jvm.ClassPkg = native:com.giyeok.bibix.plugins.maven.Artifact
    
    action def deploy(
      target: jvm.Jar,
      group: string,
      artifact: string,
      version: string,
      gpgKey?: file,
    ) = native:com.giyeok.bibix.plugins.maven.Deploy
  """.trimIndent(),
  PluginInstanceProvider(
    Artifact::class.java,
    Deploy::class.java,
  ),
)
