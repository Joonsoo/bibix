package com.giyeok.bibix.plugins.bibix

import com.giyeok.bibix.base.*
import java.io.PrintWriter
import java.nio.file.Path
import kotlin.io.path.bufferedWriter
import kotlin.io.path.createDirectories

class GenTypesKt {
  companion object {
    fun generateDataClassType(
      p: PrintWriter,
      cls: DataClassTypeDetails,
      superClasses: List<String>,
      indent: String,
    ) {
      check(!cls.className.contains('.')) { "Classes to generate must be placed in the root of the script(not under namespace)" }
      val clsName = cls.className
      p.println("${indent}data class $clsName(")
      cls.fields.forEach { field ->
        val optional = if (field.optional) "?" else ""
        p.println("$indent  val ${field.name}: ${bibixTypeToKtType(field.type)}$optional,")
      }
      if (superClasses.isNotEmpty()) {
        check(superClasses.size == 1) {
          "Cannot generate the code for multiple super type: $cls is extending $superClasses"
        }
        p.println("$indent): ${superClasses.first()}() {")
      } else {
        p.println("$indent) {")
      }
      p.println("$indent  companion object {")
      p.println("$indent    fun fromBibix(value: BibixValue): $clsName {")
      p.println("$indent      value as ClassInstanceValue")
      p.println("$indent      check(value.packageName == \"${cls.packageName}\")")
      p.println("$indent      check(value.className == \"$clsName\")")
      p.println("$indent      return $clsName(")
      cls.fields.forEach { field ->
        val fieldExpr = if (!field.optional) {
          bibixValueToKt("value[\"${field.name}\"]!!", field.type)
        } else {
          val bibixToKt = bibixValueToKt("v", field.type)
          "value.getNullableField(\"${field.name}\")?.let { v -> $bibixToKt }"
        }
        p.println("$indent        ${field.name}=$fieldExpr,")
      }
      p.println("$indent      )")
      p.println("$indent    }")
      p.println("$indent  }")
      val override = if (superClasses.isNotEmpty()) "override " else ""
      p.println("$indent  ${override}fun toBibix(): ClassInstanceValue = ClassInstanceValue(")
      p.println("$indent    \"${cls.packageName}\",")
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
      superClasses: List<String>,
      subDataClasses: List<String>,
      indent: String,
    ) {
      val clsName = cls.className // including dots?
      if (superClasses.isNotEmpty()) {
        check(superClasses.size == 1)
        p.println("${indent}sealed class $clsName: ${superClasses.first()}() {")
      } else {
        p.println("${indent}sealed class $clsName {")
      }
      p.println("$indent  companion object {")
      p.println("$indent    fun fromBibix(value: BibixValue): $clsName {")
      p.println("$indent      value as ClassInstanceValue")
      p.println("$indent      check(value.packageName == \"${cls.packageName}\")")
      p.println("$indent      return when (value.className) {")
      subDataClasses.forEach { sub ->
        p.println("$indent        \"$sub\" -> $sub.fromBibix(value)")
      }
      p.println("$indent        else -> throw IllegalStateException(\"Unknown subclass of ${clsName}: \${value.className}\")")
      p.println("$indent      }")
      p.println("$indent    }")
      p.println("$indent  }")
      if (superClasses.isEmpty()) {
        p.println("$indent  abstract fun toBibix(): ClassInstanceValue")
      }
      p.println("$indent}")
    }

    fun findSubDataClassesOf(
      typeName: TypeName,
      classDetails: Map<TypeName, TypeDetails>
    ): List<String> = when (val details = classDetails[typeName]) {
      is SuperClassTypeDetails -> details.subClasses.flatMap {
        findSubDataClassesOf(TypeName(typeName.packageName, it), classDetails)
      }.sorted().distinct()

      is DataClassTypeDetails -> listOf(typeName.typeName)
      else -> listOf()
    }

    fun generateEnumType(p: PrintWriter, detail: EnumTypeDetails, indent: String) {
      p.println("${indent}enum class ${detail.enumName} {")
      detail.values.forEach { enumValue ->
        p.println("$indent  $enumValue,")
      }
      p.println("$indent}")
    }

    fun superClassesMap(typeDetails: Map<TypeName, TypeDetails>): Map<TypeName, List<String>> {
      val map = mutableMapOf<TypeName, MutableList<String>>()
      typeDetails.forEach { (typeName, details) ->
        if (details is SuperClassTypeDetails) {
          details.subClasses.forEach { subClass ->
            map.getOrPut(TypeName(typeName.packageName, subClass)) { mutableListOf() }
              .add(details.className)
          }
        }
      }
      return map
    }
  }

  private val requiredTypeNames = mutableSetOf<TypeName>()
  private val typeDetailsMap = mutableMapOf<TypeName, TypeDetails>()

  fun getTypeDetails(
    generateRelatedTypes: Boolean,
    whenDone: (Map<TypeName, TypeDetails>) -> BuildRuleReturn
  ): BuildRuleReturn =
    BuildRuleReturn.getTypeDetails((requiredTypeNames - typeDetailsMap.keys).toList()) { result ->
      check(result.relativeNamed.isEmpty())
      typeDetailsMap.putAll(result.canonicalNamed)
      if (generateRelatedTypes) {
        result.canonicalNamed.values.forEach { typeDetails ->
          when (typeDetails) {
            is DataClassTypeDetails ->
              typeDetails.fields.forEach { field ->
                val requiredFieldType = when (val fieldType = field.type) {
                  is TypeValue.DataClassTypeValue -> fieldType.typeName
                  is TypeValue.EnumTypeValue -> fieldType.typeName
                  is TypeValue.SuperClassTypeValue -> fieldType.typeName
                  else -> null
                }
                if (requiredFieldType != null) {
                  requiredTypeNames.add(requiredFieldType)
                }
              }

            is SuperClassTypeDetails ->
              typeDetails.subClasses.forEach { subClass ->
                requiredTypeNames.add(TypeName(typeDetails.packageName, subClass))
              }

            is EnumTypeDetails -> {}// Do nothing
          }
        }
      }
      if (generateRelatedTypes && !typeDetailsMap.keys.containsAll(requiredTypeNames)) {
        getTypeDetails(true, whenDone)
      } else {
        whenDone(typeDetailsMap.toMap())
      }
    }

  fun build(context: BuildContext): BuildRuleReturn {
    val types = (context.arguments.getValue("types") as SetValue).values.map {
      it as TypeValue
    }
    val packageName = (context.arguments["packageName"] as? StringValue)?.value
    val generateRelatedTypes = (context.arguments["generateRelatedTypes"] as BooleanValue).value
    val fileName = (context.arguments.getValue("fileName") as StringValue).value
    val outerClassName = (context.arguments["outerClassName"] as? StringValue)?.value

    val targetDir: Path = packageName?.split('.')?.fold(context.destDirectory) { path, name ->
      path.resolve(name)
    } ?: context.destDirectory
    val targetFile = targetDir.resolve(fileName)

    if (context.hashChanged) {
      try {
        targetDir.createDirectories()
      } catch (e: FileAlreadyExistsException) {
        // Ignore
      }

      val classTypes = types.filterIsInstance<TypeValue.PackageNamed>()

      requiredTypeNames.addAll(classTypes.map { it.typeName })
      return getTypeDetails(generateRelatedTypes) { classTypeDetails ->
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
          val superClassesMap = superClassesMap(classTypeDetails)
          val indent = if (outerClassName == null) "" else "  "
          classTypeDetails.values.forEachIndexed { idx, typeDetail ->
            if (idx > 0) {
              printer.println()
            }
            when (typeDetail) {
              is DataClassTypeDetails -> {
                val superClasses = superClassesMap[typeDetail.typeName] ?: listOf()
                generateDataClassType(printer, typeDetail, superClasses, indent)
              }

              is SuperClassTypeDetails -> {
                val superClasses = superClassesMap[typeDetail.typeName] ?: listOf()
                val subClasses = findSubDataClassesOf(typeDetail.typeName, classTypeDetails)
                generateSuperClassType(printer, typeDetail, superClasses, subClasses, indent)
              }

              is EnumTypeDetails -> {
                generateEnumType(printer, typeDetail, indent)
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
