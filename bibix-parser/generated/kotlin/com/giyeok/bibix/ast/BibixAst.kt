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
  val isOptional: Boolean,
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
val var191 = getSequenceElems(history, 370, listOf(371,7,65,318,376,322), beginGen, endGen)
val var192 = matchSimpleName(var191[2].first, var191[2].second)
val var193 = history[var191[3].second].findByBeginGenOpt(100, 1, var191[3].first)
val var194 = history[var191[3].second].findByBeginGenOpt(319, 1, var191[3].first)
check(hasSingleTrue(var193 != null, var194 != null))
val var195 = when {
var193 != null -> null
else -> {
val var196 = getSequenceElems(history, 320, listOf(7,321), var191[3].first, var191[3].second)
source[var196[1].first]
}
}
val var197 = history[var191[4].second].findByBeginGenOpt(100, 1, var191[4].first)
val var198 = history[var191[4].second].findByBeginGenOpt(377, 1, var191[4].first)
check(hasSingleTrue(var197 != null, var198 != null))
val var199 = when {
var197 != null -> null
else -> {
val var200 = getSequenceElems(history, 378, listOf(7,163,7,141), var191[4].first, var191[4].second)
val var201 = matchTypeExpr(var200[3].first, var200[3].second)
var201
}
}
val var202 = history[var191[5].second].findByBeginGenOpt(100, 1, var191[5].first)
val var203 = history[var191[5].second].findByBeginGenOpt(323, 1, var191[5].first)
check(hasSingleTrue(var202 != null, var203 != null))
val var204 = when {
var202 != null -> null
else -> {
val var205 = getSequenceElems(history, 324, listOf(7,183,7,121), var191[5].first, var191[5].second)
val var206 = matchExpr(var205[3].first, var205[3].second)
var206
}
}
val var207 = VarDef(var192, var195 != null, var199, var204, nextId(), beginGen, endGen)
return var207
}

fun matchNamespaceDef(beginGen: Int, endGen: Int): NamespaceDef {
val var208 = getSequenceElems(history, 256, listOf(65,257,7,239,274,7,240), beginGen, endGen)
val var209 = matchSimpleName(var208[0].first, var208[0].second)
val var210 = history[var208[1].second].findByBeginGenOpt(100, 1, var208[1].first)
val var211 = history[var208[1].second].findByBeginGenOpt(258, 1, var208[1].first)
check(hasSingleTrue(var210 != null, var211 != null))
val var212 = when {
var210 != null -> null
else -> {
val var213 = getSequenceElems(history, 259, listOf(7,260,7,264), var208[1].first, var208[1].second)
val var214 = matchPathDirection(var213[3].first, var213[3].second)
var214
}
}
val var216 = history[var208[4].second].findByBeginGenOpt(100, 1, var208[4].first)
val var217 = history[var208[4].second].findByBeginGenOpt(275, 1, var208[4].first)
check(hasSingleTrue(var216 != null, var217 != null))
val var218 = when {
var216 != null -> null
else -> {
val var219 = getSequenceElems(history, 276, listOf(7,101), var208[4].first, var208[4].second)
val var220 = matchDefs(var219[1].first, var219[1].second)
var220
}
}
val var215 = var218
val var221 = NamespaceDef(var209, var212, (var215 ?: listOf()), nextId(), beginGen, endGen)
return var221
}

fun matchPathDirection(beginGen: Int, endGen: Int): PathDirection {
val var222 = history[endGen].findByBeginGenOpt(219, 1, beginGen)
val var223 = history[endGen].findByBeginGenOpt(265, 1, beginGen)
check(hasSingleTrue(var222 != null, var223 != null))
val var224 = when {
var222 != null -> {
val var225 = matchStringLiteral(beginGen, endGen)
var225
}
else -> {
val var226 = matchPath(beginGen, endGen)
var226
}
}
return var224
}

fun matchPath(beginGen: Int, endGen: Int): Path {
val var227 = getSequenceElems(history, 268, listOf(269,270), beginGen, endGen)
val var228 = matchPathToken(var227[0].first, var227[0].second)
val var229 = unrollRepeat0(history, 270, 272, 9, 271, var227[1].first, var227[1].second).map { k ->
val var230 = getSequenceElems(history, 273, listOf(7,19,7,269), k.first, k.second)
val var231 = matchPathToken(var230[3].first, var230[3].second)
var231
}
val var232 = Path(listOf(var228) + var229, nextId(), beginGen, endGen)
return var232
}

fun matchStringLiteral(beginGen: Int, endGen: Int): StringLiteral {
val var233 = getSequenceElems(history, 220, listOf(221,222,221), beginGen, endGen)
val var234 = unrollRepeat0(history, 222, 224, 9, 223, var233[1].first, var233[1].second).map { k ->
val var235 = matchStringElem(k.first, k.second)
var235
}
val var236 = StringLiteral(var234, nextId(), beginGen, endGen)
return var236
}

fun matchStringElem(beginGen: Int, endGen: Int): StringElem {
val var237 = history[endGen].findByBeginGenOpt(227, 1, beginGen)
val var238 = history[endGen].findByBeginGenOpt(229, 1, beginGen)
val var239 = history[endGen].findByBeginGenOpt(233, 1, beginGen)
check(hasSingleTrue(var237 != null, var238 != null, var239 != null))
val var240 = when {
var237 != null -> {
val var241 = JustChar(source[beginGen], nextId(), beginGen, endGen)
var241
}
var238 != null -> {
val var242 = matchEscapeChar(beginGen, endGen)
var242
}
else -> {
val var243 = matchStringExpr(beginGen, endGen)
var243
}
}
return var240
}

fun matchEscapeChar(beginGen: Int, endGen: Int): EscapeChar {
val var244 = getSequenceElems(history, 230, listOf(231,232), beginGen, endGen)
val var245 = EscapeChar(source[var244[1].first], nextId(), beginGen, endGen)
return var245
}

fun matchStringExpr(beginGen: Int, endGen: Int): StringExpr {
val var246 = history[endGen].findByBeginGenOpt(234, 1, beginGen)
val var247 = history[endGen].findByBeginGenOpt(238, 6, beginGen)
check(hasSingleTrue(var246 != null, var247 != null))
val var248 = when {
var246 != null -> {
val var249 = getSequenceElems(history, 236, listOf(237,65), beginGen, endGen)
val var250 = matchSimpleName(var249[1].first, var249[1].second)
val var251 = SimpleExpr(var250, nextId(), beginGen, endGen)
var251
}
else -> {
val var252 = getSequenceElems(history, 238, listOf(237,239,7,121,7,240), beginGen, endGen)
val var253 = matchExpr(var252[3].first, var252[3].second)
val var254 = ComplexExpr(var253, nextId(), beginGen, endGen)
var254
}
}
return var248
}

fun matchPathToken(beginGen: Int, endGen: Int): String {
val var255 = unrollRepeat1(history, 56, 57, 57, 58, beginGen, endGen).map { k ->
source[k.first]
}
return var255.joinToString("") { it.toString() }
}

fun matchEnumDef(beginGen: Int, endGen: Int): EnumDef {
val var256 = getSequenceElems(history, 364, listOf(365,7,65,7,239,7,65,359,175,7,240), beginGen, endGen)
val var257 = matchSimpleName(var256[2].first, var256[2].second)
val var258 = matchSimpleName(var256[6].first, var256[6].second)
val var259 = unrollRepeat0(history, 359, 361, 9, 360, var256[7].first, var256[7].second).map { k ->
val var260 = getSequenceElems(history, 362, listOf(7,155,7,65), k.first, k.second)
val var261 = matchSimpleName(var260[3].first, var260[3].second)
var261
}
val var262 = EnumDef(var257, listOf(var258) + var259, nextId(), beginGen, endGen)
return var262
}

fun matchPrimary(beginGen: Int, endGen: Int): Primary {
val var263 = history[endGen].findByBeginGenOpt(65, 1, beginGen)
val var264 = history[endGen].findByBeginGenOpt(112, 1, beginGen)
val var265 = history[endGen].findByBeginGenOpt(189, 5, beginGen)
val var266 = history[endGen].findByBeginGenOpt(190, 4, beginGen)
val var267 = history[endGen].findByBeginGenOpt(204, 8, beginGen)
val var268 = history[endGen].findByBeginGenOpt(208, 4, beginGen)
val var269 = history[endGen].findByBeginGenOpt(218, 1, beginGen)
val var270 = history[endGen].findByBeginGenOpt(245, 1, beginGen)
val var271 = history[endGen].findByBeginGenOpt(246, 5, beginGen)
check(hasSingleTrue(var263 != null, var264 != null, var265 != null, var266 != null, var267 != null, var268 != null, var269 != null, var270 != null, var271 != null))
val var272 = when {
var263 != null -> {
val var273 = matchSimpleName(beginGen, endGen)
val var274 = NameRef(var273, nextId(), beginGen, endGen)
var274
}
var264 != null -> {
val var275 = matchCallExpr(beginGen, endGen)
var275
}
var265 != null -> {
val var276 = getSequenceElems(history, 189, listOf(111,7,99,7,65), beginGen, endGen)
val var277 = matchPrimary(var276[0].first, var276[0].second)
val var278 = matchSimpleName(var276[4].first, var276[4].second)
val var279 = MemberAccess(var277, var278, nextId(), beginGen, endGen)
var279
}
var266 != null -> {
val var281 = getSequenceElems(history, 190, listOf(191,192,7,203), beginGen, endGen)
val var282 = history[var281[1].second].findByBeginGenOpt(100, 1, var281[1].first)
val var283 = history[var281[1].second].findByBeginGenOpt(193, 1, var281[1].first)
check(hasSingleTrue(var282 != null, var283 != null))
val var284 = when {
var282 != null -> null
else -> {
val var285 = getSequenceElems(history, 194, listOf(7,195,199,175), var281[1].first, var281[1].second)
val var286 = matchListElem(var285[1].first, var285[1].second)
val var287 = unrollRepeat0(history, 199, 201, 9, 200, var285[2].first, var285[2].second).map { k ->
val var288 = getSequenceElems(history, 202, listOf(7,155,7,195), k.first, k.second)
val var289 = matchListElem(var288[3].first, var288[3].second)
var289
}
listOf(var286) + var287
}
}
val var280 = var284
val var290 = ListExpr((var280 ?: listOf()), nextId(), beginGen, endGen)
var290
}
var267 != null -> {
val var291 = getSequenceElems(history, 204, listOf(116,7,121,7,155,205,7,117), beginGen, endGen)
val var292 = matchExpr(var291[2].first, var291[2].second)
val var294 = history[var291[5].second].findByBeginGenOpt(100, 1, var291[5].first)
val var295 = history[var291[5].second].findByBeginGenOpt(206, 1, var291[5].first)
check(hasSingleTrue(var294 != null, var295 != null))
val var296 = when {
var294 != null -> null
else -> {
val var297 = getSequenceElems(history, 207, listOf(7,121,171,175), var291[5].first, var291[5].second)
val var298 = matchExpr(var297[1].first, var297[1].second)
val var299 = unrollRepeat0(history, 171, 173, 9, 172, var297[2].first, var297[2].second).map { k ->
val var300 = getSequenceElems(history, 174, listOf(7,155,7,121), k.first, k.second)
val var301 = matchExpr(var300[3].first, var300[3].second)
var301
}
listOf(var298) + var299
}
}
val var293 = var296
val var302 = TupleExpr(listOf(var292) + (var293 ?: listOf()), nextId(), beginGen, endGen)
var302
}
var268 != null -> {
val var304 = getSequenceElems(history, 208, listOf(116,209,7,117), beginGen, endGen)
val var305 = history[var304[1].second].findByBeginGenOpt(100, 1, var304[1].first)
val var306 = history[var304[1].second].findByBeginGenOpt(210, 1, var304[1].first)
check(hasSingleTrue(var305 != null, var306 != null))
val var307 = when {
var305 != null -> null
else -> {
val var308 = getSequenceElems(history, 211, listOf(7,212,214,175), var304[1].first, var304[1].second)
val var309 = matchNamedExpr(var308[1].first, var308[1].second)
val var310 = unrollRepeat0(history, 214, 216, 9, 215, var308[2].first, var308[2].second).map { k ->
val var311 = getSequenceElems(history, 217, listOf(7,155,7,212), k.first, k.second)
val var312 = matchNamedExpr(var311[3].first, var311[3].second)
var312
}
listOf(var309) + var310
}
}
val var303 = var307
val var313 = NamedTupleExpr((var303 ?: listOf()), nextId(), beginGen, endGen)
var313
}
var269 != null -> {
val var314 = matchLiteral(beginGen, endGen)
var314
}
var270 != null -> {
val var315 = This(nextId(), beginGen, endGen)
var315
}
else -> {
val var316 = getSequenceElems(history, 246, listOf(116,7,121,7,117), beginGen, endGen)
val var317 = matchExpr(var316[2].first, var316[2].second)
val var318 = Paren(var317, nextId(), beginGen, endGen)
var318
}
}
return var272
}

fun matchLiteral(beginGen: Int, endGen: Int): Literal {
val var319 = history[endGen].findByBeginGenOpt(219, 1, beginGen)
val var320 = history[endGen].findByBeginGenOpt(241, 1, beginGen)
val var321 = history[endGen].findByBeginGenOpt(244, 1, beginGen)
check(hasSingleTrue(var319 != null, var320 != null, var321 != null))
val var322 = when {
var319 != null -> {
val var323 = matchStringLiteral(beginGen, endGen)
var323
}
var320 != null -> {
val var324 = matchBooleanLiteral(beginGen, endGen)
var324
}
else -> {
val var325 = matchNoneLiteral(beginGen, endGen)
var325
}
}
return var322
}

fun matchCallExpr(beginGen: Int, endGen: Int): CallExpr {
val var326 = getSequenceElems(history, 113, listOf(63,7,114), beginGen, endGen)
val var327 = matchName(var326[0].first, var326[0].second)
val var328 = matchCallParams(var326[2].first, var326[2].second)
val var329 = CallExpr(var327, var328, nextId(), beginGen, endGen)
return var329
}

fun matchNamedExpr(beginGen: Int, endGen: Int): NamedExpr {
val var330 = getSequenceElems(history, 213, listOf(65,7,163,7,121), beginGen, endGen)
val var331 = matchSimpleName(var330[0].first, var330[0].second)
val var332 = matchExpr(var330[4].first, var330[4].second)
val var333 = NamedExpr(var331, var332, nextId(), beginGen, endGen)
return var333
}

fun matchNoneLiteral(beginGen: Int, endGen: Int): NoneLiteral {
val var334 = NoneLiteral(nextId(), beginGen, endGen)
return var334
}

fun matchVarRedefs(beginGen: Int, endGen: Int): VarRedefs {
val var335 = getSequenceElems(history, 380, listOf(371,7,381,385), beginGen, endGen)
val var336 = matchVarRedef(var335[2].first, var335[2].second)
val var337 = unrollRepeat0(history, 385, 387, 9, 386, var335[3].first, var335[3].second).map { k ->
val var338 = getSequenceElems(history, 388, listOf(7,155,7,381), k.first, k.second)
val var339 = matchVarRedef(var338[3].first, var338[3].second)
var339
}
val var340 = VarRedefs(listOf(var336) + var337, nextId(), beginGen, endGen)
return var340
}

fun matchVarRedef(beginGen: Int, endGen: Int): VarRedef {
val var341 = getSequenceElems(history, 382, listOf(65,383,7,183,7,121), beginGen, endGen)
val var342 = matchSimpleName(var341[0].first, var341[0].second)
val var343 = unrollRepeat1(history, 383, 97, 97, 384, var341[1].first, var341[1].second).map { k ->
val var344 = getSequenceElems(history, 98, listOf(7,99,7,65), k.first, k.second)
val var345 = matchSimpleName(var344[3].first, var344[3].second)
var345
}
val var346 = matchExpr(var341[5].first, var341[5].second)
val var347 = VarRedef(listOf(var342) + var343, var346, nextId(), beginGen, endGen)
return var347
}

fun matchTypeExpr(beginGen: Int, endGen: Int): TypeExpr {
val var348 = history[endGen].findByBeginGenOpt(127, 1, beginGen)
val var349 = history[endGen].findByBeginGenOpt(142, 1, beginGen)
check(hasSingleTrue(var348 != null, var349 != null))
val var350 = when {
var348 != null -> {
val var351 = matchNoUnionType(beginGen, endGen)
var351
}
else -> {
val var352 = matchUnionType(beginGen, endGen)
var352
}
}
return var350
}

fun matchListElem(beginGen: Int, endGen: Int): ListElem {
val var353 = history[endGen].findByBeginGenOpt(121, 1, beginGen)
val var354 = history[endGen].findByBeginGenOpt(196, 3, beginGen)
check(hasSingleTrue(var353 != null, var354 != null))
val var355 = when {
var353 != null -> {
val var356 = matchExpr(beginGen, endGen)
var356
}
else -> {
val var357 = getSequenceElems(history, 196, listOf(197,7,121), beginGen, endGen)
val var358 = matchExpr(var357[2].first, var357[2].second)
val var359 = EllipsisElem(var358, nextId(), beginGen, endGen)
var359
}
}
return var355
}

fun matchParamDef(beginGen: Int, endGen: Int): ParamDef {
val var360 = history[endGen].findByBeginGenOpt(182, 5, beginGen)
val var361 = history[endGen].findByBeginGenOpt(317, 7, beginGen)
check(hasSingleTrue(var360 != null, var361 != null))
val var362 = when {
var360 != null -> {
val var363 = getSequenceElems(history, 182, listOf(65,7,183,7,121), beginGen, endGen)
val var364 = matchSimpleName(var363[0].first, var363[0].second)
val var365 = matchExpr(var363[4].first, var363[4].second)
val var366 = ParamDef(var364, false, null, var365, nextId(), beginGen, endGen)
var366
}
else -> {
val var367 = getSequenceElems(history, 317, listOf(65,318,7,163,7,141,322), beginGen, endGen)
val var368 = matchSimpleName(var367[0].first, var367[0].second)
val var369 = history[var367[1].second].findByBeginGenOpt(100, 1, var367[1].first)
val var370 = history[var367[1].second].findByBeginGenOpt(319, 1, var367[1].first)
check(hasSingleTrue(var369 != null, var370 != null))
val var371 = when {
var369 != null -> null
else -> {
val var372 = getSequenceElems(history, 320, listOf(7,321), var367[1].first, var367[1].second)
source[var372[1].first]
}
}
val var373 = matchTypeExpr(var367[5].first, var367[5].second)
val var374 = history[var367[6].second].findByBeginGenOpt(100, 1, var367[6].first)
val var375 = history[var367[6].second].findByBeginGenOpt(323, 1, var367[6].first)
check(hasSingleTrue(var374 != null, var375 != null))
val var376 = when {
var374 != null -> null
else -> {
val var377 = getSequenceElems(history, 324, listOf(7,183,7,121), var367[6].first, var367[6].second)
val var378 = matchExpr(var377[3].first, var377[3].second)
var378
}
}
val var379 = ParamDef(var368, var371 != null, var373, var376, nextId(), beginGen, endGen)
var379
}
}
return var362
}

fun matchUnionType(beginGen: Int, endGen: Int): UnionType {
val var380 = getSequenceElems(history, 145, listOf(127,146), beginGen, endGen)
val var381 = matchNoUnionType(var380[0].first, var380[0].second)
val var382 = unrollRepeat1(history, 146, 147, 147, 150, var380[1].first, var380[1].second).map { k ->
val var383 = getSequenceElems(history, 148, listOf(7,149,7,127), k.first, k.second)
val var384 = matchNoUnionType(var383[3].first, var383[3].second)
var384
}
val var385 = UnionType(listOf(var381) + var382, nextId(), beginGen, endGen)
return var385
}

fun matchTargetDef(beginGen: Int, endGen: Int): TargetDef {
val var386 = getSequenceElems(history, 182, listOf(65,7,183,7,121), beginGen, endGen)
val var387 = matchSimpleName(var386[0].first, var386[0].second)
val var388 = matchExpr(var386[4].first, var386[4].second)
val var389 = TargetDef(var387, var388, nextId(), beginGen, endGen)
return var389
}

fun matchSuperClassDef(beginGen: Int, endGen: Int): SuperClassDef {
val var390 = getSequenceElems(history, 354, listOf(355,7,307,7,65,7,239,7,65,359,7,240), beginGen, endGen)
val var391 = matchSimpleName(var390[4].first, var390[4].second)
val var392 = matchSimpleName(var390[8].first, var390[8].second)
val var393 = unrollRepeat0(history, 359, 361, 9, 360, var390[9].first, var390[9].second).map { k ->
val var394 = getSequenceElems(history, 362, listOf(7,155,7,65), k.first, k.second)
val var395 = matchSimpleName(var394[3].first, var394[3].second)
var395
}
val var396 = SuperClassDef(var391, listOf(var392) + var393, nextId(), beginGen, endGen)
return var396
}

fun matchActionStmt(beginGen: Int, endGen: Int): ActionStmt {
val var397 = history[endGen].findByBeginGenOpt(112, 1, beginGen)
val var398 = history[endGen].findByBeginGenOpt(295, 1, beginGen)
check(hasSingleTrue(var397 != null, var398 != null))
val var399 = when {
var397 != null -> {
val var400 = matchCallExpr(beginGen, endGen)
var400
}
else -> {
val var401 = matchLetStmt(beginGen, endGen)
var401
}
}
return var399
}

fun matchLetStmt(beginGen: Int, endGen: Int): LetStmt {
val var402 = getSequenceElems(history, 296, listOf(297,7,65,7,183,7,301), beginGen, endGen)
val var403 = matchSimpleName(var402[2].first, var402[2].second)
val var404 = matchExpr(var402[6].first, var402[6].second)
val var405 = LetStmt(var403, var404, nextId(), beginGen, endGen)
return var405
}

fun matchCallParams(beginGen: Int, endGen: Int): CallParams {
val var406 = history[endGen].findByBeginGenOpt(115, 3, beginGen)
val var407 = history[endGen].findByBeginGenOpt(118, 6, beginGen)
val var408 = history[endGen].findByBeginGenOpt(178, 6, beginGen)
val var409 = history[endGen].findByBeginGenOpt(188, 10, beginGen)
check(hasSingleTrue(var406 != null, var407 != null, var408 != null, var409 != null))
val var410 = when {
var406 != null -> {
val var411 = CallParams(listOf(), listOf(), nextId(), beginGen, endGen)
var411
}
var407 != null -> {
val var412 = getSequenceElems(history, 118, listOf(116,7,119,175,7,117), beginGen, endGen)
val var413 = matchPositionalParams(var412[2].first, var412[2].second)
val var414 = CallParams(var413, listOf(), nextId(), beginGen, endGen)
var414
}
var408 != null -> {
val var415 = getSequenceElems(history, 178, listOf(116,7,179,175,7,117), beginGen, endGen)
val var416 = matchNamedParams(var415[2].first, var415[2].second)
val var417 = CallParams(listOf(), var416, nextId(), beginGen, endGen)
var417
}
else -> {
val var418 = getSequenceElems(history, 188, listOf(116,7,119,7,155,7,179,175,7,117), beginGen, endGen)
val var419 = matchPositionalParams(var418[2].first, var418[2].second)
val var420 = matchNamedParams(var418[6].first, var418[6].second)
val var421 = CallParams(var419, var420, nextId(), beginGen, endGen)
var421
}
}
return var410
}

fun matchPositionalParams(beginGen: Int, endGen: Int): List<Expr> {
val var422 = getSequenceElems(history, 120, listOf(121,171), beginGen, endGen)
val var423 = matchExpr(var422[0].first, var422[0].second)
val var424 = unrollRepeat0(history, 171, 173, 9, 172, var422[1].first, var422[1].second).map { k ->
val var425 = getSequenceElems(history, 174, listOf(7,155,7,121), k.first, k.second)
val var426 = matchExpr(var425[3].first, var425[3].second)
var426
}
return listOf(var423) + var424
}

fun matchNamedParams(beginGen: Int, endGen: Int): List<NamedParam> {
val var427 = getSequenceElems(history, 180, listOf(181,184), beginGen, endGen)
val var428 = matchNamedParam(var427[0].first, var427[0].second)
val var429 = unrollRepeat0(history, 184, 186, 9, 185, var427[1].first, var427[1].second).map { k ->
val var430 = getSequenceElems(history, 187, listOf(7,155,7,181), k.first, k.second)
val var431 = matchNamedParam(var430[3].first, var430[3].second)
var431
}
return listOf(var428) + var429
}

fun matchNamedParam(beginGen: Int, endGen: Int): NamedParam {
val var432 = getSequenceElems(history, 182, listOf(65,7,183,7,121), beginGen, endGen)
val var433 = matchSimpleName(var432[0].first, var432[0].second)
val var434 = matchExpr(var432[4].first, var432[4].second)
val var435 = NamedParam(var433, var434, nextId(), beginGen, endGen)
return var435
}

fun matchNamedTupleType(beginGen: Int, endGen: Int): NamedTupleType {
val var436 = getSequenceElems(history, 160, listOf(116,7,161,164,7,117), beginGen, endGen)
val var437 = matchNamedType(var436[2].first, var436[2].second)
val var438 = unrollRepeat0(history, 164, 166, 9, 165, var436[3].first, var436[3].second).map { k ->
val var439 = getSequenceElems(history, 167, listOf(7,155,7,161), k.first, k.second)
val var440 = matchNamedType(var439[3].first, var439[3].second)
var440
}
val var441 = NamedTupleType(listOf(var437) + var438, nextId(), beginGen, endGen)
return var441
}

fun matchNamedType(beginGen: Int, endGen: Int): NamedType {
val var442 = getSequenceElems(history, 162, listOf(65,7,163,7,141), beginGen, endGen)
val var443 = matchSimpleName(var442[0].first, var442[0].second)
val var444 = matchTypeExpr(var442[4].first, var442[4].second)
val var445 = NamedType(var443, var444, nextId(), beginGen, endGen)
return var445
}

fun matchBooleanLiteral(beginGen: Int, endGen: Int): BooleanLiteral {
val var446 = history[endGen].findByBeginGenOpt(77, 1, beginGen)
val var447 = history[endGen].findByBeginGenOpt(82, 1, beginGen)
check(hasSingleTrue(var446 != null, var447 != null))
val var448 = when {
var446 != null -> {
val var449 = BooleanLiteral(true, nextId(), beginGen, endGen)
var449
}
else -> {
val var450 = BooleanLiteral(false, nextId(), beginGen, endGen)
var450
}
}
return var448
}

}
