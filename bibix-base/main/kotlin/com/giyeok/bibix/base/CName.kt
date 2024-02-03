package com.giyeok.bibix.base

// Canonical Name
data class CName(val sourceId: SourceId, val tokens: List<String>) {
  constructor(sourceId: SourceId, vararg tokens: String) : this(sourceId, tokens.toList())

  fun append(token: String): CName = CName(sourceId, tokens + token)

  fun append(tokens: List<String>): CName = CName(sourceId, this.tokens + tokens)

  override fun toString(): String = "$sourceId:${tokens.joinToString(".")}"

  fun parent(): CName = CName(sourceId, tokens.dropLast(1))
}
