package com.giyeok.bibix.interpreter.expr

import com.giyeok.bibix.base.*
import com.giyeok.bibix.interpreter.testInterpreter
import com.giyeok.bibix.plugins.Classes
import com.giyeok.bibix.plugins.PreloadedPlugin
import com.google.common.jimfs.Jimfs
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.io.path.writeText

class GetTypeDetailsTests {
  @Test
  fun test(): Unit = runBlocking {
    val fs = Jimfs.newFileSystem()

    val script = """
      import abc
      
      aaa = abc.testRun()
    """.trimIndent()
    fs.getPath("/build.bbx").writeText(script)

    val abcPlugin = PreloadedPlugin.fromScript(
      "com.abc",
      """
        import xyz
        class Class1(field1?: string, field2: Class2)
        class Class2(field3: string)
        
        def testRun(): string =
          native:com.giyeok.bibix.interpreter.expr.GetTypeDetailsPlugin
      """.trimIndent(),
      Classes(GetTypeDetailsPlugin::class.java)
    )

    val xyzPlugin = PreloadedPlugin.fromScript(
      "com.xyz",
      """
        enum Enum11 { hello, world }
        class Class3(field4: string)
        super class Supers{Class3}
      """.trimIndent(),
      Classes()
    )

    val interpreter = testInterpreter(fs, "/", mapOf("abc" to abcPlugin, "xyz" to xyzPlugin))

    assertThat(interpreter.userBuildRequest("aaa")).isEqualTo(StringValue("done"))

    assertThat(GetTypeDetailsPlugin.typeDetailsMap!!.canonicalNamed).containsExactly(
      TypeName("com.abc", "Class1"),
      DataClassTypeDetails(
        "com.abc",
        "Class1",
        listOf(
          RuleParam("field1", TypeValue.StringTypeValue, true),
          RuleParam("field2", TypeValue.DataClassTypeValue("com.abc", "Class2"), false)
        )
      )
    )
    assertThat(GetTypeDetailsPlugin.typeDetailsMap!!.relativeNamed).containsExactly(
      "Class2",
      DataClassTypeDetails(
        "com.abc",
        "Class2",
        listOf(RuleParam("field3", TypeValue.StringTypeValue, false))
      ),
      "xyz.Class3",
      DataClassTypeDetails(
        "com.xyz",
        "Class3",
        listOf(RuleParam("field4", TypeValue.StringTypeValue, false))
      ),
      "xyz.Enum11",
      EnumTypeDetails(
        "com.xyz",
        "Enum11",
        listOf("hello", "world")
      ),
      "xyz.Supers",
      SuperClassTypeDetails(
        "com.xyz",
        "Supers",
        listOf("Class3")
      )
    )
  }
}

class GetTypeDetailsPlugin {
  companion object {
    @JvmStatic
    var typeDetailsMap: TypeDetailsMap? = null

    @JvmStatic
    fun called(typeDetailsMap: TypeDetailsMap) {
      GetTypeDetailsPlugin.typeDetailsMap = typeDetailsMap
    }
  }

  fun build(context: BuildContext): BuildRuleReturn {
    return BuildRuleReturn.getTypeDetails(
      listOf(TypeName("com.abc", "Class1")),
      listOf("Class2", "xyz.Class3", "xyz.Enum11", "xyz.Supers"),
    ) { typeDetails ->
      called(typeDetails)
      BuildRuleReturn.value(StringValue("done"))
    }
  }
}
