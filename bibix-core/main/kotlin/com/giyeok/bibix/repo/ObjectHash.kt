package com.giyeok.bibix.repo

import com.giyeok.bibix.BibixIdProto
import com.giyeok.bibix.base.BibixValue
import com.giyeok.bibix.interpreter.expr.EvaluationResult
import com.giyeok.bibix.objectIdData
import com.google.protobuf.ByteString

data class ObjectHash(
  val targetId: TargetId,
  val inputHashes: BibixIdProto.InputHashes,
) {
  val inputsHash: ByteString by lazy {
    inputHashes.hashString()
  }

  val objectId = ObjectId(objectIdData {
    this.targetId = this@ObjectHash.targetId.targetIdBytes
    this.inputsHash = this@ObjectHash.inputsHash
  })
}

data class BibixValueWithObjectHash(val value: BibixValue, val objectHash: ObjectHash?) {
  fun toEvaluationResult(): EvaluationResult = if (objectHash == null) {
    EvaluationResult.Value(value)
  } else {
    EvaluationResult.ValueWithTargetId(value, objectHash)
  }
}
