package com.giyeok.bibix.interpreter.expr

import com.giyeok.bibix.ast.BibixAst
import com.giyeok.bibix.base.CName
import com.giyeok.bibix.base.SourceId
import com.giyeok.bibix.interpreter.BibixInterpreter
import com.giyeok.bibix.interpreter.task.Task
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.lang.invoke.MethodHandles.Lookup

class VarsManager {
  private val mutex = Mutex()

  private val varDefs = mutableMapOf<CName, VarDef>()
  private val varRedefs = mutableListOf<VarRedef>()

  data class VarDef(val defContext: NameLookupContext, val def: BibixAst.VarDef)
  data class VarRedef(val redefContext: NameLookupContext, val def: BibixAst.VarRedef)

  suspend fun addVarDef(cname: CName, varContext: NameLookupContext, varDef: BibixAst.VarDef) =
    mutex.withLock {
      varDefs[cname] = VarDef(varContext, varDef)
    }

  suspend fun addVarRedef(redefContext: NameLookupContext, def: BibixAst.VarRedef) =
    mutex.withLock {
      varRedefs.add(VarRedef(redefContext, def))
    }

  suspend fun redefsInSource(sourceId: SourceId) = mutex.withLock {
    varRedefs.filter { it.redefContext.sourceId == sourceId }
  }

  suspend fun getVarDef(cname: CName): VarDef = mutex.withLock {
    varDefs.getValue(cname)
  }
}
