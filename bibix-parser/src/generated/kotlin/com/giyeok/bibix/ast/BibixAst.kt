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
  val body: MultiCallActions,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): Def, AstNode

data class MultiCallActions(
  val stmts: List<ActionStmt>,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): AstNode

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
val var11 = getSequenceElems(history, 102, listOf(103,383), beginGen, endGen)
val var12 = matchDef(var11[0].first, var11[0].second)
val var13 = unrollRepeat0(history, 383, 385, 9, 384, var11[1].first, var11[1].second).map { k ->
val var14 = getSequenceElems(history, 386, listOf(7,103), k.first, k.second)
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
val var28 = history[endGen].findByBeginGenOpt(284, 1, beginGen)
val var29 = history[endGen].findByBeginGenOpt(319, 1, beginGen)
val var30 = history[endGen].findByBeginGenOpt(343, 1, beginGen)
val var31 = history[endGen].findByBeginGenOpt(349, 1, beginGen)
val var32 = history[endGen].findByBeginGenOpt(359, 1, beginGen)
val var33 = history[endGen].findByBeginGenOpt(369, 1, beginGen)
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
val var45 = getSequenceElems(history, 320, listOf(260,7,321,7,65,7,291,7,181,7,326), beginGen, endGen)
val var46 = matchSimpleName(var45[4].first, var45[4].second)
val var47 = matchParamsDef(var45[6].first, var45[6].second)
val var48 = matchMethodRef(var45[10].first, var45[10].second)
val var49 = ActionRuleDef(var46, var47, var48, nextId(), beginGen, endGen)
return var49
}

fun matchBuildRuleDef(beginGen: Int, endGen: Int): BuildRuleDef {
val var50 = getSequenceElems(history, 370, listOf(371,321,7,65,7,291,7,161,7,140,7,181,7,326), beginGen, endGen)
val var51 = unrollRepeat0(history, 371, 373, 9, 372, var50[0].first, var50[0].second).map { k ->
val var52 = getSequenceElems(history, 374, listOf(375,7), k.first, k.second)
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
val var111 = getSequenceElems(history, 292, listOf(116,293,173,7,117), beginGen, endGen)
val var112 = history[var111[1].second].findByBeginGenOpt(100, 1, var111[1].first)
val var113 = history[var111[1].second].findByBeginGenOpt(294, 1, var111[1].first)
check(hasSingleTrue(var112 != null, var113 != null))
val var114 = when {
var112 != null -> null
else -> {
val var115 = getSequenceElems(history, 295, listOf(7,296,305), var111[1].first, var111[1].second)
val var116 = matchParamDef(var115[1].first, var115[1].second)
val var117 = unrollRepeat0(history, 305, 307, 9, 306, var115[2].first, var115[2].second).map { k ->
val var118 = getSequenceElems(history, 308, listOf(7,148,7,296), k.first, k.second)
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
val var120 = history[endGen].findByBeginGenOpt(378, 1, beginGen)
val var121 = history[endGen].findByBeginGenOpt(380, 1, beginGen)
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

fun matchActionBody(beginGen: Int, endGen: Int): MultiCallActions {
val var140 = getSequenceElems(history, 270, listOf(143,271,7,149), beginGen, endGen)
val var141 = unrollRepeat1(history, 271, 272, 272, 283, var140[1].first, var140[1].second).map { k ->
val var142 = getSequenceElems(history, 273, listOf(7,274), k.first, k.second)
val var143 = matchActionStmt(var142[1].first, var142[1].second)
var143
}
val var144 = MultiCallActions(var141, nextId(), beginGen, endGen)
return var144
}

fun matchTupleType(beginGen: Int, endGen: Int): TupleType {
val var145 = getSequenceElems(history, 156, listOf(116,7,140,150,7,117), beginGen, endGen)
val var146 = matchTypeExpr(var145[2].first, var145[2].second)
val var147 = unrollRepeat0(history, 150, 152, 9, 151, var145[3].first, var145[3].second).map { k ->
val var148 = getSequenceElems(history, 153, listOf(7,148,7,140), k.first, k.second)
val var149 = matchTypeExpr(var148[3].first, var148[3].second)
var149
}
val var150 = TupleType(listOf(var146) + var147, nextId(), beginGen, endGen)
return var150
}

fun matchClassDef(beginGen: Int, endGen: Int): ClassDef {
val var151 = history[endGen].findByBeginGenOpt(285, 1, beginGen)
val var152 = history[endGen].findByBeginGenOpt(333, 1, beginGen)
check(hasSingleTrue(var151 != null, var152 != null))
val var153 = when {
var151 != null -> {
val var154 = matchDataClassDef(beginGen, endGen)
var154
}
else -> {
val var155 = matchSuperClassDef(beginGen, endGen)
var155
}
}
return var153
}

fun matchDataClassDef(beginGen: Int, endGen: Int): DataClassDef {
val var156 = getSequenceElems(history, 286, listOf(287,7,65,7,291,309), beginGen, endGen)
val var157 = matchSimpleName(var156[2].first, var156[2].second)
val var158 = matchParamsDef(var156[4].first, var156[4].second)
val var160 = history[var156[5].second].findByBeginGenOpt(100, 1, var156[5].first)
val var161 = history[var156[5].second].findByBeginGenOpt(310, 1, var156[5].first)
check(hasSingleTrue(var160 != null, var161 != null))
val var162 = when {
var160 != null -> null
else -> {
val var163 = getSequenceElems(history, 311, listOf(7,312), var156[5].first, var156[5].second)
val var164 = matchClassBody(var163[1].first, var163[1].second)
var164
}
}
val var159 = var162
val var165 = DataClassDef(var157, var158, (var159 ?: listOf()), nextId(), beginGen, endGen)
return var165
}

fun matchMethodRef(beginGen: Int, endGen: Int): MethodRef {
val var166 = getSequenceElems(history, 327, listOf(63,7,161,7,63,328), beginGen, endGen)
val var167 = matchName(var166[0].first, var166[0].second)
val var168 = matchName(var166[4].first, var166[4].second)
val var169 = history[var166[5].second].findByBeginGenOpt(100, 1, var166[5].first)
val var170 = history[var166[5].second].findByBeginGenOpt(329, 1, var166[5].first)
check(hasSingleTrue(var169 != null, var170 != null))
val var171 = when {
var169 != null -> null
else -> {
val var172 = getSequenceElems(history, 330, listOf(7,161,7,65), var166[5].first, var166[5].second)
val var173 = matchSimpleName(var172[3].first, var172[3].second)
var173
}
}
val var174 = MethodRef(var167, var168, var171, nextId(), beginGen, endGen)
return var174
}

fun matchClassBody(beginGen: Int, endGen: Int): List<ClassBodyElem> {
val var175 = getSequenceElems(history, 313, listOf(143,314,7,149), beginGen, endGen)
val var176 = unrollRepeat0(history, 314, 316, 9, 315, var175[1].first, var175[1].second).map { k ->
val var177 = getSequenceElems(history, 317, listOf(7,318), k.first, k.second)
val var178 = matchClassBodyElem(var177[1].first, var177[1].second)
var178
}
return var176
}

fun matchClassBodyElem(beginGen: Int, endGen: Int): ClassBodyElem {
val var179 = history[endGen].findByBeginGenOpt(319, 1, beginGen)
val var180 = history[endGen].findByBeginGenOpt(331, 1, beginGen)
check(hasSingleTrue(var179 != null, var180 != null))
val var181 = when {
var179 != null -> {
val var182 = matchActionRuleDef(beginGen, endGen)
var182
}
else -> {
val var183 = matchClassCastDef(beginGen, endGen)
var183
}
}
return var181
}

fun matchClassCastDef(beginGen: Int, endGen: Int): ClassCastDef {
val var184 = getSequenceElems(history, 332, listOf(123,7,140,7,181,7,121), beginGen, endGen)
val var185 = matchTypeExpr(var184[2].first, var184[2].second)
val var186 = matchExpr(var184[6].first, var184[6].second)
val var187 = ClassCastDef(var185, var186, nextId(), beginGen, endGen)
return var187
}

fun matchVarDef(beginGen: Int, endGen: Int): VarDef {
val var188 = getSequenceElems(history, 350, listOf(351,7,65,356,302), beginGen, endGen)
val var189 = matchSimpleName(var188[2].first, var188[2].second)
val var190 = history[var188[3].second].findByBeginGenOpt(100, 1, var188[3].first)
val var191 = history[var188[3].second].findByBeginGenOpt(357, 1, var188[3].first)
check(hasSingleTrue(var190 != null, var191 != null))
val var192 = when {
var190 != null -> null
else -> {
val var193 = getSequenceElems(history, 358, listOf(7,161,7,140), var188[3].first, var188[3].second)
val var194 = matchTypeExpr(var193[3].first, var193[3].second)
var194
}
}
val var195 = history[var188[4].second].findByBeginGenOpt(100, 1, var188[4].first)
val var196 = history[var188[4].second].findByBeginGenOpt(303, 1, var188[4].first)
check(hasSingleTrue(var195 != null, var196 != null))
val var197 = when {
var195 != null -> null
else -> {
val var198 = getSequenceElems(history, 304, listOf(7,181,7,121), var188[4].first, var188[4].second)
val var199 = matchExpr(var198[3].first, var198[3].second)
var199
}
}
val var200 = VarDef(var189, var192, var197, nextId(), beginGen, endGen)
return var200
}

fun matchNamespaceDef(beginGen: Int, endGen: Int): NamespaceDef {
val var201 = getSequenceElems(history, 253, listOf(65,7,143,254,7,149), beginGen, endGen)
val var202 = matchSimpleName(var201[0].first, var201[0].second)
val var204 = history[var201[3].second].findByBeginGenOpt(100, 1, var201[3].first)
val var205 = history[var201[3].second].findByBeginGenOpt(255, 1, var201[3].first)
check(hasSingleTrue(var204 != null, var205 != null))
val var206 = when {
var204 != null -> null
else -> {
val var207 = getSequenceElems(history, 256, listOf(7,101), var201[3].first, var201[3].second)
val var208 = matchDefs(var207[1].first, var207[1].second)
var208
}
}
val var203 = var206
val var209 = NamespaceDef(var202, (var203 ?: listOf()), nextId(), beginGen, endGen)
return var209
}

fun matchEnumDef(beginGen: Int, endGen: Int): EnumDef {
val var210 = getSequenceElems(history, 344, listOf(345,7,65,7,143,7,65,339,173,7,149), beginGen, endGen)
val var211 = matchSimpleName(var210[2].first, var210[2].second)
val var212 = matchSimpleName(var210[6].first, var210[6].second)
val var213 = unrollRepeat0(history, 339, 341, 9, 340, var210[7].first, var210[7].second).map { k ->
val var214 = getSequenceElems(history, 342, listOf(7,148,7,65), k.first, k.second)
val var215 = matchSimpleName(var214[3].first, var214[3].second)
var215
}
val var216 = EnumDef(var211, listOf(var212) + var213, nextId(), beginGen, endGen)
return var216
}

fun matchPrimary(beginGen: Int, endGen: Int): Primary {
val var217 = history[endGen].findByBeginGenOpt(65, 1, beginGen)
val var218 = history[endGen].findByBeginGenOpt(112, 1, beginGen)
val var219 = history[endGen].findByBeginGenOpt(187, 5, beginGen)
val var220 = history[endGen].findByBeginGenOpt(188, 4, beginGen)
val var221 = history[endGen].findByBeginGenOpt(202, 8, beginGen)
val var222 = history[endGen].findByBeginGenOpt(206, 4, beginGen)
val var223 = history[endGen].findByBeginGenOpt(216, 1, beginGen)
val var224 = history[endGen].findByBeginGenOpt(242, 1, beginGen)
val var225 = history[endGen].findByBeginGenOpt(243, 5, beginGen)
check(hasSingleTrue(var217 != null, var218 != null, var219 != null, var220 != null, var221 != null, var222 != null, var223 != null, var224 != null, var225 != null))
val var226 = when {
var217 != null -> {
val var227 = matchSimpleName(beginGen, endGen)
val var228 = NameRef(var227, nextId(), beginGen, endGen)
var228
}
var218 != null -> {
val var229 = matchCallExpr(beginGen, endGen)
var229
}
var219 != null -> {
val var230 = getSequenceElems(history, 187, listOf(111,7,99,7,65), beginGen, endGen)
val var231 = matchPrimary(var230[0].first, var230[0].second)
val var232 = matchSimpleName(var230[4].first, var230[4].second)
val var233 = MemberAccess(var231, var232, nextId(), beginGen, endGen)
var233
}
var220 != null -> {
val var235 = getSequenceElems(history, 188, listOf(189,190,7,201), beginGen, endGen)
val var236 = history[var235[1].second].findByBeginGenOpt(100, 1, var235[1].first)
val var237 = history[var235[1].second].findByBeginGenOpt(191, 1, var235[1].first)
check(hasSingleTrue(var236 != null, var237 != null))
val var238 = when {
var236 != null -> null
else -> {
val var239 = getSequenceElems(history, 192, listOf(7,193,197,173), var235[1].first, var235[1].second)
val var240 = matchListElem(var239[1].first, var239[1].second)
val var241 = unrollRepeat0(history, 197, 199, 9, 198, var239[2].first, var239[2].second).map { k ->
val var242 = getSequenceElems(history, 200, listOf(7,148,7,193), k.first, k.second)
val var243 = matchListElem(var242[3].first, var242[3].second)
var243
}
listOf(var240) + var241
}
}
val var234 = var238
val var244 = ListExpr((var234 ?: listOf()), nextId(), beginGen, endGen)
var244
}
var221 != null -> {
val var245 = getSequenceElems(history, 202, listOf(116,7,121,7,148,203,7,117), beginGen, endGen)
val var246 = matchExpr(var245[2].first, var245[2].second)
val var248 = history[var245[5].second].findByBeginGenOpt(100, 1, var245[5].first)
val var249 = history[var245[5].second].findByBeginGenOpt(204, 1, var245[5].first)
check(hasSingleTrue(var248 != null, var249 != null))
val var250 = when {
var248 != null -> null
else -> {
val var251 = getSequenceElems(history, 205, listOf(7,121,169,173), var245[5].first, var245[5].second)
val var252 = matchExpr(var251[1].first, var251[1].second)
val var253 = unrollRepeat0(history, 169, 171, 9, 170, var251[2].first, var251[2].second).map { k ->
val var254 = getSequenceElems(history, 172, listOf(7,148,7,121), k.first, k.second)
val var255 = matchExpr(var254[3].first, var254[3].second)
var255
}
listOf(var252) + var253
}
}
val var247 = var250
val var256 = TupleExpr(listOf(var246) + (var247 ?: listOf()), nextId(), beginGen, endGen)
var256
}
var222 != null -> {
val var258 = getSequenceElems(history, 206, listOf(116,207,7,117), beginGen, endGen)
val var259 = history[var258[1].second].findByBeginGenOpt(100, 1, var258[1].first)
val var260 = history[var258[1].second].findByBeginGenOpt(208, 1, var258[1].first)
check(hasSingleTrue(var259 != null, var260 != null))
val var261 = when {
var259 != null -> null
else -> {
val var262 = getSequenceElems(history, 209, listOf(7,210,212,173), var258[1].first, var258[1].second)
val var263 = matchNamedExpr(var262[1].first, var262[1].second)
val var264 = unrollRepeat0(history, 212, 214, 9, 213, var262[2].first, var262[2].second).map { k ->
val var265 = getSequenceElems(history, 215, listOf(7,148,7,210), k.first, k.second)
val var266 = matchNamedExpr(var265[3].first, var265[3].second)
var266
}
listOf(var263) + var264
}
}
val var257 = var261
val var267 = NamedTupleExpr((var257 ?: listOf()), nextId(), beginGen, endGen)
var267
}
var223 != null -> {
val var268 = matchLiteral(beginGen, endGen)
var268
}
var224 != null -> {
val var269 = This(nextId(), beginGen, endGen)
var269
}
else -> {
val var270 = getSequenceElems(history, 243, listOf(116,7,121,7,117), beginGen, endGen)
val var271 = matchExpr(var270[2].first, var270[2].second)
val var272 = Paren(var271, nextId(), beginGen, endGen)
var272
}
}
return var226
}

fun matchLiteral(beginGen: Int, endGen: Int): Literal {
val var273 = history[endGen].findByBeginGenOpt(217, 1, beginGen)
val var274 = history[endGen].findByBeginGenOpt(237, 1, beginGen)
val var275 = history[endGen].findByBeginGenOpt(240, 1, beginGen)
check(hasSingleTrue(var273 != null, var274 != null, var275 != null))
val var276 = when {
var273 != null -> {
val var277 = matchStringLiteral(beginGen, endGen)
var277
}
var274 != null -> {
val var278 = matchBooleanLiteral(beginGen, endGen)
var278
}
else -> {
val var279 = matchNoneLiteral(beginGen, endGen)
var279
}
}
return var276
}

fun matchStringLiteral(beginGen: Int, endGen: Int): StringLiteral {
val var280 = getSequenceElems(history, 218, listOf(219,220,219), beginGen, endGen)
val var281 = unrollRepeat0(history, 220, 222, 9, 221, var280[1].first, var280[1].second).map { k ->
val var282 = matchStringElem(k.first, k.second)
var282
}
val var283 = StringLiteral(var281, nextId(), beginGen, endGen)
return var283
}

fun matchCallExpr(beginGen: Int, endGen: Int): CallExpr {
val var284 = getSequenceElems(history, 113, listOf(63,7,114), beginGen, endGen)
val var285 = matchName(var284[0].first, var284[0].second)
val var286 = matchCallParams(var284[2].first, var284[2].second)
val var287 = CallExpr(var285, var286, nextId(), beginGen, endGen)
return var287
}

fun matchNoneLiteral(beginGen: Int, endGen: Int): NoneLiteral {
val var288 = NoneLiteral(nextId(), beginGen, endGen)
return var288
}

fun matchStringElem(beginGen: Int, endGen: Int): StringElem {
val var289 = history[endGen].findByBeginGenOpt(225, 1, beginGen)
val var290 = history[endGen].findByBeginGenOpt(227, 1, beginGen)
val var291 = history[endGen].findByBeginGenOpt(231, 1, beginGen)
check(hasSingleTrue(var289 != null, var290 != null, var291 != null))
val var292 = when {
var289 != null -> {
val var293 = JustChar(source[beginGen], nextId(), beginGen, endGen)
var293
}
var290 != null -> {
val var294 = matchEscapeChar(beginGen, endGen)
var294
}
else -> {
val var295 = matchStringExpr(beginGen, endGen)
var295
}
}
return var292
}

fun matchEscapeChar(beginGen: Int, endGen: Int): EscapeChar {
val var296 = getSequenceElems(history, 228, listOf(229,230), beginGen, endGen)
val var297 = EscapeChar(source[var296[1].first], nextId(), beginGen, endGen)
return var297
}

fun matchStringExpr(beginGen: Int, endGen: Int): StringExpr {
val var298 = history[endGen].findByBeginGenOpt(232, 1, beginGen)
val var299 = history[endGen].findByBeginGenOpt(236, 6, beginGen)
check(hasSingleTrue(var298 != null, var299 != null))
val var300 = when {
var298 != null -> {
val var301 = getSequenceElems(history, 234, listOf(235,65), beginGen, endGen)
val var302 = matchSimpleName(var301[1].first, var301[1].second)
val var303 = SimpleExpr(var302, nextId(), beginGen, endGen)
var303
}
else -> {
val var304 = getSequenceElems(history, 236, listOf(235,143,7,121,7,149), beginGen, endGen)
val var305 = matchExpr(var304[3].first, var304[3].second)
val var306 = ComplexExpr(var305, nextId(), beginGen, endGen)
var306
}
}
return var300
}

fun matchNamedExpr(beginGen: Int, endGen: Int): NamedExpr {
val var307 = getSequenceElems(history, 211, listOf(65,7,161,7,121), beginGen, endGen)
val var308 = matchSimpleName(var307[0].first, var307[0].second)
val var309 = matchExpr(var307[4].first, var307[4].second)
val var310 = NamedExpr(var308, var309, nextId(), beginGen, endGen)
return var310
}

fun matchVarRedefs(beginGen: Int, endGen: Int): VarRedefs {
val var311 = getSequenceElems(history, 360, listOf(351,7,361,365), beginGen, endGen)
val var312 = matchVarRedef(var311[2].first, var311[2].second)
val var313 = unrollRepeat0(history, 365, 367, 9, 366, var311[3].first, var311[3].second).map { k ->
val var314 = getSequenceElems(history, 368, listOf(7,148,7,361), k.first, k.second)
val var315 = matchVarRedef(var314[3].first, var314[3].second)
var315
}
val var316 = VarRedefs(listOf(var312) + var313, nextId(), beginGen, endGen)
return var316
}

fun matchVarRedef(beginGen: Int, endGen: Int): VarRedef {
val var317 = getSequenceElems(history, 362, listOf(65,363,7,181,7,121), beginGen, endGen)
val var318 = matchSimpleName(var317[0].first, var317[0].second)
val var319 = unrollRepeat1(history, 363, 97, 97, 364, var317[1].first, var317[1].second).map { k ->
val var320 = getSequenceElems(history, 98, listOf(7,99,7,65), k.first, k.second)
val var321 = matchSimpleName(var320[3].first, var320[3].second)
var321
}
val var322 = matchExpr(var317[5].first, var317[5].second)
val var323 = VarRedef(listOf(var318) + var319, var322, nextId(), beginGen, endGen)
return var323
}

fun matchTypeExpr(beginGen: Int, endGen: Int): TypeExpr {
val var324 = history[endGen].findByBeginGenOpt(127, 1, beginGen)
val var325 = history[endGen].findByBeginGenOpt(141, 1, beginGen)
check(hasSingleTrue(var324 != null, var325 != null))
val var326 = when {
var324 != null -> {
val var327 = matchNoUnionType(beginGen, endGen)
var327
}
else -> {
val var328 = matchUnionType(beginGen, endGen)
var328
}
}
return var326
}

fun matchListElem(beginGen: Int, endGen: Int): ListElem {
val var329 = history[endGen].findByBeginGenOpt(121, 1, beginGen)
val var330 = history[endGen].findByBeginGenOpt(194, 3, beginGen)
check(hasSingleTrue(var329 != null, var330 != null))
val var331 = when {
var329 != null -> {
val var332 = matchExpr(beginGen, endGen)
var332
}
else -> {
val var333 = getSequenceElems(history, 194, listOf(195,7,121), beginGen, endGen)
val var334 = matchExpr(var333[2].first, var333[2].second)
val var335 = EllipsisElem(var334, nextId(), beginGen, endGen)
var335
}
}
return var331
}

fun matchParamDef(beginGen: Int, endGen: Int): ParamDef {
val var336 = history[endGen].findByBeginGenOpt(180, 5, beginGen)
val var337 = history[endGen].findByBeginGenOpt(297, 7, beginGen)
check(hasSingleTrue(var336 != null, var337 != null))
val var338 = when {
var336 != null -> {
val var339 = getSequenceElems(history, 180, listOf(65,7,181,7,121), beginGen, endGen)
val var340 = matchSimpleName(var339[0].first, var339[0].second)
val var341 = matchExpr(var339[4].first, var339[4].second)
val var342 = ParamDef(var340, false, null, var341, nextId(), beginGen, endGen)
var342
}
else -> {
val var343 = getSequenceElems(history, 297, listOf(65,298,7,161,7,140,302), beginGen, endGen)
val var344 = matchSimpleName(var343[0].first, var343[0].second)
val var345 = history[var343[1].second].findByBeginGenOpt(100, 1, var343[1].first)
val var346 = history[var343[1].second].findByBeginGenOpt(299, 1, var343[1].first)
check(hasSingleTrue(var345 != null, var346 != null))
val var347 = when {
var345 != null -> null
else -> {
val var348 = getSequenceElems(history, 300, listOf(7,301), var343[1].first, var343[1].second)
source[var348[1].first]
}
}
val var349 = matchTypeExpr(var343[5].first, var343[5].second)
val var350 = history[var343[6].second].findByBeginGenOpt(100, 1, var343[6].first)
val var351 = history[var343[6].second].findByBeginGenOpt(303, 1, var343[6].first)
check(hasSingleTrue(var350 != null, var351 != null))
val var352 = when {
var350 != null -> null
else -> {
val var353 = getSequenceElems(history, 304, listOf(7,181,7,121), var343[6].first, var343[6].second)
val var354 = matchExpr(var353[3].first, var353[3].second)
var354
}
}
val var355 = ParamDef(var344, var347 != null, var349, var352, nextId(), beginGen, endGen)
var355
}
}
return var338
}

fun matchUnionType(beginGen: Int, endGen: Int): UnionType {
val var356 = getSequenceElems(history, 142, listOf(143,7,127,144,7,149), beginGen, endGen)
val var357 = matchNoUnionType(var356[2].first, var356[2].second)
val var358 = unrollRepeat0(history, 144, 146, 9, 145, var356[3].first, var356[3].second).map { k ->
val var359 = getSequenceElems(history, 147, listOf(7,148,7,127), k.first, k.second)
val var360 = matchNoUnionType(var359[3].first, var359[3].second)
var360
}
val var361 = UnionType(listOf(var357) + var358, nextId(), beginGen, endGen)
return var361
}

fun matchTargetDef(beginGen: Int, endGen: Int): TargetDef {
val var362 = getSequenceElems(history, 180, listOf(65,7,181,7,121), beginGen, endGen)
val var363 = matchSimpleName(var362[0].first, var362[0].second)
val var364 = matchExpr(var362[4].first, var362[4].second)
val var365 = TargetDef(var363, var364, nextId(), beginGen, endGen)
return var365
}

fun matchSuperClassDef(beginGen: Int, endGen: Int): SuperClassDef {
val var366 = getSequenceElems(history, 334, listOf(335,7,287,7,65,7,143,7,65,339,7,149), beginGen, endGen)
val var367 = matchSimpleName(var366[4].first, var366[4].second)
val var368 = matchSimpleName(var366[8].first, var366[8].second)
val var369 = unrollRepeat0(history, 339, 341, 9, 340, var366[9].first, var366[9].second).map { k ->
val var370 = getSequenceElems(history, 342, listOf(7,148,7,65), k.first, k.second)
val var371 = matchSimpleName(var370[3].first, var370[3].second)
var371
}
val var372 = SuperClassDef(var367, listOf(var368) + var369, nextId(), beginGen, endGen)
return var372
}

fun matchActionStmt(beginGen: Int, endGen: Int): ActionStmt {
val var373 = history[endGen].findByBeginGenOpt(112, 1, beginGen)
val var374 = history[endGen].findByBeginGenOpt(275, 1, beginGen)
check(hasSingleTrue(var373 != null, var374 != null))
val var375 = when {
var373 != null -> {
val var376 = matchCallExpr(beginGen, endGen)
var376
}
else -> {
val var377 = matchLetStmt(beginGen, endGen)
var377
}
}
return var375
}

fun matchLetStmt(beginGen: Int, endGen: Int): LetStmt {
val var378 = getSequenceElems(history, 276, listOf(277,7,65,7,181,7,281), beginGen, endGen)
val var379 = matchSimpleName(var378[2].first, var378[2].second)
val var380 = matchExpr(var378[6].first, var378[6].second)
val var381 = LetStmt(var379, var380, nextId(), beginGen, endGen)
return var381
}

fun matchCallParams(beginGen: Int, endGen: Int): CallParams {
val var382 = history[endGen].findByBeginGenOpt(115, 3, beginGen)
val var383 = history[endGen].findByBeginGenOpt(118, 6, beginGen)
val var384 = history[endGen].findByBeginGenOpt(176, 6, beginGen)
val var385 = history[endGen].findByBeginGenOpt(186, 10, beginGen)
check(hasSingleTrue(var382 != null, var383 != null, var384 != null, var385 != null))
val var386 = when {
var382 != null -> {
val var387 = CallParams(listOf(), listOf(), nextId(), beginGen, endGen)
var387
}
var383 != null -> {
val var388 = getSequenceElems(history, 118, listOf(116,7,119,173,7,117), beginGen, endGen)
val var389 = matchPositionalParams(var388[2].first, var388[2].second)
val var390 = CallParams(var389, listOf(), nextId(), beginGen, endGen)
var390
}
var384 != null -> {
val var391 = getSequenceElems(history, 176, listOf(116,7,177,173,7,117), beginGen, endGen)
val var392 = matchNamedParams(var391[2].first, var391[2].second)
val var393 = CallParams(listOf(), var392, nextId(), beginGen, endGen)
var393
}
else -> {
val var394 = getSequenceElems(history, 186, listOf(116,7,119,7,148,7,177,173,7,117), beginGen, endGen)
val var395 = matchPositionalParams(var394[2].first, var394[2].second)
val var396 = matchNamedParams(var394[6].first, var394[6].second)
val var397 = CallParams(var395, var396, nextId(), beginGen, endGen)
var397
}
}
return var386
}

fun matchPositionalParams(beginGen: Int, endGen: Int): List<Expr> {
val var398 = getSequenceElems(history, 120, listOf(121,169), beginGen, endGen)
val var399 = matchExpr(var398[0].first, var398[0].second)
val var400 = unrollRepeat0(history, 169, 171, 9, 170, var398[1].first, var398[1].second).map { k ->
val var401 = getSequenceElems(history, 172, listOf(7,148,7,121), k.first, k.second)
val var402 = matchExpr(var401[3].first, var401[3].second)
var402
}
return listOf(var399) + var400
}

fun matchNamedParams(beginGen: Int, endGen: Int): List<NamedParam> {
val var403 = getSequenceElems(history, 178, listOf(179,182), beginGen, endGen)
val var404 = matchNamedParam(var403[0].first, var403[0].second)
val var405 = unrollRepeat0(history, 182, 184, 9, 183, var403[1].first, var403[1].second).map { k ->
val var406 = getSequenceElems(history, 185, listOf(7,148,7,179), k.first, k.second)
val var407 = matchNamedParam(var406[3].first, var406[3].second)
var407
}
return listOf(var404) + var405
}

fun matchNamedParam(beginGen: Int, endGen: Int): NamedParam {
val var408 = getSequenceElems(history, 180, listOf(65,7,181,7,121), beginGen, endGen)
val var409 = matchSimpleName(var408[0].first, var408[0].second)
val var410 = matchExpr(var408[4].first, var408[4].second)
val var411 = NamedParam(var409, var410, nextId(), beginGen, endGen)
return var411
}

fun matchNamedTupleType(beginGen: Int, endGen: Int): NamedTupleType {
val var412 = getSequenceElems(history, 158, listOf(116,7,159,162,7,117), beginGen, endGen)
val var413 = matchNamedType(var412[2].first, var412[2].second)
val var414 = unrollRepeat0(history, 162, 164, 9, 163, var412[3].first, var412[3].second).map { k ->
val var415 = getSequenceElems(history, 165, listOf(7,148,7,159), k.first, k.second)
val var416 = matchNamedType(var415[3].first, var415[3].second)
var416
}
val var417 = NamedTupleType(listOf(var413) + var414, nextId(), beginGen, endGen)
return var417
}

fun matchNamedType(beginGen: Int, endGen: Int): NamedType {
val var418 = getSequenceElems(history, 160, listOf(65,7,161,7,140), beginGen, endGen)
val var419 = matchSimpleName(var418[0].first, var418[0].second)
val var420 = matchTypeExpr(var418[4].first, var418[4].second)
val var421 = NamedType(var419, var420, nextId(), beginGen, endGen)
return var421
}

fun matchBooleanLiteral(beginGen: Int, endGen: Int): BooleanLiteral {
val var422 = history[endGen].findByBeginGenOpt(77, 1, beginGen)
val var423 = history[endGen].findByBeginGenOpt(82, 1, beginGen)
check(hasSingleTrue(var422 != null, var423 != null))
val var424 = when {
var422 != null -> {
val var425 = BooleanLiteral(true, nextId(), beginGen, endGen)
var425
}
else -> {
val var426 = BooleanLiteral(false, nextId(), beginGen, endGen)
var426
}
}
return var424
}

}
