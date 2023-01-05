package com.giyeok.bibix.runner

class BibixBuildException(val task: BuildTask, message: String, cause: Throwable? = null) :
  Exception(message, cause)
