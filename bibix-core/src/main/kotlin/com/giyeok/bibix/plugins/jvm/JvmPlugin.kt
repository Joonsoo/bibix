package com.giyeok.bibix.plugins.jvm

import com.giyeok.bibix.plugins.Classes
import com.giyeok.bibix.runner.BibixPlugin

val jvmPlugin = BibixPlugin.fromScript(
  """
    class ClassPaths = set<path>
    class Classes extends ClassPaths = (classes: set<path>, deps: set<path>) {
      as ClassPaths = this.classes + this.deps
    }
    class ExecutableClasses extends ClassPaths = (cps: ClassPaths, mainClass: string) {
      // action run(fork: boolean, args: list<string>) =
      //   native:com.giyeok.bibix.plugins.jvm.RunExecutableClasses
      as ClassPaths = this.cps
    }
    class Jar extends ClassPaths = file {
      as ClassPaths = [this]
    }
    class ExecutableJar extends Jar = file {
      // action run(fork: boolean, args: list<string>) =
      //   native:com.giyeok.bibix.plugins.jvm.RunExecutableJar
    }

    def lib(path: path): ClassPaths = native:com.giyeok.bibix.plugins.jvm.Lib

    def jar(
      deps: set<Classes>,
      jarFileName: string = "bundle.jar",
    ): Jar = native:com.giyeok.bibix.plugins.jvm.Jar:jar

    def uberJar(
      deps: set<ClassPaths>,
      jarFileName: string = "bundle.jar",
    ): Jar = native:com.giyeok.bibix.plugins.jvm.UberJar:uberJar

    def executableUberJar(
      deps: set<ClassPaths>,
      mainClass: string,
      jarFileName: string = "bundle.jar",
    ): ExecutableJar = native:com.giyeok.bibix.plugins.jvm.Jar:executableUberJar

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
  ),
)
