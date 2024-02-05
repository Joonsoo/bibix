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
val var11 = getSequenceElems(history, 102, listOf(103,405), beginGen, endGen)
val var12 = matchDef(var11[0].first, var11[0].second)
val var13 = unrollRepeat0(history, 405, 407, 9, 406, var11[1].first, var11[1].second).map { k ->
val var14 = getSequenceElems(history, 408, listOf(7,103), k.first, k.second)
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
val var45 = getSequenceElems(history, 340, listOf(280,7,341,7,65,7,311,7,183,7,346), beginGen, endGen)
val var46 = matchSimpleName(var45[4].first, var45[4].second)
val var47 = matchParamsDef(var45[6].first, var45[6].second)
val var48 = matchMethodRef(var45[10].first, var45[10].second)
val var49 = ActionRuleDef(var46, var47, var48, nextId(), beginGen, endGen)
return var49
}

fun matchBuildRuleDef(beginGen: Int, endGen: Int): BuildRuleDef {
val var50 = getSequenceElems(history, 390, listOf(391,341,7,65,7,311,7,163,7,141,7,183,7,346), beginGen, endGen)
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
val var59 = history[endGen].findByBeginGenOpt(105, 4, beginGen)
val var60 = history[endGen].findByBeginGenOpt(250, 8, beginGen)
check(hasSingleTrue(var59 != null, var60 != null))
val var61 = when {
var59 != null -> {
val var62 = getSequenceElems(history, 105, listOf(106,7,111,247), beginGen, endGen)
val var63 = matchPrimary(var62[2].first, var62[2].second)
val var64 = history[var62[3].second].findByBeginGenOpt(100, 1, var62[3].first)
val var65 = history[var62[3].second].findByBeginGenOpt(248, 1, var62[3].first)
check(hasSingleTrue(var64 != null, var65 != null))
val var66 = when {
var64 != null -> null
else -> {
val var67 = getSequenceElems(history, 249, listOf(7,123,7,65), var62[3].first, var62[3].second)
val var68 = matchSimpleName(var67[3].first, var67[3].second)
var68
}
}
val var69 = ImportAll(var63, var66, nextId(), beginGen, endGen)
var69
}
else -> {
val var70 = getSequenceElems(history, 250, listOf(251,7,121,7,106,7,63,247), beginGen, endGen)
val var71 = matchExpr(var70[2].first, var70[2].second)
val var72 = matchName(var70[6].first, var70[6].second)
val var73 = history[var70[7].second].findByBeginGenOpt(100, 1, var70[7].first)
val var74 = history[var70[7].second].findByBeginGenOpt(248, 1, var70[7].first)
check(hasSingleTrue(var73 != null, var74 != null))
val var75 = when {
var73 != null -> null
else -> {
val var76 = getSequenceElems(history, 249, listOf(7,123,7,65), var70[7].first, var70[7].second)
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
val var80 = history[endGen].findByBeginGenOpt(168, 1, beginGen)
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
val var89 = history[endGen].findByBeginGenOpt(129, 1, beginGen)
val var90 = history[endGen].findByBeginGenOpt(157, 1, beginGen)
val var91 = history[endGen].findByBeginGenOpt(159, 1, beginGen)
check(hasSingleTrue(var87 != null, var88 != null, var89 != null, var90 != null, var91 != null))
val var92 = when {
var87 != null -> {
val var93 = matchName(beginGen, endGen)
var93
}
var88 != null -> {
val var94 = NoneType(nextId(), beginGen, endGen)
var94
}
var89 != null -> {
val var95 = matchCollectionType(beginGen, endGen)
var95
}
var90 != null -> {
val var96 = matchTupleType(beginGen, endGen)
var96
}
else -> {
val var97 = matchNamedTupleType(beginGen, endGen)
var97
}
}
return var92
}

fun matchMergeOpOrPrimary(beginGen: Int, endGen: Int): MergeOpOrPrimary {
val var98 = history[endGen].findByBeginGenOpt(111, 1, beginGen)
val var99 = history[endGen].findByBeginGenOpt(169, 5, beginGen)
check(hasSingleTrue(var98 != null, var99 != null))
val var100 = when {
var98 != null -> {
val var101 = matchPrimary(beginGen, endGen)
var101
}
else -> {
val var102 = getSequenceElems(history, 169, listOf(121,7,170,7,111), beginGen, endGen)
val var103 = matchExpr(var102[0].first, var102[0].second)
val var104 = matchPrimary(var102[4].first, var102[4].second)
val var105 = MergeOp(var103, var104, nextId(), beginGen, endGen)
var105
}
}
return var100
}

fun matchCollectionType(beginGen: Int, endGen: Int): CollectionType {
val var106 = getSequenceElems(history, 130, listOf(131,7,138), beginGen, endGen)
val var107 = history[var106[0].second].findByBeginGenOpt(134, 1, var106[0].first)
val var108 = history[var106[0].second].findByBeginGenOpt(136, 1, var106[0].first)
check(hasSingleTrue(var107 != null, var108 != null))
val var109 = when {
var107 != null -> "set"
else -> "list"
}
val var110 = matchTypeParams(var106[2].first, var106[2].second)
val var111 = CollectionType(var109, var110, nextId(), beginGen, endGen)
return var111
}

fun matchParamsDef(beginGen: Int, endGen: Int): List<ParamDef> {
val var113 = getSequenceElems(history, 312, listOf(116,313,175,7,117), beginGen, endGen)
val var114 = history[var113[1].second].findByBeginGenOpt(100, 1, var113[1].first)
val var115 = history[var113[1].second].findByBeginGenOpt(314, 1, var113[1].first)
check(hasSingleTrue(var114 != null, var115 != null))
val var116 = when {
var114 != null -> null
else -> {
val var117 = getSequenceElems(history, 315, listOf(7,316,325), var113[1].first, var113[1].second)
val var118 = matchParamDef(var117[1].first, var117[1].second)
val var119 = unrollRepeat0(history, 325, 327, 9, 326, var117[2].first, var117[2].second).map { k ->
val var120 = getSequenceElems(history, 328, listOf(7,155,7,316), k.first, k.second)
val var121 = matchParamDef(var120[3].first, var120[3].second)
var121
}
listOf(var118) + var119
}
}
val var112 = var116
return (var112 ?: listOf())
}

fun matchBuildRuleMod(beginGen: Int, endGen: Int): BuildRuleMod {
val var122 = history[endGen].findByBeginGenOpt(398, 1, beginGen)
val var123 = history[endGen].findByBeginGenOpt(400, 1, beginGen)
val var124 = history[endGen].findByBeginGenOpt(403, 1, beginGen)
check(hasSingleTrue(var122 != null, var123 != null, var124 != null))
val var125 = when {
var122 != null -> BuildRuleMod.Singleton
var123 != null -> BuildRuleMod.Synchronized
else -> BuildRuleMod.NoReuse
}
return var125
}

fun matchTypeParams(beginGen: Int, endGen: Int): TypeParams {
val var126 = getSequenceElems(history, 139, listOf(140,7,141,151,7,156), beginGen, endGen)
val var127 = matchTypeExpr(var126[2].first, var126[2].second)
val var128 = unrollRepeat0(history, 151, 153, 9, 152, var126[3].first, var126[3].second).map { k ->
val var129 = getSequenceElems(history, 154, listOf(7,155,7,141), k.first, k.second)
val var130 = matchTypeExpr(var129[3].first, var129[3].second)
var130
}
val var131 = TypeParams(listOf(var127) + var128, nextId(), beginGen, endGen)
return var131
}

fun matchActionDef(beginGen: Int, endGen: Int): ActionDef {
val var132 = getSequenceElems(history, 279, listOf(280,7,65,284,7,289), beginGen, endGen)
val var133 = matchSimpleName(var132[2].first, var132[2].second)
val var134 = history[var132[3].second].findByBeginGenOpt(100, 1, var132[3].first)
val var135 = history[var132[3].second].findByBeginGenOpt(285, 1, var132[3].first)
check(hasSingleTrue(var134 != null, var135 != null))
val var136 = when {
var134 != null -> null
else -> {
val var137 = getSequenceElems(history, 286, listOf(7,287), var132[3].first, var132[3].second)
val var138 = matchActionParams(var137[1].first, var137[1].second)
var138
}
}
val var139 = matchActionBody(var132[5].first, var132[5].second)
val var140 = ActionDef(var133, var136, var139, nextId(), beginGen, endGen)
return var140
}

fun matchActionParams(beginGen: Int, endGen: Int): String {
val var141 = getSequenceElems(history, 288, listOf(116,7,65,7,117), beginGen, endGen)
val var142 = matchSimpleName(var141[2].first, var141[2].second)
return var142
}

fun matchActionBody(beginGen: Int, endGen: Int): MultiCallActions {
val var143 = getSequenceElems(history, 290, listOf(239,291,7,240), beginGen, endGen)
val var144 = unrollRepeat1(history, 291, 292, 292, 303, var143[1].first, var143[1].second).map { k ->
val var145 = getSequenceElems(history, 293, listOf(7,294), k.first, k.second)
val var146 = matchActionStmt(var145[1].first, var145[1].second)
var146
}
val var147 = MultiCallActions(var144, nextId(), beginGen, endGen)
return var147
}

fun matchTupleType(beginGen: Int, endGen: Int): TupleType {
val var148 = getSequenceElems(history, 158, listOf(116,7,141,151,7,117), beginGen, endGen)
val var149 = matchTypeExpr(var148[2].first, var148[2].second)
val var150 = unrollRepeat0(history, 151, 153, 9, 152, var148[3].first, var148[3].second).map { k ->
val var151 = getSequenceElems(history, 154, listOf(7,155,7,141), k.first, k.second)
val var152 = matchTypeExpr(var151[3].first, var151[3].second)
var152
}
val var153 = TupleType(listOf(var149) + var150, nextId(), beginGen, endGen)
return var153
}

fun matchClassDef(beginGen: Int, endGen: Int): ClassDef {
val var154 = history[endGen].findByBeginGenOpt(305, 1, beginGen)
val var155 = history[endGen].findByBeginGenOpt(353, 1, beginGen)
check(hasSingleTrue(var154 != null, var155 != null))
val var156 = when {
var154 != null -> {
val var157 = matchDataClassDef(beginGen, endGen)
var157
}
else -> {
val var158 = matchSuperClassDef(beginGen, endGen)
var158
}
}
return var156
}

fun matchDataClassDef(beginGen: Int, endGen: Int): DataClassDef {
val var159 = getSequenceElems(history, 306, listOf(307,7,65,7,311,329), beginGen, endGen)
val var160 = matchSimpleName(var159[2].first, var159[2].second)
val var161 = matchParamsDef(var159[4].first, var159[4].second)
val var163 = history[var159[5].second].findByBeginGenOpt(100, 1, var159[5].first)
val var164 = history[var159[5].second].findByBeginGenOpt(330, 1, var159[5].first)
check(hasSingleTrue(var163 != null, var164 != null))
val var165 = when {
var163 != null -> null
else -> {
val var166 = getSequenceElems(history, 331, listOf(7,332), var159[5].first, var159[5].second)
val var167 = matchClassBody(var166[1].first, var166[1].second)
var167
}
}
val var162 = var165
val var168 = DataClassDef(var160, var161, (var162 ?: listOf()), nextId(), beginGen, endGen)
return var168
}

fun matchMethodRef(beginGen: Int, endGen: Int): MethodRef {
val var169 = getSequenceElems(history, 347, listOf(63,7,163,7,63,348), beginGen, endGen)
val var170 = matchName(var169[0].first, var169[0].second)
val var171 = matchName(var169[4].first, var169[4].second)
val var172 = history[var169[5].second].findByBeginGenOpt(100, 1, var169[5].first)
val var173 = history[var169[5].second].findByBeginGenOpt(349, 1, var169[5].first)
check(hasSingleTrue(var172 != null, var173 != null))
val var174 = when {
var172 != null -> null
else -> {
val var175 = getSequenceElems(history, 350, listOf(7,163,7,65), var169[5].first, var169[5].second)
val var176 = matchSimpleName(var175[3].first, var175[3].second)
var176
}
}
val var177 = MethodRef(var170, var171, var174, nextId(), beginGen, endGen)
return var177
}

fun matchClassBody(beginGen: Int, endGen: Int): List<ClassBodyElem> {
val var178 = getSequenceElems(history, 333, listOf(239,334,7,240), beginGen, endGen)
val var179 = unrollRepeat0(history, 334, 336, 9, 335, var178[1].first, var178[1].second).map { k ->
val var180 = getSequenceElems(history, 337, listOf(7,338), k.first, k.second)
val var181 = matchClassBodyElem(var180[1].first, var180[1].second)
var181
}
return var179
}

fun matchClassBodyElem(beginGen: Int, endGen: Int): ClassBodyElem {
val var182 = history[endGen].findByBeginGenOpt(339, 1, beginGen)
val var183 = history[endGen].findByBeginGenOpt(351, 1, beginGen)
check(hasSingleTrue(var182 != null, var183 != null))
val var184 = when {
var182 != null -> {
val var185 = matchActionRuleDef(beginGen, endGen)
var185
}
else -> {
val var186 = matchClassCastDef(beginGen, endGen)
var186
}
}
return var184
}

fun matchClassCastDef(beginGen: Int, endGen: Int): ClassCastDef {
val var187 = getSequenceElems(history, 352, listOf(123,7,141,7,183,7,121), beginGen, endGen)
val var188 = matchTypeExpr(var187[2].first, var187[2].second)
val var189 = matchExpr(var187[6].first, var187[6].second)
val var190 = ClassCastDef(var188, var189, nextId(), beginGen, endGen)
return var190
}

fun matchVarDef(beginGen: Int, endGen: Int): VarDef {
val var191 = getSequenceElems(history, 370, listOf(371,7,65,376,322), beginGen, endGen)
val var192 = matchSimpleName(var191[2].first, var191[2].second)
val var193 = history[var191[3].second].findByBeginGenOpt(100, 1, var191[3].first)
val var194 = history[var191[3].second].findByBeginGenOpt(377, 1, var191[3].first)
check(hasSingleTrue(var193 != null, var194 != null))
val var195 = when {
var193 != null -> null
else -> {
val var196 = getSequenceElems(history, 378, listOf(7,163,7,141), var191[3].first, var191[3].second)
val var197 = matchTypeExpr(var196[3].first, var196[3].second)
var197
}
}
val var198 = history[var191[4].second].findByBeginGenOpt(100, 1, var191[4].first)
val var199 = history[var191[4].second].findByBeginGenOpt(323, 1, var191[4].first)
check(hasSingleTrue(var198 != null, var199 != null))
val var200 = when {
var198 != null -> null
else -> {
val var201 = getSequenceElems(history, 324, listOf(7,183,7,121), var191[4].first, var191[4].second)
val var202 = matchExpr(var201[3].first, var201[3].second)
var202
}
}
val var203 = VarDef(var192, var195, var200, nextId(), beginGen, endGen)
return var203
}

fun matchNamespaceDef(beginGen: Int, endGen: Int): NamespaceDef {
val var204 = getSequenceElems(history, 256, listOf(65,257,7,239,274,7,240), beginGen, endGen)
val var205 = matchSimpleName(var204[0].first, var204[0].second)
val var206 = history[var204[1].second].findByBeginGenOpt(100, 1, var204[1].first)
val var207 = history[var204[1].second].findByBeginGenOpt(258, 1, var204[1].first)
check(hasSingleTrue(var206 != null, var207 != null))
val var208 = when {
var206 != null -> null
else -> {
val var209 = getSequenceElems(history, 259, listOf(7,260,7,264), var204[1].first, var204[1].second)
val var210 = matchPathDirection(var209[3].first, var209[3].second)
var210
}
}
val var212 = history[var204[4].second].findByBeginGenOpt(100, 1, var204[4].first)
val var213 = history[var204[4].second].findByBeginGenOpt(275, 1, var204[4].first)
check(hasSingleTrue(var212 != null, var213 != null))
val var214 = when {
var212 != null -> null
else -> {
val var215 = getSequenceElems(history, 276, listOf(7,101), var204[4].first, var204[4].second)
val var216 = matchDefs(var215[1].first, var215[1].second)
var216
}
}
val var211 = var214
val var217 = NamespaceDef(var205, var208, (var211 ?: listOf()), nextId(), beginGen, endGen)
return var217
}

fun matchPathDirection(beginGen: Int, endGen: Int): PathDirection {
val var218 = history[endGen].findByBeginGenOpt(219, 1, beginGen)
val var219 = history[endGen].findByBeginGenOpt(265, 1, beginGen)
check(hasSingleTrue(var218 != null, var219 != null))
val var220 = when {
var218 != null -> {
val var221 = matchStringLiteral(beginGen, endGen)
var221
}
else -> {
val var222 = matchPath(beginGen, endGen)
var222
}
}
return var220
}

fun matchPath(beginGen: Int, endGen: Int): Path {
val var223 = getSequenceElems(history, 268, listOf(269,270), beginGen, endGen)
val var224 = matchPathToken(var223[0].first, var223[0].second)
val var225 = unrollRepeat0(history, 270, 272, 9, 271, var223[1].first, var223[1].second).map { k ->
val var226 = getSequenceElems(history, 273, listOf(7,19,7,269), k.first, k.second)
val var227 = matchPathToken(var226[3].first, var226[3].second)
var227
}
val var228 = Path(listOf(var224) + var225, nextId(), beginGen, endGen)
return var228
}

fun matchStringLiteral(beginGen: Int, endGen: Int): StringLiteral {
val var229 = getSequenceElems(history, 220, listOf(221,222,221), beginGen, endGen)
val var230 = unrollRepeat0(history, 222, 224, 9, 223, var229[1].first, var229[1].second).map { k ->
val var231 = matchStringElem(k.first, k.second)
var231
}
val var232 = StringLiteral(var230, nextId(), beginGen, endGen)
return var232
}

fun matchStringElem(beginGen: Int, endGen: Int): StringElem {
val var233 = history[endGen].findByBeginGenOpt(227, 1, beginGen)
val var234 = history[endGen].findByBeginGenOpt(229, 1, beginGen)
val var235 = history[endGen].findByBeginGenOpt(233, 1, beginGen)
check(hasSingleTrue(var233 != null, var234 != null, var235 != null))
val var236 = when {
var233 != null -> {
val var237 = JustChar(source[beginGen], nextId(), beginGen, endGen)
var237
}
var234 != null -> {
val var238 = matchEscapeChar(beginGen, endGen)
var238
}
else -> {
val var239 = matchStringExpr(beginGen, endGen)
var239
}
}
return var236
}

fun matchEscapeChar(beginGen: Int, endGen: Int): EscapeChar {
val var240 = getSequenceElems(history, 230, listOf(231,232), beginGen, endGen)
val var241 = EscapeChar(source[var240[1].first], nextId(), beginGen, endGen)
return var241
}

fun matchStringExpr(beginGen: Int, endGen: Int): StringExpr {
val var242 = history[endGen].findByBeginGenOpt(234, 1, beginGen)
val var243 = history[endGen].findByBeginGenOpt(238, 6, beginGen)
check(hasSingleTrue(var242 != null, var243 != null))
val var244 = when {
var242 != null -> {
val var245 = getSequenceElems(history, 236, listOf(237,65), beginGen, endGen)
val var246 = matchSimpleName(var245[1].first, var245[1].second)
val var247 = SimpleExpr(var246, nextId(), beginGen, endGen)
var247
}
else -> {
val var248 = getSequenceElems(history, 238, listOf(237,239,7,121,7,240), beginGen, endGen)
val var249 = matchExpr(var248[3].first, var248[3].second)
val var250 = ComplexExpr(var249, nextId(), beginGen, endGen)
var250
}
}
return var244
}

fun matchPathToken(beginGen: Int, endGen: Int): String {
val var251 = unrollRepeat1(history, 56, 57, 57, 58, beginGen, endGen).map { k ->
source[k.first]
}
return var251.joinToString("") { it.toString() }
}

fun matchEnumDef(beginGen: Int, endGen: Int): EnumDef {
val var252 = getSequenceElems(history, 364, listOf(365,7,65,7,239,7,65,359,175,7,240), beginGen, endGen)
val var253 = matchSimpleName(var252[2].first, var252[2].second)
val var254 = matchSimpleName(var252[6].first, var252[6].second)
val var255 = unrollRepeat0(history, 359, 361, 9, 360, var252[7].first, var252[7].second).map { k ->
val var256 = getSequenceElems(history, 362, listOf(7,155,7,65), k.first, k.second)
val var257 = matchSimpleName(var256[3].first, var256[3].second)
var257
}
val var258 = EnumDef(var253, listOf(var254) + var255, nextId(), beginGen, endGen)
return var258
}

fun matchPrimary(beginGen: Int, endGen: Int): Primary {
val var259 = history[endGen].findByBeginGenOpt(65, 1, beginGen)
val var260 = history[endGen].findByBeginGenOpt(112, 1, beginGen)
val var261 = history[endGen].findByBeginGenOpt(189, 5, beginGen)
val var262 = history[endGen].findByBeginGenOpt(190, 4, beginGen)
val var263 = history[endGen].findByBeginGenOpt(204, 8, beginGen)
val var264 = history[endGen].findByBeginGenOpt(208, 4, beginGen)
val var265 = history[endGen].findByBeginGenOpt(218, 1, beginGen)
val var266 = history[endGen].findByBeginGenOpt(245, 1, beginGen)
val var267 = history[endGen].findByBeginGenOpt(246, 5, beginGen)
check(hasSingleTrue(var259 != null, var260 != null, var261 != null, var262 != null, var263 != null, var264 != null, var265 != null, var266 != null, var267 != null))
val var268 = when {
var259 != null -> {
val var269 = matchSimpleName(beginGen, endGen)
val var270 = NameRef(var269, nextId(), beginGen, endGen)
var270
}
var260 != null -> {
val var271 = matchCallExpr(beginGen, endGen)
var271
}
var261 != null -> {
val var272 = getSequenceElems(history, 189, listOf(111,7,99,7,65), beginGen, endGen)
val var273 = matchPrimary(var272[0].first, var272[0].second)
val var274 = matchSimpleName(var272[4].first, var272[4].second)
val var275 = MemberAccess(var273, var274, nextId(), beginGen, endGen)
var275
}
var262 != null -> {
val var277 = getSequenceElems(history, 190, listOf(191,192,7,203), beginGen, endGen)
val var278 = history[var277[1].second].findByBeginGenOpt(100, 1, var277[1].first)
val var279 = history[var277[1].second].findByBeginGenOpt(193, 1, var277[1].first)
check(hasSingleTrue(var278 != null, var279 != null))
val var280 = when {
var278 != null -> null
else -> {
val var281 = getSequenceElems(history, 194, listOf(7,195,199,175), var277[1].first, var277[1].second)
val var282 = matchListElem(var281[1].first, var281[1].second)
val var283 = unrollRepeat0(history, 199, 201, 9, 200, var281[2].first, var281[2].second).map { k ->
val var284 = getSequenceElems(history, 202, listOf(7,155,7,195), k.first, k.second)
val var285 = matchListElem(var284[3].first, var284[3].second)
var285
}
listOf(var282) + var283
}
}
val var276 = var280
val var286 = ListExpr((var276 ?: listOf()), nextId(), beginGen, endGen)
var286
}
var263 != null -> {
val var287 = getSequenceElems(history, 204, listOf(116,7,121,7,155,205,7,117), beginGen, endGen)
val var288 = matchExpr(var287[2].first, var287[2].second)
val var290 = history[var287[5].second].findByBeginGenOpt(100, 1, var287[5].first)
val var291 = history[var287[5].second].findByBeginGenOpt(206, 1, var287[5].first)
check(hasSingleTrue(var290 != null, var291 != null))
val var292 = when {
var290 != null -> null
else -> {
val var293 = getSequenceElems(history, 207, listOf(7,121,171,175), var287[5].first, var287[5].second)
val var294 = matchExpr(var293[1].first, var293[1].second)
val var295 = unrollRepeat0(history, 171, 173, 9, 172, var293[2].first, var293[2].second).map { k ->
val var296 = getSequenceElems(history, 174, listOf(7,155,7,121), k.first, k.second)
val var297 = matchExpr(var296[3].first, var296[3].second)
var297
}
listOf(var294) + var295
}
}
val var289 = var292
val var298 = TupleExpr(listOf(var288) + (var289 ?: listOf()), nextId(), beginGen, endGen)
var298
}
var264 != null -> {
val var300 = getSequenceElems(history, 208, listOf(116,209,7,117), beginGen, endGen)
val var301 = history[var300[1].second].findByBeginGenOpt(100, 1, var300[1].first)
val var302 = history[var300[1].second].findByBeginGenOpt(210, 1, var300[1].first)
check(hasSingleTrue(var301 != null, var302 != null))
val var303 = when {
var301 != null -> null
else -> {
val var304 = getSequenceElems(history, 211, listOf(7,212,214,175), var300[1].first, var300[1].second)
val var305 = matchNamedExpr(var304[1].first, var304[1].second)
val var306 = unrollRepeat0(history, 214, 216, 9, 215, var304[2].first, var304[2].second).map { k ->
val var307 = getSequenceElems(history, 217, listOf(7,155,7,212), k.first, k.second)
val var308 = matchNamedExpr(var307[3].first, var307[3].second)
var308
}
listOf(var305) + var306
}
}
val var299 = var303
val var309 = NamedTupleExpr((var299 ?: listOf()), nextId(), beginGen, endGen)
var309
}
var265 != null -> {
val var310 = matchLiteral(beginGen, endGen)
var310
}
var266 != null -> {
val var311 = This(nextId(), beginGen, endGen)
var311
}
else -> {
val var312 = getSequenceElems(history, 246, listOf(116,7,121,7,117), beginGen, endGen)
val var313 = matchExpr(var312[2].first, var312[2].second)
val var314 = Paren(var313, nextId(), beginGen, endGen)
var314
}
}
return var268
}

fun matchLiteral(beginGen: Int, endGen: Int): Literal {
val var315 = history[endGen].findByBeginGenOpt(219, 1, beginGen)
val var316 = history[endGen].findByBeginGenOpt(241, 1, beginGen)
val var317 = history[endGen].findByBeginGenOpt(244, 1, beginGen)
check(hasSingleTrue(var315 != null, var316 != null, var317 != null))
val var318 = when {
var315 != null -> {
val var319 = matchStringLiteral(beginGen, endGen)
var319
}
var316 != null -> {
val var320 = matchBooleanLiteral(beginGen, endGen)
var320
}
else -> {
val var321 = matchNoneLiteral(beginGen, endGen)
var321
}
}
return var318
}

fun matchCallExpr(beginGen: Int, endGen: Int): CallExpr {
val var322 = getSequenceElems(history, 113, listOf(63,7,114), beginGen, endGen)
val var323 = matchName(var322[0].first, var322[0].second)
val var324 = matchCallParams(var322[2].first, var322[2].second)
val var325 = CallExpr(var323, var324, nextId(), beginGen, endGen)
return var325
}

fun matchNamedExpr(beginGen: Int, endGen: Int): NamedExpr {
val var326 = getSequenceElems(history, 213, listOf(65,7,163,7,121), beginGen, endGen)
val var327 = matchSimpleName(var326[0].first, var326[0].second)
val var328 = matchExpr(var326[4].first, var326[4].second)
val var329 = NamedExpr(var327, var328, nextId(), beginGen, endGen)
return var329
}

fun matchNoneLiteral(beginGen: Int, endGen: Int): NoneLiteral {
val var330 = NoneLiteral(nextId(), beginGen, endGen)
return var330
}

fun matchVarRedefs(beginGen: Int, endGen: Int): VarRedefs {
val var331 = getSequenceElems(history, 380, listOf(371,7,381,385), beginGen, endGen)
val var332 = matchVarRedef(var331[2].first, var331[2].second)
val var333 = unrollRepeat0(history, 385, 387, 9, 386, var331[3].first, var331[3].second).map { k ->
val var334 = getSequenceElems(history, 388, listOf(7,155,7,381), k.first, k.second)
val var335 = matchVarRedef(var334[3].first, var334[3].second)
var335
}
val var336 = VarRedefs(listOf(var332) + var333, nextId(), beginGen, endGen)
return var336
}

fun matchVarRedef(beginGen: Int, endGen: Int): VarRedef {
val var337 = getSequenceElems(history, 382, listOf(65,383,7,183,7,121), beginGen, endGen)
val var338 = matchSimpleName(var337[0].first, var337[0].second)
val var339 = unrollRepeat1(history, 383, 97, 97, 384, var337[1].first, var337[1].second).map { k ->
val var340 = getSequenceElems(history, 98, listOf(7,99,7,65), k.first, k.second)
val var341 = matchSimpleName(var340[3].first, var340[3].second)
var341
}
val var342 = matchExpr(var337[5].first, var337[5].second)
val var343 = VarRedef(listOf(var338) + var339, var342, nextId(), beginGen, endGen)
return var343
}

fun matchTypeExpr(beginGen: Int, endGen: Int): TypeExpr {
val var344 = history[endGen].findByBeginGenOpt(127, 1, beginGen)
val var345 = history[endGen].findByBeginGenOpt(142, 1, beginGen)
check(hasSingleTrue(var344 != null, var345 != null))
val var346 = when {
var344 != null -> {
val var347 = matchNoUnionType(beginGen, endGen)
var347
}
else -> {
val var348 = matchUnionType(beginGen, endGen)
var348
}
}
return var346
}

fun matchListElem(beginGen: Int, endGen: Int): ListElem {
val var349 = history[endGen].findByBeginGenOpt(121, 1, beginGen)
val var350 = history[endGen].findByBeginGenOpt(196, 3, beginGen)
check(hasSingleTrue(var349 != null, var350 != null))
val var351 = when {
var349 != null -> {
val var352 = matchExpr(beginGen, endGen)
var352
}
else -> {
val var353 = getSequenceElems(history, 196, listOf(197,7,121), beginGen, endGen)
val var354 = matchExpr(var353[2].first, var353[2].second)
val var355 = EllipsisElem(var354, nextId(), beginGen, endGen)
var355
}
}
return var351
}

fun matchParamDef(beginGen: Int, endGen: Int): ParamDef {
val var356 = history[endGen].findByBeginGenOpt(182, 5, beginGen)
val var357 = history[endGen].findByBeginGenOpt(317, 7, beginGen)
check(hasSingleTrue(var356 != null, var357 != null))
val var358 = when {
var356 != null -> {
val var359 = getSequenceElems(history, 182, listOf(65,7,183,7,121), beginGen, endGen)
val var360 = matchSimpleName(var359[0].first, var359[0].second)
val var361 = matchExpr(var359[4].first, var359[4].second)
val var362 = ParamDef(var360, false, null, var361, nextId(), beginGen, endGen)
var362
}
else -> {
val var363 = getSequenceElems(history, 317, listOf(65,318,7,163,7,141,322), beginGen, endGen)
val var364 = matchSimpleName(var363[0].first, var363[0].second)
val var365 = history[var363[1].second].findByBeginGenOpt(100, 1, var363[1].first)
val var366 = history[var363[1].second].findByBeginGenOpt(319, 1, var363[1].first)
check(hasSingleTrue(var365 != null, var366 != null))
val var367 = when {
var365 != null -> null
else -> {
val var368 = getSequenceElems(history, 320, listOf(7,321), var363[1].first, var363[1].second)
source[var368[1].first]
}
}
val var369 = matchTypeExpr(var363[5].first, var363[5].second)
val var370 = history[var363[6].second].findByBeginGenOpt(100, 1, var363[6].first)
val var371 = history[var363[6].second].findByBeginGenOpt(323, 1, var363[6].first)
check(hasSingleTrue(var370 != null, var371 != null))
val var372 = when {
var370 != null -> null
else -> {
val var373 = getSequenceElems(history, 324, listOf(7,183,7,121), var363[6].first, var363[6].second)
val var374 = matchExpr(var373[3].first, var373[3].second)
var374
}
}
val var375 = ParamDef(var364, var367 != null, var369, var372, nextId(), beginGen, endGen)
var375
}
}
return var358
}

fun matchUnionType(beginGen: Int, endGen: Int): UnionType {
val var376 = getSequenceElems(history, 145, listOf(127,146), beginGen, endGen)
val var377 = matchNoUnionType(var376[0].first, var376[0].second)
val var378 = unrollRepeat1(history, 146, 147, 147, 150, var376[1].first, var376[1].second).map { k ->
val var379 = getSequenceElems(history, 148, listOf(7,149,7,127), k.first, k.second)
val var380 = matchNoUnionType(var379[3].first, var379[3].second)
var380
}
val var381 = UnionType(listOf(var377) + var378, nextId(), beginGen, endGen)
return var381
}

fun matchTargetDef(beginGen: Int, endGen: Int): TargetDef {
val var382 = getSequenceElems(history, 182, listOf(65,7,183,7,121), beginGen, endGen)
val var383 = matchSimpleName(var382[0].first, var382[0].second)
val var384 = matchExpr(var382[4].first, var382[4].second)
val var385 = TargetDef(var383, var384, nextId(), beginGen, endGen)
return var385
}

fun matchSuperClassDef(beginGen: Int, endGen: Int): SuperClassDef {
val var386 = getSequenceElems(history, 354, listOf(355,7,307,7,65,7,239,7,65,359,7,240), beginGen, endGen)
val var387 = matchSimpleName(var386[4].first, var386[4].second)
val var388 = matchSimpleName(var386[8].first, var386[8].second)
val var389 = unrollRepeat0(history, 359, 361, 9, 360, var386[9].first, var386[9].second).map { k ->
val var390 = getSequenceElems(history, 362, listOf(7,155,7,65), k.first, k.second)
val var391 = matchSimpleName(var390[3].first, var390[3].second)
var391
}
val var392 = SuperClassDef(var387, listOf(var388) + var389, nextId(), beginGen, endGen)
return var392
}

fun matchActionStmt(beginGen: Int, endGen: Int): ActionStmt {
val var393 = history[endGen].findByBeginGenOpt(112, 1, beginGen)
val var394 = history[endGen].findByBeginGenOpt(295, 1, beginGen)
check(hasSingleTrue(var393 != null, var394 != null))
val var395 = when {
var393 != null -> {
val var396 = matchCallExpr(beginGen, endGen)
var396
}
else -> {
val var397 = matchLetStmt(beginGen, endGen)
var397
}
}
return var395
}

fun matchLetStmt(beginGen: Int, endGen: Int): LetStmt {
val var398 = getSequenceElems(history, 296, listOf(297,7,65,7,183,7,301), beginGen, endGen)
val var399 = matchSimpleName(var398[2].first, var398[2].second)
val var400 = matchExpr(var398[6].first, var398[6].second)
val var401 = LetStmt(var399, var400, nextId(), beginGen, endGen)
return var401
}

fun matchCallParams(beginGen: Int, endGen: Int): CallParams {
val var402 = history[endGen].findByBeginGenOpt(115, 3, beginGen)
val var403 = history[endGen].findByBeginGenOpt(118, 6, beginGen)
val var404 = history[endGen].findByBeginGenOpt(178, 6, beginGen)
val var405 = history[endGen].findByBeginGenOpt(188, 10, beginGen)
check(hasSingleTrue(var402 != null, var403 != null, var404 != null, var405 != null))
val var406 = when {
var402 != null -> {
val var407 = CallParams(listOf(), listOf(), nextId(), beginGen, endGen)
var407
}
var403 != null -> {
val var408 = getSequenceElems(history, 118, listOf(116,7,119,175,7,117), beginGen, endGen)
val var409 = matchPositionalParams(var408[2].first, var408[2].second)
val var410 = CallParams(var409, listOf(), nextId(), beginGen, endGen)
var410
}
var404 != null -> {
val var411 = getSequenceElems(history, 178, listOf(116,7,179,175,7,117), beginGen, endGen)
val var412 = matchNamedParams(var411[2].first, var411[2].second)
val var413 = CallParams(listOf(), var412, nextId(), beginGen, endGen)
var413
}
else -> {
val var414 = getSequenceElems(history, 188, listOf(116,7,119,7,155,7,179,175,7,117), beginGen, endGen)
val var415 = matchPositionalParams(var414[2].first, var414[2].second)
val var416 = matchNamedParams(var414[6].first, var414[6].second)
val var417 = CallParams(var415, var416, nextId(), beginGen, endGen)
var417
}
}
return var406
}

fun matchPositionalParams(beginGen: Int, endGen: Int): List<Expr> {
val var418 = getSequenceElems(history, 120, listOf(121,171), beginGen, endGen)
val var419 = matchExpr(var418[0].first, var418[0].second)
val var420 = unrollRepeat0(history, 171, 173, 9, 172, var418[1].first, var418[1].second).map { k ->
val var421 = getSequenceElems(history, 174, listOf(7,155,7,121), k.first, k.second)
val var422 = matchExpr(var421[3].first, var421[3].second)
var422
}
return listOf(var419) + var420
}

fun matchNamedParams(beginGen: Int, endGen: Int): List<NamedParam> {
val var423 = getSequenceElems(history, 180, listOf(181,184), beginGen, endGen)
val var424 = matchNamedParam(var423[0].first, var423[0].second)
val var425 = unrollRepeat0(history, 184, 186, 9, 185, var423[1].first, var423[1].second).map { k ->
val var426 = getSequenceElems(history, 187, listOf(7,155,7,181), k.first, k.second)
val var427 = matchNamedParam(var426[3].first, var426[3].second)
var427
}
return listOf(var424) + var425
}

fun matchNamedParam(beginGen: Int, endGen: Int): NamedParam {
val var428 = getSequenceElems(history, 182, listOf(65,7,183,7,121), beginGen, endGen)
val var429 = matchSimpleName(var428[0].first, var428[0].second)
val var430 = matchExpr(var428[4].first, var428[4].second)
val var431 = NamedParam(var429, var430, nextId(), beginGen, endGen)
return var431
}

fun matchNamedTupleType(beginGen: Int, endGen: Int): NamedTupleType {
val var432 = getSequenceElems(history, 160, listOf(116,7,161,164,7,117), beginGen, endGen)
val var433 = matchNamedType(var432[2].first, var432[2].second)
val var434 = unrollRepeat0(history, 164, 166, 9, 165, var432[3].first, var432[3].second).map { k ->
val var435 = getSequenceElems(history, 167, listOf(7,155,7,161), k.first, k.second)
val var436 = matchNamedType(var435[3].first, var435[3].second)
var436
}
val var437 = NamedTupleType(listOf(var433) + var434, nextId(), beginGen, endGen)
return var437
}

fun matchNamedType(beginGen: Int, endGen: Int): NamedType {
val var438 = getSequenceElems(history, 162, listOf(65,7,163,7,141), beginGen, endGen)
val var439 = matchSimpleName(var438[0].first, var438[0].second)
val var440 = matchTypeExpr(var438[4].first, var438[4].second)
val var441 = NamedType(var439, var440, nextId(), beginGen, endGen)
return var441
}

fun matchBooleanLiteral(beginGen: Int, endGen: Int): BooleanLiteral {
val var442 = history[endGen].findByBeginGenOpt(77, 1, beginGen)
val var443 = history[endGen].findByBeginGenOpt(82, 1, beginGen)
check(hasSingleTrue(var442 != null, var443 != null))
val var444 = when {
var442 != null -> {
val var445 = BooleanLiteral(true, nextId(), beginGen, endGen)
var445
}
else -> {
val var446 = BooleanLiteral(false, nextId(), beginGen, endGen)
var446
}
}
return var444
}

}
