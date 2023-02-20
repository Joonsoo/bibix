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

  sealed trait ActionBody extends WithIdAndParseNode

  case class ActionDef(name: String, argsName: Option[String], body: ActionBody)(override val id: Int, override val parseNode: Node) extends Def with WithIdAndParseNode

  case class ActionRuleDef(name: String, params: List[ParamDef], impl: MethodRef)(override val id: Int, override val parseNode: Node) extends ClassBodyElem with Def with WithIdAndParseNode

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

  case class EllipsisElem(value: Expr)(override val id: Int, override val parseNode: Node) extends ListElem with WithIdAndParseNode

  case class EnumDef(name: String, values: List[String])(override val id: Int, override val parseNode: Node) extends Def with WithIdAndParseNode

  case class EscapeChar(code: Char)(override val id: Int, override val parseNode: Node) extends StringElem with WithIdAndParseNode

  sealed trait Expr extends ListElem with WithIdAndParseNode

  case class ImportAll(source: Primary, rename: Option[String])(override val id: Int, override val parseNode: Node) extends ImportDef with WithIdAndParseNode

  sealed trait ImportDef extends Def with WithIdAndParseNode

  case class ImportFrom(source: Expr, importing: Name, rename: Option[String])(override val id: Int, override val parseNode: Node) extends ImportDef with WithIdAndParseNode

  case class JustChar(chr: Char)(override val id: Int, override val parseNode: Node) extends StringElem with WithIdAndParseNode

  sealed trait ListElem extends WithIdAndParseNode

  case class ListExpr(elems: List[ListElem])(override val id: Int, override val parseNode: Node) extends Primary with WithIdAndParseNode

  sealed trait Literal extends Primary with WithIdAndParseNode

  case class MemberAccess(target: Primary, name: String)(override val id: Int, override val parseNode: Node) extends Primary with WithIdAndParseNode

  case class MergeOp(lhs: Expr, rhs: Primary)(override val id: Int, override val parseNode: Node) extends MergeOpOrPrimary with WithIdAndParseNode

  sealed trait MergeOpOrPrimary extends Expr with WithIdAndParseNode

  case class MethodRef(targetName: Name, className: Name, methodName: Option[String])(override val id: Int, override val parseNode: Node) extends WithIdAndParseNode

  case class MultiCallActions(exprs: List[CallExpr])(override val id: Int, override val parseNode: Node) extends ActionBody with WithIdAndParseNode

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

  case class SingleCallAction(expr: CallExpr)(override val id: Int, override val parseNode: Node) extends ActionBody with WithIdAndParseNode

  sealed trait StringElem extends WithIdAndParseNode

  sealed trait StringExpr extends StringElem with WithIdAndParseNode

  case class StringLiteral(elems: List[StringElem])(override val id: Int, override val parseNode: Node) extends Literal with WithIdAndParseNode

  case class SuperClassDef(name: String, subs: List[String])(override val id: Int, override val parseNode: Node) extends ClassDef with WithIdAndParseNode

  case class TargetDef(name: String, value: Expr)(override val id: Int, override val parseNode: Node) extends Def with WithIdAndParseNode

  case class This()(override val id: Int, override val parseNode: Node) extends Primary with WithIdAndParseNode

  case class TupleExpr(elems: List[Expr])(override val id: Int, override val parseNode: Node) extends Primary with WithIdAndParseNode

  case class TupleType(elems: List[TypeExpr])(override val id: Int, override val parseNode: Node) extends NoUnionType with WithIdAndParseNode

  sealed trait TypeExpr extends WithIdAndParseNode

  case class TypeParams(params: List[TypeExpr])(override val id: Int, override val parseNode: Node) extends WithIdAndParseNode

  case class UnionType(elems: List[NoUnionType])(override val id: Int, override val parseNode: Node) extends TypeExpr with WithIdAndParseNode

  case class VarDef(name: String, typ: Option[TypeExpr], defaultValue: Option[Expr])(override val id: Int, override val parseNode: Node) extends Def with WithIdAndParseNode

  case class VarRedef(nameTokens: List[String], redefValue: Expr)(override val id: Int, override val parseNode: Node) extends WithIdAndParseNode

  case class VarRedefs(redefs: List[VarRedef])(override val id: Int, override val parseNode: Node) extends Def with WithIdAndParseNode


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

  def matchActionBody(node: Node): ActionBody = {
    val BindNode(v1, v2) = node
    val v18 = v1.id match {
      case 341 =>
        val v3 = v2.asInstanceOf[SequenceNode].children(2)
        val BindNode(v4, v5) = v3
        assert(v4.id == 342)
        SingleCallAction(matchActionExpr(v5))(nextId(), v2)
      case 343 =>
        val v6 = v2.asInstanceOf[SequenceNode].children(1)
        val v7 = unrollRepeat1(v6).map { elem =>
          val BindNode(v8, v9) = elem
          assert(v8.id == 345)
          val BindNode(v10, v11) = v9
          val v17 = v10.id match {
            case 346 =>
              val BindNode(v12, v13) = v11
              assert(v12.id == 347)
              val v14 = v13.asInstanceOf[SequenceNode].children(1)
              val BindNode(v15, v16) = v14
              assert(v15.id == 342)
              matchActionExpr(v16)
          }
          v17
        }
        MultiCallActions(v7)(nextId(), v2)
    }
    v18
  }

  def matchActionDef(node: Node): ActionDef = {
    val BindNode(v19, v20) = node
    val v41 = v19.id match {
      case 330 =>
        val v21 = v20.asInstanceOf[SequenceNode].children(2)
        val BindNode(v22, v23) = v21
        assert(v22.id == 88)
        val v24 = v20.asInstanceOf[SequenceNode].children(3)
        val BindNode(v25, v26) = v24
        assert(v25.id == 334)
        val BindNode(v27, v28) = v26
        val v37 = v27.id match {
          case 131 =>
            None
          case 335 =>
            val BindNode(v29, v30) = v28
            val v36 = v29.id match {
              case 336 =>
                val BindNode(v31, v32) = v30
                assert(v31.id == 337)
                val v33 = v32.asInstanceOf[SequenceNode].children(1)
                val BindNode(v34, v35) = v33
                assert(v34.id == 338)
                matchActionParams(v35)
            }
            Some(v36)
        }
        val v38 = v20.asInstanceOf[SequenceNode].children(5)
        val BindNode(v39, v40) = v38
        assert(v39.id == 340)
        ActionDef(matchSimpleName(v23), v37, matchActionBody(v40))(nextId(), v20)
    }
    v41
  }

  def matchActionExpr(node: Node): CallExpr = {
    val BindNode(v42, v43) = node
    val v47 = v42.id match {
      case 143 =>
        val v44 = v43.asInstanceOf[SequenceNode].children.head
        val BindNode(v45, v46) = v44
        assert(v45.id == 144)
        matchCallExpr(v46)
    }
    v47
  }

  def matchActionParams(node: Node): String = {
    val BindNode(v48, v49) = node
    val v53 = v48.id match {
      case 339 =>
        val v50 = v49.asInstanceOf[SequenceNode].children(2)
        val BindNode(v51, v52) = v50
        assert(v51.id == 88)
        matchSimpleName(v52)
    }
    v53
  }

  def matchActionRuleDef(node: Node): ActionRuleDef = {
    val BindNode(v54, v55) = node
    val v65 = v54.id match {
      case 393 =>
        val v56 = v55.asInstanceOf[SequenceNode].children(4)
        val BindNode(v57, v58) = v56
        assert(v57.id == 88)
        val v59 = v55.asInstanceOf[SequenceNode].children(6)
        val BindNode(v60, v61) = v59
        assert(v60.id == 357)
        val v62 = v55.asInstanceOf[SequenceNode].children(10)
        val BindNode(v63, v64) = v62
        assert(v63.id == 398)
        ActionRuleDef(matchSimpleName(v58), matchParamsDef(v61), matchMethodRef(v64))(nextId(), v55)
    }
    v65
  }

  def matchBooleanLiteral(node: Node): BooleanLiteral = {
    val BindNode(v66, v67) = node
    val v81 = v66.id match {
      case 305 =>
        val v68 = v67.asInstanceOf[SequenceNode].children.head
        val BindNode(v69, v70) = v68
        assert(v69.id == 306)
        val JoinNode(_, v71, _) = v70
        val BindNode(v72, v73) = v71
        assert(v72.id == 307)
        val BindNode(v74, v75) = v73
        val v80 = v74.id match {
          case 308 =>
            val BindNode(v76, v77) = v75
            assert(v76.id == 103)
            BooleanLiteral(true)(nextId(), v77)
          case 309 =>
            val BindNode(v78, v79) = v75
            assert(v78.id == 109)
            BooleanLiteral(false)(nextId(), v79)
        }
        v80
    }
    v81
  }

  def matchBuildRuleDef(node: Node): BuildRuleDef = {
    val BindNode(v82, v83) = node
    val v96 = v82.id match {
      case 449 =>
        val v84 = v83.asInstanceOf[SequenceNode].children(2)
        val BindNode(v85, v86) = v84
        assert(v85.id == 88)
        val v87 = v83.asInstanceOf[SequenceNode].children(4)
        val BindNode(v88, v89) = v87
        assert(v88.id == 357)
        val v90 = v83.asInstanceOf[SequenceNode].children(8)
        val BindNode(v91, v92) = v90
        assert(v91.id == 178)
        val v93 = v83.asInstanceOf[SequenceNode].children(12)
        val BindNode(v94, v95) = v93
        assert(v94.id == 398)
        BuildRuleDef(matchSimpleName(v86), matchParamsDef(v89), matchTypeExpr(v92), matchMethodRef(v95))(nextId(), v83)
    }
    v96
  }

  def matchBuildScript(node: Node): BuildScript = {
    val BindNode(v97, v98) = node
    val v116 = v97.id match {
      case 3 =>
        val v99 = v98.asInstanceOf[SequenceNode].children.head
        val BindNode(v100, v101) = v99
        assert(v100.id == 4)
        val BindNode(v102, v103) = v101
        val v112 = v102.id match {
          case 131 =>
            None
          case 5 =>
            val BindNode(v104, v105) = v103
            val v111 = v104.id match {
              case 6 =>
                val BindNode(v106, v107) = v105
                assert(v106.id == 7)
                val v108 = v107.asInstanceOf[SequenceNode].children(1)
                val BindNode(v109, v110) = v108
                assert(v109.id == 58)
                matchPackageName(v110)
            }
            Some(v111)
        }
        val v113 = v98.asInstanceOf[SequenceNode].children(2)
        val BindNode(v114, v115) = v113
        assert(v114.id == 132)
        BuildScript(v112, matchDefs(v115))(nextId(), v98)
    }
    v116
  }

  def matchCallExpr(node: Node): CallExpr = {
    val BindNode(v117, v118) = node
    val v125 = v117.id match {
      case 145 =>
        val v119 = v118.asInstanceOf[SequenceNode].children.head
        val BindNode(v120, v121) = v119
        assert(v120.id == 86)
        val v122 = v118.asInstanceOf[SequenceNode].children(2)
        val BindNode(v123, v124) = v122
        assert(v123.id == 146)
        CallExpr(matchName(v121), matchCallParams(v124))(nextId(), v118)
    }
    v125
  }

  def matchCallParams(node: Node): CallParams = {
    val BindNode(v126, v127) = node
    val v140 = v126.id match {
      case 147 =>
        CallParams(List(), List())(nextId(), v127)
      case 150 =>
        val v128 = v127.asInstanceOf[SequenceNode].children(2)
        val BindNode(v129, v130) = v128
        assert(v129.id == 151)
        CallParams(matchPositionalParams(v130), List())(nextId(), v127)
      case 225 =>
        val v131 = v127.asInstanceOf[SequenceNode].children(2)
        val BindNode(v132, v133) = v131
        assert(v132.id == 226)
        CallParams(List(), matchNamedParams(v133))(nextId(), v127)
      case 236 =>
        val v134 = v127.asInstanceOf[SequenceNode].children(2)
        val BindNode(v135, v136) = v134
        assert(v135.id == 151)
        val v137 = v127.asInstanceOf[SequenceNode].children(6)
        val BindNode(v138, v139) = v137
        assert(v138.id == 226)
        CallParams(matchPositionalParams(v136), matchNamedParams(v139))(nextId(), v127)
    }
    v140
  }

  def matchClassBody(node: Node): List[ClassBodyElem] = {
    val BindNode(v141, v142) = node
    val v155 = v141.id match {
      case 384 =>
        val v143 = v142.asInstanceOf[SequenceNode].children(1)
        val v144 = unrollRepeat0(v143).map { elem =>
          val BindNode(v145, v146) = elem
          assert(v145.id == 387)
          val BindNode(v147, v148) = v146
          val v154 = v147.id match {
            case 388 =>
              val BindNode(v149, v150) = v148
              assert(v149.id == 389)
              val v151 = v150.asInstanceOf[SequenceNode].children(1)
              val BindNode(v152, v153) = v151
              assert(v152.id == 390)
              matchClassBodyElem(v153)
          }
          v154
        }
        v144
    }
    v155
  }

  def matchClassBodyElem(node: Node): ClassBodyElem = {
    val BindNode(v156, v157) = node
    val v164 = v156.id match {
      case 391 =>
        val v158 = v157.asInstanceOf[SequenceNode].children.head
        val BindNode(v159, v160) = v158
        assert(v159.id == 392)
        matchActionRuleDef(v160)
      case 404 =>
        val v161 = v157.asInstanceOf[SequenceNode].children.head
        val BindNode(v162, v163) = v161
        assert(v162.id == 405)
        matchClassCastDef(v163)
    }
    v164
  }

  def matchClassCastDef(node: Node): ClassCastDef = {
    val BindNode(v165, v166) = node
    val v173 = v165.id match {
      case 406 =>
        val v167 = v166.asInstanceOf[SequenceNode].children(2)
        val BindNode(v168, v169) = v167
        assert(v168.id == 178)
        val v170 = v166.asInstanceOf[SequenceNode].children(6)
        val BindNode(v171, v172) = v170
        assert(v171.id == 153)
        ClassCastDef(matchTypeExpr(v169), matchExpr(v172))(nextId(), v166)
    }
    v173
  }

  def matchClassDef(node: Node): ClassDef = {
    val BindNode(v174, v175) = node
    val v182 = v174.id match {
      case 351 =>
        val v176 = v175.asInstanceOf[SequenceNode].children.head
        val BindNode(v177, v178) = v176
        assert(v177.id == 352)
        matchDataClassDef(v178)
      case 407 =>
        val v179 = v175.asInstanceOf[SequenceNode].children.head
        val BindNode(v180, v181) = v179
        assert(v180.id == 408)
        matchSuperClassDef(v181)
    }
    v182
  }

  def matchCollectionType(node: Node): CollectionType = {
    val BindNode(v183, v184) = node
    val v203 = v183.id match {
      case 164 =>
        val v185 = v184.asInstanceOf[SequenceNode].children.head
        val BindNode(v186, v187) = v185
        assert(v186.id == 165)
        val JoinNode(_, v188, _) = v187
        val BindNode(v189, v190) = v188
        assert(v189.id == 166)
        val BindNode(v191, v192) = v190
        val v199 = v191.id match {
          case 167 =>
            val BindNode(v193, v194) = v192
            assert(v193.id == 168)
            val v195 = v194.asInstanceOf[SequenceNode].children.head
            "set"
          case 171 =>
            val BindNode(v196, v197) = v192
            assert(v196.id == 172)
            val v198 = v197.asInstanceOf[SequenceNode].children.head
            "list"
        }
        val v200 = v184.asInstanceOf[SequenceNode].children(2)
        val BindNode(v201, v202) = v200
        assert(v201.id == 175)
        CollectionType(v199, matchTypeParams(v202))(nextId(), v184)
    }
    v203
  }

  def matchDataClassDef(node: Node): DataClassDef = {
    val BindNode(v204, v205) = node
    val v227 = v204.id match {
      case 353 =>
        val v206 = v205.asInstanceOf[SequenceNode].children(2)
        val BindNode(v207, v208) = v206
        assert(v207.id == 88)
        val v209 = v205.asInstanceOf[SequenceNode].children(4)
        val BindNode(v210, v211) = v209
        assert(v210.id == 357)
        val v213 = v205.asInstanceOf[SequenceNode].children(5)
        val BindNode(v214, v215) = v213
        assert(v214.id == 379)
        val BindNode(v216, v217) = v215
        val v226 = v216.id match {
          case 131 =>
            None
          case 380 =>
            val BindNode(v218, v219) = v217
            val v225 = v218.id match {
              case 381 =>
                val BindNode(v220, v221) = v219
                assert(v220.id == 382)
                val v222 = v221.asInstanceOf[SequenceNode].children(1)
                val BindNode(v223, v224) = v222
                assert(v223.id == 383)
                matchClassBody(v224)
            }
            Some(v225)
        }
        val v212 = v226
        DataClassDef(matchSimpleName(v208), matchParamsDef(v211), if (v212.isDefined) v212.get else List())(nextId(), v205)
    }
    v227
  }

  def matchDef(node: Node): Def = {
    val BindNode(v228, v229) = node
    val v260 = v228.id match {
      case 328 =>
        val v230 = v229.asInstanceOf[SequenceNode].children.head
        val BindNode(v231, v232) = v230
        assert(v231.id == 329)
        matchActionDef(v232)
      case 323 =>
        val v233 = v229.asInstanceOf[SequenceNode].children.head
        val BindNode(v234, v235) = v233
        assert(v234.id == 324)
        matchNamespaceDef(v235)
      case 349 =>
        val v236 = v229.asInstanceOf[SequenceNode].children.head
        val BindNode(v237, v238) = v236
        assert(v237.id == 350)
        matchClassDef(v238)
      case 447 =>
        val v239 = v229.asInstanceOf[SequenceNode].children.head
        val BindNode(v240, v241) = v239
        assert(v240.id == 448)
        matchBuildRuleDef(v241)
      case 391 =>
        val v242 = v229.asInstanceOf[SequenceNode].children.head
        val BindNode(v243, v244) = v242
        assert(v243.id == 392)
        matchActionRuleDef(v244)
      case 418 =>
        val v245 = v229.asInstanceOf[SequenceNode].children.head
        val BindNode(v246, v247) = v245
        assert(v246.id == 419)
        matchEnumDef(v247)
      case 424 =>
        val v248 = v229.asInstanceOf[SequenceNode].children.head
        val BindNode(v249, v250) = v248
        assert(v249.id == 425)
        matchVarDef(v250)
      case 135 =>
        val v251 = v229.asInstanceOf[SequenceNode].children.head
        val BindNode(v252, v253) = v251
        assert(v252.id == 136)
        matchImportDef(v253)
      case 326 =>
        val v254 = v229.asInstanceOf[SequenceNode].children.head
        val BindNode(v255, v256) = v254
        assert(v255.id == 327)
        matchTargetDef(v256)
      case 435 =>
        val v257 = v229.asInstanceOf[SequenceNode].children.head
        val BindNode(v258, v259) = v257
        assert(v258.id == 436)
        matchVarRedefs(v259)
    }
    v260
  }

  def matchDefs(node: Node): List[Def] = {
    val BindNode(v261, v262) = node
    val v278 = v261.id match {
      case 133 =>
        val v263 = v262.asInstanceOf[SequenceNode].children.head
        val BindNode(v264, v265) = v263
        assert(v264.id == 134)
        val v266 = v262.asInstanceOf[SequenceNode].children(1)
        val v267 = unrollRepeat0(v266).map { elem =>
          val BindNode(v268, v269) = elem
          assert(v268.id == 452)
          val BindNode(v270, v271) = v269
          val v277 = v270.id match {
            case 453 =>
              val BindNode(v272, v273) = v271
              assert(v272.id == 454)
              val v274 = v273.asInstanceOf[SequenceNode].children(1)
              val BindNode(v275, v276) = v274
              assert(v275.id == 134)
              matchDef(v276)
          }
          v277
        }
        List(matchDef(v265)) ++ v267
    }
    v278
  }

  def matchEnumDef(node: Node): EnumDef = {
    val BindNode(v279, v280) = node
    val v299 = v279.id match {
      case 420 =>
        val v281 = v280.asInstanceOf[SequenceNode].children(2)
        val BindNode(v282, v283) = v281
        assert(v282.id == 88)
        val v284 = v280.asInstanceOf[SequenceNode].children(6)
        val BindNode(v285, v286) = v284
        assert(v285.id == 88)
        val v287 = v280.asInstanceOf[SequenceNode].children(7)
        val v288 = unrollRepeat0(v287).map { elem =>
          val BindNode(v289, v290) = elem
          assert(v289.id == 415)
          val BindNode(v291, v292) = v290
          val v298 = v291.id match {
            case 416 =>
              val BindNode(v293, v294) = v292
              assert(v293.id == 417)
              val v295 = v294.asInstanceOf[SequenceNode].children(3)
              val BindNode(v296, v297) = v295
              assert(v296.id == 88)
              matchSimpleName(v297)
          }
          v298
        }
        EnumDef(matchSimpleName(v283), List(matchSimpleName(v286)) ++ v288)(nextId(), v280)
    }
    v299
  }

  def matchEscapeChar(node: Node): EscapeChar = {
    val BindNode(v300, v301) = node
    val v305 = v300.id match {
      case 291 =>
        val v302 = v301.asInstanceOf[SequenceNode].children(1)
        val BindNode(v303, v304) = v302
        assert(v303.id == 293)
        EscapeChar(v304.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char)(nextId(), v301)
    }
    v305
  }

  def matchExpr(node: Node): Expr = {
    val BindNode(v306, v307) = node
    val v317 = v306.id match {
      case 154 =>
        val v308 = v307.asInstanceOf[SequenceNode].children.head
        val BindNode(v309, v310) = v308
        assert(v309.id == 153)
        val v311 = v307.asInstanceOf[SequenceNode].children(4)
        val BindNode(v312, v313) = v311
        assert(v312.id == 158)
        CastExpr(matchExpr(v310), matchNoUnionType(v313))(nextId(), v307)
      case 211 =>
        val v314 = v307.asInstanceOf[SequenceNode].children.head
        val BindNode(v315, v316) = v314
        assert(v315.id == 212)
        matchMergeOpOrPrimary(v316)
    }
    v317
  }

  def matchImportDef(node: Node): ImportDef = {
    val BindNode(v318, v319) = node
    val v357 = v318.id match {
      case 137 =>
        val v320 = v319.asInstanceOf[SequenceNode].children(2)
        val BindNode(v321, v322) = v320
        assert(v321.id == 142)
        val v323 = v319.asInstanceOf[SequenceNode].children(3)
        val BindNode(v324, v325) = v323
        assert(v324.id == 315)
        val BindNode(v326, v327) = v325
        val v336 = v326.id match {
          case 131 =>
            None
          case 316 =>
            val BindNode(v328, v329) = v327
            val v335 = v328.id match {
              case 317 =>
                val BindNode(v330, v331) = v329
                assert(v330.id == 318)
                val v332 = v331.asInstanceOf[SequenceNode].children(3)
                val BindNode(v333, v334) = v332
                assert(v333.id == 88)
                matchSimpleName(v334)
            }
            Some(v335)
        }
        ImportAll(matchPrimary(v322), v336)(nextId(), v319)
      case 319 =>
        val v337 = v319.asInstanceOf[SequenceNode].children(2)
        val BindNode(v338, v339) = v337
        assert(v338.id == 153)
        val v340 = v319.asInstanceOf[SequenceNode].children(6)
        val BindNode(v341, v342) = v340
        assert(v341.id == 86)
        val v343 = v319.asInstanceOf[SequenceNode].children(7)
        val BindNode(v344, v345) = v343
        assert(v344.id == 315)
        val BindNode(v346, v347) = v345
        val v356 = v346.id match {
          case 131 =>
            None
          case 316 =>
            val BindNode(v348, v349) = v347
            val v355 = v348.id match {
              case 317 =>
                val BindNode(v350, v351) = v349
                assert(v350.id == 318)
                val v352 = v351.asInstanceOf[SequenceNode].children(3)
                val BindNode(v353, v354) = v352
                assert(v353.id == 88)
                matchSimpleName(v354)
            }
            Some(v355)
        }
        ImportFrom(matchExpr(v339), matchName(v342), v356)(nextId(), v319)
    }
    v357
  }

  def matchListElem(node: Node): ListElem = {
    val BindNode(v358, v359) = node
    val v366 = v358.id match {
      case 246 =>
        val v360 = v359.asInstanceOf[SequenceNode].children.head
        val BindNode(v361, v362) = v360
        assert(v361.id == 153)
        matchExpr(v362)
      case 247 =>
        val v363 = v359.asInstanceOf[SequenceNode].children(2)
        val BindNode(v364, v365) = v363
        assert(v364.id == 153)
        EllipsisElem(matchExpr(v365))(nextId(), v359)
    }
    v366
  }

  def matchLiteral(node: Node): Literal = {
    val BindNode(v367, v368) = node
    val v378 = v367.id match {
      case 275 =>
        val v369 = v368.asInstanceOf[SequenceNode].children.head
        val BindNode(v370, v371) = v369
        assert(v370.id == 276)
        matchStringLiteral(v371)
      case 303 =>
        val v372 = v368.asInstanceOf[SequenceNode].children.head
        val BindNode(v373, v374) = v372
        assert(v373.id == 304)
        matchBooleanLiteral(v374)
      case 310 =>
        val v375 = v368.asInstanceOf[SequenceNode].children.head
        val BindNode(v376, v377) = v375
        assert(v376.id == 311)
        matchNoneLiteral(v377)
    }
    v378
  }

  def matchMergeOpOrPrimary(node: Node): MergeOpOrPrimary = {
    val BindNode(v379, v380) = node
    val v390 = v379.id match {
      case 213 =>
        val v381 = v380.asInstanceOf[SequenceNode].children.head
        val BindNode(v382, v383) = v381
        assert(v382.id == 153)
        val v384 = v380.asInstanceOf[SequenceNode].children(4)
        val BindNode(v385, v386) = v384
        assert(v385.id == 142)
        MergeOp(matchExpr(v383), matchPrimary(v386))(nextId(), v380)
      case 215 =>
        val v387 = v380.asInstanceOf[SequenceNode].children.head
        val BindNode(v388, v389) = v387
        assert(v388.id == 142)
        matchPrimary(v389)
    }
    v390
  }

  def matchMethodRef(node: Node): MethodRef = {
    val BindNode(v391, v392) = node
    val v413 = v391.id match {
      case 399 =>
        val v393 = v392.asInstanceOf[SequenceNode].children.head
        val BindNode(v394, v395) = v393
        assert(v394.id == 86)
        val v396 = v392.asInstanceOf[SequenceNode].children(4)
        val BindNode(v397, v398) = v396
        assert(v397.id == 86)
        val v399 = v392.asInstanceOf[SequenceNode].children(5)
        val BindNode(v400, v401) = v399
        assert(v400.id == 400)
        val BindNode(v402, v403) = v401
        val v412 = v402.id match {
          case 131 =>
            None
          case 401 =>
            val BindNode(v404, v405) = v403
            val v411 = v404.id match {
              case 402 =>
                val BindNode(v406, v407) = v405
                assert(v406.id == 403)
                val v408 = v407.asInstanceOf[SequenceNode].children(3)
                val BindNode(v409, v410) = v408
                assert(v409.id == 88)
                matchSimpleName(v410)
            }
            Some(v411)
        }
        MethodRef(matchName(v395), matchName(v398), v412)(nextId(), v392)
    }
    v413
  }

  def matchName(node: Node): Name = {
    val BindNode(v414, v415) = node
    val v431 = v414.id match {
      case 87 =>
        val v416 = v415.asInstanceOf[SequenceNode].children.head
        val BindNode(v417, v418) = v416
        assert(v417.id == 88)
        val v419 = v415.asInstanceOf[SequenceNode].children(1)
        val v420 = unrollRepeat0(v419).map { elem =>
          val BindNode(v421, v422) = elem
          assert(v421.id == 127)
          val BindNode(v423, v424) = v422
          val v430 = v423.id match {
            case 128 =>
              val BindNode(v425, v426) = v424
              assert(v425.id == 129)
              val v427 = v426.asInstanceOf[SequenceNode].children(3)
              val BindNode(v428, v429) = v427
              assert(v428.id == 88)
              matchSimpleName(v429)
          }
          v430
        }
        Name(List(matchSimpleName(v418)) ++ v420)(nextId(), v415)
    }
    v431
  }

  def matchNamedExpr(node: Node): NamedExpr = {
    val BindNode(v432, v433) = node
    val v440 = v432.id match {
      case 267 =>
        val v434 = v433.asInstanceOf[SequenceNode].children.head
        val BindNode(v435, v436) = v434
        assert(v435.id == 88)
        val v437 = v433.asInstanceOf[SequenceNode].children(4)
        val BindNode(v438, v439) = v437
        assert(v438.id == 153)
        NamedExpr(matchSimpleName(v436), matchExpr(v439))(nextId(), v433)
    }
    v440
  }

  def matchNamedParam(node: Node): NamedParam = {
    val BindNode(v441, v442) = node
    val v449 = v441.id match {
      case 229 =>
        val v443 = v442.asInstanceOf[SequenceNode].children.head
        val BindNode(v444, v445) = v443
        assert(v444.id == 88)
        val v446 = v442.asInstanceOf[SequenceNode].children(4)
        val BindNode(v447, v448) = v446
        assert(v447.id == 153)
        NamedParam(matchSimpleName(v445), matchExpr(v448))(nextId(), v442)
    }
    v449
  }

  def matchNamedParams(node: Node): List[NamedParam] = {
    val BindNode(v450, v451) = node
    val v467 = v450.id match {
      case 227 =>
        val v452 = v451.asInstanceOf[SequenceNode].children.head
        val BindNode(v453, v454) = v452
        assert(v453.id == 228)
        val v455 = v451.asInstanceOf[SequenceNode].children(1)
        val v456 = unrollRepeat0(v455).map { elem =>
          val BindNode(v457, v458) = elem
          assert(v457.id == 233)
          val BindNode(v459, v460) = v458
          val v466 = v459.id match {
            case 234 =>
              val BindNode(v461, v462) = v460
              assert(v461.id == 235)
              val v463 = v462.asInstanceOf[SequenceNode].children(3)
              val BindNode(v464, v465) = v463
              assert(v464.id == 228)
              matchNamedParam(v465)
          }
          v466
        }
        List(matchNamedParam(v454)) ++ v456
    }
    v467
  }

  def matchNamedTupleType(node: Node): NamedTupleType = {
    val BindNode(v468, v469) = node
    val v485 = v468.id match {
      case 202 =>
        val v470 = v469.asInstanceOf[SequenceNode].children(2)
        val BindNode(v471, v472) = v470
        assert(v471.id == 203)
        val v473 = v469.asInstanceOf[SequenceNode].children(3)
        val v474 = unrollRepeat0(v473).map { elem =>
          val BindNode(v475, v476) = elem
          assert(v475.id == 208)
          val BindNode(v477, v478) = v476
          val v484 = v477.id match {
            case 209 =>
              val BindNode(v479, v480) = v478
              assert(v479.id == 210)
              val v481 = v480.asInstanceOf[SequenceNode].children(3)
              val BindNode(v482, v483) = v481
              assert(v482.id == 203)
              matchNamedType(v483)
          }
          v484
        }
        NamedTupleType(List(matchNamedType(v472)) ++ v474)(nextId(), v469)
    }
    v485
  }

  def matchNamedType(node: Node): NamedType = {
    val BindNode(v486, v487) = node
    val v494 = v486.id match {
      case 204 =>
        val v488 = v487.asInstanceOf[SequenceNode].children.head
        val BindNode(v489, v490) = v488
        assert(v489.id == 88)
        val v491 = v487.asInstanceOf[SequenceNode].children(4)
        val BindNode(v492, v493) = v491
        assert(v492.id == 178)
        NamedType(matchSimpleName(v490), matchTypeExpr(v493))(nextId(), v487)
    }
    v494
  }

  def matchNamespaceDef(node: Node): NamespaceDef = {
    val BindNode(v495, v496) = node
    val v503 = v495.id match {
      case 325 =>
        val v497 = v496.asInstanceOf[SequenceNode].children.head
        val BindNode(v498, v499) = v497
        assert(v498.id == 88)
        val v500 = v496.asInstanceOf[SequenceNode].children(3)
        val BindNode(v501, v502) = v500
        assert(v501.id == 2)
        NamespaceDef(matchSimpleName(v499), matchBuildScript(v502))(nextId(), v496)
    }
    v503
  }

  def matchNoUnionType(node: Node): NoUnionType = {
    val BindNode(v504, v505) = node
    val v518 = v504.id match {
      case 200 =>
        val v506 = v505.asInstanceOf[SequenceNode].children.head
        val BindNode(v507, v508) = v506
        assert(v507.id == 201)
        matchNamedTupleType(v508)
      case 197 =>
        val v509 = v505.asInstanceOf[SequenceNode].children.head
        val BindNode(v510, v511) = v509
        assert(v510.id == 198)
        matchTupleType(v511)
      case 160 =>
        NoneType()(nextId(), v505)
      case 162 =>
        val v512 = v505.asInstanceOf[SequenceNode].children.head
        val BindNode(v513, v514) = v512
        assert(v513.id == 163)
        matchCollectionType(v514)
      case 159 =>
        val v515 = v505.asInstanceOf[SequenceNode].children.head
        val BindNode(v516, v517) = v515
        assert(v516.id == 86)
        matchName(v517)
    }
    v518
  }

  def matchNoneLiteral(node: Node): NoneLiteral = {
    val BindNode(v519, v520) = node
    val v521 = v519.id match {
      case 160 =>
        NoneLiteral()(nextId(), v520)
    }
    v521
  }

  def matchPackageName(node: Node): Name = {
    val BindNode(v522, v523) = node
    val v527 = v522.id match {
      case 59 =>
        val v524 = v523.asInstanceOf[SequenceNode].children(2)
        val BindNode(v525, v526) = v524
        assert(v525.id == 86)
        matchName(v526)
    }
    v527
  }

  def matchParamDef(node: Node): ParamDef = {
    val BindNode(v528, v529) = node
    val v570 = v528.id match {
      case 364 =>
        val v530 = v529.asInstanceOf[SequenceNode].children.head
        val BindNode(v531, v532) = v530
        assert(v531.id == 88)
        val v533 = v529.asInstanceOf[SequenceNode].children(1)
        val BindNode(v534, v535) = v533
        assert(v534.id == 365)
        val BindNode(v536, v537) = v535
        val v546 = v536.id match {
          case 131 =>
            None
          case 366 =>
            val BindNode(v538, v539) = v537
            val v545 = v538.id match {
              case 367 =>
                val BindNode(v540, v541) = v539
                assert(v540.id == 368)
                val v542 = v541.asInstanceOf[SequenceNode].children(1)
                val BindNode(v543, v544) = v542
                assert(v543.id == 369)
                v544.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
            }
            Some(v545)
        }
        val v547 = v529.asInstanceOf[SequenceNode].children(5)
        val BindNode(v548, v549) = v547
        assert(v548.id == 178)
        val v550 = v529.asInstanceOf[SequenceNode].children(6)
        val BindNode(v551, v552) = v550
        assert(v551.id == 370)
        val BindNode(v553, v554) = v552
        val v563 = v553.id match {
          case 131 =>
            None
          case 371 =>
            val BindNode(v555, v556) = v554
            val v562 = v555.id match {
              case 372 =>
                val BindNode(v557, v558) = v556
                assert(v557.id == 373)
                val v559 = v558.asInstanceOf[SequenceNode].children(3)
                val BindNode(v560, v561) = v559
                assert(v560.id == 153)
                matchExpr(v561)
            }
            Some(v562)
        }
        ParamDef(matchSimpleName(v532), v546.isDefined, Some(matchTypeExpr(v549)), v563)(nextId(), v529)
      case 229 =>
        val v564 = v529.asInstanceOf[SequenceNode].children.head
        val BindNode(v565, v566) = v564
        assert(v565.id == 88)
        val v567 = v529.asInstanceOf[SequenceNode].children(4)
        val BindNode(v568, v569) = v567
        assert(v568.id == 153)
        ParamDef(matchSimpleName(v566), false, None, Some(matchExpr(v569)))(nextId(), v529)
    }
    v570
  }

  def matchParamsDef(node: Node): List[ParamDef] = {
    val BindNode(v571, v572) = node
    val v599 = v571.id match {
      case 358 =>
        val v574 = v572.asInstanceOf[SequenceNode].children(1)
        val BindNode(v575, v576) = v574
        assert(v575.id == 359)
        val BindNode(v577, v578) = v576
        val v598 = v577.id match {
          case 131 =>
            None
          case 360 =>
            val BindNode(v579, v580) = v578
            assert(v579.id == 361)
            val BindNode(v581, v582) = v580
            assert(v581.id == 362)
            val v583 = v582.asInstanceOf[SequenceNode].children(1)
            val BindNode(v584, v585) = v583
            assert(v584.id == 363)
            val v586 = v582.asInstanceOf[SequenceNode].children(2)
            val v587 = unrollRepeat0(v586).map { elem =>
              val BindNode(v588, v589) = elem
              assert(v588.id == 376)
              val BindNode(v590, v591) = v589
              val v597 = v590.id match {
                case 377 =>
                  val BindNode(v592, v593) = v591
                  assert(v592.id == 378)
                  val v594 = v593.asInstanceOf[SequenceNode].children(3)
                  val BindNode(v595, v596) = v594
                  assert(v595.id == 363)
                  matchParamDef(v596)
              }
              v597
            }
            Some(List(matchParamDef(v585)) ++ v587)
        }
        val v573 = v598
        if (v573.isDefined) v573.get else List()
    }
    v599
  }

  def matchPositionalParams(node: Node): List[Expr] = {
    val BindNode(v600, v601) = node
    val v617 = v600.id match {
      case 152 =>
        val v602 = v601.asInstanceOf[SequenceNode].children.head
        val BindNode(v603, v604) = v602
        assert(v603.id == 153)
        val v605 = v601.asInstanceOf[SequenceNode].children(1)
        val v606 = unrollRepeat0(v605).map { elem =>
          val BindNode(v607, v608) = elem
          assert(v607.id == 218)
          val BindNode(v609, v610) = v608
          val v616 = v609.id match {
            case 219 =>
              val BindNode(v611, v612) = v610
              assert(v611.id == 220)
              val v613 = v612.asInstanceOf[SequenceNode].children(3)
              val BindNode(v614, v615) = v613
              assert(v614.id == 153)
              matchExpr(v615)
          }
          v616
        }
        List(matchExpr(v604)) ++ v606
    }
    v617
  }

  def matchPrimary(node: Node): Primary = {
    val BindNode(v618, v619) = node
    val v720 = v618.id match {
      case 256 =>
        val v620 = v619.asInstanceOf[SequenceNode].children(2)
        val BindNode(v621, v622) = v620
        assert(v621.id == 153)
        val v624 = v619.asInstanceOf[SequenceNode].children(5)
        val BindNode(v625, v626) = v624
        assert(v625.id == 257)
        val BindNode(v627, v628) = v626
        val v649 = v627.id match {
          case 131 =>
            None
          case 258 =>
            val BindNode(v629, v630) = v628
            val v648 = v629.id match {
              case 259 =>
                val BindNode(v631, v632) = v630
                assert(v631.id == 260)
                val v633 = v632.asInstanceOf[SequenceNode].children(1)
                val BindNode(v634, v635) = v633
                assert(v634.id == 153)
                val v636 = v632.asInstanceOf[SequenceNode].children(2)
                val v637 = unrollRepeat0(v636).map { elem =>
                  val BindNode(v638, v639) = elem
                  assert(v638.id == 218)
                  val BindNode(v640, v641) = v639
                  val v647 = v640.id match {
                    case 219 =>
                      val BindNode(v642, v643) = v641
                      assert(v642.id == 220)
                      val v644 = v643.asInstanceOf[SequenceNode].children(3)
                      val BindNode(v645, v646) = v644
                      assert(v645.id == 153)
                      matchExpr(v646)
                  }
                  v647
                }
                List(matchExpr(v635)) ++ v637
            }
            Some(v648)
        }
        val v623 = v649
        TupleExpr(List(matchExpr(v622)) ++ (if (v623.isDefined) v623.get else List()))(nextId(), v619)
      case 143 =>
        val v650 = v619.asInstanceOf[SequenceNode].children.head
        val BindNode(v651, v652) = v650
        assert(v651.id == 144)
        matchCallExpr(v652)
      case 273 =>
        val v653 = v619.asInstanceOf[SequenceNode].children.head
        val BindNode(v654, v655) = v653
        assert(v654.id == 274)
        matchLiteral(v655)
      case 314 =>
        val v656 = v619.asInstanceOf[SequenceNode].children(2)
        val BindNode(v657, v658) = v656
        assert(v657.id == 153)
        Paren(matchExpr(v658))(nextId(), v619)
      case 239 =>
        val v660 = v619.asInstanceOf[SequenceNode].children(1)
        val BindNode(v661, v662) = v660
        assert(v661.id == 241)
        val BindNode(v663, v664) = v662
        val v684 = v663.id match {
          case 131 =>
            None
          case 242 =>
            val BindNode(v665, v666) = v664
            assert(v665.id == 243)
            val BindNode(v667, v668) = v666
            assert(v667.id == 244)
            val v669 = v668.asInstanceOf[SequenceNode].children(1)
            val BindNode(v670, v671) = v669
            assert(v670.id == 245)
            val v672 = v668.asInstanceOf[SequenceNode].children(2)
            val v673 = unrollRepeat0(v672).map { elem =>
              val BindNode(v674, v675) = elem
              assert(v674.id == 252)
              val BindNode(v676, v677) = v675
              val v683 = v676.id match {
                case 253 =>
                  val BindNode(v678, v679) = v677
                  assert(v678.id == 254)
                  val v680 = v679.asInstanceOf[SequenceNode].children(3)
                  val BindNode(v681, v682) = v680
                  assert(v681.id == 245)
                  matchListElem(v682)
              }
              v683
            }
            Some(List(matchListElem(v671)) ++ v673)
        }
        val v659 = v684
        ListExpr(if (v659.isDefined) v659.get else List())(nextId(), v619)
      case 237 =>
        val v685 = v619.asInstanceOf[SequenceNode].children.head
        val BindNode(v686, v687) = v685
        assert(v686.id == 142)
        val v688 = v619.asInstanceOf[SequenceNode].children(4)
        val BindNode(v689, v690) = v688
        assert(v689.id == 88)
        MemberAccess(matchPrimary(v687), matchSimpleName(v690))(nextId(), v619)
      case 312 =>
        This()(nextId(), v619)
      case 261 =>
        val v692 = v619.asInstanceOf[SequenceNode].children(1)
        val BindNode(v693, v694) = v692
        assert(v693.id == 262)
        val BindNode(v695, v696) = v694
        val v716 = v695.id match {
          case 131 =>
            None
          case 263 =>
            val BindNode(v697, v698) = v696
            assert(v697.id == 264)
            val BindNode(v699, v700) = v698
            assert(v699.id == 265)
            val v701 = v700.asInstanceOf[SequenceNode].children(1)
            val BindNode(v702, v703) = v701
            assert(v702.id == 266)
            val v704 = v700.asInstanceOf[SequenceNode].children(2)
            val v705 = unrollRepeat0(v704).map { elem =>
              val BindNode(v706, v707) = elem
              assert(v706.id == 270)
              val BindNode(v708, v709) = v707
              val v715 = v708.id match {
                case 271 =>
                  val BindNode(v710, v711) = v709
                  assert(v710.id == 272)
                  val v712 = v711.asInstanceOf[SequenceNode].children(3)
                  val BindNode(v713, v714) = v712
                  assert(v713.id == 266)
                  matchNamedExpr(v714)
              }
              v715
            }
            Some(List(matchNamedExpr(v703)) ++ v705)
        }
        val v691 = v716
        NamedTupleExpr(if (v691.isDefined) v691.get else List())(nextId(), v619)
      case 238 =>
        val v717 = v619.asInstanceOf[SequenceNode].children.head
        val BindNode(v718, v719) = v717
        assert(v718.id == 88)
        NameRef(matchSimpleName(v719))(nextId(), v619)
    }
    v720
  }

  def matchSimpleName(node: Node): String = {
    val BindNode(v721, v722) = node
    val v753 = v721.id match {
      case 89 =>
        val v723 = v722.asInstanceOf[SequenceNode].children.head
        val BindNode(v724, v725) = v723
        assert(v724.id == 90)
        val BindNode(v726, v727) = v725
        assert(v726.id == 91)
        val BindNode(v728, v729) = v727
        assert(v728.id == 92)
        val BindNode(v730, v731) = v729
        val v752 = v730.id match {
          case 93 =>
            val BindNode(v732, v733) = v731
            assert(v732.id == 94)
            val v734 = v733.asInstanceOf[SequenceNode].children.head
            val BindNode(v735, v736) = v734
            assert(v735.id == 95)
            val JoinNode(_, v737, _) = v736
            val BindNode(v738, v739) = v737
            assert(v738.id == 96)
            val BindNode(v740, v741) = v739
            val v751 = v740.id match {
              case 97 =>
                val BindNode(v742, v743) = v741
                assert(v742.id == 98)
                val v744 = v743.asInstanceOf[SequenceNode].children.head
                val BindNode(v745, v746) = v744
                assert(v745.id == 99)
                val v747 = v743.asInstanceOf[SequenceNode].children(1)
                val v748 = unrollRepeat0(v747).map { elem =>
                  val BindNode(v749, v750) = elem
                  assert(v749.id == 76)
                  v750.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
                }
                v746.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char.toString + v748.map(x => x.toString).mkString("")
            }
            v751
        }
        v752
    }
    v753
  }

  def matchStringElem(node: Node): StringElem = {
    val BindNode(v754, v755) = node
    val v767 = v754.id match {
      case 286 =>
        val v756 = v755.asInstanceOf[SequenceNode].children.head
        val BindNode(v757, v758) = v756
        assert(v757.id == 287)
        val BindNode(v759, v760) = v758
        assert(v759.id == 34)
        JustChar(v760.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char)(nextId(), v755)
      case 289 =>
        val v761 = v755.asInstanceOf[SequenceNode].children.head
        val BindNode(v762, v763) = v761
        assert(v762.id == 290)
        matchEscapeChar(v763)
      case 294 =>
        val v764 = v755.asInstanceOf[SequenceNode].children.head
        val BindNode(v765, v766) = v764
        assert(v765.id == 295)
        matchStringExpr(v766)
    }
    v767
  }

  def matchStringExpr(node: Node): StringExpr = {
    val BindNode(v768, v769) = node
    val v786 = v768.id match {
      case 296 =>
        val v770 = v769.asInstanceOf[SequenceNode].children.head
        val BindNode(v771, v772) = v770
        assert(v771.id == 297)
        val BindNode(v773, v774) = v772
        assert(v773.id == 298)
        val BindNode(v775, v776) = v774
        val v782 = v775.id match {
          case 299 =>
            val BindNode(v777, v778) = v776
            assert(v777.id == 300)
            val v779 = v778.asInstanceOf[SequenceNode].children(1)
            val BindNode(v780, v781) = v779
            assert(v780.id == 88)
            matchSimpleName(v781)
        }
        SimpleExpr(v782)(nextId(), v769)
      case 302 =>
        val v783 = v769.asInstanceOf[SequenceNode].children(3)
        val BindNode(v784, v785) = v783
        assert(v784.id == 153)
        ComplexExpr(matchExpr(v785))(nextId(), v769)
    }
    v786
  }

  def matchStringLiteral(node: Node): StringLiteral = {
    val BindNode(v787, v788) = node
    val v803 = v787.id match {
      case 277 =>
        val v789 = v788.asInstanceOf[SequenceNode].children(1)
        val v790 = unrollRepeat0(v789).map { elem =>
          val BindNode(v791, v792) = elem
          assert(v791.id == 281)
          val BindNode(v793, v794) = v792
          assert(v793.id == 282)
          val BindNode(v795, v796) = v794
          val v802 = v795.id match {
            case 283 =>
              val BindNode(v797, v798) = v796
              assert(v797.id == 284)
              val v799 = v798.asInstanceOf[SequenceNode].children.head
              val BindNode(v800, v801) = v799
              assert(v800.id == 285)
              matchStringElem(v801)
          }
          v802
        }
        StringLiteral(v790)(nextId(), v788)
    }
    v803
  }

  def matchSuperClassDef(node: Node): SuperClassDef = {
    val BindNode(v804, v805) = node
    val v824 = v804.id match {
      case 409 =>
        val v806 = v805.asInstanceOf[SequenceNode].children(4)
        val BindNode(v807, v808) = v806
        assert(v807.id == 88)
        val v809 = v805.asInstanceOf[SequenceNode].children(8)
        val BindNode(v810, v811) = v809
        assert(v810.id == 88)
        val v812 = v805.asInstanceOf[SequenceNode].children(9)
        val v813 = unrollRepeat0(v812).map { elem =>
          val BindNode(v814, v815) = elem
          assert(v814.id == 415)
          val BindNode(v816, v817) = v815
          val v823 = v816.id match {
            case 416 =>
              val BindNode(v818, v819) = v817
              assert(v818.id == 417)
              val v820 = v819.asInstanceOf[SequenceNode].children(3)
              val BindNode(v821, v822) = v820
              assert(v821.id == 88)
              matchSimpleName(v822)
          }
          v823
        }
        SuperClassDef(matchSimpleName(v808), List(matchSimpleName(v811)) ++ v813)(nextId(), v805)
    }
    v824
  }

  def matchTargetDef(node: Node): TargetDef = {
    val BindNode(v825, v826) = node
    val v833 = v825.id match {
      case 229 =>
        val v827 = v826.asInstanceOf[SequenceNode].children.head
        val BindNode(v828, v829) = v827
        assert(v828.id == 88)
        val v830 = v826.asInstanceOf[SequenceNode].children(4)
        val BindNode(v831, v832) = v830
        assert(v831.id == 153)
        TargetDef(matchSimpleName(v829), matchExpr(v832))(nextId(), v826)
    }
    v833
  }

  def matchTupleType(node: Node): TupleType = {
    val BindNode(v834, v835) = node
    val v851 = v834.id match {
      case 199 =>
        val v836 = v835.asInstanceOf[SequenceNode].children(2)
        val BindNode(v837, v838) = v836
        assert(v837.id == 178)
        val v839 = v835.asInstanceOf[SequenceNode].children(3)
        val v840 = unrollRepeat0(v839).map { elem =>
          val BindNode(v841, v842) = elem
          assert(v841.id == 193)
          val BindNode(v843, v844) = v842
          val v850 = v843.id match {
            case 194 =>
              val BindNode(v845, v846) = v844
              assert(v845.id == 195)
              val v847 = v846.asInstanceOf[SequenceNode].children(3)
              val BindNode(v848, v849) = v847
              assert(v848.id == 178)
              matchTypeExpr(v849)
          }
          v850
        }
        TupleType(List(matchTypeExpr(v838)) ++ v840)(nextId(), v835)
    }
    v851
  }

  def matchTypeExpr(node: Node): TypeExpr = {
    val BindNode(v852, v853) = node
    val v860 = v852.id match {
      case 179 =>
        val v854 = v853.asInstanceOf[SequenceNode].children.head
        val BindNode(v855, v856) = v854
        assert(v855.id == 158)
        matchNoUnionType(v856)
      case 180 =>
        val v857 = v853.asInstanceOf[SequenceNode].children.head
        val BindNode(v858, v859) = v857
        assert(v858.id == 181)
        matchUnionType(v859)
    }
    v860
  }

  def matchTypeParams(node: Node): TypeParams = {
    val BindNode(v861, v862) = node
    val v878 = v861.id match {
      case 176 =>
        val v863 = v862.asInstanceOf[SequenceNode].children(2)
        val BindNode(v864, v865) = v863
        assert(v864.id == 178)
        val v866 = v862.asInstanceOf[SequenceNode].children(3)
        val v867 = unrollRepeat0(v866).map { elem =>
          val BindNode(v868, v869) = elem
          assert(v868.id == 193)
          val BindNode(v870, v871) = v869
          val v877 = v870.id match {
            case 194 =>
              val BindNode(v872, v873) = v871
              assert(v872.id == 195)
              val v874 = v873.asInstanceOf[SequenceNode].children(3)
              val BindNode(v875, v876) = v874
              assert(v875.id == 178)
              matchTypeExpr(v876)
          }
          v877
        }
        TypeParams(List(matchTypeExpr(v865)) ++ v867)(nextId(), v862)
    }
    v878
  }

  def matchUnionType(node: Node): UnionType = {
    val BindNode(v879, v880) = node
    val v896 = v879.id match {
      case 182 =>
        val v881 = v880.asInstanceOf[SequenceNode].children(2)
        val BindNode(v882, v883) = v881
        assert(v882.id == 158)
        val v884 = v880.asInstanceOf[SequenceNode].children(3)
        val v885 = unrollRepeat0(v884).map { elem =>
          val BindNode(v886, v887) = elem
          assert(v886.id == 186)
          val BindNode(v888, v889) = v887
          val v895 = v888.id match {
            case 187 =>
              val BindNode(v890, v891) = v889
              assert(v890.id == 188)
              val v892 = v891.asInstanceOf[SequenceNode].children(3)
              val BindNode(v893, v894) = v892
              assert(v893.id == 158)
              matchNoUnionType(v894)
          }
          v895
        }
        UnionType(List(matchNoUnionType(v883)) ++ v885)(nextId(), v880)
    }
    v896
  }

  def matchVarDef(node: Node): VarDef = {
    val BindNode(v897, v898) = node
    val v930 = v897.id match {
      case 426 =>
        val v899 = v898.asInstanceOf[SequenceNode].children(2)
        val BindNode(v900, v901) = v899
        assert(v900.id == 88)
        val v902 = v898.asInstanceOf[SequenceNode].children(3)
        val BindNode(v903, v904) = v902
        assert(v903.id == 431)
        val BindNode(v905, v906) = v904
        val v915 = v905.id match {
          case 131 =>
            None
          case 432 =>
            val BindNode(v907, v908) = v906
            val v914 = v907.id match {
              case 433 =>
                val BindNode(v909, v910) = v908
                assert(v909.id == 434)
                val v911 = v910.asInstanceOf[SequenceNode].children(3)
                val BindNode(v912, v913) = v911
                assert(v912.id == 178)
                matchTypeExpr(v913)
            }
            Some(v914)
        }
        val v916 = v898.asInstanceOf[SequenceNode].children(4)
        val BindNode(v917, v918) = v916
        assert(v917.id == 370)
        val BindNode(v919, v920) = v918
        val v929 = v919.id match {
          case 131 =>
            None
          case 371 =>
            val BindNode(v921, v922) = v920
            val v928 = v921.id match {
              case 372 =>
                val BindNode(v923, v924) = v922
                assert(v923.id == 373)
                val v925 = v924.asInstanceOf[SequenceNode].children(3)
                val BindNode(v926, v927) = v925
                assert(v926.id == 153)
                matchExpr(v927)
            }
            Some(v928)
        }
        VarDef(matchSimpleName(v901), v915, v929)(nextId(), v898)
    }
    v930
  }

  def matchVarRedef(node: Node): VarRedef = {
    val BindNode(v931, v932) = node
    val v951 = v931.id match {
      case 439 =>
        val v933 = v932.asInstanceOf[SequenceNode].children.head
        val BindNode(v934, v935) = v933
        assert(v934.id == 88)
        val v936 = v932.asInstanceOf[SequenceNode].children(1)
        val v937 = unrollRepeat1(v936).map { elem =>
          val BindNode(v938, v939) = elem
          assert(v938.id == 127)
          val BindNode(v940, v941) = v939
          val v947 = v940.id match {
            case 128 =>
              val BindNode(v942, v943) = v941
              assert(v942.id == 129)
              val v944 = v943.asInstanceOf[SequenceNode].children(3)
              val BindNode(v945, v946) = v944
              assert(v945.id == 88)
              matchSimpleName(v946)
          }
          v947
        }
        val v948 = v932.asInstanceOf[SequenceNode].children(5)
        val BindNode(v949, v950) = v948
        assert(v949.id == 153)
        VarRedef(List(matchSimpleName(v935)) ++ v937, matchExpr(v950))(nextId(), v932)
    }
    v951
  }

  def matchVarRedefs(node: Node): VarRedefs = {
    val BindNode(v952, v953) = node
    val v969 = v952.id match {
      case 437 =>
        val v954 = v953.asInstanceOf[SequenceNode].children(2)
        val BindNode(v955, v956) = v954
        assert(v955.id == 438)
        val v957 = v953.asInstanceOf[SequenceNode].children(3)
        val v958 = unrollRepeat0(v957).map { elem =>
          val BindNode(v959, v960) = elem
          assert(v959.id == 444)
          val BindNode(v961, v962) = v960
          val v968 = v961.id match {
            case 445 =>
              val BindNode(v963, v964) = v962
              assert(v963.id == 446)
              val v965 = v964.asInstanceOf[SequenceNode].children(3)
              val BindNode(v966, v967) = v965
              assert(v966.id == 438)
              matchVarRedef(v967)
          }
          v968
        }
        VarRedefs(List(matchVarRedef(v956)) ++ v958)(nextId(), v953)
    }
    v969
  }
}
