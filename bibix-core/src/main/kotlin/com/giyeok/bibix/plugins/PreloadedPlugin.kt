package com.giyeok.bibix.plugins

import com.giyeok.bibix.ast.BibixAst
import com.giyeok.jparser.ParsingErrors

data class PreloadedPlugin(
  val packageName: String,
  val defs: List<BibixAst.Def>,
  val pluginInstanceProvider: PluginInstanceProvider,
) {
  companion object {
    fun fromScript(
      packageName: String,
      script: String,
      pluginInstanceProvider: PluginInstanceProvider
    ): PreloadedPlugin {
      val parsed = try {
        BibixAst.parse(script)
      } catch (e: ParsingErrors.ParsingError) {
        val location = when (e) {
          is ParsingErrors.UnexpectedInput -> e.location()
          is ParsingErrors.UnexpectedInputByTermGroups -> e.location()
          is ParsingErrors.UnexpectedEOF -> e.location()
          is ParsingErrors.UnexpectedEOFByTermGroups -> e.location()
          else -> null
        }

        val lines = location?.let {
          script.take(location).count { it == '\n' }
        }

        throw IllegalStateException("Failed to parse build script at line $lines: ${e.message} (packageName=$packageName)")
      }
      return PreloadedPlugin(packageName, parsed.defs, pluginInstanceProvider)
    }
  }
}
