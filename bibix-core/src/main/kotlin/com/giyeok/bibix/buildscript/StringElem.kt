package com.giyeok.bibix.buildscript

sealed class StringElem

// JustChar와 EscapeChar들을 묶어서 StringChunk로 변환
data class StringChunk(val value: String) : StringElem()
data class ExprChunk(val expr: ExprNode) : StringElem()


sealed class StringElemBuilder {
  abstract fun build(): StringElem
}

class StringChunkBuilder(val builder: StringBuilder) : StringElemBuilder() {
  override fun build(): StringElem = StringChunk(builder.toString())
}

class ExprChunkBuilder(val expr: ExprNode) : StringElemBuilder() {
  override fun build(): StringElem = ExprChunk(expr)
}


class StringElemsBuilder {
  private val stringElems = mutableListOf<StringElemBuilder>()

  fun addChar(c: Char) {
    if (stringElems.isEmpty()) {
      stringElems.add(StringChunkBuilder(StringBuilder(c.toString())))
    } else {
      val last = stringElems.last()
      if (last is StringChunkBuilder) {
        last.builder.append(c)
      } else {
        stringElems.add(StringChunkBuilder(StringBuilder(c.toString())))
      }
    }
  }

  fun addExpr(expr: ExprNode) {
    stringElems.add(ExprChunkBuilder(expr))
  }

  fun build(): List<StringElem> =
    stringElems.map { it.build() }
}
