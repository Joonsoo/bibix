package com.giyeok.bibix.plugins.cputils

import org.junit.jupiter.api.Test
import java.io.File

class ClassCollectorTests {
  @Test
  fun test() {
    val collector = ClassCollector(
      listOf(
        File("/home/joonsoo/Documents/workspace/bibix/lib/jparser-base-0.2.3.jar"),
        File("/home/joonsoo/Documents/workspace/bibix/bibix-plugins/build/classes/kotlin/main"),
        File("/home/joonsoo/Documents/workspace/jparser/naive/target/test-classes"),
        File("/home/joonsoo/Documents/workspace/bibix/bibix-plugins/build/classes/kotlin/test"),
      ),
      listOf(
        File("/home/joonsoo/Documents/workspace/bibix/lib/jparser-base-0.2.3.jar"),
        File("/home/joonsoo/Documents/workspace/bibix/bibix-plugins/build/classes/kotlin/main"),
        File("/home/joonsoo/Documents/workspace/jparser/naive/target/test-classes"),
        File("/home/joonsoo/Documents/workspace/bibix/bibix-plugins/build/classes/kotlin/test"),
        File("/home/joonsoo/Documents/workspace/bibix/bbxbuild/shared/maven/org/scalatest/scalatest-core_2.13/3.2.11/scalatest-core_2.13-3.2.11.jar"),
        File("/home/joonsoo/Documents/workspace/bibix/bbxbuild/shared/maven/org/scalatest/scalatest-flatspec_2.13/3.2.11/scalatest-flatspec_2.13-3.2.11.jar"),
      )
    )
    val stests = collector.findSubclassesOf("org.scalatest.Suite")
    val tests = collector.findMethodsWithAnnotation("org.junit.jupiter.api.Test")
    println(collector)
    // collector.findSubclassesOf("")
  }
}
