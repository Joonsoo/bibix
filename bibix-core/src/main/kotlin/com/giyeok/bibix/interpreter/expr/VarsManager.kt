package com.giyeok.bibix.interpreter.expr

import com.giyeok.bibix.ast.BibixAst
import com.giyeok.bibix.base.CName
import com.giyeok.bibix.interpreter.BibixInterpreter
import com.giyeok.bibix.interpreter.task.Task
import com.giyeok.bibix.utils.toKtList

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

  fun addVarDef(cname: CName, varContext: NameLookupContext, varDef: BibixAst.VarDef) {
    varDefs[cname] = VarDef(varContext, varDef)
  }

  fun addVarRedef(redefContext: NameLookupContext, def: BibixAst.VarRedef) {
    varRedefs.add(VarRedef(redefContext, def))
  }

  suspend fun redefines(task: Task, cname: CName): List<VarRedef> {
    // TODO 모든 redef를 뒤지지 않는 방법이 없을까?
    return varRedefs.filter { redef ->
      val lookup =
        interpreter.lookupName(task, redef.redefContext, redef.def.nameTokens().toKtList())
      lookup.cname == cname
    }
  }

  fun getVarDef(cname: CName): VarDef {
    return varDefs.getValue(cname)
  }
}
