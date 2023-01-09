package com.giyeok.bibix.base

import com.giyeok.bibix.BibixIdProto
import java.nio.file.FileSystem
import java.nio.file.Path
import kotlin.io.path.createDirectory
import kotlin.io.path.notExists

data class BuildContext(
  val env: BuildEnv,
  val fileSystem: FileSystem,
  // 메인 스크립트의 베이스 디렉토리
  val mainBaseDirectory: Path,
  // 이 룰을 호출하는 위치의 베이스 디렉토리. 대체로 이 값을 사용하게 될 것
  // preloaded plugin에서 호출되는 경우엔 null이 된다
  val callerBaseDirectory: Path?,
  // sourceId의 베이스 디렉토리. rule이 preloaded plugin 안에 있는 경우 null이 된다.
  val ruleDefinedDirectory: Path?,
  // target에 지정된 parameter들의 값
  val arguments: Map<String, BibixValue>,
  // input file들이 지난 빌드때와 같으면 true.
  // 첫 빌드이거나 지난번 빌드때와 다르면 false.
  // 일반적으로는 hashPreserved==true이면 실제 빌드는 하지 않고 값만 반환해도 됨.
  val hashChanged: Boolean,
  // object id
  val objectId: BibixIdProto.ObjectId,
  // object id hash string
  val objectIdHash: String,
  // 빌드된 결과를 저장할 디렉토리
  private val destDirectoryPath: Path,
  // 로깅/진행 상황 표시
  val progressLogger: ProgressLogger,
  // TODO 전체 그래프 정보
  // TODO 현재까지의 실행 결과
  private val repo: BaseRepo,
  // TODO bibix 플러그인 실행하기
  // e.g. context.call("mvn.resolveClassSets", mapOf("classSets", deps))
) {
  val destDirectory: Path by lazy {
    if (destDirectoryPath.notExists()) {
      destDirectoryPath.createDirectory()
    }
    destDirectoryPath
  }

  fun clearDestDirectory(): Path {
    // TODO()
    return destDirectory
  }

  fun getSharedDirectory(sharedRepoName: String) =
    repo.prepareSharedDirectory(sharedRepoName)
}
