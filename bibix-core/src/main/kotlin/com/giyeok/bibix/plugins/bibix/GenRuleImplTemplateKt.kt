package com.giyeok.bibix.plugins.bibix

import com.giyeok.bibix.base.*
import java.io.PrintWriter
import java.nio.file.Files
import kotlin.io.path.deleteIfExists

class GenRuleImplTemplateKt {
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

  fun build(context: BuildContext): BibixValue {
    val rules = (context.arguments.getValue("rules") as SetValue).values.map {
      it as BuildRuleDefValue
    }
    check(rules.map { it.implClassName }.toSet().size == 1)
    val ruleNames = rules.map { it.implClassName }.distinct()
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

    val implClassFile = context.destDirectory.resolve("$ruleClassName.kt")
    val implInterfaceFile = context.destDirectory.resolve("$implInterfaceClassName.kt")

    implClassFile.deleteIfExists()
    implInterfaceFile.deleteIfExists()

    PrintWriter(Files.newBufferedWriter(implClassFile)).use { implClassPrinter ->
      if (ruleClassPkg.isNotEmpty()) {
        implClassPrinter.println("package ${ruleClassPkg.joinToString(".")}")
      }
      implClassPrinter.println()
      implClassPrinter.println("import com.giyeok.bibix.base.*")
      implClassPrinter.println("import java.nio.file.Path")
      implClassPrinter.println()
      implClassPrinter.println("class $ruleClassName(val impl: $implInterfaceClassName) {")
      implClassPrinter.println("  constructor() : this($implClassName())")
      if (rules.isNotEmpty()) {
        rules.forEach { rule ->
          implClassPrinter.println()
          generateRuleMethod(implClassPrinter, rule)
        }
      }
      implClassPrinter.println("}")
    }
    PrintWriter(Files.newBufferedWriter(implInterfaceFile)).use { implInterfacePrinter ->
      if (implInterfaceClassPkg.isNotEmpty()) {
        implInterfacePrinter.println("package ${implInterfaceClassPkg.joinToString(".")}")
      }
      implInterfacePrinter.println()
      implInterfacePrinter.println("import com.giyeok.bibix.base.*")
      implInterfacePrinter.println("import java.nio.file.Path")
      implInterfacePrinter.println()
      implInterfacePrinter.println("interface $implInterfaceClassName {")
      rules.forEachIndexed { index, rule ->
        if (index > 0) {
          implInterfacePrinter.println()
        }
        generateImplMethod(implInterfacePrinter, rule)
      }
      implInterfacePrinter.println("}")
    }
    return NClassInstanceValue(
      "RuleImplTemplate",
      mapOf(
        "implClass" to FileValue(implClassFile),
        "interfaceClass" to FileValue(implInterfaceFile),
      )
    )
  }
}
