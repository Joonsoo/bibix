package com.giyeok.bibix.interpreter.expr

import com.giyeok.bibix.ast.BibixAst
import com.giyeok.bibix.base.*
import com.giyeok.bibix.interpreter.testInterpreter
import com.giyeok.bibix.plugins.Classes
import com.giyeok.bibix.plugins.PreloadedPlugin
import com.google.common.jimfs.Jimfs
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.io.path.writeText

class ClassValueTests {
  @Test
  fun test(): Unit = runBlocking {
    val fs = Jimfs.newFileSystem()

    val script = """
      package abc.def
      
      import abc
      import xyz
      
      class Abc(name: string, value: Def)
      class Def(hello: string)
      
      xxx = Abc("joonsoo", Def("world"))
      aaa = Abc(value=Def("abc"), name="def")
      yyy = abc.Qwe("google")
      zzz = abc.Rty(xyz.Xyz("farewell!"))
    """.trimIndent()
    fs.getPath("/build.bbx").writeText(script)

    val abcPlugin = PreloadedPlugin.fromScript(
      "com.abc",
      """
        import xyz
        
        class Qwe(bye: string)
        class Rty(hi: xyz.Xyz)
      """.trimIndent(),
      Classes()
    )

    val xyzPlugin = PreloadedPlugin.fromScript(
      "com.xyz",
      """
        class Xyz(msg: string)
      """.trimIndent(),
      Classes()
    )

    val interpreter = testInterpreter(fs, "/", mapOf("abc" to abcPlugin, "xyz" to xyzPlugin))

    assertThat(interpreter.userBuildRequest("xxx")).isEqualTo(
      ClassInstanceValue(
        "abc.def", "Abc", mapOf(
          "name" to StringValue("joonsoo"),
          "value" to ClassInstanceValue("abc.def", "Def", mapOf("hello" to StringValue("world")))
        )
      )
    )
    assertThat(interpreter.userBuildRequest("aaa")).isEqualTo(
      ClassInstanceValue(
        "abc.def", "Abc", mapOf(
          "name" to StringValue("def"),
          "value" to ClassInstanceValue("abc.def", "Def", mapOf("hello" to StringValue("abc")))
        )
      )
    )
    assertThat(interpreter.userBuildRequest("yyy")).isEqualTo(
      ClassInstanceValue(
        "com.abc", "Qwe", mapOf(
          "bye" to StringValue("google"),
        )
      )
    )
    assertThat(interpreter.userBuildRequest("zzz")).isEqualTo(
      ClassInstanceValue(
        "com.abc", "Rty", mapOf(
          "hi" to ClassInstanceValue("com.xyz", "Xyz", mapOf("msg" to StringValue("farewell!")))
        )
      )
    )
  }

  @Test
  fun testSuperClass(): Unit = runBlocking {
    val fs = Jimfs.newFileSystem()

    val script = """
      import abc
      
      xxx = abc.rule1("ab")
      yyy = abc.rule1("cd")
      zzz = abc.rule1("ef")
      aaa = abc.rule1("asdf")
    """.trimIndent()
    fs.getPath("/build.bbx").writeText(script)

    val abcPlugin = PreloadedPlugin.fromScript(
      "abc.def",
      """
        class Ab(hello: string)
        class Cd(world: string)
        class Ef(earth: string)
        class NotChild()

        super class Xyz {Ab, Cd, Ef}

        def rule1(v: string): Xyz = native:com.giyeok.bibix.interpreter.expr.TestPlugin3
      """.trimIndent(),
      Classes(TestPlugin3::class.java)
    )

    val interpreter = testInterpreter(fs, "/", mapOf("abc" to abcPlugin))

    interpreter.userBuildRequest("xxx")
    interpreter.userBuildRequest("yyy")
    interpreter.userBuildRequest("zzz")
    assertThrows<IllegalStateException> { interpreter.userBuildRequest("aaa") }
  }

  @Test
  fun testFieldTypeMismatch(): Unit = runBlocking {
    val fs = Jimfs.newFileSystem()

    val script = """
      package abc.def
      
      class Abc(name: string, value: Def)
      class Def(hello: string)
      
      xxx = Abc()
      yyy = Abc(Def("xyz"), "zyx")
      aaa = Abc("a", Def("b"), "c")
      zzz = Abc(name="def", value=Def("fed"), msg="message")
    """.trimIndent()
    fs.getPath("/build.bbx").writeText(script)

    val interpreter = testInterpreter(fs, "/", mapOf())

    assertThrows<IllegalStateException>("Missing parameters") {
      interpreter.userBuildRequest("xxx")
    }
    assertThrows<IllegalStateException>("Coercion failed") {
      interpreter.userBuildRequest("yyy")
    }
    assertThrows<IllegalStateException>("Unknown positional parameters") {
      interpreter.userBuildRequest("aaa")
    }
    assertThrows<IllegalStateException>("Unknown parameters") {
      interpreter.userBuildRequest("zzz")
    }
  }

  @Test
  fun testNoPackageName(): Unit = runBlocking {
    val fs = Jimfs.newFileSystem()

    val script = """
      class Abc(name: string, value: Def)
      class Def(hello: string)
      
      xxx = Abc("joonsoo", Def("world"))
    """.trimIndent()
    fs.getPath("/build.bbx").writeText(script)

    val interpreter = testInterpreter(fs, "/", mapOf())

    assertThrows<IllegalStateException>("Package name for class main:Abc not specified") {
      interpreter.userBuildRequest("xxx")
    }
  }

  @Test
  fun testNClassInstance(): Unit = runBlocking {
    val fs = Jimfs.newFileSystem()

    val script = """
      import abc
      
      aaa = abc.hello("world")
      bbb = abc.hello("earth")
      ccc = abc.hello("error")
      ddd = abc.hello("wrong")
    """.trimIndent()
    fs.getPath("/build.bbx").writeText(script)

    val abcPlugin = PreloadedPlugin.fromScript(
      "com.abc",
      """
        import xyz
        
        def hello(
          helloTo: string
        ): xyz.Sup = native:com.giyeok.bibix.interpreter.expr.TestPlugin4
      """.trimIndent(),
      Classes(TestPlugin4::class.java)
    )

    val xyzPlugin = PreloadedPlugin.fromScript(
      "com.xyz",
      """
        super class Sup{Sub1, Sub2}
        class Sub1(message: string)
        class Sub2(another: string)
        class NotSub(haha: string)
      """.trimIndent(),
      Classes()
    )

    val interpreter = testInterpreter(fs, "/", mapOf("abc" to abcPlugin, "xyz" to xyzPlugin))

    assertThat(interpreter.userBuildRequest("aaa")).isEqualTo(
      ClassInstanceValue("com.xyz", "Sub1", mapOf("message" to StringValue("hello world~")))
    )
    assertThat(interpreter.userBuildRequest("bbb")).isEqualTo(
      ClassInstanceValue("com.xyz", "Sub2", mapOf("another" to StringValue("hello earth!")))
    )
    assertThrows<IllegalStateException> { interpreter.userBuildRequest("ccc") }
    assertThrows<IllegalStateException> { interpreter.userBuildRequest("ddd") }
  }

  @Test
  fun testNClassInstanceFields(): Unit = runBlocking {
    val fs = Jimfs.newFileSystem()

    val script = """
      import abc
      
      aaa = abc.hello("1")
      bbb = abc.hello("2")
    """.trimIndent()
    fs.getPath("/build.bbx").writeText(script)

    val abcPlugin = PreloadedPlugin.fromScript(
      "com.abc",
      """
        import xyz
        
        def hello(key: string): xyz.Class1 = native:com.giyeok.bibix.interpreter.expr.TestPlugin7
      """.trimIndent(),
      Classes(TestPlugin7::class.java)
    )

    val xyzPlugin = PreloadedPlugin.fromScript(
      "com.xyz",
      """
        class Class1(v1: list<Class2>)
        class Class2(v2: list<Class1>)
      """.trimIndent(),
      Classes()
    )

    val interpreter = testInterpreter(fs, "/", mapOf("abc" to abcPlugin, "xyz" to xyzPlugin))

    assertThat(interpreter.userBuildRequest("aaa")).isEqualTo(
      ClassInstanceValue(
        "com.xyz", "Class1",
        mapOf(
          "v1" to ListValue(
            ClassInstanceValue(
              "com.xyz", "Class2",
              mapOf("v2" to ListValue())
            )
          )
        )
      )
    )
    assertThat(interpreter.userBuildRequest("bbb")).isEqualTo(
      ClassInstanceValue(
        "com.xyz", "Class1",
        mapOf(
          "v1" to ListValue(
            ClassInstanceValue(
              "com.xyz", "Class2",
              mapOf(
                "v2" to ListValue(
                  ClassInstanceValue("com.xyz", "Class1", mapOf("v1" to ListValue()))
                )
              )
            )
          )
        )
      )
    )
  }

  @Test
  fun testGrandSubType(): Unit = runBlocking {
    val fs = Jimfs.newFileSystem()

    val script = """
      import abc
      
      target1 = abc.ruleA("data1")
      target2 = abc.ruleA("data2")
      target3 = abc.ruleA("data3")
      target4 = abc.ruleB("data1")
      target5 = abc.ruleB("data2")
      target6 = abc.ruleB("data3")
      target7 = abc.ruleC("data1")
      target8 = abc.ruleC("data2")
      target9 = abc.ruleC("data3")
    """.trimIndent()
    fs.getPath("/build.bbx").writeText(script)

    val abcPlugin = PreloadedPlugin.fromScript(
      "com.abc",
      """
        super class Super1 { Super2, Data1 }
        super class Super2 { Super3, Data2 }
        super class Super3 { Data3 }
        class Data1(hello: string)
        class Data2(hello: string)
        class Data3(hello: string)
        
        def ruleA(sel: string): Super1 = native:com.giyeok.bibix.interpreter.expr.TestPlugin5
        def ruleB(sel: string): Super2 = native:com.giyeok.bibix.interpreter.expr.TestPlugin5
        def ruleC(sel: string): Super3 = native:com.giyeok.bibix.interpreter.expr.TestPlugin5
      """.trimIndent(),
      Classes(TestPlugin5::class.java)
    )

    val interpreter = testInterpreter(fs, "/", mapOf("abc" to abcPlugin))

    val data1 = ClassInstanceValue("com.abc", "Data1", mapOf("hello" to StringValue("world")))
    val data2 = ClassInstanceValue("com.abc", "Data2", mapOf("hello" to StringValue("world")))
    val data3 = ClassInstanceValue("com.abc", "Data3", mapOf("hello" to StringValue("world")))

    assertThat(interpreter.userBuildRequest("target1")).isEqualTo(data1)
    assertThat(interpreter.userBuildRequest("target2")).isEqualTo(data2)
    assertThat(interpreter.userBuildRequest("target3")).isEqualTo(data3)
    assertThrows<IllegalStateException> { interpreter.userBuildRequest("target4") }
    assertThat(interpreter.userBuildRequest("target5")).isEqualTo(data2)
    assertThat(interpreter.userBuildRequest("target6")).isEqualTo(data3)
    assertThrows<IllegalStateException> { interpreter.userBuildRequest("target7") }
    assertThrows<IllegalStateException> { interpreter.userBuildRequest("target8") }
    assertThat(interpreter.userBuildRequest("target9")).isEqualTo(data3)
  }
}

class TestPlugin3 {
  fun build(context: BuildContext): BibixValue {
    return when ((context.arguments.getValue("v") as StringValue).value) {
      "ab" -> ClassInstanceValue("abc.def", "Ab", mapOf("hello" to StringValue("world")))
      "cd" -> ClassInstanceValue("abc.def", "Cd", mapOf("world" to StringValue("earth")))
      "ef" -> ClassInstanceValue("abc.def", "Ef", mapOf("earth" to StringValue("hello")))
      else -> ClassInstanceValue("abc.def", "NotChild", mapOf())
    }
  }
}

class TestPlugin4 {
  fun build(context: BuildContext): BibixValue {
    return when (val helloTo = (context.arguments.getValue("helloTo") as StringValue).value) {
      "world" -> NClassInstanceValue("xyz.Sub1", mapOf("message" to StringValue("hello $helloTo~")))
      "error" -> NClassInstanceValue("xyz.NotSub", mapOf("haha" to StringValue("error")))
      "wrong" -> NClassInstanceValue("xyz.Sub1", mapOf("msg" to StringValue("hello $helloTo~")))
      else -> NClassInstanceValue("xyz.Sub2", mapOf("another" to StringValue("hello $helloTo!")))
    }
  }
}

class TestPlugin5 {
  fun build(context: BuildContext): BibixValue {
    return when (val sel = (context.arguments.getValue("sel") as StringValue).value) {
      "data1" -> ClassInstanceValue("com.abc", "Data1", mapOf("hello" to StringValue("world")))
      "data2" -> ClassInstanceValue("com.abc", "Data2", mapOf("hello" to StringValue("world")))
      "data3" -> ClassInstanceValue("com.abc", "Data3", mapOf("hello" to StringValue("world")))
      else -> throw AssertionError()
    }
  }
}

class TestPlugin7 {
  fun build(context: BuildContext): BibixValue {
    return when ((context.arguments.getValue("key") as StringValue).value) {
      "1" -> NClassInstanceValue(
        "xyz.Class1",
        mapOf(
          "v1" to ListValue(
            NClassInstanceValue("xyz.Class2", mapOf("v2" to ListValue()))
          )
        )
      )

      else -> NClassInstanceValue(
        "xyz.Class1",
        mapOf(
          "v1" to ListValue(
            NClassInstanceValue(
              "xyz.Class2", mapOf(
                "v2" to ListValue(
                  NClassInstanceValue(
                    "xyz.Class1",
                    mapOf("v1" to ListValue())
                  )
                )
              )
            )
          )
        )
      )
    }
  }
}
