package com.giyeok.bibix.base

import java.io.File

interface BaseRepo {
  fun prepareSharedDirectory(sharedRepoName: String): File
}
