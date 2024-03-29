package com.giyeok.bibix.interpreter.hash

import com.giyeok.bibix.*
import com.giyeok.bibix.BibixIdProto.TargetIdData
import com.giyeok.bibix.base.*
import com.giyeok.bibix.interpreter.FakePluginImplProvider
import com.giyeok.bibix.interpreter.testInterpreter
import com.giyeok.bibix.plugins.PluginInstanceProvider
import com.giyeok.bibix.plugins.PreloadedPlugin
import com.google.common.jimfs.Jimfs
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.codehaus.plexus.classworlds.ClassWorld
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files
import kotlin.io.path.writeText

class ObjectHasherTest {
  @Test
  fun testPreloadedPlugins(): Unit = runBlocking {
    val fs = Jimfs.newFileSystem()

    val script = """
      import abc
      
      aaa = abc.rule1("hello", "world")
      bbb = abc.rule1(a="hello", b="world")
      ccc = abc.rule1(b="world", a="hello")
      ddd = abc.qqq
    """.trimIndent()
    fs.getPath("/build.bbx").writeText(script)

    val abcPlugin = PreloadedPlugin.fromScript(
      "com.abc",
      """
        def rule1(a: string, b: string): string = native:com.giyeok.bibix.interpreter.hash.Rule1
        
        qqq = rule1("hello", "world")
      """.trimIndent(),
      PluginInstanceProvider(Rule1::class.java)
    )

    val interpreter = testInterpreter(fs, "/", mapOf("abc" to abcPlugin))

    assertThat(interpreter.userBuildRequest("aaa")).isEqualTo(StringValue("Great!"))
    assertThat(interpreter.userBuildRequest("bbb")).isEqualTo(StringValue("Great!"))
    assertThat(interpreter.userBuildRequest("ccc")).isEqualTo(StringValue("Great!"))

    val objHashMap1 = interpreter.g.getObjHashMap()
    val objMemoMap1 = interpreter.g.getObjMemoMap()
    assertThat(objHashMap1).hasSize(1)
    assertThat(objMemoMap1).hasSize(1)
    assertThat(objHashMap1.keys).isEqualTo(objMemoMap1.keys)

    assertThat(Rule1.history.map { it.first }).containsExactly("fe9154991d6910cd7d9eace18b0a1fe37f5c74a1")
//    assertThat(Rule1.history.map { it.second }).containsExactly(
//      objectId {
//        this.callingSourceId = sourceId {
//          this.mainSource = empty { }
//        }
//        this.bibixVersion = "0.1.0"
//        this.ruleSourceId = sourceId { this.preloadedPlugin = "abc" }
//        this.ruleName = "rule1"
//        this.className = "com.giyeok.bibix.interpreter.hash.Rule1"
//        this.methodName = "build"
//        this.argsMap = argsMap {
//          this.pairs.add(argPair {
//            this.name = "a"
//            this.value = bibixValue {
//              this.stringValue = "hello"
//            }
//          })
//          this.pairs.add(argPair {
//            this.name = "b"
//            this.value = bibixValue {
//              this.stringValue = "world"
//            }
//          })
//        }
//      })

    assertThat(interpreter.userBuildRequest("ddd")).isEqualTo(StringValue("Great!"))

    val objHashMap2 = interpreter.g.getObjHashMap()
    val objMemoMap2 = interpreter.g.getObjMemoMap()
    assertThat(objHashMap2).hasSize(2)
    assertThat(objMemoMap2).hasSize(2)
    assertThat(objHashMap2.keys).isEqualTo(objMemoMap2.keys)

    assertThat(Rule1.history.map { it.first }).containsExactly(
      "fe9154991d6910cd7d9eace18b0a1fe37f5c74a1",
      "3e901314e65cd7fa81f8cfdde57118598aee21f0",
    )
//    assertThat(Rule1.history.map { it.second }).containsExactly(
//      objectId {
//        this.callingSourceId = sourceId {
//          this.mainSource = empty { }
//        }
//        this.bibixVersion = "0.1.0"
//        this.ruleSourceId = sourceId { this.preloadedPlugin = "abc" }
//        this.ruleName = "rule1"
//        this.className = "com.giyeok.bibix.interpreter.hash.Rule1"
//        this.methodName = "build"
//        this.argsMap = argsMap {
//          this.pairs.add(argPair {
//            this.name = "a"
//            this.value = bibixValue {
//              this.stringValue = "hello"
//            }
//          })
//          this.pairs.add(argPair {
//            this.name = "b"
//            this.value = bibixValue {
//              this.stringValue = "world"
//            }
//          })
//        }
//      },
//      objectId {
//        this.callingSourceId = sourceId {
//          this.preloadedPlugin = "abc"
//        }
//        this.bibixVersion = "0.1.0"
//        this.ruleSourceId = sourceId { this.preloadedPlugin = "abc" }
//        this.ruleName = "rule1"
//        this.className = "com.giyeok.bibix.interpreter.hash.Rule1"
//        this.methodName = "build"
//        this.argsMap = argsMap {
//          this.pairs.add(argPair {
//            this.name = "a"
//            this.value = bibixValue {
//              this.stringValue = "hello"
//            }
//          })
//          this.pairs.add(argPair {
//            this.name = "b"
//            this.value = bibixValue {
//              this.stringValue = "world"
//            }
//          })
//        }
//      })
  }

  @Test
  fun testExternPlugins(): Unit = runBlocking {
    val fs = Jimfs.newFileSystem()

    val script = """
      import "subproject" as abc
      
      aaa = abc.rule2("hello", "world")
      bbb = abc.rule2(a="hello", b="world")
      ccc = abc.rule2(b="world", a="hello")
      ddd = abc.rule2("good!", "great!")
    """.trimIndent()
    fs.getPath("/build.bbx").writeText(script)

    val subproject = """
      import jvm
      
      impl = jvm.fakeClassPaths
      
      def rule2(a: string, b: string): string = impl:com.giyeok.bibix.interpreter.hash.Rule2
      
      qqq = rule2("hello", "world")
    """.trimIndent()
    Files.createDirectory(fs.getPath("/subproject"))
    fs.getPath("/subproject/build.bbx").writeText(subproject)

    val jvmPlugin = PreloadedPlugin.fromScript(
      "com.giyeok.bibix.plugins.jvm",
      """
        class ClassPaths(cps: set<path>)
        fakeClassPaths = ClassPaths([])
      """.trimIndent(),
      PluginInstanceProvider()
    )

    val classWorld = ClassWorld()
    val classRealm = classWorld.newRealm("realm-test")
    classRealm.addURL(File(".").toURI().toURL())

    val interpreter = testInterpreter(
      fs, "/", mapOf("jvm" to jvmPlugin),
      pluginImplProvider = FakePluginImplProvider { _, _, className ->
        val cls = classRealm.loadClass(className)
        cls.getDeclaredConstructor().newInstance()
      })

    assertThat(interpreter.userBuildRequest("aaa")).isEqualTo(StringValue("awesome!"))
    assertThat(interpreter.userBuildRequest("bbb")).isEqualTo(StringValue("awesome!"))
    assertThat(interpreter.userBuildRequest("ccc")).isEqualTo(StringValue("awesome!"))

    val objHashMap1 = interpreter.g.getObjHashMap()
    val objMemoMap1 = interpreter.g.getObjMemoMap()
    assertThat(objHashMap1).hasSize(1)
    assertThat(objMemoMap1).hasSize(1)
    assertThat(objHashMap1.keys).isEqualTo(objMemoMap1.keys)

    assertThat(Rule2.history.map { it.first })
      .containsExactly("1cce93a41349885755ace4aa317b7a0d6e2a1e19")
//    assertThat(Rule2.history.map { it.second }).containsExactly(
//      objectId {
//        this.callingSourceId = sourceId {
//          this.mainSource = empty { }
//        }
//        this.ruleImplObjhash = ByteString.fromHex("d1247699531c349d71499a996fce0fa81c9f57ce")
//        this.ruleSourceId = sourceId {
//          this.externPluginObjhash = externalBibixProject { }
//        }
//        this.ruleName = "rule2"
//        this.className = "com.giyeok.bibix.interpreter.hash.Rule2"
//        this.methodName = "build"
//        this.argsMap = argsMap {
//          this.pairs.add(argPair {
//            this.name = "a"
//            this.value = bibixValue {
//              this.stringValue = "hello"
//            }
//          })
//          this.pairs.add(argPair {
//            this.name = "b"
//            this.value = bibixValue {
//              this.stringValue = "world"
//            }
//          })
//        }
//      })
  }
}

class Rule1 {
  companion object {
    @JvmStatic
    val history = mutableListOf<Pair<String, TargetIdData>>()

    @JvmStatic
    fun calledWith(targetId: String, targetIdData: TargetIdData) {
      history.add(Pair(targetId, targetIdData))
    }
  }

  fun build(context: BuildContext): BibixValue {
    calledWith(context.targetId, context.targetIdData)
    return StringValue("Great!")
  }
}

class Rule2 {
  companion object {
    @JvmStatic
    val history = mutableListOf<Pair<String, TargetIdData>>()

    @JvmStatic
    fun calledWith(targetId: String, targetIdData: TargetIdData) {
      history.add(Pair(targetId, targetIdData))
    }
  }

  fun build(context: BuildContext): BibixValue {
    calledWith(context.targetId, context.targetIdData)
    return StringValue("awesome!")
  }
}
