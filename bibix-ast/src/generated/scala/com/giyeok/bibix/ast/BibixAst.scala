package com.giyeok.bibix.ast

import com.giyeok.bibix.ast.BibixAst._
import com.giyeok.jparser.ParseResultTree._
import com.giyeok.jparser.{Inputs, ParseForest, ParsingErrors}
import com.giyeok.jparser.milestone.MilestoneParser
import com.giyeok.jparser.nparser.ParseTreeUtil.{unrollRepeat0, unrollRepeat1}
import com.giyeok.jparser.proto.{MilestoneParserDataProto, MilestoneParserProtobufConverter}

object BibixAst {

  sealed trait WithIdAndParseNode {
    val id: Int
    val parseNode: Node
  }

  case class ActionDef(name: String, argsName: Option[String], expr: CallExpr)(override val id: Int, override val parseNode: Node) extends Def with WithIdAndParseNode

  case class ActionRuleDef(name: String, params: List[ParamDef], impl: MethodRef)(override val id: Int, override val parseNode: Node) extends ClassBodyElem with Def with WithIdAndParseNode

  case class ArgDef(name: String, typ: Option[TypeExpr], defaultValue: Option[Expr])(override val id: Int, override val parseNode: Node) extends Def with WithIdAndParseNode

  case class ArgRedef(nameTokens: List[String], redefValue: Expr)(override val id: Int, override val parseNode: Node) extends Def with WithIdAndParseNode

  case class BooleanLiteral(value: Boolean)(override val id: Int, override val parseNode: Node) extends Literal with WithIdAndParseNode

  case class BuildRuleDef(name: String, params: List[ParamDef], returnType: TypeExpr, impl: MethodRef)(override val id: Int, override val parseNode: Node) extends Def with WithIdAndParseNode

  case class BuildScript(packageName: Option[Name], defs: List[Def])(override val id: Int, override val parseNode: Node) extends WithIdAndParseNode

  case class CallExpr(name: Name, params: CallParams)(override val id: Int, override val parseNode: Node) extends ImportSourceExpr with Primary with WithIdAndParseNode

  case class CallParams(posParams: List[Expr], namedParams: List[NamedParam])(override val id: Int, override val parseNode: Node) extends WithIdAndParseNode

  case class CanonicalName(packageName: Name, internalName: Name)(override val id: Int, override val parseNode: Node) extends NoUnionType with WithIdAndParseNode

  case class CastExpr(expr: Expr, castTo: NoUnionType)(override val id: Int, override val parseNode: Node) extends Expr with WithIdAndParseNode

  sealed trait ClassBodyElem extends WithIdAndParseNode

  case class ClassCastDef(castTo: TypeExpr, expr: Expr)(override val id: Int, override val parseNode: Node) extends ClassBodyElem with WithIdAndParseNode

  sealed trait ClassDef extends Def with WithIdAndParseNode

  case class ClassField(name: String, optional: Boolean, typ: TypeExpr)(override val id: Int, override val parseNode: Node) extends WithIdAndParseNode

  case class CollectionType(name: String, typeParams: TypeParams)(override val id: Int, override val parseNode: Node) extends NoUnionType with WithIdAndParseNode

  case class ComplexExpr(expr: Expr)(override val id: Int, override val parseNode: Node) extends StringExpr with WithIdAndParseNode

  case class DataClassDef(name: String, fields: List[ClassField], body: List[ClassBodyElem])(override val id: Int, override val parseNode: Node) extends ClassDef with WithIdAndParseNode

  sealed trait Def extends WithIdAndParseNode

  case class EnumDef(name: String, values: List[String])(override val id: Int, override val parseNode: Node) extends Def with WithIdAndParseNode

  case class EscapeChar(code: Char)(override val id: Int, override val parseNode: Node) extends StringElem with WithIdAndParseNode

  sealed trait Expr extends WithIdAndParseNode

  case class ImportAll(source: ImportSourceExpr, rename: String)(override val id: Int, override val parseNode: Node) extends ImportDef with WithIdAndParseNode

  sealed trait ImportDef extends Def with WithIdAndParseNode

  case class ImportFrom(source: ImportSourceExpr, importing: Name, rename: Option[String])(override val id: Int, override val parseNode: Node) extends ImportDef with WithIdAndParseNode

  case class ImportName(name: Name, rename: Option[String])(override val id: Int, override val parseNode: Node) extends ImportDef with WithIdAndParseNode

  sealed trait ImportSourceExpr extends WithIdAndParseNode

  case class JustChar(chr: Char)(override val id: Int, override val parseNode: Node) extends StringElem with WithIdAndParseNode

  case class ListExpr(elems: List[Expr])(override val id: Int, override val parseNode: Node) extends Primary with WithIdAndParseNode

  sealed trait Literal extends Primary with WithIdAndParseNode

  case class MemberAccess(target: Primary, name: String)(override val id: Int, override val parseNode: Node) extends Primary with WithIdAndParseNode

  case class MergeOp(lhs: Expr, rhs: Primary)(override val id: Int, override val parseNode: Node) extends MergeOpOrPrimary with WithIdAndParseNode

  sealed trait MergeOpOrPrimary extends Expr with WithIdAndParseNode

  case class MethodRef(targetName: Name, className: Name, methodName: Option[String])(override val id: Int, override val parseNode: Node) extends WithIdAndParseNode

  case class Name(tokens: List[String])(override val id: Int, override val parseNode: Node) extends NoUnionType with WithIdAndParseNode

  case class NameDef(name: String, value: Expr)(override val id: Int, override val parseNode: Node) extends Def with WithIdAndParseNode

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

  case class StringLiteral(elems: List[StringElem])(override val id: Int, override val parseNode: Node) extends ImportSourceExpr with Literal with WithIdAndParseNode

  case class SuperClassDef(name: String, subs: List[String])(override val id: Int, override val parseNode: Node) extends ClassDef with WithIdAndParseNode

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

  def nextId() = {
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
      case 421 =>
        val v3 = v2.asInstanceOf[SequenceNode].children(2)
        val BindNode(v4, v5) = v3
        assert(v4.id == 88)
        val v6 = v2.asInstanceOf[SequenceNode].children(3)
        val BindNode(v7, v8) = v6
        assert(v7.id == 422)
        val BindNode(v9, v10) = v8
        val v19 = v9.id match {
          case 126 =>
            None
          case 423 =>
            val BindNode(v11, v12) = v10
            val v18 = v11.id match {
              case 424 =>
                val BindNode(v13, v14) = v12
                assert(v13.id == 425)
                val v15 = v14.asInstanceOf[SequenceNode].children(1)
                val BindNode(v16, v17) = v15
                assert(v16.id == 426)
                matchActionParams(v17)
            }
            Some(v18)
        }
        val v20 = v2.asInstanceOf[SequenceNode].children(7)
        val BindNode(v21, v22) = v20
        assert(v21.id == 428)
        ActionDef(matchSimpleName(v5), v19, matchActionExpr(v22))(nextId(), v2)
    }
    v23
  }

  def matchActionExpr(node: Node): CallExpr = {
    val BindNode(v24, v25) = node
    val v29 = v24.id match {
      case 246 =>
        val v26 = v25.asInstanceOf[SequenceNode].children.head
        val BindNode(v27, v28) = v26
        assert(v27.id == 247)
        matchCallExpr(v28)
    }
    v29
  }

  def matchActionParams(node: Node): String = {
    val BindNode(v30, v31) = node
    val v35 = v30.id match {
      case 427 =>
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
      case 356 =>
        val v38 = v37.asInstanceOf[SequenceNode].children(4)
        val BindNode(v39, v40) = v38
        assert(v39.id == 88)
        val v41 = v37.asInstanceOf[SequenceNode].children(6)
        val BindNode(v42, v43) = v41
        assert(v42.id == 364)
        val v44 = v37.asInstanceOf[SequenceNode].children(10)
        val BindNode(v45, v46) = v44
        assert(v45.id == 381)
        ActionRuleDef(matchSimpleName(v40), matchParamsDef(v43), matchMethodRef(v46))(nextId(), v37)
    }
    v47
  }

  def matchArgDef(node: Node): ArgDef = {
    val BindNode(v48, v49) = node
    val v81 = v48.id match {
      case 403 =>
        val v50 = v49.asInstanceOf[SequenceNode].children(2)
        val BindNode(v51, v52) = v50
        assert(v51.id == 88)
        val v53 = v49.asInstanceOf[SequenceNode].children(3)
        val BindNode(v54, v55) = v53
        assert(v54.id == 407)
        val BindNode(v56, v57) = v55
        val v66 = v56.id match {
          case 126 =>
            None
          case 408 =>
            val BindNode(v58, v59) = v57
            val v65 = v58.id match {
              case 409 =>
                val BindNode(v60, v61) = v59
                assert(v60.id == 410)
                val v62 = v61.asInstanceOf[SequenceNode].children(3)
                val BindNode(v63, v64) = v62
                assert(v63.id == 209)
                matchTypeExpr(v64)
            }
            Some(v65)
        }
        val v67 = v49.asInstanceOf[SequenceNode].children(4)
        val BindNode(v68, v69) = v67
        assert(v68.id == 372)
        val BindNode(v70, v71) = v69
        val v80 = v70.id match {
          case 126 =>
            None
          case 373 =>
            val BindNode(v72, v73) = v71
            val v79 = v72.id match {
              case 374 =>
                val BindNode(v74, v75) = v73
                assert(v74.id == 375)
                val v76 = v75.asInstanceOf[SequenceNode].children(3)
                val BindNode(v77, v78) = v76
                assert(v77.id == 183)
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
      case 413 =>
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
        assert(v100.id == 183)
        ArgRedef(List(matchSimpleName(v86)) ++ v88, matchExpr(v101))(nextId(), v83)
    }
    v102
  }

  def matchBooleanLiteral(node: Node): BooleanLiteral = {
    val BindNode(v103, v104) = node
    val v118 = v103.id match {
      case 301 =>
        val v105 = v104.asInstanceOf[SequenceNode].children.head
        val BindNode(v106, v107) = v105
        assert(v106.id == 302)
        val JoinNode(_, v108, _) = v107
        val BindNode(v109, v110) = v108
        assert(v109.id == 303)
        val BindNode(v111, v112) = v110
        val v117 = v111.id match {
          case 304 =>
            val BindNode(v113, v114) = v112
            assert(v113.id == 103)
            BooleanLiteral(true)(nextId(), v114)
          case 305 =>
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
      case 418 =>
        val v121 = v120.asInstanceOf[SequenceNode].children(2)
        val BindNode(v122, v123) = v121
        assert(v122.id == 88)
        val v124 = v120.asInstanceOf[SequenceNode].children(4)
        val BindNode(v125, v126) = v124
        assert(v125.id == 364)
        val v127 = v120.asInstanceOf[SequenceNode].children(8)
        val BindNode(v128, v129) = v127
        assert(v128.id == 209)
        val v130 = v120.asInstanceOf[SequenceNode].children(12)
        val BindNode(v131, v132) = v130
        assert(v131.id == 381)
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
      case 248 =>
        val v156 = v155.asInstanceOf[SequenceNode].children.head
        val BindNode(v157, v158) = v156
        assert(v157.id == 86)
        val v159 = v155.asInstanceOf[SequenceNode].children(2)
        val BindNode(v160, v161) = v159
        assert(v160.id == 249)
        CallExpr(matchName(v158), matchCallParams(v161))(nextId(), v155)
    }
    v162
  }

  def matchCallParams(node: Node): CallParams = {
    val BindNode(v163, v164) = node
    val v177 = v163.id match {
      case 250 =>
        CallParams(List(), List())(nextId(), v164)
      case 251 =>
        val v165 = v164.asInstanceOf[SequenceNode].children(2)
        val BindNode(v166, v167) = v165
        assert(v166.id == 252)
        CallParams(matchPositionalParams(v167), List())(nextId(), v164)
      case 263 =>
        val v168 = v164.asInstanceOf[SequenceNode].children(2)
        val BindNode(v169, v170) = v168
        assert(v169.id == 264)
        CallParams(List(), matchNamedParams(v170))(nextId(), v164)
      case 274 =>
        val v171 = v164.asInstanceOf[SequenceNode].children(2)
        val BindNode(v172, v173) = v171
        assert(v172.id == 252)
        val v174 = v164.asInstanceOf[SequenceNode].children(6)
        val BindNode(v175, v176) = v174
        assert(v175.id == 264)
        CallParams(matchPositionalParams(v173), matchNamedParams(v176))(nextId(), v164)
    }
    v177
  }

  def matchClassBody(node: Node): List[ClassBodyElem] = {
    val BindNode(v178, v179) = node
    val v192 = v178.id match {
      case 347 =>
        val v180 = v179.asInstanceOf[SequenceNode].children(1)
        val v181 = unrollRepeat0(v180).map { elem =>
          val BindNode(v182, v183) = elem
          assert(v182.id == 350)
          val BindNode(v184, v185) = v183
          val v191 = v184.id match {
            case 351 =>
              val BindNode(v186, v187) = v185
              assert(v186.id == 352)
              val v188 = v187.asInstanceOf[SequenceNode].children(1)
              val BindNode(v189, v190) = v188
              assert(v189.id == 353)
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
      case 354 =>
        val v195 = v194.asInstanceOf[SequenceNode].children.head
        val BindNode(v196, v197) = v195
        assert(v196.id == 355)
        matchActionRuleDef(v197)
      case 387 =>
        val v198 = v194.asInstanceOf[SequenceNode].children.head
        val BindNode(v199, v200) = v198
        assert(v199.id == 388)
        matchClassCastDef(v200)
    }
    v201
  }

  def matchClassCastDef(node: Node): ClassCastDef = {
    val BindNode(v202, v203) = node
    val v210 = v202.id match {
      case 389 =>
        val v204 = v203.asInstanceOf[SequenceNode].children(2)
        val BindNode(v205, v206) = v204
        assert(v205.id == 209)
        val v207 = v203.asInstanceOf[SequenceNode].children(6)
        val BindNode(v208, v209) = v207
        assert(v208.id == 183)
        ClassCastDef(matchTypeExpr(v206), matchExpr(v209))(nextId(), v203)
    }
    v210
  }

  def matchClassDef(node: Node): ClassDef = {
    val BindNode(v211, v212) = node
    val v219 = v211.id match {
      case 318 =>
        val v213 = v212.asInstanceOf[SequenceNode].children.head
        val BindNode(v214, v215) = v213
        assert(v214.id == 319)
        matchDataClassDef(v215)
      case 390 =>
        val v216 = v212.asInstanceOf[SequenceNode].children.head
        val BindNode(v217, v218) = v216
        assert(v217.id == 391)
        matchSuperClassDef(v218)
    }
    v219
  }

  def matchClassFieldDef(node: Node): ClassField = {
    val BindNode(v220, v221) = node
    val v242 = v220.id match {
      case 331 =>
        val v222 = v221.asInstanceOf[SequenceNode].children.head
        val BindNode(v223, v224) = v222
        assert(v223.id == 88)
        val v225 = v221.asInstanceOf[SequenceNode].children(1)
        val BindNode(v226, v227) = v225
        assert(v226.id == 332)
        val BindNode(v228, v229) = v227
        val v238 = v228.id match {
          case 126 =>
            None
          case 333 =>
            val BindNode(v230, v231) = v229
            val v237 = v230.id match {
              case 334 =>
                val BindNode(v232, v233) = v231
                assert(v232.id == 335)
                val v234 = v233.asInstanceOf[SequenceNode].children(1)
                val BindNode(v235, v236) = v234
                assert(v235.id == 336)
                v236.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
            }
            Some(v237)
        }
        val v239 = v221.asInstanceOf[SequenceNode].children(5)
        val BindNode(v240, v241) = v239
        assert(v240.id == 209)
        ClassField(matchSimpleName(v224), v238.isDefined, matchTypeExpr(v241))(nextId(), v221)
    }
    v242
  }

  def matchClassFields(node: Node): List[ClassField] = {
    val BindNode(v243, v244) = node
    val v260 = v243.id match {
      case 329 =>
        val v245 = v244.asInstanceOf[SequenceNode].children.head
        val BindNode(v246, v247) = v245
        assert(v246.id == 330)
        val v248 = v244.asInstanceOf[SequenceNode].children(1)
        val v249 = unrollRepeat0(v248).map { elem =>
          val BindNode(v250, v251) = elem
          assert(v250.id == 339)
          val BindNode(v252, v253) = v251
          val v259 = v252.id match {
            case 340 =>
              val BindNode(v254, v255) = v253
              assert(v254.id == 341)
              val v256 = v255.asInstanceOf[SequenceNode].children(3)
              val BindNode(v257, v258) = v256
              assert(v257.id == 330)
              matchClassFieldDef(v258)
          }
          v259
        }
        List(matchClassFieldDef(v247)) ++ v249
    }
    v260
  }

  def matchCollectionType(node: Node): CollectionType = {
    val BindNode(v261, v262) = node
    val v281 = v261.id match {
      case 195 =>
        val v263 = v262.asInstanceOf[SequenceNode].children.head
        val BindNode(v264, v265) = v263
        assert(v264.id == 196)
        val JoinNode(_, v266, _) = v265
        val BindNode(v267, v268) = v266
        assert(v267.id == 197)
        val BindNode(v269, v270) = v268
        val v277 = v269.id match {
          case 198 =>
            val BindNode(v271, v272) = v270
            assert(v271.id == 199)
            val v273 = v272.asInstanceOf[SequenceNode].children.head
            "set"
          case 202 =>
            val BindNode(v274, v275) = v270
            assert(v274.id == 203)
            val v276 = v275.asInstanceOf[SequenceNode].children.head
            "list"
        }
        val v278 = v262.asInstanceOf[SequenceNode].children(2)
        val BindNode(v279, v280) = v278
        assert(v279.id == 206)
        CollectionType(v277, matchTypeParams(v280))(nextId(), v262)
    }
    v281
  }

  def matchDataClassDef(node: Node): DataClassDef = {
    val BindNode(v282, v283) = node
    val v317 = v282.id match {
      case 320 =>
        val v284 = v283.asInstanceOf[SequenceNode].children(2)
        val BindNode(v285, v286) = v284
        assert(v285.id == 88)
        val v288 = v283.asInstanceOf[SequenceNode].children(5)
        val BindNode(v289, v290) = v288
        assert(v289.id == 324)
        val BindNode(v291, v292) = v290
        val v301 = v291.id match {
          case 126 =>
            None
          case 325 =>
            val BindNode(v293, v294) = v292
            val v300 = v293.id match {
              case 326 =>
                val BindNode(v295, v296) = v294
                assert(v295.id == 327)
                val v297 = v296.asInstanceOf[SequenceNode].children(1)
                val BindNode(v298, v299) = v297
                assert(v298.id == 328)
                matchClassFields(v299)
            }
            Some(v300)
        }
        val v287 = v301
        val v303 = v283.asInstanceOf[SequenceNode].children(8)
        val BindNode(v304, v305) = v303
        assert(v304.id == 342)
        val BindNode(v306, v307) = v305
        val v316 = v306.id match {
          case 126 =>
            None
          case 343 =>
            val BindNode(v308, v309) = v307
            val v315 = v308.id match {
              case 344 =>
                val BindNode(v310, v311) = v309
                assert(v310.id == 345)
                val v312 = v311.asInstanceOf[SequenceNode].children(1)
                val BindNode(v313, v314) = v312
                assert(v313.id == 346)
                matchClassBody(v314)
            }
            Some(v315)
        }
        val v302 = v316
        DataClassDef(matchSimpleName(v286), if (v287.isDefined) v287.get else List(), if (v302.isDefined) v302.get else List())(nextId(), v283)
    }
    v317
  }

  def matchDef(node: Node): Def = {
    val BindNode(v318, v319) = node
    val v350 = v318.id match {
      case 316 =>
        val v320 = v319.asInstanceOf[SequenceNode].children.head
        val BindNode(v321, v322) = v320
        assert(v321.id == 317)
        matchClassDef(v322)
      case 416 =>
        val v323 = v319.asInstanceOf[SequenceNode].children.head
        val BindNode(v324, v325) = v323
        assert(v324.id == 417)
        matchBuildRuleDef(v325)
      case 401 =>
        val v326 = v319.asInstanceOf[SequenceNode].children.head
        val BindNode(v327, v328) = v326
        assert(v327.id == 402)
        matchArgDef(v328)
      case 411 =>
        val v329 = v319.asInstanceOf[SequenceNode].children.head
        val BindNode(v330, v331) = v329
        assert(v330.id == 412)
        matchArgRedef(v331)
      case 314 =>
        val v332 = v319.asInstanceOf[SequenceNode].children.head
        val BindNode(v333, v334) = v332
        assert(v333.id == 315)
        matchNameDef(v334)
      case 419 =>
        val v335 = v319.asInstanceOf[SequenceNode].children.head
        val BindNode(v336, v337) = v335
        assert(v336.id == 420)
        matchActionDef(v337)
      case 429 =>
        val v338 = v319.asInstanceOf[SequenceNode].children.head
        val BindNode(v339, v340) = v338
        assert(v339.id == 430)
        matchEnumDef(v340)
      case 139 =>
        val v341 = v319.asInstanceOf[SequenceNode].children.head
        val BindNode(v342, v343) = v341
        assert(v342.id == 140)
        matchImportDef(v343)
      case 130 =>
        val v344 = v319.asInstanceOf[SequenceNode].children.head
        val BindNode(v345, v346) = v344
        assert(v345.id == 131)
        matchNamespaceDef(v346)
      case 354 =>
        val v347 = v319.asInstanceOf[SequenceNode].children.head
        val BindNode(v348, v349) = v347
        assert(v348.id == 355)
        matchActionRuleDef(v349)
    }
    v350
  }

  def matchDefs(node: Node): List[Def] = {
    val BindNode(v351, v352) = node
    val v368 = v351.id match {
      case 128 =>
        val v353 = v352.asInstanceOf[SequenceNode].children.head
        val BindNode(v354, v355) = v353
        assert(v354.id == 129)
        val v356 = v352.asInstanceOf[SequenceNode].children(1)
        val v357 = unrollRepeat0(v356).map { elem =>
          val BindNode(v358, v359) = elem
          assert(v358.id == 437)
          val BindNode(v360, v361) = v359
          val v367 = v360.id match {
            case 438 =>
              val BindNode(v362, v363) = v361
              assert(v362.id == 439)
              val v364 = v363.asInstanceOf[SequenceNode].children(1)
              val BindNode(v365, v366) = v364
              assert(v365.id == 129)
              matchDef(v366)
          }
          v367
        }
        List(matchDef(v355)) ++ v357
    }
    v368
  }

  def matchEnumDef(node: Node): EnumDef = {
    val BindNode(v369, v370) = node
    val v389 = v369.id match {
      case 431 =>
        val v371 = v370.asInstanceOf[SequenceNode].children(2)
        val BindNode(v372, v373) = v371
        assert(v372.id == 88)
        val v374 = v370.asInstanceOf[SequenceNode].children(6)
        val BindNode(v375, v376) = v374
        assert(v375.id == 88)
        val v377 = v370.asInstanceOf[SequenceNode].children(7)
        val v378 = unrollRepeat0(v377).map { elem =>
          val BindNode(v379, v380) = elem
          assert(v379.id == 398)
          val BindNode(v381, v382) = v380
          val v388 = v381.id match {
            case 399 =>
              val BindNode(v383, v384) = v382
              assert(v383.id == 400)
              val v385 = v384.asInstanceOf[SequenceNode].children(3)
              val BindNode(v386, v387) = v385
              assert(v386.id == 88)
              matchSimpleName(v387)
          }
          v388
        }
        EnumDef(matchSimpleName(v373), List(matchSimpleName(v376)) ++ v378)(nextId(), v370)
    }
    v389
  }

  def matchEscapeChar(node: Node): EscapeChar = {
    val BindNode(v390, v391) = node
    val v395 = v390.id match {
      case 171 =>
        val v392 = v391.asInstanceOf[SequenceNode].children(1)
        val BindNode(v393, v394) = v392
        assert(v393.id == 173)
        EscapeChar(v394.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char)(nextId(), v391)
    }
    v395
  }

  def matchExpr(node: Node): Expr = {
    val BindNode(v396, v397) = node
    val v407 = v396.id match {
      case 184 =>
        val v398 = v397.asInstanceOf[SequenceNode].children.head
        val BindNode(v399, v400) = v398
        assert(v399.id == 183)
        val v401 = v397.asInstanceOf[SequenceNode].children(4)
        val BindNode(v402, v403) = v401
        assert(v402.id == 185)
        CastExpr(matchExpr(v400), matchNoUnionType(v403))(nextId(), v397)
      case 241 =>
        val v404 = v397.asInstanceOf[SequenceNode].children.head
        val BindNode(v405, v406) = v404
        assert(v405.id == 242)
        matchMergeOpOrPrimary(v406)
    }
    v407
  }

  def matchImportDef(node: Node): ImportDef = {
    val BindNode(v408, v409) = node
    val v453 = v408.id match {
      case 141 =>
        val v410 = v409.asInstanceOf[SequenceNode].children(2)
        val BindNode(v411, v412) = v410
        assert(v411.id == 86)
        val v413 = v409.asInstanceOf[SequenceNode].children(3)
        val BindNode(v414, v415) = v413
        assert(v414.id == 146)
        val BindNode(v416, v417) = v415
        val v426 = v416.id match {
          case 126 =>
            None
          case 147 =>
            val BindNode(v418, v419) = v417
            val v425 = v418.id match {
              case 148 =>
                val BindNode(v420, v421) = v419
                assert(v420.id == 149)
                val v422 = v421.asInstanceOf[SequenceNode].children(3)
                val BindNode(v423, v424) = v422
                assert(v423.id == 88)
                matchSimpleName(v424)
            }
            Some(v425)
        }
        ImportName(matchName(v412), v426)(nextId(), v409)
      case 153 =>
        val v427 = v409.asInstanceOf[SequenceNode].children(2)
        val BindNode(v428, v429) = v427
        assert(v428.id == 154)
        val v430 = v409.asInstanceOf[SequenceNode].children(6)
        val BindNode(v431, v432) = v430
        assert(v431.id == 88)
        ImportAll(matchImportSourceExpr(v429), matchSimpleName(v432))(nextId(), v409)
      case 310 =>
        val v433 = v409.asInstanceOf[SequenceNode].children(2)
        val BindNode(v434, v435) = v433
        assert(v434.id == 154)
        val v436 = v409.asInstanceOf[SequenceNode].children(6)
        val BindNode(v437, v438) = v436
        assert(v437.id == 86)
        val v439 = v409.asInstanceOf[SequenceNode].children(7)
        val BindNode(v440, v441) = v439
        assert(v440.id == 146)
        val BindNode(v442, v443) = v441
        val v452 = v442.id match {
          case 126 =>
            None
          case 147 =>
            val BindNode(v444, v445) = v443
            val v451 = v444.id match {
              case 148 =>
                val BindNode(v446, v447) = v445
                assert(v446.id == 149)
                val v448 = v447.asInstanceOf[SequenceNode].children(3)
                val BindNode(v449, v450) = v448
                assert(v449.id == 88)
                matchSimpleName(v450)
            }
            Some(v451)
        }
        ImportFrom(matchImportSourceExpr(v435), matchName(v438), v452)(nextId(), v409)
    }
    v453
  }

  def matchImportSourceExpr(node: Node): ImportSourceExpr = {
    val BindNode(v454, v455) = node
    val v462 = v454.id match {
      case 155 =>
        val v456 = v455.asInstanceOf[SequenceNode].children.head
        val BindNode(v457, v458) = v456
        assert(v457.id == 156)
        matchStringLiteral(v458)
      case 246 =>
        val v459 = v455.asInstanceOf[SequenceNode].children.head
        val BindNode(v460, v461) = v459
        assert(v460.id == 247)
        matchCallExpr(v461)
    }
    v462
  }

  def matchLiteral(node: Node): Literal = {
    val BindNode(v463, v464) = node
    val v474 = v463.id match {
      case 155 =>
        val v465 = v464.asInstanceOf[SequenceNode].children.head
        val BindNode(v466, v467) = v465
        assert(v466.id == 156)
        matchStringLiteral(v467)
      case 299 =>
        val v468 = v464.asInstanceOf[SequenceNode].children.head
        val BindNode(v469, v470) = v468
        assert(v469.id == 300)
        matchBooleanLiteral(v470)
      case 306 =>
        val v471 = v464.asInstanceOf[SequenceNode].children.head
        val BindNode(v472, v473) = v471
        assert(v472.id == 307)
        matchNoneLiteral(v473)
    }
    v474
  }

  def matchMergeOpOrPrimary(node: Node): MergeOpOrPrimary = {
    val BindNode(v475, v476) = node
    val v486 = v475.id match {
      case 243 =>
        val v477 = v476.asInstanceOf[SequenceNode].children.head
        val BindNode(v478, v479) = v477
        assert(v478.id == 183)
        val v480 = v476.asInstanceOf[SequenceNode].children(4)
        val BindNode(v481, v482) = v480
        assert(v481.id == 245)
        MergeOp(matchExpr(v479), matchPrimary(v482))(nextId(), v476)
      case 309 =>
        val v483 = v476.asInstanceOf[SequenceNode].children.head
        val BindNode(v484, v485) = v483
        assert(v484.id == 245)
        matchPrimary(v485)
    }
    v486
  }

  def matchMethodRef(node: Node): MethodRef = {
    val BindNode(v487, v488) = node
    val v509 = v487.id match {
      case 382 =>
        val v489 = v488.asInstanceOf[SequenceNode].children.head
        val BindNode(v490, v491) = v489
        assert(v490.id == 86)
        val v492 = v488.asInstanceOf[SequenceNode].children(4)
        val BindNode(v493, v494) = v492
        assert(v493.id == 86)
        val v495 = v488.asInstanceOf[SequenceNode].children(5)
        val BindNode(v496, v497) = v495
        assert(v496.id == 383)
        val BindNode(v498, v499) = v497
        val v508 = v498.id match {
          case 126 =>
            None
          case 384 =>
            val BindNode(v500, v501) = v499
            val v507 = v500.id match {
              case 385 =>
                val BindNode(v502, v503) = v501
                assert(v502.id == 386)
                val v504 = v503.asInstanceOf[SequenceNode].children(3)
                val BindNode(v505, v506) = v504
                assert(v505.id == 88)
                matchSimpleName(v506)
            }
            Some(v507)
        }
        MethodRef(matchName(v491), matchName(v494), v508)(nextId(), v488)
    }
    v509
  }

  def matchName(node: Node): Name = {
    val BindNode(v510, v511) = node
    val v527 = v510.id match {
      case 87 =>
        val v512 = v511.asInstanceOf[SequenceNode].children.head
        val BindNode(v513, v514) = v512
        assert(v513.id == 88)
        val v515 = v511.asInstanceOf[SequenceNode].children(1)
        val v516 = unrollRepeat0(v515).map { elem =>
          val BindNode(v517, v518) = elem
          assert(v517.id == 122)
          val BindNode(v519, v520) = v518
          val v526 = v519.id match {
            case 123 =>
              val BindNode(v521, v522) = v520
              assert(v521.id == 124)
              val v523 = v522.asInstanceOf[SequenceNode].children(3)
              val BindNode(v524, v525) = v523
              assert(v524.id == 88)
              matchSimpleName(v525)
          }
          v526
        }
        Name(List(matchSimpleName(v514)) ++ v516)(nextId(), v511)
    }
    v527
  }

  def matchNameDef(node: Node): NameDef = {
    val BindNode(v528, v529) = node
    val v536 = v528.id match {
      case 267 =>
        val v530 = v529.asInstanceOf[SequenceNode].children.head
        val BindNode(v531, v532) = v530
        assert(v531.id == 88)
        val v533 = v529.asInstanceOf[SequenceNode].children(4)
        val BindNode(v534, v535) = v533
        assert(v534.id == 183)
        NameDef(matchSimpleName(v532), matchExpr(v535))(nextId(), v529)
    }
    v536
  }

  def matchNamedExpr(node: Node): NamedExpr = {
    val BindNode(v537, v538) = node
    val v545 = v537.id match {
      case 291 =>
        val v539 = v538.asInstanceOf[SequenceNode].children.head
        val BindNode(v540, v541) = v539
        assert(v540.id == 88)
        val v542 = v538.asInstanceOf[SequenceNode].children(4)
        val BindNode(v543, v544) = v542
        assert(v543.id == 183)
        NamedExpr(matchSimpleName(v541), matchExpr(v544))(nextId(), v538)
    }
    v545
  }

  def matchNamedParam(node: Node): NamedParam = {
    val BindNode(v546, v547) = node
    val v554 = v546.id match {
      case 267 =>
        val v548 = v547.asInstanceOf[SequenceNode].children.head
        val BindNode(v549, v550) = v548
        assert(v549.id == 88)
        val v551 = v547.asInstanceOf[SequenceNode].children(4)
        val BindNode(v552, v553) = v551
        assert(v552.id == 183)
        NamedParam(matchSimpleName(v550), matchExpr(v553))(nextId(), v547)
    }
    v554
  }

  def matchNamedParams(node: Node): List[NamedParam] = {
    val BindNode(v555, v556) = node
    val v572 = v555.id match {
      case 265 =>
        val v557 = v556.asInstanceOf[SequenceNode].children.head
        val BindNode(v558, v559) = v557
        assert(v558.id == 266)
        val v560 = v556.asInstanceOf[SequenceNode].children(1)
        val v561 = unrollRepeat0(v560).map { elem =>
          val BindNode(v562, v563) = elem
          assert(v562.id == 271)
          val BindNode(v564, v565) = v563
          val v571 = v564.id match {
            case 272 =>
              val BindNode(v566, v567) = v565
              assert(v566.id == 273)
              val v568 = v567.asInstanceOf[SequenceNode].children(3)
              val BindNode(v569, v570) = v568
              assert(v569.id == 266)
              matchNamedParam(v570)
          }
          v571
        }
        List(matchNamedParam(v559)) ++ v561
    }
    v572
  }

  def matchNamedTupleType(node: Node): NamedTupleType = {
    val BindNode(v573, v574) = node
    val v590 = v573.id match {
      case 233 =>
        val v575 = v574.asInstanceOf[SequenceNode].children(2)
        val BindNode(v576, v577) = v575
        assert(v576.id == 234)
        val v578 = v574.asInstanceOf[SequenceNode].children(3)
        val v579 = unrollRepeat0(v578).map { elem =>
          val BindNode(v580, v581) = elem
          assert(v580.id == 238)
          val BindNode(v582, v583) = v581
          val v589 = v582.id match {
            case 239 =>
              val BindNode(v584, v585) = v583
              assert(v584.id == 240)
              val v586 = v585.asInstanceOf[SequenceNode].children(3)
              val BindNode(v587, v588) = v586
              assert(v587.id == 234)
              matchNamedType(v588)
          }
          v589
        }
        NamedTupleType(List(matchNamedType(v577)) ++ v579)(nextId(), v574)
    }
    v590
  }

  def matchNamedType(node: Node): NamedType = {
    val BindNode(v591, v592) = node
    val v599 = v591.id match {
      case 235 =>
        val v593 = v592.asInstanceOf[SequenceNode].children.head
        val BindNode(v594, v595) = v593
        assert(v594.id == 88)
        val v596 = v592.asInstanceOf[SequenceNode].children(4)
        val BindNode(v597, v598) = v596
        assert(v597.id == 209)
        NamedType(matchSimpleName(v595), matchTypeExpr(v598))(nextId(), v592)
    }
    v599
  }

  def matchNamespaceDef(node: Node): NamespaceDef = {
    val BindNode(v600, v601) = node
    val v608 = v600.id match {
      case 132 =>
        val v602 = v601.asInstanceOf[SequenceNode].children(2)
        val BindNode(v603, v604) = v602
        assert(v603.id == 88)
        val v605 = v601.asInstanceOf[SequenceNode].children(5)
        val BindNode(v606, v607) = v605
        assert(v606.id == 2)
        NamespaceDef(matchSimpleName(v604), matchBuildScript(v607))(nextId(), v601)
    }
    v608
  }

  def matchNoUnionType(node: Node): NoUnionType = {
    val BindNode(v609, v610) = node
    val v629 = v609.id match {
      case 231 =>
        val v611 = v610.asInstanceOf[SequenceNode].children.head
        val BindNode(v612, v613) = v611
        assert(v612.id == 232)
        matchNamedTupleType(v613)
      case 191 =>
        NoneType()(nextId(), v610)
      case 186 =>
        val v614 = v610.asInstanceOf[SequenceNode].children.head
        val BindNode(v615, v616) = v614
        assert(v615.id == 86)
        matchName(v616)
      case 226 =>
        val v617 = v610.asInstanceOf[SequenceNode].children.head
        val BindNode(v618, v619) = v617
        assert(v618.id == 227)
        matchTupleType(v619)
      case 187 =>
        val v620 = v610.asInstanceOf[SequenceNode].children.head
        val BindNode(v621, v622) = v620
        assert(v621.id == 86)
        val v623 = v610.asInstanceOf[SequenceNode].children(2)
        val BindNode(v624, v625) = v623
        assert(v624.id == 86)
        CanonicalName(matchName(v622), matchName(v625))(nextId(), v610)
      case 193 =>
        val v626 = v610.asInstanceOf[SequenceNode].children.head
        val BindNode(v627, v628) = v626
        assert(v627.id == 194)
        matchCollectionType(v628)
    }
    v629
  }

  def matchNoneLiteral(node: Node): NoneLiteral = {
    val BindNode(v630, v631) = node
    val v632 = v630.id match {
      case 191 =>
        NoneLiteral()(nextId(), v631)
    }
    v632
  }

  def matchPackageName(node: Node): Name = {
    val BindNode(v633, v634) = node
    val v638 = v633.id match {
      case 59 =>
        val v635 = v634.asInstanceOf[SequenceNode].children(2)
        val BindNode(v636, v637) = v635
        assert(v636.id == 86)
        matchName(v637)
    }
    v638
  }

  def matchParamDef(node: Node): ParamDef = {
    val BindNode(v639, v640) = node
    val v681 = v639.id match {
      case 371 =>
        val v641 = v640.asInstanceOf[SequenceNode].children.head
        val BindNode(v642, v643) = v641
        assert(v642.id == 88)
        val v644 = v640.asInstanceOf[SequenceNode].children(1)
        val BindNode(v645, v646) = v644
        assert(v645.id == 332)
        val BindNode(v647, v648) = v646
        val v657 = v647.id match {
          case 126 =>
            None
          case 333 =>
            val BindNode(v649, v650) = v648
            val v656 = v649.id match {
              case 334 =>
                val BindNode(v651, v652) = v650
                assert(v651.id == 335)
                val v653 = v652.asInstanceOf[SequenceNode].children(1)
                val BindNode(v654, v655) = v653
                assert(v654.id == 336)
                v655.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
            }
            Some(v656)
        }
        val v658 = v640.asInstanceOf[SequenceNode].children(5)
        val BindNode(v659, v660) = v658
        assert(v659.id == 209)
        val v661 = v640.asInstanceOf[SequenceNode].children(6)
        val BindNode(v662, v663) = v661
        assert(v662.id == 372)
        val BindNode(v664, v665) = v663
        val v674 = v664.id match {
          case 126 =>
            None
          case 373 =>
            val BindNode(v666, v667) = v665
            val v673 = v666.id match {
              case 374 =>
                val BindNode(v668, v669) = v667
                assert(v668.id == 375)
                val v670 = v669.asInstanceOf[SequenceNode].children(3)
                val BindNode(v671, v672) = v670
                assert(v671.id == 183)
                matchExpr(v672)
            }
            Some(v673)
        }
        ParamDef(matchSimpleName(v643), v657.isDefined, Some(matchTypeExpr(v660)), v674)(nextId(), v640)
      case 267 =>
        val v675 = v640.asInstanceOf[SequenceNode].children.head
        val BindNode(v676, v677) = v675
        assert(v676.id == 88)
        val v678 = v640.asInstanceOf[SequenceNode].children(4)
        val BindNode(v679, v680) = v678
        assert(v679.id == 183)
        ParamDef(matchSimpleName(v677), false, None, Some(matchExpr(v680)))(nextId(), v640)
    }
    v681
  }

  def matchParamsDef(node: Node): List[ParamDef] = {
    val BindNode(v682, v683) = node
    val v710 = v682.id match {
      case 365 =>
        val v685 = v683.asInstanceOf[SequenceNode].children(1)
        val BindNode(v686, v687) = v685
        assert(v686.id == 366)
        val BindNode(v688, v689) = v687
        val v709 = v688.id match {
          case 126 =>
            None
          case 367 =>
            val BindNode(v690, v691) = v689
            assert(v690.id == 368)
            val BindNode(v692, v693) = v691
            assert(v692.id == 369)
            val v694 = v693.asInstanceOf[SequenceNode].children(1)
            val BindNode(v695, v696) = v694
            assert(v695.id == 370)
            val v697 = v693.asInstanceOf[SequenceNode].children(2)
            val v698 = unrollRepeat0(v697).map { elem =>
              val BindNode(v699, v700) = elem
              assert(v699.id == 378)
              val BindNode(v701, v702) = v700
              val v708 = v701.id match {
                case 379 =>
                  val BindNode(v703, v704) = v702
                  assert(v703.id == 380)
                  val v705 = v704.asInstanceOf[SequenceNode].children(3)
                  val BindNode(v706, v707) = v705
                  assert(v706.id == 370)
                  matchParamDef(v707)
              }
              v708
            }
            Some(List(matchParamDef(v696)) ++ v698)
        }
        val v684 = v709
        if (v684.isDefined) v684.get else List()
    }
    v710
  }

  def matchPositionalParams(node: Node): List[Expr] = {
    val BindNode(v711, v712) = node
    val v728 = v711.id match {
      case 253 =>
        val v713 = v712.asInstanceOf[SequenceNode].children.head
        val BindNode(v714, v715) = v713
        assert(v714.id == 183)
        val v716 = v712.asInstanceOf[SequenceNode].children(1)
        val v717 = unrollRepeat0(v716).map { elem =>
          val BindNode(v718, v719) = elem
          assert(v718.id == 256)
          val BindNode(v720, v721) = v719
          val v727 = v720.id match {
            case 257 =>
              val BindNode(v722, v723) = v721
              assert(v722.id == 258)
              val v724 = v723.asInstanceOf[SequenceNode].children(3)
              val BindNode(v725, v726) = v724
              assert(v725.id == 183)
              matchExpr(v726)
          }
          v727
        }
        List(matchExpr(v715)) ++ v717
    }
    v728
  }

  def matchPrimary(node: Node): Primary = {
    val BindNode(v729, v730) = node
    val v831 = v729.id match {
      case 275 =>
        val v731 = v730.asInstanceOf[SequenceNode].children.head
        val BindNode(v732, v733) = v731
        assert(v732.id == 245)
        val v734 = v730.asInstanceOf[SequenceNode].children(4)
        val BindNode(v735, v736) = v734
        assert(v735.id == 88)
        MemberAccess(matchPrimary(v733), matchSimpleName(v736))(nextId(), v730)
      case 297 =>
        val v737 = v730.asInstanceOf[SequenceNode].children.head
        val BindNode(v738, v739) = v737
        assert(v738.id == 298)
        matchLiteral(v739)
      case 308 =>
        val v740 = v730.asInstanceOf[SequenceNode].children(2)
        val BindNode(v741, v742) = v740
        assert(v741.id == 183)
        Paren(matchExpr(v742))(nextId(), v730)
      case 277 =>
        val v744 = v730.asInstanceOf[SequenceNode].children(1)
        val BindNode(v745, v746) = v744
        assert(v745.id == 279)
        val BindNode(v747, v748) = v746
        val v768 = v747.id match {
          case 126 =>
            None
          case 280 =>
            val BindNode(v749, v750) = v748
            assert(v749.id == 281)
            val BindNode(v751, v752) = v750
            assert(v751.id == 282)
            val v753 = v752.asInstanceOf[SequenceNode].children(1)
            val BindNode(v754, v755) = v753
            assert(v754.id == 183)
            val v756 = v752.asInstanceOf[SequenceNode].children(2)
            val v757 = unrollRepeat0(v756).map { elem =>
              val BindNode(v758, v759) = elem
              assert(v758.id == 256)
              val BindNode(v760, v761) = v759
              val v767 = v760.id match {
                case 257 =>
                  val BindNode(v762, v763) = v761
                  assert(v762.id == 258)
                  val v764 = v763.asInstanceOf[SequenceNode].children(3)
                  val BindNode(v765, v766) = v764
                  assert(v765.id == 183)
                  matchExpr(v766)
              }
              v767
            }
            Some(List(matchExpr(v755)) ++ v757)
        }
        val v743 = v768
        ListExpr(if (v743.isDefined) v743.get else List())(nextId(), v730)
      case 246 =>
        val v769 = v730.asInstanceOf[SequenceNode].children.head
        val BindNode(v770, v771) = v769
        assert(v770.id == 247)
        matchCallExpr(v771)
      case 276 =>
        val v772 = v730.asInstanceOf[SequenceNode].children.head
        val BindNode(v773, v774) = v772
        assert(v773.id == 88)
        NameRef(matchSimpleName(v774))(nextId(), v730)
      case 284 =>
        val v775 = v730.asInstanceOf[SequenceNode].children(2)
        val BindNode(v776, v777) = v775
        assert(v776.id == 183)
        val v779 = v730.asInstanceOf[SequenceNode].children(5)
        val BindNode(v780, v781) = v779
        assert(v780.id == 279)
        val BindNode(v782, v783) = v781
        val v804 = v782.id match {
          case 126 =>
            None
          case 280 =>
            val BindNode(v784, v785) = v783
            val v803 = v784.id match {
              case 281 =>
                val BindNode(v786, v787) = v785
                assert(v786.id == 282)
                val v788 = v787.asInstanceOf[SequenceNode].children(1)
                val BindNode(v789, v790) = v788
                assert(v789.id == 183)
                val v791 = v787.asInstanceOf[SequenceNode].children(2)
                val v792 = unrollRepeat0(v791).map { elem =>
                  val BindNode(v793, v794) = elem
                  assert(v793.id == 256)
                  val BindNode(v795, v796) = v794
                  val v802 = v795.id match {
                    case 257 =>
                      val BindNode(v797, v798) = v796
                      assert(v797.id == 258)
                      val v799 = v798.asInstanceOf[SequenceNode].children(3)
                      val BindNode(v800, v801) = v799
                      assert(v800.id == 183)
                      matchExpr(v801)
                  }
                  v802
                }
                List(matchExpr(v790)) ++ v792
            }
            Some(v803)
        }
        val v778 = v804
        TupleExpr(List(matchExpr(v777)) ++ (if (v778.isDefined) v778.get else List()))(nextId(), v730)
      case 285 =>
        val v806 = v730.asInstanceOf[SequenceNode].children(1)
        val BindNode(v807, v808) = v806
        assert(v807.id == 286)
        val BindNode(v809, v810) = v808
        val v830 = v809.id match {
          case 126 =>
            None
          case 287 =>
            val BindNode(v811, v812) = v810
            assert(v811.id == 288)
            val BindNode(v813, v814) = v812
            assert(v813.id == 289)
            val v815 = v814.asInstanceOf[SequenceNode].children(1)
            val BindNode(v816, v817) = v815
            assert(v816.id == 290)
            val v818 = v814.asInstanceOf[SequenceNode].children(2)
            val v819 = unrollRepeat0(v818).map { elem =>
              val BindNode(v820, v821) = elem
              assert(v820.id == 294)
              val BindNode(v822, v823) = v821
              val v829 = v822.id match {
                case 295 =>
                  val BindNode(v824, v825) = v823
                  assert(v824.id == 296)
                  val v826 = v825.asInstanceOf[SequenceNode].children(3)
                  val BindNode(v827, v828) = v826
                  assert(v827.id == 290)
                  matchNamedExpr(v828)
              }
              v829
            }
            Some(List(matchNamedExpr(v817)) ++ v819)
        }
        val v805 = v830
        NamedTupleExpr(if (v805.isDefined) v805.get else List())(nextId(), v730)
    }
    v831
  }

  def matchSimpleName(node: Node): String = {
    val BindNode(v832, v833) = node
    val v864 = v832.id match {
      case 89 =>
        val v834 = v833.asInstanceOf[SequenceNode].children.head
        val BindNode(v835, v836) = v834
        assert(v835.id == 90)
        val BindNode(v837, v838) = v836
        assert(v837.id == 91)
        val BindNode(v839, v840) = v838
        assert(v839.id == 92)
        val BindNode(v841, v842) = v840
        val v863 = v841.id match {
          case 93 =>
            val BindNode(v843, v844) = v842
            assert(v843.id == 94)
            val v845 = v844.asInstanceOf[SequenceNode].children.head
            val BindNode(v846, v847) = v845
            assert(v846.id == 95)
            val JoinNode(_, v848, _) = v847
            val BindNode(v849, v850) = v848
            assert(v849.id == 96)
            val BindNode(v851, v852) = v850
            val v862 = v851.id match {
              case 97 =>
                val BindNode(v853, v854) = v852
                assert(v853.id == 98)
                val v855 = v854.asInstanceOf[SequenceNode].children.head
                val BindNode(v856, v857) = v855
                assert(v856.id == 99)
                val v858 = v854.asInstanceOf[SequenceNode].children(1)
                val v859 = unrollRepeat0(v858).map { elem =>
                  val BindNode(v860, v861) = elem
                  assert(v860.id == 76)
                  v861.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
                }
                v857.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char.toString + v859.map(x => x.toString).mkString("")
            }
            v862
        }
        v863
    }
    v864
  }

  def matchStringElem(node: Node): StringElem = {
    val BindNode(v865, v866) = node
    val v878 = v865.id match {
      case 166 =>
        val v867 = v866.asInstanceOf[SequenceNode].children.head
        val BindNode(v868, v869) = v867
        assert(v868.id == 167)
        val BindNode(v870, v871) = v869
        assert(v870.id == 34)
        JustChar(v871.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char)(nextId(), v866)
      case 169 =>
        val v872 = v866.asInstanceOf[SequenceNode].children.head
        val BindNode(v873, v874) = v872
        assert(v873.id == 170)
        matchEscapeChar(v874)
      case 174 =>
        val v875 = v866.asInstanceOf[SequenceNode].children.head
        val BindNode(v876, v877) = v875
        assert(v876.id == 175)
        matchStringExpr(v877)
    }
    v878
  }

  def matchStringExpr(node: Node): StringExpr = {
    val BindNode(v879, v880) = node
    val v897 = v879.id match {
      case 176 =>
        val v881 = v880.asInstanceOf[SequenceNode].children.head
        val BindNode(v882, v883) = v881
        assert(v882.id == 177)
        val BindNode(v884, v885) = v883
        assert(v884.id == 178)
        val BindNode(v886, v887) = v885
        val v893 = v886.id match {
          case 179 =>
            val BindNode(v888, v889) = v887
            assert(v888.id == 180)
            val v890 = v889.asInstanceOf[SequenceNode].children(1)
            val BindNode(v891, v892) = v890
            assert(v891.id == 88)
            matchSimpleName(v892)
        }
        SimpleExpr(v893)(nextId(), v880)
      case 182 =>
        val v894 = v880.asInstanceOf[SequenceNode].children(3)
        val BindNode(v895, v896) = v894
        assert(v895.id == 183)
        ComplexExpr(matchExpr(v896))(nextId(), v880)
    }
    v897
  }

  def matchStringLiteral(node: Node): StringLiteral = {
    val BindNode(v898, v899) = node
    val v914 = v898.id match {
      case 157 =>
        val v900 = v899.asInstanceOf[SequenceNode].children(1)
        val v901 = unrollRepeat0(v900).map { elem =>
          val BindNode(v902, v903) = elem
          assert(v902.id == 161)
          val BindNode(v904, v905) = v903
          assert(v904.id == 162)
          val BindNode(v906, v907) = v905
          val v913 = v906.id match {
            case 163 =>
              val BindNode(v908, v909) = v907
              assert(v908.id == 164)
              val v910 = v909.asInstanceOf[SequenceNode].children.head
              val BindNode(v911, v912) = v910
              assert(v911.id == 165)
              matchStringElem(v912)
          }
          v913
        }
        StringLiteral(v901)(nextId(), v899)
    }
    v914
  }

  def matchSuperClassDef(node: Node): SuperClassDef = {
    val BindNode(v915, v916) = node
    val v935 = v915.id match {
      case 392 =>
        val v917 = v916.asInstanceOf[SequenceNode].children(4)
        val BindNode(v918, v919) = v917
        assert(v918.id == 88)
        val v920 = v916.asInstanceOf[SequenceNode].children(8)
        val BindNode(v921, v922) = v920
        assert(v921.id == 88)
        val v923 = v916.asInstanceOf[SequenceNode].children(9)
        val v924 = unrollRepeat0(v923).map { elem =>
          val BindNode(v925, v926) = elem
          assert(v925.id == 398)
          val BindNode(v927, v928) = v926
          val v934 = v927.id match {
            case 399 =>
              val BindNode(v929, v930) = v928
              assert(v929.id == 400)
              val v931 = v930.asInstanceOf[SequenceNode].children(3)
              val BindNode(v932, v933) = v931
              assert(v932.id == 88)
              matchSimpleName(v933)
          }
          v934
        }
        SuperClassDef(matchSimpleName(v919), List(matchSimpleName(v922)) ++ v924)(nextId(), v916)
    }
    v935
  }

  def matchTupleType(node: Node): TupleType = {
    val BindNode(v936, v937) = node
    val v953 = v936.id match {
      case 228 =>
        val v938 = v937.asInstanceOf[SequenceNode].children(2)
        val BindNode(v939, v940) = v938
        assert(v939.id == 209)
        val v941 = v937.asInstanceOf[SequenceNode].children(3)
        val v942 = unrollRepeat0(v941).map { elem =>
          val BindNode(v943, v944) = elem
          assert(v943.id == 222)
          val BindNode(v945, v946) = v944
          val v952 = v945.id match {
            case 223 =>
              val BindNode(v947, v948) = v946
              assert(v947.id == 224)
              val v949 = v948.asInstanceOf[SequenceNode].children(3)
              val BindNode(v950, v951) = v949
              assert(v950.id == 209)
              matchTypeExpr(v951)
          }
          v952
        }
        TupleType(List(matchTypeExpr(v940)) ++ v942)(nextId(), v937)
    }
    v953
  }

  def matchTypeExpr(node: Node): TypeExpr = {
    val BindNode(v954, v955) = node
    val v962 = v954.id match {
      case 210 =>
        val v956 = v955.asInstanceOf[SequenceNode].children.head
        val BindNode(v957, v958) = v956
        assert(v957.id == 185)
        matchNoUnionType(v958)
      case 211 =>
        val v959 = v955.asInstanceOf[SequenceNode].children.head
        val BindNode(v960, v961) = v959
        assert(v960.id == 212)
        matchUnionType(v961)
    }
    v962
  }

  def matchTypeParams(node: Node): TypeParams = {
    val BindNode(v963, v964) = node
    val v980 = v963.id match {
      case 207 =>
        val v965 = v964.asInstanceOf[SequenceNode].children(2)
        val BindNode(v966, v967) = v965
        assert(v966.id == 209)
        val v968 = v964.asInstanceOf[SequenceNode].children(3)
        val v969 = unrollRepeat0(v968).map { elem =>
          val BindNode(v970, v971) = elem
          assert(v970.id == 222)
          val BindNode(v972, v973) = v971
          val v979 = v972.id match {
            case 223 =>
              val BindNode(v974, v975) = v973
              assert(v974.id == 224)
              val v976 = v975.asInstanceOf[SequenceNode].children(3)
              val BindNode(v977, v978) = v976
              assert(v977.id == 209)
              matchTypeExpr(v978)
          }
          v979
        }
        TypeParams(List(matchTypeExpr(v967)) ++ v969)(nextId(), v964)
    }
    v980
  }

  def matchUnionType(node: Node): UnionType = {
    val BindNode(v981, v982) = node
    val v998 = v981.id match {
      case 213 =>
        val v983 = v982.asInstanceOf[SequenceNode].children(2)
        val BindNode(v984, v985) = v983
        assert(v984.id == 185)
        val v986 = v982.asInstanceOf[SequenceNode].children(3)
        val v987 = unrollRepeat0(v986).map { elem =>
          val BindNode(v988, v989) = elem
          assert(v988.id == 216)
          val BindNode(v990, v991) = v989
          val v997 = v990.id match {
            case 217 =>
              val BindNode(v992, v993) = v991
              assert(v992.id == 218)
              val v994 = v993.asInstanceOf[SequenceNode].children(3)
              val BindNode(v995, v996) = v994
              assert(v995.id == 185)
              matchNoUnionType(v996)
          }
          v997
        }
        UnionType(List(matchNoUnionType(v985)) ++ v987)(nextId(), v982)
    }
    v998
  }
}
