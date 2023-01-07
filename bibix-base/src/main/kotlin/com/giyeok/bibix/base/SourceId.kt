package com.giyeok.bibix.base

sealed class SourceId

object BibixRootSourceId : SourceId() {
  override fun toString(): String = "root"
}

object MainSourceId : SourceId() {
  override fun toString(): String = "main"
}

data class PreloadedSourceId(val name: String) : SourceId() {
  override fun toString(): String = "preloaded($name)"
}

// 같은 디렉토리 안에 있더라도 별도의 build.bbx 파일로 정의된 서브 프로젝트이거나,
// git 등으로 외부에서 갖고온 경우
data class ExternSourceId(val externSourceId: Int) : SourceId() {
  override fun toString(): String = "extern($externSourceId)"
}
