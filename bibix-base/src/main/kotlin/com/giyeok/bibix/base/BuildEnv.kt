package com.giyeok.bibix.base

data class BuildEnv(
  val os: OS,
  val arch: Architecture,
)

sealed class OS {
  data class Linux(val distributeName: String, val version: String): OS()
  data class Windows(val distributeName: String, val version: String): OS()
  data class MacOSX(val distributeName: String, val version: String): OS()
  object Unknown: OS()
}

enum class Architecture {
  X86_64,
  X86,
  Aarch_64,
  Unknown
}
