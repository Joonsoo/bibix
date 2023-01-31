package com.giyeok.bibix.interpreter.hash

import com.giyeok.bibix.*
import com.giyeok.bibix.BibixIdProto.ObjectId
import com.giyeok.bibix.base.*
import com.giyeok.bibix.interpreter.FakePluginClassLoader
import com.giyeok.bibix.interpreter.testInterpreter
import com.giyeok.bibix.plugins.Classes
import com.giyeok.bibix.plugins.PreloadedPlugin
import com.google.common.jimfs.Jimfs
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.extensions.proto.ProtoTruth.assertThat
import com.google.protobuf.ByteString
import com.google.protobuf.empty
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
      Classes(Rule1::class.java)
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

    assertThat(Rule1.history.map { it.first }).containsExactly("db7000dcc00b327d87bb8eb07793ca36961b4160")
    assertThat(Rule1.history.map { it.second }).containsExactly(
      objectId {
        this.callingSourceId = sourceId {
          this.mainSource = empty { }
        }
        this.bibixVersion = "0.0.4"
        this.ruleSourceId = sourceId { this.preloadedPlugin = "abc" }
        this.ruleName = "rule1"
        this.className = "com.giyeok.bibix.interpreter.hash.Rule1"
        this.methodName = "build"
        this.argsMap = argsMap {
          this.pairs.add(argPair {
            this.name = "a"
            this.value = bibixValue {
              this.stringValue = "hello"
            }
          })
          this.pairs.add(argPair {
            this.name = "b"
            this.value = bibixValue {
              this.stringValue = "world"
            }
          })
        }
      })

    assertThat(interpreter.userBuildRequest("ddd")).isEqualTo(StringValue("Great!"))

    val objHashMap2 = interpreter.g.getObjHashMap()
    val objMemoMap2 = interpreter.g.getObjMemoMap()
    assertThat(objHashMap2).hasSize(2)
    assertThat(objMemoMap2).hasSize(2)
    assertThat(objHashMap2.keys).isEqualTo(objMemoMap2.keys)

    assertThat(Rule1.history.map { it.first }).containsExactly(
      "db7000dcc00b327d87bb8eb07793ca36961b4160",
      "0829664783262acd474f26802263def7ca2f63cf",
    )
    assertThat(Rule1.history.map { it.second }).containsExactly(
      objectId {
        this.callingSourceId = sourceId {
          this.mainSource = empty { }
        }
        this.bibixVersion = "0.0.4"
        this.ruleSourceId = sourceId { this.preloadedPlugin = "abc" }
        this.ruleName = "rule1"
        this.className = "com.giyeok.bibix.interpreter.hash.Rule1"
        this.methodName = "build"
        this.argsMap = argsMap {
          this.pairs.add(argPair {
            this.name = "a"
            this.value = bibixValue {
              this.stringValue = "hello"
            }
          })
          this.pairs.add(argPair {
            this.name = "b"
            this.value = bibixValue {
              this.stringValue = "world"
            }
          })
        }
      },
      objectId {
        this.callingSourceId = sourceId {
          this.preloadedPlugin = "abc"
        }
        this.bibixVersion = "0.0.4"
        this.ruleSourceId = sourceId { this.preloadedPlugin = "abc" }
        this.ruleName = "rule1"
        this.className = "com.giyeok.bibix.interpreter.hash.Rule1"
        this.methodName = "build"
        this.argsMap = argsMap {
          this.pairs.add(argPair {
            this.name = "a"
            this.value = bibixValue {
              this.stringValue = "hello"
            }
          })
          this.pairs.add(argPair {
            this.name = "b"
            this.value = bibixValue {
              this.stringValue = "world"
            }
          })
        }
      })
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
      Classes()
    )

    val classWorld = ClassWorld()
    val classRealm = classWorld.newRealm("realm-test")
    classRealm.addURL(File(".").toURI().toURL())

    val interpreter = testInterpreter(
      fs, "/", mapOf("jvm" to jvmPlugin),
      pluginClassLoader = FakePluginClassLoader { _, className ->
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

    assertThat(Rule2.history.map { it.first }).containsExactly(
      "daa07c607289197fd533ffbf5a6e81e66b3a5beb",
    )
    assertThat(Rule2.history.map { it.second }).containsExactly(
      objectId {
        this.callingSourceId = sourceId {
          this.mainSource = empty { }
        }
        this.ruleImplObjhash = ByteString.fromHex("d1247699531c349d71499a996fce0fa81c9f57ce")
        this.className = "com.giyeok.bibix.interpreter.hash.Rule2"
        this.methodName = "build"
        this.argsMap = argsMap {
          this.pairs.add(argPair {
            this.name = "a"
            this.value = bibixValue {
              this.stringValue = "hello"
            }
          })
          this.pairs.add(argPair {
            this.name = "b"
            this.value = bibixValue {
              this.stringValue = "world"
            }
          })
        }
      })
  }
}

class Rule1 {
  companion object {
    @JvmStatic
    val history = mutableListOf<Pair<String, ObjectId>>()

    @JvmStatic
    fun calledWith(objectIdHash: String, objectId: ObjectId) {
      history.add(Pair(objectIdHash, objectId))
    }
  }

  fun build(context: BuildContext): BibixValue {
    calledWith(context.objectIdHash, context.objectId)
    return StringValue("Great!")
  }
}

class Rule2 {
  companion object {
    @JvmStatic
    val history = mutableListOf<Pair<String, ObjectId>>()

    @JvmStatic
    fun calledWith(objectIdHash: String, objectId: ObjectId) {
      history.add(Pair(objectIdHash, objectId))
    }
  }

  fun build(context: BuildContext): BibixValue {
    calledWith(context.objectIdHash, context.objectId)
    return StringValue("awesome!")
  }
}
