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

  case class ArgDef(name: String, typ: Option[TypeExpr], defaultValue: Option[Expr])(override val id: Int, override val parseNode: Node) extends Def with WithIdAndParseNode

  case class ArgRedef(nameTokens: List[String], redefValue: Expr)(override val id: Int, override val parseNode: Node) extends Def with WithIdAndParseNode

  case class BooleanLiteral(value: Boolean)(override val id: Int, override val parseNode: Node) extends Literal with WithIdAndParseNode

  case class BuildRuleDef(name: String, params: List[ParamDef], returnType: TypeExpr, impl: MethodRef)(override val id: Int, override val parseNode: Node) extends Def with WithIdAndParseNode

  case class BuildScript(packageName: Option[Name], defs: List[Def])(override val id: Int, override val parseNode: Node) extends WithIdAndParseNode

  case class CallExpr(name: Name, params: CallParams)(override val id: Int, override val parseNode: Node) extends Primary with WithIdAndParseNode

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
      case 316 =>
        val v3 = v2.asInstanceOf[SequenceNode].children(2)
        val BindNode(v4, v5) = v3
        assert(v4.id == 88)
        val v6 = v2.asInstanceOf[SequenceNode].children(3)
        val BindNode(v7, v8) = v6
        assert(v7.id == 320)
        val BindNode(v9, v10) = v8
        val v19 = v9.id match {
          case 126 =>
            None
          case 321 =>
            val BindNode(v11, v12) = v10
            val v18 = v11.id match {
              case 322 =>
                val BindNode(v13, v14) = v12
                assert(v13.id == 323)
                val v15 = v14.asInstanceOf[SequenceNode].children(1)
                val BindNode(v16, v17) = v15
                assert(v16.id == 324)
                matchActionParams(v17)
            }
            Some(v18)
        }
        val v20 = v2.asInstanceOf[SequenceNode].children(7)
        val BindNode(v21, v22) = v20
        assert(v21.id == 326)
        ActionDef(matchSimpleName(v5), v19, matchActionExpr(v22))(nextId(), v2)
    }
    v23
  }

  def matchActionExpr(node: Node): CallExpr = {
    val BindNode(v24, v25) = node
    val v29 = v24.id match {
      case 206 =>
        val v26 = v25.asInstanceOf[SequenceNode].children.head
        val BindNode(v27, v28) = v26
        assert(v27.id == 207)
        matchCallExpr(v28)
    }
    v29
  }

  def matchActionParams(node: Node): String = {
    val BindNode(v30, v31) = node
    val v35 = v30.id match {
      case 325 =>
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
      case 367 =>
        val v38 = v37.asInstanceOf[SequenceNode].children(4)
        val BindNode(v39, v40) = v38
        assert(v39.id == 88)
        val v41 = v37.asInstanceOf[SequenceNode].children(6)
        val BindNode(v42, v43) = v41
        assert(v42.id == 372)
        val v44 = v37.asInstanceOf[SequenceNode].children(10)
        val BindNode(v45, v46) = v44
        assert(v45.id == 389)
        ActionRuleDef(matchSimpleName(v40), matchParamsDef(v43), matchMethodRef(v46))(nextId(), v37)
    }
    v47
  }

  def matchArgDef(node: Node): ArgDef = {
    val BindNode(v48, v49) = node
    val v81 = v48.id match {
      case 417 =>
        val v50 = v49.asInstanceOf[SequenceNode].children(2)
        val BindNode(v51, v52) = v50
        assert(v51.id == 88)
        val v53 = v49.asInstanceOf[SequenceNode].children(3)
        val BindNode(v54, v55) = v53
        assert(v54.id == 421)
        val BindNode(v56, v57) = v55
        val v66 = v56.id match {
          case 126 =>
            None
          case 422 =>
            val BindNode(v58, v59) = v57
            val v65 = v58.id match {
              case 423 =>
                val BindNode(v60, v61) = v59
                assert(v60.id == 424)
                val v62 = v61.asInstanceOf[SequenceNode].children(3)
                val BindNode(v63, v64) = v62
                assert(v63.id == 167)
                matchTypeExpr(v64)
            }
            Some(v65)
        }
        val v67 = v49.asInstanceOf[SequenceNode].children(4)
        val BindNode(v68, v69) = v67
        assert(v68.id == 380)
        val BindNode(v70, v71) = v69
        val v80 = v70.id match {
          case 126 =>
            None
          case 381 =>
            val BindNode(v72, v73) = v71
            val v79 = v72.id match {
              case 382 =>
                val BindNode(v74, v75) = v73
                assert(v74.id == 383)
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
      case 427 =>
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
      case 289 =>
        val v105 = v104.asInstanceOf[SequenceNode].children.head
        val BindNode(v106, v107) = v105
        assert(v106.id == 290)
        val JoinNode(_, v108, _) = v107
        val BindNode(v109, v110) = v108
        assert(v109.id == 291)
        val BindNode(v111, v112) = v110
        val v117 = v111.id match {
          case 292 =>
            val BindNode(v113, v114) = v112
            assert(v113.id == 103)
            BooleanLiteral(true)(nextId(), v114)
          case 293 =>
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
      case 432 =>
        val v121 = v120.asInstanceOf[SequenceNode].children(2)
        val BindNode(v122, v123) = v121
        assert(v122.id == 88)
        val v124 = v120.asInstanceOf[SequenceNode].children(4)
        val BindNode(v125, v126) = v124
        assert(v125.id == 372)
        val v127 = v120.asInstanceOf[SequenceNode].children(8)
        val BindNode(v128, v129) = v127
        assert(v128.id == 167)
        val v130 = v120.asInstanceOf[SequenceNode].children(12)
        val BindNode(v131, v132) = v130
        assert(v131.id == 389)
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
      case 208 =>
        val v156 = v155.asInstanceOf[SequenceNode].children.head
        val BindNode(v157, v158) = v156
        assert(v157.id == 86)
        val v159 = v155.asInstanceOf[SequenceNode].children(2)
        val BindNode(v160, v161) = v159
        assert(v160.id == 209)
        CallExpr(matchName(v158), matchCallParams(v161))(nextId(), v155)
    }
    v162
  }

  def matchCallParams(node: Node): CallParams = {
    val BindNode(v163, v164) = node
    val v177 = v163.id match {
      case 210 =>
        CallParams(List(), List())(nextId(), v164)
      case 211 =>
        val v165 = v164.asInstanceOf[SequenceNode].children(2)
        val BindNode(v166, v167) = v165
        assert(v166.id == 212)
        CallParams(matchPositionalParams(v167), List())(nextId(), v164)
      case 223 =>
        val v168 = v164.asInstanceOf[SequenceNode].children(2)
        val BindNode(v169, v170) = v168
        assert(v169.id == 224)
        CallParams(List(), matchNamedParams(v170))(nextId(), v164)
      case 234 =>
        val v171 = v164.asInstanceOf[SequenceNode].children(2)
        val BindNode(v172, v173) = v171
        assert(v172.id == 212)
        val v174 = v164.asInstanceOf[SequenceNode].children(6)
        val BindNode(v175, v176) = v174
        assert(v175.id == 224)
        CallParams(matchPositionalParams(v173), matchNamedParams(v176))(nextId(), v164)
    }
    v177
  }

  def matchClassBody(node: Node): List[ClassBodyElem] = {
    val BindNode(v178, v179) = node
    val v192 = v178.id match {
      case 358 =>
        val v180 = v179.asInstanceOf[SequenceNode].children(1)
        val v181 = unrollRepeat0(v180).map { elem =>
          val BindNode(v182, v183) = elem
          assert(v182.id == 361)
          val BindNode(v184, v185) = v183
          val v191 = v184.id match {
            case 362 =>
              val BindNode(v186, v187) = v185
              assert(v186.id == 363)
              val v188 = v187.asInstanceOf[SequenceNode].children(1)
              val BindNode(v189, v190) = v188
              assert(v189.id == 364)
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
      case 365 =>
        val v195 = v194.asInstanceOf[SequenceNode].children.head
        val BindNode(v196, v197) = v195
        assert(v196.id == 366)
        matchActionRuleDef(v197)
      case 395 =>
        val v198 = v194.asInstanceOf[SequenceNode].children.head
        val BindNode(v199, v200) = v198
        assert(v199.id == 396)
        matchClassCastDef(v200)
    }
    v201
  }

  def matchClassCastDef(node: Node): ClassCastDef = {
    val BindNode(v202, v203) = node
    val v210 = v202.id match {
      case 397 =>
        val v204 = v203.asInstanceOf[SequenceNode].children(2)
        val BindNode(v205, v206) = v204
        assert(v205.id == 167)
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
      case 329 =>
        val v213 = v212.asInstanceOf[SequenceNode].children.head
        val BindNode(v214, v215) = v213
        assert(v214.id == 330)
        matchDataClassDef(v215)
      case 398 =>
        val v216 = v212.asInstanceOf[SequenceNode].children.head
        val BindNode(v217, v218) = v216
        assert(v217.id == 399)
        matchSuperClassDef(v218)
    }
    v219
  }

  def matchClassFieldDef(node: Node): ClassField = {
    val BindNode(v220, v221) = node
    val v242 = v220.id match {
      case 342 =>
        val v222 = v221.asInstanceOf[SequenceNode].children.head
        val BindNode(v223, v224) = v222
        assert(v223.id == 88)
        val v225 = v221.asInstanceOf[SequenceNode].children(1)
        val BindNode(v226, v227) = v225
        assert(v226.id == 343)
        val BindNode(v228, v229) = v227
        val v238 = v228.id match {
          case 126 =>
            None
          case 344 =>
            val BindNode(v230, v231) = v229
            val v237 = v230.id match {
              case 345 =>
                val BindNode(v232, v233) = v231
                assert(v232.id == 346)
                val v234 = v233.asInstanceOf[SequenceNode].children(1)
                val BindNode(v235, v236) = v234
                assert(v235.id == 347)
                v236.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
            }
            Some(v237)
        }
        val v239 = v221.asInstanceOf[SequenceNode].children(5)
        val BindNode(v240, v241) = v239
        assert(v240.id == 167)
        ClassField(matchSimpleName(v224), v238.isDefined, matchTypeExpr(v241))(nextId(), v221)
    }
    v242
  }

  def matchClassFields(node: Node): List[ClassField] = {
    val BindNode(v243, v244) = node
    val v260 = v243.id match {
      case 340 =>
        val v245 = v244.asInstanceOf[SequenceNode].children.head
        val BindNode(v246, v247) = v245
        assert(v246.id == 341)
        val v248 = v244.asInstanceOf[SequenceNode].children(1)
        val v249 = unrollRepeat0(v248).map { elem =>
          val BindNode(v250, v251) = elem
          assert(v250.id == 350)
          val BindNode(v252, v253) = v251
          val v259 = v252.id match {
            case 351 =>
              val BindNode(v254, v255) = v253
              assert(v254.id == 352)
              val v256 = v255.asInstanceOf[SequenceNode].children(3)
              val BindNode(v257, v258) = v256
              assert(v257.id == 341)
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
      case 153 =>
        val v263 = v262.asInstanceOf[SequenceNode].children.head
        val BindNode(v264, v265) = v263
        assert(v264.id == 154)
        val JoinNode(_, v266, _) = v265
        val BindNode(v267, v268) = v266
        assert(v267.id == 155)
        val BindNode(v269, v270) = v268
        val v277 = v269.id match {
          case 156 =>
            val BindNode(v271, v272) = v270
            assert(v271.id == 157)
            val v273 = v272.asInstanceOf[SequenceNode].children.head
            "set"
          case 160 =>
            val BindNode(v274, v275) = v270
            assert(v274.id == 161)
            val v276 = v275.asInstanceOf[SequenceNode].children.head
            "list"
        }
        val v278 = v262.asInstanceOf[SequenceNode].children(2)
        val BindNode(v279, v280) = v278
        assert(v279.id == 164)
        CollectionType(v277, matchTypeParams(v280))(nextId(), v262)
    }
    v281
  }

  def matchDataClassDef(node: Node): DataClassDef = {
    val BindNode(v282, v283) = node
    val v317 = v282.id match {
      case 331 =>
        val v284 = v283.asInstanceOf[SequenceNode].children(2)
        val BindNode(v285, v286) = v284
        assert(v285.id == 88)
        val v288 = v283.asInstanceOf[SequenceNode].children(5)
        val BindNode(v289, v290) = v288
        assert(v289.id == 335)
        val BindNode(v291, v292) = v290
        val v301 = v291.id match {
          case 126 =>
            None
          case 336 =>
            val BindNode(v293, v294) = v292
            val v300 = v293.id match {
              case 337 =>
                val BindNode(v295, v296) = v294
                assert(v295.id == 338)
                val v297 = v296.asInstanceOf[SequenceNode].children(1)
                val BindNode(v298, v299) = v297
                assert(v298.id == 339)
                matchClassFields(v299)
            }
            Some(v300)
        }
        val v287 = v301
        val v303 = v283.asInstanceOf[SequenceNode].children(8)
        val BindNode(v304, v305) = v303
        assert(v304.id == 353)
        val BindNode(v306, v307) = v305
        val v316 = v306.id match {
          case 126 =>
            None
          case 354 =>
            val BindNode(v308, v309) = v307
            val v315 = v308.id match {
              case 355 =>
                val BindNode(v310, v311) = v309
                assert(v310.id == 356)
                val v312 = v311.asInstanceOf[SequenceNode].children(1)
                val BindNode(v313, v314) = v312
                assert(v313.id == 357)
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
      case 327 =>
        val v320 = v319.asInstanceOf[SequenceNode].children.head
        val BindNode(v321, v322) = v320
        assert(v321.id == 328)
        matchClassDef(v322)
      case 430 =>
        val v323 = v319.asInstanceOf[SequenceNode].children.head
        val BindNode(v324, v325) = v323
        assert(v324.id == 431)
        matchBuildRuleDef(v325)
      case 415 =>
        val v326 = v319.asInstanceOf[SequenceNode].children.head
        val BindNode(v327, v328) = v326
        assert(v327.id == 416)
        matchArgDef(v328)
      case 425 =>
        val v329 = v319.asInstanceOf[SequenceNode].children.head
        val BindNode(v330, v331) = v329
        assert(v330.id == 426)
        matchArgRedef(v331)
      case 312 =>
        val v332 = v319.asInstanceOf[SequenceNode].children.head
        val BindNode(v333, v334) = v332
        assert(v333.id == 313)
        matchTargetDef(v334)
      case 314 =>
        val v335 = v319.asInstanceOf[SequenceNode].children.head
        val BindNode(v336, v337) = v335
        assert(v336.id == 315)
        matchActionDef(v337)
      case 409 =>
        val v338 = v319.asInstanceOf[SequenceNode].children.head
        val BindNode(v339, v340) = v338
        assert(v339.id == 410)
        matchEnumDef(v340)
      case 130 =>
        val v341 = v319.asInstanceOf[SequenceNode].children.head
        val BindNode(v342, v343) = v341
        assert(v342.id == 131)
        matchImportDef(v343)
      case 306 =>
        val v344 = v319.asInstanceOf[SequenceNode].children.head
        val BindNode(v345, v346) = v344
        assert(v345.id == 307)
        matchNamespaceDef(v346)
      case 365 =>
        val v347 = v319.asInstanceOf[SequenceNode].children.head
        val BindNode(v348, v349) = v347
        assert(v348.id == 366)
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
          assert(v358.id == 435)
          val BindNode(v360, v361) = v359
          val v367 = v360.id match {
            case 436 =>
              val BindNode(v362, v363) = v361
              assert(v362.id == 437)
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
      case 411 =>
        val v371 = v370.asInstanceOf[SequenceNode].children(2)
        val BindNode(v372, v373) = v371
        assert(v372.id == 88)
        val v374 = v370.asInstanceOf[SequenceNode].children(6)
        val BindNode(v375, v376) = v374
        assert(v375.id == 88)
        val v377 = v370.asInstanceOf[SequenceNode].children(7)
        val v378 = unrollRepeat0(v377).map { elem =>
          val BindNode(v379, v380) = elem
          assert(v379.id == 406)
          val BindNode(v381, v382) = v380
          val v388 = v381.id match {
            case 407 =>
              val BindNode(v383, v384) = v382
              assert(v383.id == 408)
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
      case 275 =>
        val v392 = v391.asInstanceOf[SequenceNode].children(1)
        val BindNode(v393, v394) = v392
        assert(v393.id == 277)
        EscapeChar(v394.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char)(nextId(), v391)
    }
    v395
  }

  def matchExpr(node: Node): Expr = {
    val BindNode(v396, v397) = node
    val v407 = v396.id match {
      case 139 =>
        val v398 = v397.asInstanceOf[SequenceNode].children.head
        val BindNode(v399, v400) = v398
        assert(v399.id == 138)
        val v401 = v397.asInstanceOf[SequenceNode].children(4)
        val BindNode(v402, v403) = v401
        assert(v402.id == 143)
        CastExpr(matchExpr(v400), matchNoUnionType(v403))(nextId(), v397)
      case 201 =>
        val v404 = v397.asInstanceOf[SequenceNode].children.head
        val BindNode(v405, v406) = v404
        assert(v405.id == 202)
        matchMergeOpOrPrimary(v406)
    }
    v407
  }

  def matchImportDef(node: Node): ImportDef = {
    val BindNode(v408, v409) = node
    val v447 = v408.id match {
      case 132 =>
        val v410 = v409.asInstanceOf[SequenceNode].children(2)
        val BindNode(v411, v412) = v410
        assert(v411.id == 138)
        val v413 = v409.asInstanceOf[SequenceNode].children(3)
        val BindNode(v414, v415) = v413
        assert(v414.id == 298)
        val BindNode(v416, v417) = v415
        val v426 = v416.id match {
          case 126 =>
            None
          case 299 =>
            val BindNode(v418, v419) = v417
            val v425 = v418.id match {
              case 300 =>
                val BindNode(v420, v421) = v419
                assert(v420.id == 301)
                val v422 = v421.asInstanceOf[SequenceNode].children(3)
                val BindNode(v423, v424) = v422
                assert(v423.id == 88)
                matchSimpleName(v424)
            }
            Some(v425)
        }
        ImportAll(matchExpr(v412), v426)(nextId(), v409)
      case 302 =>
        val v427 = v409.asInstanceOf[SequenceNode].children(2)
        val BindNode(v428, v429) = v427
        assert(v428.id == 138)
        val v430 = v409.asInstanceOf[SequenceNode].children(6)
        val BindNode(v431, v432) = v430
        assert(v431.id == 86)
        val v433 = v409.asInstanceOf[SequenceNode].children(7)
        val BindNode(v434, v435) = v433
        assert(v434.id == 298)
        val BindNode(v436, v437) = v435
        val v446 = v436.id match {
          case 126 =>
            None
          case 299 =>
            val BindNode(v438, v439) = v437
            val v445 = v438.id match {
              case 300 =>
                val BindNode(v440, v441) = v439
                assert(v440.id == 301)
                val v442 = v441.asInstanceOf[SequenceNode].children(3)
                val BindNode(v443, v444) = v442
                assert(v443.id == 88)
                matchSimpleName(v444)
            }
            Some(v445)
        }
        ImportFrom(matchExpr(v429), matchName(v432), v446)(nextId(), v409)
    }
    v447
  }

  def matchLiteral(node: Node): Literal = {
    val BindNode(v448, v449) = node
    val v459 = v448.id match {
      case 259 =>
        val v450 = v449.asInstanceOf[SequenceNode].children.head
        val BindNode(v451, v452) = v450
        assert(v451.id == 260)
        matchStringLiteral(v452)
      case 287 =>
        val v453 = v449.asInstanceOf[SequenceNode].children.head
        val BindNode(v454, v455) = v453
        assert(v454.id == 288)
        matchBooleanLiteral(v455)
      case 294 =>
        val v456 = v449.asInstanceOf[SequenceNode].children.head
        val BindNode(v457, v458) = v456
        assert(v457.id == 295)
        matchNoneLiteral(v458)
    }
    v459
  }

  def matchMergeOpOrPrimary(node: Node): MergeOpOrPrimary = {
    val BindNode(v460, v461) = node
    val v471 = v460.id match {
      case 203 =>
        val v462 = v461.asInstanceOf[SequenceNode].children.head
        val BindNode(v463, v464) = v462
        assert(v463.id == 138)
        val v465 = v461.asInstanceOf[SequenceNode].children(4)
        val BindNode(v466, v467) = v465
        assert(v466.id == 205)
        MergeOp(matchExpr(v464), matchPrimary(v467))(nextId(), v461)
      case 297 =>
        val v468 = v461.asInstanceOf[SequenceNode].children.head
        val BindNode(v469, v470) = v468
        assert(v469.id == 205)
        matchPrimary(v470)
    }
    v471
  }

  def matchMethodRef(node: Node): MethodRef = {
    val BindNode(v472, v473) = node
    val v494 = v472.id match {
      case 390 =>
        val v474 = v473.asInstanceOf[SequenceNode].children.head
        val BindNode(v475, v476) = v474
        assert(v475.id == 86)
        val v477 = v473.asInstanceOf[SequenceNode].children(4)
        val BindNode(v478, v479) = v477
        assert(v478.id == 86)
        val v480 = v473.asInstanceOf[SequenceNode].children(5)
        val BindNode(v481, v482) = v480
        assert(v481.id == 391)
        val BindNode(v483, v484) = v482
        val v493 = v483.id match {
          case 126 =>
            None
          case 392 =>
            val BindNode(v485, v486) = v484
            val v492 = v485.id match {
              case 393 =>
                val BindNode(v487, v488) = v486
                assert(v487.id == 394)
                val v489 = v488.asInstanceOf[SequenceNode].children(3)
                val BindNode(v490, v491) = v489
                assert(v490.id == 88)
                matchSimpleName(v491)
            }
            Some(v492)
        }
        MethodRef(matchName(v476), matchName(v479), v493)(nextId(), v473)
    }
    v494
  }

  def matchName(node: Node): Name = {
    val BindNode(v495, v496) = node
    val v512 = v495.id match {
      case 87 =>
        val v497 = v496.asInstanceOf[SequenceNode].children.head
        val BindNode(v498, v499) = v497
        assert(v498.id == 88)
        val v500 = v496.asInstanceOf[SequenceNode].children(1)
        val v501 = unrollRepeat0(v500).map { elem =>
          val BindNode(v502, v503) = elem
          assert(v502.id == 122)
          val BindNode(v504, v505) = v503
          val v511 = v504.id match {
            case 123 =>
              val BindNode(v506, v507) = v505
              assert(v506.id == 124)
              val v508 = v507.asInstanceOf[SequenceNode].children(3)
              val BindNode(v509, v510) = v508
              assert(v509.id == 88)
              matchSimpleName(v510)
          }
          v511
        }
        Name(List(matchSimpleName(v499)) ++ v501)(nextId(), v496)
    }
    v512
  }

  def matchNamedExpr(node: Node): NamedExpr = {
    val BindNode(v513, v514) = node
    val v521 = v513.id match {
      case 251 =>
        val v515 = v514.asInstanceOf[SequenceNode].children.head
        val BindNode(v516, v517) = v515
        assert(v516.id == 88)
        val v518 = v514.asInstanceOf[SequenceNode].children(4)
        val BindNode(v519, v520) = v518
        assert(v519.id == 138)
        NamedExpr(matchSimpleName(v517), matchExpr(v520))(nextId(), v514)
    }
    v521
  }

  def matchNamedParam(node: Node): NamedParam = {
    val BindNode(v522, v523) = node
    val v530 = v522.id match {
      case 227 =>
        val v524 = v523.asInstanceOf[SequenceNode].children.head
        val BindNode(v525, v526) = v524
        assert(v525.id == 88)
        val v527 = v523.asInstanceOf[SequenceNode].children(4)
        val BindNode(v528, v529) = v527
        assert(v528.id == 138)
        NamedParam(matchSimpleName(v526), matchExpr(v529))(nextId(), v523)
    }
    v530
  }

  def matchNamedParams(node: Node): List[NamedParam] = {
    val BindNode(v531, v532) = node
    val v548 = v531.id match {
      case 225 =>
        val v533 = v532.asInstanceOf[SequenceNode].children.head
        val BindNode(v534, v535) = v533
        assert(v534.id == 226)
        val v536 = v532.asInstanceOf[SequenceNode].children(1)
        val v537 = unrollRepeat0(v536).map { elem =>
          val BindNode(v538, v539) = elem
          assert(v538.id == 231)
          val BindNode(v540, v541) = v539
          val v547 = v540.id match {
            case 232 =>
              val BindNode(v542, v543) = v541
              assert(v542.id == 233)
              val v544 = v543.asInstanceOf[SequenceNode].children(3)
              val BindNode(v545, v546) = v544
              assert(v545.id == 226)
              matchNamedParam(v546)
          }
          v547
        }
        List(matchNamedParam(v535)) ++ v537
    }
    v548
  }

  def matchNamedTupleType(node: Node): NamedTupleType = {
    val BindNode(v549, v550) = node
    val v566 = v549.id match {
      case 193 =>
        val v551 = v550.asInstanceOf[SequenceNode].children(2)
        val BindNode(v552, v553) = v551
        assert(v552.id == 194)
        val v554 = v550.asInstanceOf[SequenceNode].children(3)
        val v555 = unrollRepeat0(v554).map { elem =>
          val BindNode(v556, v557) = elem
          assert(v556.id == 198)
          val BindNode(v558, v559) = v557
          val v565 = v558.id match {
            case 199 =>
              val BindNode(v560, v561) = v559
              assert(v560.id == 200)
              val v562 = v561.asInstanceOf[SequenceNode].children(3)
              val BindNode(v563, v564) = v562
              assert(v563.id == 194)
              matchNamedType(v564)
          }
          v565
        }
        NamedTupleType(List(matchNamedType(v553)) ++ v555)(nextId(), v550)
    }
    v566
  }

  def matchNamedType(node: Node): NamedType = {
    val BindNode(v567, v568) = node
    val v575 = v567.id match {
      case 195 =>
        val v569 = v568.asInstanceOf[SequenceNode].children.head
        val BindNode(v570, v571) = v569
        assert(v570.id == 88)
        val v572 = v568.asInstanceOf[SequenceNode].children(4)
        val BindNode(v573, v574) = v572
        assert(v573.id == 167)
        NamedType(matchSimpleName(v571), matchTypeExpr(v574))(nextId(), v568)
    }
    v575
  }

  def matchNamespaceDef(node: Node): NamespaceDef = {
    val BindNode(v576, v577) = node
    val v584 = v576.id match {
      case 308 =>
        val v578 = v577.asInstanceOf[SequenceNode].children(2)
        val BindNode(v579, v580) = v578
        assert(v579.id == 88)
        val v581 = v577.asInstanceOf[SequenceNode].children(5)
        val BindNode(v582, v583) = v581
        assert(v582.id == 2)
        NamespaceDef(matchSimpleName(v580), matchBuildScript(v583))(nextId(), v577)
    }
    v584
  }

  def matchNoUnionType(node: Node): NoUnionType = {
    val BindNode(v585, v586) = node
    val v605 = v585.id match {
      case 191 =>
        val v587 = v586.asInstanceOf[SequenceNode].children.head
        val BindNode(v588, v589) = v587
        assert(v588.id == 192)
        matchNamedTupleType(v589)
      case 149 =>
        NoneType()(nextId(), v586)
      case 144 =>
        val v590 = v586.asInstanceOf[SequenceNode].children.head
        val BindNode(v591, v592) = v590
        assert(v591.id == 86)
        matchName(v592)
      case 186 =>
        val v593 = v586.asInstanceOf[SequenceNode].children.head
        val BindNode(v594, v595) = v593
        assert(v594.id == 187)
        matchTupleType(v595)
      case 145 =>
        val v596 = v586.asInstanceOf[SequenceNode].children.head
        val BindNode(v597, v598) = v596
        assert(v597.id == 86)
        val v599 = v586.asInstanceOf[SequenceNode].children(2)
        val BindNode(v600, v601) = v599
        assert(v600.id == 86)
        CanonicalName(matchName(v598), matchName(v601))(nextId(), v586)
      case 151 =>
        val v602 = v586.asInstanceOf[SequenceNode].children.head
        val BindNode(v603, v604) = v602
        assert(v603.id == 152)
        matchCollectionType(v604)
    }
    v605
  }

  def matchNoneLiteral(node: Node): NoneLiteral = {
    val BindNode(v606, v607) = node
    val v608 = v606.id match {
      case 149 =>
        NoneLiteral()(nextId(), v607)
    }
    v608
  }

  def matchPackageName(node: Node): Name = {
    val BindNode(v609, v610) = node
    val v614 = v609.id match {
      case 59 =>
        val v611 = v610.asInstanceOf[SequenceNode].children(2)
        val BindNode(v612, v613) = v611
        assert(v612.id == 86)
        matchName(v613)
    }
    v614
  }

  def matchParamDef(node: Node): ParamDef = {
    val BindNode(v615, v616) = node
    val v657 = v615.id match {
      case 379 =>
        val v617 = v616.asInstanceOf[SequenceNode].children.head
        val BindNode(v618, v619) = v617
        assert(v618.id == 88)
        val v620 = v616.asInstanceOf[SequenceNode].children(1)
        val BindNode(v621, v622) = v620
        assert(v621.id == 343)
        val BindNode(v623, v624) = v622
        val v633 = v623.id match {
          case 126 =>
            None
          case 344 =>
            val BindNode(v625, v626) = v624
            val v632 = v625.id match {
              case 345 =>
                val BindNode(v627, v628) = v626
                assert(v627.id == 346)
                val v629 = v628.asInstanceOf[SequenceNode].children(1)
                val BindNode(v630, v631) = v629
                assert(v630.id == 347)
                v631.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
            }
            Some(v632)
        }
        val v634 = v616.asInstanceOf[SequenceNode].children(5)
        val BindNode(v635, v636) = v634
        assert(v635.id == 167)
        val v637 = v616.asInstanceOf[SequenceNode].children(6)
        val BindNode(v638, v639) = v637
        assert(v638.id == 380)
        val BindNode(v640, v641) = v639
        val v650 = v640.id match {
          case 126 =>
            None
          case 381 =>
            val BindNode(v642, v643) = v641
            val v649 = v642.id match {
              case 382 =>
                val BindNode(v644, v645) = v643
                assert(v644.id == 383)
                val v646 = v645.asInstanceOf[SequenceNode].children(3)
                val BindNode(v647, v648) = v646
                assert(v647.id == 138)
                matchExpr(v648)
            }
            Some(v649)
        }
        ParamDef(matchSimpleName(v619), v633.isDefined, Some(matchTypeExpr(v636)), v650)(nextId(), v616)
      case 227 =>
        val v651 = v616.asInstanceOf[SequenceNode].children.head
        val BindNode(v652, v653) = v651
        assert(v652.id == 88)
        val v654 = v616.asInstanceOf[SequenceNode].children(4)
        val BindNode(v655, v656) = v654
        assert(v655.id == 138)
        ParamDef(matchSimpleName(v653), false, None, Some(matchExpr(v656)))(nextId(), v616)
    }
    v657
  }

  def matchParamsDef(node: Node): List[ParamDef] = {
    val BindNode(v658, v659) = node
    val v686 = v658.id match {
      case 373 =>
        val v661 = v659.asInstanceOf[SequenceNode].children(1)
        val BindNode(v662, v663) = v661
        assert(v662.id == 374)
        val BindNode(v664, v665) = v663
        val v685 = v664.id match {
          case 126 =>
            None
          case 375 =>
            val BindNode(v666, v667) = v665
            assert(v666.id == 376)
            val BindNode(v668, v669) = v667
            assert(v668.id == 377)
            val v670 = v669.asInstanceOf[SequenceNode].children(1)
            val BindNode(v671, v672) = v670
            assert(v671.id == 378)
            val v673 = v669.asInstanceOf[SequenceNode].children(2)
            val v674 = unrollRepeat0(v673).map { elem =>
              val BindNode(v675, v676) = elem
              assert(v675.id == 386)
              val BindNode(v677, v678) = v676
              val v684 = v677.id match {
                case 387 =>
                  val BindNode(v679, v680) = v678
                  assert(v679.id == 388)
                  val v681 = v680.asInstanceOf[SequenceNode].children(3)
                  val BindNode(v682, v683) = v681
                  assert(v682.id == 378)
                  matchParamDef(v683)
              }
              v684
            }
            Some(List(matchParamDef(v672)) ++ v674)
        }
        val v660 = v685
        if (v660.isDefined) v660.get else List()
    }
    v686
  }

  def matchPositionalParams(node: Node): List[Expr] = {
    val BindNode(v687, v688) = node
    val v704 = v687.id match {
      case 213 =>
        val v689 = v688.asInstanceOf[SequenceNode].children.head
        val BindNode(v690, v691) = v689
        assert(v690.id == 138)
        val v692 = v688.asInstanceOf[SequenceNode].children(1)
        val v693 = unrollRepeat0(v692).map { elem =>
          val BindNode(v694, v695) = elem
          assert(v694.id == 216)
          val BindNode(v696, v697) = v695
          val v703 = v696.id match {
            case 217 =>
              val BindNode(v698, v699) = v697
              assert(v698.id == 218)
              val v700 = v699.asInstanceOf[SequenceNode].children(3)
              val BindNode(v701, v702) = v700
              assert(v701.id == 138)
              matchExpr(v702)
          }
          v703
        }
        List(matchExpr(v691)) ++ v693
    }
    v704
  }

  def matchPrimary(node: Node): Primary = {
    val BindNode(v705, v706) = node
    val v807 = v705.id match {
      case 244 =>
        val v707 = v706.asInstanceOf[SequenceNode].children(2)
        val BindNode(v708, v709) = v707
        assert(v708.id == 138)
        val v711 = v706.asInstanceOf[SequenceNode].children(5)
        val BindNode(v712, v713) = v711
        assert(v712.id == 239)
        val BindNode(v714, v715) = v713
        val v736 = v714.id match {
          case 126 =>
            None
          case 240 =>
            val BindNode(v716, v717) = v715
            val v735 = v716.id match {
              case 241 =>
                val BindNode(v718, v719) = v717
                assert(v718.id == 242)
                val v720 = v719.asInstanceOf[SequenceNode].children(1)
                val BindNode(v721, v722) = v720
                assert(v721.id == 138)
                val v723 = v719.asInstanceOf[SequenceNode].children(2)
                val v724 = unrollRepeat0(v723).map { elem =>
                  val BindNode(v725, v726) = elem
                  assert(v725.id == 216)
                  val BindNode(v727, v728) = v726
                  val v734 = v727.id match {
                    case 217 =>
                      val BindNode(v729, v730) = v728
                      assert(v729.id == 218)
                      val v731 = v730.asInstanceOf[SequenceNode].children(3)
                      val BindNode(v732, v733) = v731
                      assert(v732.id == 138)
                      matchExpr(v733)
                  }
                  v734
                }
                List(matchExpr(v722)) ++ v724
            }
            Some(v735)
        }
        val v710 = v736
        TupleExpr(List(matchExpr(v709)) ++ (if (v710.isDefined) v710.get else List()))(nextId(), v706)
      case 235 =>
        val v737 = v706.asInstanceOf[SequenceNode].children.head
        val BindNode(v738, v739) = v737
        assert(v738.id == 205)
        val v740 = v706.asInstanceOf[SequenceNode].children(4)
        val BindNode(v741, v742) = v740
        assert(v741.id == 88)
        MemberAccess(matchPrimary(v739), matchSimpleName(v742))(nextId(), v706)
      case 296 =>
        val v743 = v706.asInstanceOf[SequenceNode].children(2)
        val BindNode(v744, v745) = v743
        assert(v744.id == 138)
        Paren(matchExpr(v745))(nextId(), v706)
      case 206 =>
        val v746 = v706.asInstanceOf[SequenceNode].children.head
        val BindNode(v747, v748) = v746
        assert(v747.id == 207)
        matchCallExpr(v748)
      case 236 =>
        val v749 = v706.asInstanceOf[SequenceNode].children.head
        val BindNode(v750, v751) = v749
        assert(v750.id == 88)
        NameRef(matchSimpleName(v751))(nextId(), v706)
      case 245 =>
        val v753 = v706.asInstanceOf[SequenceNode].children(1)
        val BindNode(v754, v755) = v753
        assert(v754.id == 246)
        val BindNode(v756, v757) = v755
        val v777 = v756.id match {
          case 126 =>
            None
          case 247 =>
            val BindNode(v758, v759) = v757
            assert(v758.id == 248)
            val BindNode(v760, v761) = v759
            assert(v760.id == 249)
            val v762 = v761.asInstanceOf[SequenceNode].children(1)
            val BindNode(v763, v764) = v762
            assert(v763.id == 250)
            val v765 = v761.asInstanceOf[SequenceNode].children(2)
            val v766 = unrollRepeat0(v765).map { elem =>
              val BindNode(v767, v768) = elem
              assert(v767.id == 254)
              val BindNode(v769, v770) = v768
              val v776 = v769.id match {
                case 255 =>
                  val BindNode(v771, v772) = v770
                  assert(v771.id == 256)
                  val v773 = v772.asInstanceOf[SequenceNode].children(3)
                  val BindNode(v774, v775) = v773
                  assert(v774.id == 250)
                  matchNamedExpr(v775)
              }
              v776
            }
            Some(List(matchNamedExpr(v764)) ++ v766)
        }
        val v752 = v777
        NamedTupleExpr(if (v752.isDefined) v752.get else List())(nextId(), v706)
      case 257 =>
        val v778 = v706.asInstanceOf[SequenceNode].children.head
        val BindNode(v779, v780) = v778
        assert(v779.id == 258)
        matchLiteral(v780)
      case 237 =>
        val v782 = v706.asInstanceOf[SequenceNode].children(1)
        val BindNode(v783, v784) = v782
        assert(v783.id == 239)
        val BindNode(v785, v786) = v784
        val v806 = v785.id match {
          case 126 =>
            None
          case 240 =>
            val BindNode(v787, v788) = v786
            assert(v787.id == 241)
            val BindNode(v789, v790) = v788
            assert(v789.id == 242)
            val v791 = v790.asInstanceOf[SequenceNode].children(1)
            val BindNode(v792, v793) = v791
            assert(v792.id == 138)
            val v794 = v790.asInstanceOf[SequenceNode].children(2)
            val v795 = unrollRepeat0(v794).map { elem =>
              val BindNode(v796, v797) = elem
              assert(v796.id == 216)
              val BindNode(v798, v799) = v797
              val v805 = v798.id match {
                case 217 =>
                  val BindNode(v800, v801) = v799
                  assert(v800.id == 218)
                  val v802 = v801.asInstanceOf[SequenceNode].children(3)
                  val BindNode(v803, v804) = v802
                  assert(v803.id == 138)
                  matchExpr(v804)
              }
              v805
            }
            Some(List(matchExpr(v793)) ++ v795)
        }
        val v781 = v806
        ListExpr(if (v781.isDefined) v781.get else List())(nextId(), v706)
    }
    v807
  }

  def matchSimpleName(node: Node): String = {
    val BindNode(v808, v809) = node
    val v840 = v808.id match {
      case 89 =>
        val v810 = v809.asInstanceOf[SequenceNode].children.head
        val BindNode(v811, v812) = v810
        assert(v811.id == 90)
        val BindNode(v813, v814) = v812
        assert(v813.id == 91)
        val BindNode(v815, v816) = v814
        assert(v815.id == 92)
        val BindNode(v817, v818) = v816
        val v839 = v817.id match {
          case 93 =>
            val BindNode(v819, v820) = v818
            assert(v819.id == 94)
            val v821 = v820.asInstanceOf[SequenceNode].children.head
            val BindNode(v822, v823) = v821
            assert(v822.id == 95)
            val JoinNode(_, v824, _) = v823
            val BindNode(v825, v826) = v824
            assert(v825.id == 96)
            val BindNode(v827, v828) = v826
            val v838 = v827.id match {
              case 97 =>
                val BindNode(v829, v830) = v828
                assert(v829.id == 98)
                val v831 = v830.asInstanceOf[SequenceNode].children.head
                val BindNode(v832, v833) = v831
                assert(v832.id == 99)
                val v834 = v830.asInstanceOf[SequenceNode].children(1)
                val v835 = unrollRepeat0(v834).map { elem =>
                  val BindNode(v836, v837) = elem
                  assert(v836.id == 76)
                  v837.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
                }
                v833.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char.toString + v835.map(x => x.toString).mkString("")
            }
            v838
        }
        v839
    }
    v840
  }

  def matchStringElem(node: Node): StringElem = {
    val BindNode(v841, v842) = node
    val v854 = v841.id match {
      case 270 =>
        val v843 = v842.asInstanceOf[SequenceNode].children.head
        val BindNode(v844, v845) = v843
        assert(v844.id == 271)
        val BindNode(v846, v847) = v845
        assert(v846.id == 34)
        JustChar(v847.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char)(nextId(), v842)
      case 273 =>
        val v848 = v842.asInstanceOf[SequenceNode].children.head
        val BindNode(v849, v850) = v848
        assert(v849.id == 274)
        matchEscapeChar(v850)
      case 278 =>
        val v851 = v842.asInstanceOf[SequenceNode].children.head
        val BindNode(v852, v853) = v851
        assert(v852.id == 279)
        matchStringExpr(v853)
    }
    v854
  }

  def matchStringExpr(node: Node): StringExpr = {
    val BindNode(v855, v856) = node
    val v873 = v855.id match {
      case 280 =>
        val v857 = v856.asInstanceOf[SequenceNode].children.head
        val BindNode(v858, v859) = v857
        assert(v858.id == 281)
        val BindNode(v860, v861) = v859
        assert(v860.id == 282)
        val BindNode(v862, v863) = v861
        val v869 = v862.id match {
          case 283 =>
            val BindNode(v864, v865) = v863
            assert(v864.id == 284)
            val v866 = v865.asInstanceOf[SequenceNode].children(1)
            val BindNode(v867, v868) = v866
            assert(v867.id == 88)
            matchSimpleName(v868)
        }
        SimpleExpr(v869)(nextId(), v856)
      case 286 =>
        val v870 = v856.asInstanceOf[SequenceNode].children(3)
        val BindNode(v871, v872) = v870
        assert(v871.id == 138)
        ComplexExpr(matchExpr(v872))(nextId(), v856)
    }
    v873
  }

  def matchStringLiteral(node: Node): StringLiteral = {
    val BindNode(v874, v875) = node
    val v890 = v874.id match {
      case 261 =>
        val v876 = v875.asInstanceOf[SequenceNode].children(1)
        val v877 = unrollRepeat0(v876).map { elem =>
          val BindNode(v878, v879) = elem
          assert(v878.id == 265)
          val BindNode(v880, v881) = v879
          assert(v880.id == 266)
          val BindNode(v882, v883) = v881
          val v889 = v882.id match {
            case 267 =>
              val BindNode(v884, v885) = v883
              assert(v884.id == 268)
              val v886 = v885.asInstanceOf[SequenceNode].children.head
              val BindNode(v887, v888) = v886
              assert(v887.id == 269)
              matchStringElem(v888)
          }
          v889
        }
        StringLiteral(v877)(nextId(), v875)
    }
    v890
  }

  def matchSuperClassDef(node: Node): SuperClassDef = {
    val BindNode(v891, v892) = node
    val v911 = v891.id match {
      case 400 =>
        val v893 = v892.asInstanceOf[SequenceNode].children(4)
        val BindNode(v894, v895) = v893
        assert(v894.id == 88)
        val v896 = v892.asInstanceOf[SequenceNode].children(8)
        val BindNode(v897, v898) = v896
        assert(v897.id == 88)
        val v899 = v892.asInstanceOf[SequenceNode].children(9)
        val v900 = unrollRepeat0(v899).map { elem =>
          val BindNode(v901, v902) = elem
          assert(v901.id == 406)
          val BindNode(v903, v904) = v902
          val v910 = v903.id match {
            case 407 =>
              val BindNode(v905, v906) = v904
              assert(v905.id == 408)
              val v907 = v906.asInstanceOf[SequenceNode].children(3)
              val BindNode(v908, v909) = v907
              assert(v908.id == 88)
              matchSimpleName(v909)
          }
          v910
        }
        SuperClassDef(matchSimpleName(v895), List(matchSimpleName(v898)) ++ v900)(nextId(), v892)
    }
    v911
  }

  def matchTargetDef(node: Node): TargetDef = {
    val BindNode(v912, v913) = node
    val v920 = v912.id match {
      case 227 =>
        val v914 = v913.asInstanceOf[SequenceNode].children.head
        val BindNode(v915, v916) = v914
        assert(v915.id == 88)
        val v917 = v913.asInstanceOf[SequenceNode].children(4)
        val BindNode(v918, v919) = v917
        assert(v918.id == 138)
        TargetDef(matchSimpleName(v916), matchExpr(v919))(nextId(), v913)
    }
    v920
  }

  def matchTupleType(node: Node): TupleType = {
    val BindNode(v921, v922) = node
    val v938 = v921.id match {
      case 188 =>
        val v923 = v922.asInstanceOf[SequenceNode].children(2)
        val BindNode(v924, v925) = v923
        assert(v924.id == 167)
        val v926 = v922.asInstanceOf[SequenceNode].children(3)
        val v927 = unrollRepeat0(v926).map { elem =>
          val BindNode(v928, v929) = elem
          assert(v928.id == 182)
          val BindNode(v930, v931) = v929
          val v937 = v930.id match {
            case 183 =>
              val BindNode(v932, v933) = v931
              assert(v932.id == 184)
              val v934 = v933.asInstanceOf[SequenceNode].children(3)
              val BindNode(v935, v936) = v934
              assert(v935.id == 167)
              matchTypeExpr(v936)
          }
          v937
        }
        TupleType(List(matchTypeExpr(v925)) ++ v927)(nextId(), v922)
    }
    v938
  }

  def matchTypeExpr(node: Node): TypeExpr = {
    val BindNode(v939, v940) = node
    val v947 = v939.id match {
      case 168 =>
        val v941 = v940.asInstanceOf[SequenceNode].children.head
        val BindNode(v942, v943) = v941
        assert(v942.id == 143)
        matchNoUnionType(v943)
      case 169 =>
        val v944 = v940.asInstanceOf[SequenceNode].children.head
        val BindNode(v945, v946) = v944
        assert(v945.id == 170)
        matchUnionType(v946)
    }
    v947
  }

  def matchTypeParams(node: Node): TypeParams = {
    val BindNode(v948, v949) = node
    val v965 = v948.id match {
      case 165 =>
        val v950 = v949.asInstanceOf[SequenceNode].children(2)
        val BindNode(v951, v952) = v950
        assert(v951.id == 167)
        val v953 = v949.asInstanceOf[SequenceNode].children(3)
        val v954 = unrollRepeat0(v953).map { elem =>
          val BindNode(v955, v956) = elem
          assert(v955.id == 182)
          val BindNode(v957, v958) = v956
          val v964 = v957.id match {
            case 183 =>
              val BindNode(v959, v960) = v958
              assert(v959.id == 184)
              val v961 = v960.asInstanceOf[SequenceNode].children(3)
              val BindNode(v962, v963) = v961
              assert(v962.id == 167)
              matchTypeExpr(v963)
          }
          v964
        }
        TypeParams(List(matchTypeExpr(v952)) ++ v954)(nextId(), v949)
    }
    v965
  }

  def matchUnionType(node: Node): UnionType = {
    val BindNode(v966, v967) = node
    val v983 = v966.id match {
      case 171 =>
        val v968 = v967.asInstanceOf[SequenceNode].children(2)
        val BindNode(v969, v970) = v968
        assert(v969.id == 143)
        val v971 = v967.asInstanceOf[SequenceNode].children(3)
        val v972 = unrollRepeat0(v971).map { elem =>
          val BindNode(v973, v974) = elem
          assert(v973.id == 175)
          val BindNode(v975, v976) = v974
          val v982 = v975.id match {
            case 176 =>
              val BindNode(v977, v978) = v976
              assert(v977.id == 177)
              val v979 = v978.asInstanceOf[SequenceNode].children(3)
              val BindNode(v980, v981) = v979
              assert(v980.id == 143)
              matchNoUnionType(v981)
          }
          v982
        }
        UnionType(List(matchNoUnionType(v970)) ++ v972)(nextId(), v967)
    }
    v983
  }
}
