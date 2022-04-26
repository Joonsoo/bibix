package com.giyeok.bibix.runner

import com.giyeok.bibix.runner.BuildTask

class BibixBuildException(val task: BuildTask, message: String, cause: Throwable? = null) :
  Exception(message, cause)
