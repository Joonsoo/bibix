package com.giyeok.bibix.graph

import com.giyeok.bibix.ast.BibixParser
import org.junit.jupiter.api.Test

class NameLookupTableTest {
  @Test
  fun test() {
    val script = BibixParser.parse(
      this::class.java.getResourceAsStream("/test1.bbx")!!.readAllBytes().decodeToString()
    )
    val nameLookup = NameLookupTable.fromScript(script)

    println(nameLookup.lookupName(listOf("base", "proto", "schema")))
    println(nameLookup.lookupName(listOf("ktjvm", "library")))
    println(nameLookup.lookupName(listOf("base", "proto")))
  }
}
