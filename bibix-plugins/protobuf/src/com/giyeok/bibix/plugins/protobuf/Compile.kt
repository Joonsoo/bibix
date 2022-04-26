package com.giyeok.bibix.plugins.protobuf

import com.giyeok.bibix.base.*
import java.io.File

class Compile(val impl: CompileInterface) {
  constructor() : this(CompileImpl())

  data class ProtoSchema(
    val schemaFiles: List<File>,
    val includes: List<File>,
  ) {
    companion object {
      fun fromBibix(value: BibixValue): ProtoSchema {
        value as ClassInstanceValue
        check(value.className.tokens == listOf("ProtoSchema"))
        val body = value.value as NamedTupleValue
        val schemaFiles = (body.pairs[0].second as SetValue).values.map { (it as FileValue).file }
        val includes = (body.pairs[1].second as SetValue).values.map { (it as PathValue).path }
        return ProtoSchema(schemaFiles, includes)
      }
    }

    fun toBibix() = NClassInstanceValue(
      "protobuf.ProtoSchema",
      NamedTupleValue(
        "schemaFiles" to SetValue(schemaFiles.map { FileValue(it) }),
        "includes" to SetValue(includes.map { PathValue(it) }),
      )
    )
  }

  enum class OS {
    linux,
    osx,
    windows,
  }

  fun schema(context: BuildContext): BuildRuleReturn {
    val srcs =
      (context.arguments.getValue("srcs") as SetValue).values.map { (it as FileValue).file }
    val deps =
      (context.arguments.getValue("deps") as SetValue).values.map { ProtoSchema.fromBibix(it) }
    val protocPath = (context.arguments.getValue("protocPath") as DirectoryValue).directory
    return impl.schema(context, srcs, deps, protocPath)
  }

  fun protoset(context: BuildContext): BuildRuleReturn {
    val schema = ProtoSchema.fromBibix(context.arguments.getValue("schema"))
    val os = OS.valueOf((context.arguments.getValue("os") as StringValue).value)
    val protocPath = (context.arguments.getValue("protocPath") as DirectoryValue).directory
    val outputFileName =
      context.arguments["outputFileName"]?.let { arg -> (arg as StringValue).value }
    return impl.protoset(context, schema, os, protocPath, outputFileName)
  }

  fun cpp(context: BuildContext): BuildRuleReturn {
    val schema = ProtoSchema.fromBibix(context.arguments.getValue("schema"))
    val os = OS.valueOf((context.arguments.getValue("os") as StringValue).value)
    val protocPath = (context.arguments.getValue("protocPath") as DirectoryValue).directory
    return impl.cpp(context, schema, os, protocPath)
  }

  fun csharp(context: BuildContext): BuildRuleReturn {
    val schema = ProtoSchema.fromBibix(context.arguments.getValue("schema"))
    val os = OS.valueOf((context.arguments.getValue("os") as StringValue).value)
    val protocPath = (context.arguments.getValue("protocPath") as DirectoryValue).directory
    return impl.csharp(context, schema, os, protocPath)
  }

  fun java(context: BuildContext): BuildRuleReturn {
    val schema = ProtoSchema.fromBibix(context.arguments.getValue("schema"))
    val os = OS.valueOf((context.arguments.getValue("os") as StringValue).value)
    val protocPath = (context.arguments.getValue("protocPath") as DirectoryValue).directory
    return impl.java(context, schema, os, protocPath)
  }

  fun javascript(context: BuildContext): BuildRuleReturn {
    val schema = ProtoSchema.fromBibix(context.arguments.getValue("schema"))
    val os = OS.valueOf((context.arguments.getValue("os") as StringValue).value)
    val protocPath = (context.arguments.getValue("protocPath") as DirectoryValue).directory
    return impl.javascript(context, schema, os, protocPath)
  }

  fun kotlin(context: BuildContext): BuildRuleReturn {
    val schema = ProtoSchema.fromBibix(context.arguments.getValue("schema"))
    val os = OS.valueOf((context.arguments.getValue("os") as StringValue).value)
    val protocPath = (context.arguments.getValue("protocPath") as DirectoryValue).directory
    return impl.kotlin(context, schema, os, protocPath)
  }

  fun php(context: BuildContext): BuildRuleReturn {
    val schema = ProtoSchema.fromBibix(context.arguments.getValue("schema"))
    val os = OS.valueOf((context.arguments.getValue("os") as StringValue).value)
    val protocPath = (context.arguments.getValue("protocPath") as DirectoryValue).directory
    return impl.php(context, schema, os, protocPath)
  }

  fun python(context: BuildContext): BuildRuleReturn {
    val schema = ProtoSchema.fromBibix(context.arguments.getValue("schema"))
    val os = OS.valueOf((context.arguments.getValue("os") as StringValue).value)
    val protocPath = (context.arguments.getValue("protocPath") as DirectoryValue).directory
    return impl.python(context, schema, os, protocPath)
  }

  fun ruby(context: BuildContext): BuildRuleReturn {
    val schema = ProtoSchema.fromBibix(context.arguments.getValue("schema"))
    val os = OS.valueOf((context.arguments.getValue("os") as StringValue).value)
    val protocPath = (context.arguments.getValue("protocPath") as DirectoryValue).directory
    return impl.ruby(context, schema, os, protocPath)
  }

  fun dart(context: BuildContext): BuildRuleReturn {
    val schema = ProtoSchema.fromBibix(context.arguments.getValue("schema"))
    val os = OS.valueOf((context.arguments.getValue("os") as StringValue).value)
    val protocPath = (context.arguments.getValue("protocPath") as DirectoryValue).directory
    return impl.dart(context, schema, os, protocPath)
  }
}
