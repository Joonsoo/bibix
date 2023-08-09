package com.giyeok.bibix.ast

import com.giyeok.jparser.ktlib.*

class BibixAst(
  val source: String,
  val history: List<KernelSet>,
  val idIssuer: IdIssuer = IdIssuerImpl(0)
) {
  private fun nextId(): Int = idIssuer.nextId()

  sealed interface AstNode {
    val nodeId: Int
    val start: Int
    val end: Int
  }

data class VarRedef(
  val nameTokens: List<String>,
  val redefValue: Expr,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): AstNode

data class This(

  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): Primary, AstNode

sealed interface ClassBodyElem: AstNode

data class ImportFrom(
  val source: Expr,
  val importing: Name,
  val rename: String?,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): ImportDef, AstNode

data class ClassCastDef(
  val castTo: TypeExpr,
  val expr: Expr,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): ClassBodyElem, AstNode

data class TupleType(
  val elems: List<TypeExpr>,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): NoUnionType, AstNode

data class NoneType(

  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): NoUnionType, AstNode

data class NamedTupleExpr(
  val elems: List<NamedExpr>,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): Primary, AstNode

data class MemberAccess(
  val target: Primary,
  val name: String,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): Primary, AstNode

data class EscapeChar(
  val code: Char,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): StringElem, AstNode

sealed interface NoUnionType: TypeExpr, AstNode

data class TupleExpr(
  val elems: List<Expr>,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): Primary, AstNode

sealed interface MergeOpOrPrimary: Expr, AstNode

data class ActionRuleDef(
  val name: String,
  val params: List<ParamDef>,
  val impl: MethodRef,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): ClassBodyElem, Def, AstNode

data class ComplexExpr(
  val expr: Expr,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): StringExpr, AstNode

data class Paren(
  val expr: Expr,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): Primary, AstNode

data class BuildRuleDef(
  val name: String,
  val params: List<ParamDef>,
  val returnType: TypeExpr,
  val impl: MethodRef,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): Def, AstNode

data class Name(
  val tokens: List<String>,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): NoUnionType, AstNode

data class DataClassDef(
  val name: String,
  val fields: List<ParamDef>,
  val body: List<ClassBodyElem>,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): ClassDef, AstNode

data class CollectionType(
  val name: String,
  val typeParams: TypeParams,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): NoUnionType, AstNode

sealed interface ClassDef: Def, AstNode

data class MethodRef(
  val targetName: Name,
  val className: Name,
  val methodName: String?,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): AstNode

data class SingleCallAction(
  val expr: CallExpr,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): ActionBody, AstNode

sealed interface Expr: ListElem, AstNode

data class CastExpr(
  val expr: Expr,
  val castTo: NoUnionType,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): Expr, AstNode

sealed interface ImportDef: Def, AstNode

data class JustChar(
  val chr: Char,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): StringElem, AstNode

sealed interface Literal: Primary, AstNode

data class NameRef(
  val name: String,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): Primary, AstNode

data class StringLiteral(
  val elems: List<StringElem>,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): Literal, AstNode

sealed interface StringExpr: StringElem, AstNode

data class CallExpr(
  val name: Name,
  val params: CallParams,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): Primary, AstNode

data class NamedType(
  val name: String,
  val typ: TypeExpr,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): AstNode

sealed interface StringElem: AstNode

data class NamedExpr(
  val name: String,
  val expr: Expr,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): AstNode

data class MergeOp(
  val lhs: Expr,
  val rhs: Primary,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): MergeOpOrPrimary, AstNode

data class VarDef(
  val name: String,
  val typ: TypeExpr?,
  val defaultValue: Expr?,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): Def, AstNode

data class NamespaceDef(
  val name: String,
  val body: BuildScript,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): Def, AstNode

data class ListExpr(
  val elems: List<ListElem>,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): Primary, AstNode

data class TypeParams(
  val params: List<TypeExpr>,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): AstNode

data class EnumDef(
  val name: String,
  val values: List<String>,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): Def, AstNode

sealed interface Primary: MergeOpOrPrimary, AstNode

data class VarRedefs(
  val redefs: List<VarRedef>,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): Def, AstNode

sealed interface TypeExpr: AstNode

sealed interface ListElem: AstNode

data class ParamDef(
  val name: String,
  val optional: Boolean,
  val typ: TypeExpr?,
  val defaultValue: Expr?,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): AstNode

data class UnionType(
  val elems: List<NoUnionType>,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): TypeExpr, AstNode

data class TargetDef(
  val name: String,
  val value: Expr,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): Def, AstNode

data class SuperClassDef(
  val name: String,
  val subs: List<String>,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): ClassDef, AstNode

data class ActionDef(
  val name: String,
  val argsName: String?,
  val body: ActionBody,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): Def, AstNode

data class MultiCallActions(
  val exprs: List<CallExpr>,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): ActionBody, AstNode

sealed interface ActionBody: AstNode

sealed interface Def: AstNode

data class NamedParam(
  val name: String,
  val value: Expr,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): AstNode

data class ImportAll(
  val source: Primary,
  val rename: String?,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): ImportDef, AstNode

data class BuildScript(
  val packageName: Name?,
  val defs: List<Def>,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): AstNode

data class EllipsisElem(
  val value: Expr,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): ListElem, AstNode

data class SimpleExpr(
  val name: String,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): StringExpr, AstNode

data class NoneLiteral(

  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): Literal, AstNode

data class CallParams(
  val posParams: List<Expr>,
  val namedParams: List<NamedParam>,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): AstNode

data class NamedTupleType(
  val elems: List<NamedType>,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): NoUnionType, AstNode

data class BooleanLiteral(
  val value: Boolean,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): Literal, AstNode


fun matchStart(): BuildScript {
  val lastGen = source.length
  val kernel = history[lastGen].getSingle(2, 1, 0, lastGen)
  return matchBuildScript(kernel.beginGen, kernel.endGen)
}

fun matchBuildScript(beginGen: Int, endGen: Int): BuildScript {
val var1 = getSequenceElems(history, 3, listOf(4,7,101,7), beginGen, endGen)
val var2 = history[var1[0].second].findByBeginGenOpt(5, 1, var1[0].first)
val var3 = history[var1[0].second].findByBeginGenOpt(100, 1, var1[0].first)
check(hasSingleTrue(var2 != null, var3 != null))
val var4 = when {
var2 != null -> {
val var5 = getSequenceElems(history, 6, listOf(7,42), var1[0].first, var1[0].second)
val var6 = matchPackageName(var5[1].first, var5[1].second)
var6
}
else -> null
}
val var7 = matchDefs(var1[2].first, var1[2].second)
val var8 = BuildScript(var4, var7, nextId(), beginGen, endGen)
return var8
}

fun matchPackageName(beginGen: Int, endGen: Int): Name {
val var9 = getSequenceElems(history, 43, listOf(44,7,63), beginGen, endGen)
val var10 = matchName(var9[2].first, var9[2].second)
return var10
}

fun matchDefs(beginGen: Int, endGen: Int): List<Def> {
val var11 = getSequenceElems(history, 102, listOf(103,361), beginGen, endGen)
val var12 = matchDef(var11[0].first, var11[0].second)
val var13 = unrollRepeat0(history, 361, 363, 9, 362, var11[1].first, var11[1].second).map { k ->
val var14 = getSequenceElems(history, 364, listOf(7,103), k.first, k.second)
val var15 = matchDef(var14[1].first, var14[1].second)
var15
}
return listOf(var12) + var13
}

fun matchName(beginGen: Int, endGen: Int): Name {
val var16 = getSequenceElems(history, 64, listOf(65,95), beginGen, endGen)
val var17 = matchSimpleName(var16[0].first, var16[0].second)
val var18 = unrollRepeat0(history, 95, 97, 9, 96, var16[1].first, var16[1].second).map { k ->
val var19 = getSequenceElems(history, 98, listOf(7,99,7,65), k.first, k.second)
val var20 = matchSimpleName(var19[3].first, var19[3].second)
var20
}
val var21 = Name(listOf(var17) + var18, nextId(), beginGen, endGen)
return var21
}

fun matchSimpleName(beginGen: Int, endGen: Int): String {
val var22 = getSequenceElems(history, 72, listOf(73,74), beginGen, endGen)
val var23 = unrollRepeat0(history, 74, 57, 9, 75, var22[1].first, var22[1].second).map { k ->
source[k.first]
}
return source[var22[0].first].toString() + var23.joinToString("") { it.toString() }
}

fun matchDef(beginGen: Int, endGen: Int): Def {
val var24 = history[endGen].findByBeginGenOpt(104, 1, beginGen)
val var25 = history[endGen].findByBeginGenOpt(252, 1, beginGen)
val var26 = history[endGen].findByBeginGenOpt(254, 1, beginGen)
val var27 = history[endGen].findByBeginGenOpt(255, 1, beginGen)
val var28 = history[endGen].findByBeginGenOpt(274, 1, beginGen)
val var29 = history[endGen].findByBeginGenOpt(309, 1, beginGen)
val var30 = history[endGen].findByBeginGenOpt(333, 1, beginGen)
val var31 = history[endGen].findByBeginGenOpt(339, 1, beginGen)
val var32 = history[endGen].findByBeginGenOpt(349, 1, beginGen)
val var33 = history[endGen].findByBeginGenOpt(359, 1, beginGen)
check(hasSingleTrue(var24 != null, var25 != null, var26 != null, var27 != null, var28 != null, var29 != null, var30 != null, var31 != null, var32 != null, var33 != null))
val var34 = when {
var24 != null -> {
val var35 = matchImportDef(beginGen, endGen)
var35
}
var25 != null -> {
val var36 = matchNamespaceDef(beginGen, endGen)
var36
}
var26 != null -> {
val var37 = matchTargetDef(beginGen, endGen)
var37
}
var27 != null -> {
val var38 = matchActionDef(beginGen, endGen)
var38
}
var28 != null -> {
val var39 = matchClassDef(beginGen, endGen)
var39
}
var29 != null -> {
val var40 = matchActionRuleDef(beginGen, endGen)
var40
}
var30 != null -> {
val var41 = matchEnumDef(beginGen, endGen)
var41
}
var31 != null -> {
val var42 = matchVarDef(beginGen, endGen)
var42
}
var32 != null -> {
val var43 = matchVarRedefs(beginGen, endGen)
var43
}
else -> {
val var44 = matchBuildRuleDef(beginGen, endGen)
var44
}
}
return var34
}

fun matchActionRuleDef(beginGen: Int, endGen: Int): ActionRuleDef {
val var45 = getSequenceElems(history, 310, listOf(257,7,311,7,65,7,281,7,182,7,316), beginGen, endGen)
val var46 = matchSimpleName(var45[4].first, var45[4].second)
val var47 = matchParamsDef(var45[6].first, var45[6].second)
val var48 = matchMethodRef(var45[10].first, var45[10].second)
val var49 = ActionRuleDef(var46, var47, var48, nextId(), beginGen, endGen)
return var49
}

fun matchBuildRuleDef(beginGen: Int, endGen: Int): BuildRuleDef {
val var50 = getSequenceElems(history, 360, listOf(311,7,65,7,281,7,162,7,141,7,182,7,316), beginGen, endGen)
val var51 = matchSimpleName(var50[2].first, var50[2].second)
val var52 = matchParamsDef(var50[4].first, var50[4].second)
val var53 = matchTypeExpr(var50[8].first, var50[8].second)
val var54 = matchMethodRef(var50[12].first, var50[12].second)
val var55 = BuildRuleDef(var51, var52, var53, var54, nextId(), beginGen, endGen)
return var55
}

fun matchImportDef(beginGen: Int, endGen: Int): ImportDef {
val var56 = history[endGen].findByBeginGenOpt(105, 4, beginGen)
val var57 = history[endGen].findByBeginGenOpt(247, 8, beginGen)
check(hasSingleTrue(var56 != null, var57 != null))
val var58 = when {
var56 != null -> {
val var59 = getSequenceElems(history, 105, listOf(106,7,111,244), beginGen, endGen)
val var60 = matchPrimary(var59[2].first, var59[2].second)
val var61 = history[var59[3].second].findByBeginGenOpt(100, 1, var59[3].first)
val var62 = history[var59[3].second].findByBeginGenOpt(245, 1, var59[3].first)
check(hasSingleTrue(var61 != null, var62 != null))
val var63 = when {
var61 != null -> null
else -> {
val var64 = getSequenceElems(history, 246, listOf(7,123,7,65), var59[3].first, var59[3].second)
val var65 = matchSimpleName(var64[3].first, var64[3].second)
var65
}
}
val var66 = ImportAll(var60, var63, nextId(), beginGen, endGen)
var66
}
else -> {
val var67 = getSequenceElems(history, 247, listOf(248,7,121,7,106,7,63,244), beginGen, endGen)
val var68 = matchExpr(var67[2].first, var67[2].second)
val var69 = matchName(var67[6].first, var67[6].second)
val var70 = history[var67[7].second].findByBeginGenOpt(100, 1, var67[7].first)
val var71 = history[var67[7].second].findByBeginGenOpt(245, 1, var67[7].first)
check(hasSingleTrue(var70 != null, var71 != null))
val var72 = when {
var70 != null -> null
else -> {
val var73 = getSequenceElems(history, 246, listOf(7,123,7,65), var67[7].first, var67[7].second)
val var74 = matchSimpleName(var73[3].first, var73[3].second)
var74
}
}
val var75 = ImportFrom(var68, var69, var72, nextId(), beginGen, endGen)
var75
}
}
return var58
}

fun matchExpr(beginGen: Int, endGen: Int): Expr {
val var76 = history[endGen].findByBeginGenOpt(122, 5, beginGen)
val var77 = history[endGen].findByBeginGenOpt(167, 1, beginGen)
check(hasSingleTrue(var76 != null, var77 != null))
val var78 = when {
var76 != null -> {
val var79 = getSequenceElems(history, 122, listOf(121,7,123,7,127), beginGen, endGen)
val var80 = matchExpr(var79[0].first, var79[0].second)
val var81 = matchNoUnionType(var79[4].first, var79[4].second)
val var82 = CastExpr(var80, var81, nextId(), beginGen, endGen)
var82
}
else -> {
val var83 = matchMergeOpOrPrimary(beginGen, endGen)
var83
}
}
return var78
}

fun matchNoUnionType(beginGen: Int, endGen: Int): NoUnionType {
val var84 = history[endGen].findByBeginGenOpt(63, 1, beginGen)
val var85 = history[endGen].findByBeginGenOpt(128, 1, beginGen)
val var86 = history[endGen].findByBeginGenOpt(129, 1, beginGen)
val var87 = history[endGen].findByBeginGenOpt(156, 1, beginGen)
val var88 = history[endGen].findByBeginGenOpt(158, 1, beginGen)
check(hasSingleTrue(var84 != null, var85 != null, var86 != null, var87 != null, var88 != null))
val var89 = when {
var84 != null -> {
val var90 = matchName(beginGen, endGen)
var90
}
var85 != null -> {
val var91 = NoneType(nextId(), beginGen, endGen)
var91
}
var86 != null -> {
val var92 = matchCollectionType(beginGen, endGen)
var92
}
var87 != null -> {
val var93 = matchTupleType(beginGen, endGen)
var93
}
else -> {
val var94 = matchNamedTupleType(beginGen, endGen)
var94
}
}
return var89
}

fun matchMergeOpOrPrimary(beginGen: Int, endGen: Int): MergeOpOrPrimary {
val var95 = history[endGen].findByBeginGenOpt(111, 1, beginGen)
val var96 = history[endGen].findByBeginGenOpt(168, 5, beginGen)
check(hasSingleTrue(var95 != null, var96 != null))
val var97 = when {
var95 != null -> {
val var98 = matchPrimary(beginGen, endGen)
var98
}
else -> {
val var99 = getSequenceElems(history, 168, listOf(121,7,169,7,111), beginGen, endGen)
val var100 = matchExpr(var99[0].first, var99[0].second)
val var101 = matchPrimary(var99[4].first, var99[4].second)
val var102 = MergeOp(var100, var101, nextId(), beginGen, endGen)
var102
}
}
return var97
}

fun matchCollectionType(beginGen: Int, endGen: Int): CollectionType {
val var103 = getSequenceElems(history, 130, listOf(131,7,138), beginGen, endGen)
val var104 = history[var103[0].second].findByBeginGenOpt(134, 1, var103[0].first)
val var105 = history[var103[0].second].findByBeginGenOpt(136, 1, var103[0].first)
check(hasSingleTrue(var104 != null, var105 != null))
val var106 = when {
var104 != null -> "set"
else -> "list"
}
val var107 = matchTypeParams(var103[2].first, var103[2].second)
val var108 = CollectionType(var106, var107, nextId(), beginGen, endGen)
return var108
}

fun matchParamsDef(beginGen: Int, endGen: Int): List<ParamDef> {
val var110 = getSequenceElems(history, 282, listOf(116,283,174,7,117), beginGen, endGen)
val var111 = history[var110[1].second].findByBeginGenOpt(100, 1, var110[1].first)
val var112 = history[var110[1].second].findByBeginGenOpt(284, 1, var110[1].first)
check(hasSingleTrue(var111 != null, var112 != null))
val var113 = when {
var111 != null -> null
else -> {
val var114 = getSequenceElems(history, 285, listOf(7,286,295), var110[1].first, var110[1].second)
val var115 = matchParamDef(var114[1].first, var114[1].second)
val var116 = unrollRepeat0(history, 295, 297, 9, 296, var114[2].first, var114[2].second).map { k ->
val var117 = getSequenceElems(history, 298, listOf(7,149,7,286), k.first, k.second)
val var118 = matchParamDef(var117[3].first, var117[3].second)
var118
}
listOf(var115) + var116
}
}
val var109 = var113
return (var109 ?: listOf())
}

fun matchTypeParams(beginGen: Int, endGen: Int): TypeParams {
val var119 = getSequenceElems(history, 139, listOf(140,7,141,151,7,155), beginGen, endGen)
val var120 = matchTypeExpr(var119[2].first, var119[2].second)
val var121 = unrollRepeat0(history, 151, 153, 9, 152, var119[3].first, var119[3].second).map { k ->
val var122 = getSequenceElems(history, 154, listOf(7,149,7,141), k.first, k.second)
val var123 = matchTypeExpr(var122[3].first, var122[3].second)
var123
}
val var124 = TypeParams(listOf(var120) + var121, nextId(), beginGen, endGen)
return var124
}

fun matchActionDef(beginGen: Int, endGen: Int): ActionDef {
val var125 = getSequenceElems(history, 256, listOf(257,7,65,261,7,266), beginGen, endGen)
val var126 = matchSimpleName(var125[2].first, var125[2].second)
val var127 = history[var125[3].second].findByBeginGenOpt(100, 1, var125[3].first)
val var128 = history[var125[3].second].findByBeginGenOpt(262, 1, var125[3].first)
check(hasSingleTrue(var127 != null, var128 != null))
val var129 = when {
var127 != null -> null
else -> {
val var130 = getSequenceElems(history, 263, listOf(7,264), var125[3].first, var125[3].second)
val var131 = matchActionParams(var130[1].first, var130[1].second)
var131
}
}
val var132 = matchActionBody(var125[5].first, var125[5].second)
val var133 = ActionDef(var126, var129, var132, nextId(), beginGen, endGen)
return var133
}

fun matchActionParams(beginGen: Int, endGen: Int): String {
val var134 = getSequenceElems(history, 265, listOf(116,7,65,7,117), beginGen, endGen)
val var135 = matchSimpleName(var134[2].first, var134[2].second)
return var135
}

fun matchActionBody(beginGen: Int, endGen: Int): ActionBody {
val var136 = history[endGen].findByBeginGenOpt(267, 3, beginGen)
val var137 = history[endGen].findByBeginGenOpt(269, 4, beginGen)
check(hasSingleTrue(var136 != null, var137 != null))
val var138 = when {
var136 != null -> {
val var139 = getSequenceElems(history, 267, listOf(182,7,268), beginGen, endGen)
val var140 = matchActionExpr(var139[2].first, var139[2].second)
val var141 = SingleCallAction(var140, nextId(), beginGen, endGen)
var141
}
else -> {
val var142 = getSequenceElems(history, 269, listOf(144,270,7,150), beginGen, endGen)
val var143 = unrollRepeat1(history, 270, 271, 271, 273, var142[1].first, var142[1].second).map { k ->
val var144 = getSequenceElems(history, 272, listOf(7,268), k.first, k.second)
val var145 = matchActionExpr(var144[1].first, var144[1].second)
var145
}
val var146 = MultiCallActions(var143, nextId(), beginGen, endGen)
var146
}
}
return var138
}

fun matchActionExpr(beginGen: Int, endGen: Int): CallExpr {
val var147 = matchCallExpr(beginGen, endGen)
return var147
}

fun matchCallExpr(beginGen: Int, endGen: Int): CallExpr {
val var148 = getSequenceElems(history, 113, listOf(63,7,114), beginGen, endGen)
val var149 = matchName(var148[0].first, var148[0].second)
val var150 = matchCallParams(var148[2].first, var148[2].second)
val var151 = CallExpr(var149, var150, nextId(), beginGen, endGen)
return var151
}

fun matchTupleType(beginGen: Int, endGen: Int): TupleType {
val var152 = getSequenceElems(history, 157, listOf(116,7,141,151,7,117), beginGen, endGen)
val var153 = matchTypeExpr(var152[2].first, var152[2].second)
val var154 = unrollRepeat0(history, 151, 153, 9, 152, var152[3].first, var152[3].second).map { k ->
val var155 = getSequenceElems(history, 154, listOf(7,149,7,141), k.first, k.second)
val var156 = matchTypeExpr(var155[3].first, var155[3].second)
var156
}
val var157 = TupleType(listOf(var153) + var154, nextId(), beginGen, endGen)
return var157
}

fun matchClassDef(beginGen: Int, endGen: Int): ClassDef {
val var158 = history[endGen].findByBeginGenOpt(275, 1, beginGen)
val var159 = history[endGen].findByBeginGenOpt(323, 1, beginGen)
check(hasSingleTrue(var158 != null, var159 != null))
val var160 = when {
var158 != null -> {
val var161 = matchDataClassDef(beginGen, endGen)
var161
}
else -> {
val var162 = matchSuperClassDef(beginGen, endGen)
var162
}
}
return var160
}

fun matchDataClassDef(beginGen: Int, endGen: Int): DataClassDef {
val var163 = getSequenceElems(history, 276, listOf(277,7,65,7,281,299), beginGen, endGen)
val var164 = matchSimpleName(var163[2].first, var163[2].second)
val var165 = matchParamsDef(var163[4].first, var163[4].second)
val var167 = history[var163[5].second].findByBeginGenOpt(100, 1, var163[5].first)
val var168 = history[var163[5].second].findByBeginGenOpt(300, 1, var163[5].first)
check(hasSingleTrue(var167 != null, var168 != null))
val var169 = when {
var167 != null -> null
else -> {
val var170 = getSequenceElems(history, 301, listOf(7,302), var163[5].first, var163[5].second)
val var171 = matchClassBody(var170[1].first, var170[1].second)
var171
}
}
val var166 = var169
val var172 = DataClassDef(var164, var165, (var166 ?: listOf()), nextId(), beginGen, endGen)
return var172
}

fun matchMethodRef(beginGen: Int, endGen: Int): MethodRef {
val var173 = getSequenceElems(history, 317, listOf(63,7,162,7,63,318), beginGen, endGen)
val var174 = matchName(var173[0].first, var173[0].second)
val var175 = matchName(var173[4].first, var173[4].second)
val var176 = history[var173[5].second].findByBeginGenOpt(100, 1, var173[5].first)
val var177 = history[var173[5].second].findByBeginGenOpt(319, 1, var173[5].first)
check(hasSingleTrue(var176 != null, var177 != null))
val var178 = when {
var176 != null -> null
else -> {
val var179 = getSequenceElems(history, 320, listOf(7,162,7,65), var173[5].first, var173[5].second)
val var180 = matchSimpleName(var179[3].first, var179[3].second)
var180
}
}
val var181 = MethodRef(var174, var175, var178, nextId(), beginGen, endGen)
return var181
}

fun matchClassBody(beginGen: Int, endGen: Int): List<ClassBodyElem> {
val var182 = getSequenceElems(history, 303, listOf(144,304,7,150), beginGen, endGen)
val var183 = unrollRepeat0(history, 304, 306, 9, 305, var182[1].first, var182[1].second).map { k ->
val var184 = getSequenceElems(history, 307, listOf(7,308), k.first, k.second)
val var185 = matchClassBodyElem(var184[1].first, var184[1].second)
var185
}
return var183
}

fun matchClassBodyElem(beginGen: Int, endGen: Int): ClassBodyElem {
val var186 = history[endGen].findByBeginGenOpt(309, 1, beginGen)
val var187 = history[endGen].findByBeginGenOpt(321, 1, beginGen)
check(hasSingleTrue(var186 != null, var187 != null))
val var188 = when {
var186 != null -> {
val var189 = matchActionRuleDef(beginGen, endGen)
var189
}
else -> {
val var190 = matchClassCastDef(beginGen, endGen)
var190
}
}
return var188
}

fun matchClassCastDef(beginGen: Int, endGen: Int): ClassCastDef {
val var191 = getSequenceElems(history, 322, listOf(123,7,141,7,182,7,121), beginGen, endGen)
val var192 = matchTypeExpr(var191[2].first, var191[2].second)
val var193 = matchExpr(var191[6].first, var191[6].second)
val var194 = ClassCastDef(var192, var193, nextId(), beginGen, endGen)
return var194
}

fun matchVarDef(beginGen: Int, endGen: Int): VarDef {
val var195 = getSequenceElems(history, 340, listOf(341,7,65,346,292), beginGen, endGen)
val var196 = matchSimpleName(var195[2].first, var195[2].second)
val var197 = history[var195[3].second].findByBeginGenOpt(100, 1, var195[3].first)
val var198 = history[var195[3].second].findByBeginGenOpt(347, 1, var195[3].first)
check(hasSingleTrue(var197 != null, var198 != null))
val var199 = when {
var197 != null -> null
else -> {
val var200 = getSequenceElems(history, 348, listOf(7,162,7,141), var195[3].first, var195[3].second)
val var201 = matchTypeExpr(var200[3].first, var200[3].second)
var201
}
}
val var202 = history[var195[4].second].findByBeginGenOpt(100, 1, var195[4].first)
val var203 = history[var195[4].second].findByBeginGenOpt(293, 1, var195[4].first)
check(hasSingleTrue(var202 != null, var203 != null))
val var204 = when {
var202 != null -> null
else -> {
val var205 = getSequenceElems(history, 294, listOf(7,182,7,121), var195[4].first, var195[4].second)
val var206 = matchExpr(var205[3].first, var205[3].second)
var206
}
}
val var207 = VarDef(var196, var199, var204, nextId(), beginGen, endGen)
return var207
}

fun matchNamespaceDef(beginGen: Int, endGen: Int): NamespaceDef {
val var208 = getSequenceElems(history, 253, listOf(65,7,144,2,150), beginGen, endGen)
val var209 = matchSimpleName(var208[0].first, var208[0].second)
val var210 = matchBuildScript(var208[3].first, var208[3].second)
val var211 = NamespaceDef(var209, var210, nextId(), beginGen, endGen)
return var211
}

fun matchEnumDef(beginGen: Int, endGen: Int): EnumDef {
val var212 = getSequenceElems(history, 334, listOf(335,7,65,7,144,7,65,329,174,7,150), beginGen, endGen)
val var213 = matchSimpleName(var212[2].first, var212[2].second)
val var214 = matchSimpleName(var212[6].first, var212[6].second)
val var215 = unrollRepeat0(history, 329, 331, 9, 330, var212[7].first, var212[7].second).map { k ->
val var216 = getSequenceElems(history, 332, listOf(7,149,7,65), k.first, k.second)
val var217 = matchSimpleName(var216[3].first, var216[3].second)
var217
}
val var218 = EnumDef(var213, listOf(var214) + var215, nextId(), beginGen, endGen)
return var218
}

fun matchPrimary(beginGen: Int, endGen: Int): Primary {
val var219 = history[endGen].findByBeginGenOpt(65, 1, beginGen)
val var220 = history[endGen].findByBeginGenOpt(112, 1, beginGen)
val var221 = history[endGen].findByBeginGenOpt(188, 5, beginGen)
val var222 = history[endGen].findByBeginGenOpt(189, 4, beginGen)
val var223 = history[endGen].findByBeginGenOpt(203, 8, beginGen)
val var224 = history[endGen].findByBeginGenOpt(207, 4, beginGen)
val var225 = history[endGen].findByBeginGenOpt(217, 1, beginGen)
val var226 = history[endGen].findByBeginGenOpt(242, 1, beginGen)
val var227 = history[endGen].findByBeginGenOpt(243, 5, beginGen)
check(hasSingleTrue(var219 != null, var220 != null, var221 != null, var222 != null, var223 != null, var224 != null, var225 != null, var226 != null, var227 != null))
val var228 = when {
var219 != null -> {
val var229 = matchSimpleName(beginGen, endGen)
val var230 = NameRef(var229, nextId(), beginGen, endGen)
var230
}
var220 != null -> {
val var231 = matchCallExpr(beginGen, endGen)
var231
}
var221 != null -> {
val var232 = getSequenceElems(history, 188, listOf(111,7,99,7,65), beginGen, endGen)
val var233 = matchPrimary(var232[0].first, var232[0].second)
val var234 = matchSimpleName(var232[4].first, var232[4].second)
val var235 = MemberAccess(var233, var234, nextId(), beginGen, endGen)
var235
}
var222 != null -> {
val var237 = getSequenceElems(history, 189, listOf(190,191,7,202), beginGen, endGen)
val var238 = history[var237[1].second].findByBeginGenOpt(100, 1, var237[1].first)
val var239 = history[var237[1].second].findByBeginGenOpt(192, 1, var237[1].first)
check(hasSingleTrue(var238 != null, var239 != null))
val var240 = when {
var238 != null -> null
else -> {
val var241 = getSequenceElems(history, 193, listOf(7,194,198,174), var237[1].first, var237[1].second)
val var242 = matchListElem(var241[1].first, var241[1].second)
val var243 = unrollRepeat0(history, 198, 200, 9, 199, var241[2].first, var241[2].second).map { k ->
val var244 = getSequenceElems(history, 201, listOf(7,149,7,194), k.first, k.second)
val var245 = matchListElem(var244[3].first, var244[3].second)
var245
}
listOf(var242) + var243
}
}
val var236 = var240
val var246 = ListExpr((var236 ?: listOf()), nextId(), beginGen, endGen)
var246
}
var223 != null -> {
val var247 = getSequenceElems(history, 203, listOf(116,7,121,7,149,204,7,117), beginGen, endGen)
val var248 = matchExpr(var247[2].first, var247[2].second)
val var250 = history[var247[5].second].findByBeginGenOpt(100, 1, var247[5].first)
val var251 = history[var247[5].second].findByBeginGenOpt(205, 1, var247[5].first)
check(hasSingleTrue(var250 != null, var251 != null))
val var252 = when {
var250 != null -> null
else -> {
val var253 = getSequenceElems(history, 206, listOf(7,121,170,174), var247[5].first, var247[5].second)
val var254 = matchExpr(var253[1].first, var253[1].second)
val var255 = unrollRepeat0(history, 170, 172, 9, 171, var253[2].first, var253[2].second).map { k ->
val var256 = getSequenceElems(history, 173, listOf(7,149,7,121), k.first, k.second)
val var257 = matchExpr(var256[3].first, var256[3].second)
var257
}
listOf(var254) + var255
}
}
val var249 = var252
val var258 = TupleExpr(listOf(var248) + (var249 ?: listOf()), nextId(), beginGen, endGen)
var258
}
var224 != null -> {
val var260 = getSequenceElems(history, 207, listOf(116,208,7,117), beginGen, endGen)
val var261 = history[var260[1].second].findByBeginGenOpt(100, 1, var260[1].first)
val var262 = history[var260[1].second].findByBeginGenOpt(209, 1, var260[1].first)
check(hasSingleTrue(var261 != null, var262 != null))
val var263 = when {
var261 != null -> null
else -> {
val var264 = getSequenceElems(history, 210, listOf(7,211,213,174), var260[1].first, var260[1].second)
val var265 = matchNamedExpr(var264[1].first, var264[1].second)
val var266 = unrollRepeat0(history, 213, 215, 9, 214, var264[2].first, var264[2].second).map { k ->
val var267 = getSequenceElems(history, 216, listOf(7,149,7,211), k.first, k.second)
val var268 = matchNamedExpr(var267[3].first, var267[3].second)
var268
}
listOf(var265) + var266
}
}
val var259 = var263
val var269 = NamedTupleExpr((var259 ?: listOf()), nextId(), beginGen, endGen)
var269
}
var225 != null -> {
val var270 = matchLiteral(beginGen, endGen)
var270
}
var226 != null -> {
val var271 = This(nextId(), beginGen, endGen)
var271
}
else -> {
val var272 = getSequenceElems(history, 243, listOf(116,7,121,7,117), beginGen, endGen)
val var273 = matchExpr(var272[2].first, var272[2].second)
val var274 = Paren(var273, nextId(), beginGen, endGen)
var274
}
}
return var228
}

fun matchLiteral(beginGen: Int, endGen: Int): Literal {
val var275 = history[endGen].findByBeginGenOpt(218, 1, beginGen)
val var276 = history[endGen].findByBeginGenOpt(238, 1, beginGen)
val var277 = history[endGen].findByBeginGenOpt(241, 1, beginGen)
check(hasSingleTrue(var275 != null, var276 != null, var277 != null))
val var278 = when {
var275 != null -> {
val var279 = matchStringLiteral(beginGen, endGen)
var279
}
var276 != null -> {
val var280 = matchBooleanLiteral(beginGen, endGen)
var280
}
else -> {
val var281 = matchNoneLiteral(beginGen, endGen)
var281
}
}
return var278
}

fun matchStringLiteral(beginGen: Int, endGen: Int): StringLiteral {
val var282 = getSequenceElems(history, 219, listOf(220,221,220), beginGen, endGen)
val var283 = unrollRepeat0(history, 221, 223, 9, 222, var282[1].first, var282[1].second).map { k ->
val var284 = matchStringElem(k.first, k.second)
var284
}
val var285 = StringLiteral(var283, nextId(), beginGen, endGen)
return var285
}

fun matchNoneLiteral(beginGen: Int, endGen: Int): NoneLiteral {
val var286 = NoneLiteral(nextId(), beginGen, endGen)
return var286
}

fun matchStringElem(beginGen: Int, endGen: Int): StringElem {
val var287 = history[endGen].findByBeginGenOpt(226, 1, beginGen)
val var288 = history[endGen].findByBeginGenOpt(228, 1, beginGen)
val var289 = history[endGen].findByBeginGenOpt(232, 1, beginGen)
check(hasSingleTrue(var287 != null, var288 != null, var289 != null))
val var290 = when {
var287 != null -> {
val var291 = JustChar(source[beginGen], nextId(), beginGen, endGen)
var291
}
var288 != null -> {
val var292 = matchEscapeChar(beginGen, endGen)
var292
}
else -> {
val var293 = matchStringExpr(beginGen, endGen)
var293
}
}
return var290
}

fun matchEscapeChar(beginGen: Int, endGen: Int): EscapeChar {
val var294 = getSequenceElems(history, 229, listOf(230,231), beginGen, endGen)
val var295 = EscapeChar(source[var294[1].first], nextId(), beginGen, endGen)
return var295
}

fun matchStringExpr(beginGen: Int, endGen: Int): StringExpr {
val var296 = history[endGen].findByBeginGenOpt(233, 1, beginGen)
val var297 = history[endGen].findByBeginGenOpt(237, 6, beginGen)
check(hasSingleTrue(var296 != null, var297 != null))
val var298 = when {
var296 != null -> {
val var299 = getSequenceElems(history, 235, listOf(236,65), beginGen, endGen)
val var300 = matchSimpleName(var299[1].first, var299[1].second)
val var301 = SimpleExpr(var300, nextId(), beginGen, endGen)
var301
}
else -> {
val var302 = getSequenceElems(history, 237, listOf(236,144,7,121,7,150), beginGen, endGen)
val var303 = matchExpr(var302[3].first, var302[3].second)
val var304 = ComplexExpr(var303, nextId(), beginGen, endGen)
var304
}
}
return var298
}

fun matchNamedExpr(beginGen: Int, endGen: Int): NamedExpr {
val var305 = getSequenceElems(history, 212, listOf(65,7,162,7,121), beginGen, endGen)
val var306 = matchSimpleName(var305[0].first, var305[0].second)
val var307 = matchExpr(var305[4].first, var305[4].second)
val var308 = NamedExpr(var306, var307, nextId(), beginGen, endGen)
return var308
}

fun matchVarRedefs(beginGen: Int, endGen: Int): VarRedefs {
val var309 = getSequenceElems(history, 350, listOf(341,7,351,355), beginGen, endGen)
val var310 = matchVarRedef(var309[2].first, var309[2].second)
val var311 = unrollRepeat0(history, 355, 357, 9, 356, var309[3].first, var309[3].second).map { k ->
val var312 = getSequenceElems(history, 358, listOf(7,149,7,351), k.first, k.second)
val var313 = matchVarRedef(var312[3].first, var312[3].second)
var313
}
val var314 = VarRedefs(listOf(var310) + var311, nextId(), beginGen, endGen)
return var314
}

fun matchVarRedef(beginGen: Int, endGen: Int): VarRedef {
val var315 = getSequenceElems(history, 352, listOf(65,353,7,182,7,121), beginGen, endGen)
val var316 = matchSimpleName(var315[0].first, var315[0].second)
val var317 = unrollRepeat1(history, 353, 97, 97, 354, var315[1].first, var315[1].second).map { k ->
val var318 = getSequenceElems(history, 98, listOf(7,99,7,65), k.first, k.second)
val var319 = matchSimpleName(var318[3].first, var318[3].second)
var319
}
val var320 = matchExpr(var315[5].first, var315[5].second)
val var321 = VarRedef(listOf(var316) + var317, var320, nextId(), beginGen, endGen)
return var321
}

fun matchTypeExpr(beginGen: Int, endGen: Int): TypeExpr {
val var322 = history[endGen].findByBeginGenOpt(127, 1, beginGen)
val var323 = history[endGen].findByBeginGenOpt(142, 1, beginGen)
check(hasSingleTrue(var322 != null, var323 != null))
val var324 = when {
var322 != null -> {
val var325 = matchNoUnionType(beginGen, endGen)
var325
}
else -> {
val var326 = matchUnionType(beginGen, endGen)
var326
}
}
return var324
}

fun matchListElem(beginGen: Int, endGen: Int): ListElem {
val var327 = history[endGen].findByBeginGenOpt(121, 1, beginGen)
val var328 = history[endGen].findByBeginGenOpt(195, 3, beginGen)
check(hasSingleTrue(var327 != null, var328 != null))
val var329 = when {
var327 != null -> {
val var330 = matchExpr(beginGen, endGen)
var330
}
else -> {
val var331 = getSequenceElems(history, 195, listOf(196,7,121), beginGen, endGen)
val var332 = matchExpr(var331[2].first, var331[2].second)
val var333 = EllipsisElem(var332, nextId(), beginGen, endGen)
var333
}
}
return var329
}

fun matchParamDef(beginGen: Int, endGen: Int): ParamDef {
val var334 = history[endGen].findByBeginGenOpt(181, 5, beginGen)
val var335 = history[endGen].findByBeginGenOpt(287, 7, beginGen)
check(hasSingleTrue(var334 != null, var335 != null))
val var336 = when {
var334 != null -> {
val var337 = getSequenceElems(history, 181, listOf(65,7,182,7,121), beginGen, endGen)
val var338 = matchSimpleName(var337[0].first, var337[0].second)
val var339 = matchExpr(var337[4].first, var337[4].second)
val var340 = ParamDef(var338, false, null, var339, nextId(), beginGen, endGen)
var340
}
else -> {
val var341 = getSequenceElems(history, 287, listOf(65,288,7,162,7,141,292), beginGen, endGen)
val var342 = matchSimpleName(var341[0].first, var341[0].second)
val var343 = history[var341[1].second].findByBeginGenOpt(100, 1, var341[1].first)
val var344 = history[var341[1].second].findByBeginGenOpt(289, 1, var341[1].first)
check(hasSingleTrue(var343 != null, var344 != null))
val var345 = when {
var343 != null -> null
else -> {
val var346 = getSequenceElems(history, 290, listOf(7,291), var341[1].first, var341[1].second)
source[var346[1].first]
}
}
val var347 = matchTypeExpr(var341[5].first, var341[5].second)
val var348 = history[var341[6].second].findByBeginGenOpt(100, 1, var341[6].first)
val var349 = history[var341[6].second].findByBeginGenOpt(293, 1, var341[6].first)
check(hasSingleTrue(var348 != null, var349 != null))
val var350 = when {
var348 != null -> null
else -> {
val var351 = getSequenceElems(history, 294, listOf(7,182,7,121), var341[6].first, var341[6].second)
val var352 = matchExpr(var351[3].first, var351[3].second)
var352
}
}
val var353 = ParamDef(var342, var345 != null, var347, var350, nextId(), beginGen, endGen)
var353
}
}
return var336
}

fun matchUnionType(beginGen: Int, endGen: Int): UnionType {
val var354 = getSequenceElems(history, 143, listOf(144,7,127,145,7,150), beginGen, endGen)
val var355 = matchNoUnionType(var354[2].first, var354[2].second)
val var356 = unrollRepeat0(history, 145, 147, 9, 146, var354[3].first, var354[3].second).map { k ->
val var357 = getSequenceElems(history, 148, listOf(7,149,7,127), k.first, k.second)
val var358 = matchNoUnionType(var357[3].first, var357[3].second)
var358
}
val var359 = UnionType(listOf(var355) + var356, nextId(), beginGen, endGen)
return var359
}

fun matchTargetDef(beginGen: Int, endGen: Int): TargetDef {
val var360 = getSequenceElems(history, 181, listOf(65,7,182,7,121), beginGen, endGen)
val var361 = matchSimpleName(var360[0].first, var360[0].second)
val var362 = matchExpr(var360[4].first, var360[4].second)
val var363 = TargetDef(var361, var362, nextId(), beginGen, endGen)
return var363
}

fun matchSuperClassDef(beginGen: Int, endGen: Int): SuperClassDef {
val var364 = getSequenceElems(history, 324, listOf(325,7,277,7,65,7,144,7,65,329,7,150), beginGen, endGen)
val var365 = matchSimpleName(var364[4].first, var364[4].second)
val var366 = matchSimpleName(var364[8].first, var364[8].second)
val var367 = unrollRepeat0(history, 329, 331, 9, 330, var364[9].first, var364[9].second).map { k ->
val var368 = getSequenceElems(history, 332, listOf(7,149,7,65), k.first, k.second)
val var369 = matchSimpleName(var368[3].first, var368[3].second)
var369
}
val var370 = SuperClassDef(var365, listOf(var366) + var367, nextId(), beginGen, endGen)
return var370
}

fun matchCallParams(beginGen: Int, endGen: Int): CallParams {
val var371 = history[endGen].findByBeginGenOpt(115, 3, beginGen)
val var372 = history[endGen].findByBeginGenOpt(118, 6, beginGen)
val var373 = history[endGen].findByBeginGenOpt(177, 6, beginGen)
val var374 = history[endGen].findByBeginGenOpt(187, 10, beginGen)
check(hasSingleTrue(var371 != null, var372 != null, var373 != null, var374 != null))
val var375 = when {
var371 != null -> {
val var376 = CallParams(listOf(), listOf(), nextId(), beginGen, endGen)
var376
}
var372 != null -> {
val var377 = getSequenceElems(history, 118, listOf(116,7,119,174,7,117), beginGen, endGen)
val var378 = matchPositionalParams(var377[2].first, var377[2].second)
val var379 = CallParams(var378, listOf(), nextId(), beginGen, endGen)
var379
}
var373 != null -> {
val var380 = getSequenceElems(history, 177, listOf(116,7,178,174,7,117), beginGen, endGen)
val var381 = matchNamedParams(var380[2].first, var380[2].second)
val var382 = CallParams(listOf(), var381, nextId(), beginGen, endGen)
var382
}
else -> {
val var383 = getSequenceElems(history, 187, listOf(116,7,119,7,149,7,178,174,7,117), beginGen, endGen)
val var384 = matchPositionalParams(var383[2].first, var383[2].second)
val var385 = matchNamedParams(var383[6].first, var383[6].second)
val var386 = CallParams(var384, var385, nextId(), beginGen, endGen)
var386
}
}
return var375
}

fun matchPositionalParams(beginGen: Int, endGen: Int): List<Expr> {
val var387 = getSequenceElems(history, 120, listOf(121,170), beginGen, endGen)
val var388 = matchExpr(var387[0].first, var387[0].second)
val var389 = unrollRepeat0(history, 170, 172, 9, 171, var387[1].first, var387[1].second).map { k ->
val var390 = getSequenceElems(history, 173, listOf(7,149,7,121), k.first, k.second)
val var391 = matchExpr(var390[3].first, var390[3].second)
var391
}
return listOf(var388) + var389
}

fun matchNamedParams(beginGen: Int, endGen: Int): List<NamedParam> {
val var392 = getSequenceElems(history, 179, listOf(180,183), beginGen, endGen)
val var393 = matchNamedParam(var392[0].first, var392[0].second)
val var394 = unrollRepeat0(history, 183, 185, 9, 184, var392[1].first, var392[1].second).map { k ->
val var395 = getSequenceElems(history, 186, listOf(7,149,7,180), k.first, k.second)
val var396 = matchNamedParam(var395[3].first, var395[3].second)
var396
}
return listOf(var393) + var394
}

fun matchNamedParam(beginGen: Int, endGen: Int): NamedParam {
val var397 = getSequenceElems(history, 181, listOf(65,7,182,7,121), beginGen, endGen)
val var398 = matchSimpleName(var397[0].first, var397[0].second)
val var399 = matchExpr(var397[4].first, var397[4].second)
val var400 = NamedParam(var398, var399, nextId(), beginGen, endGen)
return var400
}

fun matchNamedTupleType(beginGen: Int, endGen: Int): NamedTupleType {
val var401 = getSequenceElems(history, 159, listOf(116,7,160,163,7,117), beginGen, endGen)
val var402 = matchNamedType(var401[2].first, var401[2].second)
val var403 = unrollRepeat0(history, 163, 165, 9, 164, var401[3].first, var401[3].second).map { k ->
val var404 = getSequenceElems(history, 166, listOf(7,149,7,160), k.first, k.second)
val var405 = matchNamedType(var404[3].first, var404[3].second)
var405
}
val var406 = NamedTupleType(listOf(var402) + var403, nextId(), beginGen, endGen)
return var406
}

fun matchNamedType(beginGen: Int, endGen: Int): NamedType {
val var407 = getSequenceElems(history, 161, listOf(65,7,162,7,141), beginGen, endGen)
val var408 = matchSimpleName(var407[0].first, var407[0].second)
val var409 = matchTypeExpr(var407[4].first, var407[4].second)
val var410 = NamedType(var408, var409, nextId(), beginGen, endGen)
return var410
}

fun matchBooleanLiteral(beginGen: Int, endGen: Int): BooleanLiteral {
val var411 = history[endGen].findByBeginGenOpt(77, 1, beginGen)
val var412 = history[endGen].findByBeginGenOpt(82, 1, beginGen)
check(hasSingleTrue(var411 != null, var412 != null))
val var413 = when {
var411 != null -> {
val var414 = BooleanLiteral(true, nextId(), beginGen, endGen)
var414
}
else -> {
val var415 = BooleanLiteral(false, nextId(), beginGen, endGen)
var415
}
}
return var413
}

}
