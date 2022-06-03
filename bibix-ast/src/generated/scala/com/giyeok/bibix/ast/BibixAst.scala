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
import com.giyeok.jparser.milestone.{MilestoneParser, MilestoneParserContext, MilestoneParserData}
import com.giyeok.jparser.nparser.ParseTreeUtil
import com.giyeok.jparser.nparser.ParseTreeUtil.unrollRepeat0
import com.giyeok.jparser.proto.GrammarProto
import com.giyeok.jparser.proto.GrammarProtobufConverter
import com.giyeok.jparser.proto.MilestoneParserDataProto
import com.giyeok.jparser.proto.MilestoneParserProtobufConverter
import com.giyeok.jparser.utils.FileUtil.readFileBytes

import java.util.Base64

object BibixAst {

  sealed trait WithParseNode {
    val parseNode: Node
  }

  case class ActionDef(name: String, argsName: Option[String], expr: CallExpr)(override val parseNode: Node) extends Def with WithParseNode

  case class ActionRuleDef(name: String, params: List[ParamDef], impl: MethodRef)(override val parseNode: Node) extends ClassBodyElem with Def with WithParseNode

  case class ArgDef(replacing: Option[Name], name: String, typ: Option[TypeExpr], defaultValue: Option[Expr])(override val parseNode: Node) extends Def with WithParseNode

  case class BooleanLiteral(value: Boolean)(override val parseNode: Node) extends Literal with WithParseNode

  case class BuildRuleDef(name: String, params: List[ParamDef], returnType: TypeExpr, impl: MethodRef)(override val parseNode: Node) extends Def with WithParseNode

  case class BuildScript(defs: List[Def])(override val parseNode: Node) extends WithParseNode

  case class CallExpr(name: Name, params: CallParams)(override val parseNode: Node) extends ImportSourceExpr with Primary with WithParseNode

  case class CallParams(posParams: List[Expr], namedParams: List[NamedParam])(override val parseNode: Node) extends WithParseNode

  case class CastExpr(expr: Expr, castTo: NoUnionType)(override val parseNode: Node) extends Expr with WithParseNode

  sealed trait ClassBodyElem extends WithParseNode

  case class ClassCastDef(castTo: TypeExpr, expr: Expr)(override val parseNode: Node) extends ClassBodyElem with WithParseNode

  case class ClassDef(name: String, extendings: List[ClassExtending], reality: TypeExpr, body: List[ClassBodyElem])(override val parseNode: Node) extends Def with WithParseNode

  case class ClassExtending(name: Name)(override val parseNode: Node) extends WithParseNode

  case class CollectionType(name: String, typeParams: TypeParams)(override val parseNode: Node) extends NoUnionType with WithParseNode

  case class ComplexExpr(expr: Expr)(override val parseNode: Node) extends StringExpr with WithParseNode

  sealed trait Def extends WithParseNode

  case class EnumDef(name: String, values: List[String])(override val parseNode: Node) extends Def with WithParseNode

  case class EscapeChar(code: Char)(override val parseNode: Node) extends StringElem with WithParseNode

  sealed trait Expr extends WithParseNode

  case class ImportAll(source: ImportSourceExpr, rename: String)(override val parseNode: Node) extends ImportDef with WithParseNode

  sealed trait ImportDef extends Def with WithParseNode

  case class ImportFrom(source: ImportSourceExpr, importing: Name, rename: Option[String])(override val parseNode: Node) extends ImportDef with WithParseNode

  case class ImportName(name: Name, rename: Option[String])(override val parseNode: Node) extends ImportDef with WithParseNode

  sealed trait ImportSourceExpr extends WithParseNode

  case class JustChar(chr: Char)(override val parseNode: Node) extends StringElem with WithParseNode

  case class ListExpr(elems: List[Expr])(override val parseNode: Node) extends Primary with WithParseNode

  sealed trait Literal extends Primary with WithParseNode

  case class MemberAccess(target: Primary, name: String)(override val parseNode: Node) extends Primary with WithParseNode

  case class MergeOp(lhs: Expr, rhs: Primary)(override val parseNode: Node) extends MergeOpOrPrimary with WithParseNode

  sealed trait MergeOpOrPrimary extends Expr with WithParseNode

  case class MethodRef(targetName: Name, className: Name, methodName: Option[String])(override val parseNode: Node) extends WithParseNode

  case class Name(tokens: List[String])(override val parseNode: Node) extends NoUnionType with WithParseNode

  case class NameDef(name: String, value: Expr)(override val parseNode: Node) extends Def with WithParseNode

  case class NameRef(name: String)(override val parseNode: Node) extends Primary with WithParseNode

  case class NamedExpr(name: String, expr: Expr)(override val parseNode: Node) extends WithParseNode

  case class NamedParam(name: String, value: Expr)(override val parseNode: Node) extends WithParseNode

  case class NamedTupleExpr(elems: List[NamedExpr])(override val parseNode: Node) extends Primary with WithParseNode

  case class NamedTupleType(elems: List[NamedType])(override val parseNode: Node) extends NoUnionType with WithParseNode

  case class NamedType(name: String, typ: TypeExpr)(override val parseNode: Node) extends WithParseNode

  case class NamespaceDef(name: String, body: BuildScript)(override val parseNode: Node) extends Def with WithParseNode

  sealed trait NoUnionType extends TypeExpr with WithParseNode

  case class NoneLiteral()(override val parseNode: Node) extends Literal with WithParseNode

  case class NoneType()(override val parseNode: Node) extends NoUnionType with WithParseNode

  case class ParamDef(name: String, optional: Boolean, typ: Option[TypeExpr], defaultValue: Option[Expr])(override val parseNode: Node) extends WithParseNode

  case class Paren(expr: Expr)(override val parseNode: Node) extends Primary with WithParseNode

  sealed trait Primary extends MergeOpOrPrimary with WithParseNode

  case class SimpleExpr(name: String)(override val parseNode: Node) extends StringExpr with WithParseNode

  sealed trait StringElem extends WithParseNode

  sealed trait StringExpr extends StringElem with WithParseNode

  case class StringLiteral(elems: List[StringElem])(override val parseNode: Node) extends ImportSourceExpr with Literal with WithParseNode

  case class TupleExpr(elems: List[Expr])(override val parseNode: Node) extends Primary with WithParseNode

  case class TupleType(elems: List[TypeExpr])(override val parseNode: Node) extends NoUnionType with WithParseNode

  sealed trait TypeExpr extends WithParseNode

  case class TypeParams(params: List[TypeExpr])(override val parseNode: Node) extends WithParseNode

  case class UnionType(elems: List[NoUnionType])(override val parseNode: Node) extends TypeExpr with WithParseNode


  def matchActionDef(node: Node): ActionDef = {
    val BindNode(v1, v2) = node
    val v23 = v1.id match {
      case 399 =>
        val v3 = v2.asInstanceOf[SequenceNode].children(2)
        val BindNode(v4, v5) = v3
        assert(v4.id == 87)
        val v6 = v2.asInstanceOf[SequenceNode].children(3)
        val BindNode(v7, v8) = v6
        assert(v7.id == 400)
        val BindNode(v9, v10) = v8
        val v19 = v9.id match {
          case 141 =>
            None
          case 401 =>
            val BindNode(v11, v12) = v10
            val v18 = v11.id match {
              case 402 =>
                val BindNode(v13, v14) = v12
                assert(v13.id == 403)
                val v15 = v14.asInstanceOf[SequenceNode].children(1)
                val BindNode(v16, v17) = v15
                assert(v16.id == 404)
                matchActionParams(v17)
            }
            Some(v18)
        }
        val v20 = v2.asInstanceOf[SequenceNode].children(7)
        val BindNode(v21, v22) = v20
        assert(v21.id == 406)
        ActionDef(matchSimpleName(v5), v19, matchActionExpr(v22))(v2)
    }
    v23
  }

  def matchActionExpr(node: Node): CallExpr = {
    val BindNode(v24, v25) = node
    val v29 = v24.id match {
      case 232 =>
        val v26 = v25.asInstanceOf[SequenceNode].children.head
        val BindNode(v27, v28) = v26
        assert(v27.id == 233)
        matchCallExpr(v28)
    }
    v29
  }

  def matchActionParams(node: Node): String = {
    val BindNode(v30, v31) = node
    val v35 = v30.id match {
      case 405 =>
        val v32 = v31.asInstanceOf[SequenceNode].children(2)
        val BindNode(v33, v34) = v32
        assert(v33.id == 87)
        matchSimpleName(v34)
    }
    v35
  }

  def matchActionRuleDef(node: Node): ActionRuleDef = {
    val BindNode(v36, v37) = node
    val v47 = v36.id match {
      case 341 =>
        val v38 = v37.asInstanceOf[SequenceNode].children(4)
        val BindNode(v39, v40) = v38
        assert(v39.id == 87)
        val v41 = v37.asInstanceOf[SequenceNode].children(6)
        val BindNode(v42, v43) = v41
        assert(v42.id == 348)
        val v44 = v37.asInstanceOf[SequenceNode].children(10)
        val BindNode(v45, v46) = v44
        assert(v45.id == 370)
        ActionRuleDef(matchSimpleName(v40), matchParamsDef(v43), matchMethodRef(v46))(v37)
    }
    v47
  }

  def matchArgDef(node: Node): ArgDef = {
    val BindNode(v48, v49) = node
    val v94 = v48.id match {
      case 381 =>
        val v50 = v49.asInstanceOf[SequenceNode].children(1)
        val BindNode(v51, v52) = v50
        assert(v51.id == 386)
        val BindNode(v53, v54) = v52
        val v62 = v53.id match {
          case 141 =>
            None
          case 387 =>
            val BindNode(v55, v56) = v54
            assert(v55.id == 388)
            val BindNode(v57, v58) = v56
            assert(v57.id == 389)
            val v59 = v58.asInstanceOf[SequenceNode].children(1)
            val BindNode(v60, v61) = v59
            assert(v60.id == 126)
            Some(matchName(v61))
        }
        val v63 = v49.asInstanceOf[SequenceNode].children(3)
        val BindNode(v64, v65) = v63
        assert(v64.id == 87)
        val v66 = v49.asInstanceOf[SequenceNode].children(4)
        val BindNode(v67, v68) = v66
        assert(v67.id == 390)
        val BindNode(v69, v70) = v68
        val v79 = v69.id match {
          case 141 =>
            None
          case 391 =>
            val BindNode(v71, v72) = v70
            val v78 = v71.id match {
              case 392 =>
                val BindNode(v73, v74) = v72
                assert(v73.id == 393)
                val v75 = v74.asInstanceOf[SequenceNode].children(3)
                val BindNode(v76, v77) = v75
                assert(v76.id == 194)
                matchTypeExpr(v77)
            }
            Some(v78)
        }
        val v80 = v49.asInstanceOf[SequenceNode].children(5)
        val BindNode(v81, v82) = v80
        assert(v81.id == 361)
        val BindNode(v83, v84) = v82
        val v93 = v83.id match {
          case 141 =>
            None
          case 362 =>
            val BindNode(v85, v86) = v84
            val v92 = v85.id match {
              case 363 =>
                val BindNode(v87, v88) = v86
                assert(v87.id == 364)
                val v89 = v88.asInstanceOf[SequenceNode].children(3)
                val BindNode(v90, v91) = v89
                assert(v90.id == 172)
                matchExpr(v91)
            }
            Some(v92)
        }
        ArgDef(v62, matchSimpleName(v65), v79, v93)(v49)
    }
    v94
  }

  def matchBooleanLiteral(node: Node): BooleanLiteral = {
    val BindNode(v95, v96) = node
    val v110 = v95.id match {
      case 287 =>
        val v97 = v96.asInstanceOf[SequenceNode].children.head
        val BindNode(v98, v99) = v97
        assert(v98.id == 288)
        val JoinNode(_, v100, _) = v99
        val BindNode(v101, v102) = v100
        assert(v101.id == 289)
        val BindNode(v103, v104) = v102
        val v109 = v103.id match {
          case 290 =>
            val BindNode(v105, v106) = v104
            assert(v105.id == 102)
            BooleanLiteral(true)(v106)
          case 291 =>
            val BindNode(v107, v108) = v104
            assert(v107.id == 108)
            BooleanLiteral(false)(v108)
        }
        v109
    }
    v110
  }

  def matchBuildRuleDef(node: Node): BuildRuleDef = {
    val BindNode(v111, v112) = node
    val v125 = v111.id match {
      case 396 =>
        val v113 = v112.asInstanceOf[SequenceNode].children(2)
        val BindNode(v114, v115) = v113
        assert(v114.id == 87)
        val v116 = v112.asInstanceOf[SequenceNode].children(4)
        val BindNode(v117, v118) = v116
        assert(v117.id == 348)
        val v119 = v112.asInstanceOf[SequenceNode].children(8)
        val BindNode(v120, v121) = v119
        assert(v120.id == 194)
        val v122 = v112.asInstanceOf[SequenceNode].children(12)
        val BindNode(v123, v124) = v122
        assert(v123.id == 370)
        BuildRuleDef(matchSimpleName(v115), matchParamsDef(v118), matchTypeExpr(v121), matchMethodRef(v124))(v112)
    }
    v125
  }

  def matchBuildScript(node: Node): BuildScript = {
    val BindNode(v126, v127) = node
    val v131 = v126.id match {
      case 3 =>
        val v128 = v127.asInstanceOf[SequenceNode].children(1)
        val BindNode(v129, v130) = v128
        assert(v129.id == 54)
        BuildScript(matchDefs(v130))(v127)
    }
    v131
  }

  def matchCallExpr(node: Node): CallExpr = {
    val BindNode(v132, v133) = node
    val v140 = v132.id match {
      case 234 =>
        val v134 = v133.asInstanceOf[SequenceNode].children.head
        val BindNode(v135, v136) = v134
        assert(v135.id == 126)
        val v137 = v133.asInstanceOf[SequenceNode].children(2)
        val BindNode(v138, v139) = v137
        assert(v138.id == 235)
        CallExpr(matchName(v136), matchCallParams(v139))(v133)
    }
    v140
  }

  def matchCallParams(node: Node): CallParams = {
    val BindNode(v141, v142) = node
    val v155 = v141.id match {
      case 236 =>
        CallParams(List(), List())(v142)
      case 237 =>
        val v143 = v142.asInstanceOf[SequenceNode].children(2)
        val BindNode(v144, v145) = v143
        assert(v144.id == 238)
        CallParams(matchPositionalParams(v145), List())(v142)
      case 249 =>
        val v146 = v142.asInstanceOf[SequenceNode].children(2)
        val BindNode(v147, v148) = v146
        assert(v147.id == 250)
        CallParams(List(), matchNamedParams(v148))(v142)
      case 260 =>
        val v149 = v142.asInstanceOf[SequenceNode].children(2)
        val BindNode(v150, v151) = v149
        assert(v150.id == 238)
        val v152 = v142.asInstanceOf[SequenceNode].children(6)
        val BindNode(v153, v154) = v152
        assert(v153.id == 250)
        CallParams(matchPositionalParams(v151), matchNamedParams(v154))(v142)
    }
    v155
  }

  def matchClassBody(node: Node): List[ClassBodyElem] = {
    val BindNode(v156, v157) = node
    val v170 = v156.id match {
      case 332 =>
        val v158 = v157.asInstanceOf[SequenceNode].children(1)
        val v159 = unrollRepeat0(v158).map { elem =>
          val BindNode(v160, v161) = elem
          assert(v160.id == 335)
          val BindNode(v162, v163) = v161
          val v169 = v162.id match {
            case 336 =>
              val BindNode(v164, v165) = v163
              assert(v164.id == 337)
              val v166 = v165.asInstanceOf[SequenceNode].children(1)
              val BindNode(v167, v168) = v166
              assert(v167.id == 338)
              matchClassBodyElem(v168)
          }
          v169
        }
        v159
    }
    v170
  }

  def matchClassBodyElem(node: Node): ClassBodyElem = {
    val BindNode(v171, v172) = node
    val v179 = v171.id match {
      case 339 =>
        val v173 = v172.asInstanceOf[SequenceNode].children.head
        val BindNode(v174, v175) = v173
        assert(v174.id == 340)
        matchActionRuleDef(v175)
      case 376 =>
        val v176 = v172.asInstanceOf[SequenceNode].children.head
        val BindNode(v177, v178) = v176
        assert(v177.id == 377)
        matchClassCastDef(v178)
    }
    v179
  }

  def matchClassCastDef(node: Node): ClassCastDef = {
    val BindNode(v180, v181) = node
    val v188 = v180.id match {
      case 378 =>
        val v182 = v181.asInstanceOf[SequenceNode].children(2)
        val BindNode(v183, v184) = v182
        assert(v183.id == 194)
        val v185 = v181.asInstanceOf[SequenceNode].children(6)
        val BindNode(v186, v187) = v185
        assert(v186.id == 172)
        ClassCastDef(matchTypeExpr(v184), matchExpr(v187))(v181)
    }
    v188
  }

  def matchClassDef(node: Node): ClassDef = {
    val BindNode(v189, v190) = node
    val v227 = v189.id match {
      case 304 =>
        val v191 = v190.asInstanceOf[SequenceNode].children(2)
        val BindNode(v192, v193) = v191
        assert(v192.id == 87)
        val v195 = v190.asInstanceOf[SequenceNode].children(3)
        val BindNode(v196, v197) = v195
        assert(v196.id == 308)
        val BindNode(v198, v199) = v197
        val v208 = v198.id match {
          case 141 =>
            None
          case 309 =>
            val BindNode(v200, v201) = v199
            val v207 = v200.id match {
              case 310 =>
                val BindNode(v202, v203) = v201
                assert(v202.id == 311)
                val v204 = v203.asInstanceOf[SequenceNode].children(1)
                val BindNode(v205, v206) = v204
                assert(v205.id == 312)
                matchClassExtendings(v206)
            }
            Some(v207)
        }
        val v194 = v208
        val v209 = v190.asInstanceOf[SequenceNode].children(5)
        val BindNode(v210, v211) = v209
        assert(v210.id == 325)
        val v213 = v190.asInstanceOf[SequenceNode].children(6)
        val BindNode(v214, v215) = v213
        assert(v214.id == 327)
        val BindNode(v216, v217) = v215
        val v226 = v216.id match {
          case 141 =>
            None
          case 328 =>
            val BindNode(v218, v219) = v217
            val v225 = v218.id match {
              case 329 =>
                val BindNode(v220, v221) = v219
                assert(v220.id == 330)
                val v222 = v221.asInstanceOf[SequenceNode].children(1)
                val BindNode(v223, v224) = v222
                assert(v223.id == 331)
                matchClassBody(v224)
            }
            Some(v225)
        }
        val v212 = v226
        ClassDef(matchSimpleName(v193), if (v194.isDefined) v194.get else List(), matchClassRealityDef(v211), if (v212.isDefined) v212.get else List())(v190)
    }
    v227
  }

  def matchClassExtending(node: Node): ClassExtending = {
    val BindNode(v228, v229) = node
    val v233 = v228.id match {
      case 175 =>
        val v230 = v229.asInstanceOf[SequenceNode].children.head
        val BindNode(v231, v232) = v230
        assert(v231.id == 126)
        ClassExtending(matchName(v232))(v229)
    }
    v233
  }

  def matchClassExtendings(node: Node): List[ClassExtending] = {
    val BindNode(v234, v235) = node
    val v251 = v234.id match {
      case 313 =>
        val v236 = v235.asInstanceOf[SequenceNode].children(2)
        val BindNode(v237, v238) = v236
        assert(v237.id == 319)
        val v239 = v235.asInstanceOf[SequenceNode].children(3)
        val v240 = unrollRepeat0(v239).map { elem =>
          val BindNode(v241, v242) = elem
          assert(v241.id == 322)
          val BindNode(v243, v244) = v242
          val v250 = v243.id match {
            case 323 =>
              val BindNode(v245, v246) = v244
              assert(v245.id == 324)
              val v247 = v246.asInstanceOf[SequenceNode].children(3)
              val BindNode(v248, v249) = v247
              assert(v248.id == 319)
              matchClassExtending(v249)
          }
          v250
        }
        List(matchClassExtending(v238)) ++ v240
    }
    v251
  }

  def matchClassRealityDef(node: Node): TypeExpr = {
    val BindNode(v252, v253) = node
    val v257 = v252.id match {
      case 326 =>
        val v254 = v253.asInstanceOf[SequenceNode].children(2)
        val BindNode(v255, v256) = v254
        assert(v255.id == 194)
        matchTypeExpr(v256)
    }
    v257
  }

  def matchCollectionType(node: Node): CollectionType = {
    val BindNode(v258, v259) = node
    val v278 = v258.id match {
      case 180 =>
        val v260 = v259.asInstanceOf[SequenceNode].children.head
        val BindNode(v261, v262) = v260
        assert(v261.id == 181)
        val JoinNode(_, v263, _) = v262
        val BindNode(v264, v265) = v263
        assert(v264.id == 182)
        val BindNode(v266, v267) = v265
        val v274 = v266.id match {
          case 183 =>
            val BindNode(v268, v269) = v267
            assert(v268.id == 184)
            val v270 = v269.asInstanceOf[SequenceNode].children.head
            "set"
          case 187 =>
            val BindNode(v271, v272) = v267
            assert(v271.id == 188)
            val v273 = v272.asInstanceOf[SequenceNode].children.head
            "list"
        }
        val v275 = v259.asInstanceOf[SequenceNode].children(2)
        val BindNode(v276, v277) = v275
        assert(v276.id == 191)
        CollectionType(v274, matchTypeParams(v277))(v259)
    }
    v278
  }

  def matchDef(node: Node): Def = {
    val BindNode(v279, v280) = node
    val v308 = v279.id match {
      case 57 =>
        val v281 = v280.asInstanceOf[SequenceNode].children.head
        val BindNode(v282, v283) = v281
        assert(v282.id == 58)
        matchNamespaceDef(v283)
      case 394 =>
        val v284 = v280.asInstanceOf[SequenceNode].children.head
        val BindNode(v285, v286) = v284
        assert(v285.id == 395)
        matchBuildRuleDef(v286)
      case 119 =>
        val v287 = v280.asInstanceOf[SequenceNode].children.head
        val BindNode(v288, v289) = v287
        assert(v288.id == 120)
        matchImportDef(v289)
      case 339 =>
        val v290 = v280.asInstanceOf[SequenceNode].children.head
        val BindNode(v291, v292) = v290
        assert(v291.id == 340)
        matchActionRuleDef(v292)
      case 302 =>
        val v293 = v280.asInstanceOf[SequenceNode].children.head
        val BindNode(v294, v295) = v293
        assert(v294.id == 303)
        matchClassDef(v295)
      case 407 =>
        val v296 = v280.asInstanceOf[SequenceNode].children.head
        val BindNode(v297, v298) = v296
        assert(v297.id == 408)
        matchEnumDef(v298)
      case 379 =>
        val v299 = v280.asInstanceOf[SequenceNode].children.head
        val BindNode(v300, v301) = v299
        assert(v300.id == 380)
        matchArgDef(v301)
      case 300 =>
        val v302 = v280.asInstanceOf[SequenceNode].children.head
        val BindNode(v303, v304) = v302
        assert(v303.id == 301)
        matchNameDef(v304)
      case 397 =>
        val v305 = v280.asInstanceOf[SequenceNode].children.head
        val BindNode(v306, v307) = v305
        assert(v306.id == 398)
        matchActionDef(v307)
    }
    v308
  }

  def matchDefs(node: Node): List[Def] = {
    val BindNode(v309, v310) = node
    val v326 = v309.id match {
      case 55 =>
        val v311 = v310.asInstanceOf[SequenceNode].children.head
        val BindNode(v312, v313) = v311
        assert(v312.id == 56)
        val v314 = v310.asInstanceOf[SequenceNode].children(1)
        val v315 = unrollRepeat0(v314).map { elem =>
          val BindNode(v316, v317) = elem
          assert(v316.id == 420)
          val BindNode(v318, v319) = v317
          val v325 = v318.id match {
            case 421 =>
              val BindNode(v320, v321) = v319
              assert(v320.id == 422)
              val v322 = v321.asInstanceOf[SequenceNode].children(1)
              val BindNode(v323, v324) = v322
              assert(v323.id == 56)
              matchDef(v324)
          }
          v325
        }
        List(matchDef(v313)) ++ v315
    }
    v326
  }

  def matchEnumDef(node: Node): EnumDef = {
    val BindNode(v327, v328) = node
    val v347 = v327.id match {
      case 409 =>
        val v329 = v328.asInstanceOf[SequenceNode].children(2)
        val BindNode(v330, v331) = v329
        assert(v330.id == 87)
        val v332 = v328.asInstanceOf[SequenceNode].children(6)
        val BindNode(v333, v334) = v332
        assert(v333.id == 87)
        val v335 = v328.asInstanceOf[SequenceNode].children(7)
        val v336 = unrollRepeat0(v335).map { elem =>
          val BindNode(v337, v338) = elem
          assert(v337.id == 415)
          val BindNode(v339, v340) = v338
          val v346 = v339.id match {
            case 416 =>
              val BindNode(v341, v342) = v340
              assert(v341.id == 417)
              val v343 = v342.asInstanceOf[SequenceNode].children(3)
              val BindNode(v344, v345) = v343
              assert(v344.id == 87)
              matchSimpleName(v345)
          }
          v346
        }
        EnumDef(matchSimpleName(v331), List(matchSimpleName(v334)) ++ v336)(v328)
    }
    v347
  }

  def matchEscapeChar(node: Node): EscapeChar = {
    val BindNode(v348, v349) = node
    val v353 = v348.id match {
      case 160 =>
        val v350 = v349.asInstanceOf[SequenceNode].children(1)
        val BindNode(v351, v352) = v350
        assert(v351.id == 162)
        EscapeChar(v352.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char)(v349)
    }
    v353
  }

  def matchExpr(node: Node): Expr = {
    val BindNode(v354, v355) = node
    val v365 = v354.id match {
      case 173 =>
        val v356 = v355.asInstanceOf[SequenceNode].children.head
        val BindNode(v357, v358) = v356
        assert(v357.id == 172)
        val v359 = v355.asInstanceOf[SequenceNode].children(4)
        val BindNode(v360, v361) = v359
        assert(v360.id == 174)
        CastExpr(matchExpr(v358), matchNoUnionType(v361))(v355)
      case 227 =>
        val v362 = v355.asInstanceOf[SequenceNode].children.head
        val BindNode(v363, v364) = v362
        assert(v363.id == 228)
        matchMergeOpOrPrimary(v364)
    }
    v365
  }

  def matchImportDef(node: Node): ImportDef = {
    val BindNode(v366, v367) = node
    val v411 = v366.id match {
      case 121 =>
        val v368 = v367.asInstanceOf[SequenceNode].children(2)
        val BindNode(v369, v370) = v368
        assert(v369.id == 126)
        val v371 = v367.asInstanceOf[SequenceNode].children(3)
        val BindNode(v372, v373) = v371
        assert(v372.id == 134)
        val BindNode(v374, v375) = v373
        val v384 = v374.id match {
          case 141 =>
            None
          case 135 =>
            val BindNode(v376, v377) = v375
            val v383 = v376.id match {
              case 136 =>
                val BindNode(v378, v379) = v377
                assert(v378.id == 137)
                val v380 = v379.asInstanceOf[SequenceNode].children(3)
                val BindNode(v381, v382) = v380
                assert(v381.id == 87)
                matchSimpleName(v382)
            }
            Some(v383)
        }
        ImportName(matchName(v370), v384)(v367)
      case 142 =>
        val v385 = v367.asInstanceOf[SequenceNode].children(2)
        val BindNode(v386, v387) = v385
        assert(v386.id == 143)
        val v388 = v367.asInstanceOf[SequenceNode].children(6)
        val BindNode(v389, v390) = v388
        assert(v389.id == 87)
        ImportAll(matchImportSourceExpr(v387), matchSimpleName(v390))(v367)
      case 296 =>
        val v391 = v367.asInstanceOf[SequenceNode].children(2)
        val BindNode(v392, v393) = v391
        assert(v392.id == 143)
        val v394 = v367.asInstanceOf[SequenceNode].children(6)
        val BindNode(v395, v396) = v394
        assert(v395.id == 126)
        val v397 = v367.asInstanceOf[SequenceNode].children(7)
        val BindNode(v398, v399) = v397
        assert(v398.id == 134)
        val BindNode(v400, v401) = v399
        val v410 = v400.id match {
          case 141 =>
            None
          case 135 =>
            val BindNode(v402, v403) = v401
            val v409 = v402.id match {
              case 136 =>
                val BindNode(v404, v405) = v403
                assert(v404.id == 137)
                val v406 = v405.asInstanceOf[SequenceNode].children(3)
                val BindNode(v407, v408) = v406
                assert(v407.id == 87)
                matchSimpleName(v408)
            }
            Some(v409)
        }
        ImportFrom(matchImportSourceExpr(v393), matchName(v396), v410)(v367)
    }
    v411
  }

  def matchImportSourceExpr(node: Node): ImportSourceExpr = {
    val BindNode(v412, v413) = node
    val v420 = v412.id match {
      case 144 =>
        val v414 = v413.asInstanceOf[SequenceNode].children.head
        val BindNode(v415, v416) = v414
        assert(v415.id == 145)
        matchStringLiteral(v416)
      case 232 =>
        val v417 = v413.asInstanceOf[SequenceNode].children.head
        val BindNode(v418, v419) = v417
        assert(v418.id == 233)
        matchCallExpr(v419)
    }
    v420
  }

  def matchLiteral(node: Node): Literal = {
    val BindNode(v421, v422) = node
    val v432 = v421.id match {
      case 144 =>
        val v423 = v422.asInstanceOf[SequenceNode].children.head
        val BindNode(v424, v425) = v423
        assert(v424.id == 145)
        matchStringLiteral(v425)
      case 285 =>
        val v426 = v422.asInstanceOf[SequenceNode].children.head
        val BindNode(v427, v428) = v426
        assert(v427.id == 286)
        matchBooleanLiteral(v428)
      case 292 =>
        val v429 = v422.asInstanceOf[SequenceNode].children.head
        val BindNode(v430, v431) = v429
        assert(v430.id == 293)
        matchNoneLiteral(v431)
    }
    v432
  }

  def matchMergeOpOrPrimary(node: Node): MergeOpOrPrimary = {
    val BindNode(v433, v434) = node
    val v444 = v433.id match {
      case 229 =>
        val v435 = v434.asInstanceOf[SequenceNode].children.head
        val BindNode(v436, v437) = v435
        assert(v436.id == 172)
        val v438 = v434.asInstanceOf[SequenceNode].children(4)
        val BindNode(v439, v440) = v438
        assert(v439.id == 231)
        MergeOp(matchExpr(v437), matchPrimary(v440))(v434)
      case 295 =>
        val v441 = v434.asInstanceOf[SequenceNode].children.head
        val BindNode(v442, v443) = v441
        assert(v442.id == 231)
        matchPrimary(v443)
    }
    v444
  }

  def matchMethodRef(node: Node): MethodRef = {
    val BindNode(v445, v446) = node
    val v467 = v445.id match {
      case 371 =>
        val v447 = v446.asInstanceOf[SequenceNode].children.head
        val BindNode(v448, v449) = v447
        assert(v448.id == 126)
        val v450 = v446.asInstanceOf[SequenceNode].children(4)
        val BindNode(v451, v452) = v450
        assert(v451.id == 126)
        val v453 = v446.asInstanceOf[SequenceNode].children(5)
        val BindNode(v454, v455) = v453
        assert(v454.id == 372)
        val BindNode(v456, v457) = v455
        val v466 = v456.id match {
          case 141 =>
            None
          case 373 =>
            val BindNode(v458, v459) = v457
            val v465 = v458.id match {
              case 374 =>
                val BindNode(v460, v461) = v459
                assert(v460.id == 375)
                val v462 = v461.asInstanceOf[SequenceNode].children(3)
                val BindNode(v463, v464) = v462
                assert(v463.id == 87)
                matchSimpleName(v464)
            }
            Some(v465)
        }
        MethodRef(matchName(v449), matchName(v452), v466)(v446)
    }
    v467
  }

  def matchName(node: Node): Name = {
    val BindNode(v468, v469) = node
    val v485 = v468.id match {
      case 127 =>
        val v470 = v469.asInstanceOf[SequenceNode].children.head
        val BindNode(v471, v472) = v470
        assert(v471.id == 87)
        val v473 = v469.asInstanceOf[SequenceNode].children(1)
        val v474 = unrollRepeat0(v473).map { elem =>
          val BindNode(v475, v476) = elem
          assert(v475.id == 130)
          val BindNode(v477, v478) = v476
          val v484 = v477.id match {
            case 131 =>
              val BindNode(v479, v480) = v478
              assert(v479.id == 132)
              val v481 = v480.asInstanceOf[SequenceNode].children(3)
              val BindNode(v482, v483) = v481
              assert(v482.id == 87)
              matchSimpleName(v483)
          }
          v484
        }
        Name(List(matchSimpleName(v472)) ++ v474)(v469)
    }
    v485
  }

  def matchNameDef(node: Node): NameDef = {
    val BindNode(v486, v487) = node
    val v494 = v486.id match {
      case 253 =>
        val v488 = v487.asInstanceOf[SequenceNode].children.head
        val BindNode(v489, v490) = v488
        assert(v489.id == 87)
        val v491 = v487.asInstanceOf[SequenceNode].children(4)
        val BindNode(v492, v493) = v491
        assert(v492.id == 172)
        NameDef(matchSimpleName(v490), matchExpr(v493))(v487)
    }
    v494
  }

  def matchNamedExpr(node: Node): NamedExpr = {
    val BindNode(v495, v496) = node
    val v503 = v495.id match {
      case 277 =>
        val v497 = v496.asInstanceOf[SequenceNode].children.head
        val BindNode(v498, v499) = v497
        assert(v498.id == 87)
        val v500 = v496.asInstanceOf[SequenceNode].children(4)
        val BindNode(v501, v502) = v500
        assert(v501.id == 172)
        NamedExpr(matchSimpleName(v499), matchExpr(v502))(v496)
    }
    v503
  }

  def matchNamedParam(node: Node): NamedParam = {
    val BindNode(v504, v505) = node
    val v512 = v504.id match {
      case 253 =>
        val v506 = v505.asInstanceOf[SequenceNode].children.head
        val BindNode(v507, v508) = v506
        assert(v507.id == 87)
        val v509 = v505.asInstanceOf[SequenceNode].children(4)
        val BindNode(v510, v511) = v509
        assert(v510.id == 172)
        NamedParam(matchSimpleName(v508), matchExpr(v511))(v505)
    }
    v512
  }

  def matchNamedParams(node: Node): List[NamedParam] = {
    val BindNode(v513, v514) = node
    val v530 = v513.id match {
      case 251 =>
        val v515 = v514.asInstanceOf[SequenceNode].children.head
        val BindNode(v516, v517) = v515
        assert(v516.id == 252)
        val v518 = v514.asInstanceOf[SequenceNode].children(1)
        val v519 = unrollRepeat0(v518).map { elem =>
          val BindNode(v520, v521) = elem
          assert(v520.id == 257)
          val BindNode(v522, v523) = v521
          val v529 = v522.id match {
            case 258 =>
              val BindNode(v524, v525) = v523
              assert(v524.id == 259)
              val v526 = v525.asInstanceOf[SequenceNode].children(3)
              val BindNode(v527, v528) = v526
              assert(v527.id == 252)
              matchNamedParam(v528)
          }
          v529
        }
        List(matchNamedParam(v517)) ++ v519
    }
    v530
  }

  def matchNamedTupleType(node: Node): NamedTupleType = {
    val BindNode(v531, v532) = node
    val v548 = v531.id match {
      case 218 =>
        val v533 = v532.asInstanceOf[SequenceNode].children(2)
        val BindNode(v534, v535) = v533
        assert(v534.id == 219)
        val v536 = v532.asInstanceOf[SequenceNode].children(3)
        val v537 = unrollRepeat0(v536).map { elem =>
          val BindNode(v538, v539) = elem
          assert(v538.id == 224)
          val BindNode(v540, v541) = v539
          val v547 = v540.id match {
            case 225 =>
              val BindNode(v542, v543) = v541
              assert(v542.id == 226)
              val v544 = v543.asInstanceOf[SequenceNode].children(3)
              val BindNode(v545, v546) = v544
              assert(v545.id == 219)
              matchNamedType(v546)
          }
          v547
        }
        NamedTupleType(List(matchNamedType(v535)) ++ v537)(v532)
    }
    v548
  }

  def matchNamedType(node: Node): NamedType = {
    val BindNode(v549, v550) = node
    val v557 = v549.id match {
      case 220 =>
        val v551 = v550.asInstanceOf[SequenceNode].children.head
        val BindNode(v552, v553) = v551
        assert(v552.id == 87)
        val v554 = v550.asInstanceOf[SequenceNode].children(4)
        val BindNode(v555, v556) = v554
        assert(v555.id == 194)
        NamedType(matchSimpleName(v553), matchTypeExpr(v556))(v550)
    }
    v557
  }

  def matchNamespaceDef(node: Node): NamespaceDef = {
    val BindNode(v558, v559) = node
    val v566 = v558.id match {
      case 59 =>
        val v560 = v559.asInstanceOf[SequenceNode].children(2)
        val BindNode(v561, v562) = v560
        assert(v561.id == 87)
        val v563 = v559.asInstanceOf[SequenceNode].children(5)
        val BindNode(v564, v565) = v563
        assert(v564.id == 2)
        NamespaceDef(matchSimpleName(v562), matchBuildScript(v565))(v559)
    }
    v566
  }

  def matchNoUnionType(node: Node): NoUnionType = {
    val BindNode(v567, v568) = node
    val v581 = v567.id match {
      case 175 =>
        val v569 = v568.asInstanceOf[SequenceNode].children.head
        val BindNode(v570, v571) = v569
        assert(v570.id == 126)
        matchName(v571)
      case 176 =>
        NoneType()(v568)
      case 211 =>
        val v572 = v568.asInstanceOf[SequenceNode].children.head
        val BindNode(v573, v574) = v572
        assert(v573.id == 212)
        matchTupleType(v574)
      case 178 =>
        val v575 = v568.asInstanceOf[SequenceNode].children.head
        val BindNode(v576, v577) = v575
        assert(v576.id == 179)
        matchCollectionType(v577)
      case 216 =>
        val v578 = v568.asInstanceOf[SequenceNode].children.head
        val BindNode(v579, v580) = v578
        assert(v579.id == 217)
        matchNamedTupleType(v580)
    }
    v581
  }

  def matchNoneLiteral(node: Node): NoneLiteral = {
    val BindNode(v582, v583) = node
    val v584 = v582.id match {
      case 176 =>
        NoneLiteral()(v583)
    }
    v584
  }

  def matchParamDef(node: Node): ParamDef = {
    val BindNode(v585, v586) = node
    val v627 = v585.id match {
      case 355 =>
        val v587 = v586.asInstanceOf[SequenceNode].children.head
        val BindNode(v588, v589) = v587
        assert(v588.id == 87)
        val v590 = v586.asInstanceOf[SequenceNode].children(1)
        val BindNode(v591, v592) = v590
        assert(v591.id == 356)
        val BindNode(v593, v594) = v592
        val v603 = v593.id match {
          case 141 =>
            None
          case 357 =>
            val BindNode(v595, v596) = v594
            val v602 = v595.id match {
              case 358 =>
                val BindNode(v597, v598) = v596
                assert(v597.id == 359)
                val v599 = v598.asInstanceOf[SequenceNode].children(1)
                val BindNode(v600, v601) = v599
                assert(v600.id == 360)
                v601.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
            }
            Some(v602)
        }
        val v604 = v586.asInstanceOf[SequenceNode].children(5)
        val BindNode(v605, v606) = v604
        assert(v605.id == 194)
        val v607 = v586.asInstanceOf[SequenceNode].children(6)
        val BindNode(v608, v609) = v607
        assert(v608.id == 361)
        val BindNode(v610, v611) = v609
        val v620 = v610.id match {
          case 141 =>
            None
          case 362 =>
            val BindNode(v612, v613) = v611
            val v619 = v612.id match {
              case 363 =>
                val BindNode(v614, v615) = v613
                assert(v614.id == 364)
                val v616 = v615.asInstanceOf[SequenceNode].children(3)
                val BindNode(v617, v618) = v616
                assert(v617.id == 172)
                matchExpr(v618)
            }
            Some(v619)
        }
        ParamDef(matchSimpleName(v589), v603.isDefined, Some(matchTypeExpr(v606)), v620)(v586)
      case 253 =>
        val v621 = v586.asInstanceOf[SequenceNode].children.head
        val BindNode(v622, v623) = v621
        assert(v622.id == 87)
        val v624 = v586.asInstanceOf[SequenceNode].children(4)
        val BindNode(v625, v626) = v624
        assert(v625.id == 172)
        ParamDef(matchSimpleName(v623), false, None, Some(matchExpr(v626)))(v586)
    }
    v627
  }

  def matchParamsDef(node: Node): List[ParamDef] = {
    val BindNode(v628, v629) = node
    val v656 = v628.id match {
      case 349 =>
        val v631 = v629.asInstanceOf[SequenceNode].children(1)
        val BindNode(v632, v633) = v631
        assert(v632.id == 350)
        val BindNode(v634, v635) = v633
        val v655 = v634.id match {
          case 141 =>
            None
          case 351 =>
            val BindNode(v636, v637) = v635
            assert(v636.id == 352)
            val BindNode(v638, v639) = v637
            assert(v638.id == 353)
            val v640 = v639.asInstanceOf[SequenceNode].children(1)
            val BindNode(v641, v642) = v640
            assert(v641.id == 354)
            val v643 = v639.asInstanceOf[SequenceNode].children(2)
            val v644 = unrollRepeat0(v643).map { elem =>
              val BindNode(v645, v646) = elem
              assert(v645.id == 367)
              val BindNode(v647, v648) = v646
              val v654 = v647.id match {
                case 368 =>
                  val BindNode(v649, v650) = v648
                  assert(v649.id == 369)
                  val v651 = v650.asInstanceOf[SequenceNode].children(3)
                  val BindNode(v652, v653) = v651
                  assert(v652.id == 354)
                  matchParamDef(v653)
              }
              v654
            }
            Some(List(matchParamDef(v642)) ++ v644)
        }
        val v630 = v655
        if (v630.isDefined) v630.get else List()
    }
    v656
  }

  def matchPositionalParams(node: Node): List[Expr] = {
    val BindNode(v657, v658) = node
    val v674 = v657.id match {
      case 239 =>
        val v659 = v658.asInstanceOf[SequenceNode].children.head
        val BindNode(v660, v661) = v659
        assert(v660.id == 172)
        val v662 = v658.asInstanceOf[SequenceNode].children(1)
        val v663 = unrollRepeat0(v662).map { elem =>
          val BindNode(v664, v665) = elem
          assert(v664.id == 242)
          val BindNode(v666, v667) = v665
          val v673 = v666.id match {
            case 243 =>
              val BindNode(v668, v669) = v667
              assert(v668.id == 244)
              val v670 = v669.asInstanceOf[SequenceNode].children(3)
              val BindNode(v671, v672) = v670
              assert(v671.id == 172)
              matchExpr(v672)
          }
          v673
        }
        List(matchExpr(v661)) ++ v663
    }
    v674
  }

  def matchPrimary(node: Node): Primary = {
    val BindNode(v675, v676) = node
    val v777 = v675.id match {
      case 294 =>
        val v677 = v676.asInstanceOf[SequenceNode].children(2)
        val BindNode(v678, v679) = v677
        assert(v678.id == 172)
        Paren(matchExpr(v679))(v676)
      case 283 =>
        val v680 = v676.asInstanceOf[SequenceNode].children.head
        val BindNode(v681, v682) = v680
        assert(v681.id == 284)
        matchLiteral(v682)
      case 271 =>
        val v684 = v676.asInstanceOf[SequenceNode].children(1)
        val BindNode(v685, v686) = v684
        assert(v685.id == 272)
        val BindNode(v687, v688) = v686
        val v708 = v687.id match {
          case 141 =>
            None
          case 273 =>
            val BindNode(v689, v690) = v688
            assert(v689.id == 274)
            val BindNode(v691, v692) = v690
            assert(v691.id == 275)
            val v693 = v692.asInstanceOf[SequenceNode].children(1)
            val BindNode(v694, v695) = v693
            assert(v694.id == 276)
            val v696 = v692.asInstanceOf[SequenceNode].children(2)
            val v697 = unrollRepeat0(v696).map { elem =>
              val BindNode(v698, v699) = elem
              assert(v698.id == 280)
              val BindNode(v700, v701) = v699
              val v707 = v700.id match {
                case 281 =>
                  val BindNode(v702, v703) = v701
                  assert(v702.id == 282)
                  val v704 = v703.asInstanceOf[SequenceNode].children(3)
                  val BindNode(v705, v706) = v704
                  assert(v705.id == 276)
                  matchNamedExpr(v706)
              }
              v707
            }
            Some(List(matchNamedExpr(v695)) ++ v697)
        }
        val v683 = v708
        NamedTupleExpr(if (v683.isDefined) v683.get else List())(v676)
      case 232 =>
        val v709 = v676.asInstanceOf[SequenceNode].children.head
        val BindNode(v710, v711) = v709
        assert(v710.id == 233)
        matchCallExpr(v711)
      case 270 =>
        val v712 = v676.asInstanceOf[SequenceNode].children(2)
        val BindNode(v713, v714) = v712
        assert(v713.id == 172)
        val v716 = v676.asInstanceOf[SequenceNode].children(5)
        val BindNode(v717, v718) = v716
        assert(v717.id == 265)
        val BindNode(v719, v720) = v718
        val v741 = v719.id match {
          case 141 =>
            None
          case 266 =>
            val BindNode(v721, v722) = v720
            val v740 = v721.id match {
              case 267 =>
                val BindNode(v723, v724) = v722
                assert(v723.id == 268)
                val v725 = v724.asInstanceOf[SequenceNode].children(1)
                val BindNode(v726, v727) = v725
                assert(v726.id == 172)
                val v728 = v724.asInstanceOf[SequenceNode].children(2)
                val v729 = unrollRepeat0(v728).map { elem =>
                  val BindNode(v730, v731) = elem
                  assert(v730.id == 242)
                  val BindNode(v732, v733) = v731
                  val v739 = v732.id match {
                    case 243 =>
                      val BindNode(v734, v735) = v733
                      assert(v734.id == 244)
                      val v736 = v735.asInstanceOf[SequenceNode].children(3)
                      val BindNode(v737, v738) = v736
                      assert(v737.id == 172)
                      matchExpr(v738)
                  }
                  v739
                }
                List(matchExpr(v727)) ++ v729
            }
            Some(v740)
        }
        val v715 = v741
        TupleExpr(List(matchExpr(v714)) ++ (if (v715.isDefined) v715.get else List()))(v676)
      case 262 =>
        val v742 = v676.asInstanceOf[SequenceNode].children.head
        val BindNode(v743, v744) = v742
        assert(v743.id == 87)
        NameRef(matchSimpleName(v744))(v676)
      case 261 =>
        val v745 = v676.asInstanceOf[SequenceNode].children.head
        val BindNode(v746, v747) = v745
        assert(v746.id == 231)
        val v748 = v676.asInstanceOf[SequenceNode].children(4)
        val BindNode(v749, v750) = v748
        assert(v749.id == 87)
        MemberAccess(matchPrimary(v747), matchSimpleName(v750))(v676)
      case 263 =>
        val v752 = v676.asInstanceOf[SequenceNode].children(1)
        val BindNode(v753, v754) = v752
        assert(v753.id == 265)
        val BindNode(v755, v756) = v754
        val v776 = v755.id match {
          case 141 =>
            None
          case 266 =>
            val BindNode(v757, v758) = v756
            assert(v757.id == 267)
            val BindNode(v759, v760) = v758
            assert(v759.id == 268)
            val v761 = v760.asInstanceOf[SequenceNode].children(1)
            val BindNode(v762, v763) = v761
            assert(v762.id == 172)
            val v764 = v760.asInstanceOf[SequenceNode].children(2)
            val v765 = unrollRepeat0(v764).map { elem =>
              val BindNode(v766, v767) = elem
              assert(v766.id == 242)
              val BindNode(v768, v769) = v767
              val v775 = v768.id match {
                case 243 =>
                  val BindNode(v770, v771) = v769
                  assert(v770.id == 244)
                  val v772 = v771.asInstanceOf[SequenceNode].children(3)
                  val BindNode(v773, v774) = v772
                  assert(v773.id == 172)
                  matchExpr(v774)
              }
              v775
            }
            Some(List(matchExpr(v763)) ++ v765)
        }
        val v751 = v776
        ListExpr(if (v751.isDefined) v751.get else List())(v676)
    }
    v777
  }

  def matchSimpleName(node: Node): String = {
    val BindNode(v778, v779) = node
    val v810 = v778.id match {
      case 88 =>
        val v780 = v779.asInstanceOf[SequenceNode].children.head
        val BindNode(v781, v782) = v780
        assert(v781.id == 89)
        val BindNode(v783, v784) = v782
        assert(v783.id == 90)
        val BindNode(v785, v786) = v784
        assert(v785.id == 91)
        val BindNode(v787, v788) = v786
        val v809 = v787.id match {
          case 92 =>
            val BindNode(v789, v790) = v788
            assert(v789.id == 93)
            val v791 = v790.asInstanceOf[SequenceNode].children.head
            val BindNode(v792, v793) = v791
            assert(v792.id == 94)
            val JoinNode(_, v794, _) = v793
            val BindNode(v795, v796) = v794
            assert(v795.id == 95)
            val BindNode(v797, v798) = v796
            val v808 = v797.id match {
              case 96 =>
                val BindNode(v799, v800) = v798
                assert(v799.id == 97)
                val v801 = v800.asInstanceOf[SequenceNode].children.head
                val BindNode(v802, v803) = v801
                assert(v802.id == 98)
                val v804 = v800.asInstanceOf[SequenceNode].children(1)
                val v805 = unrollRepeat0(v804).map { elem =>
                  val BindNode(v806, v807) = elem
                  assert(v806.id == 77)
                  v807.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
                }
                v803.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char.toString + v805.map(x => x.toString).mkString("")
            }
            v808
        }
        v809
    }
    v810
  }

  def matchStringElem(node: Node): StringElem = {
    val BindNode(v811, v812) = node
    val v824 = v811.id match {
      case 155 =>
        val v813 = v812.asInstanceOf[SequenceNode].children.head
        val BindNode(v814, v815) = v813
        assert(v814.id == 156)
        val BindNode(v816, v817) = v815
        assert(v816.id == 30)
        JustChar(v817.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char)(v812)
      case 158 =>
        val v818 = v812.asInstanceOf[SequenceNode].children.head
        val BindNode(v819, v820) = v818
        assert(v819.id == 159)
        matchEscapeChar(v820)
      case 163 =>
        val v821 = v812.asInstanceOf[SequenceNode].children.head
        val BindNode(v822, v823) = v821
        assert(v822.id == 164)
        matchStringExpr(v823)
    }
    v824
  }

  def matchStringExpr(node: Node): StringExpr = {
    val BindNode(v825, v826) = node
    val v843 = v825.id match {
      case 165 =>
        val v827 = v826.asInstanceOf[SequenceNode].children.head
        val BindNode(v828, v829) = v827
        assert(v828.id == 166)
        val BindNode(v830, v831) = v829
        assert(v830.id == 167)
        val BindNode(v832, v833) = v831
        val v839 = v832.id match {
          case 168 =>
            val BindNode(v834, v835) = v833
            assert(v834.id == 169)
            val v836 = v835.asInstanceOf[SequenceNode].children(1)
            val BindNode(v837, v838) = v836
            assert(v837.id == 87)
            matchSimpleName(v838)
        }
        SimpleExpr(v839)(v826)
      case 171 =>
        val v840 = v826.asInstanceOf[SequenceNode].children(3)
        val BindNode(v841, v842) = v840
        assert(v841.id == 172)
        ComplexExpr(matchExpr(v842))(v826)
    }
    v843
  }

  def matchStringLiteral(node: Node): StringLiteral = {
    val BindNode(v844, v845) = node
    val v860 = v844.id match {
      case 146 =>
        val v846 = v845.asInstanceOf[SequenceNode].children(1)
        val v847 = unrollRepeat0(v846).map { elem =>
          val BindNode(v848, v849) = elem
          assert(v848.id == 150)
          val BindNode(v850, v851) = v849
          assert(v850.id == 151)
          val BindNode(v852, v853) = v851
          val v859 = v852.id match {
            case 152 =>
              val BindNode(v854, v855) = v853
              assert(v854.id == 153)
              val v856 = v855.asInstanceOf[SequenceNode].children.head
              val BindNode(v857, v858) = v856
              assert(v857.id == 154)
              matchStringElem(v858)
          }
          v859
        }
        StringLiteral(v847)(v845)
    }
    v860
  }

  def matchTupleType(node: Node): TupleType = {
    val BindNode(v861, v862) = node
    val v878 = v861.id match {
      case 213 =>
        val v863 = v862.asInstanceOf[SequenceNode].children(2)
        val BindNode(v864, v865) = v863
        assert(v864.id == 194)
        val v866 = v862.asInstanceOf[SequenceNode].children(3)
        val v867 = unrollRepeat0(v866).map { elem =>
          val BindNode(v868, v869) = elem
          assert(v868.id == 207)
          val BindNode(v870, v871) = v869
          val v877 = v870.id match {
            case 208 =>
              val BindNode(v872, v873) = v871
              assert(v872.id == 209)
              val v874 = v873.asInstanceOf[SequenceNode].children(3)
              val BindNode(v875, v876) = v874
              assert(v875.id == 194)
              matchTypeExpr(v876)
          }
          v877
        }
        TupleType(List(matchTypeExpr(v865)) ++ v867)(v862)
    }
    v878
  }

  def matchTypeExpr(node: Node): TypeExpr = {
    val BindNode(v879, v880) = node
    val v887 = v879.id match {
      case 195 =>
        val v881 = v880.asInstanceOf[SequenceNode].children.head
        val BindNode(v882, v883) = v881
        assert(v882.id == 174)
        matchNoUnionType(v883)
      case 196 =>
        val v884 = v880.asInstanceOf[SequenceNode].children.head
        val BindNode(v885, v886) = v884
        assert(v885.id == 197)
        matchUnionType(v886)
    }
    v887
  }

  def matchTypeParams(node: Node): TypeParams = {
    val BindNode(v888, v889) = node
    val v905 = v888.id match {
      case 192 =>
        val v890 = v889.asInstanceOf[SequenceNode].children(2)
        val BindNode(v891, v892) = v890
        assert(v891.id == 194)
        val v893 = v889.asInstanceOf[SequenceNode].children(3)
        val v894 = unrollRepeat0(v893).map { elem =>
          val BindNode(v895, v896) = elem
          assert(v895.id == 207)
          val BindNode(v897, v898) = v896
          val v904 = v897.id match {
            case 208 =>
              val BindNode(v899, v900) = v898
              assert(v899.id == 209)
              val v901 = v900.asInstanceOf[SequenceNode].children(3)
              val BindNode(v902, v903) = v901
              assert(v902.id == 194)
              matchTypeExpr(v903)
          }
          v904
        }
        TypeParams(List(matchTypeExpr(v892)) ++ v894)(v889)
    }
    v905
  }

  def matchUnionType(node: Node): UnionType = {
    val BindNode(v906, v907) = node
    val v923 = v906.id match {
      case 198 =>
        val v908 = v907.asInstanceOf[SequenceNode].children(2)
        val BindNode(v909, v910) = v908
        assert(v909.id == 174)
        val v911 = v907.asInstanceOf[SequenceNode].children(3)
        val v912 = unrollRepeat0(v911).map { elem =>
          val BindNode(v913, v914) = elem
          assert(v913.id == 201)
          val BindNode(v915, v916) = v914
          val v922 = v915.id match {
            case 202 =>
              val BindNode(v917, v918) = v916
              assert(v917.id == 203)
              val v919 = v918.asInstanceOf[SequenceNode].children(3)
              val BindNode(v920, v921) = v919
              assert(v920.id == 174)
              matchNoUnionType(v921)
          }
          v922
        }
        UnionType(List(matchNoUnionType(v910)) ++ v912)(v907)
    }
    v923
  }

  def matchStart(node: Node): BuildScript = {
    val BindNode(start, BindNode(_, body)) = node
    assert(start.id == 1)
    matchBuildScript(body)
  }

  val milestoneParserData: MilestoneParserData = MilestoneParserProtobufConverter.convertProtoToMilestoneParserData(
    MilestoneParserDataProto.MilestoneParserData.parseFrom(getClass.getResourceAsStream("/parserdata.pb")))

  val milestoneParser = new MilestoneParser(milestoneParserData)

  def parse(text: String): Either[ParseForest, ParsingErrors.ParsingError] =
    milestoneParser.parseAndReconstructToForest(text)

  def parseAst(text: String): Either[BuildScript, ParsingErrors.ParsingError] =
    parse(text) match {
      case Left(forest) => Left(matchStart(forest.trees.head))
      case Right(error) => Right(error)
    }
}
