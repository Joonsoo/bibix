package com.giyeok.bibix.plugins.jvm

import com.giyeok.bibix.plugins.PluginInstanceProvider
import com.giyeok.bibix.plugins.PreloadedPlugin

val jvmPlugin = PreloadedPlugin.fromScript(
  "com.giyeok.bibix.plugins.jvm",
  """
    class ClassPaths(
      cps: set<path>,
      // TODO coercion할 때 data class field default value 처리
      runtimeCps: set<path> = [],
      nativeLibDirs: set<path> = [],
    )

    class ClassPkg(
      origin: ClassOrigin,
      cpinfo: CpInfo,
      deps: set<ClassPkg>,
      runtimeDeps: set<ClassPkg> = [],
      nativeLibDirs: set<directory> = [],
    ) {
      // resources는 classpath의 일부로 취급
      as ClassPaths = resolveClassPkgs([this])
    }

    super class ClassOrigin {MavenDep, LocalLib, LocalBuilt}

    class MavenDep(repo: string, group: string, artifact: string, version: string, classifier: string = "")
    // TODO LocalBuilt는 뭐가 들어가야되지..? objHash, builder name
    class LocalLib(path: path)
    class LocalBuilt(objHash: string, builderName: string)

    // bibix.genClassesKt 에서 union type을 body로 갖는 class는 sealed class로 해서 상위 클래스로 생성됨
    // TODO 내장 플러그인에 대해서도 GenRuleImplTemplateKt 기능을 지원하고, 플러그인을 이걸 사용해서 작성하기
    // -> 그래야 그나마 빌드 스크립트나 플러그인이 바뀌었을 때 컴파일 타임에 확인 가능
    // -> 다만 ktjvm만 예외적으로 손수 작업해야할 듯.. 코틀린이 없는 세계에서 동작하는 유일한 플러그인이기 때문
    super class CpInfo {JarInfo, ClassesInfo}

    // sourceJar는 optional
    class JarInfo(jar: file, sourceJar?: file)
    // resource는 classDir의 일부로 넣으면 됨.
    // -> 그런데 한 폴더에 한 자바 모듈의 리소스 외에 다른게 같이 있으면 어쩌지? dest directory로 리소스 파일만 복사하든지..
    // srcs는 optional. optional은 typescript에서처럼 union type으로 처리.
    class ClassesInfo(classDirs: set<directory>, resDirs: set<directory>, srcs?: set<file>)

    def resolveClassPkgs(
      classPkgs: set<ClassPkg>
    ): ClassPaths = native:com.giyeok.bibix.plugins.jvm.ResolveClassPkgs

    def lib(
      path: path,
      deps: set<ClassPkg> = [],
      runtimeDeps: set<ClassPkg> = [],
      srcJar?: file,
      nativeLibDirs: set<directory> = [],
    ): ClassPkg = native:com.giyeok.bibix.plugins.jvm.Lib

    action def run(
      deps: set<ClassPkg>,
      mainClass: string,
      jvmPath: string = "java",
      args: list<string> = [],
    ) = native:com.giyeok.bibix.plugins.jvm.Run
  """.trimIndent(),
  PluginInstanceProvider(
    Lib::class.java,
    Run::class.java,
    ResolveClassPkgs::class.java,
  ),
)
