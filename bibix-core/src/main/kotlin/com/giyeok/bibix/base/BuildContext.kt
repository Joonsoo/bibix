package com.giyeok.bibix.base

import com.giyeok.bibix.runner.BuildTaskRoutinesManager
import com.giyeok.bibix.runner.Repo
import com.giyeok.bibix.utils.ProgressIndicator
import java.io.File

data class BuildContext(
  // 해당 모듈의 definition source id
  val sourceId: SourceId,
  // sourceId의 베이스 디렉토리
  val ruleBaseDirectory: File,
  // 이 룰을 호출하는 쪽의 베이스 디렉토리. 대체로 이 값을 사용하게 될 것
  val callerBaseDirectory: File,
  // 메인 스크립트의 베이스 디렉토리
  val mainBaseDirectory: File,
  // target에 지정된 parameter들의 값
  val arguments: Map<String, BibixValue>,
  // input file들이 지난 빌드때와 같으면 true.
  // 첫 빌드이거나 지난번 빌드때와 다르면 false.
  // 일반적으로는 hashPreserved==true이면 실제 빌드는 하지 않고 값만 반환해도 됨.
  val hashChanged: Boolean,
  // 빌드된 결과를 저장할 디렉토리
  private val destDirectoryPath: File,
  // 로깅/진행 상황 표시
  val progressIndicator: ProgressIndicator<BuildTaskRoutinesManager.BuildTaskRoutineId>,
  // TODO 전체 그래프 정보
  // TODO 현재까지의 실행 결과
  private val repo: Repo,
) {
  val destDirectory: File
    get() {
      destDirectoryPath.mkdir()
      return destDirectoryPath
    }

  fun getSharedDirectory(sharedRepoName: String) =
    repo.prepareSharedDirectory(sharedRepoName)
}
