package com.giyeok.bibix.interpreter.hash

import com.giyeok.bibix.BibixIdProto
import com.giyeok.bibix.utils.toHexString
import com.google.protobuf.ByteString

data class ObjectHash(val objectId: BibixIdProto.ObjectId, val hashString: ByteString) {
  val hashHexString = hashString.toHexString()
}
