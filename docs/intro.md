1. bibix의 특징
  - bibix를 만들게 된 동기
    - maven, gradle, sbt vs blaze, bazel
    - 자바로 프로젝트를 하면서 별다른 비판 의식 없이 maven을 사용하기 시작했고, maven이 xml로 빌드 스크립트를 정의하는게 길고 피곤하기 때문에 gradle을 사용하기 시작했고, 스칼라를 쓰기 시작하면서 sbt를 사용했다. 그런데 항상 멀티 프로젝트에 대한 불만이 있었다. 프로젝트를 하다보면 여러 언어(자바, 스칼라, 코틀린)을 섞어서 사용하는 경우나, 혹은 프로젝트가 크고 복잡해져서 여러 모듈로 쪼개서 관리하고 싶을 때가 있는데, 이들 빌드 시스템에서 멀티 프로젝트가 지원이 안 되는건 아니지만 사용하기 불편했다.
    - 여러 언어를 섞어 사용하는 경우에 대해서, 경우에 따라 기존 빌드툴이 잘 처리해주는 경우도 있다. 코틀린의 경우 코틀린 컴파일러가 자바로 된 프로그램을 이해하기 때문에 그냥 아무렇게나 코드를 섞어서 빌드해도 빌드 순서를 알아서 잘 정리해서 잘 빌드해 준다. 하지만 jparser 프로젝트를 하면서 스칼라, 코틀린, 자바를 다 섞어 쓰는 경우가 생겼는데, 이런 경우엔 한 모듈에 여러 언어를 섞어 쓰는 것이 잘 지원되지 않았고, 생각해보면 적절한 방법도 아니다.
    - 그리고 gradle의 경우, imperative 한 방식으로 빌드 그래프를 생성한다. 무슨 말인가 하면, maven만 해도 xml을 사용해서 현재 프로젝트의 설정값들을 명시적으로 빌드 정의 파일(pom.xml)에 적게 되어 있는데, gradle은 API를 이용해서 설정값들을 변경하게 되어 있고, 플러그인이 끼어들거나, 빌드 스크립트에 복잡한 문법을 사용하게 되면 실제로 빌드에 사용되는 설정값이 무엇인지 파악하기가 쉽지 않게 된다. 그러면 빌드 스크립트를 디버깅해야 하는 상황이 생기기도 한다. 빌드 따위를 위해 머리와 에너지와 시간을 써야 되는 끔찍한 상황이 생기는 것이다.
    - 그래도 전에는 별 대안이 없다고 생각해서 그냥 gradle과 sbt를 썼다.
    - 그러다 구글에 입사해보니 blaze라는 빌드 툴을 쓰고 있더라. blaze는 bazel이라는 이름으로 오픈소스화 되어 있기도 하다.
    - blaze는 내가 그래들에 대해 불만을 갖고 있던 부분을 거의 완전히 해결해 놓은 빌드 툴이었다. 여러 모듈을 관리하는데 특화되어 있었고, 각 모듈의 언어가 무엇인지 관계 없이 한 프로젝트 안에서 관리할 수 있었고, 각 모듈의 설정값은 선언적으로(declarative하게) 설정하도록 되어 있었다.
    - 이런 구조는 구글이 갖고 있는 [단일 리포지토리](https://research.google.com/pubs/pub45424.html?authuser=5) 와도 관련이 있는 것 같다. 왜 그랬는지 모르겠지만 구글은 초창기에 하나의 [Perforce](https://www.perforce.com/ko) 리포지토리에 회사의 모든 코드를 넣었다고 한다. 그러다 코드 양이 너무 많아져서 더이상 Perforce를 쓸 수 없게 되어서 Piper라는 새로운 백엔드까지 만들어서 지금까지 단일 리포지토리를 이어오고 있다. 상황이 이렇다 보니 한 리포지토리에 여러개의 언어로 작성된 모든 프로젝트의 코드가 들어가게 되었고, 거기에 맞는 빌드 시스템을 만들면서 나온게 blaze였던 것으로 나는 이해하고 있다.
    - 구글 안에서 사용하는 blaze는 빌드 속도가 좀 느리고 IntelliJ와의 연동이 다소 불안정한 느낌이긴 하지만 그래도 상당히 편리하고 좋은 빌드 툴이었다. 그래서 내 개인 프로젝트를 blaze의 오픈소스 버전인 bazel을 이용해 만들려고 시도해 보았다. 특히 하다보니 자바, 스칼라, 코틀린을 섞어서 만들게 된 jparser에 먼저 적용해보고 싶었다.
    - 그런데 문제가 있었다. blaze와 bazel은 공히 각 폴더 안에 BUILD 라는 이름의 파일을 가질 수 있고, 이 BUILD 파일 안에 빌드할 모듈의 정의가 들어있다. 그리고 다른 폴더에 있는 라이브러리에 의존하는 모듈을 만들려면 참조하려는 라이브러리가 있는 폴더의 경로와 그 폴더의 BUILD 파일 안에서 정의한 모듈의 이름을 쓴다. 예를 들면 "//some/module/in/another/directory:module_name" 과 같은 형태다. 여기서 맨 앞의 //는 리포지토리의 루트를 의미한다. 구글 안에서는 어차피 리포지토리가 하나기 때문에 이 "루트"라는 것이 무엇인지 명백하고, 오픈소스 버전인 bazel에서는 빌드가 수행되는 현재 디렉토리의 상위 디렉토리 중 "WORKSPACE"라는 이름의 파일이 있는 폴더를 "루트"로 인식한다. blaze와 bazel에서는 어떤 모듈을 빌드하고자 할 때 해당 모듈의 모든 의존성이 이 "루트" 밑에, 즉 구글 안에서라면 Piper라는 소프트웨어로 관리되는 단일 리포지토리 안에, bazel을 쓸 때는 WORKSPACE 파일이 있는 디렉토리 밑에 있어야 한다.
    - 이게 왜 문제였을까? maven, gradle, sbt 등을 쓸 때 가장 큰 장점 중 하나가 maven central과 같은 메이븐 저장소에 있는 라이브러리를 이름만 지정해서 쉽게 사용할 수 있는데, 이들 라이브러리는 WORKSPACE 밑에 있지 않다. 인터넷으로 접속해야 접근 가능한 외부의 어딘가에 있다. 그래서 bazel에서는 이러한 라이브러리를 사용하려면 WORKSPACE 파일이 있는 "루트" 디렉토리 밑의 어딘가로 복사해오는 과정을 수행해줘야 했다. 이 과정이 생각보다 잘 되지 않았다. 구글 안에서 쓸 수 있는 단일 리포지토리에는 내가 상상할 수 있는 거의 모든 라이브러리가 이미 잘 정리된 형태로 들어있기 때문에 문제가 되지 않았던 부분이다.
    - 지금은 bazel에서도 이 부분을 조금 더 쉽게 해결할 수 있는 방법이 추가된 것 같긴 하지만, 근본적으로 구글의 "단일 리포지토리"처럼 "내가 상상할 수 있는 모든 라이브러리를 포함한 리포지토리"가 없으면 유사한 형태의 불편이 계속 생길 수 밖에 없는 구조라는 것이 내 생각이다.
  - 그래서 bibix를 만들게 되었다.
    - bibix는 blaze와 bazel의 다음과 같은 특징을 모방하려고 노력했다.
      - 여러 언어를 한 프로젝트 안에서 빌드할 수 있다
      - 확장하기 쉽다
      - 빌드할 때 캐싱을 통해 불필요한 재 빌드를 줄인다
      - 서로간에 의존성이 없는 빌드를 동시에 실행해서 전체 빌드 시간을 줄인다
      - 각 모듈을 최대한 declarative하게 정의해서 빌드 스크립트를 디버깅해야 하는 필요성을 줄인다
    - 여기에 blaze와 bazel이 약한 "maven central과 같은 외부 시스템과의 연동성"을 강화하는 것이 bibix의 목적이었다.

2. 시작하기
  - 간단한(전형적인) gradle 프로젝트를 bibix로 옮기려면 어떻게 해야 할 지 알아보자.
  - 설치. 비빅스를 설치하려면 [릴리즈 페이지](https://github.com/Joonsoo/bibix/releases/tag/0.8.1)에서 bibix-0.8.1-all.jar 를 다운받는다. 적당한 폴더에 이 파일을 넣고, (리눅스 혹은 맥 기준) bibix 라는 파일을 만들고 다음의 내용을 넣은 다음 chmod u+x bibix 해준다. 다음 그 폴더를 .bashrc 같은 파일의 PATH에 추가해준다.
  - java -jar /Users/joonsoo/Documents/apps/bibix/bibix-0.8.1-all.jar "$@"
  - build.bbx
    - bibix 커맨드를 실행하면 커맨드가 실행된 폴더에 build.bbx라는 이름의 파일이 있는지 먼저 확인한다. 해당 파일이 있으면 비빅스 빌드 스크립트로 인식하고, 없으면 오류가 발생한다.
    - build.bbx 파일과 같은 bibix 빌드 스크립트 하나는 하나의 "프로젝트"를 나타낸다고 한다.
    - build.bbx 파일을 포함한 디렉토리의 서브 디렉토리에 build.bbx 파일을 포함한 다른 디렉토리가 있을 수 있다. 이들은 "서브 프로젝트"라고 부른다.
  - import maven
    from bibix.plugins import java
    from bibix.plugins import ktjvm

    main = java.library(
      srcs = glob("src/main/**.java"),
      deps = [
        maven.artifact("com.google.guava", "guava", "32.1.3-jre"),
      ],
    )

    test = java.library(
      srcs = glob("src/test/**.java"),
      deps = [
        main,
        maven.artifact("org.junit.jupiter", "junit-jupiter-api", "5.8.2"),
      ]
    )
  - maven, gradle, sbt 등처럼 디렉토리 구조를 강제하지 않는다. 사용자가 쓰고싶은 형태로 아무렇게나 써도 아무 문제가 없다. 다만 소스 코드의 위치가 바뀌면 모듈들의 srcs 인자의 값이 바뀌어야 할 것이다.
  - main과 test는 별개의 모듈이다. 따라서 test의 deps(dependencies의 약자, 의존성)에 main을 꼭 써 주어야 한다.
  - bibix는 각 모듈이 어떤 언어로 작성되어 있는지 모른다. 그래서 main과 test가 java로 작성되어 java로 컴파일해야 한다는 것을 java.library라는 룰을 사용함으로써 알려주어야 한다.
  - maven.artifact 는 메이븐 센트럴에 있는 아티팩트를 갖고 온다. 세 개의 스트링을 인자로 받고, 각각 그룹, 이름, 버젼이다. repo는 별도로 지정 가능하다. 3장에서 var를 설명하면서 더 이야기해보기로 하자.
  - 비빅스는 스크립트에 명시된 모듈과 액션 외에 임의로 새로운 기능을 추가하지 않는다. 위와 같은 빌드 스크립트가 있는 디렉토리에선 "bibix main"이나 "bibix test"로 main과 test 모듈의 빌드만 가능하다.
  - 만약 위의 main 모듈을 uber jar(모든 디펜던시를 포함해서 별도의 추가적인 -classpath 옵션을 주지 않아도 되는 jar 파일)로 만들고자 한다면 다음과 같은 스크립트를 추가하고, "bibix uberJar"를 실행한다.
  - from bibix.plugins import jar
    uberJar = jar.uberJar([main], "my-lib.jar")
  - uberJar 모듈을 빌드하면 bbxbuild/outputs/uberJar 폴더 밑에 my-app.jar 라는 파일이 생성된다. 좀더 정확히는 bbxbuild/objects/<uberJar 모듈의 hash ID> 폴더 밑에 my-app.jar 라는 파일이 생성되고, bbxbuild/outputs/uberJar 는 objects 밑의 폴더에 대한 소프트 링크이다.
  - main class를 정의해서 실행 가능한 uber jar를 만들려면 다음의 코드를 추가한다
    executableUberJar = jar.executableUberJar(
      [main],
      mainClass="com.giyeok.bibix.frontend.cli.BibixCli",
      jarFileName="my-app.jar"
    )

3. bibix의 개념과 build.bbx 문법
  - import
    - build.bbx 에서 사용할 "룰"들을 포함한 외부 "프로젝트"를 불러오기 위해 사용한다. 편의상 이처럼 "룰"을 제공하는 "프로젝트"를 "플러그인"이라고 부르기도 한다.
    - bibix가 기본적으로 제공하는 플러그인들이 있다. bibix, file, curl, jvm, maven 이 있고, 이들은 `import file`과 같이 임포트 해주어야 사용할 수 있다. 다만 예외적으로 bibix는 별도로 import하지 않아도 항상 사용할 수 있다.
    - bibix 외에도 몇몇 함수(정확히는 룰)와 클래스 등은 별도의 임포트 없이 사용 가능하다. 빌드가 실행되는 환경을 나타내는 Env, OS, Arch 등의 자료형, 현재 환경을 나타내는 currentEnv 함수와 env, 비빅스 프로젝트의 위치를 나타내는 BibixProject 클래스, 비빅스 프로젝트를 포함하는 git 리포의 위치를 나타내기 위한 git 함수, glob 패턴으로 특정 디렉토리의 파일들의 목록을 얻어오기 위한 glob 함수 등이다.
    - 일반적으로 흔히 사용되는(좀더 솔직하게 말하자면 내가 흔히 사용하는) 플러그인들은 bibix.plugins에서 얻어올 수 있다. bibix.plugins는 [bibix-plugins](https://github.com/Joonsoo/bibix-plugins) 깃허브 리포지토리를 가리킨다.
      - 자바 코드를 컴파일하기 위한 java
      - JVM 코틀린을 컴파일하기 위한 ktjvm
      - jar 파일을 만들기 위한 jar
      - 프로토콜 버퍼 코드를 컴파일해주는 protobuf
      - 프로토버프 메시지를 이용해 rpc 기능을 지원하는 grpc
      - 압축을 위한 tar, xz, gzip
      - 간단한 텍스트 템플릿을 사용할 수 있는 mustache
      - junit5 로 작성된 코드를 실행하기 위한 junit5
      - 그 외에도 몇몇 폴더가 있는데, 대부분 만들다 만 것들이거나, 내가 쓰려고 만들었다 방치해두어서 아마도 지금은 동작 안하는 것들이니 사용하지 않기를 권한다.
    - bibix.plugins에 포함된 플러그인을 사용하려면 `from bibix.plugins import ktjvm` 라고 쓴다. 이러면 [bibix-plugins](https://github.com/Joonsoo/bibix-plugins) 에 있는 `build.bbx` 파일을 읽고, 거기서 `import "ktjvm" as ktjvm`라고 되어 있는 것을 따라 ktjvm이라는 서브 프로젝트를 읽어와서 해당 플러그인을 ktjvm이란 이름으로 사용할 수 있게 된다.
  - 모듈
    - 비빅스에서 모듈은 다음과 같이 정의한다.
    - abc = rulename(srcs = [], deps = [])
    - main = ktjvm.library()
    - 모듈은 한번 빌드되면 파라메터가 변경되거나 빌드할 때 사용된 다른 모듈의 내용이 변경되는 경우가 아니면 재사용될 수 있다. 다만, 설정에 따라 얼마간 시간이 지나면 항상 다시 빌드할 수도 있다.
    - 모듈의 ID는 파라메터들을 합쳐서 SHA1을 돌린 해시값의 hex 스트링. 따라서 파라메터가 바뀌면 모듈 ID가 바뀐다.
    - 모듈의 파라메터에 포함된 파일이나 디렉토리, 혹은 의존하는 모듈의 내용이 변경되면 재빌드한다. ID보다 더 엄격한 기준.
  - 네임스페이스
    - 1장에서 설명한 바와 같이, 비빅스는 여러 개의 모듈을 포함하는 비교적 큰 프로젝트를 쉽게 관리하기 위해 만들어졌다. 그래서 모듈들을 유사한 것들끼리 묶어서 이름을 붙여주고 싶은 경우가 있다. 이 때 사용하는 것이 네임스페이스.
    - 네임스페이스 문법은 간단하다. 별다른 것 없이 그냥 네임스페이스 이름을 쓰고 뒤에 중괄호로 묶어서 해당 네임스페이스에 포함시키고 싶은 모듈과 액션들을 넣어주면 된다.
    - `base {main = ktjvm.library(...)}`
  - 액션
    - 액션은 사용자가 명시적으로 실행하면 항상 실행되는 동작을 나타낸다.
  - 룰
    - 룰은 모듈이나 액션의 동작을 실제로 정의하는 JVM 코드이다. 새로운 룰을 만드는 방법은 6장에서 살펴본다.
  - var
    - 룰의 def에는 기본 값을 정의할 수 있다. 다음의 ktjvm.library 룰을 보자.
    - def library(
        srcs: set<file>,
        deps: set<jvm.ClassPkg> = [],
        runtimeDeps: set<jvm.ClassPkg> = [],
        resources: set<file> = [],
        compilerVersion: string = compilerVersion,
        sdk?: jvm.ClassPkg = ktjvmSdk,
        outVersion: string = outVersion,
        optIns?: list<string>,
      ): jvm.ClassPkg = impl:com.giyeok.bibix.plugins.ktjvm.Library
    - srcs에는 기본 값이 없고 ?로 옵셔널하다고 표시되어 있지도 않기 때문에 이 룰을 사용하는 사용자는 srcs 인자의 값은 반드시 넘겨주어야 한다. 하지만 deps에는 빈 리스트(`[]`)가 기본 값으로 들어가고 있기 때문에 별도로 지정해주지 않아도 된다.
    - 그런데 여기서 compilerVersion 파라메터를 보면, compilerVersion이라는 기본 값을 갖고 있다. 이 둘은 이름은 같지만 다른 것을 나타낸다. 기본값으로 주어진 compilerVersion은 빌드 스크립트의 위에서 별도로 지정된 "프로젝트 변수"이다.
    - `var compilerVersion: string = "1.9.20"`
    - 그래서 ktjvm.library에 compilerVersion을 별도로 명시해주지 않으면 "프로젝트 변수" compilerVersion의 값, 즉 "1.9.20"을 사용하게 된다.
  - var 재정의
    - 그런데 이 프로젝트 변수를 사용하는 이유는 뭘까? 그냥 ktjvm.library 룰의 파라메터를 `compilerVersion: string = "1.9.20"`라고 쓰는 것과는 어떻게 다를까?
    - 프로젝트 변수를 사용하면 해당 프로젝트를 import해서 사용하는 다른 프로젝트가 해당 프로젝트 변수의 값을 재정의할 수 있다.
    - var ktjvm.compilerVersion = "1.9.22"
    - 이렇게 프로젝트 변수 값을 재정의하면, 그 프로젝트 내에서는 ktjvm.library를 사용하면서 compilerVersion 값을 주지 않았을 때 "1.9.20"이 아니라 "1.9.22"라는 값이 기본값으로 사용된다.
  - coercion
    - ktjvm.library 에는 srcs라는 파라메터가 있다. 이 파라메터는 set<file> 타입의 값을 받는다. 그런데 여태까지 우리가 준 값은 ["src/main/abc.kt"] 와 같은 list<string> 값이었다.
    - 이게 가능한 것은 스트링 -> 파일, 스트링 -> 디렉토리 등의 타입 변환이 자동으로 이루어지기 때문이다.

4. 데이터 타입 정의
  - 클래스와 enum을 정의할 수 있다.
  - 클래스는 super class와 일반 클래스가 있다.
  - super class는 말그대로 다른 클래스의 슈퍼 클래스인데, 별도의 필드를 가질 수 없고, 사실상 enum과 비슷하게 동작한다.
  - 일반 클래스는 필드들을 가질 수 있다.
  - super class OS { Linux, Windows }
    class Linux(distName: string, version: string, kernel: string)
    class Windows(version: string)
  - enum Arch {
      unknown,
      x86,
      x86_64,
      aarch_64,
    }

5. intellij 통합
  - bibix-intellij daemon과 bibix-intellij-plugin
  - bibix의 [메인 github 리포지토리](https://github.com/Joonsoo/bibix)에 bibix-intellij 라는 폴더가 있는데, 거기에 BibixIntellijService 라는 프로토버프 서비스가 정의되고 구현되어 있다. [여기](https://github.com/Joonsoo/bibix/releases/tag/0.8.1)에서 bibix-intellij-daemon-0.8.1-all.jar 를 눌러서 다운로드 받을 수 있다. bibix-intellij 폴더 밑에 있는 BibixIntellijServiceGrpcServerMain 라는 클래스가 그 구현체를 실행시키는 메인 클래스다. 이걸 실행시키면 프로세스가 하나 뜨고, 서비스에서 정의된 loadProject rpc 메소드를 실행하면 특정 디렉토리에 있는 비빅스 프로젝트를 읽어서 그 구조를 반환해준다.
  - [bibix-intellij-plugin](https://github.com/Joonsoo/bibix-intellij-plugin)는 intellij에 설치할 수 있는 intellij 플러그인 소스 코드이다. 빌드된 플러그인은 [여기](https://github.com/Joonsoo/bibix/releases/tag/0.8.1)에서 bibix-intellij-plugin-0.0.10.zip 를 눌러서 다운로드 받을 수 있다. 이 플러그인을 설치하면 build.bbx 파일이 포함된 프로젝트를 bibix 프로젝트로 인식해서 로드할 수 있고, intellij에 빨간색 숟가락(비빔밥을 비비는 것을 형상화) 아이콘을 가진 탭이 추가되고 거기서 새로고침 버튼을 눌러서 프로젝트를 재로드할 수 있다. 그런데 이 플러그인은 바로 위에서 이야기한 bibix-intellij-daemon 이 실행되고 있을 때만 동작한다. 그래서 intellij 플러그인을 사용하려면 bibix-intellij-daemon 을 실행시킨 상태여야 한다.

6. rule 만들기
  - bibix-plugins 리포지토리
  - bibix.base 모듈
  - BuildContext 클래스
  - BuildContext.arguments
  - 룰 구현체의 반환값
    - BibixValue
    - BuildRuleReturn.ValueReturn
    - BuildRuleReturn.EvalAndThen
    - BuildRuleReturn.GetTypeDetails
    - BuildRuleReturn.WithDirectoryLock (잘 안됨)
  - BibixValue 클래스
    - BooleanValue
    - StringValue
    - PathValue
    - FileValue
    - DirectoryValue
    - EnumValue
    - CollectionValue
    - TupleValue
    - NamedTupleValue
    - ClassInstanceValue
    - NClassInstanceValue
    - NoneValue
    - BuildRuleDefValue, ActionRuleDefValue, TypeValue 는 일종의 리플렉션 타입인데, 보통 쓸 일은 없으므로 넘어가기로 하자.

  - noreuse 모디파이어
    - 이 모디파이어가 붙은 룰 구현체는 그 결과값을 재사용하지 않는다. 대표적으로 glob의 경우, 빌드가 수행되는 시점에 지정된 패턴에 해당하는 모든 파일을 찾아야 하기 때문에 noreuse 모디파이어가 붙어 있다.
  - singleton 모디파이어 (아직 미구현)
    - 기본적으로 룰 구현체는 각 모듈마다 새 인스턴스를 만들어서 실행되는데, singleton 모디파이어가 붙으면 빌드 내내 하나의 객체를 만들어서 재사용한다
  - synchronized 모디파이어 (아직 미구현)
    - 이 룰의 구현체는 동시에 두 스레드 이상에서 실행되지 않는다. 항상 최대 한 개의 스레드에서만 실행된다.

7. bbxbuild 디렉토리
  - 비빅스는 빌드 결과를 모두 bbxbuild라는 파일 안에 넣는다. 나는 소스 코드 디렉토리 사이 사이에 빌드 결과가 들어가서 더럽혀지는 게 너무 싫다. CMake가 싫은 부분 중 하나
  - 그래서 비빅스는 빌드 결과를 모두 bbxbuild 밑에 넣는다.
  - config.pbsuf
    - 설정 포맷. sugarproto의 sugarformat이라는 문법으로 리포지토리 설정을 적을 수 있다
    - max_threads: 동시에 최대 몇 개의 빌드를 수행할지. 기본값 4
    - min_log_level: 로그 레벨. 빌드를 수행하다 로그를 남길 일이 있으면 bbxbuild/logs 폴더 밑에 새로운 파일이 하나 생성되는데, 이 때 기록할 로그의 최소 로그 레벨을 정의한다. 이들 로그는 rule의 정의에서 BuildContext.progressLogger 를 통해 남긴 로그들이다. 기본값 INFO
    - target_result_reuse_duration: 비빅스는 빌드된 뒤로 얼마간의 시간이 지나면 의존하는 모듈이나 파일의 변경이 없어도 다시 빌드를 수행한다. 그 "얼마간의 시간"을 정의한다. 기본값 1h(1시간)
  - objects 폴더
    - 빌드를 수행하면서 생성된 모든 모듈의 아웃풋을 저장하는 폴더. 물론 룰의 구현체가 이 규칙을 어긴다고 해도 bibix가 알 수는 없다. 하지만 제대로 만들어진 룰 구현체라면 BuildContext.destDirectory 밑에 생성되는 파일들을 저장해야 하고, 이 destDirectory가 가리키는 디렉토리가 바로 objects 폴더 밑에 모듈 ID 이름으로 만들어진 폴더들이다.
  - outputs 폴더
    - objects 폴더 밑에는 모듈 ID 이름으로 저장되기 때문에 사람이 읽기 어렵다. 그래서 build.bbx 스크립트에서 정한 이름으로 objects 폴더 밑의 모듈 아웃풋 폴더로 소프트 링크를 만들어서 outputs 폴더 밑에 만들어준다.
  - shared 폴더
    - 몇몇 룰은 시간이 오래 걸리지만 여러 모듈에서 공동으로 사용할 수 있는 작업들을 수행한다. 예를 들면 외부의 git 리포지토리 복제(bibix.plugins를 사용하는 경우와 같이), maven 라이브러리 다운로드와 같은 작업이다. 이런 경우 shared 폴더에 각 룰 구현체를 위한 폴더를 만들고 그 밑에 각 룰 구현체가 공유할 내용을 저장할 수 있다.
    - BuildContex.getSharedDirectory 메소드를 이용하면 bbxbuild/shared 폴더 밑에 폴더를 하나 생성해서 그 위치를 반환해준다. getSharedDirectory 메소드를 사용할 때는 이름을 지정해주어야 하는데, 이름은 해당 룰 구현체의 패키지 이름을 포함해 world-unique하게 만들기를 권한다. 그렇지 않고 다른 룰 구현체와 이름이 충돌하면 같은 폴더를 공유하게 돼서 어떻게 동작할 지 알 수 없다.

8. 기타 문제와 향후 계획
  - maven deploy
    - 구현 필요
  - junit test
    - 테스트 필요
  - 루프 지원
    - blaze와 bazel은 starlark라는 언어를 사용한다. starlark는 python의 변형인데, 내 생각에 가장 큰 특징은 starlark로 작성된 스크립트는 항상 종료된다는 점이다. 이를 보장하기 위해 while 문을 없앴고 for 문도 유한한 콜렉션에 대해서만 수행 가능하다.
    - 하지만 여전히 starlark에는 for 문이 있긴 하다. 그리고 이 for문이 종종 유용하게 쓰일 때도 있는데, 예를 들면 여러 컨피그의 조합으로 모듈을 정의해야 하는 경우다. 예를 들면 같은 코드를 리눅스용으로, 윈도우 용으로, 맥 용으로 빌드해야 하는 경우가 있다면 for 문이 없는 경우 거의 동일한 내용의 모듈 세 개를 정의해야 하는 반면 for 문을 사용해 코드를 간소화할 수 있다. 예를 들면 이런 식이다.
    - for target in ["linux", "win", "mac"]:
        cc_library(name="my-module-%s" % target, ...)
    - 이런 경우, 실제로 "my-module-linux", "my-module-win", "my-module-mac" 와 같은 이름을 가진 모듈 세 개가 정의된 것이다.
    - 이 때 첫번째 단점은 모듈의 정의를 직관적으로 찾을 수 없다는 점이다. 예를 들어, 해당 모듈이 정의된 폴더의 BUILD 파일을 열어서, Ctrl-F 를 눌러서 "my-module-linux"를 검색해도 뭐가 나오지 않는다는 것이다. 그래서 모듈이 어디서 정의됐는지 찾아보려면 BUILD 파일 전체를 뒤져봐야 한다. 물론 다행히도 다른 모듈들이 저런 for문 없이 평이하게 정의되어 있다면 찾기가 어렵지 않겠지만 저런 식으로 복잡한 빌드 스크립트 문법을 지원하기 시작하면 빌드 스크립트가 한없이 복잡해질 가능성이 있다.
    - 또다른 단점은 모듈에 전달되는 인자의 값을 파악하기 어려울 수 있다는 점이다. 만약 name을 제외한 다른 인자가 모두 동일하다면야 큰 문제가 아니겠지만, 대체로 이런 경우엔 인자가 조금씩은 다를 것이기 때문에 복잡성이 빠르게 증가할 가능성이 있다.
    - 그럼에도 불구하고 이런 문법을 고민하는 가장 큰 이유는 코드를 간결하게 할 수 있기 때문일 것이다. 실제로는 어떨까? blaze BUILD 파일에서 이처럼 for 문을 사용하는 경우를 구글 다니면서 딱 한 번 본 적이 있다. 그런데 당시에는 빌드 스크립트가 좀 복잡하게 느껴져서 저 for문을 손수 풀어서 평이하게 모듈 여러개를 정의하는 식으로 바꿔봤는데, 각 모듈별로 공유할 수 있는 값들은 별도 변수로 정의해서 사용하니 의외로 코드가 몇 줄 차이 나지 않았다. 그래서 개인적으론 이런 기능이 크게 유용하지 않을 것이라고 생각한다.
    - 그럼에도 여전히 혹여나 이런 기능이 필요할 수도 있겠다 싶어서 사용 사례를 찾고 있다.
  - intellij 플러그인 개선
    - bibix 대몬을 다운로드 받고 프로세스를 실행시켜 주는 기능 필요
  - 추가 언어(C++) 지원
    - C나 C++의 경우 로컬 시스템에 설치된 디펜던시를 사용해서 빌드하거나 하는 경우가 많은데, 비빅스 프로젝트는 포터블하게 이 컴퓨터에서 저 컴퓨터로 옮겨가도 빌드가 되는 것을 목표로 하기 때문에 그대로 빌드하긴 어렵다.
    - 이 문제를 해결하기 위해 빌드 환경을 정의한 도커 이미지같은걸 사용하는 방법도 잠시 고민해 봤는데, 가능할 것 같긴 하지만 쉽지 않아 보인다. 일단 내가 C++ 로 코딩 하는 일이 드물어서 보류상태
  - 네임스페이스에 베이스 디렉토리 명시 문법 추가
    - 네임스페이스 기능을 사용하다 보면 네임스페이스가 실제 서브디렉토리 하나를 나타내게 되는 경우가 많다. 그래서 네임스페이스 안에서는 현재 디렉토리의 서브 디렉토리를 베이스로 사용하도록 하는 기능을 생각중이다.
    - 사실 bibix 문법을 정의하는 grammar/bibix.cdg 에서 NamespaceDef에 이미 해당 기능을 위한 문법은 추가해 놓았다. 아직 구현은 안 됨.








