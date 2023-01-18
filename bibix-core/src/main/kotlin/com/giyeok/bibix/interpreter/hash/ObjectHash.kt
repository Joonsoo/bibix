package com.giyeok.bibix.interpreter.hash

import com.giyeok.bibix.BibixIdProto
import com.giyeok.bibix.base.BibixValue
import com.giyeok.bibix.interpreter.expr.EvaluationResult
import com.giyeok.bibix.utils.toHexString
import com.google.protobuf.ByteString

data class ObjectHash(val objectId: BibixIdProto.ObjectId, val hashString: ByteString) {
  val hashHexString = hashString.toHexString()
}

data class BibixValueWithObjectHash(val value: BibixValue, val objectHash: ObjectHash?) {
  fun toEvaluationResult(): EvaluationResult = if (objectHash == null) {
    EvaluationResult.Value(value)
  } else {
    EvaluationResult.ValueWithObjectHash(value, objectHash)
  }
}
