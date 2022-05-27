package com.giyeok.bibix.base

import java.io.File

data class OriginPath(
  val origin: SourceId,
  val originBaseDirectory: File,
  val filePath: String
) {
  val file get() = File(originBaseDirectory, filePath)
}
