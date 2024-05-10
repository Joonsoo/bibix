# Bibix

A language-neutral, declarative, and extensible software building tool.


## Getting started

### 1. Install

  * The instruction here should work on linux and mac.
  * Bibix needs JVM to run. Please make sure you have the latest version of JRE or JDK on your computer.
  * Create a bibix installation directory. I recommend to create a new folder named "bibix" under your home directory.
  * Download the `bibix-0.8.1-all.jar` from the [release page](https://github.com/Joonsoo/bibix/releases/tag/0.8.1) to the installation directory.
  * Add a file named `bibix` in the installation directory (where the jar file is located) and add:
    * `java -jar /Users/joonsoo/Documents/apps/bibix/bibix-0.8.1-all.jar "$@"`
  * Make the `bibix` file you just created executable by running `chmod u+x bibix`
  * Make the `bibix` script accessible from anywhere by adding the installation directory to PATH variable in `~/.bashrc` or `~/.zprofile` file.

### 2. Add `build.bbx` file to your project

  * When you run the bibix interpreter on a directory, bibix first tries to find the `build.bbx` file in the directory.
  * `build.bbx` file defines the build rules of your project.
  * The following shows a simple project with java source files structured like maven or gradle project, i.e. source files are located under `src/main/java` folder.

```
from bibix.plugins import java
import maven

main = java.library(
  srcs = glob("src/main/java/**.java"),
  deps = [
    maven.artifact("com.google.code.gson", "gson", "2.10.1"),
    maven.artifact("com.google.guava", "guava", "32.1.3-jre"),
  ]
)

test = java.library(
  srcs = glob("src/test/java/**.java"),
  deps = [
    main,
    maven.artifact("org.junit.jupiter", "junit-jupiter-api", "5.8.2"),
    maven.artifact("com.google.truth", "truth", "1.1.3"),
  ]
)
```

  * This project defines two targets: `main` and `test`.
    * `main` is the java library that is compiled from the .java files under src/main/java and has 2 dependencies of GSON and Guava from maven central repository.
    * `test` is the java library that is compiled from the .java files under src/test/java and has 2 dependenceis of `main` and junit from maven central repository.
  * The names `main` and `test` do not have specific meaning for bibix, that is, bibix treats `main` and `test` equally.
    * `main` and `test` are common names you may want to use in your projects.
    * Because bibix does not assume that these modules are related in any ways, you need to explicitly add `main` to the list of `deps` of `test`.

### 3. Build

  * To build the `main` target, simply run `bibix main` in the project directory.
  * After long list of build progress log, the build result will be shown like this:
    * `main: com.giyeok.bibix.plugins.jvm::ClassPkg(cpinfo=com.giyeok.bibix.plugins.jvm::ClassesInfo(classDirs={dir(<projectroot>/bbxbuild/objects/4be6d79d1b7136609880c66cc25efc43cda017ca)}, resDirs={}, srcs={file(...)}), deps={...}, nativeLibDirs={}, ...)`
    * This line shows the build result value of the target `main` is an instance of `ClassPkg` class and the instance has following fields.
    * This line can be very long as it contains all information about its dependencies, including transitive dependencies.
  * You can find the compiled class files under the directory shown after `classDirs` in the line. In my case, the compiled .class files are under `<projectroot/bbxbuild/objects/4be6d79d1b7136609880c66cc25efc43cda017ca` directory.
  * The symbolic link to the directory is also created at `bbxbuild/outputs/main`.

### 4. Make jar file

  * Bibix does not implicitly create other targets or actions.
  * Therefore, if you want to make jar files, you need to explicitly define the rules for making jar files. The following snippet shows how to define such rules to generate jar file.

```
from bibix.plugins import jar

uberJar = jar.uberJar([main], "main-all.jar")
```

  * Now you can simply run `bibix uberJar` to create the uber jar file for your `main` target.
    * It may take some time to run the command, because it will need to download kotlin stdlib to compile the `jar` plugin.
    * It will be faster after the first run, since the downloaded kotlin stdlib (and other maven dependencies as well) are cached in `bbxbuild/shared` directory.
  * After the build is done, bibix will print out the last line like the following:
  * `uberJar: file(<projectroot>/bbxbuild/objects/730c73fe939217ce81f9efdf55eeed620ebae454/main-all.jar)`
    * This means the build result value of `uberJar` target is the file `bbxbuild/objects/730c73fe939217ce81f9efdf55eeed620ebae454/main-all.jar`, which is the generated uber jar file.
  * The symbolic link to the directory will be created at `bbxbuild/outputs/uberJar`, so you can access to the generated uber jar at `<projectroot>/bbxbuild/outputs/uberJar/main-all.jar`.

### 5. (WIP) Run junit tests

  * This section is a WIP. The instructions in this section may not work as documented here.
  * Again, bibix does not implicitly add targets or actions. You need to explicitly declare an "action" to run your unit tests.

```
from bibix.plugins import junit5

action runTests {
  junit5.runTests([test])
}
```

  * Now, you can run the tests in `test` target by running `bibix runTests`.
  * `runTests` is an "action", while `main`, `test`, and `uberJar` were "target"s. A target must create a result value and may be reused if no argument values or input files have been changed. On the other hand, an action does not return a value and will always be executed whenever the user runs the action.
  * The test result will be saved in the `bbxbuil/logs` folder, which stores the log files created while running bibix.
  * Find and open the last log. If the test was successful, the file should contain the lines like the following:

```
  - level: INFO
    time: 2024-05-09T04:51:49.541Z
    message: "Test summary: \n"
             "Test run finished after 12 ms\n"
             "[         1 containers found      ]\n"
             "[         0 containers skipped    ]\n"
             "[         1 containers started    ]\n"
             "[         0 containers aborted    ]\n"
             "[         1 containers successful ]\n"
             "[         0 containers failed     ]\n"
             "[         0 tests found           ]\n"
             "[         0 tests skipped         ]\n"
             "[         0 tests started         ]\n"
             "[         0 tests aborted         ]\n"
             "[         0 tests successful      ]\n"
             "[         0 tests failed          ]\n"
             "\n"
             ""
```


## See also
  * [intro.md](intro.md)
    * More detailed and informal (and incomplete) introduction about bibix. Written in Korean.
  * [blog](https://giyeok.com/categories.html#bibix)
    * Thoughts on the build tools and design decisions of bibix. Written in Korean.
  * [bibix-plugins](https://github.com/Joonsoo/bibix-plugins)
    * Includes a few common plugins, including java, ktjvm, jar, protobuf, grpc, tar, xz, gzip, and mustache.
    * You can use the plugins from the repository by simply adding a line like `from bibix.plugins import ktjvm`.
    * `bibix.plugins` points to the github repository.
  * [jparser](https://github.com/Joonsoo/jparser)
    * The grammar of the bibix build script is defined [here](https://github.com/Joonsoo/bibix/blob/main/grammar/bibix.cdg).
    * The grammar definition is written in CDG format. For more information, see [jparser](https://github.com/Joonsoo/jparser) project.
