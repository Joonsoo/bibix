package com.giyeok.bibix.plugins.protobuf

import com.giyeok.bibix.base.BuildContext
import com.giyeok.bibix.base.BuildRuleReturn
import com.giyeok.bibix.plugins.protobuf.Compile.OS
import com.giyeok.bibix.plugins.protobuf.Compile.ProtoSchema
import java.io.File

interface CompileInterface {

  fun schema(
    context: BuildContext,
    srcs: List<File>,
    deps: List<ProtoSchema>,
    protocPath: File,
  ): BuildRuleReturn

  fun protoset(
    context: BuildContext,
    schema: ProtoSchema,
    os: OS,
    protocPath: File,
    outputFileName: String?,
  ): BuildRuleReturn

  fun cpp(
    context: BuildContext,
    schema: ProtoSchema,
    os: OS,
    protocPath: File,
  ): BuildRuleReturn

  fun csharp(
    context: BuildContext,
    schema: ProtoSchema,
    os: OS,
    protocPath: File,
  ): BuildRuleReturn

  fun java(
    context: BuildContext,
    schema: ProtoSchema,
    os: OS,
    protocPath: File,
  ): BuildRuleReturn

  fun javascript(
    context: BuildContext,
    schema: ProtoSchema,
    os: OS,
    protocPath: File,
  ): BuildRuleReturn

  fun kotlin(
    context: BuildContext,
    schema: ProtoSchema,
    os: OS,
    protocPath: File,
  ): BuildRuleReturn

  fun php(
    context: BuildContext,
    schema: ProtoSchema,
    os: OS,
    protocPath: File,
  ): BuildRuleReturn

  fun python(
    context: BuildContext,
    schema: ProtoSchema,
    os: OS,
    protocPath: File,
  ): BuildRuleReturn

  fun ruby(
    context: BuildContext,
    schema: ProtoSchema,
    os: OS,
    protocPath: File,
  ): BuildRuleReturn

  fun dart(
    context: BuildContext,
    schema: ProtoSchema,
    os: OS,
    protocPath: File,
  ): BuildRuleReturn
}
