package com.giyeok.bibix.plugins.bibix

import com.giyeok.bibix.base.*
import com.giyeok.bibix.plugins.bibix.GenCommon.bibixTypeToKtType
import com.giyeok.bibix.plugins.bibix.GenCommon.bibixValueToKt
import com.giyeok.bibix.plugins.bibix.GenCommon.ktValueToBibix
import java.io.PrintWriter
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists

class GenClassesKt {
  companion object {
    fun generateClassType(
      p: PrintWriter,
      cls: TypeValue.ClassTypeDetail,
      superClass: CName?,
      indent: String,
    ) {
      val clsName = cls.className.tokens.last()
      p.println("${indent}data class $clsName(")
      when (val bodyType = cls.bodyType) {
        is TypeValue.NamedTupleTypeValue ->
          bodyType.elemTypes.forEach { field ->
            p.println("$indent  val ${field.first}: ${bibixTypeToKtType(field.second)},")
          }
        else -> p.println("$indent  val value: ${bibixTypeToKtType(cls.bodyType)}")
      }
      if (superClass != null) {
        p.println("$indent): ${superClass.tokens.last()}() {")
      } else {
        p.println("$indent) {")
      }
      p.println("$indent  companion object {")
      p.println("$indent    fun fromBibix(value: BibixValue): $clsName {")
      p.println("$indent      value as ClassInstanceValue")
      p.println("$indent      check(value.className.tokens == listOf(${cls.className.tokens.joinToString { "\"$it\"" }}))")
      when (val bodyType = cls.bodyType) {
        is TypeValue.NamedTupleTypeValue -> {
          p.println("$indent      val body = value.value as NamedTupleValue")
          bodyType.elemTypes.forEachIndexed { index, field ->
            val convert = bibixValueToKt("body.pairs[$index].second", field.second)
            p.println("$indent      val ${field.first} = $convert")
          }
          p.println("$indent      return $clsName(${bodyType.elemTypes.joinToString { it.first }})")
        }
        else -> {
          val convert = bibixValueToKt("value.value", bodyType)
          p.println("$indent      val value = $convert")
          p.println("$indent      return $clsName(value)")
        }
      }
      p.println("$indent    }")
      p.println("$indent  }")
      p.println("$indent  fun toBibix() = NClassInstanceValue(")
      p.println("$indent    \"${cls.relativeName.joinToString(".")}\",")
      when (val bodyType = cls.bodyType) {
        is TypeValue.NamedTupleTypeValue -> {
          p.println("$indent    NamedTupleValue(")
          bodyType.elemTypes.forEachIndexed { index, field ->
            val convert = ktValueToBibix(field.first, field.second)
            p.println("$indent      \"${field.first}\" to $convert,")
          }
          p.println("$indent    )")
        }
        is TypeValue.UnionTypeValue -> TODO() // must not happen
        else -> {
          p.println("$indent    ${ktValueToBibix("value", bodyType)}")
        }
      }
      p.println("$indent  )")
      p.println("$indent}")
    }

    fun generateEnumType(p: PrintWriter, type: TypeValue.EnumTypeValue, indent: String) {
      p.println("${indent}enum class ${type.enumTypeName.tokens.last()} {")
      type.enumValues.forEach { enumValue ->
        p.println("$indent  $enumValue,")
      }
      p.println("$indent}")
    }
  }

  fun build(context: BuildContext): BuildRuleReturn {
    val types = (context.arguments.getValue("types") as SetValue).values.map {
      it as TypeValue
    }
    val packageName = (context.arguments["packageName"] as? StringValue)?.value
    val fileName = (context.arguments.getValue("fileName") as StringValue).value
    val outerClassName = (context.arguments["outerClassName"] as? StringValue)?.value

    val targetDir: Path = packageName?.split('.')?.fold(context.destDirectory) { path, name ->
      path.resolve(name)
    } ?: context.destDirectory
    val targetFile = targetDir.resolve(fileName)

    if (context.hashChanged) {
      targetDir.deleteIfExists()
      targetDir.createDirectories()

      val classTypes = types.filterIsInstance<TypeValue.ClassTypeValue>()
      return BuildRuleReturn.getClassInfos(classTypes.map { it.className }) { classTypeDetails ->
        PrintWriter(Files.newBufferedWriter(targetFile)).use { printer ->
          if (packageName != null) {
            printer.println("package $packageName")
            printer.println()
          }
          if (outerClassName != null) {
            printer.println("object $outerClassName {")
          }
          val classTypeDetailsMap = classTypeDetails.associateBy { it.className }
          // TODO classTypes중 union type body를 가진 클래스들은 super class가 되어야하니.. 별도로 추려서 처리할 필요가 있음
          val superclasses = mutableMapOf<CName, CName>()
          classTypes.forEach { clsType ->
            val detail = classTypeDetailsMap.getValue(clsType.className)
            if (detail.bodyType is TypeValue.UnionTypeValue) {
              (detail.bodyType as TypeValue.UnionTypeValue).types.forEach { subType ->
                if (subType is TypeValue.ClassTypeValue) {
                  superclasses[subType.className] = clsType.className
                }
              }
            }
          }
          val indent = if (outerClassName == null) "" else "  "
          types.forEachIndexed { idx, type ->
            if (idx > 0) {
              printer.println()
            }
            when (type) {
              is TypeValue.ClassTypeValue -> {
                val className = type.className.tokens.last()
                val detail = classTypeDetailsMap.getValue(type.className)
                when (detail.bodyType) {
                  is TypeValue.UnionTypeValue -> {
                    printer.println("sealed class $className")
                  }
                  else -> generateClassType(printer, detail, superclasses[type.className], indent)
                }
              }
              is TypeValue.EnumTypeValue ->
                generateEnumType(printer, type, indent)
              else -> TODO()
            }
          }
          if (outerClassName != null) {
            printer.println("}")
          }
        }
        BuildRuleReturn.value(FileValue(targetFile))
      }
    } else {
      return BuildRuleReturn.value(FileValue(targetFile))
    }
  }
}
