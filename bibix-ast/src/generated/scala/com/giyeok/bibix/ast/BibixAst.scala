package com.giyeok.bibix.ast

import com.giyeok.bibix.ast.BibixAst._
import com.giyeok.jparser.ParseResultTree._
import com.giyeok.jparser.{Inputs, ParseForest, ParsingErrors}
import com.giyeok.jparser.milestone.MilestoneParser
import com.giyeok.jparser.nparser.ParseTreeUtil.{unrollRepeat0, unrollRepeat1}
import com.giyeok.jparser.proto.{MilestoneParserDataProto, MilestoneParserProtobufConverter}

object BibixAst {

  sealed trait WithIdAndParseNode {
    val id: Int;
    val parseNode: Node
  }

  case class ActionDef(name: String, argsName: Option[String], expr: CallExpr)(override val id: Int, override val parseNode: Node) extends Def with WithIdAndParseNode

  case class ActionRuleDef(name: String, params: List[ParamDef], impl: MethodRef)(override val id: Int, override val parseNode: Node) extends ClassBodyElem with Def with WithIdAndParseNode

  case class ArgDef(name: String, typ: Option[TypeExpr], defaultValue: Option[Expr])(override val id: Int, override val parseNode: Node) extends Def with WithIdAndParseNode

  case class ArgRedef(nameTokens: List[String], redefValue: Expr)(override val id: Int, override val parseNode: Node) extends Def with WithIdAndParseNode

  case class BooleanLiteral(value: Boolean)(override val id: Int, override val parseNode: Node) extends Literal with WithIdAndParseNode

  case class BuildRuleDef(name: String, params: List[ParamDef], returnType: TypeExpr, impl: MethodRef)(override val id: Int, override val parseNode: Node) extends Def with WithIdAndParseNode

  case class BuildScript(packageName: Option[Name], defs: List[Def])(override val id: Int, override val parseNode: Node) extends WithIdAndParseNode

  case class CallExpr(name: Name, params: CallParams)(override val id: Int, override val parseNode: Node) extends Primary with WithIdAndParseNode

  case class CallParams(posParams: List[Expr], namedParams: List[NamedParam])(override val id: Int, override val parseNode: Node) extends WithIdAndParseNode

  case class CastExpr(expr: Expr, castTo: NoUnionType)(override val id: Int, override val parseNode: Node) extends Expr with WithIdAndParseNode

  sealed trait ClassBodyElem extends WithIdAndParseNode

  case class ClassCastDef(castTo: TypeExpr, expr: Expr)(override val id: Int, override val parseNode: Node) extends ClassBodyElem with WithIdAndParseNode

  sealed trait ClassDef extends Def with WithIdAndParseNode

  case class CollectionType(name: String, typeParams: TypeParams)(override val id: Int, override val parseNode: Node) extends NoUnionType with WithIdAndParseNode

  case class ComplexExpr(expr: Expr)(override val id: Int, override val parseNode: Node) extends StringExpr with WithIdAndParseNode

  case class DataClassDef(name: String, fields: List[ParamDef], body: List[ClassBodyElem])(override val id: Int, override val parseNode: Node) extends ClassDef with WithIdAndParseNode

  sealed trait Def extends WithIdAndParseNode

  case class EnumDef(name: String, values: List[String])(override val id: Int, override val parseNode: Node) extends Def with WithIdAndParseNode

  case class EscapeChar(code: Char)(override val id: Int, override val parseNode: Node) extends StringElem with WithIdAndParseNode

  sealed trait Expr extends WithIdAndParseNode

  case class ImportAll(source: Expr, rename: Option[String])(override val id: Int, override val parseNode: Node) extends ImportDef with WithIdAndParseNode

  sealed trait ImportDef extends Def with WithIdAndParseNode

  case class ImportFrom(source: Expr, importing: Name, rename: Option[String])(override val id: Int, override val parseNode: Node) extends ImportDef with WithIdAndParseNode

  case class JustChar(chr: Char)(override val id: Int, override val parseNode: Node) extends StringElem with WithIdAndParseNode

  case class ListExpr(elems: List[Expr])(override val id: Int, override val parseNode: Node) extends Primary with WithIdAndParseNode

  sealed trait Literal extends Primary with WithIdAndParseNode

  case class MemberAccess(target: Primary, name: String)(override val id: Int, override val parseNode: Node) extends Primary with WithIdAndParseNode

  case class MergeOp(lhs: Expr, rhs: Primary)(override val id: Int, override val parseNode: Node) extends MergeOpOrPrimary with WithIdAndParseNode

  sealed trait MergeOpOrPrimary extends Expr with WithIdAndParseNode

  case class MethodRef(targetName: Name, className: Name, methodName: Option[String])(override val id: Int, override val parseNode: Node) extends WithIdAndParseNode

  case class Name(tokens: List[String])(override val id: Int, override val parseNode: Node) extends NoUnionType with WithIdAndParseNode

  case class NameRef(name: String)(override val id: Int, override val parseNode: Node) extends Primary with WithIdAndParseNode

  case class NamedExpr(name: String, expr: Expr)(override val id: Int, override val parseNode: Node) extends WithIdAndParseNode

  case class NamedParam(name: String, value: Expr)(override val id: Int, override val parseNode: Node) extends WithIdAndParseNode

  case class NamedTupleExpr(elems: List[NamedExpr])(override val id: Int, override val parseNode: Node) extends Primary with WithIdAndParseNode

  case class NamedTupleType(elems: List[NamedType])(override val id: Int, override val parseNode: Node) extends NoUnionType with WithIdAndParseNode

  case class NamedType(name: String, typ: TypeExpr)(override val id: Int, override val parseNode: Node) extends WithIdAndParseNode

  case class NamespaceDef(name: String, body: BuildScript)(override val id: Int, override val parseNode: Node) extends Def with WithIdAndParseNode

  sealed trait NoUnionType extends TypeExpr with WithIdAndParseNode

  case class NoneLiteral()(override val id: Int, override val parseNode: Node) extends Literal with WithIdAndParseNode

  case class NoneType()(override val id: Int, override val parseNode: Node) extends NoUnionType with WithIdAndParseNode

  case class ParamDef(name: String, optional: Boolean, typ: Option[TypeExpr], defaultValue: Option[Expr])(override val id: Int, override val parseNode: Node) extends WithIdAndParseNode

  case class Paren(expr: Expr)(override val id: Int, override val parseNode: Node) extends Primary with WithIdAndParseNode

  sealed trait Primary extends MergeOpOrPrimary with WithIdAndParseNode

  case class SimpleExpr(name: String)(override val id: Int, override val parseNode: Node) extends StringExpr with WithIdAndParseNode

  sealed trait StringElem extends WithIdAndParseNode

  sealed trait StringExpr extends StringElem with WithIdAndParseNode

  case class StringLiteral(elems: List[StringElem])(override val id: Int, override val parseNode: Node) extends Literal with WithIdAndParseNode

  case class SuperClassDef(name: String, subs: List[String])(override val id: Int, override val parseNode: Node) extends ClassDef with WithIdAndParseNode

  case class TargetDef(name: String, value: Expr)(override val id: Int, override val parseNode: Node) extends Def with WithIdAndParseNode

  case class TupleExpr(elems: List[Expr])(override val id: Int, override val parseNode: Node) extends Primary with WithIdAndParseNode

  case class TupleType(elems: List[TypeExpr])(override val id: Int, override val parseNode: Node) extends NoUnionType with WithIdAndParseNode

  sealed trait TypeExpr extends WithIdAndParseNode

  case class TypeParams(params: List[TypeExpr])(override val id: Int, override val parseNode: Node) extends WithIdAndParseNode

  case class UnionType(elems: List[NoUnionType])(override val id: Int, override val parseNode: Node) extends TypeExpr with WithIdAndParseNode


  val milestoneParserData = MilestoneParserProtobufConverter.convertProtoToMilestoneParserData(
    MilestoneParserDataProto.MilestoneParserData.parseFrom(getClass.getResourceAsStream("/parserdata.pb")))

  val milestoneParser = new MilestoneParser(milestoneParserData)

  def parse(text: String): Either[ParseForest, ParsingErrors.ParsingError] =
    milestoneParser.parseAndReconstructToForest(text)

  def parseAst(text: String): Either[BuildScript, ParsingErrors.ParsingError] =
    parse(text) match {
      case Left(forest) => Left(new BibixAst().matchStart(forest.trees.head))
      case Right(error) => Right(error)
    }
}

class BibixAst {
  private var idCounter = 0

  def nextId(): Int = {
    idCounter += 1
    idCounter
  }

  def matchStart(node: Node): BuildScript = {
    val BindNode(start, BindNode(_, body)) = node
    assert(start.id == 1)
    matchBuildScript(body)
  }

  def matchActionDef(node: Node): ActionDef = {
    val BindNode(v1, v2) = node
    val v23 = v1.id match {
      case 313 =>
        val v3 = v2.asInstanceOf[SequenceNode].children(2)
        val BindNode(v4, v5) = v3
        assert(v4.id == 88)
        val v6 = v2.asInstanceOf[SequenceNode].children(3)
        val BindNode(v7, v8) = v6
        assert(v7.id == 317)
        val BindNode(v9, v10) = v8
        val v19 = v9.id match {
          case 126 =>
            None
          case 318 =>
            val BindNode(v11, v12) = v10
            val v18 = v11.id match {
              case 319 =>
                val BindNode(v13, v14) = v12
                assert(v13.id == 320)
                val v15 = v14.asInstanceOf[SequenceNode].children(1)
                val BindNode(v16, v17) = v15
                assert(v16.id == 321)
                matchActionParams(v17)
            }
            Some(v18)
        }
        val v20 = v2.asInstanceOf[SequenceNode].children(7)
        val BindNode(v21, v22) = v20
        assert(v21.id == 323)
        ActionDef(matchSimpleName(v5), v19, matchActionExpr(v22))(nextId(), v2)
    }
    v23
  }

  def matchActionExpr(node: Node): CallExpr = {
    val BindNode(v24, v25) = node
    val v29 = v24.id match {
      case 203 =>
        val v26 = v25.asInstanceOf[SequenceNode].children.head
        val BindNode(v27, v28) = v26
        assert(v27.id == 204)
        matchCallExpr(v28)
    }
    v29
  }

  def matchActionParams(node: Node): String = {
    val BindNode(v30, v31) = node
    val v35 = v30.id match {
      case 322 =>
        val v32 = v31.asInstanceOf[SequenceNode].children(2)
        val BindNode(v33, v34) = v32
        assert(v33.id == 88)
        matchSimpleName(v34)
    }
    v35
  }

  def matchActionRuleDef(node: Node): ActionRuleDef = {
    val BindNode(v36, v37) = node
    val v47 = v36.id match {
      case 368 =>
        val v38 = v37.asInstanceOf[SequenceNode].children(4)
        val BindNode(v39, v40) = v38
        assert(v39.id == 88)
        val v41 = v37.asInstanceOf[SequenceNode].children(6)
        val BindNode(v42, v43) = v41
        assert(v42.id == 332)
        val v44 = v37.asInstanceOf[SequenceNode].children(10)
        val BindNode(v45, v46) = v44
        assert(v45.id == 373)
        ActionRuleDef(matchSimpleName(v40), matchParamsDef(v43), matchMethodRef(v46))(nextId(), v37)
    }
    v47
  }

  def matchArgDef(node: Node): ArgDef = {
    val BindNode(v48, v49) = node
    val v81 = v48.id match {
      case 401 =>
        val v50 = v49.asInstanceOf[SequenceNode].children(2)
        val BindNode(v51, v52) = v50
        assert(v51.id == 88)
        val v53 = v49.asInstanceOf[SequenceNode].children(3)
        val BindNode(v54, v55) = v53
        assert(v54.id == 405)
        val BindNode(v56, v57) = v55
        val v66 = v56.id match {
          case 126 =>
            None
          case 406 =>
            val BindNode(v58, v59) = v57
            val v65 = v58.id match {
              case 407 =>
                val BindNode(v60, v61) = v59
                assert(v60.id == 408)
                val v62 = v61.asInstanceOf[SequenceNode].children(3)
                val BindNode(v63, v64) = v62
                assert(v63.id == 163)
                matchTypeExpr(v64)
            }
            Some(v65)
        }
        val v67 = v49.asInstanceOf[SequenceNode].children(4)
        val BindNode(v68, v69) = v67
        assert(v68.id == 345)
        val BindNode(v70, v71) = v69
        val v80 = v70.id match {
          case 126 =>
            None
          case 346 =>
            val BindNode(v72, v73) = v71
            val v79 = v72.id match {
              case 347 =>
                val BindNode(v74, v75) = v73
                assert(v74.id == 348)
                val v76 = v75.asInstanceOf[SequenceNode].children(3)
                val BindNode(v77, v78) = v76
                assert(v77.id == 138)
                matchExpr(v78)
            }
            Some(v79)
        }
        ArgDef(matchSimpleName(v52), v66, v80)(nextId(), v49)
    }
    v81
  }

  def matchArgRedef(node: Node): ArgRedef = {
    val BindNode(v82, v83) = node
    val v102 = v82.id match {
      case 411 =>
        val v84 = v83.asInstanceOf[SequenceNode].children(2)
        val BindNode(v85, v86) = v84
        assert(v85.id == 88)
        val v87 = v83.asInstanceOf[SequenceNode].children(3)
        val v88 = unrollRepeat1(v87).map { elem =>
          val BindNode(v89, v90) = elem
          assert(v89.id == 122)
          val BindNode(v91, v92) = v90
          val v98 = v91.id match {
            case 123 =>
              val BindNode(v93, v94) = v92
              assert(v93.id == 124)
              val v95 = v94.asInstanceOf[SequenceNode].children(3)
              val BindNode(v96, v97) = v95
              assert(v96.id == 88)
              matchSimpleName(v97)
          }
          v98
        }
        val v99 = v83.asInstanceOf[SequenceNode].children(7)
        val BindNode(v100, v101) = v99
        assert(v100.id == 138)
        ArgRedef(List(matchSimpleName(v86)) ++ v88, matchExpr(v101))(nextId(), v83)
    }
    v102
  }

  def matchBooleanLiteral(node: Node): BooleanLiteral = {
    val BindNode(v103, v104) = node
    val v118 = v103.id match {
      case 286 =>
        val v105 = v104.asInstanceOf[SequenceNode].children.head
        val BindNode(v106, v107) = v105
        assert(v106.id == 287)
        val JoinNode(_, v108, _) = v107
        val BindNode(v109, v110) = v108
        assert(v109.id == 288)
        val BindNode(v111, v112) = v110
        val v117 = v111.id match {
          case 289 =>
            val BindNode(v113, v114) = v112
            assert(v113.id == 103)
            BooleanLiteral(true)(nextId(), v114)
          case 290 =>
            val BindNode(v115, v116) = v112
            assert(v115.id == 109)
            BooleanLiteral(false)(nextId(), v116)
        }
        v117
    }
    v118
  }

  def matchBuildRuleDef(node: Node): BuildRuleDef = {
    val BindNode(v119, v120) = node
    val v133 = v119.id match {
      case 416 =>
        val v121 = v120.asInstanceOf[SequenceNode].children(2)
        val BindNode(v122, v123) = v121
        assert(v122.id == 88)
        val v124 = v120.asInstanceOf[SequenceNode].children(4)
        val BindNode(v125, v126) = v124
        assert(v125.id == 332)
        val v127 = v120.asInstanceOf[SequenceNode].children(8)
        val BindNode(v128, v129) = v127
        assert(v128.id == 163)
        val v130 = v120.asInstanceOf[SequenceNode].children(12)
        val BindNode(v131, v132) = v130
        assert(v131.id == 373)
        BuildRuleDef(matchSimpleName(v123), matchParamsDef(v126), matchTypeExpr(v129), matchMethodRef(v132))(nextId(), v120)
    }
    v133
  }

  def matchBuildScript(node: Node): BuildScript = {
    val BindNode(v134, v135) = node
    val v153 = v134.id match {
      case 3 =>
        val v136 = v135.asInstanceOf[SequenceNode].children.head
        val BindNode(v137, v138) = v136
        assert(v137.id == 4)
        val BindNode(v139, v140) = v138
        val v149 = v139.id match {
          case 126 =>
            None
          case 5 =>
            val BindNode(v141, v142) = v140
            val v148 = v141.id match {
              case 6 =>
                val BindNode(v143, v144) = v142
                assert(v143.id == 7)
                val v145 = v144.asInstanceOf[SequenceNode].children(1)
                val BindNode(v146, v147) = v145
                assert(v146.id == 58)
                matchPackageName(v147)
            }
            Some(v148)
        }
        val v150 = v135.asInstanceOf[SequenceNode].children(2)
        val BindNode(v151, v152) = v150
        assert(v151.id == 127)
        BuildScript(v149, matchDefs(v152))(nextId(), v135)
    }
    v153
  }

  def matchCallExpr(node: Node): CallExpr = {
    val BindNode(v154, v155) = node
    val v162 = v154.id match {
      case 205 =>
        val v156 = v155.asInstanceOf[SequenceNode].children.head
        val BindNode(v157, v158) = v156
        assert(v157.id == 86)
        val v159 = v155.asInstanceOf[SequenceNode].children(2)
        val BindNode(v160, v161) = v159
        assert(v160.id == 206)
        CallExpr(matchName(v158), matchCallParams(v161))(nextId(), v155)
    }
    v162
  }

  def matchCallParams(node: Node): CallParams = {
    val BindNode(v163, v164) = node
    val v177 = v163.id match {
      case 207 =>
        CallParams(List(), List())(nextId(), v164)
      case 208 =>
        val v165 = v164.asInstanceOf[SequenceNode].children(2)
        val BindNode(v166, v167) = v165
        assert(v166.id == 209)
        CallParams(matchPositionalParams(v167), List())(nextId(), v164)
      case 220 =>
        val v168 = v164.asInstanceOf[SequenceNode].children(2)
        val BindNode(v169, v170) = v168
        assert(v169.id == 221)
        CallParams(List(), matchNamedParams(v170))(nextId(), v164)
      case 231 =>
        val v171 = v164.asInstanceOf[SequenceNode].children(2)
        val BindNode(v172, v173) = v171
        assert(v172.id == 209)
        val v174 = v164.asInstanceOf[SequenceNode].children(6)
        val BindNode(v175, v176) = v174
        assert(v175.id == 221)
        CallParams(matchPositionalParams(v173), matchNamedParams(v176))(nextId(), v164)
    }
    v177
  }

  def matchClassBody(node: Node): List[ClassBodyElem] = {
    val BindNode(v178, v179) = node
    val v192 = v178.id match {
      case 359 =>
        val v180 = v179.asInstanceOf[SequenceNode].children(1)
        val v181 = unrollRepeat0(v180).map { elem =>
          val BindNode(v182, v183) = elem
          assert(v182.id == 362)
          val BindNode(v184, v185) = v183
          val v191 = v184.id match {
            case 363 =>
              val BindNode(v186, v187) = v185
              assert(v186.id == 364)
              val v188 = v187.asInstanceOf[SequenceNode].children(1)
              val BindNode(v189, v190) = v188
              assert(v189.id == 365)
              matchClassBodyElem(v190)
          }
          v191
        }
        v181
    }
    v192
  }

  def matchClassBodyElem(node: Node): ClassBodyElem = {
    val BindNode(v193, v194) = node
    val v201 = v193.id match {
      case 366 =>
        val v195 = v194.asInstanceOf[SequenceNode].children.head
        val BindNode(v196, v197) = v195
        assert(v196.id == 367)
        matchActionRuleDef(v197)
      case 379 =>
        val v198 = v194.asInstanceOf[SequenceNode].children.head
        val BindNode(v199, v200) = v198
        assert(v199.id == 380)
        matchClassCastDef(v200)
    }
    v201
  }

  def matchClassCastDef(node: Node): ClassCastDef = {
    val BindNode(v202, v203) = node
    val v210 = v202.id match {
      case 381 =>
        val v204 = v203.asInstanceOf[SequenceNode].children(2)
        val BindNode(v205, v206) = v204
        assert(v205.id == 163)
        val v207 = v203.asInstanceOf[SequenceNode].children(6)
        val BindNode(v208, v209) = v207
        assert(v208.id == 138)
        ClassCastDef(matchTypeExpr(v206), matchExpr(v209))(nextId(), v203)
    }
    v210
  }

  def matchClassDef(node: Node): ClassDef = {
    val BindNode(v211, v212) = node
    val v219 = v211.id match {
      case 326 =>
        val v213 = v212.asInstanceOf[SequenceNode].children.head
        val BindNode(v214, v215) = v213
        assert(v214.id == 327)
        matchDataClassDef(v215)
      case 382 =>
        val v216 = v212.asInstanceOf[SequenceNode].children.head
        val BindNode(v217, v218) = v216
        assert(v217.id == 383)
        matchSuperClassDef(v218)
    }
    v219
  }

  def matchCollectionType(node: Node): CollectionType = {
    val BindNode(v220, v221) = node
    val v240 = v220.id match {
      case 149 =>
        val v222 = v221.asInstanceOf[SequenceNode].children.head
        val BindNode(v223, v224) = v222
        assert(v223.id == 150)
        val JoinNode(_, v225, _) = v224
        val BindNode(v226, v227) = v225
        assert(v226.id == 151)
        val BindNode(v228, v229) = v227
        val v236 = v228.id match {
          case 152 =>
            val BindNode(v230, v231) = v229
            assert(v230.id == 153)
            val v232 = v231.asInstanceOf[SequenceNode].children.head
            "set"
          case 156 =>
            val BindNode(v233, v234) = v229
            assert(v233.id == 157)
            val v235 = v234.asInstanceOf[SequenceNode].children.head
            "list"
        }
        val v237 = v221.asInstanceOf[SequenceNode].children(2)
        val BindNode(v238, v239) = v237
        assert(v238.id == 160)
        CollectionType(v236, matchTypeParams(v239))(nextId(), v221)
    }
    v240
  }

  def matchDataClassDef(node: Node): DataClassDef = {
    val BindNode(v241, v242) = node
    val v264 = v241.id match {
      case 328 =>
        val v243 = v242.asInstanceOf[SequenceNode].children(2)
        val BindNode(v244, v245) = v243
        assert(v244.id == 88)
        val v246 = v242.asInstanceOf[SequenceNode].children(4)
        val BindNode(v247, v248) = v246
        assert(v247.id == 332)
        val v250 = v242.asInstanceOf[SequenceNode].children(5)
        val BindNode(v251, v252) = v250
        assert(v251.id == 354)
        val BindNode(v253, v254) = v252
        val v263 = v253.id match {
          case 126 =>
            None
          case 355 =>
            val BindNode(v255, v256) = v254
            val v262 = v255.id match {
              case 356 =>
                val BindNode(v257, v258) = v256
                assert(v257.id == 357)
                val v259 = v258.asInstanceOf[SequenceNode].children(1)
                val BindNode(v260, v261) = v259
                assert(v260.id == 358)
                matchClassBody(v261)
            }
            Some(v262)
        }
        val v249 = v263
        DataClassDef(matchSimpleName(v245), matchParamsDef(v248), if (v249.isDefined) v249.get else List())(nextId(), v242)
    }
    v264
  }

  def matchDef(node: Node): Def = {
    val BindNode(v265, v266) = node
    val v297 = v265.id match {
      case 324 =>
        val v267 = v266.asInstanceOf[SequenceNode].children.head
        val BindNode(v268, v269) = v267
        assert(v268.id == 325)
        matchClassDef(v269)
      case 414 =>
        val v270 = v266.asInstanceOf[SequenceNode].children.head
        val BindNode(v271, v272) = v270
        assert(v271.id == 415)
        matchBuildRuleDef(v272)
      case 399 =>
        val v273 = v266.asInstanceOf[SequenceNode].children.head
        val BindNode(v274, v275) = v273
        assert(v274.id == 400)
        matchArgDef(v275)
      case 409 =>
        val v276 = v266.asInstanceOf[SequenceNode].children.head
        val BindNode(v277, v278) = v276
        assert(v277.id == 410)
        matchArgRedef(v278)
      case 309 =>
        val v279 = v266.asInstanceOf[SequenceNode].children.head
        val BindNode(v280, v281) = v279
        assert(v280.id == 310)
        matchTargetDef(v281)
      case 311 =>
        val v282 = v266.asInstanceOf[SequenceNode].children.head
        val BindNode(v283, v284) = v282
        assert(v283.id == 312)
        matchActionDef(v284)
      case 393 =>
        val v285 = v266.asInstanceOf[SequenceNode].children.head
        val BindNode(v286, v287) = v285
        assert(v286.id == 394)
        matchEnumDef(v287)
      case 130 =>
        val v288 = v266.asInstanceOf[SequenceNode].children.head
        val BindNode(v289, v290) = v288
        assert(v289.id == 131)
        matchImportDef(v290)
      case 303 =>
        val v291 = v266.asInstanceOf[SequenceNode].children.head
        val BindNode(v292, v293) = v291
        assert(v292.id == 304)
        matchNamespaceDef(v293)
      case 366 =>
        val v294 = v266.asInstanceOf[SequenceNode].children.head
        val BindNode(v295, v296) = v294
        assert(v295.id == 367)
        matchActionRuleDef(v296)
    }
    v297
  }

  def matchDefs(node: Node): List[Def] = {
    val BindNode(v298, v299) = node
    val v315 = v298.id match {
      case 128 =>
        val v300 = v299.asInstanceOf[SequenceNode].children.head
        val BindNode(v301, v302) = v300
        assert(v301.id == 129)
        val v303 = v299.asInstanceOf[SequenceNode].children(1)
        val v304 = unrollRepeat0(v303).map { elem =>
          val BindNode(v305, v306) = elem
          assert(v305.id == 419)
          val BindNode(v307, v308) = v306
          val v314 = v307.id match {
            case 420 =>
              val BindNode(v309, v310) = v308
              assert(v309.id == 421)
              val v311 = v310.asInstanceOf[SequenceNode].children(1)
              val BindNode(v312, v313) = v311
              assert(v312.id == 129)
              matchDef(v313)
          }
          v314
        }
        List(matchDef(v302)) ++ v304
    }
    v315
  }

  def matchEnumDef(node: Node): EnumDef = {
    val BindNode(v316, v317) = node
    val v336 = v316.id match {
      case 395 =>
        val v318 = v317.asInstanceOf[SequenceNode].children(2)
        val BindNode(v319, v320) = v318
        assert(v319.id == 88)
        val v321 = v317.asInstanceOf[SequenceNode].children(6)
        val BindNode(v322, v323) = v321
        assert(v322.id == 88)
        val v324 = v317.asInstanceOf[SequenceNode].children(7)
        val v325 = unrollRepeat0(v324).map { elem =>
          val BindNode(v326, v327) = elem
          assert(v326.id == 390)
          val BindNode(v328, v329) = v327
          val v335 = v328.id match {
            case 391 =>
              val BindNode(v330, v331) = v329
              assert(v330.id == 392)
              val v332 = v331.asInstanceOf[SequenceNode].children(3)
              val BindNode(v333, v334) = v332
              assert(v333.id == 88)
              matchSimpleName(v334)
          }
          v335
        }
        EnumDef(matchSimpleName(v320), List(matchSimpleName(v323)) ++ v325)(nextId(), v317)
    }
    v336
  }

  def matchEscapeChar(node: Node): EscapeChar = {
    val BindNode(v337, v338) = node
    val v342 = v337.id match {
      case 272 =>
        val v339 = v338.asInstanceOf[SequenceNode].children(1)
        val BindNode(v340, v341) = v339
        assert(v340.id == 274)
        EscapeChar(v341.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char)(nextId(), v338)
    }
    v342
  }

  def matchExpr(node: Node): Expr = {
    val BindNode(v343, v344) = node
    val v354 = v343.id match {
      case 139 =>
        val v345 = v344.asInstanceOf[SequenceNode].children.head
        val BindNode(v346, v347) = v345
        assert(v346.id == 138)
        val v348 = v344.asInstanceOf[SequenceNode].children(4)
        val BindNode(v349, v350) = v348
        assert(v349.id == 143)
        CastExpr(matchExpr(v347), matchNoUnionType(v350))(nextId(), v344)
      case 198 =>
        val v351 = v344.asInstanceOf[SequenceNode].children.head
        val BindNode(v352, v353) = v351
        assert(v352.id == 199)
        matchMergeOpOrPrimary(v353)
    }
    v354
  }

  def matchImportDef(node: Node): ImportDef = {
    val BindNode(v355, v356) = node
    val v394 = v355.id match {
      case 132 =>
        val v357 = v356.asInstanceOf[SequenceNode].children(2)
        val BindNode(v358, v359) = v357
        assert(v358.id == 138)
        val v360 = v356.asInstanceOf[SequenceNode].children(3)
        val BindNode(v361, v362) = v360
        assert(v361.id == 295)
        val BindNode(v363, v364) = v362
        val v373 = v363.id match {
          case 126 =>
            None
          case 296 =>
            val BindNode(v365, v366) = v364
            val v372 = v365.id match {
              case 297 =>
                val BindNode(v367, v368) = v366
                assert(v367.id == 298)
                val v369 = v368.asInstanceOf[SequenceNode].children(3)
                val BindNode(v370, v371) = v369
                assert(v370.id == 88)
                matchSimpleName(v371)
            }
            Some(v372)
        }
        ImportAll(matchExpr(v359), v373)(nextId(), v356)
      case 299 =>
        val v374 = v356.asInstanceOf[SequenceNode].children(2)
        val BindNode(v375, v376) = v374
        assert(v375.id == 138)
        val v377 = v356.asInstanceOf[SequenceNode].children(6)
        val BindNode(v378, v379) = v377
        assert(v378.id == 86)
        val v380 = v356.asInstanceOf[SequenceNode].children(7)
        val BindNode(v381, v382) = v380
        assert(v381.id == 295)
        val BindNode(v383, v384) = v382
        val v393 = v383.id match {
          case 126 =>
            None
          case 296 =>
            val BindNode(v385, v386) = v384
            val v392 = v385.id match {
              case 297 =>
                val BindNode(v387, v388) = v386
                assert(v387.id == 298)
                val v389 = v388.asInstanceOf[SequenceNode].children(3)
                val BindNode(v390, v391) = v389
                assert(v390.id == 88)
                matchSimpleName(v391)
            }
            Some(v392)
        }
        ImportFrom(matchExpr(v376), matchName(v379), v393)(nextId(), v356)
    }
    v394
  }

  def matchLiteral(node: Node): Literal = {
    val BindNode(v395, v396) = node
    val v406 = v395.id match {
      case 256 =>
        val v397 = v396.asInstanceOf[SequenceNode].children.head
        val BindNode(v398, v399) = v397
        assert(v398.id == 257)
        matchStringLiteral(v399)
      case 284 =>
        val v400 = v396.asInstanceOf[SequenceNode].children.head
        val BindNode(v401, v402) = v400
        assert(v401.id == 285)
        matchBooleanLiteral(v402)
      case 291 =>
        val v403 = v396.asInstanceOf[SequenceNode].children.head
        val BindNode(v404, v405) = v403
        assert(v404.id == 292)
        matchNoneLiteral(v405)
    }
    v406
  }

  def matchMergeOpOrPrimary(node: Node): MergeOpOrPrimary = {
    val BindNode(v407, v408) = node
    val v418 = v407.id match {
      case 200 =>
        val v409 = v408.asInstanceOf[SequenceNode].children.head
        val BindNode(v410, v411) = v409
        assert(v410.id == 138)
        val v412 = v408.asInstanceOf[SequenceNode].children(4)
        val BindNode(v413, v414) = v412
        assert(v413.id == 202)
        MergeOp(matchExpr(v411), matchPrimary(v414))(nextId(), v408)
      case 294 =>
        val v415 = v408.asInstanceOf[SequenceNode].children.head
        val BindNode(v416, v417) = v415
        assert(v416.id == 202)
        matchPrimary(v417)
    }
    v418
  }

  def matchMethodRef(node: Node): MethodRef = {
    val BindNode(v419, v420) = node
    val v441 = v419.id match {
      case 374 =>
        val v421 = v420.asInstanceOf[SequenceNode].children.head
        val BindNode(v422, v423) = v421
        assert(v422.id == 86)
        val v424 = v420.asInstanceOf[SequenceNode].children(4)
        val BindNode(v425, v426) = v424
        assert(v425.id == 86)
        val v427 = v420.asInstanceOf[SequenceNode].children(5)
        val BindNode(v428, v429) = v427
        assert(v428.id == 375)
        val BindNode(v430, v431) = v429
        val v440 = v430.id match {
          case 126 =>
            None
          case 376 =>
            val BindNode(v432, v433) = v431
            val v439 = v432.id match {
              case 377 =>
                val BindNode(v434, v435) = v433
                assert(v434.id == 378)
                val v436 = v435.asInstanceOf[SequenceNode].children(3)
                val BindNode(v437, v438) = v436
                assert(v437.id == 88)
                matchSimpleName(v438)
            }
            Some(v439)
        }
        MethodRef(matchName(v423), matchName(v426), v440)(nextId(), v420)
    }
    v441
  }

  def matchName(node: Node): Name = {
    val BindNode(v442, v443) = node
    val v459 = v442.id match {
      case 87 =>
        val v444 = v443.asInstanceOf[SequenceNode].children.head
        val BindNode(v445, v446) = v444
        assert(v445.id == 88)
        val v447 = v443.asInstanceOf[SequenceNode].children(1)
        val v448 = unrollRepeat0(v447).map { elem =>
          val BindNode(v449, v450) = elem
          assert(v449.id == 122)
          val BindNode(v451, v452) = v450
          val v458 = v451.id match {
            case 123 =>
              val BindNode(v453, v454) = v452
              assert(v453.id == 124)
              val v455 = v454.asInstanceOf[SequenceNode].children(3)
              val BindNode(v456, v457) = v455
              assert(v456.id == 88)
              matchSimpleName(v457)
          }
          v458
        }
        Name(List(matchSimpleName(v446)) ++ v448)(nextId(), v443)
    }
    v459
  }

  def matchNamedExpr(node: Node): NamedExpr = {
    val BindNode(v460, v461) = node
    val v468 = v460.id match {
      case 248 =>
        val v462 = v461.asInstanceOf[SequenceNode].children.head
        val BindNode(v463, v464) = v462
        assert(v463.id == 88)
        val v465 = v461.asInstanceOf[SequenceNode].children(4)
        val BindNode(v466, v467) = v465
        assert(v466.id == 138)
        NamedExpr(matchSimpleName(v464), matchExpr(v467))(nextId(), v461)
    }
    v468
  }

  def matchNamedParam(node: Node): NamedParam = {
    val BindNode(v469, v470) = node
    val v477 = v469.id match {
      case 224 =>
        val v471 = v470.asInstanceOf[SequenceNode].children.head
        val BindNode(v472, v473) = v471
        assert(v472.id == 88)
        val v474 = v470.asInstanceOf[SequenceNode].children(4)
        val BindNode(v475, v476) = v474
        assert(v475.id == 138)
        NamedParam(matchSimpleName(v473), matchExpr(v476))(nextId(), v470)
    }
    v477
  }

  def matchNamedParams(node: Node): List[NamedParam] = {
    val BindNode(v478, v479) = node
    val v495 = v478.id match {
      case 222 =>
        val v480 = v479.asInstanceOf[SequenceNode].children.head
        val BindNode(v481, v482) = v480
        assert(v481.id == 223)
        val v483 = v479.asInstanceOf[SequenceNode].children(1)
        val v484 = unrollRepeat0(v483).map { elem =>
          val BindNode(v485, v486) = elem
          assert(v485.id == 228)
          val BindNode(v487, v488) = v486
          val v494 = v487.id match {
            case 229 =>
              val BindNode(v489, v490) = v488
              assert(v489.id == 230)
              val v491 = v490.asInstanceOf[SequenceNode].children(3)
              val BindNode(v492, v493) = v491
              assert(v492.id == 223)
              matchNamedParam(v493)
          }
          v494
        }
        List(matchNamedParam(v482)) ++ v484
    }
    v495
  }

  def matchNamedTupleType(node: Node): NamedTupleType = {
    val BindNode(v496, v497) = node
    val v513 = v496.id match {
      case 189 =>
        val v498 = v497.asInstanceOf[SequenceNode].children(2)
        val BindNode(v499, v500) = v498
        assert(v499.id == 190)
        val v501 = v497.asInstanceOf[SequenceNode].children(3)
        val v502 = unrollRepeat0(v501).map { elem =>
          val BindNode(v503, v504) = elem
          assert(v503.id == 195)
          val BindNode(v505, v506) = v504
          val v512 = v505.id match {
            case 196 =>
              val BindNode(v507, v508) = v506
              assert(v507.id == 197)
              val v509 = v508.asInstanceOf[SequenceNode].children(3)
              val BindNode(v510, v511) = v509
              assert(v510.id == 190)
              matchNamedType(v511)
          }
          v512
        }
        NamedTupleType(List(matchNamedType(v500)) ++ v502)(nextId(), v497)
    }
    v513
  }

  def matchNamedType(node: Node): NamedType = {
    val BindNode(v514, v515) = node
    val v522 = v514.id match {
      case 191 =>
        val v516 = v515.asInstanceOf[SequenceNode].children.head
        val BindNode(v517, v518) = v516
        assert(v517.id == 88)
        val v519 = v515.asInstanceOf[SequenceNode].children(4)
        val BindNode(v520, v521) = v519
        assert(v520.id == 163)
        NamedType(matchSimpleName(v518), matchTypeExpr(v521))(nextId(), v515)
    }
    v522
  }

  def matchNamespaceDef(node: Node): NamespaceDef = {
    val BindNode(v523, v524) = node
    val v531 = v523.id match {
      case 305 =>
        val v525 = v524.asInstanceOf[SequenceNode].children(2)
        val BindNode(v526, v527) = v525
        assert(v526.id == 88)
        val v528 = v524.asInstanceOf[SequenceNode].children(5)
        val BindNode(v529, v530) = v528
        assert(v529.id == 2)
        NamespaceDef(matchSimpleName(v527), matchBuildScript(v530))(nextId(), v524)
    }
    v531
  }

  def matchNoUnionType(node: Node): NoUnionType = {
    val BindNode(v532, v533) = node
    val v546 = v532.id match {
      case 187 =>
        val v534 = v533.asInstanceOf[SequenceNode].children.head
        val BindNode(v535, v536) = v534
        assert(v535.id == 188)
        matchNamedTupleType(v536)
      case 145 =>
        NoneType()(nextId(), v533)
      case 144 =>
        val v537 = v533.asInstanceOf[SequenceNode].children.head
        val BindNode(v538, v539) = v537
        assert(v538.id == 86)
        matchName(v539)
      case 182 =>
        val v540 = v533.asInstanceOf[SequenceNode].children.head
        val BindNode(v541, v542) = v540
        assert(v541.id == 183)
        matchTupleType(v542)
      case 147 =>
        val v543 = v533.asInstanceOf[SequenceNode].children.head
        val BindNode(v544, v545) = v543
        assert(v544.id == 148)
        matchCollectionType(v545)
    }
    v546
  }

  def matchNoneLiteral(node: Node): NoneLiteral = {
    val BindNode(v547, v548) = node
    val v549 = v547.id match {
      case 145 =>
        NoneLiteral()(nextId(), v548)
    }
    v549
  }

  def matchPackageName(node: Node): Name = {
    val BindNode(v550, v551) = node
    val v555 = v550.id match {
      case 59 =>
        val v552 = v551.asInstanceOf[SequenceNode].children(2)
        val BindNode(v553, v554) = v552
        assert(v553.id == 86)
        matchName(v554)
    }
    v555
  }

  def matchParamDef(node: Node): ParamDef = {
    val BindNode(v556, v557) = node
    val v598 = v556.id match {
      case 339 =>
        val v558 = v557.asInstanceOf[SequenceNode].children.head
        val BindNode(v559, v560) = v558
        assert(v559.id == 88)
        val v561 = v557.asInstanceOf[SequenceNode].children(1)
        val BindNode(v562, v563) = v561
        assert(v562.id == 340)
        val BindNode(v564, v565) = v563
        val v574 = v564.id match {
          case 126 =>
            None
          case 341 =>
            val BindNode(v566, v567) = v565
            val v573 = v566.id match {
              case 342 =>
                val BindNode(v568, v569) = v567
                assert(v568.id == 343)
                val v570 = v569.asInstanceOf[SequenceNode].children(1)
                val BindNode(v571, v572) = v570
                assert(v571.id == 344)
                v572.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
            }
            Some(v573)
        }
        val v575 = v557.asInstanceOf[SequenceNode].children(5)
        val BindNode(v576, v577) = v575
        assert(v576.id == 163)
        val v578 = v557.asInstanceOf[SequenceNode].children(6)
        val BindNode(v579, v580) = v578
        assert(v579.id == 345)
        val BindNode(v581, v582) = v580
        val v591 = v581.id match {
          case 126 =>
            None
          case 346 =>
            val BindNode(v583, v584) = v582
            val v590 = v583.id match {
              case 347 =>
                val BindNode(v585, v586) = v584
                assert(v585.id == 348)
                val v587 = v586.asInstanceOf[SequenceNode].children(3)
                val BindNode(v588, v589) = v587
                assert(v588.id == 138)
                matchExpr(v589)
            }
            Some(v590)
        }
        ParamDef(matchSimpleName(v560), v574.isDefined, Some(matchTypeExpr(v577)), v591)(nextId(), v557)
      case 224 =>
        val v592 = v557.asInstanceOf[SequenceNode].children.head
        val BindNode(v593, v594) = v592
        assert(v593.id == 88)
        val v595 = v557.asInstanceOf[SequenceNode].children(4)
        val BindNode(v596, v597) = v595
        assert(v596.id == 138)
        ParamDef(matchSimpleName(v594), false, None, Some(matchExpr(v597)))(nextId(), v557)
    }
    v598
  }

  def matchParamsDef(node: Node): List[ParamDef] = {
    val BindNode(v599, v600) = node
    val v627 = v599.id match {
      case 333 =>
        val v602 = v600.asInstanceOf[SequenceNode].children(1)
        val BindNode(v603, v604) = v602
        assert(v603.id == 334)
        val BindNode(v605, v606) = v604
        val v626 = v605.id match {
          case 126 =>
            None
          case 335 =>
            val BindNode(v607, v608) = v606
            assert(v607.id == 336)
            val BindNode(v609, v610) = v608
            assert(v609.id == 337)
            val v611 = v610.asInstanceOf[SequenceNode].children(1)
            val BindNode(v612, v613) = v611
            assert(v612.id == 338)
            val v614 = v610.asInstanceOf[SequenceNode].children(2)
            val v615 = unrollRepeat0(v614).map { elem =>
              val BindNode(v616, v617) = elem
              assert(v616.id == 351)
              val BindNode(v618, v619) = v617
              val v625 = v618.id match {
                case 352 =>
                  val BindNode(v620, v621) = v619
                  assert(v620.id == 353)
                  val v622 = v621.asInstanceOf[SequenceNode].children(3)
                  val BindNode(v623, v624) = v622
                  assert(v623.id == 338)
                  matchParamDef(v624)
              }
              v625
            }
            Some(List(matchParamDef(v613)) ++ v615)
        }
        val v601 = v626
        if (v601.isDefined) v601.get else List()
    }
    v627
  }

  def matchPositionalParams(node: Node): List[Expr] = {
    val BindNode(v628, v629) = node
    val v645 = v628.id match {
      case 210 =>
        val v630 = v629.asInstanceOf[SequenceNode].children.head
        val BindNode(v631, v632) = v630
        assert(v631.id == 138)
        val v633 = v629.asInstanceOf[SequenceNode].children(1)
        val v634 = unrollRepeat0(v633).map { elem =>
          val BindNode(v635, v636) = elem
          assert(v635.id == 213)
          val BindNode(v637, v638) = v636
          val v644 = v637.id match {
            case 214 =>
              val BindNode(v639, v640) = v638
              assert(v639.id == 215)
              val v641 = v640.asInstanceOf[SequenceNode].children(3)
              val BindNode(v642, v643) = v641
              assert(v642.id == 138)
              matchExpr(v643)
          }
          v644
        }
        List(matchExpr(v632)) ++ v634
    }
    v645
  }

  def matchPrimary(node: Node): Primary = {
    val BindNode(v646, v647) = node
    val v748 = v646.id match {
      case 232 =>
        val v648 = v647.asInstanceOf[SequenceNode].children.head
        val BindNode(v649, v650) = v648
        assert(v649.id == 202)
        val v651 = v647.asInstanceOf[SequenceNode].children(4)
        val BindNode(v652, v653) = v651
        assert(v652.id == 88)
        MemberAccess(matchPrimary(v650), matchSimpleName(v653))(nextId(), v647)
      case 254 =>
        val v654 = v647.asInstanceOf[SequenceNode].children.head
        val BindNode(v655, v656) = v654
        assert(v655.id == 255)
        matchLiteral(v656)
      case 293 =>
        val v657 = v647.asInstanceOf[SequenceNode].children(2)
        val BindNode(v658, v659) = v657
        assert(v658.id == 138)
        Paren(matchExpr(v659))(nextId(), v647)
      case 241 =>
        val v660 = v647.asInstanceOf[SequenceNode].children(2)
        val BindNode(v661, v662) = v660
        assert(v661.id == 138)
        val v664 = v647.asInstanceOf[SequenceNode].children(5)
        val BindNode(v665, v666) = v664
        assert(v665.id == 236)
        val BindNode(v667, v668) = v666
        val v689 = v667.id match {
          case 126 =>
            None
          case 237 =>
            val BindNode(v669, v670) = v668
            val v688 = v669.id match {
              case 238 =>
                val BindNode(v671, v672) = v670
                assert(v671.id == 239)
                val v673 = v672.asInstanceOf[SequenceNode].children(1)
                val BindNode(v674, v675) = v673
                assert(v674.id == 138)
                val v676 = v672.asInstanceOf[SequenceNode].children(2)
                val v677 = unrollRepeat0(v676).map { elem =>
                  val BindNode(v678, v679) = elem
                  assert(v678.id == 213)
                  val BindNode(v680, v681) = v679
                  val v687 = v680.id match {
                    case 214 =>
                      val BindNode(v682, v683) = v681
                      assert(v682.id == 215)
                      val v684 = v683.asInstanceOf[SequenceNode].children(3)
                      val BindNode(v685, v686) = v684
                      assert(v685.id == 138)
                      matchExpr(v686)
                  }
                  v687
                }
                List(matchExpr(v675)) ++ v677
            }
            Some(v688)
        }
        val v663 = v689
        TupleExpr(List(matchExpr(v662)) ++ (if (v663.isDefined) v663.get else List()))(nextId(), v647)
      case 234 =>
        val v691 = v647.asInstanceOf[SequenceNode].children(1)
        val BindNode(v692, v693) = v691
        assert(v692.id == 236)
        val BindNode(v694, v695) = v693
        val v715 = v694.id match {
          case 126 =>
            None
          case 237 =>
            val BindNode(v696, v697) = v695
            assert(v696.id == 238)
            val BindNode(v698, v699) = v697
            assert(v698.id == 239)
            val v700 = v699.asInstanceOf[SequenceNode].children(1)
            val BindNode(v701, v702) = v700
            assert(v701.id == 138)
            val v703 = v699.asInstanceOf[SequenceNode].children(2)
            val v704 = unrollRepeat0(v703).map { elem =>
              val BindNode(v705, v706) = elem
              assert(v705.id == 213)
              val BindNode(v707, v708) = v706
              val v714 = v707.id match {
                case 214 =>
                  val BindNode(v709, v710) = v708
                  assert(v709.id == 215)
                  val v711 = v710.asInstanceOf[SequenceNode].children(3)
                  val BindNode(v712, v713) = v711
                  assert(v712.id == 138)
                  matchExpr(v713)
              }
              v714
            }
            Some(List(matchExpr(v702)) ++ v704)
        }
        val v690 = v715
        ListExpr(if (v690.isDefined) v690.get else List())(nextId(), v647)
      case 203 =>
        val v716 = v647.asInstanceOf[SequenceNode].children.head
        val BindNode(v717, v718) = v716
        assert(v717.id == 204)
        matchCallExpr(v718)
      case 233 =>
        val v719 = v647.asInstanceOf[SequenceNode].children.head
        val BindNode(v720, v721) = v719
        assert(v720.id == 88)
        NameRef(matchSimpleName(v721))(nextId(), v647)
      case 242 =>
        val v723 = v647.asInstanceOf[SequenceNode].children(1)
        val BindNode(v724, v725) = v723
        assert(v724.id == 243)
        val BindNode(v726, v727) = v725
        val v747 = v726.id match {
          case 126 =>
            None
          case 244 =>
            val BindNode(v728, v729) = v727
            assert(v728.id == 245)
            val BindNode(v730, v731) = v729
            assert(v730.id == 246)
            val v732 = v731.asInstanceOf[SequenceNode].children(1)
            val BindNode(v733, v734) = v732
            assert(v733.id == 247)
            val v735 = v731.asInstanceOf[SequenceNode].children(2)
            val v736 = unrollRepeat0(v735).map { elem =>
              val BindNode(v737, v738) = elem
              assert(v737.id == 251)
              val BindNode(v739, v740) = v738
              val v746 = v739.id match {
                case 252 =>
                  val BindNode(v741, v742) = v740
                  assert(v741.id == 253)
                  val v743 = v742.asInstanceOf[SequenceNode].children(3)
                  val BindNode(v744, v745) = v743
                  assert(v744.id == 247)
                  matchNamedExpr(v745)
              }
              v746
            }
            Some(List(matchNamedExpr(v734)) ++ v736)
        }
        val v722 = v747
        NamedTupleExpr(if (v722.isDefined) v722.get else List())(nextId(), v647)
    }
    v748
  }

  def matchSimpleName(node: Node): String = {
    val BindNode(v749, v750) = node
    val v781 = v749.id match {
      case 89 =>
        val v751 = v750.asInstanceOf[SequenceNode].children.head
        val BindNode(v752, v753) = v751
        assert(v752.id == 90)
        val BindNode(v754, v755) = v753
        assert(v754.id == 91)
        val BindNode(v756, v757) = v755
        assert(v756.id == 92)
        val BindNode(v758, v759) = v757
        val v780 = v758.id match {
          case 93 =>
            val BindNode(v760, v761) = v759
            assert(v760.id == 94)
            val v762 = v761.asInstanceOf[SequenceNode].children.head
            val BindNode(v763, v764) = v762
            assert(v763.id == 95)
            val JoinNode(_, v765, _) = v764
            val BindNode(v766, v767) = v765
            assert(v766.id == 96)
            val BindNode(v768, v769) = v767
            val v779 = v768.id match {
              case 97 =>
                val BindNode(v770, v771) = v769
                assert(v770.id == 98)
                val v772 = v771.asInstanceOf[SequenceNode].children.head
                val BindNode(v773, v774) = v772
                assert(v773.id == 99)
                val v775 = v771.asInstanceOf[SequenceNode].children(1)
                val v776 = unrollRepeat0(v775).map { elem =>
                  val BindNode(v777, v778) = elem
                  assert(v777.id == 76)
                  v778.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
                }
                v774.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char.toString + v776.map(x => x.toString).mkString("")
            }
            v779
        }
        v780
    }
    v781
  }

  def matchStringElem(node: Node): StringElem = {
    val BindNode(v782, v783) = node
    val v795 = v782.id match {
      case 267 =>
        val v784 = v783.asInstanceOf[SequenceNode].children.head
        val BindNode(v785, v786) = v784
        assert(v785.id == 268)
        val BindNode(v787, v788) = v786
        assert(v787.id == 34)
        JustChar(v788.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char)(nextId(), v783)
      case 270 =>
        val v789 = v783.asInstanceOf[SequenceNode].children.head
        val BindNode(v790, v791) = v789
        assert(v790.id == 271)
        matchEscapeChar(v791)
      case 275 =>
        val v792 = v783.asInstanceOf[SequenceNode].children.head
        val BindNode(v793, v794) = v792
        assert(v793.id == 276)
        matchStringExpr(v794)
    }
    v795
  }

  def matchStringExpr(node: Node): StringExpr = {
    val BindNode(v796, v797) = node
    val v814 = v796.id match {
      case 277 =>
        val v798 = v797.asInstanceOf[SequenceNode].children.head
        val BindNode(v799, v800) = v798
        assert(v799.id == 278)
        val BindNode(v801, v802) = v800
        assert(v801.id == 279)
        val BindNode(v803, v804) = v802
        val v810 = v803.id match {
          case 280 =>
            val BindNode(v805, v806) = v804
            assert(v805.id == 281)
            val v807 = v806.asInstanceOf[SequenceNode].children(1)
            val BindNode(v808, v809) = v807
            assert(v808.id == 88)
            matchSimpleName(v809)
        }
        SimpleExpr(v810)(nextId(), v797)
      case 283 =>
        val v811 = v797.asInstanceOf[SequenceNode].children(3)
        val BindNode(v812, v813) = v811
        assert(v812.id == 138)
        ComplexExpr(matchExpr(v813))(nextId(), v797)
    }
    v814
  }

  def matchStringLiteral(node: Node): StringLiteral = {
    val BindNode(v815, v816) = node
    val v831 = v815.id match {
      case 258 =>
        val v817 = v816.asInstanceOf[SequenceNode].children(1)
        val v818 = unrollRepeat0(v817).map { elem =>
          val BindNode(v819, v820) = elem
          assert(v819.id == 262)
          val BindNode(v821, v822) = v820
          assert(v821.id == 263)
          val BindNode(v823, v824) = v822
          val v830 = v823.id match {
            case 264 =>
              val BindNode(v825, v826) = v824
              assert(v825.id == 265)
              val v827 = v826.asInstanceOf[SequenceNode].children.head
              val BindNode(v828, v829) = v827
              assert(v828.id == 266)
              matchStringElem(v829)
          }
          v830
        }
        StringLiteral(v818)(nextId(), v816)
    }
    v831
  }

  def matchSuperClassDef(node: Node): SuperClassDef = {
    val BindNode(v832, v833) = node
    val v852 = v832.id match {
      case 384 =>
        val v834 = v833.asInstanceOf[SequenceNode].children(4)
        val BindNode(v835, v836) = v834
        assert(v835.id == 88)
        val v837 = v833.asInstanceOf[SequenceNode].children(8)
        val BindNode(v838, v839) = v837
        assert(v838.id == 88)
        val v840 = v833.asInstanceOf[SequenceNode].children(9)
        val v841 = unrollRepeat0(v840).map { elem =>
          val BindNode(v842, v843) = elem
          assert(v842.id == 390)
          val BindNode(v844, v845) = v843
          val v851 = v844.id match {
            case 391 =>
              val BindNode(v846, v847) = v845
              assert(v846.id == 392)
              val v848 = v847.asInstanceOf[SequenceNode].children(3)
              val BindNode(v849, v850) = v848
              assert(v849.id == 88)
              matchSimpleName(v850)
          }
          v851
        }
        SuperClassDef(matchSimpleName(v836), List(matchSimpleName(v839)) ++ v841)(nextId(), v833)
    }
    v852
  }

  def matchTargetDef(node: Node): TargetDef = {
    val BindNode(v853, v854) = node
    val v861 = v853.id match {
      case 224 =>
        val v855 = v854.asInstanceOf[SequenceNode].children.head
        val BindNode(v856, v857) = v855
        assert(v856.id == 88)
        val v858 = v854.asInstanceOf[SequenceNode].children(4)
        val BindNode(v859, v860) = v858
        assert(v859.id == 138)
        TargetDef(matchSimpleName(v857), matchExpr(v860))(nextId(), v854)
    }
    v861
  }

  def matchTupleType(node: Node): TupleType = {
    val BindNode(v862, v863) = node
    val v879 = v862.id match {
      case 184 =>
        val v864 = v863.asInstanceOf[SequenceNode].children(2)
        val BindNode(v865, v866) = v864
        assert(v865.id == 163)
        val v867 = v863.asInstanceOf[SequenceNode].children(3)
        val v868 = unrollRepeat0(v867).map { elem =>
          val BindNode(v869, v870) = elem
          assert(v869.id == 178)
          val BindNode(v871, v872) = v870
          val v878 = v871.id match {
            case 179 =>
              val BindNode(v873, v874) = v872
              assert(v873.id == 180)
              val v875 = v874.asInstanceOf[SequenceNode].children(3)
              val BindNode(v876, v877) = v875
              assert(v876.id == 163)
              matchTypeExpr(v877)
          }
          v878
        }
        TupleType(List(matchTypeExpr(v866)) ++ v868)(nextId(), v863)
    }
    v879
  }

  def matchTypeExpr(node: Node): TypeExpr = {
    val BindNode(v880, v881) = node
    val v888 = v880.id match {
      case 164 =>
        val v882 = v881.asInstanceOf[SequenceNode].children.head
        val BindNode(v883, v884) = v882
        assert(v883.id == 143)
        matchNoUnionType(v884)
      case 165 =>
        val v885 = v881.asInstanceOf[SequenceNode].children.head
        val BindNode(v886, v887) = v885
        assert(v886.id == 166)
        matchUnionType(v887)
    }
    v888
  }

  def matchTypeParams(node: Node): TypeParams = {
    val BindNode(v889, v890) = node
    val v906 = v889.id match {
      case 161 =>
        val v891 = v890.asInstanceOf[SequenceNode].children(2)
        val BindNode(v892, v893) = v891
        assert(v892.id == 163)
        val v894 = v890.asInstanceOf[SequenceNode].children(3)
        val v895 = unrollRepeat0(v894).map { elem =>
          val BindNode(v896, v897) = elem
          assert(v896.id == 178)
          val BindNode(v898, v899) = v897
          val v905 = v898.id match {
            case 179 =>
              val BindNode(v900, v901) = v899
              assert(v900.id == 180)
              val v902 = v901.asInstanceOf[SequenceNode].children(3)
              val BindNode(v903, v904) = v902
              assert(v903.id == 163)
              matchTypeExpr(v904)
          }
          v905
        }
        TypeParams(List(matchTypeExpr(v893)) ++ v895)(nextId(), v890)
    }
    v906
  }

  def matchUnionType(node: Node): UnionType = {
    val BindNode(v907, v908) = node
    val v924 = v907.id match {
      case 167 =>
        val v909 = v908.asInstanceOf[SequenceNode].children(2)
        val BindNode(v910, v911) = v909
        assert(v910.id == 143)
        val v912 = v908.asInstanceOf[SequenceNode].children(3)
        val v913 = unrollRepeat0(v912).map { elem =>
          val BindNode(v914, v915) = elem
          assert(v914.id == 171)
          val BindNode(v916, v917) = v915
          val v923 = v916.id match {
            case 172 =>
              val BindNode(v918, v919) = v917
              assert(v918.id == 173)
              val v920 = v919.asInstanceOf[SequenceNode].children(3)
              val BindNode(v921, v922) = v920
              assert(v921.id == 143)
              matchNoUnionType(v922)
          }
          v923
        }
        UnionType(List(matchNoUnionType(v911)) ++ v913)(nextId(), v908)
    }
    v924
  }
}
