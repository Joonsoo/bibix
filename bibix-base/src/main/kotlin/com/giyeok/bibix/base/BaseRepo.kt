package com.giyeok.bibix.base

import java.nio.file.Path

interface BaseRepo {
  fun prepareSharedDirectory(sharedRepoName: String): Path
}
