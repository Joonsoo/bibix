package com.giyeok.bibix.repo

import com.giyeok.bibix.BibixIdProto.ObjectIdData
import com.giyeok.bibix.BibixIdProto.TargetIdData
import com.giyeok.bibix.utils.toHexString
import com.google.protobuf.ByteString

data class TargetId(
  val targetIdData: TargetIdData
) {
  val targetIdBytes: ByteString by lazy {
    targetIdData.hashString()
  }

  val targetIdHex: String by lazy {
    targetIdBytes.toHexString()
  }
}

data class ObjectId(
  val objectIdData: ObjectIdData
) {
  val objectIdBytes: ByteString by lazy {
    objectIdData.hashString()
  }

  val objectIdHex: String by lazy {
    objectIdBytes.toHexString()
  }
}
