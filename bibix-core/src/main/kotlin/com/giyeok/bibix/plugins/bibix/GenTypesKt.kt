package com.giyeok.bibix.plugins.bibix

import com.giyeok.bibix.base.*
import java.io.PrintWriter
import java.nio.file.Path
import kotlin.io.path.bufferedWriter
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists

class GenTypesKt {
  companion object {
    fun generateDataClassType(
      p: PrintWriter,
      cls: DataClassTypeDetails,
      superClass: CName?,
      indent: String,
    ) {
      val clsName = cls.className.substringAfter('.')
      p.println("${indent}data class $clsName(")
      cls.fields.forEach { field ->
        val optional = if (field.optional) "?" else ""
        p.println("$indent  val ${field.name}: ${bibixTypeToKtType(field.type)}$optional,")
      }
      if (superClass != null) {
        p.println("$indent): ${superClass.tokens.last()}() {")
      } else {
        p.println("$indent) {")
      }
      p.println("$indent  companion object {")
      p.println("$indent    fun fromBibix(value: BibixValue): $clsName {")
      p.println("$indent      value as DataClassInstanceValue")
      p.println("$indent      check(value.className.tokens == TODO())")
      p.println("$indent      return $clsName(")
      cls.fields.forEach { field ->
        val fieldExpr = if (!field.optional) {
          bibixValueToKt("value[\"${field.name}\"]!!", field.type)
        } else {
          "value[\"${field.name}\"]?.let { ${bibixValueToKt("it", field.type)} }"
        }
        p.println("$indent        ${field.name}=$fieldExpr,")
      }
      p.println("$indent      )")
      p.println("$indent    }")
      p.println("$indent  }")
      val override = if (superClass != null) "override " else ""
      p.println("$indent  ${override}fun toBibix(): NDataClassInstanceValue = NDataClassInstanceValue(")
      p.println("$indent    \"${cls.className}\",")
      val hasOptional = cls.fields.any { it.optional }
      if (hasOptional) {
        p.println("$indent    listOfNotNull(")
        cls.fields.forEach { field ->
          if (field.optional) {
            val expr = ktValueToBibix("it", field.type)
            p.println("$indent      this.${field.name}?.let { \"${field.name}\" to $expr },")
          } else {
            val expr = ktValueToBibix("this.${field.name}", field.type)
            p.println("$indent      \"${field.name}\" to $expr,")
          }
        }
        p.println("$indent    ).toMap()")
      } else {
        p.println("$indent    mapOf(")
        cls.fields.forEach { field ->
          val expr = ktValueToBibix("this.${field.name}", field.type)
          p.println("$indent      \"${field.name}\" to $expr,")
        }
        p.println("$indent    )")
      }
      p.println("$indent  )")
      p.println("$indent}")
    }

    fun generateSuperClassType(
      p: PrintWriter,
      cls: SuperClassTypeDetails,
      superClass: CName?,
      indent: String,
    ) {
      val clsName = cls.className // including dots?
      if (superClass != null) {
        p.println("${indent}sealed class $clsName ${superClass.tokens.last()} {")
      } else {
        p.println("${indent}sealed class $clsName {")
      }
      p.println("$indent  companion object {")
      p.println("$indent    fun fromBibix(value: BibixValue): $clsName {")
      p.println("$indent      value as DataClassInstanceValue")
      p.println("$indent      return when (value.className.tokens) {")
      // TODO subClasses 중에서 super class인 것은 다시 풀어서 전부 data class로 바꿔서 처리
      cls.subClasses.forEach { sub ->
        val tokens = "\"$sub\""
        p.println("$indent        listOf($tokens) -> $sub.fromBibix(value)")
      }
      p.println("$indent        else -> throw IllegalStateException(\"Unknown subclass of ${clsName}: \${value.className}\")")
      p.println("$indent      }")
      p.println("$indent    }")
      p.println("$indent  }")
      if (superClass == null) {
        p.println("$indent  abstract fun toBibix(): NDataClassInstanceValue")
      }
      p.println("$indent}")
    }

    fun generateClassType(
      p: PrintWriter,
      cls: ClassTypeDetails,
      superClass: CName?,
      indent: String,
    ) {
      when (cls) {
        is DataClassTypeDetails -> generateDataClassType(p, cls, superClass, indent)
        is SuperClassTypeDetails -> generateSuperClassType(p, cls, superClass, indent)
      }
    }

    fun generateEnumType(p: PrintWriter, detail: EnumTypeDetails, indent: String) {
      p.println("${indent}enum class ${detail.enumName} {")
      detail.values.forEach { enumValue ->
        p.println("$indent  $enumValue,")
      }
      p.println("$indent}")
    }

    fun superClassesMap(
      classTypes: List<TypeValue.DataClassTypeValue>,
      classTypeDetailsMap: Map<CName, ClassTypeDetails>
    ): Map<CName, CName> {
      TODO()
//      val superclasses = mutableMapOf<CName, CName>()
//      classTypes.forEach { clsType ->
//        val detail = classTypeDetailsMap.getValue(clsType.className)
//        if (detail is TypeValue.SuperClassTypeDetail) {
//          detail.subClasses.forEach { subClass ->
//            superclasses[subClass] = clsType.className
//          }
//        }
//      }
//      return superclasses
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

      val classTypes = types.filterIsInstance<TypeValue.PackageNamed>()
      return BuildRuleReturn.getTypeDetails(classTypes.map { it.typeName }) { classTypeDetails ->
        PrintWriter(targetFile.bufferedWriter()).use { printer ->
          if (packageName != null) {
            printer.println("package $packageName")
            printer.println()
          }
          printer.println("import com.giyeok.bibix.base.*")
          printer.println("import java.nio.file.Path")
          printer.println()
          if (outerClassName != null) {
            printer.println("object $outerClassName {")
          }
          val superclasses = mapOf<String, CName>() // superClassesMap(classTypes, classTypeDetails)
          val indent = if (outerClassName == null) "" else "  "
          types.forEachIndexed { idx, type ->
            if (idx > 0) {
              printer.println()
            }
            when (type) {
              is TypeValue.DataClassTypeValue -> {
                val detail =
                  classTypeDetails.canonicalNamed.getValue(type.typeName) as DataClassTypeDetails
                generateDataClassType(printer, detail, superclasses[type.className], indent)
              }

              is TypeValue.SuperClassTypeValue -> {
                val detail =
                  classTypeDetails.canonicalNamed.getValue(type.typeName) as SuperClassTypeDetails
                generateSuperClassType(printer, detail, superclasses[type.className], indent)
              }

              is TypeValue.EnumTypeValue -> {
                val detail =
                  classTypeDetails.canonicalNamed.getValue(type.typeName) as EnumTypeDetails
                generateEnumType(printer, detail, indent)
              }

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
