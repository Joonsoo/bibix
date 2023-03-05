package com.giyeok.bibix.repo

import com.giyeok.bibix.BibixIdProto
import com.giyeok.bibix.base.BibixValue
import com.giyeok.bibix.interpreter.expr.EvaluationResult
import com.giyeok.bibix.objectIdData
import com.giyeok.bibix.utils.toHexString
import com.google.protobuf.ByteString

data class ObjectHash(
  val targetId: TargetId,
  val inputHashes: BibixIdProto.InputHashes,
) {
  val objectId = ObjectId(objectIdData {
    this.targetId = this@ObjectHash.targetId.targetIdBytes
    this.inputHashes = this@ObjectHash.inputHashes
  })
}

data class BibixValueWithObjectHash(val value: BibixValue, val objectHash: ObjectHash?) {
  fun toEvaluationResult(): EvaluationResult = if (objectHash == null) {
    EvaluationResult.Value(value)
  } else {
    EvaluationResult.ValueWithObjectHash(value, objectHash)
  }
}
