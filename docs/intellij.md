# IntelliJ에서 bibix 사용하기

## 설치

1. [최신 release 페이지](https://github.com/Joonsoo/bibix/releases/tag/0.8.1)에서 IntelliJ 플러그인(bibix-intellij-plugin-0.0.10.zip)과 IntelliJ 대몬(bibix-intellij-daemon-0.8.1-all.jar) 다운로드

2. IntelliJ를 열고 File > Settings... > Plugins에서 상단 톱니바퀴 버튼 누른 다음 "Install Plugin from Disk..." 클릭하고 1에서 다운로드 받은 zip 파일 선택해서 플러그인 설치

![Install Plugin from Disk...](./install_plugin.png)

3. 플러그인을 사용하기 전에 `java -jar bibix-intellij-daemon-0.8.1-all.jar`를 실행해서 1에서 다운 받은 IntelliJ 대몬을 실행시킨 상태로 사용

4. 이제 IntelliJ에서 프로젝트를 새로 열기 위해 File > Open... 를 실행하는 경우 build.bbx 파일이 있으면 비빅스 프로젝트로 열 수 있고, 비빅스 프로젝트로 열면 오른쪽에 빨간색 숟가락 아이콘(숟가락이라고 그린거임..)을 눌러 Bibix 탭을 열어서 "Reload All Bibix Projects" 버튼 눌러서 새로 고침 가능


> gradle 등의 intellij 플러그인도 비슷한 방식으로 동작하는데, 그들은 자동으로 daemon 을 받아서 알아서 실행시켜주기 때문에 직접 대몬 프로세스를 띄우거나 할 필요가 없다. bibix intellij 플러그인에도 그런 기능이 있으면 좋겠지만... 혹시 관심이 있다면.. [github](https://github.com/Joonsoo/bibix-intellij-plugin)과 [블로그](https://giyeok.com/2023/01/26/bibix-3) 확인 요망


## 한계

- 지금 bibix-intellij-daemon은 java, ktjvm(코틀린), scala 만 지원됨


## Troubleshooting

### JVM 버전 때문에 뭔가 안되는 경우

  bibix의 기본 플러그인 프로젝트인 [bibix-plugins](https://github.com/Joonsoo/bibix-plugins)에서는 java는 21 버전을, 코틀린을 1.9.20 버전을 기본으로 사용하고 있고, release 페이지에 업로드된 jar 파일들도 JRE 버전 21에서 컴파일되어 있다. 비빅스가 코틀린은 필요한 버전을 다운로드 받아서 사용하므로 신경 쓸 필요가 없지만 JVM은 최신 버전으로 설치한다.

### 메인 메뉴에서 Build > Build Project 등을 사용하는 경우 빌드를 저장할 디렉토리가 설정되어 있지 않다거나 SDK가 설정되지 않았다고 하는 경우
  1. "Project" 탭을 연다. 기본 단축키 Alt-1

  2. "Project" 보기 상태인지 확인하고, 맞으면 가장 위의 프로젝트 루트에서 오른쪽 클릭을 해서 "Open Module Settings" 버튼을 클릭한다. "Project Structure" 창이 열린다.

  3. 왼쪽에서 "Project"를 선택하면 다음과 같은 화면이 나온다.

![Project Structure](./project_structure.png)

  4. "Compiler output:" 이 비어있으면 현재 프로젝트 밑의 bbxbuild 밑에 ijbuild 와 같은 이름의 폴더를 생성해서 그 폴더를 지정해준다.

  5. SDK나 Language level이 비어있으면 설정해준다.

  6. 왼쪽에서 "Modules"를 선택해서 나오는 모듈 목록에서 각 모듈의 Language level이 잘 설정되어 있는지 확인한다.


### 코틀린 코드를 컴파일할 때 JVM 버전 때문에 빌드가 실패하는 경우
  1. File > Settings... > Build, Execution, Deployment > Compiler > Kotlin Compiler 에서 "Target JVM version"을 최신으로 바꿔본다.

![Kotlin Compiler](./kotlin_build_setting.png)


### JVM 버전 때문에 실행이 안된다고 하는 경우

  1. IDE 상단의 실행/디버그 버튼 왼쪽의 "Run/Debug Configuration"를 클릭하고 "Edit Configurations..."를 선택한다.

  2. 실행할 Configuration을 선택하고 JRE 값이 잘 설정되어 있는지 확인한다.

![Run Configuration](./run_config.png)


### 새 모듈을 추가했는데 모듈의 sources root가 제대로 설정되지 않는 경우

  - bibix-intellij-daemon은 모듈 구조를 파악할 때 build.bbx 파일만 보는 것이 아니고, 각 모듈의 소스 코드(srcs에 지정된 파일들)을 읽어서, 그 파일에서 설정한 package를 반영한다.
  - bibix는 소스 코드가 여러 디렉토리에 나뉘어져 있어도 되도록 설계된 반면, intellij는 한 모듈의 소스 코드는 한 디렉토리에 모여 있어야 되는 것으로 설계되어 있기 때문에 생기는 차이점때문에 이렇게 설계한 것. intellij에서 bibix를 사용하려면 intellij에서 요구하는대로 한 모듈의 소스 코드들이 한 디렉토리에 모여 있어야 한다.
  - 따라서 build.bbx에서 어떤 모듈을 추가했는데, srcs에 빈 리스트를 주고 있으면 모듈 구조가 제대로 파악되지 않아서 intellij에서 모듈 구조가 이상하게 나타날 수 있다.
    - 이 문제를 해결하는 가장 쉬운 방법은 srcs에 빈 리스트를 주지 않는 것이다. glob을 사용하고 있다면 해당 디렉토리에 실제로 소스 코드 파일을 추가하고, package 문을 추가하고 프로젝트를 reload한다.
  - 또, 코틀린의 경우 package 문 앞에 annotation이 올 수 있는데, 이 경우 bibix-intellij-daemon이 해당 파일을 파싱할 수 없어서 제대로 모듈 구조가 파악되지 않을 수 있다.
    - 이 부분은 현재 비빅스 intellij daemon의 한계로, Project Structure 창을 열어서 직접 수정해주어야 한다.
    - Project Structure에서 왼쪽에서 Modules를 선택하고, 소스 루트가 제대로 설정되지 않은 모듈을 선택한 다음 오른쪽에서 Sources 탭을 선택하고 "+Add Content Root"로 소스 코드 루트를 선택해서 설정한다.
