package com.giyeok.bibix.ast

import com.giyeok.jparser.ktparser.mgroup2.MilestoneGroupParserDataKt
import com.giyeok.jparser.ktparser.mgroup2.MilestoneGroupParserKt
import com.giyeok.jparser.ktparser.mgroup2.MilestoneGroupParserLoader

object BibixParser {
  val parserData =
    MilestoneGroupParserLoader.loadParserDataFromGzippedResource("/bibix2-mg2-parserdata-trimmed.pb.gz")
  val parser = MilestoneGroupParserKt(MilestoneGroupParserDataKt(parserData))

  fun parse(source: String): BibixAst.BuildScript {
    val parseResult = parser.parse(source)
    val history = parser.kernelsHistory(parseResult)
    return BibixAst(source, history).matchStart()
  }
}
