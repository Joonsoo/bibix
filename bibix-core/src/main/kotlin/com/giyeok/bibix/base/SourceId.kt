package com.giyeok.bibix.base

sealed class SourceId

object BibixRootSourceId : SourceId() {
  override fun toString(): String = "root"
}

object MainSourceId : SourceId() {
  override fun toString(): String = "main"
}

data class BibixInternalSourceId(val name: String) : SourceId() {
  override fun toString(): String = "native($name)"
}

// 별도로 sources 디렉토리 밑에 코드를 불러오지 않아도 되는 경우에는 LocalSourceId,
// 외부 깃 등에서 코드를 갖고와야해서 sources 디렉토리 밑에 복사해와야 하는 경우에는 RemoteSourceId.
// RemoteSourceId의 remoteSourceId는 BuildGraph에 ArgsMap의 형태로 저장
data class LocalSourceId(val path: String) : SourceId() {
  override fun toString(): String = "local($path)"
}

data class RemoteSourceId(val remoteSourceId: Int) : SourceId() {
  override fun toString(): String = "remote($remoteSourceId)"
}
