package com.giyeok.bibix.ast

import com.giyeok.jparser.ktparser.mgroup2.MilestoneGroupParserLoader

object BibixParser {
  val parser =
    MilestoneGroupParserLoader.loadParserFromGzippedResource("/bibix2-mg2-parserdata-trimmed.pb.gz")

  fun parse(source: String): BibixAst.BuildScript {
    val parseResult = parser.parse(source)
    val history = parser.kernelsHistory(parseResult)
    return BibixAst(source, history).matchStart()
  }
}
