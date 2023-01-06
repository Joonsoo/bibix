package com.giyeok.bibix.interpreter

import com.giyeok.bibix.base.CName

// CustomType은 실제로 클래스인지 enum인지 알 수 없는 경우
sealed class BibixType

object AnyType : BibixType()

object BooleanType : BibixType()

object StringType : BibixType()

object PathType : BibixType()

object FileType : BibixType()

object DirectoryType : BibixType()

data class CustomType(val name: CName) : BibixType()

data class ListType(val elemType: BibixType) : BibixType()

data class SetType(val elemType: BibixType) : BibixType()

data class TupleType(val elemTypes: List<BibixType>) : BibixType()

data class NamedTupleType(val pairs: List<Pair<String, BibixType>>) : BibixType() {
  fun names() = pairs.map { it.first }
  fun valueTypes() = pairs.map { it.second }
}

data class UnionType(val types: List<BibixType>) : BibixType()

object NoneType : BibixType()

object BuildRuleDefType : BibixType()

object ActionRuleDefType : BibixType()

object TypeType : BibixType()
