package com.giyeok.bibix.ast

import com.giyeok.jparser.ktlib.*

class KotlinAst(
  val source: String,
  val history: List<KernelSet>,
  val idIssuer: IdIssuer = IdIssuerImpl(0)
) {
  private fun nextId(): Int = idIssuer.nextId()

  sealed interface AstNode {
    val nodeId: Int
    val start: Int
    val end: Int
  }


fun matchStart(): Char {
  val lastGen = source.length
  val kernel = history[lastGen].getSingle(2, 1, 0, lastGen)
  return matchA(kernel.beginGen, kernel.endGen)
}

fun matchA(beginGen: Int, endGen: Int): Char {
return source[beginGen]
}

}
