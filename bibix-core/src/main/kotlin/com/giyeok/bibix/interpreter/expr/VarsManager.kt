package com.giyeok.bibix.interpreter.expr

import com.giyeok.bibix.ast.BibixAst
import com.giyeok.bibix.base.CName
import com.giyeok.bibix.base.SourceId
import com.giyeok.bibix.interpreter.BibixInterpreter
import com.giyeok.bibix.interpreter.task.Task
import java.lang.invoke.MethodHandles.Lookup

class VarsManager(private val interpreter: BibixInterpreter) {
  companion object {
    fun fromArgs(args: Map<String, String>): VarsManager {
      TODO()
    }
  }

  private val varDefs = mutableMapOf<CName, VarDef>()
  private val varRedefs = mutableListOf<VarRedef>()

  data class VarDef(val defContext: NameLookupContext, val def: BibixAst.VarDef)
  data class VarRedef(val redefContext: NameLookupContext, val def: BibixAst.VarRedef)

  // TODO mutex 추가

  suspend fun addVarDef(cname: CName, varContext: NameLookupContext, varDef: BibixAst.VarDef) {
    varDefs[cname] = VarDef(varContext, varDef)
  }

  suspend fun addVarRedef(redefContext: NameLookupContext, def: BibixAst.VarRedef) {
    varRedefs.add(VarRedef(redefContext, def))
  }

  suspend fun redefsInSource(sourceId: SourceId) =
    varRedefs.filter { it.redefContext.sourceId == sourceId }

  fun getVarDef(cname: CName): VarDef {
    return varDefs.getValue(cname)
  }
}
