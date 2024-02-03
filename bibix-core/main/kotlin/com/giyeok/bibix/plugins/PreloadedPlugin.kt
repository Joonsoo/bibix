package com.giyeok.bibix.plugins

import com.giyeok.bibix.ast.BibixAst
import com.giyeok.bibix.ast.BibixParser
import com.giyeok.jparser.ktlib.ParsingErrorKt

data class PreloadedPlugin(
  val packageName: String,
  val defs: List<BibixAst.Def>,
  val pluginInstanceProvider: PluginInstanceProvider,
  val script: String,
) {
  companion object {
    fun fromScript(
      packageName: String,
      script: String,
      pluginInstanceProvider: PluginInstanceProvider
    ): PreloadedPlugin {
      val parsed = try {
        BibixParser.parse(script)
      } catch (e: ParsingErrorKt) {
        val lines = script.take(e.location).count { it == '\n' }

        throw IllegalStateException("Failed to parse build script at line $lines: ${e.message} (packageName=$packageName)")
      }
      return PreloadedPlugin(packageName, parsed.defs, pluginInstanceProvider, script)
    }
  }
}
