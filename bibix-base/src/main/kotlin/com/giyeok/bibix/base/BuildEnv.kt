package com.giyeok.bibix.base

data class BuildEnv(
  val os: OS,
  val arch: Architecture,
)

sealed class OS {
  abstract val distributeName: String
  abstract val version: String

  data class Linux(override val distributeName: String, override val version: String) : OS()
  data class Windows(override val distributeName: String, override val version: String) : OS()
  data class MacOS(override val distributeName: String, override val version: String) : OS()
}

enum class Architecture {
  X86_64
}
