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
val var11 = getSequenceElems(history, 102, listOf(103,385), beginGen, endGen)
val var12 = matchDef(var11[0].first, var11[0].second)
val var13 = unrollRepeat0(history, 385, 387, 9, 386, var11[1].first, var11[1].second).map { k ->
val var14 = getSequenceElems(history, 388, listOf(7,103), k.first, k.second)
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
val var122 = history[endGen].findByBeginGenOpt(383, 1, beginGen)
check(hasSingleTrue(var120 != null, var121 != null, var122 != null))
val var123 = when {
var120 != null -> BuildRuleMod.Singleton
var121 != null -> BuildRuleMod.Synchronized
else -> BuildRuleMod.NoReuse
}
return var123
}

fun matchTypeParams(beginGen: Int, endGen: Int): TypeParams {
val var124 = getSequenceElems(history, 138, listOf(139,7,140,150,7,154), beginGen, endGen)
val var125 = matchTypeExpr(var124[2].first, var124[2].second)
val var126 = unrollRepeat0(history, 150, 152, 9, 151, var124[3].first, var124[3].second).map { k ->
val var127 = getSequenceElems(history, 153, listOf(7,148,7,140), k.first, k.second)
val var128 = matchTypeExpr(var127[3].first, var127[3].second)
var128
}
val var129 = TypeParams(listOf(var125) + var126, nextId(), beginGen, endGen)
return var129
}

fun matchActionDef(beginGen: Int, endGen: Int): ActionDef {
val var130 = getSequenceElems(history, 259, listOf(260,7,65,264,7,269), beginGen, endGen)
val var131 = matchSimpleName(var130[2].first, var130[2].second)
val var132 = history[var130[3].second].findByBeginGenOpt(100, 1, var130[3].first)
val var133 = history[var130[3].second].findByBeginGenOpt(265, 1, var130[3].first)
check(hasSingleTrue(var132 != null, var133 != null))
val var134 = when {
var132 != null -> null
else -> {
val var135 = getSequenceElems(history, 266, listOf(7,267), var130[3].first, var130[3].second)
val var136 = matchActionParams(var135[1].first, var135[1].second)
var136
}
}
val var137 = matchActionBody(var130[5].first, var130[5].second)
val var138 = ActionDef(var131, var134, var137, nextId(), beginGen, endGen)
return var138
}

fun matchActionParams(beginGen: Int, endGen: Int): String {
val var139 = getSequenceElems(history, 268, listOf(116,7,65,7,117), beginGen, endGen)
val var140 = matchSimpleName(var139[2].first, var139[2].second)
return var140
}

fun matchActionBody(beginGen: Int, endGen: Int): MultiCallActions {
val var141 = getSequenceElems(history, 270, listOf(143,271,7,149), beginGen, endGen)
val var142 = unrollRepeat1(history, 271, 272, 272, 283, var141[1].first, var141[1].second).map { k ->
val var143 = getSequenceElems(history, 273, listOf(7,274), k.first, k.second)
val var144 = matchActionStmt(var143[1].first, var143[1].second)
var144
}
val var145 = MultiCallActions(var142, nextId(), beginGen, endGen)
return var145
}

fun matchTupleType(beginGen: Int, endGen: Int): TupleType {
val var146 = getSequenceElems(history, 156, listOf(116,7,140,150,7,117), beginGen, endGen)
val var147 = matchTypeExpr(var146[2].first, var146[2].second)
val var148 = unrollRepeat0(history, 150, 152, 9, 151, var146[3].first, var146[3].second).map { k ->
val var149 = getSequenceElems(history, 153, listOf(7,148,7,140), k.first, k.second)
val var150 = matchTypeExpr(var149[3].first, var149[3].second)
var150
}
val var151 = TupleType(listOf(var147) + var148, nextId(), beginGen, endGen)
return var151
}

fun matchClassDef(beginGen: Int, endGen: Int): ClassDef {
val var152 = history[endGen].findByBeginGenOpt(285, 1, beginGen)
val var153 = history[endGen].findByBeginGenOpt(333, 1, beginGen)
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
val var157 = getSequenceElems(history, 286, listOf(287,7,65,7,291,309), beginGen, endGen)
val var158 = matchSimpleName(var157[2].first, var157[2].second)
val var159 = matchParamsDef(var157[4].first, var157[4].second)
val var161 = history[var157[5].second].findByBeginGenOpt(100, 1, var157[5].first)
val var162 = history[var157[5].second].findByBeginGenOpt(310, 1, var157[5].first)
check(hasSingleTrue(var161 != null, var162 != null))
val var163 = when {
var161 != null -> null
else -> {
val var164 = getSequenceElems(history, 311, listOf(7,312), var157[5].first, var157[5].second)
val var165 = matchClassBody(var164[1].first, var164[1].second)
var165
}
}
val var160 = var163
val var166 = DataClassDef(var158, var159, (var160 ?: listOf()), nextId(), beginGen, endGen)
return var166
}

fun matchMethodRef(beginGen: Int, endGen: Int): MethodRef {
val var167 = getSequenceElems(history, 327, listOf(63,7,161,7,63,328), beginGen, endGen)
val var168 = matchName(var167[0].first, var167[0].second)
val var169 = matchName(var167[4].first, var167[4].second)
val var170 = history[var167[5].second].findByBeginGenOpt(100, 1, var167[5].first)
val var171 = history[var167[5].second].findByBeginGenOpt(329, 1, var167[5].first)
check(hasSingleTrue(var170 != null, var171 != null))
val var172 = when {
var170 != null -> null
else -> {
val var173 = getSequenceElems(history, 330, listOf(7,161,7,65), var167[5].first, var167[5].second)
val var174 = matchSimpleName(var173[3].first, var173[3].second)
var174
}
}
val var175 = MethodRef(var168, var169, var172, nextId(), beginGen, endGen)
return var175
}

fun matchClassBody(beginGen: Int, endGen: Int): List<ClassBodyElem> {
val var176 = getSequenceElems(history, 313, listOf(143,314,7,149), beginGen, endGen)
val var177 = unrollRepeat0(history, 314, 316, 9, 315, var176[1].first, var176[1].second).map { k ->
val var178 = getSequenceElems(history, 317, listOf(7,318), k.first, k.second)
val var179 = matchClassBodyElem(var178[1].first, var178[1].second)
var179
}
return var177
}

fun matchClassBodyElem(beginGen: Int, endGen: Int): ClassBodyElem {
val var180 = history[endGen].findByBeginGenOpt(319, 1, beginGen)
val var181 = history[endGen].findByBeginGenOpt(331, 1, beginGen)
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
val var185 = getSequenceElems(history, 332, listOf(123,7,140,7,181,7,121), beginGen, endGen)
val var186 = matchTypeExpr(var185[2].first, var185[2].second)
val var187 = matchExpr(var185[6].first, var185[6].second)
val var188 = ClassCastDef(var186, var187, nextId(), beginGen, endGen)
return var188
}

fun matchVarDef(beginGen: Int, endGen: Int): VarDef {
val var189 = getSequenceElems(history, 350, listOf(351,7,65,356,302), beginGen, endGen)
val var190 = matchSimpleName(var189[2].first, var189[2].second)
val var191 = history[var189[3].second].findByBeginGenOpt(100, 1, var189[3].first)
val var192 = history[var189[3].second].findByBeginGenOpt(357, 1, var189[3].first)
check(hasSingleTrue(var191 != null, var192 != null))
val var193 = when {
var191 != null -> null
else -> {
val var194 = getSequenceElems(history, 358, listOf(7,161,7,140), var189[3].first, var189[3].second)
val var195 = matchTypeExpr(var194[3].first, var194[3].second)
var195
}
}
val var196 = history[var189[4].second].findByBeginGenOpt(100, 1, var189[4].first)
val var197 = history[var189[4].second].findByBeginGenOpt(303, 1, var189[4].first)
check(hasSingleTrue(var196 != null, var197 != null))
val var198 = when {
var196 != null -> null
else -> {
val var199 = getSequenceElems(history, 304, listOf(7,181,7,121), var189[4].first, var189[4].second)
val var200 = matchExpr(var199[3].first, var199[3].second)
var200
}
}
val var201 = VarDef(var190, var193, var198, nextId(), beginGen, endGen)
return var201
}

fun matchNamespaceDef(beginGen: Int, endGen: Int): NamespaceDef {
val var202 = getSequenceElems(history, 253, listOf(65,7,143,254,7,149), beginGen, endGen)
val var203 = matchSimpleName(var202[0].first, var202[0].second)
val var205 = history[var202[3].second].findByBeginGenOpt(100, 1, var202[3].first)
val var206 = history[var202[3].second].findByBeginGenOpt(255, 1, var202[3].first)
check(hasSingleTrue(var205 != null, var206 != null))
val var207 = when {
var205 != null -> null
else -> {
val var208 = getSequenceElems(history, 256, listOf(7,101), var202[3].first, var202[3].second)
val var209 = matchDefs(var208[1].first, var208[1].second)
var209
}
}
val var204 = var207
val var210 = NamespaceDef(var203, (var204 ?: listOf()), nextId(), beginGen, endGen)
return var210
}

fun matchEnumDef(beginGen: Int, endGen: Int): EnumDef {
val var211 = getSequenceElems(history, 344, listOf(345,7,65,7,143,7,65,339,173,7,149), beginGen, endGen)
val var212 = matchSimpleName(var211[2].first, var211[2].second)
val var213 = matchSimpleName(var211[6].first, var211[6].second)
val var214 = unrollRepeat0(history, 339, 341, 9, 340, var211[7].first, var211[7].second).map { k ->
val var215 = getSequenceElems(history, 342, listOf(7,148,7,65), k.first, k.second)
val var216 = matchSimpleName(var215[3].first, var215[3].second)
var216
}
val var217 = EnumDef(var212, listOf(var213) + var214, nextId(), beginGen, endGen)
return var217
}

fun matchPrimary(beginGen: Int, endGen: Int): Primary {
val var218 = history[endGen].findByBeginGenOpt(65, 1, beginGen)
val var219 = history[endGen].findByBeginGenOpt(112, 1, beginGen)
val var220 = history[endGen].findByBeginGenOpt(187, 5, beginGen)
val var221 = history[endGen].findByBeginGenOpt(188, 4, beginGen)
val var222 = history[endGen].findByBeginGenOpt(202, 8, beginGen)
val var223 = history[endGen].findByBeginGenOpt(206, 4, beginGen)
val var224 = history[endGen].findByBeginGenOpt(216, 1, beginGen)
val var225 = history[endGen].findByBeginGenOpt(242, 1, beginGen)
val var226 = history[endGen].findByBeginGenOpt(243, 5, beginGen)
check(hasSingleTrue(var218 != null, var219 != null, var220 != null, var221 != null, var222 != null, var223 != null, var224 != null, var225 != null, var226 != null))
val var227 = when {
var218 != null -> {
val var228 = matchSimpleName(beginGen, endGen)
val var229 = NameRef(var228, nextId(), beginGen, endGen)
var229
}
var219 != null -> {
val var230 = matchCallExpr(beginGen, endGen)
var230
}
var220 != null -> {
val var231 = getSequenceElems(history, 187, listOf(111,7,99,7,65), beginGen, endGen)
val var232 = matchPrimary(var231[0].first, var231[0].second)
val var233 = matchSimpleName(var231[4].first, var231[4].second)
val var234 = MemberAccess(var232, var233, nextId(), beginGen, endGen)
var234
}
var221 != null -> {
val var236 = getSequenceElems(history, 188, listOf(189,190,7,201), beginGen, endGen)
val var237 = history[var236[1].second].findByBeginGenOpt(100, 1, var236[1].first)
val var238 = history[var236[1].second].findByBeginGenOpt(191, 1, var236[1].first)
check(hasSingleTrue(var237 != null, var238 != null))
val var239 = when {
var237 != null -> null
else -> {
val var240 = getSequenceElems(history, 192, listOf(7,193,197,173), var236[1].first, var236[1].second)
val var241 = matchListElem(var240[1].first, var240[1].second)
val var242 = unrollRepeat0(history, 197, 199, 9, 198, var240[2].first, var240[2].second).map { k ->
val var243 = getSequenceElems(history, 200, listOf(7,148,7,193), k.first, k.second)
val var244 = matchListElem(var243[3].first, var243[3].second)
var244
}
listOf(var241) + var242
}
}
val var235 = var239
val var245 = ListExpr((var235 ?: listOf()), nextId(), beginGen, endGen)
var245
}
var222 != null -> {
val var246 = getSequenceElems(history, 202, listOf(116,7,121,7,148,203,7,117), beginGen, endGen)
val var247 = matchExpr(var246[2].first, var246[2].second)
val var249 = history[var246[5].second].findByBeginGenOpt(100, 1, var246[5].first)
val var250 = history[var246[5].second].findByBeginGenOpt(204, 1, var246[5].first)
check(hasSingleTrue(var249 != null, var250 != null))
val var251 = when {
var249 != null -> null
else -> {
val var252 = getSequenceElems(history, 205, listOf(7,121,169,173), var246[5].first, var246[5].second)
val var253 = matchExpr(var252[1].first, var252[1].second)
val var254 = unrollRepeat0(history, 169, 171, 9, 170, var252[2].first, var252[2].second).map { k ->
val var255 = getSequenceElems(history, 172, listOf(7,148,7,121), k.first, k.second)
val var256 = matchExpr(var255[3].first, var255[3].second)
var256
}
listOf(var253) + var254
}
}
val var248 = var251
val var257 = TupleExpr(listOf(var247) + (var248 ?: listOf()), nextId(), beginGen, endGen)
var257
}
var223 != null -> {
val var259 = getSequenceElems(history, 206, listOf(116,207,7,117), beginGen, endGen)
val var260 = history[var259[1].second].findByBeginGenOpt(100, 1, var259[1].first)
val var261 = history[var259[1].second].findByBeginGenOpt(208, 1, var259[1].first)
check(hasSingleTrue(var260 != null, var261 != null))
val var262 = when {
var260 != null -> null
else -> {
val var263 = getSequenceElems(history, 209, listOf(7,210,212,173), var259[1].first, var259[1].second)
val var264 = matchNamedExpr(var263[1].first, var263[1].second)
val var265 = unrollRepeat0(history, 212, 214, 9, 213, var263[2].first, var263[2].second).map { k ->
val var266 = getSequenceElems(history, 215, listOf(7,148,7,210), k.first, k.second)
val var267 = matchNamedExpr(var266[3].first, var266[3].second)
var267
}
listOf(var264) + var265
}
}
val var258 = var262
val var268 = NamedTupleExpr((var258 ?: listOf()), nextId(), beginGen, endGen)
var268
}
var224 != null -> {
val var269 = matchLiteral(beginGen, endGen)
var269
}
var225 != null -> {
val var270 = This(nextId(), beginGen, endGen)
var270
}
else -> {
val var271 = getSequenceElems(history, 243, listOf(116,7,121,7,117), beginGen, endGen)
val var272 = matchExpr(var271[2].first, var271[2].second)
val var273 = Paren(var272, nextId(), beginGen, endGen)
var273
}
}
return var227
}

fun matchLiteral(beginGen: Int, endGen: Int): Literal {
val var274 = history[endGen].findByBeginGenOpt(217, 1, beginGen)
val var275 = history[endGen].findByBeginGenOpt(237, 1, beginGen)
val var276 = history[endGen].findByBeginGenOpt(240, 1, beginGen)
check(hasSingleTrue(var274 != null, var275 != null, var276 != null))
val var277 = when {
var274 != null -> {
val var278 = matchStringLiteral(beginGen, endGen)
var278
}
var275 != null -> {
val var279 = matchBooleanLiteral(beginGen, endGen)
var279
}
else -> {
val var280 = matchNoneLiteral(beginGen, endGen)
var280
}
}
return var277
}

fun matchStringLiteral(beginGen: Int, endGen: Int): StringLiteral {
val var281 = getSequenceElems(history, 218, listOf(219,220,219), beginGen, endGen)
val var282 = unrollRepeat0(history, 220, 222, 9, 221, var281[1].first, var281[1].second).map { k ->
val var283 = matchStringElem(k.first, k.second)
var283
}
val var284 = StringLiteral(var282, nextId(), beginGen, endGen)
return var284
}

fun matchCallExpr(beginGen: Int, endGen: Int): CallExpr {
val var285 = getSequenceElems(history, 113, listOf(63,7,114), beginGen, endGen)
val var286 = matchName(var285[0].first, var285[0].second)
val var287 = matchCallParams(var285[2].first, var285[2].second)
val var288 = CallExpr(var286, var287, nextId(), beginGen, endGen)
return var288
}

fun matchNoneLiteral(beginGen: Int, endGen: Int): NoneLiteral {
val var289 = NoneLiteral(nextId(), beginGen, endGen)
return var289
}

fun matchStringElem(beginGen: Int, endGen: Int): StringElem {
val var290 = history[endGen].findByBeginGenOpt(225, 1, beginGen)
val var291 = history[endGen].findByBeginGenOpt(227, 1, beginGen)
val var292 = history[endGen].findByBeginGenOpt(231, 1, beginGen)
check(hasSingleTrue(var290 != null, var291 != null, var292 != null))
val var293 = when {
var290 != null -> {
val var294 = JustChar(source[beginGen], nextId(), beginGen, endGen)
var294
}
var291 != null -> {
val var295 = matchEscapeChar(beginGen, endGen)
var295
}
else -> {
val var296 = matchStringExpr(beginGen, endGen)
var296
}
}
return var293
}

fun matchEscapeChar(beginGen: Int, endGen: Int): EscapeChar {
val var297 = getSequenceElems(history, 228, listOf(229,230), beginGen, endGen)
val var298 = EscapeChar(source[var297[1].first], nextId(), beginGen, endGen)
return var298
}

fun matchStringExpr(beginGen: Int, endGen: Int): StringExpr {
val var299 = history[endGen].findByBeginGenOpt(232, 1, beginGen)
val var300 = history[endGen].findByBeginGenOpt(236, 6, beginGen)
check(hasSingleTrue(var299 != null, var300 != null))
val var301 = when {
var299 != null -> {
val var302 = getSequenceElems(history, 234, listOf(235,65), beginGen, endGen)
val var303 = matchSimpleName(var302[1].first, var302[1].second)
val var304 = SimpleExpr(var303, nextId(), beginGen, endGen)
var304
}
else -> {
val var305 = getSequenceElems(history, 236, listOf(235,143,7,121,7,149), beginGen, endGen)
val var306 = matchExpr(var305[3].first, var305[3].second)
val var307 = ComplexExpr(var306, nextId(), beginGen, endGen)
var307
}
}
return var301
}

fun matchNamedExpr(beginGen: Int, endGen: Int): NamedExpr {
val var308 = getSequenceElems(history, 211, listOf(65,7,161,7,121), beginGen, endGen)
val var309 = matchSimpleName(var308[0].first, var308[0].second)
val var310 = matchExpr(var308[4].first, var308[4].second)
val var311 = NamedExpr(var309, var310, nextId(), beginGen, endGen)
return var311
}

fun matchVarRedefs(beginGen: Int, endGen: Int): VarRedefs {
val var312 = getSequenceElems(history, 360, listOf(351,7,361,365), beginGen, endGen)
val var313 = matchVarRedef(var312[2].first, var312[2].second)
val var314 = unrollRepeat0(history, 365, 367, 9, 366, var312[3].first, var312[3].second).map { k ->
val var315 = getSequenceElems(history, 368, listOf(7,148,7,361), k.first, k.second)
val var316 = matchVarRedef(var315[3].first, var315[3].second)
var316
}
val var317 = VarRedefs(listOf(var313) + var314, nextId(), beginGen, endGen)
return var317
}

fun matchVarRedef(beginGen: Int, endGen: Int): VarRedef {
val var318 = getSequenceElems(history, 362, listOf(65,363,7,181,7,121), beginGen, endGen)
val var319 = matchSimpleName(var318[0].first, var318[0].second)
val var320 = unrollRepeat1(history, 363, 97, 97, 364, var318[1].first, var318[1].second).map { k ->
val var321 = getSequenceElems(history, 98, listOf(7,99,7,65), k.first, k.second)
val var322 = matchSimpleName(var321[3].first, var321[3].second)
var322
}
val var323 = matchExpr(var318[5].first, var318[5].second)
val var324 = VarRedef(listOf(var319) + var320, var323, nextId(), beginGen, endGen)
return var324
}

fun matchTypeExpr(beginGen: Int, endGen: Int): TypeExpr {
val var325 = history[endGen].findByBeginGenOpt(127, 1, beginGen)
val var326 = history[endGen].findByBeginGenOpt(141, 1, beginGen)
check(hasSingleTrue(var325 != null, var326 != null))
val var327 = when {
var325 != null -> {
val var328 = matchNoUnionType(beginGen, endGen)
var328
}
else -> {
val var329 = matchUnionType(beginGen, endGen)
var329
}
}
return var327
}

fun matchListElem(beginGen: Int, endGen: Int): ListElem {
val var330 = history[endGen].findByBeginGenOpt(121, 1, beginGen)
val var331 = history[endGen].findByBeginGenOpt(194, 3, beginGen)
check(hasSingleTrue(var330 != null, var331 != null))
val var332 = when {
var330 != null -> {
val var333 = matchExpr(beginGen, endGen)
var333
}
else -> {
val var334 = getSequenceElems(history, 194, listOf(195,7,121), beginGen, endGen)
val var335 = matchExpr(var334[2].first, var334[2].second)
val var336 = EllipsisElem(var335, nextId(), beginGen, endGen)
var336
}
}
return var332
}

fun matchParamDef(beginGen: Int, endGen: Int): ParamDef {
val var337 = history[endGen].findByBeginGenOpt(180, 5, beginGen)
val var338 = history[endGen].findByBeginGenOpt(297, 7, beginGen)
check(hasSingleTrue(var337 != null, var338 != null))
val var339 = when {
var337 != null -> {
val var340 = getSequenceElems(history, 180, listOf(65,7,181,7,121), beginGen, endGen)
val var341 = matchSimpleName(var340[0].first, var340[0].second)
val var342 = matchExpr(var340[4].first, var340[4].second)
val var343 = ParamDef(var341, false, null, var342, nextId(), beginGen, endGen)
var343
}
else -> {
val var344 = getSequenceElems(history, 297, listOf(65,298,7,161,7,140,302), beginGen, endGen)
val var345 = matchSimpleName(var344[0].first, var344[0].second)
val var346 = history[var344[1].second].findByBeginGenOpt(100, 1, var344[1].first)
val var347 = history[var344[1].second].findByBeginGenOpt(299, 1, var344[1].first)
check(hasSingleTrue(var346 != null, var347 != null))
val var348 = when {
var346 != null -> null
else -> {
val var349 = getSequenceElems(history, 300, listOf(7,301), var344[1].first, var344[1].second)
source[var349[1].first]
}
}
val var350 = matchTypeExpr(var344[5].first, var344[5].second)
val var351 = history[var344[6].second].findByBeginGenOpt(100, 1, var344[6].first)
val var352 = history[var344[6].second].findByBeginGenOpt(303, 1, var344[6].first)
check(hasSingleTrue(var351 != null, var352 != null))
val var353 = when {
var351 != null -> null
else -> {
val var354 = getSequenceElems(history, 304, listOf(7,181,7,121), var344[6].first, var344[6].second)
val var355 = matchExpr(var354[3].first, var354[3].second)
var355
}
}
val var356 = ParamDef(var345, var348 != null, var350, var353, nextId(), beginGen, endGen)
var356
}
}
return var339
}

fun matchUnionType(beginGen: Int, endGen: Int): UnionType {
val var357 = getSequenceElems(history, 142, listOf(143,7,127,144,7,149), beginGen, endGen)
val var358 = matchNoUnionType(var357[2].first, var357[2].second)
val var359 = unrollRepeat0(history, 144, 146, 9, 145, var357[3].first, var357[3].second).map { k ->
val var360 = getSequenceElems(history, 147, listOf(7,148,7,127), k.first, k.second)
val var361 = matchNoUnionType(var360[3].first, var360[3].second)
var361
}
val var362 = UnionType(listOf(var358) + var359, nextId(), beginGen, endGen)
return var362
}

fun matchTargetDef(beginGen: Int, endGen: Int): TargetDef {
val var363 = getSequenceElems(history, 180, listOf(65,7,181,7,121), beginGen, endGen)
val var364 = matchSimpleName(var363[0].first, var363[0].second)
val var365 = matchExpr(var363[4].first, var363[4].second)
val var366 = TargetDef(var364, var365, nextId(), beginGen, endGen)
return var366
}

fun matchSuperClassDef(beginGen: Int, endGen: Int): SuperClassDef {
val var367 = getSequenceElems(history, 334, listOf(335,7,287,7,65,7,143,7,65,339,7,149), beginGen, endGen)
val var368 = matchSimpleName(var367[4].first, var367[4].second)
val var369 = matchSimpleName(var367[8].first, var367[8].second)
val var370 = unrollRepeat0(history, 339, 341, 9, 340, var367[9].first, var367[9].second).map { k ->
val var371 = getSequenceElems(history, 342, listOf(7,148,7,65), k.first, k.second)
val var372 = matchSimpleName(var371[3].first, var371[3].second)
var372
}
val var373 = SuperClassDef(var368, listOf(var369) + var370, nextId(), beginGen, endGen)
return var373
}

fun matchActionStmt(beginGen: Int, endGen: Int): ActionStmt {
val var374 = history[endGen].findByBeginGenOpt(112, 1, beginGen)
val var375 = history[endGen].findByBeginGenOpt(275, 1, beginGen)
check(hasSingleTrue(var374 != null, var375 != null))
val var376 = when {
var374 != null -> {
val var377 = matchCallExpr(beginGen, endGen)
var377
}
else -> {
val var378 = matchLetStmt(beginGen, endGen)
var378
}
}
return var376
}

fun matchLetStmt(beginGen: Int, endGen: Int): LetStmt {
val var379 = getSequenceElems(history, 276, listOf(277,7,65,7,181,7,281), beginGen, endGen)
val var380 = matchSimpleName(var379[2].first, var379[2].second)
val var381 = matchExpr(var379[6].first, var379[6].second)
val var382 = LetStmt(var380, var381, nextId(), beginGen, endGen)
return var382
}

fun matchCallParams(beginGen: Int, endGen: Int): CallParams {
val var383 = history[endGen].findByBeginGenOpt(115, 3, beginGen)
val var384 = history[endGen].findByBeginGenOpt(118, 6, beginGen)
val var385 = history[endGen].findByBeginGenOpt(176, 6, beginGen)
val var386 = history[endGen].findByBeginGenOpt(186, 10, beginGen)
check(hasSingleTrue(var383 != null, var384 != null, var385 != null, var386 != null))
val var387 = when {
var383 != null -> {
val var388 = CallParams(listOf(), listOf(), nextId(), beginGen, endGen)
var388
}
var384 != null -> {
val var389 = getSequenceElems(history, 118, listOf(116,7,119,173,7,117), beginGen, endGen)
val var390 = matchPositionalParams(var389[2].first, var389[2].second)
val var391 = CallParams(var390, listOf(), nextId(), beginGen, endGen)
var391
}
var385 != null -> {
val var392 = getSequenceElems(history, 176, listOf(116,7,177,173,7,117), beginGen, endGen)
val var393 = matchNamedParams(var392[2].first, var392[2].second)
val var394 = CallParams(listOf(), var393, nextId(), beginGen, endGen)
var394
}
else -> {
val var395 = getSequenceElems(history, 186, listOf(116,7,119,7,148,7,177,173,7,117), beginGen, endGen)
val var396 = matchPositionalParams(var395[2].first, var395[2].second)
val var397 = matchNamedParams(var395[6].first, var395[6].second)
val var398 = CallParams(var396, var397, nextId(), beginGen, endGen)
var398
}
}
return var387
}

fun matchPositionalParams(beginGen: Int, endGen: Int): List<Expr> {
val var399 = getSequenceElems(history, 120, listOf(121,169), beginGen, endGen)
val var400 = matchExpr(var399[0].first, var399[0].second)
val var401 = unrollRepeat0(history, 169, 171, 9, 170, var399[1].first, var399[1].second).map { k ->
val var402 = getSequenceElems(history, 172, listOf(7,148,7,121), k.first, k.second)
val var403 = matchExpr(var402[3].first, var402[3].second)
var403
}
return listOf(var400) + var401
}

fun matchNamedParams(beginGen: Int, endGen: Int): List<NamedParam> {
val var404 = getSequenceElems(history, 178, listOf(179,182), beginGen, endGen)
val var405 = matchNamedParam(var404[0].first, var404[0].second)
val var406 = unrollRepeat0(history, 182, 184, 9, 183, var404[1].first, var404[1].second).map { k ->
val var407 = getSequenceElems(history, 185, listOf(7,148,7,179), k.first, k.second)
val var408 = matchNamedParam(var407[3].first, var407[3].second)
var408
}
return listOf(var405) + var406
}

fun matchNamedParam(beginGen: Int, endGen: Int): NamedParam {
val var409 = getSequenceElems(history, 180, listOf(65,7,181,7,121), beginGen, endGen)
val var410 = matchSimpleName(var409[0].first, var409[0].second)
val var411 = matchExpr(var409[4].first, var409[4].second)
val var412 = NamedParam(var410, var411, nextId(), beginGen, endGen)
return var412
}

fun matchNamedTupleType(beginGen: Int, endGen: Int): NamedTupleType {
val var413 = getSequenceElems(history, 158, listOf(116,7,159,162,7,117), beginGen, endGen)
val var414 = matchNamedType(var413[2].first, var413[2].second)
val var415 = unrollRepeat0(history, 162, 164, 9, 163, var413[3].first, var413[3].second).map { k ->
val var416 = getSequenceElems(history, 165, listOf(7,148,7,159), k.first, k.second)
val var417 = matchNamedType(var416[3].first, var416[3].second)
var417
}
val var418 = NamedTupleType(listOf(var414) + var415, nextId(), beginGen, endGen)
return var418
}

fun matchNamedType(beginGen: Int, endGen: Int): NamedType {
val var419 = getSequenceElems(history, 160, listOf(65,7,161,7,140), beginGen, endGen)
val var420 = matchSimpleName(var419[0].first, var419[0].second)
val var421 = matchTypeExpr(var419[4].first, var419[4].second)
val var422 = NamedType(var420, var421, nextId(), beginGen, endGen)
return var422
}

fun matchBooleanLiteral(beginGen: Int, endGen: Int): BooleanLiteral {
val var423 = history[endGen].findByBeginGenOpt(77, 1, beginGen)
val var424 = history[endGen].findByBeginGenOpt(82, 1, beginGen)
check(hasSingleTrue(var423 != null, var424 != null))
val var425 = when {
var423 != null -> {
val var426 = BooleanLiteral(true, nextId(), beginGen, endGen)
var426
}
else -> {
val var427 = BooleanLiteral(false, nextId(), beginGen, endGen)
var427
}
}
return var425
}

}
