package com.giyeok.bibix.runner

import com.giyeok.bibix.argPair
import com.giyeok.bibix.argsMap
import com.giyeok.bibix.bibixValue
import com.giyeok.bibix.repo.hashString
import org.junit.jupiter.api.Test

class HashesTests {
  @Test
  fun testArgsHashString() {
    val argsMap = argsMap {
      this.pairs.add(argPair {
        this.name = "test1"
        this.value = bibixValue {
          this.stringValue = "foobar"
        }
      })
      this.pairs.add(argPair {
        this.name = "test2"
        this.value = bibixValue {
          this.booleanValue = true
        }
      })
    }
    println(argsMap.hashString())
  }
}
