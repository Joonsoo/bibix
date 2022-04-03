package com.giyeok.bibix.runner

import com.giyeok.bibix.ast.BibixAst
import com.giyeok.bibix.plugins.Classes
import com.giyeok.bibix.utils.toKtList

data class BibixPlugin(
  val defs: List<BibixAst.Def>,
  val classes: Classes,
) {
  companion object {
    fun fromScript(script: String, classes: Classes): BibixPlugin {
      val parsed = BibixAst.parseAst(script)

      if (parsed.isRight) {
        println(parsed.right().get())
      }
      check(parsed.isLeft)
      val ast = parsed.left().get()
      return BibixPlugin(ast.defs().toKtList(), classes)
    }
  }
}
