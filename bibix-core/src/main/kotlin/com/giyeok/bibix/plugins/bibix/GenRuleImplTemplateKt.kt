package com.giyeok.bibix.plugins.bibix

import com.giyeok.bibix.base.*

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

  private fun convertExpr(expr: String, bibixType: TypeValue): String = when (bibixType) {
    TypeValue.AnyTypeValue -> expr
    TypeValue.BooleanTypeValue -> "($expr as BooleanValue).value"
    TypeValue.StringTypeValue -> "($expr as StringValue).value"
    TypeValue.PathTypeValue -> "($expr as PathValue).path"
    TypeValue.FileTypeValue -> "($expr as FileValue).file"
    TypeValue.DirectoryTypeValue -> "($expr as DirectoryValue).directory"
    is TypeValue.ClassTypeValue -> "${bibixType.className.tokens.last()}.fromBibix($expr)"
    is TypeValue.EnumTypeValue -> TODO()
    is TypeValue.ListTypeValue ->
      "($expr as ListValue).values.map { ${convertExpr("it", bibixType.elemType)} }"
    is TypeValue.SetTypeValue ->
      "($expr as SetValue).values.map { ${convertExpr("it", bibixType.elemType)} }"
    is TypeValue.TupleTypeValue -> TODO()
    is TypeValue.NamedTupleTypeValue -> TODO()
    is TypeValue.UnionTypeValue -> TODO()
    TypeValue.BuildRuleDefTypeValue -> TODO()
    TypeValue.ActionRuleDefTypeValue -> TODO()
    TypeValue.TypeTypeValue -> TODO()
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
    val implName = (context.arguments.getValue("implName") as StringValue).value.split('.')
    val implClassName = implName.last()
    val implClassPkg = implName.dropLast(1)
    val implInterfaceName =
      (context.arguments.getValue("implInterfaceName") as StringValue).value.split('.')
    val implInterfaceClassName = implInterfaceName.last()
    val implInterfaceClassPkg = implInterfaceName.dropLast(1)
    return BuildRuleReturn.getClassInfos(classTypes) { classTypeDetails ->
      if (implClassPkg.isNotEmpty()) {
        println("package ${implClassPkg.joinToString(".")}")
      }
      println()
      println("class $implClassName(val impl: $implInterfaceClassName) {")
      println("  constructor() : this($implClassName)")
      println()
      classTypeDetails.forEach { cls ->
        val clsName = cls.className.tokens.last()
        println("  data class $clsName(")
        when (cls.bodyType) {
          is TypeValue.NamedTupleTypeValue ->
            cls.bodyType.elemTypes.forEach { field ->
              println("    val ${field.first}: ${bibixTypeToKtType(field.second)},")
            }
          else -> println("    val value: ${bibixTypeToKtType(cls.bodyType)}")
        }
        println("  ) {")
        println("    companion object {")
        println("      fun fromBibix(value: BibixValue): $clsName {")
        println("        value as ClassInstanceValue")
        println("        check(value.className.tokens == listOf(\"${cls.className.tokens.joinToString { "\"$it\"" }}\"))")
        when (cls.bodyType) {
          is TypeValue.NamedTupleTypeValue -> {
            println("        val body = value.value as NamedTupleValue")
            cls.bodyType.elemTypes.forEachIndexed { index, field ->
              val convert = convertExpr("body.getValue(\"${field.first}\")", field.second)
              println("        val ${field.first} = $convert")
            }
            println("        return $clsName(${cls.bodyType.elemTypes.joinToString { it.first }})")
          }
          else -> {
            println("        val value = ${convertExpr("value.value", cls.bodyType)}")
            println("        return $clsName(value)")
          }
        }
        println("      }")
        println("    }")
        println("    fun toBibix(): BibixValue {")
        println("      ")
        println("    }")
        println("  }")
      }
      println("}")
      TODO()
    }
  }
}
