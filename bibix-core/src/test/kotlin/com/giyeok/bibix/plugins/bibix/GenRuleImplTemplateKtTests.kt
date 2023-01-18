package com.giyeok.bibix.plugins.bibix

import com.giyeok.bibix.base.ClassInstanceValue
import com.giyeok.bibix.base.FileValue
import com.giyeok.bibix.base.StringValue
import com.giyeok.bibix.interpreter.testInterpreter
import com.giyeok.bibix.plugins.prelude.preludePlugin
import com.google.common.jimfs.Jimfs
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.io.path.readText
import kotlin.io.path.writeText

class GenRuleImplTemplateKtTests {
  @Test
  fun test(): Unit = runBlocking {
    val fs = Jimfs.newFileSystem()

    val script = """
      import jvm
      
      impl = jvm.ClassPaths(["/abc.jar"])
      def helloRule(message: string): list<path> = impl:testing.name.HelloRule

      aaa = bibix.genRuleImplTemplateKt(
        rules = [helloRule],
        types = [],
        implName = "testing.name.HelloRuleImpl",
        implInterfaceName = "testing.name.HelloRuleInterface"
      )
    """.trimIndent()
    fs.getPath("/build.bbx").writeText(script)

    val interpreter = testInterpreter(
      fs, "/",
      preloadedPlugins = mapOf("bibix" to bibixPlugin),
      preludePlugin = preludePlugin
    )

    val template = interpreter.userBuildRequest("aaa") as ClassInstanceValue
    assertThat(template.packageName).isEqualTo("com.giyeok.bibix.plugins.bibix")
    assertThat(template.className).isEqualTo("RuleImplTemplate")

    val implClass = (template.fieldValues.getValue("implClass") as FileValue).file.readText()
    val interfaceClass =
      (template.fieldValues.getValue("interfaceClass") as FileValue).file.readText()

    println(implClass)
    println(interfaceClass)

    assertThat(implClass).isEqualTo("")
    assertThat(interfaceClass).isEqualTo("")
  }
}
