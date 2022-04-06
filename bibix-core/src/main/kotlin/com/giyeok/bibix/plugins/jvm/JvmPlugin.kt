package com.giyeok.bibix.plugins.jvm

import com.giyeok.bibix.plugins.Classes
import com.giyeok.bibix.runner.BibixPlugin

val jvmPlugin = BibixPlugin.fromScript(
  """
    class ClassPaths = set<path>
    
    class ClassPkg extends ClassPaths =
      (origin: ClassOrigin, cps: set<path>, deps: set<ClassPkg>) {
      as ClassPaths = flattenClassPkg(this)
    }
    
    class ClassOrigin = {MavenDep, LocalBuilt, LocalLib}
    
    class MavenDep = (repo: string, group: string, artifact: string, version: string)
    // TODO LocalBuilt는 뭐가 들어가야되지..?
    class LocalBuilt = (desc: string)
    class LocalLib = (path: path)
    
    def flattenClassPkg(
      classPkg: ClassPkg
    ): ClassPaths = native:com.giyeok.bibix.plugins.jvm.FlattenClassPkg
    
    def resolveClassPkgs(
      classPkgs: set<ClassPkg>
    ): ClassPaths = native:com.giyeok.bibix.plugins.jvm.ResolveClassPkgs
    
    def lib(path: path): ClassPkg = native:com.giyeok.bibix.plugins.jvm.Lib
    
    def jar(
      deps: set<ClassPkg>,
      jarFileName: string = "bundle.jar",
    ): file = native:com.giyeok.bibix.plugins.jvm.Jar:jar
    
    def uberJar(
      deps: set<ClassPkg>,
      jarFileName: string = "bundle.jar",
    ): file = native:com.giyeok.bibix.plugins.jvm.UberJar:uberJar
    
    def executableUberJar(
      deps: set<ClassPkg>,
      mainClass: string,
      jarFileName: string = "bundle.jar",
    ): file = native:com.giyeok.bibix.plugins.jvm.Jar:executableUberJar
    
    action def run(
      deps: set<ClassPaths>,
      mainClass: string,
      jvmPath: string = "java",
      args: list<string> = [],
    ) = native:com.giyeok.bibix.plugins.jvm.Run
  """.trimIndent(),
  Classes(
    Lib::class.java,
    Jar::class.java,
    Run::class.java,
    FlattenClassPkg::class.java,
    ResolveClassPkgs::class.java,
  ),
)
