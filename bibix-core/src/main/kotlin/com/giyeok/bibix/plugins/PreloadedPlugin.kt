package com.giyeok.bibix.plugins

import com.giyeok.bibix.ast.BibixAst
import com.giyeok.bibix.utils.toKtList
import com.giyeok.jparser.ParsingErrors

data class PreloadedPlugin(
  val packageName: String,
  val defs: List<BibixAst.Def>,
  val pluginInstanceProvider: PluginInstanceProvider,
) {
  companion object {
    fun fromScript(packageName: String, script: String, pluginInstanceProvider: PluginInstanceProvider): PreloadedPlugin {
      val parsed = BibixAst.parseAst(script)

      if (parsed.isRight) {
        val exception = parsed.right().get()
        println(exception)
        val location = when (exception) {
          is ParsingErrors.UnexpectedInput -> exception.location()
          is ParsingErrors.UnexpectedInputByTermGroups -> exception.location()
          is ParsingErrors.UnexpectedEOF -> exception.location()
          is ParsingErrors.UnexpectedEOFByTermGroups -> exception.location()
          else -> null
        }
        val lines = location?.let {
          script.take(location).count { it == '\n' }
        }
        throw IllegalStateException("Failed to parse build script at line $lines: ${exception.msg()} (packageName=$packageName)")
      }
      val ast = parsed.left().get()
      return PreloadedPlugin(packageName, ast.defs().toKtList(), pluginInstanceProvider)
    }
  }
}
