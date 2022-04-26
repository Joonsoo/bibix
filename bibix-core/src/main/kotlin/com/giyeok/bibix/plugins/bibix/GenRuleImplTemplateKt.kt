package com.giyeok.bibix.plugins.bibix

import com.giyeok.bibix.base.*
import java.io.File
import java.io.PrintWriter

class GenRuleImplTemplateKt {
  private fun bibixTypeToKtType(bibixType: TypeValue): String =
    when (bibixType) {
      TypeValue.AnyTypeValue -> "BibixValue"
      TypeValue.BooleanTypeValue -> "Boolean"
      TypeValue.StringTypeValue -> "String"
      TypeValue.PathTypeValue, TypeValue.FileTypeValue, TypeValue.DirectoryTypeValue -> "File"
      is TypeValue.ClassTypeValue -> bibixType.className.tokens.last()
      is TypeValue.EnumTypeValue -> bibixType.enumTypeName.tokens.last()
      is TypeValue.ListTypeValue -> "List<${bibixTypeToKtType(bibixType.elemType)}>"
      is TypeValue.SetTypeValue -> "List<${bibixTypeToKtType(bibixType.elemType)}>"
      is TypeValue.TupleTypeValue -> TODO()
      is TypeValue.NamedTupleTypeValue -> TODO()
      is TypeValue.UnionTypeValue -> TODO()
      TypeValue.BuildRuleDefTypeValue -> TODO()
      TypeValue.ActionRuleDefTypeValue -> TODO()
      TypeValue.TypeTypeValue -> TODO()
    }

  private fun bibixValueToKt(expr: String, bibixType: TypeValue): String = when (bibixType) {
    TypeValue.AnyTypeValue -> expr
    TypeValue.BooleanTypeValue -> "($expr as BooleanValue).value"
    TypeValue.StringTypeValue -> "($expr as StringValue).value"
    TypeValue.PathTypeValue -> "($expr as PathValue).path"
    TypeValue.FileTypeValue -> "($expr as FileValue).file"
    TypeValue.DirectoryTypeValue -> "($expr as DirectoryValue).directory"
    is TypeValue.ClassTypeValue -> "${bibixType.className.tokens.last()}.fromBibix($expr)"
    is TypeValue.EnumTypeValue -> {
      val enumString = bibixValueToKt(expr, TypeValue.StringTypeValue)
      "${bibixType.enumTypeName.tokens.last()}.valueOf($enumString)"
    }
    is TypeValue.ListTypeValue ->
      "($expr as ListValue).values.map { ${bibixValueToKt("it", bibixType.elemType)} }"
    is TypeValue.SetTypeValue ->
      "($expr as SetValue).values.map { ${bibixValueToKt("it", bibixType.elemType)} }"
    is TypeValue.TupleTypeValue -> TODO()
    is TypeValue.NamedTupleTypeValue -> TODO()
    is TypeValue.UnionTypeValue -> TODO()
    TypeValue.BuildRuleDefTypeValue -> TODO()
    TypeValue.ActionRuleDefTypeValue -> TODO()
    TypeValue.TypeTypeValue -> TODO()
  }

  private fun ktValueToBibix(expr: String, bibixType: TypeValue): String = when (bibixType) {
    TypeValue.AnyTypeValue -> expr
    TypeValue.BooleanTypeValue -> "BooleanValue($expr)"
    TypeValue.StringTypeValue -> "StringValue($expr)"
    TypeValue.PathTypeValue -> "PathValue($expr)"
    TypeValue.FileTypeValue -> "FileValue($expr)"
    TypeValue.DirectoryTypeValue -> "DirectoryValue($expr)"
    is TypeValue.ClassTypeValue -> "$expr.toBibix()"
    is TypeValue.EnumTypeValue -> TODO()
    is TypeValue.ListTypeValue ->
      "ListValue($expr.map { ${ktValueToBibix("it", bibixType.elemType)} })"
    is TypeValue.SetTypeValue ->
      "SetValue($expr.map { ${ktValueToBibix("it", bibixType.elemType)} })"
    is TypeValue.TupleTypeValue -> TODO()
    is TypeValue.NamedTupleTypeValue -> TODO()
    is TypeValue.UnionTypeValue -> TODO()
    TypeValue.BuildRuleDefTypeValue -> TODO()
    TypeValue.ActionRuleDefTypeValue -> TODO()
    TypeValue.TypeTypeValue -> TODO()
  }

  private fun generateClassType(p: PrintWriter, cls: TypeValue.ClassTypeDetail) {
    val clsName = cls.className.tokens.last()
    p.println("  data class $clsName(")
    when (cls.bodyType) {
      is TypeValue.NamedTupleTypeValue ->
        cls.bodyType.elemTypes.forEach { field ->
          p.println("    val ${field.first}: ${bibixTypeToKtType(field.second)},")
        }
      else -> p.println("    val value: ${bibixTypeToKtType(cls.bodyType)}")
    }
    p.println("  ) {")
    p.println("    companion object {")
    p.println("      fun fromBibix(value: BibixValue): $clsName {")
    p.println("        value as ClassInstanceValue")
    p.println("        check(value.className.tokens == listOf(${cls.className.tokens.joinToString { "\"$it\"" }}))")
    when (cls.bodyType) {
      is TypeValue.NamedTupleTypeValue -> {
        p.println("        val body = value.value as NamedTupleValue")
        cls.bodyType.elemTypes.forEachIndexed { index, field ->
          val convert = bibixValueToKt("body.pairs[$index].second", field.second)
          p.println("        val ${field.first} = $convert")
        }
        p.println("        return $clsName(${cls.bodyType.elemTypes.joinToString { it.first }})")
      }
      else -> {
        val convert = bibixValueToKt("value.value", cls.bodyType)
        p.println("        val value = $convert")
        p.println("        return $clsName(value)")
      }
    }
    p.println("      }")
    p.println("    }")
    p.println("    fun toBibix() = NClassInstanceValue(")
    p.println("      \"${cls.relativeName.joinToString(".")}\",")
    when (cls.bodyType) {
      is TypeValue.NamedTupleTypeValue -> {
        p.println("      NamedTupleValue(")
        cls.bodyType.elemTypes.forEachIndexed { index, field ->
          val convert = ktValueToBibix(field.first, field.second)
          p.println("        \"${field.first}\" to $convert,")
        }
        p.println("      )")
      }
      else -> {
        p.println("      ${ktValueToBibix("value", cls.bodyType)}")
      }
    }
    p.println("    )")
    p.println("  }")
  }

  private fun generateEnumType(p: PrintWriter, type: TypeValue.EnumTypeValue) {
    p.println("  enum class ${type.enumTypeName.tokens.last()} {")
    type.enumValues.forEach { enumValue ->
      p.println("    $enumValue,")
    }
    p.println("  }")
  }

  private fun generateRuleMethod(p: PrintWriter, rule: BuildRuleDefValue) {
    p.println("  fun ${rule.implMethodName}(context: BuildContext): BuildRuleReturn {")
    rule.params.forEach { param ->
      val paramConvert = if (param.optional) {
        "context.arguments[\"${param.name}\"]?.let { arg -> ${bibixValueToKt("arg", param.type)} }"
      } else {
        bibixValueToKt("context.arguments.getValue(\"${param.name}\")", param.type)
      }
      p.println("    val ${param.name} = $paramConvert")
    }
    p.println("    return impl.${rule.implMethodName}(context, ${rule.params.joinToString { it.name }})")
    p.println("  }")
  }

  private fun generateImplMethod(p: PrintWriter, rule: BuildRuleDefValue) {
    p.println("  fun ${rule.implMethodName}(")
    p.println("    context: BuildContext,")
    rule.params.forEach { param ->
      p.println("    ${param.name}: ${bibixTypeToKtType(param.type)}${if (param.optional) "?" else ""},")
    }
    p.println("  ): BuildRuleReturn")
  }

  fun build(context: BuildContext): BuildRuleReturn {
    val rules = (context.arguments.getValue("rules") as SetValue).values.map {
      it as BuildRuleDefValue
    }
    check(rules.map { it.implClass }.toSet().size == 1)
    val types = (context.arguments.getValue("types") as SetValue).values.map {
      it as TypeValue
    }
    val classTypes = types.filterIsInstance<TypeValue.ClassTypeValue>().map { it.className }
    val ruleNames = rules.map { it.implClass }.distinct()
    check(ruleNames.size == 1)
    val ruleName = ruleNames[0].split('.')
    val ruleClassName = ruleName.last()
    val ruleClassPkg = ruleName.dropLast(1)
    val implName = (context.arguments.getValue("implName") as StringValue).value.split('.')
    val implClassName = implName.last()
    val implClassPkg = implName.dropLast(1)
    val implInterfaceName =
      (context.arguments.getValue("implInterfaceName") as StringValue).value.split('.')
    val implInterfaceClassName = implInterfaceName.last()
    val implInterfaceClassPkg = implInterfaceName.dropLast(1)

    val implClassFile = File(context.destDirectory, "$ruleClassName.kt")
    val implInterfaceFile = File(context.destDirectory, "$implInterfaceClassName.kt")

    return BuildRuleReturn.getClassInfos(classTypes) { classTypeDetails ->
      val classTypeDetailsMap = classTypeDetails.associateBy { it.className }
      PrintWriter(implClassFile).use { implClassPrinter ->
        if (ruleClassPkg.isNotEmpty()) {
          implClassPrinter.println("package ${ruleClassPkg.joinToString(".")}")
        }
        implClassPrinter.println()
        implClassPrinter.println("import com.giyeok.bibix.base.*")
        implClassPrinter.println("import java.io.File")
        implClassPrinter.println()
        implClassPrinter.println("class $ruleClassName(val impl: $implInterfaceClassName) {")
        implClassPrinter.println("  constructor() : this($implClassName())")
        types.forEach { type ->
          implClassPrinter.println()
          when (type) {
            is TypeValue.ClassTypeValue ->
              generateClassType(implClassPrinter, classTypeDetailsMap.getValue(type.className))
            is TypeValue.EnumTypeValue ->
              generateEnumType(implClassPrinter, type)
            else -> TODO()
          }
        }
        if (rules.isNotEmpty()) {
          rules.forEach { rule ->
            implClassPrinter.println()
            generateRuleMethod(implClassPrinter, rule)
          }
        }
        implClassPrinter.println("}")
      }
      PrintWriter(implInterfaceFile).use { implInterfacePrinter ->
        if (implInterfaceClassPkg.isNotEmpty()) {
          implInterfacePrinter.println("package ${implInterfaceClassPkg.joinToString(".")}")
        }
        implInterfacePrinter.println()
        implInterfacePrinter.println("import com.giyeok.bibix.base.*")
        implInterfacePrinter.println("import java.io.File")
        implInterfacePrinter.println("import ${ruleName.joinToString(".")}.*")
        implInterfacePrinter.println()
        implInterfacePrinter.println("interface $implInterfaceClassName {")
        rules.forEach { rule ->
          implInterfacePrinter.println()
          generateImplMethod(implInterfacePrinter, rule)
        }
        implInterfacePrinter.println("}")
      }
      BuildRuleReturn.value(
        NamedTupleValue(
          "implClass" to FileValue(implClassFile),
          "interfaceClass" to FileValue(implInterfaceFile)
        )
      )
    }
  }
}
