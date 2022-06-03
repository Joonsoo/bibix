package com.giyeok.bibix.ast

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class Test extends AnyFlatSpec with should.Matchers {
  "Parser" should "parse" in {
    val x = BibixAst.parseAst("import x as y")
    println(x)
  }
}
