package com.giyeok.bibix.interpreter

import com.giyeok.bibix.interpreter.expr.NameLookupContext
import com.giyeok.bibix.interpreter.expr.VarsContext

class ExprEvalContext(
  val nameLookupContext: NameLookupContext,
  val varsContext: VarsContext,
) {
  val sourceId = nameLookupContext.sourceId
}
