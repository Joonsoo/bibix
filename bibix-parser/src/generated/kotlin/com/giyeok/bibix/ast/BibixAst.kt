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
  val mods: List<BuildRuleMod>,
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
  val expr: ActionStmt,
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
): ActionStmt, Primary, AstNode

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
  val exprs: List<ActionStmt>,
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

data class LetStmt(
  val name: String,
  val expr: Expr,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): ActionStmt, AstNode

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

sealed interface ActionStmt: AstNode

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
enum class BuildRuleMod { Singleton, Synchronized }

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
val var11 = getSequenceElems(history, 102, listOf(103,384), beginGen, endGen)
val var12 = matchDef(var11[0].first, var11[0].second)
val var13 = unrollRepeat0(history, 384, 386, 9, 385, var11[1].first, var11[1].second).map { k ->
val var14 = getSequenceElems(history, 387, listOf(7,103), k.first, k.second)
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
val var28 = history[endGen].findByBeginGenOpt(285, 1, beginGen)
val var29 = history[endGen].findByBeginGenOpt(320, 1, beginGen)
val var30 = history[endGen].findByBeginGenOpt(344, 1, beginGen)
val var31 = history[endGen].findByBeginGenOpt(350, 1, beginGen)
val var32 = history[endGen].findByBeginGenOpt(360, 1, beginGen)
val var33 = history[endGen].findByBeginGenOpt(370, 1, beginGen)
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
val var45 = getSequenceElems(history, 321, listOf(260,7,322,7,65,7,292,7,181,7,327), beginGen, endGen)
val var46 = matchSimpleName(var45[4].first, var45[4].second)
val var47 = matchParamsDef(var45[6].first, var45[6].second)
val var48 = matchMethodRef(var45[10].first, var45[10].second)
val var49 = ActionRuleDef(var46, var47, var48, nextId(), beginGen, endGen)
return var49
}

fun matchBuildRuleDef(beginGen: Int, endGen: Int): BuildRuleDef {
val var50 = getSequenceElems(history, 371, listOf(372,322,7,65,7,292,7,161,7,140,7,181,7,327), beginGen, endGen)
val var51 = unrollRepeat0(history, 372, 374, 9, 373, var50[0].first, var50[0].second).map { k ->
val var52 = getSequenceElems(history, 375, listOf(376,7), k.first, k.second)
val var53 = matchBuildRuleMod(var52[0].first, var52[0].second)
var53
}
val var54 = matchSimpleName(var50[3].first, var50[3].second)
val var55 = matchParamsDef(var50[5].first, var50[5].second)
val var56 = matchTypeExpr(var50[9].first, var50[9].second)
val var57 = matchMethodRef(var50[13].first, var50[13].second)
val var58 = BuildRuleDef(var51, var54, var55, var56, var57, nextId(), beginGen, endGen)
return var58
}

fun matchImportDef(beginGen: Int, endGen: Int): ImportDef {
val var59 = history[endGen].findByBeginGenOpt(105, 4, beginGen)
val var60 = history[endGen].findByBeginGenOpt(247, 8, beginGen)
check(hasSingleTrue(var59 != null, var60 != null))
val var61 = when {
var59 != null -> {
val var62 = getSequenceElems(history, 105, listOf(106,7,111,244), beginGen, endGen)
val var63 = matchPrimary(var62[2].first, var62[2].second)
val var64 = history[var62[3].second].findByBeginGenOpt(100, 1, var62[3].first)
val var65 = history[var62[3].second].findByBeginGenOpt(245, 1, var62[3].first)
check(hasSingleTrue(var64 != null, var65 != null))
val var66 = when {
var64 != null -> null
else -> {
val var67 = getSequenceElems(history, 246, listOf(7,123,7,65), var62[3].first, var62[3].second)
val var68 = matchSimpleName(var67[3].first, var67[3].second)
var68
}
}
val var69 = ImportAll(var63, var66, nextId(), beginGen, endGen)
var69
}
else -> {
val var70 = getSequenceElems(history, 247, listOf(248,7,121,7,106,7,63,244), beginGen, endGen)
val var71 = matchExpr(var70[2].first, var70[2].second)
val var72 = matchName(var70[6].first, var70[6].second)
val var73 = history[var70[7].second].findByBeginGenOpt(100, 1, var70[7].first)
val var74 = history[var70[7].second].findByBeginGenOpt(245, 1, var70[7].first)
check(hasSingleTrue(var73 != null, var74 != null))
val var75 = when {
var73 != null -> null
else -> {
val var76 = getSequenceElems(history, 246, listOf(7,123,7,65), var70[7].first, var70[7].second)
val var77 = matchSimpleName(var76[3].first, var76[3].second)
var77
}
}
val var78 = ImportFrom(var71, var72, var75, nextId(), beginGen, endGen)
var78
}
}
return var61
}

fun matchExpr(beginGen: Int, endGen: Int): Expr {
val var79 = history[endGen].findByBeginGenOpt(122, 5, beginGen)
val var80 = history[endGen].findByBeginGenOpt(166, 1, beginGen)
check(hasSingleTrue(var79 != null, var80 != null))
val var81 = when {
var79 != null -> {
val var82 = getSequenceElems(history, 122, listOf(121,7,123,7,127), beginGen, endGen)
val var83 = matchExpr(var82[0].first, var82[0].second)
val var84 = matchNoUnionType(var82[4].first, var82[4].second)
val var85 = CastExpr(var83, var84, nextId(), beginGen, endGen)
var85
}
else -> {
val var86 = matchMergeOpOrPrimary(beginGen, endGen)
var86
}
}
return var81
}

fun matchNoUnionType(beginGen: Int, endGen: Int): NoUnionType {
val var87 = history[endGen].findByBeginGenOpt(63, 1, beginGen)
val var88 = history[endGen].findByBeginGenOpt(128, 1, beginGen)
val var89 = history[endGen].findByBeginGenOpt(155, 1, beginGen)
val var90 = history[endGen].findByBeginGenOpt(157, 1, beginGen)
check(hasSingleTrue(var87 != null, var88 != null, var89 != null, var90 != null))
val var91 = when {
var87 != null -> {
val var92 = matchName(beginGen, endGen)
var92
}
var88 != null -> {
val var93 = matchCollectionType(beginGen, endGen)
var93
}
var89 != null -> {
val var94 = matchTupleType(beginGen, endGen)
var94
}
else -> {
val var95 = matchNamedTupleType(beginGen, endGen)
var95
}
}
return var91
}

fun matchMergeOpOrPrimary(beginGen: Int, endGen: Int): MergeOpOrPrimary {
val var96 = history[endGen].findByBeginGenOpt(111, 1, beginGen)
val var97 = history[endGen].findByBeginGenOpt(167, 5, beginGen)
check(hasSingleTrue(var96 != null, var97 != null))
val var98 = when {
var96 != null -> {
val var99 = matchPrimary(beginGen, endGen)
var99
}
else -> {
val var100 = getSequenceElems(history, 167, listOf(121,7,168,7,111), beginGen, endGen)
val var101 = matchExpr(var100[0].first, var100[0].second)
val var102 = matchPrimary(var100[4].first, var100[4].second)
val var103 = MergeOp(var101, var102, nextId(), beginGen, endGen)
var103
}
}
return var98
}

fun matchCollectionType(beginGen: Int, endGen: Int): CollectionType {
val var104 = getSequenceElems(history, 129, listOf(130,7,137), beginGen, endGen)
val var105 = history[var104[0].second].findByBeginGenOpt(133, 1, var104[0].first)
val var106 = history[var104[0].second].findByBeginGenOpt(135, 1, var104[0].first)
check(hasSingleTrue(var105 != null, var106 != null))
val var107 = when {
var105 != null -> "set"
else -> "list"
}
val var108 = matchTypeParams(var104[2].first, var104[2].second)
val var109 = CollectionType(var107, var108, nextId(), beginGen, endGen)
return var109
}

fun matchParamsDef(beginGen: Int, endGen: Int): List<ParamDef> {
val var111 = getSequenceElems(history, 293, listOf(116,294,173,7,117), beginGen, endGen)
val var112 = history[var111[1].second].findByBeginGenOpt(100, 1, var111[1].first)
val var113 = history[var111[1].second].findByBeginGenOpt(295, 1, var111[1].first)
check(hasSingleTrue(var112 != null, var113 != null))
val var114 = when {
var112 != null -> null
else -> {
val var115 = getSequenceElems(history, 296, listOf(7,297,306), var111[1].first, var111[1].second)
val var116 = matchParamDef(var115[1].first, var115[1].second)
val var117 = unrollRepeat0(history, 306, 308, 9, 307, var115[2].first, var115[2].second).map { k ->
val var118 = getSequenceElems(history, 309, listOf(7,148,7,297), k.first, k.second)
val var119 = matchParamDef(var118[3].first, var118[3].second)
var119
}
listOf(var116) + var117
}
}
val var110 = var114
return (var110 ?: listOf())
}

fun matchBuildRuleMod(beginGen: Int, endGen: Int): BuildRuleMod {
val var120 = history[endGen].findByBeginGenOpt(379, 1, beginGen)
val var121 = history[endGen].findByBeginGenOpt(381, 1, beginGen)
check(hasSingleTrue(var120 != null, var121 != null))
val var122 = when {
var120 != null -> BuildRuleMod.Singleton
else -> BuildRuleMod.Synchronized
}
return var122
}

fun matchTypeParams(beginGen: Int, endGen: Int): TypeParams {
val var123 = getSequenceElems(history, 138, listOf(139,7,140,150,7,154), beginGen, endGen)
val var124 = matchTypeExpr(var123[2].first, var123[2].second)
val var125 = unrollRepeat0(history, 150, 152, 9, 151, var123[3].first, var123[3].second).map { k ->
val var126 = getSequenceElems(history, 153, listOf(7,148,7,140), k.first, k.second)
val var127 = matchTypeExpr(var126[3].first, var126[3].second)
var127
}
val var128 = TypeParams(listOf(var124) + var125, nextId(), beginGen, endGen)
return var128
}

fun matchActionDef(beginGen: Int, endGen: Int): ActionDef {
val var129 = getSequenceElems(history, 259, listOf(260,7,65,264,7,269), beginGen, endGen)
val var130 = matchSimpleName(var129[2].first, var129[2].second)
val var131 = history[var129[3].second].findByBeginGenOpt(100, 1, var129[3].first)
val var132 = history[var129[3].second].findByBeginGenOpt(265, 1, var129[3].first)
check(hasSingleTrue(var131 != null, var132 != null))
val var133 = when {
var131 != null -> null
else -> {
val var134 = getSequenceElems(history, 266, listOf(7,267), var129[3].first, var129[3].second)
val var135 = matchActionParams(var134[1].first, var134[1].second)
var135
}
}
val var136 = matchActionBody(var129[5].first, var129[5].second)
val var137 = ActionDef(var130, var133, var136, nextId(), beginGen, endGen)
return var137
}

fun matchActionParams(beginGen: Int, endGen: Int): String {
val var138 = getSequenceElems(history, 268, listOf(116,7,65,7,117), beginGen, endGen)
val var139 = matchSimpleName(var138[2].first, var138[2].second)
return var139
}

fun matchActionBody(beginGen: Int, endGen: Int): ActionBody {
val var140 = history[endGen].findByBeginGenOpt(270, 3, beginGen)
val var141 = history[endGen].findByBeginGenOpt(280, 4, beginGen)
check(hasSingleTrue(var140 != null, var141 != null))
val var142 = when {
var140 != null -> {
val var143 = getSequenceElems(history, 270, listOf(181,7,271), beginGen, endGen)
val var144 = matchActionStmt(var143[2].first, var143[2].second)
val var145 = SingleCallAction(var144, nextId(), beginGen, endGen)
var145
}
else -> {
val var146 = getSequenceElems(history, 280, listOf(143,281,7,149), beginGen, endGen)
val var147 = unrollRepeat1(history, 281, 282, 282, 284, var146[1].first, var146[1].second).map { k ->
val var148 = getSequenceElems(history, 283, listOf(7,271), k.first, k.second)
val var149 = matchActionStmt(var148[1].first, var148[1].second)
var149
}
val var150 = MultiCallActions(var147, nextId(), beginGen, endGen)
var150
}
}
return var142
}

fun matchTupleType(beginGen: Int, endGen: Int): TupleType {
val var151 = getSequenceElems(history, 156, listOf(116,7,140,150,7,117), beginGen, endGen)
val var152 = matchTypeExpr(var151[2].first, var151[2].second)
val var153 = unrollRepeat0(history, 150, 152, 9, 151, var151[3].first, var151[3].second).map { k ->
val var154 = getSequenceElems(history, 153, listOf(7,148,7,140), k.first, k.second)
val var155 = matchTypeExpr(var154[3].first, var154[3].second)
var155
}
val var156 = TupleType(listOf(var152) + var153, nextId(), beginGen, endGen)
return var156
}

fun matchClassDef(beginGen: Int, endGen: Int): ClassDef {
val var157 = history[endGen].findByBeginGenOpt(286, 1, beginGen)
val var158 = history[endGen].findByBeginGenOpt(334, 1, beginGen)
check(hasSingleTrue(var157 != null, var158 != null))
val var159 = when {
var157 != null -> {
val var160 = matchDataClassDef(beginGen, endGen)
var160
}
else -> {
val var161 = matchSuperClassDef(beginGen, endGen)
var161
}
}
return var159
}

fun matchDataClassDef(beginGen: Int, endGen: Int): DataClassDef {
val var162 = getSequenceElems(history, 287, listOf(288,7,65,7,292,310), beginGen, endGen)
val var163 = matchSimpleName(var162[2].first, var162[2].second)
val var164 = matchParamsDef(var162[4].first, var162[4].second)
val var166 = history[var162[5].second].findByBeginGenOpt(100, 1, var162[5].first)
val var167 = history[var162[5].second].findByBeginGenOpt(311, 1, var162[5].first)
check(hasSingleTrue(var166 != null, var167 != null))
val var168 = when {
var166 != null -> null
else -> {
val var169 = getSequenceElems(history, 312, listOf(7,313), var162[5].first, var162[5].second)
val var170 = matchClassBody(var169[1].first, var169[1].second)
var170
}
}
val var165 = var168
val var171 = DataClassDef(var163, var164, (var165 ?: listOf()), nextId(), beginGen, endGen)
return var171
}

fun matchMethodRef(beginGen: Int, endGen: Int): MethodRef {
val var172 = getSequenceElems(history, 328, listOf(63,7,161,7,63,329), beginGen, endGen)
val var173 = matchName(var172[0].first, var172[0].second)
val var174 = matchName(var172[4].first, var172[4].second)
val var175 = history[var172[5].second].findByBeginGenOpt(100, 1, var172[5].first)
val var176 = history[var172[5].second].findByBeginGenOpt(330, 1, var172[5].first)
check(hasSingleTrue(var175 != null, var176 != null))
val var177 = when {
var175 != null -> null
else -> {
val var178 = getSequenceElems(history, 331, listOf(7,161,7,65), var172[5].first, var172[5].second)
val var179 = matchSimpleName(var178[3].first, var178[3].second)
var179
}
}
val var180 = MethodRef(var173, var174, var177, nextId(), beginGen, endGen)
return var180
}

fun matchClassBody(beginGen: Int, endGen: Int): List<ClassBodyElem> {
val var181 = getSequenceElems(history, 314, listOf(143,315,7,149), beginGen, endGen)
val var182 = unrollRepeat0(history, 315, 317, 9, 316, var181[1].first, var181[1].second).map { k ->
val var183 = getSequenceElems(history, 318, listOf(7,319), k.first, k.second)
val var184 = matchClassBodyElem(var183[1].first, var183[1].second)
var184
}
return var182
}

fun matchClassBodyElem(beginGen: Int, endGen: Int): ClassBodyElem {
val var185 = history[endGen].findByBeginGenOpt(320, 1, beginGen)
val var186 = history[endGen].findByBeginGenOpt(332, 1, beginGen)
check(hasSingleTrue(var185 != null, var186 != null))
val var187 = when {
var185 != null -> {
val var188 = matchActionRuleDef(beginGen, endGen)
var188
}
else -> {
val var189 = matchClassCastDef(beginGen, endGen)
var189
}
}
return var187
}

fun matchClassCastDef(beginGen: Int, endGen: Int): ClassCastDef {
val var190 = getSequenceElems(history, 333, listOf(123,7,140,7,181,7,121), beginGen, endGen)
val var191 = matchTypeExpr(var190[2].first, var190[2].second)
val var192 = matchExpr(var190[6].first, var190[6].second)
val var193 = ClassCastDef(var191, var192, nextId(), beginGen, endGen)
return var193
}

fun matchVarDef(beginGen: Int, endGen: Int): VarDef {
val var194 = getSequenceElems(history, 351, listOf(352,7,65,357,303), beginGen, endGen)
val var195 = matchSimpleName(var194[2].first, var194[2].second)
val var196 = history[var194[3].second].findByBeginGenOpt(100, 1, var194[3].first)
val var197 = history[var194[3].second].findByBeginGenOpt(358, 1, var194[3].first)
check(hasSingleTrue(var196 != null, var197 != null))
val var198 = when {
var196 != null -> null
else -> {
val var199 = getSequenceElems(history, 359, listOf(7,161,7,140), var194[3].first, var194[3].second)
val var200 = matchTypeExpr(var199[3].first, var199[3].second)
var200
}
}
val var201 = history[var194[4].second].findByBeginGenOpt(100, 1, var194[4].first)
val var202 = history[var194[4].second].findByBeginGenOpt(304, 1, var194[4].first)
check(hasSingleTrue(var201 != null, var202 != null))
val var203 = when {
var201 != null -> null
else -> {
val var204 = getSequenceElems(history, 305, listOf(7,181,7,121), var194[4].first, var194[4].second)
val var205 = matchExpr(var204[3].first, var204[3].second)
var205
}
}
val var206 = VarDef(var195, var198, var203, nextId(), beginGen, endGen)
return var206
}

fun matchNamespaceDef(beginGen: Int, endGen: Int): NamespaceDef {
val var207 = getSequenceElems(history, 253, listOf(65,7,143,254,7,149), beginGen, endGen)
val var208 = matchSimpleName(var207[0].first, var207[0].second)
val var210 = history[var207[3].second].findByBeginGenOpt(100, 1, var207[3].first)
val var211 = history[var207[3].second].findByBeginGenOpt(255, 1, var207[3].first)
check(hasSingleTrue(var210 != null, var211 != null))
val var212 = when {
var210 != null -> null
else -> {
val var213 = getSequenceElems(history, 256, listOf(7,101), var207[3].first, var207[3].second)
val var214 = matchDefs(var213[1].first, var213[1].second)
var214
}
}
val var209 = var212
val var215 = NamespaceDef(var208, (var209 ?: listOf()), nextId(), beginGen, endGen)
return var215
}

fun matchEnumDef(beginGen: Int, endGen: Int): EnumDef {
val var216 = getSequenceElems(history, 345, listOf(346,7,65,7,143,7,65,340,173,7,149), beginGen, endGen)
val var217 = matchSimpleName(var216[2].first, var216[2].second)
val var218 = matchSimpleName(var216[6].first, var216[6].second)
val var219 = unrollRepeat0(history, 340, 342, 9, 341, var216[7].first, var216[7].second).map { k ->
val var220 = getSequenceElems(history, 343, listOf(7,148,7,65), k.first, k.second)
val var221 = matchSimpleName(var220[3].first, var220[3].second)
var221
}
val var222 = EnumDef(var217, listOf(var218) + var219, nextId(), beginGen, endGen)
return var222
}

fun matchPrimary(beginGen: Int, endGen: Int): Primary {
val var223 = history[endGen].findByBeginGenOpt(65, 1, beginGen)
val var224 = history[endGen].findByBeginGenOpt(112, 1, beginGen)
val var225 = history[endGen].findByBeginGenOpt(187, 5, beginGen)
val var226 = history[endGen].findByBeginGenOpt(188, 4, beginGen)
val var227 = history[endGen].findByBeginGenOpt(202, 8, beginGen)
val var228 = history[endGen].findByBeginGenOpt(206, 4, beginGen)
val var229 = history[endGen].findByBeginGenOpt(216, 1, beginGen)
val var230 = history[endGen].findByBeginGenOpt(242, 1, beginGen)
val var231 = history[endGen].findByBeginGenOpt(243, 5, beginGen)
check(hasSingleTrue(var223 != null, var224 != null, var225 != null, var226 != null, var227 != null, var228 != null, var229 != null, var230 != null, var231 != null))
val var232 = when {
var223 != null -> {
val var233 = matchSimpleName(beginGen, endGen)
val var234 = NameRef(var233, nextId(), beginGen, endGen)
var234
}
var224 != null -> {
val var235 = matchCallExpr(beginGen, endGen)
var235
}
var225 != null -> {
val var236 = getSequenceElems(history, 187, listOf(111,7,99,7,65), beginGen, endGen)
val var237 = matchPrimary(var236[0].first, var236[0].second)
val var238 = matchSimpleName(var236[4].first, var236[4].second)
val var239 = MemberAccess(var237, var238, nextId(), beginGen, endGen)
var239
}
var226 != null -> {
val var241 = getSequenceElems(history, 188, listOf(189,190,7,201), beginGen, endGen)
val var242 = history[var241[1].second].findByBeginGenOpt(100, 1, var241[1].first)
val var243 = history[var241[1].second].findByBeginGenOpt(191, 1, var241[1].first)
check(hasSingleTrue(var242 != null, var243 != null))
val var244 = when {
var242 != null -> null
else -> {
val var245 = getSequenceElems(history, 192, listOf(7,193,197,173), var241[1].first, var241[1].second)
val var246 = matchListElem(var245[1].first, var245[1].second)
val var247 = unrollRepeat0(history, 197, 199, 9, 198, var245[2].first, var245[2].second).map { k ->
val var248 = getSequenceElems(history, 200, listOf(7,148,7,193), k.first, k.second)
val var249 = matchListElem(var248[3].first, var248[3].second)
var249
}
listOf(var246) + var247
}
}
val var240 = var244
val var250 = ListExpr((var240 ?: listOf()), nextId(), beginGen, endGen)
var250
}
var227 != null -> {
val var251 = getSequenceElems(history, 202, listOf(116,7,121,7,148,203,7,117), beginGen, endGen)
val var252 = matchExpr(var251[2].first, var251[2].second)
val var254 = history[var251[5].second].findByBeginGenOpt(100, 1, var251[5].first)
val var255 = history[var251[5].second].findByBeginGenOpt(204, 1, var251[5].first)
check(hasSingleTrue(var254 != null, var255 != null))
val var256 = when {
var254 != null -> null
else -> {
val var257 = getSequenceElems(history, 205, listOf(7,121,169,173), var251[5].first, var251[5].second)
val var258 = matchExpr(var257[1].first, var257[1].second)
val var259 = unrollRepeat0(history, 169, 171, 9, 170, var257[2].first, var257[2].second).map { k ->
val var260 = getSequenceElems(history, 172, listOf(7,148,7,121), k.first, k.second)
val var261 = matchExpr(var260[3].first, var260[3].second)
var261
}
listOf(var258) + var259
}
}
val var253 = var256
val var262 = TupleExpr(listOf(var252) + (var253 ?: listOf()), nextId(), beginGen, endGen)
var262
}
var228 != null -> {
val var264 = getSequenceElems(history, 206, listOf(116,207,7,117), beginGen, endGen)
val var265 = history[var264[1].second].findByBeginGenOpt(100, 1, var264[1].first)
val var266 = history[var264[1].second].findByBeginGenOpt(208, 1, var264[1].first)
check(hasSingleTrue(var265 != null, var266 != null))
val var267 = when {
var265 != null -> null
else -> {
val var268 = getSequenceElems(history, 209, listOf(7,210,212,173), var264[1].first, var264[1].second)
val var269 = matchNamedExpr(var268[1].first, var268[1].second)
val var270 = unrollRepeat0(history, 212, 214, 9, 213, var268[2].first, var268[2].second).map { k ->
val var271 = getSequenceElems(history, 215, listOf(7,148,7,210), k.first, k.second)
val var272 = matchNamedExpr(var271[3].first, var271[3].second)
var272
}
listOf(var269) + var270
}
}
val var263 = var267
val var273 = NamedTupleExpr((var263 ?: listOf()), nextId(), beginGen, endGen)
var273
}
var229 != null -> {
val var274 = matchLiteral(beginGen, endGen)
var274
}
var230 != null -> {
val var275 = This(nextId(), beginGen, endGen)
var275
}
else -> {
val var276 = getSequenceElems(history, 243, listOf(116,7,121,7,117), beginGen, endGen)
val var277 = matchExpr(var276[2].first, var276[2].second)
val var278 = Paren(var277, nextId(), beginGen, endGen)
var278
}
}
return var232
}

fun matchLiteral(beginGen: Int, endGen: Int): Literal {
val var279 = history[endGen].findByBeginGenOpt(217, 1, beginGen)
val var280 = history[endGen].findByBeginGenOpt(237, 1, beginGen)
val var281 = history[endGen].findByBeginGenOpt(240, 1, beginGen)
check(hasSingleTrue(var279 != null, var280 != null, var281 != null))
val var282 = when {
var279 != null -> {
val var283 = matchStringLiteral(beginGen, endGen)
var283
}
var280 != null -> {
val var284 = matchBooleanLiteral(beginGen, endGen)
var284
}
else -> {
val var285 = matchNoneLiteral(beginGen, endGen)
var285
}
}
return var282
}

fun matchStringLiteral(beginGen: Int, endGen: Int): StringLiteral {
val var286 = getSequenceElems(history, 218, listOf(219,220,219), beginGen, endGen)
val var287 = unrollRepeat0(history, 220, 222, 9, 221, var286[1].first, var286[1].second).map { k ->
val var288 = matchStringElem(k.first, k.second)
var288
}
val var289 = StringLiteral(var287, nextId(), beginGen, endGen)
return var289
}

fun matchCallExpr(beginGen: Int, endGen: Int): CallExpr {
val var290 = getSequenceElems(history, 113, listOf(63,7,114), beginGen, endGen)
val var291 = matchName(var290[0].first, var290[0].second)
val var292 = matchCallParams(var290[2].first, var290[2].second)
val var293 = CallExpr(var291, var292, nextId(), beginGen, endGen)
return var293
}

fun matchNoneLiteral(beginGen: Int, endGen: Int): NoneLiteral {
val var294 = NoneLiteral(nextId(), beginGen, endGen)
return var294
}

fun matchStringElem(beginGen: Int, endGen: Int): StringElem {
val var295 = history[endGen].findByBeginGenOpt(225, 1, beginGen)
val var296 = history[endGen].findByBeginGenOpt(227, 1, beginGen)
val var297 = history[endGen].findByBeginGenOpt(231, 1, beginGen)
check(hasSingleTrue(var295 != null, var296 != null, var297 != null))
val var298 = when {
var295 != null -> {
val var299 = JustChar(source[beginGen], nextId(), beginGen, endGen)
var299
}
var296 != null -> {
val var300 = matchEscapeChar(beginGen, endGen)
var300
}
else -> {
val var301 = matchStringExpr(beginGen, endGen)
var301
}
}
return var298
}

fun matchEscapeChar(beginGen: Int, endGen: Int): EscapeChar {
val var302 = getSequenceElems(history, 228, listOf(229,230), beginGen, endGen)
val var303 = EscapeChar(source[var302[1].first], nextId(), beginGen, endGen)
return var303
}

fun matchStringExpr(beginGen: Int, endGen: Int): StringExpr {
val var304 = history[endGen].findByBeginGenOpt(232, 1, beginGen)
val var305 = history[endGen].findByBeginGenOpt(236, 6, beginGen)
check(hasSingleTrue(var304 != null, var305 != null))
val var306 = when {
var304 != null -> {
val var307 = getSequenceElems(history, 234, listOf(235,65), beginGen, endGen)
val var308 = matchSimpleName(var307[1].first, var307[1].second)
val var309 = SimpleExpr(var308, nextId(), beginGen, endGen)
var309
}
else -> {
val var310 = getSequenceElems(history, 236, listOf(235,143,7,121,7,149), beginGen, endGen)
val var311 = matchExpr(var310[3].first, var310[3].second)
val var312 = ComplexExpr(var311, nextId(), beginGen, endGen)
var312
}
}
return var306
}

fun matchNamedExpr(beginGen: Int, endGen: Int): NamedExpr {
val var313 = getSequenceElems(history, 211, listOf(65,7,161,7,121), beginGen, endGen)
val var314 = matchSimpleName(var313[0].first, var313[0].second)
val var315 = matchExpr(var313[4].first, var313[4].second)
val var316 = NamedExpr(var314, var315, nextId(), beginGen, endGen)
return var316
}

fun matchVarRedefs(beginGen: Int, endGen: Int): VarRedefs {
val var317 = getSequenceElems(history, 361, listOf(352,7,362,366), beginGen, endGen)
val var318 = matchVarRedef(var317[2].first, var317[2].second)
val var319 = unrollRepeat0(history, 366, 368, 9, 367, var317[3].first, var317[3].second).map { k ->
val var320 = getSequenceElems(history, 369, listOf(7,148,7,362), k.first, k.second)
val var321 = matchVarRedef(var320[3].first, var320[3].second)
var321
}
val var322 = VarRedefs(listOf(var318) + var319, nextId(), beginGen, endGen)
return var322
}

fun matchVarRedef(beginGen: Int, endGen: Int): VarRedef {
val var323 = getSequenceElems(history, 363, listOf(65,364,7,181,7,121), beginGen, endGen)
val var324 = matchSimpleName(var323[0].first, var323[0].second)
val var325 = unrollRepeat1(history, 364, 97, 97, 365, var323[1].first, var323[1].second).map { k ->
val var326 = getSequenceElems(history, 98, listOf(7,99,7,65), k.first, k.second)
val var327 = matchSimpleName(var326[3].first, var326[3].second)
var327
}
val var328 = matchExpr(var323[5].first, var323[5].second)
val var329 = VarRedef(listOf(var324) + var325, var328, nextId(), beginGen, endGen)
return var329
}

fun matchTypeExpr(beginGen: Int, endGen: Int): TypeExpr {
val var330 = history[endGen].findByBeginGenOpt(127, 1, beginGen)
val var331 = history[endGen].findByBeginGenOpt(141, 1, beginGen)
check(hasSingleTrue(var330 != null, var331 != null))
val var332 = when {
var330 != null -> {
val var333 = matchNoUnionType(beginGen, endGen)
var333
}
else -> {
val var334 = matchUnionType(beginGen, endGen)
var334
}
}
return var332
}

fun matchListElem(beginGen: Int, endGen: Int): ListElem {
val var335 = history[endGen].findByBeginGenOpt(121, 1, beginGen)
val var336 = history[endGen].findByBeginGenOpt(194, 3, beginGen)
check(hasSingleTrue(var335 != null, var336 != null))
val var337 = when {
var335 != null -> {
val var338 = matchExpr(beginGen, endGen)
var338
}
else -> {
val var339 = getSequenceElems(history, 194, listOf(195,7,121), beginGen, endGen)
val var340 = matchExpr(var339[2].first, var339[2].second)
val var341 = EllipsisElem(var340, nextId(), beginGen, endGen)
var341
}
}
return var337
}

fun matchParamDef(beginGen: Int, endGen: Int): ParamDef {
val var342 = history[endGen].findByBeginGenOpt(180, 5, beginGen)
val var343 = history[endGen].findByBeginGenOpt(298, 7, beginGen)
check(hasSingleTrue(var342 != null, var343 != null))
val var344 = when {
var342 != null -> {
val var345 = getSequenceElems(history, 180, listOf(65,7,181,7,121), beginGen, endGen)
val var346 = matchSimpleName(var345[0].first, var345[0].second)
val var347 = matchExpr(var345[4].first, var345[4].second)
val var348 = ParamDef(var346, false, null, var347, nextId(), beginGen, endGen)
var348
}
else -> {
val var349 = getSequenceElems(history, 298, listOf(65,299,7,161,7,140,303), beginGen, endGen)
val var350 = matchSimpleName(var349[0].first, var349[0].second)
val var351 = history[var349[1].second].findByBeginGenOpt(100, 1, var349[1].first)
val var352 = history[var349[1].second].findByBeginGenOpt(300, 1, var349[1].first)
check(hasSingleTrue(var351 != null, var352 != null))
val var353 = when {
var351 != null -> null
else -> {
val var354 = getSequenceElems(history, 301, listOf(7,302), var349[1].first, var349[1].second)
source[var354[1].first]
}
}
val var355 = matchTypeExpr(var349[5].first, var349[5].second)
val var356 = history[var349[6].second].findByBeginGenOpt(100, 1, var349[6].first)
val var357 = history[var349[6].second].findByBeginGenOpt(304, 1, var349[6].first)
check(hasSingleTrue(var356 != null, var357 != null))
val var358 = when {
var356 != null -> null
else -> {
val var359 = getSequenceElems(history, 305, listOf(7,181,7,121), var349[6].first, var349[6].second)
val var360 = matchExpr(var359[3].first, var359[3].second)
var360
}
}
val var361 = ParamDef(var350, var353 != null, var355, var358, nextId(), beginGen, endGen)
var361
}
}
return var344
}

fun matchUnionType(beginGen: Int, endGen: Int): UnionType {
val var362 = getSequenceElems(history, 142, listOf(143,7,127,144,7,149), beginGen, endGen)
val var363 = matchNoUnionType(var362[2].first, var362[2].second)
val var364 = unrollRepeat0(history, 144, 146, 9, 145, var362[3].first, var362[3].second).map { k ->
val var365 = getSequenceElems(history, 147, listOf(7,148,7,127), k.first, k.second)
val var366 = matchNoUnionType(var365[3].first, var365[3].second)
var366
}
val var367 = UnionType(listOf(var363) + var364, nextId(), beginGen, endGen)
return var367
}

fun matchTargetDef(beginGen: Int, endGen: Int): TargetDef {
val var368 = getSequenceElems(history, 180, listOf(65,7,181,7,121), beginGen, endGen)
val var369 = matchSimpleName(var368[0].first, var368[0].second)
val var370 = matchExpr(var368[4].first, var368[4].second)
val var371 = TargetDef(var369, var370, nextId(), beginGen, endGen)
return var371
}

fun matchSuperClassDef(beginGen: Int, endGen: Int): SuperClassDef {
val var372 = getSequenceElems(history, 335, listOf(336,7,288,7,65,7,143,7,65,340,7,149), beginGen, endGen)
val var373 = matchSimpleName(var372[4].first, var372[4].second)
val var374 = matchSimpleName(var372[8].first, var372[8].second)
val var375 = unrollRepeat0(history, 340, 342, 9, 341, var372[9].first, var372[9].second).map { k ->
val var376 = getSequenceElems(history, 343, listOf(7,148,7,65), k.first, k.second)
val var377 = matchSimpleName(var376[3].first, var376[3].second)
var377
}
val var378 = SuperClassDef(var373, listOf(var374) + var375, nextId(), beginGen, endGen)
return var378
}

fun matchActionStmt(beginGen: Int, endGen: Int): ActionStmt {
val var379 = history[endGen].findByBeginGenOpt(112, 1, beginGen)
val var380 = history[endGen].findByBeginGenOpt(272, 1, beginGen)
check(hasSingleTrue(var379 != null, var380 != null))
val var381 = when {
var379 != null -> {
val var382 = matchCallExpr(beginGen, endGen)
var382
}
else -> {
val var383 = matchLetStmt(beginGen, endGen)
var383
}
}
return var381
}

fun matchLetStmt(beginGen: Int, endGen: Int): LetStmt {
val var384 = getSequenceElems(history, 273, listOf(274,7,65,7,181,7,278), beginGen, endGen)
val var385 = matchSimpleName(var384[2].first, var384[2].second)
val var386 = matchExpr(var384[6].first, var384[6].second)
val var387 = LetStmt(var385, var386, nextId(), beginGen, endGen)
return var387
}

fun matchCallParams(beginGen: Int, endGen: Int): CallParams {
val var388 = history[endGen].findByBeginGenOpt(115, 3, beginGen)
val var389 = history[endGen].findByBeginGenOpt(118, 6, beginGen)
val var390 = history[endGen].findByBeginGenOpt(176, 6, beginGen)
val var391 = history[endGen].findByBeginGenOpt(186, 10, beginGen)
check(hasSingleTrue(var388 != null, var389 != null, var390 != null, var391 != null))
val var392 = when {
var388 != null -> {
val var393 = CallParams(listOf(), listOf(), nextId(), beginGen, endGen)
var393
}
var389 != null -> {
val var394 = getSequenceElems(history, 118, listOf(116,7,119,173,7,117), beginGen, endGen)
val var395 = matchPositionalParams(var394[2].first, var394[2].second)
val var396 = CallParams(var395, listOf(), nextId(), beginGen, endGen)
var396
}
var390 != null -> {
val var397 = getSequenceElems(history, 176, listOf(116,7,177,173,7,117), beginGen, endGen)
val var398 = matchNamedParams(var397[2].first, var397[2].second)
val var399 = CallParams(listOf(), var398, nextId(), beginGen, endGen)
var399
}
else -> {
val var400 = getSequenceElems(history, 186, listOf(116,7,119,7,148,7,177,173,7,117), beginGen, endGen)
val var401 = matchPositionalParams(var400[2].first, var400[2].second)
val var402 = matchNamedParams(var400[6].first, var400[6].second)
val var403 = CallParams(var401, var402, nextId(), beginGen, endGen)
var403
}
}
return var392
}

fun matchPositionalParams(beginGen: Int, endGen: Int): List<Expr> {
val var404 = getSequenceElems(history, 120, listOf(121,169), beginGen, endGen)
val var405 = matchExpr(var404[0].first, var404[0].second)
val var406 = unrollRepeat0(history, 169, 171, 9, 170, var404[1].first, var404[1].second).map { k ->
val var407 = getSequenceElems(history, 172, listOf(7,148,7,121), k.first, k.second)
val var408 = matchExpr(var407[3].first, var407[3].second)
var408
}
return listOf(var405) + var406
}

fun matchNamedParams(beginGen: Int, endGen: Int): List<NamedParam> {
val var409 = getSequenceElems(history, 178, listOf(179,182), beginGen, endGen)
val var410 = matchNamedParam(var409[0].first, var409[0].second)
val var411 = unrollRepeat0(history, 182, 184, 9, 183, var409[1].first, var409[1].second).map { k ->
val var412 = getSequenceElems(history, 185, listOf(7,148,7,179), k.first, k.second)
val var413 = matchNamedParam(var412[3].first, var412[3].second)
var413
}
return listOf(var410) + var411
}

fun matchNamedParam(beginGen: Int, endGen: Int): NamedParam {
val var414 = getSequenceElems(history, 180, listOf(65,7,181,7,121), beginGen, endGen)
val var415 = matchSimpleName(var414[0].first, var414[0].second)
val var416 = matchExpr(var414[4].first, var414[4].second)
val var417 = NamedParam(var415, var416, nextId(), beginGen, endGen)
return var417
}

fun matchNamedTupleType(beginGen: Int, endGen: Int): NamedTupleType {
val var418 = getSequenceElems(history, 158, listOf(116,7,159,162,7,117), beginGen, endGen)
val var419 = matchNamedType(var418[2].first, var418[2].second)
val var420 = unrollRepeat0(history, 162, 164, 9, 163, var418[3].first, var418[3].second).map { k ->
val var421 = getSequenceElems(history, 165, listOf(7,148,7,159), k.first, k.second)
val var422 = matchNamedType(var421[3].first, var421[3].second)
var422
}
val var423 = NamedTupleType(listOf(var419) + var420, nextId(), beginGen, endGen)
return var423
}

fun matchNamedType(beginGen: Int, endGen: Int): NamedType {
val var424 = getSequenceElems(history, 160, listOf(65,7,161,7,140), beginGen, endGen)
val var425 = matchSimpleName(var424[0].first, var424[0].second)
val var426 = matchTypeExpr(var424[4].first, var424[4].second)
val var427 = NamedType(var425, var426, nextId(), beginGen, endGen)
return var427
}

fun matchBooleanLiteral(beginGen: Int, endGen: Int): BooleanLiteral {
val var428 = history[endGen].findByBeginGenOpt(77, 1, beginGen)
val var429 = history[endGen].findByBeginGenOpt(82, 1, beginGen)
check(hasSingleTrue(var428 != null, var429 != null))
val var430 = when {
var428 != null -> {
val var431 = BooleanLiteral(true, nextId(), beginGen, endGen)
var431
}
else -> {
val var432 = BooleanLiteral(false, nextId(), beginGen, endGen)
var432
}
}
return var430
}

}
