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

sealed interface PathDirection: AstNode

data class This(

  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): Primary, AstNode

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

data class Path(
  val dirs: List<String>,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): PathDirection, AstNode

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
): Literal, PathDirection, AstNode

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
  val path: PathDirection?,
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

data class NamespaceFromDef(
  val name: String,
  val path: PathDirection,
  override val nodeId: Int,
  override val start: Int,
  override val end: Int,
): AstNode

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
enum class BuildRuleMod { NoReuse, Singleton, Synchronized }

fun matchStart(): BuildScript {
  val lastGen = source.length
  val kernel = history[lastGen].getSingle(2, 1, 0, lastGen)
  return matchBuildScript(kernel.beginGen, kernel.endGen)
}

fun matchBuildScript(beginGen: Int, endGen: Int): BuildScript {
val var1 = getSequenceElems(history, 3, listOf(4,7,97,7), beginGen, endGen)
val var2 = history[var1[0].second].findByBeginGenOpt(5, 1, var1[0].first)
val var3 = history[var1[0].second].findByBeginGenOpt(96, 1, var1[0].first)
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
val var11 = getSequenceElems(history, 98, listOf(99,405), beginGen, endGen)
val var12 = matchDef(var11[0].first, var11[0].second)
val var13 = unrollRepeat0(history, 405, 407, 9, 406, var11[1].first, var11[1].second).map { k ->
val var14 = getSequenceElems(history, 408, listOf(7,99), k.first, k.second)
val var15 = matchDef(var14[1].first, var14[1].second)
var15
}
return listOf(var12) + var13
}

fun matchName(beginGen: Int, endGen: Int): Name {
val var16 = getSequenceElems(history, 64, listOf(65,91), beginGen, endGen)
val var17 = matchSimpleName(var16[0].first, var16[0].second)
val var18 = unrollRepeat0(history, 91, 93, 9, 92, var16[1].first, var16[1].second).map { k ->
val var19 = getSequenceElems(history, 94, listOf(7,95,7,65), k.first, k.second)
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
val var24 = history[endGen].findByBeginGenOpt(100, 1, beginGen)
val var25 = history[endGen].findByBeginGenOpt(255, 1, beginGen)
val var26 = history[endGen].findByBeginGenOpt(277, 1, beginGen)
val var27 = history[endGen].findByBeginGenOpt(278, 1, beginGen)
val var28 = history[endGen].findByBeginGenOpt(304, 1, beginGen)
val var29 = history[endGen].findByBeginGenOpt(339, 1, beginGen)
val var30 = history[endGen].findByBeginGenOpt(363, 1, beginGen)
val var31 = history[endGen].findByBeginGenOpt(369, 1, beginGen)
val var32 = history[endGen].findByBeginGenOpt(379, 1, beginGen)
val var33 = history[endGen].findByBeginGenOpt(389, 1, beginGen)
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
val var45 = getSequenceElems(history, 340, listOf(280,7,341,7,65,7,311,7,179,7,346), beginGen, endGen)
val var46 = matchSimpleName(var45[4].first, var45[4].second)
val var47 = matchParamsDef(var45[6].first, var45[6].second)
val var48 = matchMethodRef(var45[10].first, var45[10].second)
val var49 = ActionRuleDef(var46, var47, var48, nextId(), beginGen, endGen)
return var49
}

fun matchBuildRuleDef(beginGen: Int, endGen: Int): BuildRuleDef {
val var50 = getSequenceElems(history, 390, listOf(391,341,7,65,7,311,7,159,7,137,7,179,7,346), beginGen, endGen)
val var51 = unrollRepeat0(history, 391, 393, 9, 392, var50[0].first, var50[0].second).map { k ->
val var52 = getSequenceElems(history, 394, listOf(395,7), k.first, k.second)
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
val var59 = history[endGen].findByBeginGenOpt(101, 4, beginGen)
val var60 = history[endGen].findByBeginGenOpt(250, 8, beginGen)
check(hasSingleTrue(var59 != null, var60 != null))
val var61 = when {
var59 != null -> {
val var62 = getSequenceElems(history, 101, listOf(102,7,108,247), beginGen, endGen)
val var63 = matchPrimary(var62[2].first, var62[2].second)
val var64 = history[var62[3].second].findByBeginGenOpt(96, 1, var62[3].first)
val var65 = history[var62[3].second].findByBeginGenOpt(248, 1, var62[3].first)
check(hasSingleTrue(var64 != null, var65 != null))
val var66 = when {
var64 != null -> null
else -> {
val var67 = getSequenceElems(history, 249, listOf(7,120,7,65), var62[3].first, var62[3].second)
val var68 = matchSimpleName(var67[3].first, var67[3].second)
var68
}
}
val var69 = ImportAll(var63, var66, nextId(), beginGen, endGen)
var69
}
else -> {
val var70 = getSequenceElems(history, 250, listOf(251,7,118,7,102,7,63,247), beginGen, endGen)
val var71 = matchExpr(var70[2].first, var70[2].second)
val var72 = matchName(var70[6].first, var70[6].second)
val var73 = history[var70[7].second].findByBeginGenOpt(96, 1, var70[7].first)
val var74 = history[var70[7].second].findByBeginGenOpt(248, 1, var70[7].first)
check(hasSingleTrue(var73 != null, var74 != null))
val var75 = when {
var73 != null -> null
else -> {
val var76 = getSequenceElems(history, 249, listOf(7,120,7,65), var70[7].first, var70[7].second)
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
val var79 = history[endGen].findByBeginGenOpt(119, 5, beginGen)
val var80 = history[endGen].findByBeginGenOpt(164, 1, beginGen)
check(hasSingleTrue(var79 != null, var80 != null))
val var81 = when {
var79 != null -> {
val var82 = getSequenceElems(history, 119, listOf(118,7,120,7,124), beginGen, endGen)
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
val var88 = history[endGen].findByBeginGenOpt(125, 1, beginGen)
val var89 = history[endGen].findByBeginGenOpt(153, 1, beginGen)
val var90 = history[endGen].findByBeginGenOpt(155, 1, beginGen)
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
val var96 = history[endGen].findByBeginGenOpt(108, 1, beginGen)
val var97 = history[endGen].findByBeginGenOpt(165, 5, beginGen)
check(hasSingleTrue(var96 != null, var97 != null))
val var98 = when {
var96 != null -> {
val var99 = matchPrimary(beginGen, endGen)
var99
}
else -> {
val var100 = getSequenceElems(history, 165, listOf(118,7,166,7,108), beginGen, endGen)
val var101 = matchExpr(var100[0].first, var100[0].second)
val var102 = matchPrimary(var100[4].first, var100[4].second)
val var103 = MergeOp(var101, var102, nextId(), beginGen, endGen)
var103
}
}
return var98
}

fun matchCollectionType(beginGen: Int, endGen: Int): CollectionType {
val var104 = getSequenceElems(history, 126, listOf(127,7,134), beginGen, endGen)
val var105 = history[var104[0].second].findByBeginGenOpt(130, 1, var104[0].first)
val var106 = history[var104[0].second].findByBeginGenOpt(132, 1, var104[0].first)
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
val var111 = getSequenceElems(history, 312, listOf(113,313,171,7,114), beginGen, endGen)
val var112 = history[var111[1].second].findByBeginGenOpt(96, 1, var111[1].first)
val var113 = history[var111[1].second].findByBeginGenOpt(314, 1, var111[1].first)
check(hasSingleTrue(var112 != null, var113 != null))
val var114 = when {
var112 != null -> null
else -> {
val var115 = getSequenceElems(history, 315, listOf(7,316,325), var111[1].first, var111[1].second)
val var116 = matchParamDef(var115[1].first, var115[1].second)
val var117 = unrollRepeat0(history, 325, 327, 9, 326, var115[2].first, var115[2].second).map { k ->
val var118 = getSequenceElems(history, 328, listOf(7,151,7,316), k.first, k.second)
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
val var120 = history[endGen].findByBeginGenOpt(398, 1, beginGen)
val var121 = history[endGen].findByBeginGenOpt(400, 1, beginGen)
val var122 = history[endGen].findByBeginGenOpt(403, 1, beginGen)
check(hasSingleTrue(var120 != null, var121 != null, var122 != null))
val var123 = when {
var120 != null -> BuildRuleMod.Singleton
var121 != null -> BuildRuleMod.Synchronized
else -> BuildRuleMod.NoReuse
}
return var123
}

fun matchTypeParams(beginGen: Int, endGen: Int): TypeParams {
val var124 = getSequenceElems(history, 135, listOf(136,7,137,147,7,152), beginGen, endGen)
val var125 = matchTypeExpr(var124[2].first, var124[2].second)
val var126 = unrollRepeat0(history, 147, 149, 9, 148, var124[3].first, var124[3].second).map { k ->
val var127 = getSequenceElems(history, 150, listOf(7,151,7,137), k.first, k.second)
val var128 = matchTypeExpr(var127[3].first, var127[3].second)
var128
}
val var129 = TypeParams(listOf(var125) + var126, nextId(), beginGen, endGen)
return var129
}

fun matchActionDef(beginGen: Int, endGen: Int): ActionDef {
val var130 = getSequenceElems(history, 279, listOf(280,7,65,284,7,289), beginGen, endGen)
val var131 = matchSimpleName(var130[2].first, var130[2].second)
val var132 = history[var130[3].second].findByBeginGenOpt(96, 1, var130[3].first)
val var133 = history[var130[3].second].findByBeginGenOpt(285, 1, var130[3].first)
check(hasSingleTrue(var132 != null, var133 != null))
val var134 = when {
var132 != null -> null
else -> {
val var135 = getSequenceElems(history, 286, listOf(7,287), var130[3].first, var130[3].second)
val var136 = matchActionParams(var135[1].first, var135[1].second)
var136
}
}
val var137 = matchActionBody(var130[5].first, var130[5].second)
val var138 = ActionDef(var131, var134, var137, nextId(), beginGen, endGen)
return var138
}

fun matchActionParams(beginGen: Int, endGen: Int): String {
val var139 = getSequenceElems(history, 288, listOf(113,7,65,7,114), beginGen, endGen)
val var140 = matchSimpleName(var139[2].first, var139[2].second)
return var140
}

fun matchActionBody(beginGen: Int, endGen: Int): MultiCallActions {
val var141 = getSequenceElems(history, 290, listOf(235,291,7,236), beginGen, endGen)
val var142 = unrollRepeat1(history, 291, 292, 292, 303, var141[1].first, var141[1].second).map { k ->
val var143 = getSequenceElems(history, 293, listOf(7,294), k.first, k.second)
val var144 = matchActionStmt(var143[1].first, var143[1].second)
var144
}
val var145 = MultiCallActions(var142, nextId(), beginGen, endGen)
return var145
}

fun matchTupleType(beginGen: Int, endGen: Int): TupleType {
val var146 = getSequenceElems(history, 154, listOf(113,7,137,147,7,114), beginGen, endGen)
val var147 = matchTypeExpr(var146[2].first, var146[2].second)
val var148 = unrollRepeat0(history, 147, 149, 9, 148, var146[3].first, var146[3].second).map { k ->
val var149 = getSequenceElems(history, 150, listOf(7,151,7,137), k.first, k.second)
val var150 = matchTypeExpr(var149[3].first, var149[3].second)
var150
}
val var151 = TupleType(listOf(var147) + var148, nextId(), beginGen, endGen)
return var151
}

fun matchClassDef(beginGen: Int, endGen: Int): ClassDef {
val var152 = history[endGen].findByBeginGenOpt(305, 1, beginGen)
val var153 = history[endGen].findByBeginGenOpt(353, 1, beginGen)
check(hasSingleTrue(var152 != null, var153 != null))
val var154 = when {
var152 != null -> {
val var155 = matchDataClassDef(beginGen, endGen)
var155
}
else -> {
val var156 = matchSuperClassDef(beginGen, endGen)
var156
}
}
return var154
}

fun matchDataClassDef(beginGen: Int, endGen: Int): DataClassDef {
val var157 = getSequenceElems(history, 306, listOf(307,7,65,7,311,329), beginGen, endGen)
val var158 = matchSimpleName(var157[2].first, var157[2].second)
val var159 = matchParamsDef(var157[4].first, var157[4].second)
val var161 = history[var157[5].second].findByBeginGenOpt(96, 1, var157[5].first)
val var162 = history[var157[5].second].findByBeginGenOpt(330, 1, var157[5].first)
check(hasSingleTrue(var161 != null, var162 != null))
val var163 = when {
var161 != null -> null
else -> {
val var164 = getSequenceElems(history, 331, listOf(7,332), var157[5].first, var157[5].second)
val var165 = matchClassBody(var164[1].first, var164[1].second)
var165
}
}
val var160 = var163
val var166 = DataClassDef(var158, var159, (var160 ?: listOf()), nextId(), beginGen, endGen)
return var166
}

fun matchMethodRef(beginGen: Int, endGen: Int): MethodRef {
val var167 = getSequenceElems(history, 347, listOf(63,7,159,7,63,348), beginGen, endGen)
val var168 = matchName(var167[0].first, var167[0].second)
val var169 = matchName(var167[4].first, var167[4].second)
val var170 = history[var167[5].second].findByBeginGenOpt(96, 1, var167[5].first)
val var171 = history[var167[5].second].findByBeginGenOpt(349, 1, var167[5].first)
check(hasSingleTrue(var170 != null, var171 != null))
val var172 = when {
var170 != null -> null
else -> {
val var173 = getSequenceElems(history, 350, listOf(7,159,7,65), var167[5].first, var167[5].second)
val var174 = matchSimpleName(var173[3].first, var173[3].second)
var174
}
}
val var175 = MethodRef(var168, var169, var172, nextId(), beginGen, endGen)
return var175
}

fun matchClassBody(beginGen: Int, endGen: Int): List<ClassBodyElem> {
val var176 = getSequenceElems(history, 333, listOf(235,334,7,236), beginGen, endGen)
val var177 = unrollRepeat0(history, 334, 336, 9, 335, var176[1].first, var176[1].second).map { k ->
val var178 = getSequenceElems(history, 337, listOf(7,338), k.first, k.second)
val var179 = matchClassBodyElem(var178[1].first, var178[1].second)
var179
}
return var177
}

fun matchClassBodyElem(beginGen: Int, endGen: Int): ClassBodyElem {
val var180 = history[endGen].findByBeginGenOpt(339, 1, beginGen)
val var181 = history[endGen].findByBeginGenOpt(351, 1, beginGen)
check(hasSingleTrue(var180 != null, var181 != null))
val var182 = when {
var180 != null -> {
val var183 = matchActionRuleDef(beginGen, endGen)
var183
}
else -> {
val var184 = matchClassCastDef(beginGen, endGen)
var184
}
}
return var182
}

fun matchClassCastDef(beginGen: Int, endGen: Int): ClassCastDef {
val var185 = getSequenceElems(history, 352, listOf(120,7,137,7,179,7,118), beginGen, endGen)
val var186 = matchTypeExpr(var185[2].first, var185[2].second)
val var187 = matchExpr(var185[6].first, var185[6].second)
val var188 = ClassCastDef(var186, var187, nextId(), beginGen, endGen)
return var188
}

fun matchVarDef(beginGen: Int, endGen: Int): VarDef {
val var189 = getSequenceElems(history, 370, listOf(371,7,65,376,322), beginGen, endGen)
val var190 = matchSimpleName(var189[2].first, var189[2].second)
val var191 = history[var189[3].second].findByBeginGenOpt(96, 1, var189[3].first)
val var192 = history[var189[3].second].findByBeginGenOpt(377, 1, var189[3].first)
check(hasSingleTrue(var191 != null, var192 != null))
val var193 = when {
var191 != null -> null
else -> {
val var194 = getSequenceElems(history, 378, listOf(7,159,7,137), var189[3].first, var189[3].second)
val var195 = matchTypeExpr(var194[3].first, var194[3].second)
var195
}
}
val var196 = history[var189[4].second].findByBeginGenOpt(96, 1, var189[4].first)
val var197 = history[var189[4].second].findByBeginGenOpt(323, 1, var189[4].first)
check(hasSingleTrue(var196 != null, var197 != null))
val var198 = when {
var196 != null -> null
else -> {
val var199 = getSequenceElems(history, 324, listOf(7,179,7,118), var189[4].first, var189[4].second)
val var200 = matchExpr(var199[3].first, var199[3].second)
var200
}
}
val var201 = VarDef(var190, var193, var198, nextId(), beginGen, endGen)
return var201
}

fun matchNamespaceDef(beginGen: Int, endGen: Int): NamespaceDef {
val var202 = getSequenceElems(history, 256, listOf(65,257,7,235,274,7,236), beginGen, endGen)
val var203 = matchSimpleName(var202[0].first, var202[0].second)
val var204 = history[var202[1].second].findByBeginGenOpt(96, 1, var202[1].first)
val var205 = history[var202[1].second].findByBeginGenOpt(258, 1, var202[1].first)
check(hasSingleTrue(var204 != null, var205 != null))
val var206 = when {
var204 != null -> null
else -> {
val var207 = getSequenceElems(history, 259, listOf(7,260,7,264), var202[1].first, var202[1].second)
val var208 = matchPathDirection(var207[3].first, var207[3].second)
var208
}
}
val var210 = history[var202[4].second].findByBeginGenOpt(96, 1, var202[4].first)
val var211 = history[var202[4].second].findByBeginGenOpt(275, 1, var202[4].first)
check(hasSingleTrue(var210 != null, var211 != null))
val var212 = when {
var210 != null -> null
else -> {
val var213 = getSequenceElems(history, 276, listOf(7,97), var202[4].first, var202[4].second)
val var214 = matchDefs(var213[1].first, var213[1].second)
var214
}
}
val var209 = var212
val var215 = NamespaceDef(var203, var206, (var209 ?: listOf()), nextId(), beginGen, endGen)
return var215
}

fun matchPathDirection(beginGen: Int, endGen: Int): PathDirection {
val var216 = history[endGen].findByBeginGenOpt(215, 1, beginGen)
val var217 = history[endGen].findByBeginGenOpt(265, 1, beginGen)
check(hasSingleTrue(var216 != null, var217 != null))
val var218 = when {
var216 != null -> {
val var219 = matchStringLiteral(beginGen, endGen)
var219
}
else -> {
val var220 = matchPath(beginGen, endGen)
var220
}
}
return var218
}

fun matchPath(beginGen: Int, endGen: Int): Path {
val var221 = getSequenceElems(history, 268, listOf(269,270), beginGen, endGen)
val var222 = matchPathToken(var221[0].first, var221[0].second)
val var223 = unrollRepeat0(history, 270, 272, 9, 271, var221[1].first, var221[1].second).map { k ->
val var224 = getSequenceElems(history, 273, listOf(7,19,7,269), k.first, k.second)
val var225 = matchPathToken(var224[3].first, var224[3].second)
var225
}
val var226 = Path(listOf(var222) + var223, nextId(), beginGen, endGen)
return var226
}

fun matchStringLiteral(beginGen: Int, endGen: Int): StringLiteral {
val var227 = getSequenceElems(history, 216, listOf(217,218,217), beginGen, endGen)
val var228 = unrollRepeat0(history, 218, 220, 9, 219, var227[1].first, var227[1].second).map { k ->
val var229 = matchStringElem(k.first, k.second)
var229
}
val var230 = StringLiteral(var228, nextId(), beginGen, endGen)
return var230
}

fun matchStringElem(beginGen: Int, endGen: Int): StringElem {
val var231 = history[endGen].findByBeginGenOpt(223, 1, beginGen)
val var232 = history[endGen].findByBeginGenOpt(225, 1, beginGen)
val var233 = history[endGen].findByBeginGenOpt(229, 1, beginGen)
check(hasSingleTrue(var231 != null, var232 != null, var233 != null))
val var234 = when {
var231 != null -> {
val var235 = JustChar(source[beginGen], nextId(), beginGen, endGen)
var235
}
var232 != null -> {
val var236 = matchEscapeChar(beginGen, endGen)
var236
}
else -> {
val var237 = matchStringExpr(beginGen, endGen)
var237
}
}
return var234
}

fun matchEscapeChar(beginGen: Int, endGen: Int): EscapeChar {
val var238 = getSequenceElems(history, 226, listOf(227,228), beginGen, endGen)
val var239 = EscapeChar(source[var238[1].first], nextId(), beginGen, endGen)
return var239
}

fun matchStringExpr(beginGen: Int, endGen: Int): StringExpr {
val var240 = history[endGen].findByBeginGenOpt(230, 1, beginGen)
val var241 = history[endGen].findByBeginGenOpt(234, 6, beginGen)
check(hasSingleTrue(var240 != null, var241 != null))
val var242 = when {
var240 != null -> {
val var243 = getSequenceElems(history, 232, listOf(233,65), beginGen, endGen)
val var244 = matchSimpleName(var243[1].first, var243[1].second)
val var245 = SimpleExpr(var244, nextId(), beginGen, endGen)
var245
}
else -> {
val var246 = getSequenceElems(history, 234, listOf(233,235,7,118,7,236), beginGen, endGen)
val var247 = matchExpr(var246[3].first, var246[3].second)
val var248 = ComplexExpr(var247, nextId(), beginGen, endGen)
var248
}
}
return var242
}

fun matchPathToken(beginGen: Int, endGen: Int): String {
val var249 = unrollRepeat1(history, 56, 57, 57, 58, beginGen, endGen).map { k ->
source[k.first]
}
return var249.joinToString("") { it.toString() }
}

fun matchEnumDef(beginGen: Int, endGen: Int): EnumDef {
val var250 = getSequenceElems(history, 364, listOf(365,7,65,7,235,7,65,359,171,7,236), beginGen, endGen)
val var251 = matchSimpleName(var250[2].first, var250[2].second)
val var252 = matchSimpleName(var250[6].first, var250[6].second)
val var253 = unrollRepeat0(history, 359, 361, 9, 360, var250[7].first, var250[7].second).map { k ->
val var254 = getSequenceElems(history, 362, listOf(7,151,7,65), k.first, k.second)
val var255 = matchSimpleName(var254[3].first, var254[3].second)
var255
}
val var256 = EnumDef(var251, listOf(var252) + var253, nextId(), beginGen, endGen)
return var256
}

fun matchPrimary(beginGen: Int, endGen: Int): Primary {
val var257 = history[endGen].findByBeginGenOpt(65, 1, beginGen)
val var258 = history[endGen].findByBeginGenOpt(109, 1, beginGen)
val var259 = history[endGen].findByBeginGenOpt(185, 5, beginGen)
val var260 = history[endGen].findByBeginGenOpt(186, 4, beginGen)
val var261 = history[endGen].findByBeginGenOpt(200, 8, beginGen)
val var262 = history[endGen].findByBeginGenOpt(204, 4, beginGen)
val var263 = history[endGen].findByBeginGenOpt(214, 1, beginGen)
val var264 = history[endGen].findByBeginGenOpt(245, 1, beginGen)
val var265 = history[endGen].findByBeginGenOpt(246, 5, beginGen)
check(hasSingleTrue(var257 != null, var258 != null, var259 != null, var260 != null, var261 != null, var262 != null, var263 != null, var264 != null, var265 != null))
val var266 = when {
var257 != null -> {
val var267 = matchSimpleName(beginGen, endGen)
val var268 = NameRef(var267, nextId(), beginGen, endGen)
var268
}
var258 != null -> {
val var269 = matchCallExpr(beginGen, endGen)
var269
}
var259 != null -> {
val var270 = getSequenceElems(history, 185, listOf(108,7,95,7,65), beginGen, endGen)
val var271 = matchPrimary(var270[0].first, var270[0].second)
val var272 = matchSimpleName(var270[4].first, var270[4].second)
val var273 = MemberAccess(var271, var272, nextId(), beginGen, endGen)
var273
}
var260 != null -> {
val var275 = getSequenceElems(history, 186, listOf(187,188,7,199), beginGen, endGen)
val var276 = history[var275[1].second].findByBeginGenOpt(96, 1, var275[1].first)
val var277 = history[var275[1].second].findByBeginGenOpt(189, 1, var275[1].first)
check(hasSingleTrue(var276 != null, var277 != null))
val var278 = when {
var276 != null -> null
else -> {
val var279 = getSequenceElems(history, 190, listOf(7,191,195,171), var275[1].first, var275[1].second)
val var280 = matchListElem(var279[1].first, var279[1].second)
val var281 = unrollRepeat0(history, 195, 197, 9, 196, var279[2].first, var279[2].second).map { k ->
val var282 = getSequenceElems(history, 198, listOf(7,151,7,191), k.first, k.second)
val var283 = matchListElem(var282[3].first, var282[3].second)
var283
}
listOf(var280) + var281
}
}
val var274 = var278
val var284 = ListExpr((var274 ?: listOf()), nextId(), beginGen, endGen)
var284
}
var261 != null -> {
val var285 = getSequenceElems(history, 200, listOf(113,7,118,7,151,201,7,114), beginGen, endGen)
val var286 = matchExpr(var285[2].first, var285[2].second)
val var288 = history[var285[5].second].findByBeginGenOpt(96, 1, var285[5].first)
val var289 = history[var285[5].second].findByBeginGenOpt(202, 1, var285[5].first)
check(hasSingleTrue(var288 != null, var289 != null))
val var290 = when {
var288 != null -> null
else -> {
val var291 = getSequenceElems(history, 203, listOf(7,118,167,171), var285[5].first, var285[5].second)
val var292 = matchExpr(var291[1].first, var291[1].second)
val var293 = unrollRepeat0(history, 167, 169, 9, 168, var291[2].first, var291[2].second).map { k ->
val var294 = getSequenceElems(history, 170, listOf(7,151,7,118), k.first, k.second)
val var295 = matchExpr(var294[3].first, var294[3].second)
var295
}
listOf(var292) + var293
}
}
val var287 = var290
val var296 = TupleExpr(listOf(var286) + (var287 ?: listOf()), nextId(), beginGen, endGen)
var296
}
var262 != null -> {
val var298 = getSequenceElems(history, 204, listOf(113,205,7,114), beginGen, endGen)
val var299 = history[var298[1].second].findByBeginGenOpt(96, 1, var298[1].first)
val var300 = history[var298[1].second].findByBeginGenOpt(206, 1, var298[1].first)
check(hasSingleTrue(var299 != null, var300 != null))
val var301 = when {
var299 != null -> null
else -> {
val var302 = getSequenceElems(history, 207, listOf(7,208,210,171), var298[1].first, var298[1].second)
val var303 = matchNamedExpr(var302[1].first, var302[1].second)
val var304 = unrollRepeat0(history, 210, 212, 9, 211, var302[2].first, var302[2].second).map { k ->
val var305 = getSequenceElems(history, 213, listOf(7,151,7,208), k.first, k.second)
val var306 = matchNamedExpr(var305[3].first, var305[3].second)
var306
}
listOf(var303) + var304
}
}
val var297 = var301
val var307 = NamedTupleExpr((var297 ?: listOf()), nextId(), beginGen, endGen)
var307
}
var263 != null -> {
val var308 = matchLiteral(beginGen, endGen)
var308
}
var264 != null -> {
val var309 = This(nextId(), beginGen, endGen)
var309
}
else -> {
val var310 = getSequenceElems(history, 246, listOf(113,7,118,7,114), beginGen, endGen)
val var311 = matchExpr(var310[2].first, var310[2].second)
val var312 = Paren(var311, nextId(), beginGen, endGen)
var312
}
}
return var266
}

fun matchLiteral(beginGen: Int, endGen: Int): Literal {
val var313 = history[endGen].findByBeginGenOpt(215, 1, beginGen)
val var314 = history[endGen].findByBeginGenOpt(237, 1, beginGen)
val var315 = history[endGen].findByBeginGenOpt(240, 1, beginGen)
check(hasSingleTrue(var313 != null, var314 != null, var315 != null))
val var316 = when {
var313 != null -> {
val var317 = matchStringLiteral(beginGen, endGen)
var317
}
var314 != null -> {
val var318 = matchBooleanLiteral(beginGen, endGen)
var318
}
else -> {
val var319 = matchNoneLiteral(beginGen, endGen)
var319
}
}
return var316
}

fun matchCallExpr(beginGen: Int, endGen: Int): CallExpr {
val var320 = getSequenceElems(history, 110, listOf(63,7,111), beginGen, endGen)
val var321 = matchName(var320[0].first, var320[0].second)
val var322 = matchCallParams(var320[2].first, var320[2].second)
val var323 = CallExpr(var321, var322, nextId(), beginGen, endGen)
return var323
}

fun matchNamedExpr(beginGen: Int, endGen: Int): NamedExpr {
val var324 = getSequenceElems(history, 209, listOf(65,7,159,7,118), beginGen, endGen)
val var325 = matchSimpleName(var324[0].first, var324[0].second)
val var326 = matchExpr(var324[4].first, var324[4].second)
val var327 = NamedExpr(var325, var326, nextId(), beginGen, endGen)
return var327
}

fun matchNoneLiteral(beginGen: Int, endGen: Int): NoneLiteral {
val var328 = NoneLiteral(nextId(), beginGen, endGen)
return var328
}

fun matchVarRedefs(beginGen: Int, endGen: Int): VarRedefs {
val var329 = getSequenceElems(history, 380, listOf(371,7,381,385), beginGen, endGen)
val var330 = matchVarRedef(var329[2].first, var329[2].second)
val var331 = unrollRepeat0(history, 385, 387, 9, 386, var329[3].first, var329[3].second).map { k ->
val var332 = getSequenceElems(history, 388, listOf(7,151,7,381), k.first, k.second)
val var333 = matchVarRedef(var332[3].first, var332[3].second)
var333
}
val var334 = VarRedefs(listOf(var330) + var331, nextId(), beginGen, endGen)
return var334
}

fun matchVarRedef(beginGen: Int, endGen: Int): VarRedef {
val var335 = getSequenceElems(history, 382, listOf(65,383,7,179,7,118), beginGen, endGen)
val var336 = matchSimpleName(var335[0].first, var335[0].second)
val var337 = unrollRepeat1(history, 383, 93, 93, 384, var335[1].first, var335[1].second).map { k ->
val var338 = getSequenceElems(history, 94, listOf(7,95,7,65), k.first, k.second)
val var339 = matchSimpleName(var338[3].first, var338[3].second)
var339
}
val var340 = matchExpr(var335[5].first, var335[5].second)
val var341 = VarRedef(listOf(var336) + var337, var340, nextId(), beginGen, endGen)
return var341
}

fun matchTypeExpr(beginGen: Int, endGen: Int): TypeExpr {
val var342 = history[endGen].findByBeginGenOpt(124, 1, beginGen)
val var343 = history[endGen].findByBeginGenOpt(138, 1, beginGen)
check(hasSingleTrue(var342 != null, var343 != null))
val var344 = when {
var342 != null -> {
val var345 = matchNoUnionType(beginGen, endGen)
var345
}
else -> {
val var346 = matchUnionType(beginGen, endGen)
var346
}
}
return var344
}

fun matchListElem(beginGen: Int, endGen: Int): ListElem {
val var347 = history[endGen].findByBeginGenOpt(118, 1, beginGen)
val var348 = history[endGen].findByBeginGenOpt(192, 3, beginGen)
check(hasSingleTrue(var347 != null, var348 != null))
val var349 = when {
var347 != null -> {
val var350 = matchExpr(beginGen, endGen)
var350
}
else -> {
val var351 = getSequenceElems(history, 192, listOf(193,7,118), beginGen, endGen)
val var352 = matchExpr(var351[2].first, var351[2].second)
val var353 = EllipsisElem(var352, nextId(), beginGen, endGen)
var353
}
}
return var349
}

fun matchParamDef(beginGen: Int, endGen: Int): ParamDef {
val var354 = history[endGen].findByBeginGenOpt(178, 5, beginGen)
val var355 = history[endGen].findByBeginGenOpt(317, 7, beginGen)
check(hasSingleTrue(var354 != null, var355 != null))
val var356 = when {
var354 != null -> {
val var357 = getSequenceElems(history, 178, listOf(65,7,179,7,118), beginGen, endGen)
val var358 = matchSimpleName(var357[0].first, var357[0].second)
val var359 = matchExpr(var357[4].first, var357[4].second)
val var360 = ParamDef(var358, false, null, var359, nextId(), beginGen, endGen)
var360
}
else -> {
val var361 = getSequenceElems(history, 317, listOf(65,318,7,159,7,137,322), beginGen, endGen)
val var362 = matchSimpleName(var361[0].first, var361[0].second)
val var363 = history[var361[1].second].findByBeginGenOpt(96, 1, var361[1].first)
val var364 = history[var361[1].second].findByBeginGenOpt(319, 1, var361[1].first)
check(hasSingleTrue(var363 != null, var364 != null))
val var365 = when {
var363 != null -> null
else -> {
val var366 = getSequenceElems(history, 320, listOf(7,321), var361[1].first, var361[1].second)
source[var366[1].first]
}
}
val var367 = matchTypeExpr(var361[5].first, var361[5].second)
val var368 = history[var361[6].second].findByBeginGenOpt(96, 1, var361[6].first)
val var369 = history[var361[6].second].findByBeginGenOpt(323, 1, var361[6].first)
check(hasSingleTrue(var368 != null, var369 != null))
val var370 = when {
var368 != null -> null
else -> {
val var371 = getSequenceElems(history, 324, listOf(7,179,7,118), var361[6].first, var361[6].second)
val var372 = matchExpr(var371[3].first, var371[3].second)
var372
}
}
val var373 = ParamDef(var362, var365 != null, var367, var370, nextId(), beginGen, endGen)
var373
}
}
return var356
}

fun matchUnionType(beginGen: Int, endGen: Int): UnionType {
val var374 = getSequenceElems(history, 141, listOf(124,142), beginGen, endGen)
val var375 = matchNoUnionType(var374[0].first, var374[0].second)
val var376 = unrollRepeat0(history, 142, 144, 9, 143, var374[1].first, var374[1].second).map { k ->
val var377 = getSequenceElems(history, 145, listOf(7,146,7,124), k.first, k.second)
val var378 = matchNoUnionType(var377[3].first, var377[3].second)
var378
}
val var379 = UnionType(listOf(var375) + var376, nextId(), beginGen, endGen)
return var379
}

fun matchTargetDef(beginGen: Int, endGen: Int): TargetDef {
val var380 = getSequenceElems(history, 178, listOf(65,7,179,7,118), beginGen, endGen)
val var381 = matchSimpleName(var380[0].first, var380[0].second)
val var382 = matchExpr(var380[4].first, var380[4].second)
val var383 = TargetDef(var381, var382, nextId(), beginGen, endGen)
return var383
}

fun matchSuperClassDef(beginGen: Int, endGen: Int): SuperClassDef {
val var384 = getSequenceElems(history, 354, listOf(355,7,307,7,65,7,235,7,65,359,7,236), beginGen, endGen)
val var385 = matchSimpleName(var384[4].first, var384[4].second)
val var386 = matchSimpleName(var384[8].first, var384[8].second)
val var387 = unrollRepeat0(history, 359, 361, 9, 360, var384[9].first, var384[9].second).map { k ->
val var388 = getSequenceElems(history, 362, listOf(7,151,7,65), k.first, k.second)
val var389 = matchSimpleName(var388[3].first, var388[3].second)
var389
}
val var390 = SuperClassDef(var385, listOf(var386) + var387, nextId(), beginGen, endGen)
return var390
}

fun matchActionStmt(beginGen: Int, endGen: Int): ActionStmt {
val var391 = history[endGen].findByBeginGenOpt(109, 1, beginGen)
val var392 = history[endGen].findByBeginGenOpt(295, 1, beginGen)
check(hasSingleTrue(var391 != null, var392 != null))
val var393 = when {
var391 != null -> {
val var394 = matchCallExpr(beginGen, endGen)
var394
}
else -> {
val var395 = matchLetStmt(beginGen, endGen)
var395
}
}
return var393
}

fun matchLetStmt(beginGen: Int, endGen: Int): LetStmt {
val var396 = getSequenceElems(history, 296, listOf(297,7,65,7,179,7,301), beginGen, endGen)
val var397 = matchSimpleName(var396[2].first, var396[2].second)
val var398 = matchExpr(var396[6].first, var396[6].second)
val var399 = LetStmt(var397, var398, nextId(), beginGen, endGen)
return var399
}

fun matchCallParams(beginGen: Int, endGen: Int): CallParams {
val var400 = history[endGen].findByBeginGenOpt(112, 3, beginGen)
val var401 = history[endGen].findByBeginGenOpt(115, 6, beginGen)
val var402 = history[endGen].findByBeginGenOpt(174, 6, beginGen)
val var403 = history[endGen].findByBeginGenOpt(184, 10, beginGen)
check(hasSingleTrue(var400 != null, var401 != null, var402 != null, var403 != null))
val var404 = when {
var400 != null -> {
val var405 = CallParams(listOf(), listOf(), nextId(), beginGen, endGen)
var405
}
var401 != null -> {
val var406 = getSequenceElems(history, 115, listOf(113,7,116,171,7,114), beginGen, endGen)
val var407 = matchPositionalParams(var406[2].first, var406[2].second)
val var408 = CallParams(var407, listOf(), nextId(), beginGen, endGen)
var408
}
var402 != null -> {
val var409 = getSequenceElems(history, 174, listOf(113,7,175,171,7,114), beginGen, endGen)
val var410 = matchNamedParams(var409[2].first, var409[2].second)
val var411 = CallParams(listOf(), var410, nextId(), beginGen, endGen)
var411
}
else -> {
val var412 = getSequenceElems(history, 184, listOf(113,7,116,7,151,7,175,171,7,114), beginGen, endGen)
val var413 = matchPositionalParams(var412[2].first, var412[2].second)
val var414 = matchNamedParams(var412[6].first, var412[6].second)
val var415 = CallParams(var413, var414, nextId(), beginGen, endGen)
var415
}
}
return var404
}

fun matchPositionalParams(beginGen: Int, endGen: Int): List<Expr> {
val var416 = getSequenceElems(history, 117, listOf(118,167), beginGen, endGen)
val var417 = matchExpr(var416[0].first, var416[0].second)
val var418 = unrollRepeat0(history, 167, 169, 9, 168, var416[1].first, var416[1].second).map { k ->
val var419 = getSequenceElems(history, 170, listOf(7,151,7,118), k.first, k.second)
val var420 = matchExpr(var419[3].first, var419[3].second)
var420
}
return listOf(var417) + var418
}

fun matchNamedParams(beginGen: Int, endGen: Int): List<NamedParam> {
val var421 = getSequenceElems(history, 176, listOf(177,180), beginGen, endGen)
val var422 = matchNamedParam(var421[0].first, var421[0].second)
val var423 = unrollRepeat0(history, 180, 182, 9, 181, var421[1].first, var421[1].second).map { k ->
val var424 = getSequenceElems(history, 183, listOf(7,151,7,177), k.first, k.second)
val var425 = matchNamedParam(var424[3].first, var424[3].second)
var425
}
return listOf(var422) + var423
}

fun matchNamedParam(beginGen: Int, endGen: Int): NamedParam {
val var426 = getSequenceElems(history, 178, listOf(65,7,179,7,118), beginGen, endGen)
val var427 = matchSimpleName(var426[0].first, var426[0].second)
val var428 = matchExpr(var426[4].first, var426[4].second)
val var429 = NamedParam(var427, var428, nextId(), beginGen, endGen)
return var429
}

fun matchNamedTupleType(beginGen: Int, endGen: Int): NamedTupleType {
val var430 = getSequenceElems(history, 156, listOf(113,7,157,160,7,114), beginGen, endGen)
val var431 = matchNamedType(var430[2].first, var430[2].second)
val var432 = unrollRepeat0(history, 160, 162, 9, 161, var430[3].first, var430[3].second).map { k ->
val var433 = getSequenceElems(history, 163, listOf(7,151,7,157), k.first, k.second)
val var434 = matchNamedType(var433[3].first, var433[3].second)
var434
}
val var435 = NamedTupleType(listOf(var431) + var432, nextId(), beginGen, endGen)
return var435
}

fun matchNamedType(beginGen: Int, endGen: Int): NamedType {
val var436 = getSequenceElems(history, 158, listOf(65,7,159,7,137), beginGen, endGen)
val var437 = matchSimpleName(var436[0].first, var436[0].second)
val var438 = matchTypeExpr(var436[4].first, var436[4].second)
val var439 = NamedType(var437, var438, nextId(), beginGen, endGen)
return var439
}

fun matchBooleanLiteral(beginGen: Int, endGen: Int): BooleanLiteral {
val var440 = history[endGen].findByBeginGenOpt(77, 1, beginGen)
val var441 = history[endGen].findByBeginGenOpt(82, 1, beginGen)
check(hasSingleTrue(var440 != null, var441 != null))
val var442 = when {
var440 != null -> {
val var443 = BooleanLiteral(true, nextId(), beginGen, endGen)
var443
}
else -> {
val var444 = BooleanLiteral(false, nextId(), beginGen, endGen)
var444
}
}
return var442
}

}
