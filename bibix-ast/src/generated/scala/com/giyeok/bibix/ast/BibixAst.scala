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
      case 392 =>
        val v3 = v2.asInstanceOf[SequenceNode].children(2)
        val BindNode(v4, v5) = v3
        assert(v4.id == 87)
        val v6 = v2.asInstanceOf[SequenceNode].children(3)
        val BindNode(v7, v8) = v6
        assert(v7.id == 393)
        val BindNode(v9, v10) = v8
        val v19 = v9.id match {
          case 138 =>
            None
          case 394 =>
            val BindNode(v11, v12) = v10
            val v18 = v11.id match {
              case 395 =>
                val BindNode(v13, v14) = v12
                assert(v13.id == 396)
                val v15 = v14.asInstanceOf[SequenceNode].children(1)
                val BindNode(v16, v17) = v15
                assert(v16.id == 397)
                matchActionParams(v17)
            }
            Some(v18)
        }
        val v20 = v2.asInstanceOf[SequenceNode].children(7)
        val BindNode(v21, v22) = v20
        assert(v21.id == 399)
        ActionDef(matchSimpleName(v5), v19, matchActionExpr(v22))(v2)
    }
    v23
  }

  def matchActionExpr(node: Node): CallExpr = {
    val BindNode(v24, v25) = node
    val v29 = v24.id match {
      case 227 =>
        val v26 = v25.asInstanceOf[SequenceNode].children.head
        val BindNode(v27, v28) = v26
        assert(v27.id == 228)
        matchCallExpr(v28)
    }
    v29
  }

  def matchActionParams(node: Node): String = {
    val BindNode(v30, v31) = node
    val v35 = v30.id match {
      case 398 =>
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
      case 334 =>
        val v38 = v37.asInstanceOf[SequenceNode].children(4)
        val BindNode(v39, v40) = v38
        assert(v39.id == 87)
        val v41 = v37.asInstanceOf[SequenceNode].children(6)
        val BindNode(v42, v43) = v41
        assert(v42.id == 341)
        val v44 = v37.asInstanceOf[SequenceNode].children(10)
        val BindNode(v45, v46) = v44
        assert(v45.id == 363)
        ActionRuleDef(matchSimpleName(v40), matchParamsDef(v43), matchMethodRef(v46))(v37)
    }
    v47
  }

  def matchArgDef(node: Node): ArgDef = {
    val BindNode(v48, v49) = node
    val v94 = v48.id match {
      case 374 =>
        val v50 = v49.asInstanceOf[SequenceNode].children(1)
        val BindNode(v51, v52) = v50
        assert(v51.id == 379)
        val BindNode(v53, v54) = v52
        val v62 = v53.id match {
          case 138 =>
            None
          case 380 =>
            val BindNode(v55, v56) = v54
            assert(v55.id == 381)
            val BindNode(v57, v58) = v56
            assert(v57.id == 382)
            val v59 = v58.asInstanceOf[SequenceNode].children(1)
            val BindNode(v60, v61) = v59
            assert(v60.id == 123)
            Some(matchName(v61))
        }
        val v63 = v49.asInstanceOf[SequenceNode].children(3)
        val BindNode(v64, v65) = v63
        assert(v64.id == 87)
        val v66 = v49.asInstanceOf[SequenceNode].children(4)
        val BindNode(v67, v68) = v66
        assert(v67.id == 383)
        val BindNode(v69, v70) = v68
        val v79 = v69.id match {
          case 138 =>
            None
          case 384 =>
            val BindNode(v71, v72) = v70
            val v78 = v71.id match {
              case 385 =>
                val BindNode(v73, v74) = v72
                assert(v73.id == 386)
                val v75 = v74.asInstanceOf[SequenceNode].children(3)
                val BindNode(v76, v77) = v75
                assert(v76.id == 189)
                matchTypeExpr(v77)
            }
            Some(v78)
        }
        val v80 = v49.asInstanceOf[SequenceNode].children(5)
        val BindNode(v81, v82) = v80
        assert(v81.id == 354)
        val BindNode(v83, v84) = v82
        val v93 = v83.id match {
          case 138 =>
            None
          case 355 =>
            val BindNode(v85, v86) = v84
            val v92 = v85.id match {
              case 356 =>
                val BindNode(v87, v88) = v86
                assert(v87.id == 357)
                val v89 = v88.asInstanceOf[SequenceNode].children(3)
                val BindNode(v90, v91) = v89
                assert(v90.id == 169)
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
      case 282 =>
        val v97 = v96.asInstanceOf[SequenceNode].children.head
        val BindNode(v98, v99) = v97
        assert(v98.id == 283)
        val JoinNode(_, v100, _) = v99
        val BindNode(v101, v102) = v100
        assert(v101.id == 284)
        val BindNode(v103, v104) = v102
        val v109 = v103.id match {
          case 285 =>
            val BindNode(v105, v106) = v104
            assert(v105.id == 102)
            BooleanLiteral(true)(v106)
          case 286 =>
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
      case 389 =>
        val v113 = v112.asInstanceOf[SequenceNode].children(2)
        val BindNode(v114, v115) = v113
        assert(v114.id == 87)
        val v116 = v112.asInstanceOf[SequenceNode].children(4)
        val BindNode(v117, v118) = v116
        assert(v117.id == 341)
        val v119 = v112.asInstanceOf[SequenceNode].children(8)
        val BindNode(v120, v121) = v119
        assert(v120.id == 189)
        val v122 = v112.asInstanceOf[SequenceNode].children(12)
        val BindNode(v123, v124) = v122
        assert(v123.id == 363)
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
      case 229 =>
        val v134 = v133.asInstanceOf[SequenceNode].children.head
        val BindNode(v135, v136) = v134
        assert(v135.id == 123)
        val v137 = v133.asInstanceOf[SequenceNode].children(2)
        val BindNode(v138, v139) = v137
        assert(v138.id == 230)
        CallExpr(matchName(v136), matchCallParams(v139))(v133)
    }
    v140
  }

  def matchCallParams(node: Node): CallParams = {
    val BindNode(v141, v142) = node
    val v155 = v141.id match {
      case 231 =>
        CallParams(List(), List())(v142)
      case 232 =>
        val v143 = v142.asInstanceOf[SequenceNode].children(2)
        val BindNode(v144, v145) = v143
        assert(v144.id == 233)
        CallParams(matchPositionalParams(v145), List())(v142)
      case 244 =>
        val v146 = v142.asInstanceOf[SequenceNode].children(2)
        val BindNode(v147, v148) = v146
        assert(v147.id == 245)
        CallParams(List(), matchNamedParams(v148))(v142)
      case 255 =>
        val v149 = v142.asInstanceOf[SequenceNode].children(2)
        val BindNode(v150, v151) = v149
        assert(v150.id == 233)
        val v152 = v142.asInstanceOf[SequenceNode].children(6)
        val BindNode(v153, v154) = v152
        assert(v153.id == 245)
        CallParams(matchPositionalParams(v151), matchNamedParams(v154))(v142)
    }
    v155
  }

  def matchClassBody(node: Node): List[ClassBodyElem] = {
    val BindNode(v156, v157) = node
    val v170 = v156.id match {
      case 325 =>
        val v158 = v157.asInstanceOf[SequenceNode].children(1)
        val v159 = unrollRepeat0(v158).map { elem =>
          val BindNode(v160, v161) = elem
          assert(v160.id == 328)
          val BindNode(v162, v163) = v161
          val v169 = v162.id match {
            case 329 =>
              val BindNode(v164, v165) = v163
              assert(v164.id == 330)
              val v166 = v165.asInstanceOf[SequenceNode].children(1)
              val BindNode(v167, v168) = v166
              assert(v167.id == 331)
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
      case 332 =>
        val v173 = v172.asInstanceOf[SequenceNode].children.head
        val BindNode(v174, v175) = v173
        assert(v174.id == 333)
        matchActionRuleDef(v175)
      case 369 =>
        val v176 = v172.asInstanceOf[SequenceNode].children.head
        val BindNode(v177, v178) = v176
        assert(v177.id == 370)
        matchClassCastDef(v178)
    }
    v179
  }

  def matchClassCastDef(node: Node): ClassCastDef = {
    val BindNode(v180, v181) = node
    val v188 = v180.id match {
      case 371 =>
        val v182 = v181.asInstanceOf[SequenceNode].children(2)
        val BindNode(v183, v184) = v182
        assert(v183.id == 189)
        val v185 = v181.asInstanceOf[SequenceNode].children(6)
        val BindNode(v186, v187) = v185
        assert(v186.id == 169)
        ClassCastDef(matchTypeExpr(v184), matchExpr(v187))(v181)
    }
    v188
  }

  def matchClassDef(node: Node): ClassDef = {
    val BindNode(v189, v190) = node
    val v227 = v189.id match {
      case 297 =>
        val v191 = v190.asInstanceOf[SequenceNode].children(2)
        val BindNode(v192, v193) = v191
        assert(v192.id == 87)
        val v195 = v190.asInstanceOf[SequenceNode].children(3)
        val BindNode(v196, v197) = v195
        assert(v196.id == 301)
        val BindNode(v198, v199) = v197
        val v208 = v198.id match {
          case 138 =>
            None
          case 302 =>
            val BindNode(v200, v201) = v199
            val v207 = v200.id match {
              case 303 =>
                val BindNode(v202, v203) = v201
                assert(v202.id == 304)
                val v204 = v203.asInstanceOf[SequenceNode].children(1)
                val BindNode(v205, v206) = v204
                assert(v205.id == 305)
                matchClassExtendings(v206)
            }
            Some(v207)
        }
        val v194 = v208
        val v209 = v190.asInstanceOf[SequenceNode].children(5)
        val BindNode(v210, v211) = v209
        assert(v210.id == 318)
        val v213 = v190.asInstanceOf[SequenceNode].children(6)
        val BindNode(v214, v215) = v213
        assert(v214.id == 320)
        val BindNode(v216, v217) = v215
        val v226 = v216.id match {
          case 138 =>
            None
          case 321 =>
            val BindNode(v218, v219) = v217
            val v225 = v218.id match {
              case 322 =>
                val BindNode(v220, v221) = v219
                assert(v220.id == 323)
                val v222 = v221.asInstanceOf[SequenceNode].children(1)
                val BindNode(v223, v224) = v222
                assert(v223.id == 324)
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
      case 172 =>
        val v230 = v229.asInstanceOf[SequenceNode].children.head
        val BindNode(v231, v232) = v230
        assert(v231.id == 123)
        ClassExtending(matchName(v232))(v229)
    }
    v233
  }

  def matchClassExtendings(node: Node): List[ClassExtending] = {
    val BindNode(v234, v235) = node
    val v251 = v234.id match {
      case 306 =>
        val v236 = v235.asInstanceOf[SequenceNode].children(2)
        val BindNode(v237, v238) = v236
        assert(v237.id == 312)
        val v239 = v235.asInstanceOf[SequenceNode].children(3)
        val v240 = unrollRepeat0(v239).map { elem =>
          val BindNode(v241, v242) = elem
          assert(v241.id == 315)
          val BindNode(v243, v244) = v242
          val v250 = v243.id match {
            case 316 =>
              val BindNode(v245, v246) = v244
              assert(v245.id == 317)
              val v247 = v246.asInstanceOf[SequenceNode].children(3)
              val BindNode(v248, v249) = v247
              assert(v248.id == 312)
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
      case 319 =>
        val v254 = v253.asInstanceOf[SequenceNode].children(2)
        val BindNode(v255, v256) = v254
        assert(v255.id == 189)
        matchTypeExpr(v256)
    }
    v257
  }

  def matchCollectionType(node: Node): CollectionType = {
    val BindNode(v258, v259) = node
    val v278 = v258.id match {
      case 175 =>
        val v260 = v259.asInstanceOf[SequenceNode].children.head
        val BindNode(v261, v262) = v260
        assert(v261.id == 176)
        val JoinNode(_, v263, _) = v262
        val BindNode(v264, v265) = v263
        assert(v264.id == 177)
        val BindNode(v266, v267) = v265
        val v274 = v266.id match {
          case 178 =>
            val BindNode(v268, v269) = v267
            assert(v268.id == 179)
            val v270 = v269.asInstanceOf[SequenceNode].children.head
            "set"
          case 182 =>
            val BindNode(v271, v272) = v267
            assert(v271.id == 183)
            val v273 = v272.asInstanceOf[SequenceNode].children.head
            "list"
        }
        val v275 = v259.asInstanceOf[SequenceNode].children(2)
        val BindNode(v276, v277) = v275
        assert(v276.id == 186)
        CollectionType(v274, matchTypeParams(v277))(v259)
    }
    v278
  }

  def matchDef(node: Node): Def = {
    val BindNode(v279, v280) = node
    val v308 = v279.id match {
      case 295 =>
        val v281 = v280.asInstanceOf[SequenceNode].children.head
        val BindNode(v282, v283) = v281
        assert(v282.id == 296)
        matchClassDef(v283)
      case 115 =>
        val v284 = v280.asInstanceOf[SequenceNode].children.head
        val BindNode(v285, v286) = v284
        assert(v285.id == 116)
        matchImportDef(v286)
      case 390 =>
        val v287 = v280.asInstanceOf[SequenceNode].children.head
        val BindNode(v288, v289) = v287
        assert(v288.id == 391)
        matchActionDef(v289)
      case 57 =>
        val v290 = v280.asInstanceOf[SequenceNode].children.head
        val BindNode(v291, v292) = v290
        assert(v291.id == 58)
        matchNamespaceDef(v292)
      case 372 =>
        val v293 = v280.asInstanceOf[SequenceNode].children.head
        val BindNode(v294, v295) = v293
        assert(v294.id == 373)
        matchArgDef(v295)
      case 332 =>
        val v296 = v280.asInstanceOf[SequenceNode].children.head
        val BindNode(v297, v298) = v296
        assert(v297.id == 333)
        matchActionRuleDef(v298)
      case 293 =>
        val v299 = v280.asInstanceOf[SequenceNode].children.head
        val BindNode(v300, v301) = v299
        assert(v300.id == 294)
        matchNameDef(v301)
      case 387 =>
        val v302 = v280.asInstanceOf[SequenceNode].children.head
        val BindNode(v303, v304) = v302
        assert(v303.id == 388)
        matchBuildRuleDef(v304)
      case 400 =>
        val v305 = v280.asInstanceOf[SequenceNode].children.head
        val BindNode(v306, v307) = v305
        assert(v306.id == 401)
        matchEnumDef(v307)
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
          assert(v316.id == 413)
          val BindNode(v318, v319) = v317
          val v325 = v318.id match {
            case 414 =>
              val BindNode(v320, v321) = v319
              assert(v320.id == 415)
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
      case 402 =>
        val v329 = v328.asInstanceOf[SequenceNode].children(2)
        val BindNode(v330, v331) = v329
        assert(v330.id == 87)
        val v332 = v328.asInstanceOf[SequenceNode].children(6)
        val BindNode(v333, v334) = v332
        assert(v333.id == 87)
        val v335 = v328.asInstanceOf[SequenceNode].children(7)
        val v336 = unrollRepeat0(v335).map { elem =>
          val BindNode(v337, v338) = elem
          assert(v337.id == 408)
          val BindNode(v339, v340) = v338
          val v346 = v339.id match {
            case 409 =>
              val BindNode(v341, v342) = v340
              assert(v341.id == 410)
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
      case 157 =>
        val v350 = v349.asInstanceOf[SequenceNode].children(1)
        val BindNode(v351, v352) = v350
        assert(v351.id == 159)
        EscapeChar(v352.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char)(v349)
    }
    v353
  }

  def matchExpr(node: Node): Expr = {
    val BindNode(v354, v355) = node
    val v365 = v354.id match {
      case 170 =>
        val v356 = v355.asInstanceOf[SequenceNode].children.head
        val BindNode(v357, v358) = v356
        assert(v357.id == 169)
        val v359 = v355.asInstanceOf[SequenceNode].children(4)
        val BindNode(v360, v361) = v359
        assert(v360.id == 171)
        CastExpr(matchExpr(v358), matchNoUnionType(v361))(v355)
      case 222 =>
        val v362 = v355.asInstanceOf[SequenceNode].children.head
        val BindNode(v363, v364) = v362
        assert(v363.id == 223)
        matchMergeOpOrPrimary(v364)
    }
    v365
  }

  def matchImportDef(node: Node): ImportDef = {
    val BindNode(v366, v367) = node
    val v411 = v366.id match {
      case 117 =>
        val v368 = v367.asInstanceOf[SequenceNode].children(2)
        val BindNode(v369, v370) = v368
        assert(v369.id == 123)
        val v371 = v367.asInstanceOf[SequenceNode].children(3)
        val BindNode(v372, v373) = v371
        assert(v372.id == 131)
        val BindNode(v374, v375) = v373
        val v384 = v374.id match {
          case 138 =>
            None
          case 132 =>
            val BindNode(v376, v377) = v375
            val v383 = v376.id match {
              case 133 =>
                val BindNode(v378, v379) = v377
                assert(v378.id == 134)
                val v380 = v379.asInstanceOf[SequenceNode].children(3)
                val BindNode(v381, v382) = v380
                assert(v381.id == 87)
                matchSimpleName(v382)
            }
            Some(v383)
        }
        ImportName(matchName(v370), v384)(v367)
      case 139 =>
        val v385 = v367.asInstanceOf[SequenceNode].children(2)
        val BindNode(v386, v387) = v385
        assert(v386.id == 140)
        val v388 = v367.asInstanceOf[SequenceNode].children(6)
        val BindNode(v389, v390) = v388
        assert(v389.id == 87)
        ImportAll(matchImportSourceExpr(v387), matchSimpleName(v390))(v367)
      case 289 =>
        val v391 = v367.asInstanceOf[SequenceNode].children(2)
        val BindNode(v392, v393) = v391
        assert(v392.id == 140)
        val v394 = v367.asInstanceOf[SequenceNode].children(6)
        val BindNode(v395, v396) = v394
        assert(v395.id == 123)
        val v397 = v367.asInstanceOf[SequenceNode].children(7)
        val BindNode(v398, v399) = v397
        assert(v398.id == 131)
        val BindNode(v400, v401) = v399
        val v410 = v400.id match {
          case 138 =>
            None
          case 132 =>
            val BindNode(v402, v403) = v401
            val v409 = v402.id match {
              case 133 =>
                val BindNode(v404, v405) = v403
                assert(v404.id == 134)
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
      case 141 =>
        val v414 = v413.asInstanceOf[SequenceNode].children.head
        val BindNode(v415, v416) = v414
        assert(v415.id == 142)
        matchStringLiteral(v416)
      case 227 =>
        val v417 = v413.asInstanceOf[SequenceNode].children.head
        val BindNode(v418, v419) = v417
        assert(v418.id == 228)
        matchCallExpr(v419)
    }
    v420
  }

  def matchLiteral(node: Node): Literal = {
    val BindNode(v421, v422) = node
    val v429 = v421.id match {
      case 141 =>
        val v423 = v422.asInstanceOf[SequenceNode].children.head
        val BindNode(v424, v425) = v423
        assert(v424.id == 142)
        matchStringLiteral(v425)
      case 280 =>
        val v426 = v422.asInstanceOf[SequenceNode].children.head
        val BindNode(v427, v428) = v426
        assert(v427.id == 281)
        matchBooleanLiteral(v428)
    }
    v429
  }

  def matchMergeOpOrPrimary(node: Node): MergeOpOrPrimary = {
    val BindNode(v430, v431) = node
    val v441 = v430.id match {
      case 224 =>
        val v432 = v431.asInstanceOf[SequenceNode].children.head
        val BindNode(v433, v434) = v432
        assert(v433.id == 169)
        val v435 = v431.asInstanceOf[SequenceNode].children(4)
        val BindNode(v436, v437) = v435
        assert(v436.id == 226)
        MergeOp(matchExpr(v434), matchPrimary(v437))(v431)
      case 288 =>
        val v438 = v431.asInstanceOf[SequenceNode].children.head
        val BindNode(v439, v440) = v438
        assert(v439.id == 226)
        matchPrimary(v440)
    }
    v441
  }

  def matchMethodRef(node: Node): MethodRef = {
    val BindNode(v442, v443) = node
    val v464 = v442.id match {
      case 364 =>
        val v444 = v443.asInstanceOf[SequenceNode].children.head
        val BindNode(v445, v446) = v444
        assert(v445.id == 123)
        val v447 = v443.asInstanceOf[SequenceNode].children(4)
        val BindNode(v448, v449) = v447
        assert(v448.id == 123)
        val v450 = v443.asInstanceOf[SequenceNode].children(5)
        val BindNode(v451, v452) = v450
        assert(v451.id == 365)
        val BindNode(v453, v454) = v452
        val v463 = v453.id match {
          case 138 =>
            None
          case 366 =>
            val BindNode(v455, v456) = v454
            val v462 = v455.id match {
              case 367 =>
                val BindNode(v457, v458) = v456
                assert(v457.id == 368)
                val v459 = v458.asInstanceOf[SequenceNode].children(3)
                val BindNode(v460, v461) = v459
                assert(v460.id == 87)
                matchSimpleName(v461)
            }
            Some(v462)
        }
        MethodRef(matchName(v446), matchName(v449), v463)(v443)
    }
    v464
  }

  def matchName(node: Node): Name = {
    val BindNode(v465, v466) = node
    val v482 = v465.id match {
      case 124 =>
        val v467 = v466.asInstanceOf[SequenceNode].children.head
        val BindNode(v468, v469) = v467
        assert(v468.id == 87)
        val v470 = v466.asInstanceOf[SequenceNode].children(1)
        val v471 = unrollRepeat0(v470).map { elem =>
          val BindNode(v472, v473) = elem
          assert(v472.id == 127)
          val BindNode(v474, v475) = v473
          val v481 = v474.id match {
            case 128 =>
              val BindNode(v476, v477) = v475
              assert(v476.id == 129)
              val v478 = v477.asInstanceOf[SequenceNode].children(3)
              val BindNode(v479, v480) = v478
              assert(v479.id == 87)
              matchSimpleName(v480)
          }
          v481
        }
        Name(List(matchSimpleName(v469)) ++ v471)(v466)
    }
    v482
  }

  def matchNameDef(node: Node): NameDef = {
    val BindNode(v483, v484) = node
    val v491 = v483.id match {
      case 248 =>
        val v485 = v484.asInstanceOf[SequenceNode].children.head
        val BindNode(v486, v487) = v485
        assert(v486.id == 87)
        val v488 = v484.asInstanceOf[SequenceNode].children(4)
        val BindNode(v489, v490) = v488
        assert(v489.id == 169)
        NameDef(matchSimpleName(v487), matchExpr(v490))(v484)
    }
    v491
  }

  def matchNamedExpr(node: Node): NamedExpr = {
    val BindNode(v492, v493) = node
    val v500 = v492.id match {
      case 272 =>
        val v494 = v493.asInstanceOf[SequenceNode].children.head
        val BindNode(v495, v496) = v494
        assert(v495.id == 87)
        val v497 = v493.asInstanceOf[SequenceNode].children(4)
        val BindNode(v498, v499) = v497
        assert(v498.id == 169)
        NamedExpr(matchSimpleName(v496), matchExpr(v499))(v493)
    }
    v500
  }

  def matchNamedParam(node: Node): NamedParam = {
    val BindNode(v501, v502) = node
    val v509 = v501.id match {
      case 248 =>
        val v503 = v502.asInstanceOf[SequenceNode].children.head
        val BindNode(v504, v505) = v503
        assert(v504.id == 87)
        val v506 = v502.asInstanceOf[SequenceNode].children(4)
        val BindNode(v507, v508) = v506
        assert(v507.id == 169)
        NamedParam(matchSimpleName(v505), matchExpr(v508))(v502)
    }
    v509
  }

  def matchNamedParams(node: Node): List[NamedParam] = {
    val BindNode(v510, v511) = node
    val v527 = v510.id match {
      case 246 =>
        val v512 = v511.asInstanceOf[SequenceNode].children.head
        val BindNode(v513, v514) = v512
        assert(v513.id == 247)
        val v515 = v511.asInstanceOf[SequenceNode].children(1)
        val v516 = unrollRepeat0(v515).map { elem =>
          val BindNode(v517, v518) = elem
          assert(v517.id == 252)
          val BindNode(v519, v520) = v518
          val v526 = v519.id match {
            case 253 =>
              val BindNode(v521, v522) = v520
              assert(v521.id == 254)
              val v523 = v522.asInstanceOf[SequenceNode].children(3)
              val BindNode(v524, v525) = v523
              assert(v524.id == 247)
              matchNamedParam(v525)
          }
          v526
        }
        List(matchNamedParam(v514)) ++ v516
    }
    v527
  }

  def matchNamedTupleType(node: Node): NamedTupleType = {
    val BindNode(v528, v529) = node
    val v545 = v528.id match {
      case 213 =>
        val v530 = v529.asInstanceOf[SequenceNode].children(2)
        val BindNode(v531, v532) = v530
        assert(v531.id == 214)
        val v533 = v529.asInstanceOf[SequenceNode].children(3)
        val v534 = unrollRepeat0(v533).map { elem =>
          val BindNode(v535, v536) = elem
          assert(v535.id == 219)
          val BindNode(v537, v538) = v536
          val v544 = v537.id match {
            case 220 =>
              val BindNode(v539, v540) = v538
              assert(v539.id == 221)
              val v541 = v540.asInstanceOf[SequenceNode].children(3)
              val BindNode(v542, v543) = v541
              assert(v542.id == 214)
              matchNamedType(v543)
          }
          v544
        }
        NamedTupleType(List(matchNamedType(v532)) ++ v534)(v529)
    }
    v545
  }

  def matchNamedType(node: Node): NamedType = {
    val BindNode(v546, v547) = node
    val v554 = v546.id match {
      case 215 =>
        val v548 = v547.asInstanceOf[SequenceNode].children.head
        val BindNode(v549, v550) = v548
        assert(v549.id == 87)
        val v551 = v547.asInstanceOf[SequenceNode].children(4)
        val BindNode(v552, v553) = v551
        assert(v552.id == 189)
        NamedType(matchSimpleName(v550), matchTypeExpr(v553))(v547)
    }
    v554
  }

  def matchNamespaceDef(node: Node): NamespaceDef = {
    val BindNode(v555, v556) = node
    val v563 = v555.id match {
      case 59 =>
        val v557 = v556.asInstanceOf[SequenceNode].children(2)
        val BindNode(v558, v559) = v557
        assert(v558.id == 87)
        val v560 = v556.asInstanceOf[SequenceNode].children(5)
        val BindNode(v561, v562) = v560
        assert(v561.id == 2)
        NamespaceDef(matchSimpleName(v559), matchBuildScript(v562))(v556)
    }
    v563
  }

  def matchNoUnionType(node: Node): NoUnionType = {
    val BindNode(v564, v565) = node
    val v578 = v564.id match {
      case 172 =>
        val v566 = v565.asInstanceOf[SequenceNode].children.head
        val BindNode(v567, v568) = v566
        assert(v567.id == 123)
        matchName(v568)
      case 173 =>
        val v569 = v565.asInstanceOf[SequenceNode].children.head
        val BindNode(v570, v571) = v569
        assert(v570.id == 174)
        matchCollectionType(v571)
      case 206 =>
        val v572 = v565.asInstanceOf[SequenceNode].children.head
        val BindNode(v573, v574) = v572
        assert(v573.id == 207)
        matchTupleType(v574)
      case 211 =>
        val v575 = v565.asInstanceOf[SequenceNode].children.head
        val BindNode(v576, v577) = v575
        assert(v576.id == 212)
        matchNamedTupleType(v577)
    }
    v578
  }

  def matchParamDef(node: Node): ParamDef = {
    val BindNode(v579, v580) = node
    val v621 = v579.id match {
      case 348 =>
        val v581 = v580.asInstanceOf[SequenceNode].children.head
        val BindNode(v582, v583) = v581
        assert(v582.id == 87)
        val v584 = v580.asInstanceOf[SequenceNode].children(1)
        val BindNode(v585, v586) = v584
        assert(v585.id == 349)
        val BindNode(v587, v588) = v586
        val v597 = v587.id match {
          case 138 =>
            None
          case 350 =>
            val BindNode(v589, v590) = v588
            val v596 = v589.id match {
              case 351 =>
                val BindNode(v591, v592) = v590
                assert(v591.id == 352)
                val v593 = v592.asInstanceOf[SequenceNode].children(1)
                val BindNode(v594, v595) = v593
                assert(v594.id == 353)
                v595.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
            }
            Some(v596)
        }
        val v598 = v580.asInstanceOf[SequenceNode].children(5)
        val BindNode(v599, v600) = v598
        assert(v599.id == 189)
        val v601 = v580.asInstanceOf[SequenceNode].children(6)
        val BindNode(v602, v603) = v601
        assert(v602.id == 354)
        val BindNode(v604, v605) = v603
        val v614 = v604.id match {
          case 138 =>
            None
          case 355 =>
            val BindNode(v606, v607) = v605
            val v613 = v606.id match {
              case 356 =>
                val BindNode(v608, v609) = v607
                assert(v608.id == 357)
                val v610 = v609.asInstanceOf[SequenceNode].children(3)
                val BindNode(v611, v612) = v610
                assert(v611.id == 169)
                matchExpr(v612)
            }
            Some(v613)
        }
        ParamDef(matchSimpleName(v583), v597.isDefined, Some(matchTypeExpr(v600)), v614)(v580)
      case 248 =>
        val v615 = v580.asInstanceOf[SequenceNode].children.head
        val BindNode(v616, v617) = v615
        assert(v616.id == 87)
        val v618 = v580.asInstanceOf[SequenceNode].children(4)
        val BindNode(v619, v620) = v618
        assert(v619.id == 169)
        ParamDef(matchSimpleName(v617), false, None, Some(matchExpr(v620)))(v580)
    }
    v621
  }

  def matchParamsDef(node: Node): List[ParamDef] = {
    val BindNode(v622, v623) = node
    val v650 = v622.id match {
      case 342 =>
        val v625 = v623.asInstanceOf[SequenceNode].children(1)
        val BindNode(v626, v627) = v625
        assert(v626.id == 343)
        val BindNode(v628, v629) = v627
        val v649 = v628.id match {
          case 138 =>
            None
          case 344 =>
            val BindNode(v630, v631) = v629
            assert(v630.id == 345)
            val BindNode(v632, v633) = v631
            assert(v632.id == 346)
            val v634 = v633.asInstanceOf[SequenceNode].children(1)
            val BindNode(v635, v636) = v634
            assert(v635.id == 347)
            val v637 = v633.asInstanceOf[SequenceNode].children(2)
            val v638 = unrollRepeat0(v637).map { elem =>
              val BindNode(v639, v640) = elem
              assert(v639.id == 360)
              val BindNode(v641, v642) = v640
              val v648 = v641.id match {
                case 361 =>
                  val BindNode(v643, v644) = v642
                  assert(v643.id == 362)
                  val v645 = v644.asInstanceOf[SequenceNode].children(3)
                  val BindNode(v646, v647) = v645
                  assert(v646.id == 347)
                  matchParamDef(v647)
              }
              v648
            }
            Some(List(matchParamDef(v636)) ++ v638)
        }
        val v624 = v649
        if (v624.isDefined) v624.get else List()
    }
    v650
  }

  def matchPositionalParams(node: Node): List[Expr] = {
    val BindNode(v651, v652) = node
    val v668 = v651.id match {
      case 234 =>
        val v653 = v652.asInstanceOf[SequenceNode].children.head
        val BindNode(v654, v655) = v653
        assert(v654.id == 169)
        val v656 = v652.asInstanceOf[SequenceNode].children(1)
        val v657 = unrollRepeat0(v656).map { elem =>
          val BindNode(v658, v659) = elem
          assert(v658.id == 237)
          val BindNode(v660, v661) = v659
          val v667 = v660.id match {
            case 238 =>
              val BindNode(v662, v663) = v661
              assert(v662.id == 239)
              val v664 = v663.asInstanceOf[SequenceNode].children(3)
              val BindNode(v665, v666) = v664
              assert(v665.id == 169)
              matchExpr(v666)
          }
          v667
        }
        List(matchExpr(v655)) ++ v657
    }
    v668
  }

  def matchPrimary(node: Node): Primary = {
    val BindNode(v669, v670) = node
    val v771 = v669.id match {
      case 258 =>
        val v672 = v670.asInstanceOf[SequenceNode].children(1)
        val BindNode(v673, v674) = v672
        assert(v673.id == 260)
        val BindNode(v675, v676) = v674
        val v696 = v675.id match {
          case 138 =>
            None
          case 261 =>
            val BindNode(v677, v678) = v676
            assert(v677.id == 262)
            val BindNode(v679, v680) = v678
            assert(v679.id == 263)
            val v681 = v680.asInstanceOf[SequenceNode].children(1)
            val BindNode(v682, v683) = v681
            assert(v682.id == 169)
            val v684 = v680.asInstanceOf[SequenceNode].children(2)
            val v685 = unrollRepeat0(v684).map { elem =>
              val BindNode(v686, v687) = elem
              assert(v686.id == 237)
              val BindNode(v688, v689) = v687
              val v695 = v688.id match {
                case 238 =>
                  val BindNode(v690, v691) = v689
                  assert(v690.id == 239)
                  val v692 = v691.asInstanceOf[SequenceNode].children(3)
                  val BindNode(v693, v694) = v692
                  assert(v693.id == 169)
                  matchExpr(v694)
              }
              v695
            }
            Some(List(matchExpr(v683)) ++ v685)
        }
        val v671 = v696
        ListExpr(if (v671.isDefined) v671.get else List())(v670)
      case 227 =>
        val v697 = v670.asInstanceOf[SequenceNode].children.head
        val BindNode(v698, v699) = v697
        assert(v698.id == 228)
        matchCallExpr(v699)
      case 266 =>
        val v701 = v670.asInstanceOf[SequenceNode].children(1)
        val BindNode(v702, v703) = v701
        assert(v702.id == 267)
        val BindNode(v704, v705) = v703
        val v725 = v704.id match {
          case 138 =>
            None
          case 268 =>
            val BindNode(v706, v707) = v705
            assert(v706.id == 269)
            val BindNode(v708, v709) = v707
            assert(v708.id == 270)
            val v710 = v709.asInstanceOf[SequenceNode].children(1)
            val BindNode(v711, v712) = v710
            assert(v711.id == 271)
            val v713 = v709.asInstanceOf[SequenceNode].children(2)
            val v714 = unrollRepeat0(v713).map { elem =>
              val BindNode(v715, v716) = elem
              assert(v715.id == 275)
              val BindNode(v717, v718) = v716
              val v724 = v717.id match {
                case 276 =>
                  val BindNode(v719, v720) = v718
                  assert(v719.id == 277)
                  val v721 = v720.asInstanceOf[SequenceNode].children(3)
                  val BindNode(v722, v723) = v721
                  assert(v722.id == 271)
                  matchNamedExpr(v723)
              }
              v724
            }
            Some(List(matchNamedExpr(v712)) ++ v714)
        }
        val v700 = v725
        NamedTupleExpr(if (v700.isDefined) v700.get else List())(v670)
      case 278 =>
        val v726 = v670.asInstanceOf[SequenceNode].children.head
        val BindNode(v727, v728) = v726
        assert(v727.id == 279)
        matchLiteral(v728)
      case 256 =>
        val v729 = v670.asInstanceOf[SequenceNode].children.head
        val BindNode(v730, v731) = v729
        assert(v730.id == 226)
        val v732 = v670.asInstanceOf[SequenceNode].children(4)
        val BindNode(v733, v734) = v732
        assert(v733.id == 87)
        MemberAccess(matchPrimary(v731), matchSimpleName(v734))(v670)
      case 287 =>
        val v735 = v670.asInstanceOf[SequenceNode].children(2)
        val BindNode(v736, v737) = v735
        assert(v736.id == 169)
        Paren(matchExpr(v737))(v670)
      case 265 =>
        val v738 = v670.asInstanceOf[SequenceNode].children(2)
        val BindNode(v739, v740) = v738
        assert(v739.id == 169)
        val v742 = v670.asInstanceOf[SequenceNode].children(5)
        val BindNode(v743, v744) = v742
        assert(v743.id == 260)
        val BindNode(v745, v746) = v744
        val v767 = v745.id match {
          case 138 =>
            None
          case 261 =>
            val BindNode(v747, v748) = v746
            val v766 = v747.id match {
              case 262 =>
                val BindNode(v749, v750) = v748
                assert(v749.id == 263)
                val v751 = v750.asInstanceOf[SequenceNode].children(1)
                val BindNode(v752, v753) = v751
                assert(v752.id == 169)
                val v754 = v750.asInstanceOf[SequenceNode].children(2)
                val v755 = unrollRepeat0(v754).map { elem =>
                  val BindNode(v756, v757) = elem
                  assert(v756.id == 237)
                  val BindNode(v758, v759) = v757
                  val v765 = v758.id match {
                    case 238 =>
                      val BindNode(v760, v761) = v759
                      assert(v760.id == 239)
                      val v762 = v761.asInstanceOf[SequenceNode].children(3)
                      val BindNode(v763, v764) = v762
                      assert(v763.id == 169)
                      matchExpr(v764)
                  }
                  v765
                }
                List(matchExpr(v753)) ++ v755
            }
            Some(v766)
        }
        val v741 = v767
        TupleExpr(List(matchExpr(v740)) ++ (if (v741.isDefined) v741.get else List()))(v670)
      case 257 =>
        val v768 = v670.asInstanceOf[SequenceNode].children.head
        val BindNode(v769, v770) = v768
        assert(v769.id == 87)
        NameRef(matchSimpleName(v770))(v670)
    }
    v771
  }

  def matchSimpleName(node: Node): String = {
    val BindNode(v772, v773) = node
    val v804 = v772.id match {
      case 88 =>
        val v774 = v773.asInstanceOf[SequenceNode].children.head
        val BindNode(v775, v776) = v774
        assert(v775.id == 89)
        val BindNode(v777, v778) = v776
        assert(v777.id == 90)
        val BindNode(v779, v780) = v778
        assert(v779.id == 91)
        val BindNode(v781, v782) = v780
        val v803 = v781.id match {
          case 92 =>
            val BindNode(v783, v784) = v782
            assert(v783.id == 93)
            val v785 = v784.asInstanceOf[SequenceNode].children.head
            val BindNode(v786, v787) = v785
            assert(v786.id == 94)
            val JoinNode(_, v788, _) = v787
            val BindNode(v789, v790) = v788
            assert(v789.id == 95)
            val BindNode(v791, v792) = v790
            val v802 = v791.id match {
              case 96 =>
                val BindNode(v793, v794) = v792
                assert(v793.id == 97)
                val v795 = v794.asInstanceOf[SequenceNode].children.head
                val BindNode(v796, v797) = v795
                assert(v796.id == 98)
                val v798 = v794.asInstanceOf[SequenceNode].children(1)
                val v799 = unrollRepeat0(v798).map { elem =>
                  val BindNode(v800, v801) = elem
                  assert(v800.id == 77)
                  v801.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
                }
                v797.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char.toString + v799.map(x => x.toString).mkString("")
            }
            v802
        }
        v803
    }
    v804
  }

  def matchStringElem(node: Node): StringElem = {
    val BindNode(v805, v806) = node
    val v818 = v805.id match {
      case 152 =>
        val v807 = v806.asInstanceOf[SequenceNode].children.head
        val BindNode(v808, v809) = v807
        assert(v808.id == 153)
        val BindNode(v810, v811) = v809
        assert(v810.id == 30)
        JustChar(v811.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char)(v806)
      case 155 =>
        val v812 = v806.asInstanceOf[SequenceNode].children.head
        val BindNode(v813, v814) = v812
        assert(v813.id == 156)
        matchEscapeChar(v814)
      case 160 =>
        val v815 = v806.asInstanceOf[SequenceNode].children.head
        val BindNode(v816, v817) = v815
        assert(v816.id == 161)
        matchStringExpr(v817)
    }
    v818
  }

  def matchStringExpr(node: Node): StringExpr = {
    val BindNode(v819, v820) = node
    val v837 = v819.id match {
      case 162 =>
        val v821 = v820.asInstanceOf[SequenceNode].children.head
        val BindNode(v822, v823) = v821
        assert(v822.id == 163)
        val BindNode(v824, v825) = v823
        assert(v824.id == 164)
        val BindNode(v826, v827) = v825
        val v833 = v826.id match {
          case 165 =>
            val BindNode(v828, v829) = v827
            assert(v828.id == 166)
            val v830 = v829.asInstanceOf[SequenceNode].children(1)
            val BindNode(v831, v832) = v830
            assert(v831.id == 87)
            matchSimpleName(v832)
        }
        SimpleExpr(v833)(v820)
      case 168 =>
        val v834 = v820.asInstanceOf[SequenceNode].children(3)
        val BindNode(v835, v836) = v834
        assert(v835.id == 169)
        ComplexExpr(matchExpr(v836))(v820)
    }
    v837
  }

  def matchStringLiteral(node: Node): StringLiteral = {
    val BindNode(v838, v839) = node
    val v854 = v838.id match {
      case 143 =>
        val v840 = v839.asInstanceOf[SequenceNode].children(1)
        val v841 = unrollRepeat0(v840).map { elem =>
          val BindNode(v842, v843) = elem
          assert(v842.id == 147)
          val BindNode(v844, v845) = v843
          assert(v844.id == 148)
          val BindNode(v846, v847) = v845
          val v853 = v846.id match {
            case 149 =>
              val BindNode(v848, v849) = v847
              assert(v848.id == 150)
              val v850 = v849.asInstanceOf[SequenceNode].children.head
              val BindNode(v851, v852) = v850
              assert(v851.id == 151)
              matchStringElem(v852)
          }
          v853
        }
        StringLiteral(v841)(v839)
    }
    v854
  }

  def matchTupleType(node: Node): TupleType = {
    val BindNode(v855, v856) = node
    val v872 = v855.id match {
      case 208 =>
        val v857 = v856.asInstanceOf[SequenceNode].children(2)
        val BindNode(v858, v859) = v857
        assert(v858.id == 189)
        val v860 = v856.asInstanceOf[SequenceNode].children(3)
        val v861 = unrollRepeat0(v860).map { elem =>
          val BindNode(v862, v863) = elem
          assert(v862.id == 202)
          val BindNode(v864, v865) = v863
          val v871 = v864.id match {
            case 203 =>
              val BindNode(v866, v867) = v865
              assert(v866.id == 204)
              val v868 = v867.asInstanceOf[SequenceNode].children(3)
              val BindNode(v869, v870) = v868
              assert(v869.id == 189)
              matchTypeExpr(v870)
          }
          v871
        }
        TupleType(List(matchTypeExpr(v859)) ++ v861)(v856)
    }
    v872
  }

  def matchTypeExpr(node: Node): TypeExpr = {
    val BindNode(v873, v874) = node
    val v881 = v873.id match {
      case 190 =>
        val v875 = v874.asInstanceOf[SequenceNode].children.head
        val BindNode(v876, v877) = v875
        assert(v876.id == 171)
        matchNoUnionType(v877)
      case 191 =>
        val v878 = v874.asInstanceOf[SequenceNode].children.head
        val BindNode(v879, v880) = v878
        assert(v879.id == 192)
        matchUnionType(v880)
    }
    v881
  }

  def matchTypeParams(node: Node): TypeParams = {
    val BindNode(v882, v883) = node
    val v899 = v882.id match {
      case 187 =>
        val v884 = v883.asInstanceOf[SequenceNode].children(2)
        val BindNode(v885, v886) = v884
        assert(v885.id == 189)
        val v887 = v883.asInstanceOf[SequenceNode].children(3)
        val v888 = unrollRepeat0(v887).map { elem =>
          val BindNode(v889, v890) = elem
          assert(v889.id == 202)
          val BindNode(v891, v892) = v890
          val v898 = v891.id match {
            case 203 =>
              val BindNode(v893, v894) = v892
              assert(v893.id == 204)
              val v895 = v894.asInstanceOf[SequenceNode].children(3)
              val BindNode(v896, v897) = v895
              assert(v896.id == 189)
              matchTypeExpr(v897)
          }
          v898
        }
        TypeParams(List(matchTypeExpr(v886)) ++ v888)(v883)
    }
    v899
  }

  def matchUnionType(node: Node): UnionType = {
    val BindNode(v900, v901) = node
    val v917 = v900.id match {
      case 193 =>
        val v902 = v901.asInstanceOf[SequenceNode].children(2)
        val BindNode(v903, v904) = v902
        assert(v903.id == 171)
        val v905 = v901.asInstanceOf[SequenceNode].children(3)
        val v906 = unrollRepeat0(v905).map { elem =>
          val BindNode(v907, v908) = elem
          assert(v907.id == 196)
          val BindNode(v909, v910) = v908
          val v916 = v909.id match {
            case 197 =>
              val BindNode(v911, v912) = v910
              assert(v911.id == 198)
              val v913 = v912.asInstanceOf[SequenceNode].children(3)
              val BindNode(v914, v915) = v913
              assert(v914.id == 171)
              matchNoUnionType(v915)
          }
          v916
        }
        UnionType(List(matchNoUnionType(v904)) ++ v906)(v901)
    }
    v917
  }

  def matchStart(node: Node): BuildScript = {
    val BindNode(start, BindNode(_, body)) = node
    assert(start.id == 1)
    matchBuildScript(body)
  }

  val milestoneParserData = MilestoneParserProtobufConverter.convertProtoToMilestoneParserData(
    MilestoneParserDataProto.MilestoneParserData.parseFrom(getClass().getResourceAsStream("/parserdata.pb")))

  val milestoneParser = new MilestoneParser(milestoneParserData)

  def parse(text: String): Either[ParseForest, ParsingErrors.ParsingError] =
    milestoneParser.parseAndReconstructToForest(text)

  def parseAst(text: String): Either[BuildScript, ParsingErrors.ParsingError] =
    parse(text) match {
      case Left(forest) => Left(matchStart(forest.trees.head))
      case Right(error) => Right(error)
    }
}
