package com.giyeok.bibix.base

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
data class NamedTupleType(val elemTypes: List<Pair<String, BibixType>>) : BibixType()
data class UnionType(val types: List<BibixType>) : BibixType()
