package com.giyeok.bibix.ast

import com.giyeok.jparser.Inputs
import com.giyeok.jparser.NGrammar
import com.giyeok.jparser.ParseForest
import com.giyeok.jparser.ParseResultTree.BindNode
import com.giyeok.jparser.ParseResultTree.JoinNode
import com.giyeok.jparser.ParseResultTree.Node
import com.giyeok.jparser.ParseResultTree.SequenceNode
import com.giyeok.jparser.ParseResultTree.TerminalNode
import com.giyeok.jparser.ParsingErrors
import com.giyeok.jparser.milestone.MilestoneParser
import com.giyeok.jparser.milestone.MilestoneParserContext
import com.giyeok.jparser.nparser.ParseTreeUtil
import com.giyeok.jparser.nparser.ParseTreeUtil.unrollRepeat0
import com.giyeok.jparser.nparser.ParseTreeUtil.unrollRepeat1
import com.giyeok.jparser.proto.GrammarProto
import com.giyeok.jparser.proto.GrammarProtobufConverter
import com.giyeok.jparser.proto.MilestoneParserDataProto
import com.giyeok.jparser.proto.MilestoneParserProtobufConverter
import com.giyeok.jparser.utils.FileUtil.readFileBytes
import java.util.Base64
import BibixAst._

object BibixAst {

  sealed trait WithIdAndParseNode {
    val id: Int;
    val parseNode: Node
  }

  case class ActionDef(name: String, argsName: Option[String], expr: CallExpr)(override val id: Int, override val parseNode: Node) extends Def with WithIdAndParseNode

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

  case class EnumDef(name: String, values: List[String])(override val id: Int, override val parseNode: Node) extends Def with WithIdAndParseNode

  case class EscapeChar(code: Char)(override val id: Int, override val parseNode: Node) extends StringElem with WithIdAndParseNode

  sealed trait Expr extends WithIdAndParseNode

  case class ImportAll(source: Primary, rename: Option[String])(override val id: Int, override val parseNode: Node) extends ImportDef with WithIdAndParseNode

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

  case class This()(override val id: Int, override val parseNode: Node) extends Primary with WithIdAndParseNode

  case class TupleExpr(elems: List[Expr])(override val id: Int, override val parseNode: Node) extends Primary with WithIdAndParseNode

  case class TupleType(elems: List[TypeExpr])(override val id: Int, override val parseNode: Node) extends NoUnionType with WithIdAndParseNode

  sealed trait TypeExpr extends WithIdAndParseNode

  case class TypeParams(params: List[TypeExpr])(override val id: Int, override val parseNode: Node) extends WithIdAndParseNode

  case class UnionType(elems: List[NoUnionType])(override val id: Int, override val parseNode: Node) extends TypeExpr with WithIdAndParseNode

  case class VarDef(name: String, typ: Option[TypeExpr], defaultValue: Option[Expr])(override val id: Int, override val parseNode: Node) extends Def with WithIdAndParseNode

  case class VarRedef(nameTokens: List[String], redefValue: Expr)(override val id: Int, override val parseNode: Node) extends Def with WithIdAndParseNode


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
      case 319 =>
        val v3 = v2.asInstanceOf[SequenceNode].children(2)
        val BindNode(v4, v5) = v3
        assert(v4.id == 88)
        val v6 = v2.asInstanceOf[SequenceNode].children(3)
        val BindNode(v7, v8) = v6
        assert(v7.id == 323)
        val BindNode(v9, v10) = v8
        val v19 = v9.id match {
          case 131 =>
            None
          case 324 =>
            val BindNode(v11, v12) = v10
            val v18 = v11.id match {
              case 325 =>
                val BindNode(v13, v14) = v12
                assert(v13.id == 326)
                val v15 = v14.asInstanceOf[SequenceNode].children(1)
                val BindNode(v16, v17) = v15
                assert(v16.id == 327)
                matchActionParams(v17)
            }
            Some(v18)
        }
        val v20 = v2.asInstanceOf[SequenceNode].children(7)
        val BindNode(v21, v22) = v20
        assert(v21.id == 329)
        ActionDef(matchSimpleName(v5), v19, matchActionExpr(v22))(nextId(), v2)
    }
    v23
  }

  def matchActionExpr(node: Node): CallExpr = {
    val BindNode(v24, v25) = node
    val v29 = v24.id match {
      case 143 =>
        val v26 = v25.asInstanceOf[SequenceNode].children.head
        val BindNode(v27, v28) = v26
        assert(v27.id == 144)
        matchCallExpr(v28)
    }
    v29
  }

  def matchActionParams(node: Node): String = {
    val BindNode(v30, v31) = node
    val v35 = v30.id match {
      case 328 =>
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
      case 374 =>
        val v38 = v37.asInstanceOf[SequenceNode].children(4)
        val BindNode(v39, v40) = v38
        assert(v39.id == 88)
        val v41 = v37.asInstanceOf[SequenceNode].children(6)
        val BindNode(v42, v43) = v41
        assert(v42.id == 338)
        val v44 = v37.asInstanceOf[SequenceNode].children(10)
        val BindNode(v45, v46) = v44
        assert(v45.id == 379)
        ActionRuleDef(matchSimpleName(v40), matchParamsDef(v43), matchMethodRef(v46))(nextId(), v37)
    }
    v47
  }

  def matchBooleanLiteral(node: Node): BooleanLiteral = {
    val BindNode(v48, v49) = node
    val v63 = v48.id match {
      case 291 =>
        val v50 = v49.asInstanceOf[SequenceNode].children.head
        val BindNode(v51, v52) = v50
        assert(v51.id == 292)
        val JoinNode(_, v53, _) = v52
        val BindNode(v54, v55) = v53
        assert(v54.id == 293)
        val BindNode(v56, v57) = v55
        val v62 = v56.id match {
          case 294 =>
            val BindNode(v58, v59) = v57
            assert(v58.id == 103)
            BooleanLiteral(true)(nextId(), v59)
          case 295 =>
            val BindNode(v60, v61) = v57
            assert(v60.id == 109)
            BooleanLiteral(false)(nextId(), v61)
        }
        v62
    }
    v63
  }

  def matchBuildRuleDef(node: Node): BuildRuleDef = {
    val BindNode(v64, v65) = node
    val v78 = v64.id match {
      case 423 =>
        val v66 = v65.asInstanceOf[SequenceNode].children(2)
        val BindNode(v67, v68) = v66
        assert(v67.id == 88)
        val v69 = v65.asInstanceOf[SequenceNode].children(4)
        val BindNode(v70, v71) = v69
        assert(v70.id == 338)
        val v72 = v65.asInstanceOf[SequenceNode].children(8)
        val BindNode(v73, v74) = v72
        assert(v73.id == 178)
        val v75 = v65.asInstanceOf[SequenceNode].children(12)
        val BindNode(v76, v77) = v75
        assert(v76.id == 379)
        BuildRuleDef(matchSimpleName(v68), matchParamsDef(v71), matchTypeExpr(v74), matchMethodRef(v77))(nextId(), v65)
    }
    v78
  }

  def matchBuildScript(node: Node): BuildScript = {
    val BindNode(v79, v80) = node
    val v98 = v79.id match {
      case 3 =>
        val v81 = v80.asInstanceOf[SequenceNode].children.head
        val BindNode(v82, v83) = v81
        assert(v82.id == 4)
        val BindNode(v84, v85) = v83
        val v94 = v84.id match {
          case 131 =>
            None
          case 5 =>
            val BindNode(v86, v87) = v85
            val v93 = v86.id match {
              case 6 =>
                val BindNode(v88, v89) = v87
                assert(v88.id == 7)
                val v90 = v89.asInstanceOf[SequenceNode].children(1)
                val BindNode(v91, v92) = v90
                assert(v91.id == 58)
                matchPackageName(v92)
            }
            Some(v93)
        }
        val v95 = v80.asInstanceOf[SequenceNode].children(2)
        val BindNode(v96, v97) = v95
        assert(v96.id == 132)
        BuildScript(v94, matchDefs(v97))(nextId(), v80)
    }
    v98
  }

  def matchCallExpr(node: Node): CallExpr = {
    val BindNode(v99, v100) = node
    val v107 = v99.id match {
      case 145 =>
        val v101 = v100.asInstanceOf[SequenceNode].children.head
        val BindNode(v102, v103) = v101
        assert(v102.id == 86)
        val v104 = v100.asInstanceOf[SequenceNode].children(2)
        val BindNode(v105, v106) = v104
        assert(v105.id == 146)
        CallExpr(matchName(v103), matchCallParams(v106))(nextId(), v100)
    }
    v107
  }

  def matchCallParams(node: Node): CallParams = {
    val BindNode(v108, v109) = node
    val v122 = v108.id match {
      case 147 =>
        CallParams(List(), List())(nextId(), v109)
      case 150 =>
        val v110 = v109.asInstanceOf[SequenceNode].children(2)
        val BindNode(v111, v112) = v110
        assert(v111.id == 151)
        CallParams(matchPositionalParams(v112), List())(nextId(), v109)
      case 225 =>
        val v113 = v109.asInstanceOf[SequenceNode].children(2)
        val BindNode(v114, v115) = v113
        assert(v114.id == 226)
        CallParams(List(), matchNamedParams(v115))(nextId(), v109)
      case 236 =>
        val v116 = v109.asInstanceOf[SequenceNode].children(2)
        val BindNode(v117, v118) = v116
        assert(v117.id == 151)
        val v119 = v109.asInstanceOf[SequenceNode].children(6)
        val BindNode(v120, v121) = v119
        assert(v120.id == 226)
        CallParams(matchPositionalParams(v118), matchNamedParams(v121))(nextId(), v109)
    }
    v122
  }

  def matchClassBody(node: Node): List[ClassBodyElem] = {
    val BindNode(v123, v124) = node
    val v137 = v123.id match {
      case 365 =>
        val v125 = v124.asInstanceOf[SequenceNode].children(1)
        val v126 = unrollRepeat0(v125).map { elem =>
          val BindNode(v127, v128) = elem
          assert(v127.id == 368)
          val BindNode(v129, v130) = v128
          val v136 = v129.id match {
            case 369 =>
              val BindNode(v131, v132) = v130
              assert(v131.id == 370)
              val v133 = v132.asInstanceOf[SequenceNode].children(1)
              val BindNode(v134, v135) = v133
              assert(v134.id == 371)
              matchClassBodyElem(v135)
          }
          v136
        }
        v126
    }
    v137
  }

  def matchClassBodyElem(node: Node): ClassBodyElem = {
    val BindNode(v138, v139) = node
    val v146 = v138.id match {
      case 372 =>
        val v140 = v139.asInstanceOf[SequenceNode].children.head
        val BindNode(v141, v142) = v140
        assert(v141.id == 373)
        matchActionRuleDef(v142)
      case 385 =>
        val v143 = v139.asInstanceOf[SequenceNode].children.head
        val BindNode(v144, v145) = v143
        assert(v144.id == 386)
        matchClassCastDef(v145)
    }
    v146
  }

  def matchClassCastDef(node: Node): ClassCastDef = {
    val BindNode(v147, v148) = node
    val v155 = v147.id match {
      case 387 =>
        val v149 = v148.asInstanceOf[SequenceNode].children(2)
        val BindNode(v150, v151) = v149
        assert(v150.id == 178)
        val v152 = v148.asInstanceOf[SequenceNode].children(6)
        val BindNode(v153, v154) = v152
        assert(v153.id == 153)
        ClassCastDef(matchTypeExpr(v151), matchExpr(v154))(nextId(), v148)
    }
    v155
  }

  def matchClassDef(node: Node): ClassDef = {
    val BindNode(v156, v157) = node
    val v164 = v156.id match {
      case 332 =>
        val v158 = v157.asInstanceOf[SequenceNode].children.head
        val BindNode(v159, v160) = v158
        assert(v159.id == 333)
        matchDataClassDef(v160)
      case 388 =>
        val v161 = v157.asInstanceOf[SequenceNode].children.head
        val BindNode(v162, v163) = v161
        assert(v162.id == 389)
        matchSuperClassDef(v163)
    }
    v164
  }

  def matchCollectionType(node: Node): CollectionType = {
    val BindNode(v165, v166) = node
    val v185 = v165.id match {
      case 164 =>
        val v167 = v166.asInstanceOf[SequenceNode].children.head
        val BindNode(v168, v169) = v167
        assert(v168.id == 165)
        val JoinNode(_, v170, _) = v169
        val BindNode(v171, v172) = v170
        assert(v171.id == 166)
        val BindNode(v173, v174) = v172
        val v181 = v173.id match {
          case 167 =>
            val BindNode(v175, v176) = v174
            assert(v175.id == 168)
            val v177 = v176.asInstanceOf[SequenceNode].children.head
            "set"
          case 171 =>
            val BindNode(v178, v179) = v174
            assert(v178.id == 172)
            val v180 = v179.asInstanceOf[SequenceNode].children.head
            "list"
        }
        val v182 = v166.asInstanceOf[SequenceNode].children(2)
        val BindNode(v183, v184) = v182
        assert(v183.id == 175)
        CollectionType(v181, matchTypeParams(v184))(nextId(), v166)
    }
    v185
  }

  def matchDataClassDef(node: Node): DataClassDef = {
    val BindNode(v186, v187) = node
    val v209 = v186.id match {
      case 334 =>
        val v188 = v187.asInstanceOf[SequenceNode].children(2)
        val BindNode(v189, v190) = v188
        assert(v189.id == 88)
        val v191 = v187.asInstanceOf[SequenceNode].children(4)
        val BindNode(v192, v193) = v191
        assert(v192.id == 338)
        val v195 = v187.asInstanceOf[SequenceNode].children(5)
        val BindNode(v196, v197) = v195
        assert(v196.id == 360)
        val BindNode(v198, v199) = v197
        val v208 = v198.id match {
          case 131 =>
            None
          case 361 =>
            val BindNode(v200, v201) = v199
            val v207 = v200.id match {
              case 362 =>
                val BindNode(v202, v203) = v201
                assert(v202.id == 363)
                val v204 = v203.asInstanceOf[SequenceNode].children(1)
                val BindNode(v205, v206) = v204
                assert(v205.id == 364)
                matchClassBody(v206)
            }
            Some(v207)
        }
        val v194 = v208
        DataClassDef(matchSimpleName(v190), matchParamsDef(v193), if (v194.isDefined) v194.get else List())(nextId(), v187)
    }
    v209
  }

  def matchDef(node: Node): Def = {
    val BindNode(v210, v211) = node
    val v242 = v210.id match {
      case 405 =>
        val v212 = v211.asInstanceOf[SequenceNode].children.head
        val BindNode(v213, v214) = v212
        assert(v213.id == 406)
        matchVarDef(v214)
      case 416 =>
        val v215 = v211.asInstanceOf[SequenceNode].children.head
        val BindNode(v216, v217) = v215
        assert(v216.id == 417)
        matchVarRedef(v217)
      case 330 =>
        val v218 = v211.asInstanceOf[SequenceNode].children.head
        val BindNode(v219, v220) = v218
        assert(v219.id == 331)
        matchClassDef(v220)
      case 421 =>
        val v221 = v211.asInstanceOf[SequenceNode].children.head
        val BindNode(v222, v223) = v221
        assert(v222.id == 422)
        matchBuildRuleDef(v223)
      case 315 =>
        val v224 = v211.asInstanceOf[SequenceNode].children.head
        val BindNode(v225, v226) = v224
        assert(v225.id == 316)
        matchTargetDef(v226)
      case 317 =>
        val v227 = v211.asInstanceOf[SequenceNode].children.head
        val BindNode(v228, v229) = v227
        assert(v228.id == 318)
        matchActionDef(v229)
      case 399 =>
        val v230 = v211.asInstanceOf[SequenceNode].children.head
        val BindNode(v231, v232) = v230
        assert(v231.id == 400)
        matchEnumDef(v232)
      case 135 =>
        val v233 = v211.asInstanceOf[SequenceNode].children.head
        val BindNode(v234, v235) = v233
        assert(v234.id == 136)
        matchImportDef(v235)
      case 309 =>
        val v236 = v211.asInstanceOf[SequenceNode].children.head
        val BindNode(v237, v238) = v236
        assert(v237.id == 310)
        matchNamespaceDef(v238)
      case 372 =>
        val v239 = v211.asInstanceOf[SequenceNode].children.head
        val BindNode(v240, v241) = v239
        assert(v240.id == 373)
        matchActionRuleDef(v241)
    }
    v242
  }

  def matchDefs(node: Node): List[Def] = {
    val BindNode(v243, v244) = node
    val v260 = v243.id match {
      case 133 =>
        val v245 = v244.asInstanceOf[SequenceNode].children.head
        val BindNode(v246, v247) = v245
        assert(v246.id == 134)
        val v248 = v244.asInstanceOf[SequenceNode].children(1)
        val v249 = unrollRepeat0(v248).map { elem =>
          val BindNode(v250, v251) = elem
          assert(v250.id == 426)
          val BindNode(v252, v253) = v251
          val v259 = v252.id match {
            case 427 =>
              val BindNode(v254, v255) = v253
              assert(v254.id == 428)
              val v256 = v255.asInstanceOf[SequenceNode].children(1)
              val BindNode(v257, v258) = v256
              assert(v257.id == 134)
              matchDef(v258)
          }
          v259
        }
        List(matchDef(v247)) ++ v249
    }
    v260
  }

  def matchEnumDef(node: Node): EnumDef = {
    val BindNode(v261, v262) = node
    val v281 = v261.id match {
      case 401 =>
        val v263 = v262.asInstanceOf[SequenceNode].children(2)
        val BindNode(v264, v265) = v263
        assert(v264.id == 88)
        val v266 = v262.asInstanceOf[SequenceNode].children(6)
        val BindNode(v267, v268) = v266
        assert(v267.id == 88)
        val v269 = v262.asInstanceOf[SequenceNode].children(7)
        val v270 = unrollRepeat0(v269).map { elem =>
          val BindNode(v271, v272) = elem
          assert(v271.id == 396)
          val BindNode(v273, v274) = v272
          val v280 = v273.id match {
            case 397 =>
              val BindNode(v275, v276) = v274
              assert(v275.id == 398)
              val v277 = v276.asInstanceOf[SequenceNode].children(3)
              val BindNode(v278, v279) = v277
              assert(v278.id == 88)
              matchSimpleName(v279)
          }
          v280
        }
        EnumDef(matchSimpleName(v265), List(matchSimpleName(v268)) ++ v270)(nextId(), v262)
    }
    v281
  }

  def matchEscapeChar(node: Node): EscapeChar = {
    val BindNode(v282, v283) = node
    val v287 = v282.id match {
      case 277 =>
        val v284 = v283.asInstanceOf[SequenceNode].children(1)
        val BindNode(v285, v286) = v284
        assert(v285.id == 279)
        EscapeChar(v286.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char)(nextId(), v283)
    }
    v287
  }

  def matchExpr(node: Node): Expr = {
    val BindNode(v288, v289) = node
    val v299 = v288.id match {
      case 154 =>
        val v290 = v289.asInstanceOf[SequenceNode].children.head
        val BindNode(v291, v292) = v290
        assert(v291.id == 153)
        val v293 = v289.asInstanceOf[SequenceNode].children(4)
        val BindNode(v294, v295) = v293
        assert(v294.id == 158)
        CastExpr(matchExpr(v292), matchNoUnionType(v295))(nextId(), v289)
      case 211 =>
        val v296 = v289.asInstanceOf[SequenceNode].children.head
        val BindNode(v297, v298) = v296
        assert(v297.id == 212)
        matchMergeOpOrPrimary(v298)
    }
    v299
  }

  def matchImportDef(node: Node): ImportDef = {
    val BindNode(v300, v301) = node
    val v339 = v300.id match {
      case 137 =>
        val v302 = v301.asInstanceOf[SequenceNode].children(2)
        val BindNode(v303, v304) = v302
        assert(v303.id == 142)
        val v305 = v301.asInstanceOf[SequenceNode].children(3)
        val BindNode(v306, v307) = v305
        assert(v306.id == 301)
        val BindNode(v308, v309) = v307
        val v318 = v308.id match {
          case 131 =>
            None
          case 302 =>
            val BindNode(v310, v311) = v309
            val v317 = v310.id match {
              case 303 =>
                val BindNode(v312, v313) = v311
                assert(v312.id == 304)
                val v314 = v313.asInstanceOf[SequenceNode].children(3)
                val BindNode(v315, v316) = v314
                assert(v315.id == 88)
                matchSimpleName(v316)
            }
            Some(v317)
        }
        ImportAll(matchPrimary(v304), v318)(nextId(), v301)
      case 305 =>
        val v319 = v301.asInstanceOf[SequenceNode].children(2)
        val BindNode(v320, v321) = v319
        assert(v320.id == 153)
        val v322 = v301.asInstanceOf[SequenceNode].children(6)
        val BindNode(v323, v324) = v322
        assert(v323.id == 86)
        val v325 = v301.asInstanceOf[SequenceNode].children(7)
        val BindNode(v326, v327) = v325
        assert(v326.id == 301)
        val BindNode(v328, v329) = v327
        val v338 = v328.id match {
          case 131 =>
            None
          case 302 =>
            val BindNode(v330, v331) = v329
            val v337 = v330.id match {
              case 303 =>
                val BindNode(v332, v333) = v331
                assert(v332.id == 304)
                val v334 = v333.asInstanceOf[SequenceNode].children(3)
                val BindNode(v335, v336) = v334
                assert(v335.id == 88)
                matchSimpleName(v336)
            }
            Some(v337)
        }
        ImportFrom(matchExpr(v321), matchName(v324), v338)(nextId(), v301)
    }
    v339
  }

  def matchLiteral(node: Node): Literal = {
    val BindNode(v340, v341) = node
    val v351 = v340.id match {
      case 261 =>
        val v342 = v341.asInstanceOf[SequenceNode].children.head
        val BindNode(v343, v344) = v342
        assert(v343.id == 262)
        matchStringLiteral(v344)
      case 289 =>
        val v345 = v341.asInstanceOf[SequenceNode].children.head
        val BindNode(v346, v347) = v345
        assert(v346.id == 290)
        matchBooleanLiteral(v347)
      case 296 =>
        val v348 = v341.asInstanceOf[SequenceNode].children.head
        val BindNode(v349, v350) = v348
        assert(v349.id == 297)
        matchNoneLiteral(v350)
    }
    v351
  }

  def matchMergeOpOrPrimary(node: Node): MergeOpOrPrimary = {
    val BindNode(v352, v353) = node
    val v363 = v352.id match {
      case 213 =>
        val v354 = v353.asInstanceOf[SequenceNode].children.head
        val BindNode(v355, v356) = v354
        assert(v355.id == 153)
        val v357 = v353.asInstanceOf[SequenceNode].children(4)
        val BindNode(v358, v359) = v357
        assert(v358.id == 142)
        MergeOp(matchExpr(v356), matchPrimary(v359))(nextId(), v353)
      case 215 =>
        val v360 = v353.asInstanceOf[SequenceNode].children.head
        val BindNode(v361, v362) = v360
        assert(v361.id == 142)
        matchPrimary(v362)
    }
    v363
  }

  def matchMethodRef(node: Node): MethodRef = {
    val BindNode(v364, v365) = node
    val v386 = v364.id match {
      case 380 =>
        val v366 = v365.asInstanceOf[SequenceNode].children.head
        val BindNode(v367, v368) = v366
        assert(v367.id == 86)
        val v369 = v365.asInstanceOf[SequenceNode].children(4)
        val BindNode(v370, v371) = v369
        assert(v370.id == 86)
        val v372 = v365.asInstanceOf[SequenceNode].children(5)
        val BindNode(v373, v374) = v372
        assert(v373.id == 381)
        val BindNode(v375, v376) = v374
        val v385 = v375.id match {
          case 131 =>
            None
          case 382 =>
            val BindNode(v377, v378) = v376
            val v384 = v377.id match {
              case 383 =>
                val BindNode(v379, v380) = v378
                assert(v379.id == 384)
                val v381 = v380.asInstanceOf[SequenceNode].children(3)
                val BindNode(v382, v383) = v381
                assert(v382.id == 88)
                matchSimpleName(v383)
            }
            Some(v384)
        }
        MethodRef(matchName(v368), matchName(v371), v385)(nextId(), v365)
    }
    v386
  }

  def matchName(node: Node): Name = {
    val BindNode(v387, v388) = node
    val v404 = v387.id match {
      case 87 =>
        val v389 = v388.asInstanceOf[SequenceNode].children.head
        val BindNode(v390, v391) = v389
        assert(v390.id == 88)
        val v392 = v388.asInstanceOf[SequenceNode].children(1)
        val v393 = unrollRepeat0(v392).map { elem =>
          val BindNode(v394, v395) = elem
          assert(v394.id == 127)
          val BindNode(v396, v397) = v395
          val v403 = v396.id match {
            case 128 =>
              val BindNode(v398, v399) = v397
              assert(v398.id == 129)
              val v400 = v399.asInstanceOf[SequenceNode].children(3)
              val BindNode(v401, v402) = v400
              assert(v401.id == 88)
              matchSimpleName(v402)
          }
          v403
        }
        Name(List(matchSimpleName(v391)) ++ v393)(nextId(), v388)
    }
    v404
  }

  def matchNamedExpr(node: Node): NamedExpr = {
    val BindNode(v405, v406) = node
    val v413 = v405.id match {
      case 253 =>
        val v407 = v406.asInstanceOf[SequenceNode].children.head
        val BindNode(v408, v409) = v407
        assert(v408.id == 88)
        val v410 = v406.asInstanceOf[SequenceNode].children(4)
        val BindNode(v411, v412) = v410
        assert(v411.id == 153)
        NamedExpr(matchSimpleName(v409), matchExpr(v412))(nextId(), v406)
    }
    v413
  }

  def matchNamedParam(node: Node): NamedParam = {
    val BindNode(v414, v415) = node
    val v422 = v414.id match {
      case 229 =>
        val v416 = v415.asInstanceOf[SequenceNode].children.head
        val BindNode(v417, v418) = v416
        assert(v417.id == 88)
        val v419 = v415.asInstanceOf[SequenceNode].children(4)
        val BindNode(v420, v421) = v419
        assert(v420.id == 153)
        NamedParam(matchSimpleName(v418), matchExpr(v421))(nextId(), v415)
    }
    v422
  }

  def matchNamedParams(node: Node): List[NamedParam] = {
    val BindNode(v423, v424) = node
    val v440 = v423.id match {
      case 227 =>
        val v425 = v424.asInstanceOf[SequenceNode].children.head
        val BindNode(v426, v427) = v425
        assert(v426.id == 228)
        val v428 = v424.asInstanceOf[SequenceNode].children(1)
        val v429 = unrollRepeat0(v428).map { elem =>
          val BindNode(v430, v431) = elem
          assert(v430.id == 233)
          val BindNode(v432, v433) = v431
          val v439 = v432.id match {
            case 234 =>
              val BindNode(v434, v435) = v433
              assert(v434.id == 235)
              val v436 = v435.asInstanceOf[SequenceNode].children(3)
              val BindNode(v437, v438) = v436
              assert(v437.id == 228)
              matchNamedParam(v438)
          }
          v439
        }
        List(matchNamedParam(v427)) ++ v429
    }
    v440
  }

  def matchNamedTupleType(node: Node): NamedTupleType = {
    val BindNode(v441, v442) = node
    val v458 = v441.id match {
      case 202 =>
        val v443 = v442.asInstanceOf[SequenceNode].children(2)
        val BindNode(v444, v445) = v443
        assert(v444.id == 203)
        val v446 = v442.asInstanceOf[SequenceNode].children(3)
        val v447 = unrollRepeat0(v446).map { elem =>
          val BindNode(v448, v449) = elem
          assert(v448.id == 208)
          val BindNode(v450, v451) = v449
          val v457 = v450.id match {
            case 209 =>
              val BindNode(v452, v453) = v451
              assert(v452.id == 210)
              val v454 = v453.asInstanceOf[SequenceNode].children(3)
              val BindNode(v455, v456) = v454
              assert(v455.id == 203)
              matchNamedType(v456)
          }
          v457
        }
        NamedTupleType(List(matchNamedType(v445)) ++ v447)(nextId(), v442)
    }
    v458
  }

  def matchNamedType(node: Node): NamedType = {
    val BindNode(v459, v460) = node
    val v467 = v459.id match {
      case 204 =>
        val v461 = v460.asInstanceOf[SequenceNode].children.head
        val BindNode(v462, v463) = v461
        assert(v462.id == 88)
        val v464 = v460.asInstanceOf[SequenceNode].children(4)
        val BindNode(v465, v466) = v464
        assert(v465.id == 178)
        NamedType(matchSimpleName(v463), matchTypeExpr(v466))(nextId(), v460)
    }
    v467
  }

  def matchNamespaceDef(node: Node): NamespaceDef = {
    val BindNode(v468, v469) = node
    val v476 = v468.id match {
      case 311 =>
        val v470 = v469.asInstanceOf[SequenceNode].children(2)
        val BindNode(v471, v472) = v470
        assert(v471.id == 88)
        val v473 = v469.asInstanceOf[SequenceNode].children(5)
        val BindNode(v474, v475) = v473
        assert(v474.id == 2)
        NamespaceDef(matchSimpleName(v472), matchBuildScript(v475))(nextId(), v469)
    }
    v476
  }

  def matchNoUnionType(node: Node): NoUnionType = {
    val BindNode(v477, v478) = node
    val v491 = v477.id match {
      case 200 =>
        val v479 = v478.asInstanceOf[SequenceNode].children.head
        val BindNode(v480, v481) = v479
        assert(v480.id == 201)
        matchNamedTupleType(v481)
      case 160 =>
        NoneType()(nextId(), v478)
      case 159 =>
        val v482 = v478.asInstanceOf[SequenceNode].children.head
        val BindNode(v483, v484) = v482
        assert(v483.id == 86)
        matchName(v484)
      case 197 =>
        val v485 = v478.asInstanceOf[SequenceNode].children.head
        val BindNode(v486, v487) = v485
        assert(v486.id == 198)
        matchTupleType(v487)
      case 162 =>
        val v488 = v478.asInstanceOf[SequenceNode].children.head
        val BindNode(v489, v490) = v488
        assert(v489.id == 163)
        matchCollectionType(v490)
    }
    v491
  }

  def matchNoneLiteral(node: Node): NoneLiteral = {
    val BindNode(v492, v493) = node
    val v494 = v492.id match {
      case 160 =>
        NoneLiteral()(nextId(), v493)
    }
    v494
  }

  def matchPackageName(node: Node): Name = {
    val BindNode(v495, v496) = node
    val v500 = v495.id match {
      case 59 =>
        val v497 = v496.asInstanceOf[SequenceNode].children(2)
        val BindNode(v498, v499) = v497
        assert(v498.id == 86)
        matchName(v499)
    }
    v500
  }

  def matchParamDef(node: Node): ParamDef = {
    val BindNode(v501, v502) = node
    val v543 = v501.id match {
      case 345 =>
        val v503 = v502.asInstanceOf[SequenceNode].children.head
        val BindNode(v504, v505) = v503
        assert(v504.id == 88)
        val v506 = v502.asInstanceOf[SequenceNode].children(1)
        val BindNode(v507, v508) = v506
        assert(v507.id == 346)
        val BindNode(v509, v510) = v508
        val v519 = v509.id match {
          case 131 =>
            None
          case 347 =>
            val BindNode(v511, v512) = v510
            val v518 = v511.id match {
              case 348 =>
                val BindNode(v513, v514) = v512
                assert(v513.id == 349)
                val v515 = v514.asInstanceOf[SequenceNode].children(1)
                val BindNode(v516, v517) = v515
                assert(v516.id == 350)
                v517.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
            }
            Some(v518)
        }
        val v520 = v502.asInstanceOf[SequenceNode].children(5)
        val BindNode(v521, v522) = v520
        assert(v521.id == 178)
        val v523 = v502.asInstanceOf[SequenceNode].children(6)
        val BindNode(v524, v525) = v523
        assert(v524.id == 351)
        val BindNode(v526, v527) = v525
        val v536 = v526.id match {
          case 131 =>
            None
          case 352 =>
            val BindNode(v528, v529) = v527
            val v535 = v528.id match {
              case 353 =>
                val BindNode(v530, v531) = v529
                assert(v530.id == 354)
                val v532 = v531.asInstanceOf[SequenceNode].children(3)
                val BindNode(v533, v534) = v532
                assert(v533.id == 153)
                matchExpr(v534)
            }
            Some(v535)
        }
        ParamDef(matchSimpleName(v505), v519.isDefined, Some(matchTypeExpr(v522)), v536)(nextId(), v502)
      case 229 =>
        val v537 = v502.asInstanceOf[SequenceNode].children.head
        val BindNode(v538, v539) = v537
        assert(v538.id == 88)
        val v540 = v502.asInstanceOf[SequenceNode].children(4)
        val BindNode(v541, v542) = v540
        assert(v541.id == 153)
        ParamDef(matchSimpleName(v539), false, None, Some(matchExpr(v542)))(nextId(), v502)
    }
    v543
  }

  def matchParamsDef(node: Node): List[ParamDef] = {
    val BindNode(v544, v545) = node
    val v572 = v544.id match {
      case 339 =>
        val v547 = v545.asInstanceOf[SequenceNode].children(1)
        val BindNode(v548, v549) = v547
        assert(v548.id == 340)
        val BindNode(v550, v551) = v549
        val v571 = v550.id match {
          case 131 =>
            None
          case 341 =>
            val BindNode(v552, v553) = v551
            assert(v552.id == 342)
            val BindNode(v554, v555) = v553
            assert(v554.id == 343)
            val v556 = v555.asInstanceOf[SequenceNode].children(1)
            val BindNode(v557, v558) = v556
            assert(v557.id == 344)
            val v559 = v555.asInstanceOf[SequenceNode].children(2)
            val v560 = unrollRepeat0(v559).map { elem =>
              val BindNode(v561, v562) = elem
              assert(v561.id == 357)
              val BindNode(v563, v564) = v562
              val v570 = v563.id match {
                case 358 =>
                  val BindNode(v565, v566) = v564
                  assert(v565.id == 359)
                  val v567 = v566.asInstanceOf[SequenceNode].children(3)
                  val BindNode(v568, v569) = v567
                  assert(v568.id == 344)
                  matchParamDef(v569)
              }
              v570
            }
            Some(List(matchParamDef(v558)) ++ v560)
        }
        val v546 = v571
        if (v546.isDefined) v546.get else List()
    }
    v572
  }

  def matchPositionalParams(node: Node): List[Expr] = {
    val BindNode(v573, v574) = node
    val v590 = v573.id match {
      case 152 =>
        val v575 = v574.asInstanceOf[SequenceNode].children.head
        val BindNode(v576, v577) = v575
        assert(v576.id == 153)
        val v578 = v574.asInstanceOf[SequenceNode].children(1)
        val v579 = unrollRepeat0(v578).map { elem =>
          val BindNode(v580, v581) = elem
          assert(v580.id == 218)
          val BindNode(v582, v583) = v581
          val v589 = v582.id match {
            case 219 =>
              val BindNode(v584, v585) = v583
              assert(v584.id == 220)
              val v586 = v585.asInstanceOf[SequenceNode].children(3)
              val BindNode(v587, v588) = v586
              assert(v587.id == 153)
              matchExpr(v588)
          }
          v589
        }
        List(matchExpr(v577)) ++ v579
    }
    v590
  }

  def matchPrimary(node: Node): Primary = {
    val BindNode(v591, v592) = node
    val v693 = v591.id match {
      case 298 =>
        This()(nextId(), v592)
      case 259 =>
        val v593 = v592.asInstanceOf[SequenceNode].children.head
        val BindNode(v594, v595) = v593
        assert(v594.id == 260)
        matchLiteral(v595)
      case 300 =>
        val v596 = v592.asInstanceOf[SequenceNode].children(2)
        val BindNode(v597, v598) = v596
        assert(v597.id == 153)
        Paren(matchExpr(v598))(nextId(), v592)
      case 246 =>
        val v599 = v592.asInstanceOf[SequenceNode].children(2)
        val BindNode(v600, v601) = v599
        assert(v600.id == 153)
        val v603 = v592.asInstanceOf[SequenceNode].children(5)
        val BindNode(v604, v605) = v603
        assert(v604.id == 241)
        val BindNode(v606, v607) = v605
        val v628 = v606.id match {
          case 131 =>
            None
          case 242 =>
            val BindNode(v608, v609) = v607
            val v627 = v608.id match {
              case 243 =>
                val BindNode(v610, v611) = v609
                assert(v610.id == 244)
                val v612 = v611.asInstanceOf[SequenceNode].children(1)
                val BindNode(v613, v614) = v612
                assert(v613.id == 153)
                val v615 = v611.asInstanceOf[SequenceNode].children(2)
                val v616 = unrollRepeat0(v615).map { elem =>
                  val BindNode(v617, v618) = elem
                  assert(v617.id == 218)
                  val BindNode(v619, v620) = v618
                  val v626 = v619.id match {
                    case 219 =>
                      val BindNode(v621, v622) = v620
                      assert(v621.id == 220)
                      val v623 = v622.asInstanceOf[SequenceNode].children(3)
                      val BindNode(v624, v625) = v623
                      assert(v624.id == 153)
                      matchExpr(v625)
                  }
                  v626
                }
                List(matchExpr(v614)) ++ v616
            }
            Some(v627)
        }
        val v602 = v628
        TupleExpr(List(matchExpr(v601)) ++ (if (v602.isDefined) v602.get else List()))(nextId(), v592)
      case 143 =>
        val v629 = v592.asInstanceOf[SequenceNode].children.head
        val BindNode(v630, v631) = v629
        assert(v630.id == 144)
        matchCallExpr(v631)
      case 247 =>
        val v633 = v592.asInstanceOf[SequenceNode].children(1)
        val BindNode(v634, v635) = v633
        assert(v634.id == 248)
        val BindNode(v636, v637) = v635
        val v657 = v636.id match {
          case 131 =>
            None
          case 249 =>
            val BindNode(v638, v639) = v637
            assert(v638.id == 250)
            val BindNode(v640, v641) = v639
            assert(v640.id == 251)
            val v642 = v641.asInstanceOf[SequenceNode].children(1)
            val BindNode(v643, v644) = v642
            assert(v643.id == 252)
            val v645 = v641.asInstanceOf[SequenceNode].children(2)
            val v646 = unrollRepeat0(v645).map { elem =>
              val BindNode(v647, v648) = elem
              assert(v647.id == 256)
              val BindNode(v649, v650) = v648
              val v656 = v649.id match {
                case 257 =>
                  val BindNode(v651, v652) = v650
                  assert(v651.id == 258)
                  val v653 = v652.asInstanceOf[SequenceNode].children(3)
                  val BindNode(v654, v655) = v653
                  assert(v654.id == 252)
                  matchNamedExpr(v655)
              }
              v656
            }
            Some(List(matchNamedExpr(v644)) ++ v646)
        }
        val v632 = v657
        NamedTupleExpr(if (v632.isDefined) v632.get else List())(nextId(), v592)
      case 237 =>
        val v658 = v592.asInstanceOf[SequenceNode].children.head
        val BindNode(v659, v660) = v658
        assert(v659.id == 142)
        val v661 = v592.asInstanceOf[SequenceNode].children(4)
        val BindNode(v662, v663) = v661
        assert(v662.id == 88)
        MemberAccess(matchPrimary(v660), matchSimpleName(v663))(nextId(), v592)
      case 239 =>
        val v665 = v592.asInstanceOf[SequenceNode].children(1)
        val BindNode(v666, v667) = v665
        assert(v666.id == 241)
        val BindNode(v668, v669) = v667
        val v689 = v668.id match {
          case 131 =>
            None
          case 242 =>
            val BindNode(v670, v671) = v669
            assert(v670.id == 243)
            val BindNode(v672, v673) = v671
            assert(v672.id == 244)
            val v674 = v673.asInstanceOf[SequenceNode].children(1)
            val BindNode(v675, v676) = v674
            assert(v675.id == 153)
            val v677 = v673.asInstanceOf[SequenceNode].children(2)
            val v678 = unrollRepeat0(v677).map { elem =>
              val BindNode(v679, v680) = elem
              assert(v679.id == 218)
              val BindNode(v681, v682) = v680
              val v688 = v681.id match {
                case 219 =>
                  val BindNode(v683, v684) = v682
                  assert(v683.id == 220)
                  val v685 = v684.asInstanceOf[SequenceNode].children(3)
                  val BindNode(v686, v687) = v685
                  assert(v686.id == 153)
                  matchExpr(v687)
              }
              v688
            }
            Some(List(matchExpr(v676)) ++ v678)
        }
        val v664 = v689
        ListExpr(if (v664.isDefined) v664.get else List())(nextId(), v592)
      case 238 =>
        val v690 = v592.asInstanceOf[SequenceNode].children.head
        val BindNode(v691, v692) = v690
        assert(v691.id == 88)
        NameRef(matchSimpleName(v692))(nextId(), v592)
    }
    v693
  }

  def matchSimpleName(node: Node): String = {
    val BindNode(v694, v695) = node
    val v726 = v694.id match {
      case 89 =>
        val v696 = v695.asInstanceOf[SequenceNode].children.head
        val BindNode(v697, v698) = v696
        assert(v697.id == 90)
        val BindNode(v699, v700) = v698
        assert(v699.id == 91)
        val BindNode(v701, v702) = v700
        assert(v701.id == 92)
        val BindNode(v703, v704) = v702
        val v725 = v703.id match {
          case 93 =>
            val BindNode(v705, v706) = v704
            assert(v705.id == 94)
            val v707 = v706.asInstanceOf[SequenceNode].children.head
            val BindNode(v708, v709) = v707
            assert(v708.id == 95)
            val JoinNode(_, v710, _) = v709
            val BindNode(v711, v712) = v710
            assert(v711.id == 96)
            val BindNode(v713, v714) = v712
            val v724 = v713.id match {
              case 97 =>
                val BindNode(v715, v716) = v714
                assert(v715.id == 98)
                val v717 = v716.asInstanceOf[SequenceNode].children.head
                val BindNode(v718, v719) = v717
                assert(v718.id == 99)
                val v720 = v716.asInstanceOf[SequenceNode].children(1)
                val v721 = unrollRepeat0(v720).map { elem =>
                  val BindNode(v722, v723) = elem
                  assert(v722.id == 76)
                  v723.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
                }
                v719.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char.toString + v721.map(x => x.toString).mkString("")
            }
            v724
        }
        v725
    }
    v726
  }

  def matchStringElem(node: Node): StringElem = {
    val BindNode(v727, v728) = node
    val v740 = v727.id match {
      case 272 =>
        val v729 = v728.asInstanceOf[SequenceNode].children.head
        val BindNode(v730, v731) = v729
        assert(v730.id == 273)
        val BindNode(v732, v733) = v731
        assert(v732.id == 34)
        JustChar(v733.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char)(nextId(), v728)
      case 275 =>
        val v734 = v728.asInstanceOf[SequenceNode].children.head
        val BindNode(v735, v736) = v734
        assert(v735.id == 276)
        matchEscapeChar(v736)
      case 280 =>
        val v737 = v728.asInstanceOf[SequenceNode].children.head
        val BindNode(v738, v739) = v737
        assert(v738.id == 281)
        matchStringExpr(v739)
    }
    v740
  }

  def matchStringExpr(node: Node): StringExpr = {
    val BindNode(v741, v742) = node
    val v759 = v741.id match {
      case 282 =>
        val v743 = v742.asInstanceOf[SequenceNode].children.head
        val BindNode(v744, v745) = v743
        assert(v744.id == 283)
        val BindNode(v746, v747) = v745
        assert(v746.id == 284)
        val BindNode(v748, v749) = v747
        val v755 = v748.id match {
          case 285 =>
            val BindNode(v750, v751) = v749
            assert(v750.id == 286)
            val v752 = v751.asInstanceOf[SequenceNode].children(1)
            val BindNode(v753, v754) = v752
            assert(v753.id == 88)
            matchSimpleName(v754)
        }
        SimpleExpr(v755)(nextId(), v742)
      case 288 =>
        val v756 = v742.asInstanceOf[SequenceNode].children(3)
        val BindNode(v757, v758) = v756
        assert(v757.id == 153)
        ComplexExpr(matchExpr(v758))(nextId(), v742)
    }
    v759
  }

  def matchStringLiteral(node: Node): StringLiteral = {
    val BindNode(v760, v761) = node
    val v776 = v760.id match {
      case 263 =>
        val v762 = v761.asInstanceOf[SequenceNode].children(1)
        val v763 = unrollRepeat0(v762).map { elem =>
          val BindNode(v764, v765) = elem
          assert(v764.id == 267)
          val BindNode(v766, v767) = v765
          assert(v766.id == 268)
          val BindNode(v768, v769) = v767
          val v775 = v768.id match {
            case 269 =>
              val BindNode(v770, v771) = v769
              assert(v770.id == 270)
              val v772 = v771.asInstanceOf[SequenceNode].children.head
              val BindNode(v773, v774) = v772
              assert(v773.id == 271)
              matchStringElem(v774)
          }
          v775
        }
        StringLiteral(v763)(nextId(), v761)
    }
    v776
  }

  def matchSuperClassDef(node: Node): SuperClassDef = {
    val BindNode(v777, v778) = node
    val v797 = v777.id match {
      case 390 =>
        val v779 = v778.asInstanceOf[SequenceNode].children(4)
        val BindNode(v780, v781) = v779
        assert(v780.id == 88)
        val v782 = v778.asInstanceOf[SequenceNode].children(8)
        val BindNode(v783, v784) = v782
        assert(v783.id == 88)
        val v785 = v778.asInstanceOf[SequenceNode].children(9)
        val v786 = unrollRepeat0(v785).map { elem =>
          val BindNode(v787, v788) = elem
          assert(v787.id == 396)
          val BindNode(v789, v790) = v788
          val v796 = v789.id match {
            case 397 =>
              val BindNode(v791, v792) = v790
              assert(v791.id == 398)
              val v793 = v792.asInstanceOf[SequenceNode].children(3)
              val BindNode(v794, v795) = v793
              assert(v794.id == 88)
              matchSimpleName(v795)
          }
          v796
        }
        SuperClassDef(matchSimpleName(v781), List(matchSimpleName(v784)) ++ v786)(nextId(), v778)
    }
    v797
  }

  def matchTargetDef(node: Node): TargetDef = {
    val BindNode(v798, v799) = node
    val v806 = v798.id match {
      case 229 =>
        val v800 = v799.asInstanceOf[SequenceNode].children.head
        val BindNode(v801, v802) = v800
        assert(v801.id == 88)
        val v803 = v799.asInstanceOf[SequenceNode].children(4)
        val BindNode(v804, v805) = v803
        assert(v804.id == 153)
        TargetDef(matchSimpleName(v802), matchExpr(v805))(nextId(), v799)
    }
    v806
  }

  def matchTupleType(node: Node): TupleType = {
    val BindNode(v807, v808) = node
    val v824 = v807.id match {
      case 199 =>
        val v809 = v808.asInstanceOf[SequenceNode].children(2)
        val BindNode(v810, v811) = v809
        assert(v810.id == 178)
        val v812 = v808.asInstanceOf[SequenceNode].children(3)
        val v813 = unrollRepeat0(v812).map { elem =>
          val BindNode(v814, v815) = elem
          assert(v814.id == 193)
          val BindNode(v816, v817) = v815
          val v823 = v816.id match {
            case 194 =>
              val BindNode(v818, v819) = v817
              assert(v818.id == 195)
              val v820 = v819.asInstanceOf[SequenceNode].children(3)
              val BindNode(v821, v822) = v820
              assert(v821.id == 178)
              matchTypeExpr(v822)
          }
          v823
        }
        TupleType(List(matchTypeExpr(v811)) ++ v813)(nextId(), v808)
    }
    v824
  }

  def matchTypeExpr(node: Node): TypeExpr = {
    val BindNode(v825, v826) = node
    val v833 = v825.id match {
      case 179 =>
        val v827 = v826.asInstanceOf[SequenceNode].children.head
        val BindNode(v828, v829) = v827
        assert(v828.id == 158)
        matchNoUnionType(v829)
      case 180 =>
        val v830 = v826.asInstanceOf[SequenceNode].children.head
        val BindNode(v831, v832) = v830
        assert(v831.id == 181)
        matchUnionType(v832)
    }
    v833
  }

  def matchTypeParams(node: Node): TypeParams = {
    val BindNode(v834, v835) = node
    val v851 = v834.id match {
      case 176 =>
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
        TypeParams(List(matchTypeExpr(v838)) ++ v840)(nextId(), v835)
    }
    v851
  }

  def matchUnionType(node: Node): UnionType = {
    val BindNode(v852, v853) = node
    val v869 = v852.id match {
      case 182 =>
        val v854 = v853.asInstanceOf[SequenceNode].children(2)
        val BindNode(v855, v856) = v854
        assert(v855.id == 158)
        val v857 = v853.asInstanceOf[SequenceNode].children(3)
        val v858 = unrollRepeat0(v857).map { elem =>
          val BindNode(v859, v860) = elem
          assert(v859.id == 186)
          val BindNode(v861, v862) = v860
          val v868 = v861.id match {
            case 187 =>
              val BindNode(v863, v864) = v862
              assert(v863.id == 188)
              val v865 = v864.asInstanceOf[SequenceNode].children(3)
              val BindNode(v866, v867) = v865
              assert(v866.id == 158)
              matchNoUnionType(v867)
          }
          v868
        }
        UnionType(List(matchNoUnionType(v856)) ++ v858)(nextId(), v853)
    }
    v869
  }

  def matchVarDef(node: Node): VarDef = {
    val BindNode(v870, v871) = node
    val v903 = v870.id match {
      case 407 =>
        val v872 = v871.asInstanceOf[SequenceNode].children(2)
        val BindNode(v873, v874) = v872
        assert(v873.id == 88)
        val v875 = v871.asInstanceOf[SequenceNode].children(3)
        val BindNode(v876, v877) = v875
        assert(v876.id == 412)
        val BindNode(v878, v879) = v877
        val v888 = v878.id match {
          case 131 =>
            None
          case 413 =>
            val BindNode(v880, v881) = v879
            val v887 = v880.id match {
              case 414 =>
                val BindNode(v882, v883) = v881
                assert(v882.id == 415)
                val v884 = v883.asInstanceOf[SequenceNode].children(3)
                val BindNode(v885, v886) = v884
                assert(v885.id == 178)
                matchTypeExpr(v886)
            }
            Some(v887)
        }
        val v889 = v871.asInstanceOf[SequenceNode].children(4)
        val BindNode(v890, v891) = v889
        assert(v890.id == 351)
        val BindNode(v892, v893) = v891
        val v902 = v892.id match {
          case 131 =>
            None
          case 352 =>
            val BindNode(v894, v895) = v893
            val v901 = v894.id match {
              case 353 =>
                val BindNode(v896, v897) = v895
                assert(v896.id == 354)
                val v898 = v897.asInstanceOf[SequenceNode].children(3)
                val BindNode(v899, v900) = v898
                assert(v899.id == 153)
                matchExpr(v900)
            }
            Some(v901)
        }
        VarDef(matchSimpleName(v874), v888, v902)(nextId(), v871)
    }
    v903
  }

  def matchVarRedef(node: Node): VarRedef = {
    val BindNode(v904, v905) = node
    val v924 = v904.id match {
      case 418 =>
        val v906 = v905.asInstanceOf[SequenceNode].children(2)
        val BindNode(v907, v908) = v906
        assert(v907.id == 88)
        val v909 = v905.asInstanceOf[SequenceNode].children(3)
        val v910 = unrollRepeat1(v909).map { elem =>
          val BindNode(v911, v912) = elem
          assert(v911.id == 127)
          val BindNode(v913, v914) = v912
          val v920 = v913.id match {
            case 128 =>
              val BindNode(v915, v916) = v914
              assert(v915.id == 129)
              val v917 = v916.asInstanceOf[SequenceNode].children(3)
              val BindNode(v918, v919) = v917
              assert(v918.id == 88)
              matchSimpleName(v919)
          }
          v920
        }
        val v921 = v905.asInstanceOf[SequenceNode].children(7)
        val BindNode(v922, v923) = v921
        assert(v922.id == 153)
        VarRedef(List(matchSimpleName(v908)) ++ v910, matchExpr(v923))(nextId(), v905)
    }
    v924
  }
}
