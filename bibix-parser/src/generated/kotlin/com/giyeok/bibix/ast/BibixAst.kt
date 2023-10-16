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
val var47 = getSequenceElems(history, 313, listOf(260,7,314,7,65,7,284,7,181,7,319), beginGen, endGen)
val var48 = matchSimpleName(var47[4].first, var47[4].second)
val var49 = matchParamsDef(var47[6].first, var47[6].second)
val var50 = matchMethodRef(var47[10].first, var47[10].second)
val var51 = ActionRuleDef(var48, var49, var50, nextId(), beginGen, endGen)
return var51
}

fun matchBuildRuleDef(beginGen: Int, endGen: Int): BuildRuleDef {
val var52 = getSequenceElems(history, 363, listOf(314,7,65,7,284,7,161,7,140,7,181,7,319), beginGen, endGen)
val var53 = matchSimpleName(var52[2].first, var52[2].second)
val var54 = matchParamsDef(var52[4].first, var52[4].second)
val var55 = matchTypeExpr(var52[8].first, var52[8].second)
val var56 = matchMethodRef(var52[12].first, var52[12].second)
val var57 = BuildRuleDef(var53, var54, var55, var56, nextId(), beginGen, endGen)
return var57
}

fun matchParamsDef(beginGen: Int, endGen: Int): List<ParamDef> {
val var59 = getSequenceElems(history, 285, listOf(116,286,173,7,117), beginGen, endGen)
val var60 = history[var59[1].second].findByBeginGenOpt(100, 1, var59[1].first)
val var61 = history[var59[1].second].findByBeginGenOpt(287, 1, var59[1].first)
check(hasSingleTrue(var60 != null, var61 != null))
val var62 = when {
var60 != null -> null
else -> {
val var63 = getSequenceElems(history, 288, listOf(7,289,298), var59[1].first, var59[1].second)
val var64 = matchParamDef(var63[1].first, var63[1].second)
val var65 = unrollRepeat0(history, 298, 300, 9, 299, var63[2].first, var63[2].second).map { k ->
val var66 = getSequenceElems(history, 301, listOf(7,148,7,289), k.first, k.second)
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
val var82 = getSequenceElems(history, 270, listOf(181,7,271), beginGen, endGen)
val var83 = matchActionExpr(var82[2].first, var82[2].second)
val var84 = SingleCallAction(var83, nextId(), beginGen, endGen)
var84
}
else -> {
val var85 = getSequenceElems(history, 272, listOf(143,273,7,149), beginGen, endGen)
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
val var105 = getSequenceElems(history, 320, listOf(63,7,161,7,63,321), beginGen, endGen)
val var106 = matchName(var105[0].first, var105[0].second)
val var107 = matchName(var105[4].first, var105[4].second)
val var108 = history[var105[5].second].findByBeginGenOpt(100, 1, var105[5].first)
val var109 = history[var105[5].second].findByBeginGenOpt(322, 1, var105[5].first)
check(hasSingleTrue(var108 != null, var109 != null))
val var110 = when {
var108 != null -> null
else -> {
val var111 = getSequenceElems(history, 323, listOf(7,161,7,65), var105[5].first, var105[5].second)
val var112 = matchSimpleName(var111[3].first, var111[3].second)
var112
}
}
val var113 = MethodRef(var106, var107, var110, nextId(), beginGen, endGen)
return var113
}

fun matchClassBody(beginGen: Int, endGen: Int): List<ClassBodyElem> {
val var114 = getSequenceElems(history, 306, listOf(143,307,7,149), beginGen, endGen)
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
val var123 = getSequenceElems(history, 325, listOf(123,7,140,7,181,7,121), beginGen, endGen)
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
val var158 = history[endGen].findByBeginGenOpt(176, 6, beginGen)
val var159 = history[endGen].findByBeginGenOpt(186, 10, beginGen)
check(hasSingleTrue(var156 != null, var157 != null, var158 != null, var159 != null))
val var160 = when {
var156 != null -> {
val var161 = CallParams(listOf(), listOf(), nextId(), beginGen, endGen)
var161
}
var157 != null -> {
val var162 = getSequenceElems(history, 118, listOf(116,7,119,173,7,117), beginGen, endGen)
val var163 = matchPositionalParams(var162[2].first, var162[2].second)
val var164 = CallParams(var163, listOf(), nextId(), beginGen, endGen)
var164
}
var158 != null -> {
val var165 = getSequenceElems(history, 176, listOf(116,7,177,173,7,117), beginGen, endGen)
val var166 = matchNamedParams(var165[2].first, var165[2].second)
val var167 = CallParams(listOf(), var166, nextId(), beginGen, endGen)
var167
}
else -> {
val var168 = getSequenceElems(history, 186, listOf(116,7,119,7,148,7,177,173,7,117), beginGen, endGen)
val var169 = matchPositionalParams(var168[2].first, var168[2].second)
val var170 = matchNamedParams(var168[6].first, var168[6].second)
val var171 = CallParams(var169, var170, nextId(), beginGen, endGen)
var171
}
}
return var160
}

fun matchPositionalParams(beginGen: Int, endGen: Int): List<Expr> {
val var172 = getSequenceElems(history, 120, listOf(121,169), beginGen, endGen)
val var173 = matchExpr(var172[0].first, var172[0].second)
val var174 = unrollRepeat0(history, 169, 171, 9, 170, var172[1].first, var172[1].second).map { k ->
val var175 = getSequenceElems(history, 172, listOf(7,148,7,121), k.first, k.second)
val var176 = matchExpr(var175[3].first, var175[3].second)
var176
}
return listOf(var173) + var174
}

fun matchNamedParams(beginGen: Int, endGen: Int): List<NamedParam> {
val var177 = getSequenceElems(history, 178, listOf(179,182), beginGen, endGen)
val var178 = matchNamedParam(var177[0].first, var177[0].second)
val var179 = unrollRepeat0(history, 182, 184, 9, 183, var177[1].first, var177[1].second).map { k ->
val var180 = getSequenceElems(history, 185, listOf(7,148,7,179), k.first, k.second)
val var181 = matchNamedParam(var180[3].first, var180[3].second)
var181
}
return listOf(var178) + var179
}

fun matchNamedParam(beginGen: Int, endGen: Int): NamedParam {
val var182 = getSequenceElems(history, 180, listOf(65,7,181,7,121), beginGen, endGen)
val var183 = matchSimpleName(var182[0].first, var182[0].second)
val var184 = matchExpr(var182[4].first, var182[4].second)
val var185 = NamedParam(var183, var184, nextId(), beginGen, endGen)
return var185
}

fun matchDefsWithVarRedefs(beginGen: Int, endGen: Int): DefsWithVarRedefs {
val var186 = getSequenceElems(history, 365, listOf(366,7,354,358,7,143,7,101,7,149), beginGen, endGen)
val var187 = matchVarRedef(var186[2].first, var186[2].second)
val var188 = unrollRepeat0(history, 358, 360, 9, 359, var186[3].first, var186[3].second).map { k ->
val var189 = getSequenceElems(history, 361, listOf(7,148,7,354), k.first, k.second)
val var190 = matchVarRedef(var189[3].first, var189[3].second)
var190
}
val var191 = matchDefs(var186[7].first, var186[7].second)
val var192 = DefsWithVarRedefs(listOf(var187) + var188, var191, nextId(), beginGen, endGen)
return var192
}

fun matchVarRedef(beginGen: Int, endGen: Int): VarRedef {
val var193 = getSequenceElems(history, 355, listOf(65,356,7,181,7,121), beginGen, endGen)
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
val var201 = history[endGen].findByBeginGenOpt(166, 1, beginGen)
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
val var210 = history[endGen].findByBeginGenOpt(155, 1, beginGen)
val var211 = history[endGen].findByBeginGenOpt(157, 1, beginGen)
check(hasSingleTrue(var208 != null, var209 != null, var210 != null, var211 != null))
val var212 = when {
var208 != null -> {
val var213 = matchName(beginGen, endGen)
var213
}
var209 != null -> {
val var214 = matchCollectionType(beginGen, endGen)
var214
}
var210 != null -> {
val var215 = matchTupleType(beginGen, endGen)
var215
}
else -> {
val var216 = matchNamedTupleType(beginGen, endGen)
var216
}
}
return var212
}

fun matchMergeOpOrPrimary(beginGen: Int, endGen: Int): MergeOpOrPrimary {
val var217 = history[endGen].findByBeginGenOpt(111, 1, beginGen)
val var218 = history[endGen].findByBeginGenOpt(167, 5, beginGen)
check(hasSingleTrue(var217 != null, var218 != null))
val var219 = when {
var217 != null -> {
val var220 = matchPrimary(beginGen, endGen)
var220
}
else -> {
val var221 = getSequenceElems(history, 167, listOf(121,7,168,7,111), beginGen, endGen)
val var222 = matchExpr(var221[0].first, var221[0].second)
val var223 = matchPrimary(var221[4].first, var221[4].second)
val var224 = MergeOp(var222, var223, nextId(), beginGen, endGen)
var224
}
}
return var219
}

fun matchCollectionType(beginGen: Int, endGen: Int): CollectionType {
val var225 = getSequenceElems(history, 129, listOf(130,7,137), beginGen, endGen)
val var226 = history[var225[0].second].findByBeginGenOpt(133, 1, var225[0].first)
val var227 = history[var225[0].second].findByBeginGenOpt(135, 1, var225[0].first)
check(hasSingleTrue(var226 != null, var227 != null))
val var228 = when {
var226 != null -> "set"
else -> "list"
}
val var229 = matchTypeParams(var225[2].first, var225[2].second)
val var230 = CollectionType(var228, var229, nextId(), beginGen, endGen)
return var230
}

fun matchTypeParams(beginGen: Int, endGen: Int): TypeParams {
val var231 = getSequenceElems(history, 138, listOf(139,7,140,150,7,154), beginGen, endGen)
val var232 = matchTypeExpr(var231[2].first, var231[2].second)
val var233 = unrollRepeat0(history, 150, 152, 9, 151, var231[3].first, var231[3].second).map { k ->
val var234 = getSequenceElems(history, 153, listOf(7,148,7,140), k.first, k.second)
val var235 = matchTypeExpr(var234[3].first, var234[3].second)
var235
}
val var236 = TypeParams(listOf(var232) + var233, nextId(), beginGen, endGen)
return var236
}

fun matchTupleType(beginGen: Int, endGen: Int): TupleType {
val var237 = getSequenceElems(history, 156, listOf(116,7,140,150,7,117), beginGen, endGen)
val var238 = matchTypeExpr(var237[2].first, var237[2].second)
val var239 = unrollRepeat0(history, 150, 152, 9, 151, var237[3].first, var237[3].second).map { k ->
val var240 = getSequenceElems(history, 153, listOf(7,148,7,140), k.first, k.second)
val var241 = matchTypeExpr(var240[3].first, var240[3].second)
var241
}
val var242 = TupleType(listOf(var238) + var239, nextId(), beginGen, endGen)
return var242
}

fun matchNamedTupleType(beginGen: Int, endGen: Int): NamedTupleType {
val var243 = getSequenceElems(history, 158, listOf(116,7,159,162,7,117), beginGen, endGen)
val var244 = matchNamedType(var243[2].first, var243[2].second)
val var245 = unrollRepeat0(history, 162, 164, 9, 163, var243[3].first, var243[3].second).map { k ->
val var246 = getSequenceElems(history, 165, listOf(7,148,7,159), k.first, k.second)
val var247 = matchNamedType(var246[3].first, var246[3].second)
var247
}
val var248 = NamedTupleType(listOf(var244) + var245, nextId(), beginGen, endGen)
return var248
}

fun matchNamedType(beginGen: Int, endGen: Int): NamedType {
val var249 = getSequenceElems(history, 160, listOf(65,7,161,7,140), beginGen, endGen)
val var250 = matchSimpleName(var249[0].first, var249[0].second)
val var251 = matchTypeExpr(var249[4].first, var249[4].second)
val var252 = NamedType(var250, var251, nextId(), beginGen, endGen)
return var252
}

fun matchVarDef(beginGen: Int, endGen: Int): VarDef {
val var253 = getSequenceElems(history, 343, listOf(344,7,65,349,295), beginGen, endGen)
val var254 = matchSimpleName(var253[2].first, var253[2].second)
val var255 = history[var253[3].second].findByBeginGenOpt(100, 1, var253[3].first)
val var256 = history[var253[3].second].findByBeginGenOpt(350, 1, var253[3].first)
check(hasSingleTrue(var255 != null, var256 != null))
val var257 = when {
var255 != null -> null
else -> {
val var258 = getSequenceElems(history, 351, listOf(7,161,7,140), var253[3].first, var253[3].second)
val var259 = matchTypeExpr(var258[3].first, var258[3].second)
var259
}
}
val var260 = history[var253[4].second].findByBeginGenOpt(100, 1, var253[4].first)
val var261 = history[var253[4].second].findByBeginGenOpt(296, 1, var253[4].first)
check(hasSingleTrue(var260 != null, var261 != null))
val var262 = when {
var260 != null -> null
else -> {
val var263 = getSequenceElems(history, 297, listOf(7,181,7,121), var253[4].first, var253[4].second)
val var264 = matchExpr(var263[3].first, var263[3].second)
var264
}
}
val var265 = VarDef(var254, var257, var262, nextId(), beginGen, endGen)
return var265
}

fun matchNamespaceDef(beginGen: Int, endGen: Int): NamespaceDef {
val var266 = getSequenceElems(history, 253, listOf(65,7,143,254,7,149), beginGen, endGen)
val var267 = matchSimpleName(var266[0].first, var266[0].second)
val var269 = history[var266[3].second].findByBeginGenOpt(100, 1, var266[3].first)
val var270 = history[var266[3].second].findByBeginGenOpt(255, 1, var266[3].first)
check(hasSingleTrue(var269 != null, var270 != null))
val var271 = when {
var269 != null -> null
else -> {
val var272 = getSequenceElems(history, 256, listOf(7,101), var266[3].first, var266[3].second)
val var273 = matchDefs(var272[1].first, var272[1].second)
var273
}
}
val var268 = var271
val var274 = NamespaceDef(var267, (var268 ?: listOf()), nextId(), beginGen, endGen)
return var274
}

fun matchEnumDef(beginGen: Int, endGen: Int): EnumDef {
val var275 = getSequenceElems(history, 337, listOf(338,7,65,7,143,7,65,332,173,7,149), beginGen, endGen)
val var276 = matchSimpleName(var275[2].first, var275[2].second)
val var277 = matchSimpleName(var275[6].first, var275[6].second)
val var278 = unrollRepeat0(history, 332, 334, 9, 333, var275[7].first, var275[7].second).map { k ->
val var279 = getSequenceElems(history, 335, listOf(7,148,7,65), k.first, k.second)
val var280 = matchSimpleName(var279[3].first, var279[3].second)
var280
}
val var281 = EnumDef(var276, listOf(var277) + var278, nextId(), beginGen, endGen)
return var281
}

fun matchPrimary(beginGen: Int, endGen: Int): Primary {
val var282 = history[endGen].findByBeginGenOpt(65, 1, beginGen)
val var283 = history[endGen].findByBeginGenOpt(112, 1, beginGen)
val var284 = history[endGen].findByBeginGenOpt(187, 5, beginGen)
val var285 = history[endGen].findByBeginGenOpt(188, 4, beginGen)
val var286 = history[endGen].findByBeginGenOpt(202, 8, beginGen)
val var287 = history[endGen].findByBeginGenOpt(206, 4, beginGen)
val var288 = history[endGen].findByBeginGenOpt(216, 1, beginGen)
val var289 = history[endGen].findByBeginGenOpt(242, 1, beginGen)
val var290 = history[endGen].findByBeginGenOpt(243, 5, beginGen)
check(hasSingleTrue(var282 != null, var283 != null, var284 != null, var285 != null, var286 != null, var287 != null, var288 != null, var289 != null, var290 != null))
val var291 = when {
var282 != null -> {
val var292 = matchSimpleName(beginGen, endGen)
val var293 = NameRef(var292, nextId(), beginGen, endGen)
var293
}
var283 != null -> {
val var294 = matchCallExpr(beginGen, endGen)
var294
}
var284 != null -> {
val var295 = getSequenceElems(history, 187, listOf(111,7,99,7,65), beginGen, endGen)
val var296 = matchPrimary(var295[0].first, var295[0].second)
val var297 = matchSimpleName(var295[4].first, var295[4].second)
val var298 = MemberAccess(var296, var297, nextId(), beginGen, endGen)
var298
}
var285 != null -> {
val var300 = getSequenceElems(history, 188, listOf(189,190,7,201), beginGen, endGen)
val var301 = history[var300[1].second].findByBeginGenOpt(100, 1, var300[1].first)
val var302 = history[var300[1].second].findByBeginGenOpt(191, 1, var300[1].first)
check(hasSingleTrue(var301 != null, var302 != null))
val var303 = when {
var301 != null -> null
else -> {
val var304 = getSequenceElems(history, 192, listOf(7,193,197,173), var300[1].first, var300[1].second)
val var305 = matchListElem(var304[1].first, var304[1].second)
val var306 = unrollRepeat0(history, 197, 199, 9, 198, var304[2].first, var304[2].second).map { k ->
val var307 = getSequenceElems(history, 200, listOf(7,148,7,193), k.first, k.second)
val var308 = matchListElem(var307[3].first, var307[3].second)
var308
}
listOf(var305) + var306
}
}
val var299 = var303
val var309 = ListExpr((var299 ?: listOf()), nextId(), beginGen, endGen)
var309
}
var286 != null -> {
val var310 = getSequenceElems(history, 202, listOf(116,7,121,7,148,203,7,117), beginGen, endGen)
val var311 = matchExpr(var310[2].first, var310[2].second)
val var313 = history[var310[5].second].findByBeginGenOpt(100, 1, var310[5].first)
val var314 = history[var310[5].second].findByBeginGenOpt(204, 1, var310[5].first)
check(hasSingleTrue(var313 != null, var314 != null))
val var315 = when {
var313 != null -> null
else -> {
val var316 = getSequenceElems(history, 205, listOf(7,121,169,173), var310[5].first, var310[5].second)
val var317 = matchExpr(var316[1].first, var316[1].second)
val var318 = unrollRepeat0(history, 169, 171, 9, 170, var316[2].first, var316[2].second).map { k ->
val var319 = getSequenceElems(history, 172, listOf(7,148,7,121), k.first, k.second)
val var320 = matchExpr(var319[3].first, var319[3].second)
var320
}
listOf(var317) + var318
}
}
val var312 = var315
val var321 = TupleExpr(listOf(var311) + (var312 ?: listOf()), nextId(), beginGen, endGen)
var321
}
var287 != null -> {
val var323 = getSequenceElems(history, 206, listOf(116,207,7,117), beginGen, endGen)
val var324 = history[var323[1].second].findByBeginGenOpt(100, 1, var323[1].first)
val var325 = history[var323[1].second].findByBeginGenOpt(208, 1, var323[1].first)
check(hasSingleTrue(var324 != null, var325 != null))
val var326 = when {
var324 != null -> null
else -> {
val var327 = getSequenceElems(history, 209, listOf(7,210,212,173), var323[1].first, var323[1].second)
val var328 = matchNamedExpr(var327[1].first, var327[1].second)
val var329 = unrollRepeat0(history, 212, 214, 9, 213, var327[2].first, var327[2].second).map { k ->
val var330 = getSequenceElems(history, 215, listOf(7,148,7,210), k.first, k.second)
val var331 = matchNamedExpr(var330[3].first, var330[3].second)
var331
}
listOf(var328) + var329
}
}
val var322 = var326
val var332 = NamedTupleExpr((var322 ?: listOf()), nextId(), beginGen, endGen)
var332
}
var288 != null -> {
val var333 = matchLiteral(beginGen, endGen)
var333
}
var289 != null -> {
val var334 = This(nextId(), beginGen, endGen)
var334
}
else -> {
val var335 = getSequenceElems(history, 243, listOf(116,7,121,7,117), beginGen, endGen)
val var336 = matchExpr(var335[2].first, var335[2].second)
val var337 = Paren(var336, nextId(), beginGen, endGen)
var337
}
}
return var291
}

fun matchLiteral(beginGen: Int, endGen: Int): Literal {
val var338 = history[endGen].findByBeginGenOpt(217, 1, beginGen)
val var339 = history[endGen].findByBeginGenOpt(237, 1, beginGen)
val var340 = history[endGen].findByBeginGenOpt(240, 1, beginGen)
check(hasSingleTrue(var338 != null, var339 != null, var340 != null))
val var341 = when {
var338 != null -> {
val var342 = matchStringLiteral(beginGen, endGen)
var342
}
var339 != null -> {
val var343 = matchBooleanLiteral(beginGen, endGen)
var343
}
else -> {
val var344 = matchNoneLiteral(beginGen, endGen)
var344
}
}
return var341
}

fun matchStringLiteral(beginGen: Int, endGen: Int): StringLiteral {
val var345 = getSequenceElems(history, 218, listOf(219,220,219), beginGen, endGen)
val var346 = unrollRepeat0(history, 220, 222, 9, 221, var345[1].first, var345[1].second).map { k ->
val var347 = matchStringElem(k.first, k.second)
var347
}
val var348 = StringLiteral(var346, nextId(), beginGen, endGen)
return var348
}

fun matchNoneLiteral(beginGen: Int, endGen: Int): NoneLiteral {
val var349 = NoneLiteral(nextId(), beginGen, endGen)
return var349
}

fun matchBooleanLiteral(beginGen: Int, endGen: Int): BooleanLiteral {
val var350 = history[endGen].findByBeginGenOpt(77, 1, beginGen)
val var351 = history[endGen].findByBeginGenOpt(82, 1, beginGen)
check(hasSingleTrue(var350 != null, var351 != null))
val var352 = when {
var350 != null -> {
val var353 = BooleanLiteral(true, nextId(), beginGen, endGen)
var353
}
else -> {
val var354 = BooleanLiteral(false, nextId(), beginGen, endGen)
var354
}
}
return var352
}

fun matchStringElem(beginGen: Int, endGen: Int): StringElem {
val var355 = history[endGen].findByBeginGenOpt(225, 1, beginGen)
val var356 = history[endGen].findByBeginGenOpt(227, 1, beginGen)
val var357 = history[endGen].findByBeginGenOpt(231, 1, beginGen)
check(hasSingleTrue(var355 != null, var356 != null, var357 != null))
val var358 = when {
var355 != null -> {
val var359 = JustChar(source[beginGen], nextId(), beginGen, endGen)
var359
}
var356 != null -> {
val var360 = matchEscapeChar(beginGen, endGen)
var360
}
else -> {
val var361 = matchStringExpr(beginGen, endGen)
var361
}
}
return var358
}

fun matchEscapeChar(beginGen: Int, endGen: Int): EscapeChar {
val var362 = getSequenceElems(history, 228, listOf(229,230), beginGen, endGen)
val var363 = EscapeChar(source[var362[1].first], nextId(), beginGen, endGen)
return var363
}

fun matchStringExpr(beginGen: Int, endGen: Int): StringExpr {
val var364 = history[endGen].findByBeginGenOpt(232, 1, beginGen)
val var365 = history[endGen].findByBeginGenOpt(236, 6, beginGen)
check(hasSingleTrue(var364 != null, var365 != null))
val var366 = when {
var364 != null -> {
val var367 = getSequenceElems(history, 234, listOf(235,65), beginGen, endGen)
val var368 = matchSimpleName(var367[1].first, var367[1].second)
val var369 = SimpleExpr(var368, nextId(), beginGen, endGen)
var369
}
else -> {
val var370 = getSequenceElems(history, 236, listOf(235,143,7,121,7,149), beginGen, endGen)
val var371 = matchExpr(var370[3].first, var370[3].second)
val var372 = ComplexExpr(var371, nextId(), beginGen, endGen)
var372
}
}
return var366
}

fun matchNamedExpr(beginGen: Int, endGen: Int): NamedExpr {
val var373 = getSequenceElems(history, 211, listOf(65,7,161,7,121), beginGen, endGen)
val var374 = matchSimpleName(var373[0].first, var373[0].second)
val var375 = matchExpr(var373[4].first, var373[4].second)
val var376 = NamedExpr(var374, var375, nextId(), beginGen, endGen)
return var376
}

fun matchVarRedefs(beginGen: Int, endGen: Int): VarRedefs {
val var377 = getSequenceElems(history, 353, listOf(344,7,354,358), beginGen, endGen)
val var378 = matchVarRedef(var377[2].first, var377[2].second)
val var379 = unrollRepeat0(history, 358, 360, 9, 359, var377[3].first, var377[3].second).map { k ->
val var380 = getSequenceElems(history, 361, listOf(7,148,7,354), k.first, k.second)
val var381 = matchVarRedef(var380[3].first, var380[3].second)
var381
}
val var382 = VarRedefs(listOf(var378) + var379, nextId(), beginGen, endGen)
return var382
}

fun matchTypeExpr(beginGen: Int, endGen: Int): TypeExpr {
val var383 = history[endGen].findByBeginGenOpt(127, 1, beginGen)
val var384 = history[endGen].findByBeginGenOpt(141, 1, beginGen)
check(hasSingleTrue(var383 != null, var384 != null))
val var385 = when {
var383 != null -> {
val var386 = matchNoUnionType(beginGen, endGen)
var386
}
else -> {
val var387 = matchUnionType(beginGen, endGen)
var387
}
}
return var385
}

fun matchListElem(beginGen: Int, endGen: Int): ListElem {
val var388 = history[endGen].findByBeginGenOpt(121, 1, beginGen)
val var389 = history[endGen].findByBeginGenOpt(194, 3, beginGen)
check(hasSingleTrue(var388 != null, var389 != null))
val var390 = when {
var388 != null -> {
val var391 = matchExpr(beginGen, endGen)
var391
}
else -> {
val var392 = getSequenceElems(history, 194, listOf(195,7,121), beginGen, endGen)
val var393 = matchExpr(var392[2].first, var392[2].second)
val var394 = EllipsisElem(var393, nextId(), beginGen, endGen)
var394
}
}
return var390
}

fun matchParamDef(beginGen: Int, endGen: Int): ParamDef {
val var395 = history[endGen].findByBeginGenOpt(180, 5, beginGen)
val var396 = history[endGen].findByBeginGenOpt(290, 7, beginGen)
check(hasSingleTrue(var395 != null, var396 != null))
val var397 = when {
var395 != null -> {
val var398 = getSequenceElems(history, 180, listOf(65,7,181,7,121), beginGen, endGen)
val var399 = matchSimpleName(var398[0].first, var398[0].second)
val var400 = matchExpr(var398[4].first, var398[4].second)
val var401 = ParamDef(var399, false, null, var400, nextId(), beginGen, endGen)
var401
}
else -> {
val var402 = getSequenceElems(history, 290, listOf(65,291,7,161,7,140,295), beginGen, endGen)
val var403 = matchSimpleName(var402[0].first, var402[0].second)
val var404 = history[var402[1].second].findByBeginGenOpt(100, 1, var402[1].first)
val var405 = history[var402[1].second].findByBeginGenOpt(292, 1, var402[1].first)
check(hasSingleTrue(var404 != null, var405 != null))
val var406 = when {
var404 != null -> null
else -> {
val var407 = getSequenceElems(history, 293, listOf(7,294), var402[1].first, var402[1].second)
source[var407[1].first]
}
}
val var408 = matchTypeExpr(var402[5].first, var402[5].second)
val var409 = history[var402[6].second].findByBeginGenOpt(100, 1, var402[6].first)
val var410 = history[var402[6].second].findByBeginGenOpt(296, 1, var402[6].first)
check(hasSingleTrue(var409 != null, var410 != null))
val var411 = when {
var409 != null -> null
else -> {
val var412 = getSequenceElems(history, 297, listOf(7,181,7,121), var402[6].first, var402[6].second)
val var413 = matchExpr(var412[3].first, var412[3].second)
var413
}
}
val var414 = ParamDef(var403, var406 != null, var408, var411, nextId(), beginGen, endGen)
var414
}
}
return var397
}

fun matchUnionType(beginGen: Int, endGen: Int): UnionType {
val var415 = getSequenceElems(history, 142, listOf(143,7,127,144,7,149), beginGen, endGen)
val var416 = matchNoUnionType(var415[2].first, var415[2].second)
val var417 = unrollRepeat0(history, 144, 146, 9, 145, var415[3].first, var415[3].second).map { k ->
val var418 = getSequenceElems(history, 147, listOf(7,148,7,127), k.first, k.second)
val var419 = matchNoUnionType(var418[3].first, var418[3].second)
var419
}
val var420 = UnionType(listOf(var416) + var417, nextId(), beginGen, endGen)
return var420
}

fun matchTargetDef(beginGen: Int, endGen: Int): TargetDef {
val var421 = getSequenceElems(history, 180, listOf(65,7,181,7,121), beginGen, endGen)
val var422 = matchSimpleName(var421[0].first, var421[0].second)
val var423 = matchExpr(var421[4].first, var421[4].second)
val var424 = TargetDef(var422, var423, nextId(), beginGen, endGen)
return var424
}

fun matchSuperClassDef(beginGen: Int, endGen: Int): SuperClassDef {
val var425 = getSequenceElems(history, 327, listOf(328,7,280,7,65,7,143,7,65,332,7,149), beginGen, endGen)
val var426 = matchSimpleName(var425[4].first, var425[4].second)
val var427 = matchSimpleName(var425[8].first, var425[8].second)
val var428 = unrollRepeat0(history, 332, 334, 9, 333, var425[9].first, var425[9].second).map { k ->
val var429 = getSequenceElems(history, 335, listOf(7,148,7,65), k.first, k.second)
val var430 = matchSimpleName(var429[3].first, var429[3].second)
var430
}
val var431 = SuperClassDef(var426, listOf(var427) + var428, nextId(), beginGen, endGen)
return var431
}

}
