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
      case 333 =>
        val v3 = v2.asInstanceOf[SequenceNode].children(2)
        val BindNode(v4, v5) = v3
        assert(v4.id == 88)
        val v6 = v2.asInstanceOf[SequenceNode].children(3)
        val BindNode(v7, v8) = v6
        assert(v7.id == 337)
        val BindNode(v9, v10) = v8
        val v19 = v9.id match {
          case 131 =>
            None
          case 338 =>
            val BindNode(v11, v12) = v10
            val v18 = v11.id match {
              case 339 =>
                val BindNode(v13, v14) = v12
                assert(v13.id == 340)
                val v15 = v14.asInstanceOf[SequenceNode].children(1)
                val BindNode(v16, v17) = v15
                assert(v16.id == 341)
                matchActionParams(v17)
            }
            Some(v18)
        }
        val v20 = v2.asInstanceOf[SequenceNode].children(7)
        val BindNode(v21, v22) = v20
        assert(v21.id == 343)
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
      case 342 =>
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
      case 388 =>
        val v38 = v37.asInstanceOf[SequenceNode].children(4)
        val BindNode(v39, v40) = v38
        assert(v39.id == 88)
        val v41 = v37.asInstanceOf[SequenceNode].children(6)
        val BindNode(v42, v43) = v41
        assert(v42.id == 352)
        val v44 = v37.asInstanceOf[SequenceNode].children(10)
        val BindNode(v45, v46) = v44
        assert(v45.id == 393)
        ActionRuleDef(matchSimpleName(v40), matchParamsDef(v43), matchMethodRef(v46))(nextId(), v37)
    }
    v47
  }

  def matchBooleanLiteral(node: Node): BooleanLiteral = {
    val BindNode(v48, v49) = node
    val v63 = v48.id match {
      case 305 =>
        val v50 = v49.asInstanceOf[SequenceNode].children.head
        val BindNode(v51, v52) = v50
        assert(v51.id == 306)
        val JoinNode(_, v53, _) = v52
        val BindNode(v54, v55) = v53
        assert(v54.id == 307)
        val BindNode(v56, v57) = v55
        val v62 = v56.id match {
          case 308 =>
            val BindNode(v58, v59) = v57
            assert(v58.id == 103)
            BooleanLiteral(true)(nextId(), v59)
          case 309 =>
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
      case 437 =>
        val v66 = v65.asInstanceOf[SequenceNode].children(2)
        val BindNode(v67, v68) = v66
        assert(v67.id == 88)
        val v69 = v65.asInstanceOf[SequenceNode].children(4)
        val BindNode(v70, v71) = v69
        assert(v70.id == 352)
        val v72 = v65.asInstanceOf[SequenceNode].children(8)
        val BindNode(v73, v74) = v72
        assert(v73.id == 178)
        val v75 = v65.asInstanceOf[SequenceNode].children(12)
        val BindNode(v76, v77) = v75
        assert(v76.id == 393)
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
      case 379 =>
        val v125 = v124.asInstanceOf[SequenceNode].children(1)
        val v126 = unrollRepeat0(v125).map { elem =>
          val BindNode(v127, v128) = elem
          assert(v127.id == 382)
          val BindNode(v129, v130) = v128
          val v136 = v129.id match {
            case 383 =>
              val BindNode(v131, v132) = v130
              assert(v131.id == 384)
              val v133 = v132.asInstanceOf[SequenceNode].children(1)
              val BindNode(v134, v135) = v133
              assert(v134.id == 385)
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
      case 386 =>
        val v140 = v139.asInstanceOf[SequenceNode].children.head
        val BindNode(v141, v142) = v140
        assert(v141.id == 387)
        matchActionRuleDef(v142)
      case 399 =>
        val v143 = v139.asInstanceOf[SequenceNode].children.head
        val BindNode(v144, v145) = v143
        assert(v144.id == 400)
        matchClassCastDef(v145)
    }
    v146
  }

  def matchClassCastDef(node: Node): ClassCastDef = {
    val BindNode(v147, v148) = node
    val v155 = v147.id match {
      case 401 =>
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
      case 346 =>
        val v158 = v157.asInstanceOf[SequenceNode].children.head
        val BindNode(v159, v160) = v158
        assert(v159.id == 347)
        matchDataClassDef(v160)
      case 402 =>
        val v161 = v157.asInstanceOf[SequenceNode].children.head
        val BindNode(v162, v163) = v161
        assert(v162.id == 403)
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
      case 348 =>
        val v188 = v187.asInstanceOf[SequenceNode].children(2)
        val BindNode(v189, v190) = v188
        assert(v189.id == 88)
        val v191 = v187.asInstanceOf[SequenceNode].children(4)
        val BindNode(v192, v193) = v191
        assert(v192.id == 352)
        val v195 = v187.asInstanceOf[SequenceNode].children(5)
        val BindNode(v196, v197) = v195
        assert(v196.id == 374)
        val BindNode(v198, v199) = v197
        val v208 = v198.id match {
          case 131 =>
            None
          case 375 =>
            val BindNode(v200, v201) = v199
            val v207 = v200.id match {
              case 376 =>
                val BindNode(v202, v203) = v201
                assert(v202.id == 377)
                val v204 = v203.asInstanceOf[SequenceNode].children(1)
                val BindNode(v205, v206) = v204
                assert(v205.id == 378)
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
      case 419 =>
        val v212 = v211.asInstanceOf[SequenceNode].children.head
        val BindNode(v213, v214) = v212
        assert(v213.id == 420)
        matchVarDef(v214)
      case 430 =>
        val v215 = v211.asInstanceOf[SequenceNode].children.head
        val BindNode(v216, v217) = v215
        assert(v216.id == 431)
        matchVarRedef(v217)
      case 344 =>
        val v218 = v211.asInstanceOf[SequenceNode].children.head
        val BindNode(v219, v220) = v218
        assert(v219.id == 345)
        matchClassDef(v220)
      case 435 =>
        val v221 = v211.asInstanceOf[SequenceNode].children.head
        val BindNode(v222, v223) = v221
        assert(v222.id == 436)
        matchBuildRuleDef(v223)
      case 329 =>
        val v224 = v211.asInstanceOf[SequenceNode].children.head
        val BindNode(v225, v226) = v224
        assert(v225.id == 330)
        matchTargetDef(v226)
      case 331 =>
        val v227 = v211.asInstanceOf[SequenceNode].children.head
        val BindNode(v228, v229) = v227
        assert(v228.id == 332)
        matchActionDef(v229)
      case 413 =>
        val v230 = v211.asInstanceOf[SequenceNode].children.head
        val BindNode(v231, v232) = v230
        assert(v231.id == 414)
        matchEnumDef(v232)
      case 135 =>
        val v233 = v211.asInstanceOf[SequenceNode].children.head
        val BindNode(v234, v235) = v233
        assert(v234.id == 136)
        matchImportDef(v235)
      case 323 =>
        val v236 = v211.asInstanceOf[SequenceNode].children.head
        val BindNode(v237, v238) = v236
        assert(v237.id == 324)
        matchNamespaceDef(v238)
      case 386 =>
        val v239 = v211.asInstanceOf[SequenceNode].children.head
        val BindNode(v240, v241) = v239
        assert(v240.id == 387)
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
          assert(v250.id == 440)
          val BindNode(v252, v253) = v251
          val v259 = v252.id match {
            case 441 =>
              val BindNode(v254, v255) = v253
              assert(v254.id == 442)
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
      case 415 =>
        val v263 = v262.asInstanceOf[SequenceNode].children(2)
        val BindNode(v264, v265) = v263
        assert(v264.id == 88)
        val v266 = v262.asInstanceOf[SequenceNode].children(6)
        val BindNode(v267, v268) = v266
        assert(v267.id == 88)
        val v269 = v262.asInstanceOf[SequenceNode].children(7)
        val v270 = unrollRepeat0(v269).map { elem =>
          val BindNode(v271, v272) = elem
          assert(v271.id == 410)
          val BindNode(v273, v274) = v272
          val v280 = v273.id match {
            case 411 =>
              val BindNode(v275, v276) = v274
              assert(v275.id == 412)
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
      case 291 =>
        val v284 = v283.asInstanceOf[SequenceNode].children(1)
        val BindNode(v285, v286) = v284
        assert(v285.id == 293)
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
        assert(v306.id == 315)
        val BindNode(v308, v309) = v307
        val v318 = v308.id match {
          case 131 =>
            None
          case 316 =>
            val BindNode(v310, v311) = v309
            val v317 = v310.id match {
              case 317 =>
                val BindNode(v312, v313) = v311
                assert(v312.id == 318)
                val v314 = v313.asInstanceOf[SequenceNode].children(3)
                val BindNode(v315, v316) = v314
                assert(v315.id == 88)
                matchSimpleName(v316)
            }
            Some(v317)
        }
        ImportAll(matchPrimary(v304), v318)(nextId(), v301)
      case 319 =>
        val v319 = v301.asInstanceOf[SequenceNode].children(2)
        val BindNode(v320, v321) = v319
        assert(v320.id == 153)
        val v322 = v301.asInstanceOf[SequenceNode].children(6)
        val BindNode(v323, v324) = v322
        assert(v323.id == 86)
        val v325 = v301.asInstanceOf[SequenceNode].children(7)
        val BindNode(v326, v327) = v325
        assert(v326.id == 315)
        val BindNode(v328, v329) = v327
        val v338 = v328.id match {
          case 131 =>
            None
          case 316 =>
            val BindNode(v330, v331) = v329
            val v337 = v330.id match {
              case 317 =>
                val BindNode(v332, v333) = v331
                assert(v332.id == 318)
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

  def matchListElem(node: Node): ListElem = {
    val BindNode(v340, v341) = node
    val v348 = v340.id match {
      case 246 =>
        val v342 = v341.asInstanceOf[SequenceNode].children.head
        val BindNode(v343, v344) = v342
        assert(v343.id == 153)
        matchExpr(v344)
      case 247 =>
        val v345 = v341.asInstanceOf[SequenceNode].children(2)
        val BindNode(v346, v347) = v345
        assert(v346.id == 153)
        EllipsisElem(matchExpr(v347))(nextId(), v341)
    }
    v348
  }

  def matchLiteral(node: Node): Literal = {
    val BindNode(v349, v350) = node
    val v360 = v349.id match {
      case 275 =>
        val v351 = v350.asInstanceOf[SequenceNode].children.head
        val BindNode(v352, v353) = v351
        assert(v352.id == 276)
        matchStringLiteral(v353)
      case 303 =>
        val v354 = v350.asInstanceOf[SequenceNode].children.head
        val BindNode(v355, v356) = v354
        assert(v355.id == 304)
        matchBooleanLiteral(v356)
      case 310 =>
        val v357 = v350.asInstanceOf[SequenceNode].children.head
        val BindNode(v358, v359) = v357
        assert(v358.id == 311)
        matchNoneLiteral(v359)
    }
    v360
  }

  def matchMergeOpOrPrimary(node: Node): MergeOpOrPrimary = {
    val BindNode(v361, v362) = node
    val v372 = v361.id match {
      case 213 =>
        val v363 = v362.asInstanceOf[SequenceNode].children.head
        val BindNode(v364, v365) = v363
        assert(v364.id == 153)
        val v366 = v362.asInstanceOf[SequenceNode].children(4)
        val BindNode(v367, v368) = v366
        assert(v367.id == 142)
        MergeOp(matchExpr(v365), matchPrimary(v368))(nextId(), v362)
      case 215 =>
        val v369 = v362.asInstanceOf[SequenceNode].children.head
        val BindNode(v370, v371) = v369
        assert(v370.id == 142)
        matchPrimary(v371)
    }
    v372
  }

  def matchMethodRef(node: Node): MethodRef = {
    val BindNode(v373, v374) = node
    val v395 = v373.id match {
      case 394 =>
        val v375 = v374.asInstanceOf[SequenceNode].children.head
        val BindNode(v376, v377) = v375
        assert(v376.id == 86)
        val v378 = v374.asInstanceOf[SequenceNode].children(4)
        val BindNode(v379, v380) = v378
        assert(v379.id == 86)
        val v381 = v374.asInstanceOf[SequenceNode].children(5)
        val BindNode(v382, v383) = v381
        assert(v382.id == 395)
        val BindNode(v384, v385) = v383
        val v394 = v384.id match {
          case 131 =>
            None
          case 396 =>
            val BindNode(v386, v387) = v385
            val v393 = v386.id match {
              case 397 =>
                val BindNode(v388, v389) = v387
                assert(v388.id == 398)
                val v390 = v389.asInstanceOf[SequenceNode].children(3)
                val BindNode(v391, v392) = v390
                assert(v391.id == 88)
                matchSimpleName(v392)
            }
            Some(v393)
        }
        MethodRef(matchName(v377), matchName(v380), v394)(nextId(), v374)
    }
    v395
  }

  def matchName(node: Node): Name = {
    val BindNode(v396, v397) = node
    val v413 = v396.id match {
      case 87 =>
        val v398 = v397.asInstanceOf[SequenceNode].children.head
        val BindNode(v399, v400) = v398
        assert(v399.id == 88)
        val v401 = v397.asInstanceOf[SequenceNode].children(1)
        val v402 = unrollRepeat0(v401).map { elem =>
          val BindNode(v403, v404) = elem
          assert(v403.id == 127)
          val BindNode(v405, v406) = v404
          val v412 = v405.id match {
            case 128 =>
              val BindNode(v407, v408) = v406
              assert(v407.id == 129)
              val v409 = v408.asInstanceOf[SequenceNode].children(3)
              val BindNode(v410, v411) = v409
              assert(v410.id == 88)
              matchSimpleName(v411)
          }
          v412
        }
        Name(List(matchSimpleName(v400)) ++ v402)(nextId(), v397)
    }
    v413
  }

  def matchNamedExpr(node: Node): NamedExpr = {
    val BindNode(v414, v415) = node
    val v422 = v414.id match {
      case 267 =>
        val v416 = v415.asInstanceOf[SequenceNode].children.head
        val BindNode(v417, v418) = v416
        assert(v417.id == 88)
        val v419 = v415.asInstanceOf[SequenceNode].children(4)
        val BindNode(v420, v421) = v419
        assert(v420.id == 153)
        NamedExpr(matchSimpleName(v418), matchExpr(v421))(nextId(), v415)
    }
    v422
  }

  def matchNamedParam(node: Node): NamedParam = {
    val BindNode(v423, v424) = node
    val v431 = v423.id match {
      case 229 =>
        val v425 = v424.asInstanceOf[SequenceNode].children.head
        val BindNode(v426, v427) = v425
        assert(v426.id == 88)
        val v428 = v424.asInstanceOf[SequenceNode].children(4)
        val BindNode(v429, v430) = v428
        assert(v429.id == 153)
        NamedParam(matchSimpleName(v427), matchExpr(v430))(nextId(), v424)
    }
    v431
  }

  def matchNamedParams(node: Node): List[NamedParam] = {
    val BindNode(v432, v433) = node
    val v449 = v432.id match {
      case 227 =>
        val v434 = v433.asInstanceOf[SequenceNode].children.head
        val BindNode(v435, v436) = v434
        assert(v435.id == 228)
        val v437 = v433.asInstanceOf[SequenceNode].children(1)
        val v438 = unrollRepeat0(v437).map { elem =>
          val BindNode(v439, v440) = elem
          assert(v439.id == 233)
          val BindNode(v441, v442) = v440
          val v448 = v441.id match {
            case 234 =>
              val BindNode(v443, v444) = v442
              assert(v443.id == 235)
              val v445 = v444.asInstanceOf[SequenceNode].children(3)
              val BindNode(v446, v447) = v445
              assert(v446.id == 228)
              matchNamedParam(v447)
          }
          v448
        }
        List(matchNamedParam(v436)) ++ v438
    }
    v449
  }

  def matchNamedTupleType(node: Node): NamedTupleType = {
    val BindNode(v450, v451) = node
    val v467 = v450.id match {
      case 202 =>
        val v452 = v451.asInstanceOf[SequenceNode].children(2)
        val BindNode(v453, v454) = v452
        assert(v453.id == 203)
        val v455 = v451.asInstanceOf[SequenceNode].children(3)
        val v456 = unrollRepeat0(v455).map { elem =>
          val BindNode(v457, v458) = elem
          assert(v457.id == 208)
          val BindNode(v459, v460) = v458
          val v466 = v459.id match {
            case 209 =>
              val BindNode(v461, v462) = v460
              assert(v461.id == 210)
              val v463 = v462.asInstanceOf[SequenceNode].children(3)
              val BindNode(v464, v465) = v463
              assert(v464.id == 203)
              matchNamedType(v465)
          }
          v466
        }
        NamedTupleType(List(matchNamedType(v454)) ++ v456)(nextId(), v451)
    }
    v467
  }

  def matchNamedType(node: Node): NamedType = {
    val BindNode(v468, v469) = node
    val v476 = v468.id match {
      case 204 =>
        val v470 = v469.asInstanceOf[SequenceNode].children.head
        val BindNode(v471, v472) = v470
        assert(v471.id == 88)
        val v473 = v469.asInstanceOf[SequenceNode].children(4)
        val BindNode(v474, v475) = v473
        assert(v474.id == 178)
        NamedType(matchSimpleName(v472), matchTypeExpr(v475))(nextId(), v469)
    }
    v476
  }

  def matchNamespaceDef(node: Node): NamespaceDef = {
    val BindNode(v477, v478) = node
    val v485 = v477.id match {
      case 325 =>
        val v479 = v478.asInstanceOf[SequenceNode].children(2)
        val BindNode(v480, v481) = v479
        assert(v480.id == 88)
        val v482 = v478.asInstanceOf[SequenceNode].children(5)
        val BindNode(v483, v484) = v482
        assert(v483.id == 2)
        NamespaceDef(matchSimpleName(v481), matchBuildScript(v484))(nextId(), v478)
    }
    v485
  }

  def matchNoUnionType(node: Node): NoUnionType = {
    val BindNode(v486, v487) = node
    val v500 = v486.id match {
      case 200 =>
        val v488 = v487.asInstanceOf[SequenceNode].children.head
        val BindNode(v489, v490) = v488
        assert(v489.id == 201)
        matchNamedTupleType(v490)
      case 159 =>
        val v491 = v487.asInstanceOf[SequenceNode].children.head
        val BindNode(v492, v493) = v491
        assert(v492.id == 86)
        matchName(v493)
      case 197 =>
        val v494 = v487.asInstanceOf[SequenceNode].children.head
        val BindNode(v495, v496) = v494
        assert(v495.id == 198)
        matchTupleType(v496)
      case 162 =>
        val v497 = v487.asInstanceOf[SequenceNode].children.head
        val BindNode(v498, v499) = v497
        assert(v498.id == 163)
        matchCollectionType(v499)
      case 160 =>
        NoneType()(nextId(), v487)
    }
    v500
  }

  def matchNoneLiteral(node: Node): NoneLiteral = {
    val BindNode(v501, v502) = node
    val v503 = v501.id match {
      case 160 =>
        NoneLiteral()(nextId(), v502)
    }
    v503
  }

  def matchPackageName(node: Node): Name = {
    val BindNode(v504, v505) = node
    val v509 = v504.id match {
      case 59 =>
        val v506 = v505.asInstanceOf[SequenceNode].children(2)
        val BindNode(v507, v508) = v506
        assert(v507.id == 86)
        matchName(v508)
    }
    v509
  }

  def matchParamDef(node: Node): ParamDef = {
    val BindNode(v510, v511) = node
    val v552 = v510.id match {
      case 359 =>
        val v512 = v511.asInstanceOf[SequenceNode].children.head
        val BindNode(v513, v514) = v512
        assert(v513.id == 88)
        val v515 = v511.asInstanceOf[SequenceNode].children(1)
        val BindNode(v516, v517) = v515
        assert(v516.id == 360)
        val BindNode(v518, v519) = v517
        val v528 = v518.id match {
          case 131 =>
            None
          case 361 =>
            val BindNode(v520, v521) = v519
            val v527 = v520.id match {
              case 362 =>
                val BindNode(v522, v523) = v521
                assert(v522.id == 363)
                val v524 = v523.asInstanceOf[SequenceNode].children(1)
                val BindNode(v525, v526) = v524
                assert(v525.id == 364)
                v526.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
            }
            Some(v527)
        }
        val v529 = v511.asInstanceOf[SequenceNode].children(5)
        val BindNode(v530, v531) = v529
        assert(v530.id == 178)
        val v532 = v511.asInstanceOf[SequenceNode].children(6)
        val BindNode(v533, v534) = v532
        assert(v533.id == 365)
        val BindNode(v535, v536) = v534
        val v545 = v535.id match {
          case 131 =>
            None
          case 366 =>
            val BindNode(v537, v538) = v536
            val v544 = v537.id match {
              case 367 =>
                val BindNode(v539, v540) = v538
                assert(v539.id == 368)
                val v541 = v540.asInstanceOf[SequenceNode].children(3)
                val BindNode(v542, v543) = v541
                assert(v542.id == 153)
                matchExpr(v543)
            }
            Some(v544)
        }
        ParamDef(matchSimpleName(v514), v528.isDefined, Some(matchTypeExpr(v531)), v545)(nextId(), v511)
      case 229 =>
        val v546 = v511.asInstanceOf[SequenceNode].children.head
        val BindNode(v547, v548) = v546
        assert(v547.id == 88)
        val v549 = v511.asInstanceOf[SequenceNode].children(4)
        val BindNode(v550, v551) = v549
        assert(v550.id == 153)
        ParamDef(matchSimpleName(v548), false, None, Some(matchExpr(v551)))(nextId(), v511)
    }
    v552
  }

  def matchParamsDef(node: Node): List[ParamDef] = {
    val BindNode(v553, v554) = node
    val v581 = v553.id match {
      case 353 =>
        val v556 = v554.asInstanceOf[SequenceNode].children(1)
        val BindNode(v557, v558) = v556
        assert(v557.id == 354)
        val BindNode(v559, v560) = v558
        val v580 = v559.id match {
          case 131 =>
            None
          case 355 =>
            val BindNode(v561, v562) = v560
            assert(v561.id == 356)
            val BindNode(v563, v564) = v562
            assert(v563.id == 357)
            val v565 = v564.asInstanceOf[SequenceNode].children(1)
            val BindNode(v566, v567) = v565
            assert(v566.id == 358)
            val v568 = v564.asInstanceOf[SequenceNode].children(2)
            val v569 = unrollRepeat0(v568).map { elem =>
              val BindNode(v570, v571) = elem
              assert(v570.id == 371)
              val BindNode(v572, v573) = v571
              val v579 = v572.id match {
                case 372 =>
                  val BindNode(v574, v575) = v573
                  assert(v574.id == 373)
                  val v576 = v575.asInstanceOf[SequenceNode].children(3)
                  val BindNode(v577, v578) = v576
                  assert(v577.id == 358)
                  matchParamDef(v578)
              }
              v579
            }
            Some(List(matchParamDef(v567)) ++ v569)
        }
        val v555 = v580
        if (v555.isDefined) v555.get else List()
    }
    v581
  }

  def matchPositionalParams(node: Node): List[Expr] = {
    val BindNode(v582, v583) = node
    val v599 = v582.id match {
      case 152 =>
        val v584 = v583.asInstanceOf[SequenceNode].children.head
        val BindNode(v585, v586) = v584
        assert(v585.id == 153)
        val v587 = v583.asInstanceOf[SequenceNode].children(1)
        val v588 = unrollRepeat0(v587).map { elem =>
          val BindNode(v589, v590) = elem
          assert(v589.id == 218)
          val BindNode(v591, v592) = v590
          val v598 = v591.id match {
            case 219 =>
              val BindNode(v593, v594) = v592
              assert(v593.id == 220)
              val v595 = v594.asInstanceOf[SequenceNode].children(3)
              val BindNode(v596, v597) = v595
              assert(v596.id == 153)
              matchExpr(v597)
          }
          v598
        }
        List(matchExpr(v586)) ++ v588
    }
    v599
  }

  def matchPrimary(node: Node): Primary = {
    val BindNode(v600, v601) = node
    val v702 = v600.id match {
      case 312 =>
        This()(nextId(), v601)
      case 273 =>
        val v602 = v601.asInstanceOf[SequenceNode].children.head
        val BindNode(v603, v604) = v602
        assert(v603.id == 274)
        matchLiteral(v604)
      case 314 =>
        val v605 = v601.asInstanceOf[SequenceNode].children(2)
        val BindNode(v606, v607) = v605
        assert(v606.id == 153)
        Paren(matchExpr(v607))(nextId(), v601)
      case 239 =>
        val v609 = v601.asInstanceOf[SequenceNode].children(1)
        val BindNode(v610, v611) = v609
        assert(v610.id == 241)
        val BindNode(v612, v613) = v611
        val v633 = v612.id match {
          case 131 =>
            None
          case 242 =>
            val BindNode(v614, v615) = v613
            assert(v614.id == 243)
            val BindNode(v616, v617) = v615
            assert(v616.id == 244)
            val v618 = v617.asInstanceOf[SequenceNode].children(1)
            val BindNode(v619, v620) = v618
            assert(v619.id == 245)
            val v621 = v617.asInstanceOf[SequenceNode].children(2)
            val v622 = unrollRepeat0(v621).map { elem =>
              val BindNode(v623, v624) = elem
              assert(v623.id == 252)
              val BindNode(v625, v626) = v624
              val v632 = v625.id match {
                case 253 =>
                  val BindNode(v627, v628) = v626
                  assert(v627.id == 254)
                  val v629 = v628.asInstanceOf[SequenceNode].children(3)
                  val BindNode(v630, v631) = v629
                  assert(v630.id == 245)
                  matchListElem(v631)
              }
              v632
            }
            Some(List(matchListElem(v620)) ++ v622)
        }
        val v608 = v633
        ListExpr(if (v608.isDefined) v608.get else List())(nextId(), v601)
      case 256 =>
        val v634 = v601.asInstanceOf[SequenceNode].children(2)
        val BindNode(v635, v636) = v634
        assert(v635.id == 153)
        val v638 = v601.asInstanceOf[SequenceNode].children(5)
        val BindNode(v639, v640) = v638
        assert(v639.id == 257)
        val BindNode(v641, v642) = v640
        val v663 = v641.id match {
          case 131 =>
            None
          case 258 =>
            val BindNode(v643, v644) = v642
            val v662 = v643.id match {
              case 259 =>
                val BindNode(v645, v646) = v644
                assert(v645.id == 260)
                val v647 = v646.asInstanceOf[SequenceNode].children(1)
                val BindNode(v648, v649) = v647
                assert(v648.id == 153)
                val v650 = v646.asInstanceOf[SequenceNode].children(2)
                val v651 = unrollRepeat0(v650).map { elem =>
                  val BindNode(v652, v653) = elem
                  assert(v652.id == 218)
                  val BindNode(v654, v655) = v653
                  val v661 = v654.id match {
                    case 219 =>
                      val BindNode(v656, v657) = v655
                      assert(v656.id == 220)
                      val v658 = v657.asInstanceOf[SequenceNode].children(3)
                      val BindNode(v659, v660) = v658
                      assert(v659.id == 153)
                      matchExpr(v660)
                  }
                  v661
                }
                List(matchExpr(v649)) ++ v651
            }
            Some(v662)
        }
        val v637 = v663
        TupleExpr(List(matchExpr(v636)) ++ (if (v637.isDefined) v637.get else List()))(nextId(), v601)
      case 143 =>
        val v664 = v601.asInstanceOf[SequenceNode].children.head
        val BindNode(v665, v666) = v664
        assert(v665.id == 144)
        matchCallExpr(v666)
      case 238 =>
        val v667 = v601.asInstanceOf[SequenceNode].children.head
        val BindNode(v668, v669) = v667
        assert(v668.id == 88)
        NameRef(matchSimpleName(v669))(nextId(), v601)
      case 261 =>
        val v671 = v601.asInstanceOf[SequenceNode].children(1)
        val BindNode(v672, v673) = v671
        assert(v672.id == 262)
        val BindNode(v674, v675) = v673
        val v695 = v674.id match {
          case 131 =>
            None
          case 263 =>
            val BindNode(v676, v677) = v675
            assert(v676.id == 264)
            val BindNode(v678, v679) = v677
            assert(v678.id == 265)
            val v680 = v679.asInstanceOf[SequenceNode].children(1)
            val BindNode(v681, v682) = v680
            assert(v681.id == 266)
            val v683 = v679.asInstanceOf[SequenceNode].children(2)
            val v684 = unrollRepeat0(v683).map { elem =>
              val BindNode(v685, v686) = elem
              assert(v685.id == 270)
              val BindNode(v687, v688) = v686
              val v694 = v687.id match {
                case 271 =>
                  val BindNode(v689, v690) = v688
                  assert(v689.id == 272)
                  val v691 = v690.asInstanceOf[SequenceNode].children(3)
                  val BindNode(v692, v693) = v691
                  assert(v692.id == 266)
                  matchNamedExpr(v693)
              }
              v694
            }
            Some(List(matchNamedExpr(v682)) ++ v684)
        }
        val v670 = v695
        NamedTupleExpr(if (v670.isDefined) v670.get else List())(nextId(), v601)
      case 237 =>
        val v696 = v601.asInstanceOf[SequenceNode].children.head
        val BindNode(v697, v698) = v696
        assert(v697.id == 142)
        val v699 = v601.asInstanceOf[SequenceNode].children(4)
        val BindNode(v700, v701) = v699
        assert(v700.id == 88)
        MemberAccess(matchPrimary(v698), matchSimpleName(v701))(nextId(), v601)
    }
    v702
  }

  def matchSimpleName(node: Node): String = {
    val BindNode(v703, v704) = node
    val v735 = v703.id match {
      case 89 =>
        val v705 = v704.asInstanceOf[SequenceNode].children.head
        val BindNode(v706, v707) = v705
        assert(v706.id == 90)
        val BindNode(v708, v709) = v707
        assert(v708.id == 91)
        val BindNode(v710, v711) = v709
        assert(v710.id == 92)
        val BindNode(v712, v713) = v711
        val v734 = v712.id match {
          case 93 =>
            val BindNode(v714, v715) = v713
            assert(v714.id == 94)
            val v716 = v715.asInstanceOf[SequenceNode].children.head
            val BindNode(v717, v718) = v716
            assert(v717.id == 95)
            val JoinNode(_, v719, _) = v718
            val BindNode(v720, v721) = v719
            assert(v720.id == 96)
            val BindNode(v722, v723) = v721
            val v733 = v722.id match {
              case 97 =>
                val BindNode(v724, v725) = v723
                assert(v724.id == 98)
                val v726 = v725.asInstanceOf[SequenceNode].children.head
                val BindNode(v727, v728) = v726
                assert(v727.id == 99)
                val v729 = v725.asInstanceOf[SequenceNode].children(1)
                val v730 = unrollRepeat0(v729).map { elem =>
                  val BindNode(v731, v732) = elem
                  assert(v731.id == 76)
                  v732.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
                }
                v728.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char.toString + v730.map(x => x.toString).mkString("")
            }
            v733
        }
        v734
    }
    v735
  }

  def matchStringElem(node: Node): StringElem = {
    val BindNode(v736, v737) = node
    val v749 = v736.id match {
      case 286 =>
        val v738 = v737.asInstanceOf[SequenceNode].children.head
        val BindNode(v739, v740) = v738
        assert(v739.id == 287)
        val BindNode(v741, v742) = v740
        assert(v741.id == 34)
        JustChar(v742.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char)(nextId(), v737)
      case 289 =>
        val v743 = v737.asInstanceOf[SequenceNode].children.head
        val BindNode(v744, v745) = v743
        assert(v744.id == 290)
        matchEscapeChar(v745)
      case 294 =>
        val v746 = v737.asInstanceOf[SequenceNode].children.head
        val BindNode(v747, v748) = v746
        assert(v747.id == 295)
        matchStringExpr(v748)
    }
    v749
  }

  def matchStringExpr(node: Node): StringExpr = {
    val BindNode(v750, v751) = node
    val v768 = v750.id match {
      case 296 =>
        val v752 = v751.asInstanceOf[SequenceNode].children.head
        val BindNode(v753, v754) = v752
        assert(v753.id == 297)
        val BindNode(v755, v756) = v754
        assert(v755.id == 298)
        val BindNode(v757, v758) = v756
        val v764 = v757.id match {
          case 299 =>
            val BindNode(v759, v760) = v758
            assert(v759.id == 300)
            val v761 = v760.asInstanceOf[SequenceNode].children(1)
            val BindNode(v762, v763) = v761
            assert(v762.id == 88)
            matchSimpleName(v763)
        }
        SimpleExpr(v764)(nextId(), v751)
      case 302 =>
        val v765 = v751.asInstanceOf[SequenceNode].children(3)
        val BindNode(v766, v767) = v765
        assert(v766.id == 153)
        ComplexExpr(matchExpr(v767))(nextId(), v751)
    }
    v768
  }

  def matchStringLiteral(node: Node): StringLiteral = {
    val BindNode(v769, v770) = node
    val v785 = v769.id match {
      case 277 =>
        val v771 = v770.asInstanceOf[SequenceNode].children(1)
        val v772 = unrollRepeat0(v771).map { elem =>
          val BindNode(v773, v774) = elem
          assert(v773.id == 281)
          val BindNode(v775, v776) = v774
          assert(v775.id == 282)
          val BindNode(v777, v778) = v776
          val v784 = v777.id match {
            case 283 =>
              val BindNode(v779, v780) = v778
              assert(v779.id == 284)
              val v781 = v780.asInstanceOf[SequenceNode].children.head
              val BindNode(v782, v783) = v781
              assert(v782.id == 285)
              matchStringElem(v783)
          }
          v784
        }
        StringLiteral(v772)(nextId(), v770)
    }
    v785
  }

  def matchSuperClassDef(node: Node): SuperClassDef = {
    val BindNode(v786, v787) = node
    val v806 = v786.id match {
      case 404 =>
        val v788 = v787.asInstanceOf[SequenceNode].children(4)
        val BindNode(v789, v790) = v788
        assert(v789.id == 88)
        val v791 = v787.asInstanceOf[SequenceNode].children(8)
        val BindNode(v792, v793) = v791
        assert(v792.id == 88)
        val v794 = v787.asInstanceOf[SequenceNode].children(9)
        val v795 = unrollRepeat0(v794).map { elem =>
          val BindNode(v796, v797) = elem
          assert(v796.id == 410)
          val BindNode(v798, v799) = v797
          val v805 = v798.id match {
            case 411 =>
              val BindNode(v800, v801) = v799
              assert(v800.id == 412)
              val v802 = v801.asInstanceOf[SequenceNode].children(3)
              val BindNode(v803, v804) = v802
              assert(v803.id == 88)
              matchSimpleName(v804)
          }
          v805
        }
        SuperClassDef(matchSimpleName(v790), List(matchSimpleName(v793)) ++ v795)(nextId(), v787)
    }
    v806
  }

  def matchTargetDef(node: Node): TargetDef = {
    val BindNode(v807, v808) = node
    val v815 = v807.id match {
      case 229 =>
        val v809 = v808.asInstanceOf[SequenceNode].children.head
        val BindNode(v810, v811) = v809
        assert(v810.id == 88)
        val v812 = v808.asInstanceOf[SequenceNode].children(4)
        val BindNode(v813, v814) = v812
        assert(v813.id == 153)
        TargetDef(matchSimpleName(v811), matchExpr(v814))(nextId(), v808)
    }
    v815
  }

  def matchTupleType(node: Node): TupleType = {
    val BindNode(v816, v817) = node
    val v833 = v816.id match {
      case 199 =>
        val v818 = v817.asInstanceOf[SequenceNode].children(2)
        val BindNode(v819, v820) = v818
        assert(v819.id == 178)
        val v821 = v817.asInstanceOf[SequenceNode].children(3)
        val v822 = unrollRepeat0(v821).map { elem =>
          val BindNode(v823, v824) = elem
          assert(v823.id == 193)
          val BindNode(v825, v826) = v824
          val v832 = v825.id match {
            case 194 =>
              val BindNode(v827, v828) = v826
              assert(v827.id == 195)
              val v829 = v828.asInstanceOf[SequenceNode].children(3)
              val BindNode(v830, v831) = v829
              assert(v830.id == 178)
              matchTypeExpr(v831)
          }
          v832
        }
        TupleType(List(matchTypeExpr(v820)) ++ v822)(nextId(), v817)
    }
    v833
  }

  def matchTypeExpr(node: Node): TypeExpr = {
    val BindNode(v834, v835) = node
    val v842 = v834.id match {
      case 179 =>
        val v836 = v835.asInstanceOf[SequenceNode].children.head
        val BindNode(v837, v838) = v836
        assert(v837.id == 158)
        matchNoUnionType(v838)
      case 180 =>
        val v839 = v835.asInstanceOf[SequenceNode].children.head
        val BindNode(v840, v841) = v839
        assert(v840.id == 181)
        matchUnionType(v841)
    }
    v842
  }

  def matchTypeParams(node: Node): TypeParams = {
    val BindNode(v843, v844) = node
    val v860 = v843.id match {
      case 176 =>
        val v845 = v844.asInstanceOf[SequenceNode].children(2)
        val BindNode(v846, v847) = v845
        assert(v846.id == 178)
        val v848 = v844.asInstanceOf[SequenceNode].children(3)
        val v849 = unrollRepeat0(v848).map { elem =>
          val BindNode(v850, v851) = elem
          assert(v850.id == 193)
          val BindNode(v852, v853) = v851
          val v859 = v852.id match {
            case 194 =>
              val BindNode(v854, v855) = v853
              assert(v854.id == 195)
              val v856 = v855.asInstanceOf[SequenceNode].children(3)
              val BindNode(v857, v858) = v856
              assert(v857.id == 178)
              matchTypeExpr(v858)
          }
          v859
        }
        TypeParams(List(matchTypeExpr(v847)) ++ v849)(nextId(), v844)
    }
    v860
  }

  def matchUnionType(node: Node): UnionType = {
    val BindNode(v861, v862) = node
    val v878 = v861.id match {
      case 182 =>
        val v863 = v862.asInstanceOf[SequenceNode].children(2)
        val BindNode(v864, v865) = v863
        assert(v864.id == 158)
        val v866 = v862.asInstanceOf[SequenceNode].children(3)
        val v867 = unrollRepeat0(v866).map { elem =>
          val BindNode(v868, v869) = elem
          assert(v868.id == 186)
          val BindNode(v870, v871) = v869
          val v877 = v870.id match {
            case 187 =>
              val BindNode(v872, v873) = v871
              assert(v872.id == 188)
              val v874 = v873.asInstanceOf[SequenceNode].children(3)
              val BindNode(v875, v876) = v874
              assert(v875.id == 158)
              matchNoUnionType(v876)
          }
          v877
        }
        UnionType(List(matchNoUnionType(v865)) ++ v867)(nextId(), v862)
    }
    v878
  }

  def matchVarDef(node: Node): VarDef = {
    val BindNode(v879, v880) = node
    val v912 = v879.id match {
      case 421 =>
        val v881 = v880.asInstanceOf[SequenceNode].children(2)
        val BindNode(v882, v883) = v881
        assert(v882.id == 88)
        val v884 = v880.asInstanceOf[SequenceNode].children(3)
        val BindNode(v885, v886) = v884
        assert(v885.id == 426)
        val BindNode(v887, v888) = v886
        val v897 = v887.id match {
          case 131 =>
            None
          case 427 =>
            val BindNode(v889, v890) = v888
            val v896 = v889.id match {
              case 428 =>
                val BindNode(v891, v892) = v890
                assert(v891.id == 429)
                val v893 = v892.asInstanceOf[SequenceNode].children(3)
                val BindNode(v894, v895) = v893
                assert(v894.id == 178)
                matchTypeExpr(v895)
            }
            Some(v896)
        }
        val v898 = v880.asInstanceOf[SequenceNode].children(4)
        val BindNode(v899, v900) = v898
        assert(v899.id == 365)
        val BindNode(v901, v902) = v900
        val v911 = v901.id match {
          case 131 =>
            None
          case 366 =>
            val BindNode(v903, v904) = v902
            val v910 = v903.id match {
              case 367 =>
                val BindNode(v905, v906) = v904
                assert(v905.id == 368)
                val v907 = v906.asInstanceOf[SequenceNode].children(3)
                val BindNode(v908, v909) = v907
                assert(v908.id == 153)
                matchExpr(v909)
            }
            Some(v910)
        }
        VarDef(matchSimpleName(v883), v897, v911)(nextId(), v880)
    }
    v912
  }

  def matchVarRedef(node: Node): VarRedef = {
    val BindNode(v913, v914) = node
    val v933 = v913.id match {
      case 432 =>
        val v915 = v914.asInstanceOf[SequenceNode].children(2)
        val BindNode(v916, v917) = v915
        assert(v916.id == 88)
        val v918 = v914.asInstanceOf[SequenceNode].children(3)
        val v919 = unrollRepeat1(v918).map { elem =>
          val BindNode(v920, v921) = elem
          assert(v920.id == 127)
          val BindNode(v922, v923) = v921
          val v929 = v922.id match {
            case 128 =>
              val BindNode(v924, v925) = v923
              assert(v924.id == 129)
              val v926 = v925.asInstanceOf[SequenceNode].children(3)
              val BindNode(v927, v928) = v926
              assert(v927.id == 88)
              matchSimpleName(v928)
          }
          v929
        }
        val v930 = v914.asInstanceOf[SequenceNode].children(7)
        val BindNode(v931, v932) = v930
        assert(v931.id == 153)
        VarRedef(List(matchSimpleName(v917)) ++ v919, matchExpr(v932))(nextId(), v914)
    }
    v933
  }
}
