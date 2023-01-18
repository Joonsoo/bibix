package com.giyeok.bibix.interpreter

import com.giyeok.bibix.base.ClassInstanceValue
import com.giyeok.bibix.base.DirectoryValue
import com.giyeok.bibix.base.PathValue
import com.giyeok.bibix.base.StringValue
import java.nio.file.Path

data class BibixProject(val projectRoot: Path, val scriptName: String?) {
  companion object {
    val packageName = "com.giyeok.bibix.prelude"
    val className = "BibixProject"

    fun fromBibixValue(classInstanceValue: ClassInstanceValue): BibixProject? {
      if (classInstanceValue.packageName == packageName && classInstanceValue.className == className) {
        return BibixProject(
          projectRoot = (classInstanceValue.fieldValues.getValue("projectRoot") as DirectoryValue).directory,
          scriptName = (classInstanceValue.fieldValues["scriptName"] as? StringValue)?.value
        )
      }
      return null
    }
  }

  fun toBibixValue() = ClassInstanceValue(packageName, className,
    listOfNotNull(
      "projectRoot" to DirectoryValue(projectRoot),
      scriptName?.let { "scriptName" to StringValue(it) }
    ).toMap()
  )
}
