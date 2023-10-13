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
): ActionExpr, NoUnionType, AstNode

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
  val expr: ActionExpr,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): ActionBody, AstNode

sealed interface ActionExpr: AstNode

data class DefsWithVarRedefs(
  val varRedefs: List<VarRedef>,
  val defs: List<Def>,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): Def, AstNode

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
): ActionExpr, Primary, AstNode

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
  val body: List<Def>,
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
  val exprs: List<ActionExpr>,
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
val var11 = getSequenceElems(history, 102, listOf(103,371), beginGen, endGen)
val var12 = matchDef(var11[0].first, var11[0].second)
val var13 = unrollRepeat0(history, 371, 373, 9, 372, var11[1].first, var11[1].second).map { k ->
val var14 = getSequenceElems(history, 374, listOf(7,103), k.first, k.second)
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
val var26 = history[endGen].findByBeginGenOpt(257, 1, beginGen)
val var27 = history[endGen].findByBeginGenOpt(258, 1, beginGen)
val var28 = history[endGen].findByBeginGenOpt(277, 1, beginGen)
val var29 = history[endGen].findByBeginGenOpt(312, 1, beginGen)
val var30 = history[endGen].findByBeginGenOpt(336, 1, beginGen)
val var31 = history[endGen].findByBeginGenOpt(342, 1, beginGen)
val var32 = history[endGen].findByBeginGenOpt(352, 1, beginGen)
val var33 = history[endGen].findByBeginGenOpt(362, 1, beginGen)
val var34 = history[endGen].findByBeginGenOpt(364, 1, beginGen)
check(hasSingleTrue(var24 != null, var25 != null, var26 != null, var27 != null, var28 != null, var29 != null, var30 != null, var31 != null, var32 != null, var33 != null, var34 != null))
val var35 = when {
var24 != null -> {
val var36 = matchImportDef(beginGen, endGen)
var36
}
var25 != null -> {
val var37 = matchNamespaceDef(beginGen, endGen)
var37
}
var26 != null -> {
val var38 = matchTargetDef(beginGen, endGen)
var38
}
var27 != null -> {
val var39 = matchActionDef(beginGen, endGen)
var39
}
var28 != null -> {
val var40 = matchClassDef(beginGen, endGen)
var40
}
var29 != null -> {
val var41 = matchActionRuleDef(beginGen, endGen)
var41
}
var30 != null -> {
val var42 = matchEnumDef(beginGen, endGen)
var42
}
var31 != null -> {
val var43 = matchVarDef(beginGen, endGen)
var43
}
var32 != null -> {
val var44 = matchVarRedefs(beginGen, endGen)
var44
}
var33 != null -> {
val var45 = matchBuildRuleDef(beginGen, endGen)
var45
}
else -> {
val var46 = matchDefsWithVarRedefs(beginGen, endGen)
var46
}
}
return var35
}

fun matchActionRuleDef(beginGen: Int, endGen: Int): ActionRuleDef {
val var47 = getSequenceElems(history, 313, listOf(260,7,314,7,65,7,284,7,182,7,319), beginGen, endGen)
val var48 = matchSimpleName(var47[4].first, var47[4].second)
val var49 = matchParamsDef(var47[6].first, var47[6].second)
val var50 = matchMethodRef(var47[10].first, var47[10].second)
val var51 = ActionRuleDef(var48, var49, var50, nextId(), beginGen, endGen)
return var51
}

fun matchBuildRuleDef(beginGen: Int, endGen: Int): BuildRuleDef {
val var52 = getSequenceElems(history, 363, listOf(314,7,65,7,284,7,162,7,141,7,182,7,319), beginGen, endGen)
val var53 = matchSimpleName(var52[2].first, var52[2].second)
val var54 = matchParamsDef(var52[4].first, var52[4].second)
val var55 = matchTypeExpr(var52[8].first, var52[8].second)
val var56 = matchMethodRef(var52[12].first, var52[12].second)
val var57 = BuildRuleDef(var53, var54, var55, var56, nextId(), beginGen, endGen)
return var57
}

fun matchParamsDef(beginGen: Int, endGen: Int): List<ParamDef> {
val var59 = getSequenceElems(history, 285, listOf(116,286,174,7,117), beginGen, endGen)
val var60 = history[var59[1].second].findByBeginGenOpt(100, 1, var59[1].first)
val var61 = history[var59[1].second].findByBeginGenOpt(287, 1, var59[1].first)
check(hasSingleTrue(var60 != null, var61 != null))
val var62 = when {
var60 != null -> null
else -> {
val var63 = getSequenceElems(history, 288, listOf(7,289,298), var59[1].first, var59[1].second)
val var64 = matchParamDef(var63[1].first, var63[1].second)
val var65 = unrollRepeat0(history, 298, 300, 9, 299, var63[2].first, var63[2].second).map { k ->
val var66 = getSequenceElems(history, 301, listOf(7,149,7,289), k.first, k.second)
val var67 = matchParamDef(var66[3].first, var66[3].second)
var67
}
listOf(var64) + var65
}
}
val var58 = var62
return (var58 ?: listOf())
}

fun matchActionDef(beginGen: Int, endGen: Int): ActionDef {
val var68 = getSequenceElems(history, 259, listOf(260,7,65,264,7,269), beginGen, endGen)
val var69 = matchSimpleName(var68[2].first, var68[2].second)
val var70 = history[var68[3].second].findByBeginGenOpt(100, 1, var68[3].first)
val var71 = history[var68[3].second].findByBeginGenOpt(265, 1, var68[3].first)
check(hasSingleTrue(var70 != null, var71 != null))
val var72 = when {
var70 != null -> null
else -> {
val var73 = getSequenceElems(history, 266, listOf(7,267), var68[3].first, var68[3].second)
val var74 = matchActionParams(var73[1].first, var73[1].second)
var74
}
}
val var75 = matchActionBody(var68[5].first, var68[5].second)
val var76 = ActionDef(var69, var72, var75, nextId(), beginGen, endGen)
return var76
}

fun matchActionParams(beginGen: Int, endGen: Int): String {
val var77 = getSequenceElems(history, 268, listOf(116,7,65,7,117), beginGen, endGen)
val var78 = matchSimpleName(var77[2].first, var77[2].second)
return var78
}

fun matchActionBody(beginGen: Int, endGen: Int): ActionBody {
val var79 = history[endGen].findByBeginGenOpt(270, 3, beginGen)
val var80 = history[endGen].findByBeginGenOpt(272, 4, beginGen)
check(hasSingleTrue(var79 != null, var80 != null))
val var81 = when {
var79 != null -> {
val var82 = getSequenceElems(history, 270, listOf(182,7,271), beginGen, endGen)
val var83 = matchActionExpr(var82[2].first, var82[2].second)
val var84 = SingleCallAction(var83, nextId(), beginGen, endGen)
var84
}
else -> {
val var85 = getSequenceElems(history, 272, listOf(144,273,7,150), beginGen, endGen)
val var86 = unrollRepeat1(history, 273, 274, 274, 276, var85[1].first, var85[1].second).map { k ->
val var87 = getSequenceElems(history, 275, listOf(7,271), k.first, k.second)
val var88 = matchActionExpr(var87[1].first, var87[1].second)
var88
}
val var89 = MultiCallActions(var86, nextId(), beginGen, endGen)
var89
}
}
return var81
}

fun matchClassDef(beginGen: Int, endGen: Int): ClassDef {
val var90 = history[endGen].findByBeginGenOpt(278, 1, beginGen)
val var91 = history[endGen].findByBeginGenOpt(326, 1, beginGen)
check(hasSingleTrue(var90 != null, var91 != null))
val var92 = when {
var90 != null -> {
val var93 = matchDataClassDef(beginGen, endGen)
var93
}
else -> {
val var94 = matchSuperClassDef(beginGen, endGen)
var94
}
}
return var92
}

fun matchDataClassDef(beginGen: Int, endGen: Int): DataClassDef {
val var95 = getSequenceElems(history, 279, listOf(280,7,65,7,284,302), beginGen, endGen)
val var96 = matchSimpleName(var95[2].first, var95[2].second)
val var97 = matchParamsDef(var95[4].first, var95[4].second)
val var99 = history[var95[5].second].findByBeginGenOpt(100, 1, var95[5].first)
val var100 = history[var95[5].second].findByBeginGenOpt(303, 1, var95[5].first)
check(hasSingleTrue(var99 != null, var100 != null))
val var101 = when {
var99 != null -> null
else -> {
val var102 = getSequenceElems(history, 304, listOf(7,305), var95[5].first, var95[5].second)
val var103 = matchClassBody(var102[1].first, var102[1].second)
var103
}
}
val var98 = var101
val var104 = DataClassDef(var96, var97, (var98 ?: listOf()), nextId(), beginGen, endGen)
return var104
}

fun matchMethodRef(beginGen: Int, endGen: Int): MethodRef {
val var105 = getSequenceElems(history, 320, listOf(63,7,162,7,63,321), beginGen, endGen)
val var106 = matchName(var105[0].first, var105[0].second)
val var107 = matchName(var105[4].first, var105[4].second)
val var108 = history[var105[5].second].findByBeginGenOpt(100, 1, var105[5].first)
val var109 = history[var105[5].second].findByBeginGenOpt(322, 1, var105[5].first)
check(hasSingleTrue(var108 != null, var109 != null))
val var110 = when {
var108 != null -> null
else -> {
val var111 = getSequenceElems(history, 323, listOf(7,162,7,65), var105[5].first, var105[5].second)
val var112 = matchSimpleName(var111[3].first, var111[3].second)
var112
}
}
val var113 = MethodRef(var106, var107, var110, nextId(), beginGen, endGen)
return var113
}

fun matchClassBody(beginGen: Int, endGen: Int): List<ClassBodyElem> {
val var114 = getSequenceElems(history, 306, listOf(144,307,7,150), beginGen, endGen)
val var115 = unrollRepeat0(history, 307, 309, 9, 308, var114[1].first, var114[1].second).map { k ->
val var116 = getSequenceElems(history, 310, listOf(7,311), k.first, k.second)
val var117 = matchClassBodyElem(var116[1].first, var116[1].second)
var117
}
return var115
}

fun matchClassBodyElem(beginGen: Int, endGen: Int): ClassBodyElem {
val var118 = history[endGen].findByBeginGenOpt(312, 1, beginGen)
val var119 = history[endGen].findByBeginGenOpt(324, 1, beginGen)
check(hasSingleTrue(var118 != null, var119 != null))
val var120 = when {
var118 != null -> {
val var121 = matchActionRuleDef(beginGen, endGen)
var121
}
else -> {
val var122 = matchClassCastDef(beginGen, endGen)
var122
}
}
return var120
}

fun matchClassCastDef(beginGen: Int, endGen: Int): ClassCastDef {
val var123 = getSequenceElems(history, 325, listOf(123,7,141,7,182,7,121), beginGen, endGen)
val var124 = matchTypeExpr(var123[2].first, var123[2].second)
val var125 = matchExpr(var123[6].first, var123[6].second)
val var126 = ClassCastDef(var124, var125, nextId(), beginGen, endGen)
return var126
}

fun matchImportDef(beginGen: Int, endGen: Int): ImportDef {
val var127 = history[endGen].findByBeginGenOpt(105, 4, beginGen)
val var128 = history[endGen].findByBeginGenOpt(247, 8, beginGen)
check(hasSingleTrue(var127 != null, var128 != null))
val var129 = when {
var127 != null -> {
val var130 = getSequenceElems(history, 105, listOf(106,7,111,244), beginGen, endGen)
val var131 = matchPrimary(var130[2].first, var130[2].second)
val var132 = history[var130[3].second].findByBeginGenOpt(100, 1, var130[3].first)
val var133 = history[var130[3].second].findByBeginGenOpt(245, 1, var130[3].first)
check(hasSingleTrue(var132 != null, var133 != null))
val var134 = when {
var132 != null -> null
else -> {
val var135 = getSequenceElems(history, 246, listOf(7,123,7,65), var130[3].first, var130[3].second)
val var136 = matchSimpleName(var135[3].first, var135[3].second)
var136
}
}
val var137 = ImportAll(var131, var134, nextId(), beginGen, endGen)
var137
}
else -> {
val var138 = getSequenceElems(history, 247, listOf(248,7,121,7,106,7,63,244), beginGen, endGen)
val var139 = matchExpr(var138[2].first, var138[2].second)
val var140 = matchName(var138[6].first, var138[6].second)
val var141 = history[var138[7].second].findByBeginGenOpt(100, 1, var138[7].first)
val var142 = history[var138[7].second].findByBeginGenOpt(245, 1, var138[7].first)
check(hasSingleTrue(var141 != null, var142 != null))
val var143 = when {
var141 != null -> null
else -> {
val var144 = getSequenceElems(history, 246, listOf(7,123,7,65), var138[7].first, var138[7].second)
val var145 = matchSimpleName(var144[3].first, var144[3].second)
var145
}
}
val var146 = ImportFrom(var139, var140, var143, nextId(), beginGen, endGen)
var146
}
}
return var129
}

fun matchActionExpr(beginGen: Int, endGen: Int): ActionExpr {
val var147 = history[endGen].findByBeginGenOpt(63, 1, beginGen)
val var148 = history[endGen].findByBeginGenOpt(112, 1, beginGen)
check(hasSingleTrue(var147 != null, var148 != null))
val var149 = when {
var147 != null -> {
val var150 = matchName(beginGen, endGen)
var150
}
else -> {
val var151 = matchCallExpr(beginGen, endGen)
var151
}
}
return var149
}

fun matchCallExpr(beginGen: Int, endGen: Int): CallExpr {
val var152 = getSequenceElems(history, 113, listOf(63,7,114), beginGen, endGen)
val var153 = matchName(var152[0].first, var152[0].second)
val var154 = matchCallParams(var152[2].first, var152[2].second)
val var155 = CallExpr(var153, var154, nextId(), beginGen, endGen)
return var155
}

fun matchCallParams(beginGen: Int, endGen: Int): CallParams {
val var156 = history[endGen].findByBeginGenOpt(115, 3, beginGen)
val var157 = history[endGen].findByBeginGenOpt(118, 6, beginGen)
val var158 = history[endGen].findByBeginGenOpt(177, 6, beginGen)
val var159 = history[endGen].findByBeginGenOpt(187, 10, beginGen)
check(hasSingleTrue(var156 != null, var157 != null, var158 != null, var159 != null))
val var160 = when {
var156 != null -> {
val var161 = CallParams(listOf(), listOf(), nextId(), beginGen, endGen)
var161
}
var157 != null -> {
val var162 = getSequenceElems(history, 118, listOf(116,7,119,174,7,117), beginGen, endGen)
val var163 = matchPositionalParams(var162[2].first, var162[2].second)
val var164 = CallParams(var163, listOf(), nextId(), beginGen, endGen)
var164
}
var158 != null -> {
val var165 = getSequenceElems(history, 177, listOf(116,7,178,174,7,117), beginGen, endGen)
val var166 = matchNamedParams(var165[2].first, var165[2].second)
val var167 = CallParams(listOf(), var166, nextId(), beginGen, endGen)
var167
}
else -> {
val var168 = getSequenceElems(history, 187, listOf(116,7,119,7,149,7,178,174,7,117), beginGen, endGen)
val var169 = matchPositionalParams(var168[2].first, var168[2].second)
val var170 = matchNamedParams(var168[6].first, var168[6].second)
val var171 = CallParams(var169, var170, nextId(), beginGen, endGen)
var171
}
}
return var160
}

fun matchPositionalParams(beginGen: Int, endGen: Int): List<Expr> {
val var172 = getSequenceElems(history, 120, listOf(121,170), beginGen, endGen)
val var173 = matchExpr(var172[0].first, var172[0].second)
val var174 = unrollRepeat0(history, 170, 172, 9, 171, var172[1].first, var172[1].second).map { k ->
val var175 = getSequenceElems(history, 173, listOf(7,149,7,121), k.first, k.second)
val var176 = matchExpr(var175[3].first, var175[3].second)
var176
}
return listOf(var173) + var174
}

fun matchNamedParams(beginGen: Int, endGen: Int): List<NamedParam> {
val var177 = getSequenceElems(history, 179, listOf(180,183), beginGen, endGen)
val var178 = matchNamedParam(var177[0].first, var177[0].second)
val var179 = unrollRepeat0(history, 183, 185, 9, 184, var177[1].first, var177[1].second).map { k ->
val var180 = getSequenceElems(history, 186, listOf(7,149,7,180), k.first, k.second)
val var181 = matchNamedParam(var180[3].first, var180[3].second)
var181
}
return listOf(var178) + var179
}

fun matchNamedParam(beginGen: Int, endGen: Int): NamedParam {
val var182 = getSequenceElems(history, 181, listOf(65,7,182,7,121), beginGen, endGen)
val var183 = matchSimpleName(var182[0].first, var182[0].second)
val var184 = matchExpr(var182[4].first, var182[4].second)
val var185 = NamedParam(var183, var184, nextId(), beginGen, endGen)
return var185
}

fun matchDefsWithVarRedefs(beginGen: Int, endGen: Int): DefsWithVarRedefs {
val var186 = getSequenceElems(history, 365, listOf(366,7,354,358,7,144,7,101,7,150), beginGen, endGen)
val var187 = matchVarRedef(var186[2].first, var186[2].second)
val var188 = unrollRepeat0(history, 358, 360, 9, 359, var186[3].first, var186[3].second).map { k ->
val var189 = getSequenceElems(history, 361, listOf(7,149,7,354), k.first, k.second)
val var190 = matchVarRedef(var189[3].first, var189[3].second)
var190
}
val var191 = matchDefs(var186[7].first, var186[7].second)
val var192 = DefsWithVarRedefs(listOf(var187) + var188, var191, nextId(), beginGen, endGen)
return var192
}

fun matchVarRedef(beginGen: Int, endGen: Int): VarRedef {
val var193 = getSequenceElems(history, 355, listOf(65,356,7,182,7,121), beginGen, endGen)
val var194 = matchSimpleName(var193[0].first, var193[0].second)
val var195 = unrollRepeat1(history, 356, 97, 97, 357, var193[1].first, var193[1].second).map { k ->
val var196 = getSequenceElems(history, 98, listOf(7,99,7,65), k.first, k.second)
val var197 = matchSimpleName(var196[3].first, var196[3].second)
var197
}
val var198 = matchExpr(var193[5].first, var193[5].second)
val var199 = VarRedef(listOf(var194) + var195, var198, nextId(), beginGen, endGen)
return var199
}

fun matchExpr(beginGen: Int, endGen: Int): Expr {
val var200 = history[endGen].findByBeginGenOpt(122, 5, beginGen)
val var201 = history[endGen].findByBeginGenOpt(167, 1, beginGen)
check(hasSingleTrue(var200 != null, var201 != null))
val var202 = when {
var200 != null -> {
val var203 = getSequenceElems(history, 122, listOf(121,7,123,7,127), beginGen, endGen)
val var204 = matchExpr(var203[0].first, var203[0].second)
val var205 = matchNoUnionType(var203[4].first, var203[4].second)
val var206 = CastExpr(var204, var205, nextId(), beginGen, endGen)
var206
}
else -> {
val var207 = matchMergeOpOrPrimary(beginGen, endGen)
var207
}
}
return var202
}

fun matchNoUnionType(beginGen: Int, endGen: Int): NoUnionType {
val var208 = history[endGen].findByBeginGenOpt(63, 1, beginGen)
val var209 = history[endGen].findByBeginGenOpt(128, 1, beginGen)
val var210 = history[endGen].findByBeginGenOpt(129, 1, beginGen)
val var211 = history[endGen].findByBeginGenOpt(156, 1, beginGen)
val var212 = history[endGen].findByBeginGenOpt(158, 1, beginGen)
check(hasSingleTrue(var208 != null, var209 != null, var210 != null, var211 != null, var212 != null))
val var213 = when {
var208 != null -> {
val var214 = matchName(beginGen, endGen)
var214
}
var209 != null -> {
val var215 = NoneType(nextId(), beginGen, endGen)
var215
}
var210 != null -> {
val var216 = matchCollectionType(beginGen, endGen)
var216
}
var211 != null -> {
val var217 = matchTupleType(beginGen, endGen)
var217
}
else -> {
val var218 = matchNamedTupleType(beginGen, endGen)
var218
}
}
return var213
}

fun matchMergeOpOrPrimary(beginGen: Int, endGen: Int): MergeOpOrPrimary {
val var219 = history[endGen].findByBeginGenOpt(111, 1, beginGen)
val var220 = history[endGen].findByBeginGenOpt(168, 5, beginGen)
check(hasSingleTrue(var219 != null, var220 != null))
val var221 = when {
var219 != null -> {
val var222 = matchPrimary(beginGen, endGen)
var222
}
else -> {
val var223 = getSequenceElems(history, 168, listOf(121,7,169,7,111), beginGen, endGen)
val var224 = matchExpr(var223[0].first, var223[0].second)
val var225 = matchPrimary(var223[4].first, var223[4].second)
val var226 = MergeOp(var224, var225, nextId(), beginGen, endGen)
var226
}
}
return var221
}

fun matchCollectionType(beginGen: Int, endGen: Int): CollectionType {
val var227 = getSequenceElems(history, 130, listOf(131,7,138), beginGen, endGen)
val var228 = history[var227[0].second].findByBeginGenOpt(134, 1, var227[0].first)
val var229 = history[var227[0].second].findByBeginGenOpt(136, 1, var227[0].first)
check(hasSingleTrue(var228 != null, var229 != null))
val var230 = when {
var228 != null -> "set"
else -> "list"
}
val var231 = matchTypeParams(var227[2].first, var227[2].second)
val var232 = CollectionType(var230, var231, nextId(), beginGen, endGen)
return var232
}

fun matchTypeParams(beginGen: Int, endGen: Int): TypeParams {
val var233 = getSequenceElems(history, 139, listOf(140,7,141,151,7,155), beginGen, endGen)
val var234 = matchTypeExpr(var233[2].first, var233[2].second)
val var235 = unrollRepeat0(history, 151, 153, 9, 152, var233[3].first, var233[3].second).map { k ->
val var236 = getSequenceElems(history, 154, listOf(7,149,7,141), k.first, k.second)
val var237 = matchTypeExpr(var236[3].first, var236[3].second)
var237
}
val var238 = TypeParams(listOf(var234) + var235, nextId(), beginGen, endGen)
return var238
}

fun matchTupleType(beginGen: Int, endGen: Int): TupleType {
val var239 = getSequenceElems(history, 157, listOf(116,7,141,151,7,117), beginGen, endGen)
val var240 = matchTypeExpr(var239[2].first, var239[2].second)
val var241 = unrollRepeat0(history, 151, 153, 9, 152, var239[3].first, var239[3].second).map { k ->
val var242 = getSequenceElems(history, 154, listOf(7,149,7,141), k.first, k.second)
val var243 = matchTypeExpr(var242[3].first, var242[3].second)
var243
}
val var244 = TupleType(listOf(var240) + var241, nextId(), beginGen, endGen)
return var244
}

fun matchNamedTupleType(beginGen: Int, endGen: Int): NamedTupleType {
val var245 = getSequenceElems(history, 159, listOf(116,7,160,163,7,117), beginGen, endGen)
val var246 = matchNamedType(var245[2].first, var245[2].second)
val var247 = unrollRepeat0(history, 163, 165, 9, 164, var245[3].first, var245[3].second).map { k ->
val var248 = getSequenceElems(history, 166, listOf(7,149,7,160), k.first, k.second)
val var249 = matchNamedType(var248[3].first, var248[3].second)
var249
}
val var250 = NamedTupleType(listOf(var246) + var247, nextId(), beginGen, endGen)
return var250
}

fun matchNamedType(beginGen: Int, endGen: Int): NamedType {
val var251 = getSequenceElems(history, 161, listOf(65,7,162,7,141), beginGen, endGen)
val var252 = matchSimpleName(var251[0].first, var251[0].second)
val var253 = matchTypeExpr(var251[4].first, var251[4].second)
val var254 = NamedType(var252, var253, nextId(), beginGen, endGen)
return var254
}

fun matchVarDef(beginGen: Int, endGen: Int): VarDef {
val var255 = getSequenceElems(history, 343, listOf(344,7,65,349,295), beginGen, endGen)
val var256 = matchSimpleName(var255[2].first, var255[2].second)
val var257 = history[var255[3].second].findByBeginGenOpt(100, 1, var255[3].first)
val var258 = history[var255[3].second].findByBeginGenOpt(350, 1, var255[3].first)
check(hasSingleTrue(var257 != null, var258 != null))
val var259 = when {
var257 != null -> null
else -> {
val var260 = getSequenceElems(history, 351, listOf(7,162,7,141), var255[3].first, var255[3].second)
val var261 = matchTypeExpr(var260[3].first, var260[3].second)
var261
}
}
val var262 = history[var255[4].second].findByBeginGenOpt(100, 1, var255[4].first)
val var263 = history[var255[4].second].findByBeginGenOpt(296, 1, var255[4].first)
check(hasSingleTrue(var262 != null, var263 != null))
val var264 = when {
var262 != null -> null
else -> {
val var265 = getSequenceElems(history, 297, listOf(7,182,7,121), var255[4].first, var255[4].second)
val var266 = matchExpr(var265[3].first, var265[3].second)
var266
}
}
val var267 = VarDef(var256, var259, var264, nextId(), beginGen, endGen)
return var267
}

fun matchNamespaceDef(beginGen: Int, endGen: Int): NamespaceDef {
val var268 = getSequenceElems(history, 253, listOf(65,7,144,254,7,150), beginGen, endGen)
val var269 = matchSimpleName(var268[0].first, var268[0].second)
val var271 = history[var268[3].second].findByBeginGenOpt(100, 1, var268[3].first)
val var272 = history[var268[3].second].findByBeginGenOpt(255, 1, var268[3].first)
check(hasSingleTrue(var271 != null, var272 != null))
val var273 = when {
var271 != null -> null
else -> {
val var274 = getSequenceElems(history, 256, listOf(7,101), var268[3].first, var268[3].second)
val var275 = matchDefs(var274[1].first, var274[1].second)
var275
}
}
val var270 = var273
val var276 = NamespaceDef(var269, (var270 ?: listOf()), nextId(), beginGen, endGen)
return var276
}

fun matchEnumDef(beginGen: Int, endGen: Int): EnumDef {
val var277 = getSequenceElems(history, 337, listOf(338,7,65,7,144,7,65,332,174,7,150), beginGen, endGen)
val var278 = matchSimpleName(var277[2].first, var277[2].second)
val var279 = matchSimpleName(var277[6].first, var277[6].second)
val var280 = unrollRepeat0(history, 332, 334, 9, 333, var277[7].first, var277[7].second).map { k ->
val var281 = getSequenceElems(history, 335, listOf(7,149,7,65), k.first, k.second)
val var282 = matchSimpleName(var281[3].first, var281[3].second)
var282
}
val var283 = EnumDef(var278, listOf(var279) + var280, nextId(), beginGen, endGen)
return var283
}

fun matchPrimary(beginGen: Int, endGen: Int): Primary {
val var284 = history[endGen].findByBeginGenOpt(65, 1, beginGen)
val var285 = history[endGen].findByBeginGenOpt(112, 1, beginGen)
val var286 = history[endGen].findByBeginGenOpt(188, 5, beginGen)
val var287 = history[endGen].findByBeginGenOpt(189, 4, beginGen)
val var288 = history[endGen].findByBeginGenOpt(203, 8, beginGen)
val var289 = history[endGen].findByBeginGenOpt(207, 4, beginGen)
val var290 = history[endGen].findByBeginGenOpt(217, 1, beginGen)
val var291 = history[endGen].findByBeginGenOpt(242, 1, beginGen)
val var292 = history[endGen].findByBeginGenOpt(243, 5, beginGen)
check(hasSingleTrue(var284 != null, var285 != null, var286 != null, var287 != null, var288 != null, var289 != null, var290 != null, var291 != null, var292 != null))
val var293 = when {
var284 != null -> {
val var294 = matchSimpleName(beginGen, endGen)
val var295 = NameRef(var294, nextId(), beginGen, endGen)
var295
}
var285 != null -> {
val var296 = matchCallExpr(beginGen, endGen)
var296
}
var286 != null -> {
val var297 = getSequenceElems(history, 188, listOf(111,7,99,7,65), beginGen, endGen)
val var298 = matchPrimary(var297[0].first, var297[0].second)
val var299 = matchSimpleName(var297[4].first, var297[4].second)
val var300 = MemberAccess(var298, var299, nextId(), beginGen, endGen)
var300
}
var287 != null -> {
val var302 = getSequenceElems(history, 189, listOf(190,191,7,202), beginGen, endGen)
val var303 = history[var302[1].second].findByBeginGenOpt(100, 1, var302[1].first)
val var304 = history[var302[1].second].findByBeginGenOpt(192, 1, var302[1].first)
check(hasSingleTrue(var303 != null, var304 != null))
val var305 = when {
var303 != null -> null
else -> {
val var306 = getSequenceElems(history, 193, listOf(7,194,198,174), var302[1].first, var302[1].second)
val var307 = matchListElem(var306[1].first, var306[1].second)
val var308 = unrollRepeat0(history, 198, 200, 9, 199, var306[2].first, var306[2].second).map { k ->
val var309 = getSequenceElems(history, 201, listOf(7,149,7,194), k.first, k.second)
val var310 = matchListElem(var309[3].first, var309[3].second)
var310
}
listOf(var307) + var308
}
}
val var301 = var305
val var311 = ListExpr((var301 ?: listOf()), nextId(), beginGen, endGen)
var311
}
var288 != null -> {
val var312 = getSequenceElems(history, 203, listOf(116,7,121,7,149,204,7,117), beginGen, endGen)
val var313 = matchExpr(var312[2].first, var312[2].second)
val var315 = history[var312[5].second].findByBeginGenOpt(100, 1, var312[5].first)
val var316 = history[var312[5].second].findByBeginGenOpt(205, 1, var312[5].first)
check(hasSingleTrue(var315 != null, var316 != null))
val var317 = when {
var315 != null -> null
else -> {
val var318 = getSequenceElems(history, 206, listOf(7,121,170,174), var312[5].first, var312[5].second)
val var319 = matchExpr(var318[1].first, var318[1].second)
val var320 = unrollRepeat0(history, 170, 172, 9, 171, var318[2].first, var318[2].second).map { k ->
val var321 = getSequenceElems(history, 173, listOf(7,149,7,121), k.first, k.second)
val var322 = matchExpr(var321[3].first, var321[3].second)
var322
}
listOf(var319) + var320
}
}
val var314 = var317
val var323 = TupleExpr(listOf(var313) + (var314 ?: listOf()), nextId(), beginGen, endGen)
var323
}
var289 != null -> {
val var325 = getSequenceElems(history, 207, listOf(116,208,7,117), beginGen, endGen)
val var326 = history[var325[1].second].findByBeginGenOpt(100, 1, var325[1].first)
val var327 = history[var325[1].second].findByBeginGenOpt(209, 1, var325[1].first)
check(hasSingleTrue(var326 != null, var327 != null))
val var328 = when {
var326 != null -> null
else -> {
val var329 = getSequenceElems(history, 210, listOf(7,211,213,174), var325[1].first, var325[1].second)
val var330 = matchNamedExpr(var329[1].first, var329[1].second)
val var331 = unrollRepeat0(history, 213, 215, 9, 214, var329[2].first, var329[2].second).map { k ->
val var332 = getSequenceElems(history, 216, listOf(7,149,7,211), k.first, k.second)
val var333 = matchNamedExpr(var332[3].first, var332[3].second)
var333
}
listOf(var330) + var331
}
}
val var324 = var328
val var334 = NamedTupleExpr((var324 ?: listOf()), nextId(), beginGen, endGen)
var334
}
var290 != null -> {
val var335 = matchLiteral(beginGen, endGen)
var335
}
var291 != null -> {
val var336 = This(nextId(), beginGen, endGen)
var336
}
else -> {
val var337 = getSequenceElems(history, 243, listOf(116,7,121,7,117), beginGen, endGen)
val var338 = matchExpr(var337[2].first, var337[2].second)
val var339 = Paren(var338, nextId(), beginGen, endGen)
var339
}
}
return var293
}

fun matchLiteral(beginGen: Int, endGen: Int): Literal {
val var340 = history[endGen].findByBeginGenOpt(218, 1, beginGen)
val var341 = history[endGen].findByBeginGenOpt(238, 1, beginGen)
val var342 = history[endGen].findByBeginGenOpt(241, 1, beginGen)
check(hasSingleTrue(var340 != null, var341 != null, var342 != null))
val var343 = when {
var340 != null -> {
val var344 = matchStringLiteral(beginGen, endGen)
var344
}
var341 != null -> {
val var345 = matchBooleanLiteral(beginGen, endGen)
var345
}
else -> {
val var346 = matchNoneLiteral(beginGen, endGen)
var346
}
}
return var343
}

fun matchStringLiteral(beginGen: Int, endGen: Int): StringLiteral {
val var347 = getSequenceElems(history, 219, listOf(220,221,220), beginGen, endGen)
val var348 = unrollRepeat0(history, 221, 223, 9, 222, var347[1].first, var347[1].second).map { k ->
val var349 = matchStringElem(k.first, k.second)
var349
}
val var350 = StringLiteral(var348, nextId(), beginGen, endGen)
return var350
}

fun matchNoneLiteral(beginGen: Int, endGen: Int): NoneLiteral {
val var351 = NoneLiteral(nextId(), beginGen, endGen)
return var351
}

fun matchBooleanLiteral(beginGen: Int, endGen: Int): BooleanLiteral {
val var352 = history[endGen].findByBeginGenOpt(77, 1, beginGen)
val var353 = history[endGen].findByBeginGenOpt(82, 1, beginGen)
check(hasSingleTrue(var352 != null, var353 != null))
val var354 = when {
var352 != null -> {
val var355 = BooleanLiteral(true, nextId(), beginGen, endGen)
var355
}
else -> {
val var356 = BooleanLiteral(false, nextId(), beginGen, endGen)
var356
}
}
return var354
}

fun matchStringElem(beginGen: Int, endGen: Int): StringElem {
val var357 = history[endGen].findByBeginGenOpt(226, 1, beginGen)
val var358 = history[endGen].findByBeginGenOpt(228, 1, beginGen)
val var359 = history[endGen].findByBeginGenOpt(232, 1, beginGen)
check(hasSingleTrue(var357 != null, var358 != null, var359 != null))
val var360 = when {
var357 != null -> {
val var361 = JustChar(source[beginGen], nextId(), beginGen, endGen)
var361
}
var358 != null -> {
val var362 = matchEscapeChar(beginGen, endGen)
var362
}
else -> {
val var363 = matchStringExpr(beginGen, endGen)
var363
}
}
return var360
}

fun matchEscapeChar(beginGen: Int, endGen: Int): EscapeChar {
val var364 = getSequenceElems(history, 229, listOf(230,231), beginGen, endGen)
val var365 = EscapeChar(source[var364[1].first], nextId(), beginGen, endGen)
return var365
}

fun matchStringExpr(beginGen: Int, endGen: Int): StringExpr {
val var366 = history[endGen].findByBeginGenOpt(233, 1, beginGen)
val var367 = history[endGen].findByBeginGenOpt(237, 6, beginGen)
check(hasSingleTrue(var366 != null, var367 != null))
val var368 = when {
var366 != null -> {
val var369 = getSequenceElems(history, 235, listOf(236,65), beginGen, endGen)
val var370 = matchSimpleName(var369[1].first, var369[1].second)
val var371 = SimpleExpr(var370, nextId(), beginGen, endGen)
var371
}
else -> {
val var372 = getSequenceElems(history, 237, listOf(236,144,7,121,7,150), beginGen, endGen)
val var373 = matchExpr(var372[3].first, var372[3].second)
val var374 = ComplexExpr(var373, nextId(), beginGen, endGen)
var374
}
}
return var368
}

fun matchNamedExpr(beginGen: Int, endGen: Int): NamedExpr {
val var375 = getSequenceElems(history, 212, listOf(65,7,162,7,121), beginGen, endGen)
val var376 = matchSimpleName(var375[0].first, var375[0].second)
val var377 = matchExpr(var375[4].first, var375[4].second)
val var378 = NamedExpr(var376, var377, nextId(), beginGen, endGen)
return var378
}

fun matchVarRedefs(beginGen: Int, endGen: Int): VarRedefs {
val var379 = getSequenceElems(history, 353, listOf(344,7,354,358), beginGen, endGen)
val var380 = matchVarRedef(var379[2].first, var379[2].second)
val var381 = unrollRepeat0(history, 358, 360, 9, 359, var379[3].first, var379[3].second).map { k ->
val var382 = getSequenceElems(history, 361, listOf(7,149,7,354), k.first, k.second)
val var383 = matchVarRedef(var382[3].first, var382[3].second)
var383
}
val var384 = VarRedefs(listOf(var380) + var381, nextId(), beginGen, endGen)
return var384
}

fun matchTypeExpr(beginGen: Int, endGen: Int): TypeExpr {
val var385 = history[endGen].findByBeginGenOpt(127, 1, beginGen)
val var386 = history[endGen].findByBeginGenOpt(142, 1, beginGen)
check(hasSingleTrue(var385 != null, var386 != null))
val var387 = when {
var385 != null -> {
val var388 = matchNoUnionType(beginGen, endGen)
var388
}
else -> {
val var389 = matchUnionType(beginGen, endGen)
var389
}
}
return var387
}

fun matchListElem(beginGen: Int, endGen: Int): ListElem {
val var390 = history[endGen].findByBeginGenOpt(121, 1, beginGen)
val var391 = history[endGen].findByBeginGenOpt(195, 3, beginGen)
check(hasSingleTrue(var390 != null, var391 != null))
val var392 = when {
var390 != null -> {
val var393 = matchExpr(beginGen, endGen)
var393
}
else -> {
val var394 = getSequenceElems(history, 195, listOf(196,7,121), beginGen, endGen)
val var395 = matchExpr(var394[2].first, var394[2].second)
val var396 = EllipsisElem(var395, nextId(), beginGen, endGen)
var396
}
}
return var392
}

fun matchParamDef(beginGen: Int, endGen: Int): ParamDef {
val var397 = history[endGen].findByBeginGenOpt(181, 5, beginGen)
val var398 = history[endGen].findByBeginGenOpt(290, 7, beginGen)
check(hasSingleTrue(var397 != null, var398 != null))
val var399 = when {
var397 != null -> {
val var400 = getSequenceElems(history, 181, listOf(65,7,182,7,121), beginGen, endGen)
val var401 = matchSimpleName(var400[0].first, var400[0].second)
val var402 = matchExpr(var400[4].first, var400[4].second)
val var403 = ParamDef(var401, false, null, var402, nextId(), beginGen, endGen)
var403
}
else -> {
val var404 = getSequenceElems(history, 290, listOf(65,291,7,162,7,141,295), beginGen, endGen)
val var405 = matchSimpleName(var404[0].first, var404[0].second)
val var406 = history[var404[1].second].findByBeginGenOpt(100, 1, var404[1].first)
val var407 = history[var404[1].second].findByBeginGenOpt(292, 1, var404[1].first)
check(hasSingleTrue(var406 != null, var407 != null))
val var408 = when {
var406 != null -> null
else -> {
val var409 = getSequenceElems(history, 293, listOf(7,294), var404[1].first, var404[1].second)
source[var409[1].first]
}
}
val var410 = matchTypeExpr(var404[5].first, var404[5].second)
val var411 = history[var404[6].second].findByBeginGenOpt(100, 1, var404[6].first)
val var412 = history[var404[6].second].findByBeginGenOpt(296, 1, var404[6].first)
check(hasSingleTrue(var411 != null, var412 != null))
val var413 = when {
var411 != null -> null
else -> {
val var414 = getSequenceElems(history, 297, listOf(7,182,7,121), var404[6].first, var404[6].second)
val var415 = matchExpr(var414[3].first, var414[3].second)
var415
}
}
val var416 = ParamDef(var405, var408 != null, var410, var413, nextId(), beginGen, endGen)
var416
}
}
return var399
}

fun matchUnionType(beginGen: Int, endGen: Int): UnionType {
val var417 = getSequenceElems(history, 143, listOf(144,7,127,145,7,150), beginGen, endGen)
val var418 = matchNoUnionType(var417[2].first, var417[2].second)
val var419 = unrollRepeat0(history, 145, 147, 9, 146, var417[3].first, var417[3].second).map { k ->
val var420 = getSequenceElems(history, 148, listOf(7,149,7,127), k.first, k.second)
val var421 = matchNoUnionType(var420[3].first, var420[3].second)
var421
}
val var422 = UnionType(listOf(var418) + var419, nextId(), beginGen, endGen)
return var422
}

fun matchTargetDef(beginGen: Int, endGen: Int): TargetDef {
val var423 = getSequenceElems(history, 181, listOf(65,7,182,7,121), beginGen, endGen)
val var424 = matchSimpleName(var423[0].first, var423[0].second)
val var425 = matchExpr(var423[4].first, var423[4].second)
val var426 = TargetDef(var424, var425, nextId(), beginGen, endGen)
return var426
}

fun matchSuperClassDef(beginGen: Int, endGen: Int): SuperClassDef {
val var427 = getSequenceElems(history, 327, listOf(328,7,280,7,65,7,144,7,65,332,7,150), beginGen, endGen)
val var428 = matchSimpleName(var427[4].first, var427[4].second)
val var429 = matchSimpleName(var427[8].first, var427[8].second)
val var430 = unrollRepeat0(history, 332, 334, 9, 333, var427[9].first, var427[9].second).map { k ->
val var431 = getSequenceElems(history, 335, listOf(7,149,7,65), k.first, k.second)
val var432 = matchSimpleName(var431[3].first, var431[3].second)
var432
}
val var433 = SuperClassDef(var428, listOf(var429) + var430, nextId(), beginGen, endGen)
return var433
}

}
