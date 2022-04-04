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

  sealed trait WithParseNode { val parseNode: Node }
  case class ActionDef(name: String, argsName: Option[String], expr: CallExpr)(override val parseNode: Node) extends Def with WithParseNode
  case class ActionRuleDef(name: String, params: List[ParamDef], impl: MethodRef)(override val parseNode: Node) extends ClassBodyElem with Def with WithParseNode
  case class ArgDef(replacing: Option[Name], name: String, typ: Option[TypeExpr], defaultValue: Option[Expr])(override val parseNode: Node) extends Def with WithParseNode
  case class BooleanLiteral(value: Boolean)(override val parseNode: Node) extends Literal with WithParseNode
  case class BuildRuleDef(name: String, params: List[ParamDef], returnType: TypeExpr, impl: MethodRef)(override val parseNode: Node) extends Def with WithParseNode
  case class BuildScript(defs: List[Def])(override val parseNode: Node) extends WithParseNode
  case class CallExpr(name: Name, params: CallParams)(override val parseNode: Node) extends ImportSourceExpr with Primary with WithParseNode
  case class CallParams(posParams: List[Expr], namedParams: List[NamedParam])(override val parseNode: Node) extends WithParseNode
  sealed trait ClassBodyElem extends WithParseNode
  case class ClassCastDef(castTo: Name, expr: Expr)(override val parseNode: Node) extends ClassBodyElem with WithParseNode
  case class ClassDef(name: String, typeParams: List[NamedTypeParam], extendings: List[ClassExtending], reality: TypeExpr, body: List[ClassBodyElem])(override val parseNode: Node) extends Def with WithParseNode
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
  case class MergeOp(lhs: Expr, rhs: Primary)(override val parseNode: Node) extends Expr with WithParseNode
  case class MethodRef(targetName: Name, className: Name, methodName: Option[String])(override val parseNode: Node) extends WithParseNode
  case class Name(tokens: List[String])(override val parseNode: Node) extends NoUnionType with WithParseNode
  case class NameDef(name: String, value: Expr)(override val parseNode: Node) extends Def with WithParseNode
  case class NameRef(name: String)(override val parseNode: Node) extends Primary with WithParseNode
  case class NamedExpr(name: String, expr: Expr)(override val parseNode: Node) extends WithParseNode
  case class NamedParam(name: String, value: Expr)(override val parseNode: Node) extends WithParseNode
  case class NamedTupleExpr(elems: List[NamedExpr])(override val parseNode: Node) extends Primary with WithParseNode
  case class NamedTupleType(elems: List[NamedType])(override val parseNode: Node) extends NoUnionType with WithParseNode
  case class NamedType(name: String, typ: TypeExpr)(override val parseNode: Node) extends WithParseNode
  case class NamedTypeParam(name: String, typ: TypeExpr)(override val parseNode: Node) extends WithParseNode
  case class NamespaceDef(name: String, body: BuildScript)(override val parseNode: Node) extends Def with WithParseNode
  sealed trait NoUnionType extends TypeExpr with WithParseNode
  case class ParamDef(name: String, optional: Boolean, typ: Option[TypeExpr], defaultValue: Option[Expr])(override val parseNode: Node) extends WithParseNode
  sealed trait Primary extends Expr with WithParseNode
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
      case 400 =>
        val v3 = v2.asInstanceOf[SequenceNode].children(2)
        val BindNode(v4, v5) = v3
        assert(v4.id == 87)
        val v6 = v2.asInstanceOf[SequenceNode].children(3)
        val BindNode(v7, v8) = v6
        assert(v7.id == 401)
        val BindNode(v9, v10) = v8
        val v19 = v9.id match {
        case 138 =>
        None
        case 402 =>
          val BindNode(v11, v12) = v10
          val v18 = v11.id match {
          case 403 =>
            val BindNode(v13, v14) = v12
            assert(v13.id == 404)
            val v15 = v14.asInstanceOf[SequenceNode].children(1)
            val BindNode(v16, v17) = v15
            assert(v16.id == 405)
            matchActionParams(v17)
        }
          Some(v18)
      }
        val v20 = v2.asInstanceOf[SequenceNode].children(7)
        val BindNode(v21, v22) = v20
        assert(v21.id == 407)
        ActionDef(matchSimpleName(v5), v19, matchActionExpr(v22))(v2)
    }
    v23
  }

  def matchActionExpr(node: Node): CallExpr = {
    val BindNode(v24, v25) = node
    val v29 = v24.id match {
      case 173 =>
        val v26 = v25.asInstanceOf[SequenceNode].children.head
        val BindNode(v27, v28) = v26
        assert(v27.id == 174)
        matchCallExpr(v28)
    }
    v29
  }

  def matchActionParams(node: Node): String = {
    val BindNode(v30, v31) = node
    val v35 = v30.id match {
      case 406 =>
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
      case 342 =>
        val v38 = v37.asInstanceOf[SequenceNode].children(4)
        val BindNode(v39, v40) = v38
        assert(v39.id == 87)
        val v41 = v37.asInstanceOf[SequenceNode].children(6)
        val BindNode(v42, v43) = v41
        assert(v42.id == 349)
        val v44 = v37.asInstanceOf[SequenceNode].children(10)
        val BindNode(v45, v46) = v44
        assert(v45.id == 371)
        ActionRuleDef(matchSimpleName(v40), matchParamsDef(v43), matchMethodRef(v46))(v37)
    }
    v47
  }

  def matchArgDef(node: Node): ArgDef = {
    val BindNode(v48, v49) = node
    val v94 = v48.id match {
      case 382 =>
        val v50 = v49.asInstanceOf[SequenceNode].children(1)
        val BindNode(v51, v52) = v50
        assert(v51.id == 387)
        val BindNode(v53, v54) = v52
        val v62 = v53.id match {
        case 138 =>
        None
        case 388 =>
          val BindNode(v55, v56) = v54
          assert(v55.id == 389)
          val BindNode(v57, v58) = v56
          assert(v57.id == 390)
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
        assert(v67.id == 391)
        val BindNode(v69, v70) = v68
        val v79 = v69.id match {
        case 138 =>
        None
        case 392 =>
          val BindNode(v71, v72) = v70
          val v78 = v71.id match {
          case 393 =>
            val BindNode(v73, v74) = v72
            assert(v73.id == 394)
            val v75 = v74.asInstanceOf[SequenceNode].children(3)
            val BindNode(v76, v77) = v75
            assert(v76.id == 258)
            matchTypeExpr(v77)
        }
          Some(v78)
      }
        val v80 = v49.asInstanceOf[SequenceNode].children(5)
        val BindNode(v81, v82) = v80
        assert(v81.id == 362)
        val BindNode(v83, v84) = v82
        val v93 = v83.id match {
        case 138 =>
        None
        case 363 =>
          val BindNode(v85, v86) = v84
          val v92 = v85.id match {
          case 364 =>
            val BindNode(v87, v88) = v86
            assert(v87.id == 365)
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
      case 232 =>
        val v97 = v96.asInstanceOf[SequenceNode].children.head
        val BindNode(v98, v99) = v97
        assert(v98.id == 233)
        val JoinNode(_, v100, _) = v99
        val BindNode(v101, v102) = v100
        assert(v101.id == 234)
        val BindNode(v103, v104) = v102
        val v109 = v103.id match {
        case 235 =>
          val BindNode(v105, v106) = v104
          assert(v105.id == 102)
          BooleanLiteral(true)(v106)
        case 236 =>
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
      case 397 =>
        val v113 = v112.asInstanceOf[SequenceNode].children(2)
        val BindNode(v114, v115) = v113
        assert(v114.id == 87)
        val v116 = v112.asInstanceOf[SequenceNode].children(4)
        val BindNode(v117, v118) = v116
        assert(v117.id == 349)
        val v119 = v112.asInstanceOf[SequenceNode].children(8)
        val BindNode(v120, v121) = v119
        assert(v120.id == 258)
        val v122 = v112.asInstanceOf[SequenceNode].children(12)
        val BindNode(v123, v124) = v122
        assert(v123.id == 371)
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
      case 175 =>
        val v134 = v133.asInstanceOf[SequenceNode].children.head
        val BindNode(v135, v136) = v134
        assert(v135.id == 123)
        val v137 = v133.asInstanceOf[SequenceNode].children(2)
        val BindNode(v138, v139) = v137
        assert(v138.id == 176)
        CallExpr(matchName(v136), matchCallParams(v139))(v133)
    }
    v140
  }

  def matchCallParams(node: Node): CallParams = {
    val BindNode(v141, v142) = node
    val v155 = v141.id match {
      case 177 =>
      CallParams(List(), List())(v142)
      case 180 =>
        val v143 = v142.asInstanceOf[SequenceNode].children(2)
        val BindNode(v144, v145) = v143
        assert(v144.id == 181)
        CallParams(matchPositionalParams(v145), List())(v142)
      case 193 =>
        val v146 = v142.asInstanceOf[SequenceNode].children(2)
        val BindNode(v147, v148) = v146
        assert(v147.id == 194)
        CallParams(List(), matchNamedParams(v148))(v142)
      case 204 =>
        val v149 = v142.asInstanceOf[SequenceNode].children(2)
        val BindNode(v150, v151) = v149
        assert(v150.id == 181)
        val v152 = v142.asInstanceOf[SequenceNode].children(6)
        val BindNode(v153, v154) = v152
        assert(v153.id == 194)
        CallParams(matchPositionalParams(v151), matchNamedParams(v154))(v142)
    }
    v155
  }

  def matchClassBody(node: Node): List[ClassBodyElem] = {
    val BindNode(v156, v157) = node
    val v170 = v156.id match {
      case 333 =>
        val v158 = v157.asInstanceOf[SequenceNode].children(1)
        val v159 = unrollRepeat0(v158).map { elem =>
        val BindNode(v160, v161) = elem
        assert(v160.id == 336)
        val BindNode(v162, v163) = v161
        val v169 = v162.id match {
        case 337 =>
          val BindNode(v164, v165) = v163
          assert(v164.id == 338)
          val v166 = v165.asInstanceOf[SequenceNode].children(1)
          val BindNode(v167, v168) = v166
          assert(v167.id == 339)
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
      case 340 =>
        val v173 = v172.asInstanceOf[SequenceNode].children.head
        val BindNode(v174, v175) = v173
        assert(v174.id == 341)
        matchActionRuleDef(v175)
      case 377 =>
        val v176 = v172.asInstanceOf[SequenceNode].children.head
        val BindNode(v177, v178) = v176
        assert(v177.id == 378)
        matchClassCastDef(v178)
    }
    v179
  }

  def matchClassCastDef(node: Node): ClassCastDef = {
    val BindNode(v180, v181) = node
    val v188 = v180.id match {
      case 379 =>
        val v182 = v181.asInstanceOf[SequenceNode].children(2)
        val BindNode(v183, v184) = v182
        assert(v183.id == 123)
        val v185 = v181.asInstanceOf[SequenceNode].children(6)
        val BindNode(v186, v187) = v185
        assert(v186.id == 169)
        ClassCastDef(matchName(v184), matchExpr(v187))(v181)
    }
    v188
  }

  def matchClassDef(node: Node): ClassDef = {
    val BindNode(v189, v190) = node
    val v242 = v189.id match {
      case 246 =>
        val v191 = v190.asInstanceOf[SequenceNode].children(2)
        val BindNode(v192, v193) = v191
        assert(v192.id == 87)
        val v195 = v190.asInstanceOf[SequenceNode].children(3)
        val BindNode(v196, v197) = v195
        assert(v196.id == 250)
        val BindNode(v198, v199) = v197
        val v208 = v198.id match {
        case 138 =>
        None
        case 251 =>
          val BindNode(v200, v201) = v199
          val v207 = v200.id match {
          case 252 =>
            val BindNode(v202, v203) = v201
            assert(v202.id == 253)
            val v204 = v203.asInstanceOf[SequenceNode].children(1)
            val BindNode(v205, v206) = v204
            assert(v205.id == 254)
            matchNamedTypeParams(v206)
        }
          Some(v207)
      }
        val v194 = v208
        val v210 = v190.asInstanceOf[SequenceNode].children(4)
        val BindNode(v211, v212) = v210
        assert(v211.id == 309)
        val BindNode(v213, v214) = v212
        val v223 = v213.id match {
        case 138 =>
        None
        case 310 =>
          val BindNode(v215, v216) = v214
          val v222 = v215.id match {
          case 311 =>
            val BindNode(v217, v218) = v216
            assert(v217.id == 312)
            val v219 = v218.asInstanceOf[SequenceNode].children(1)
            val BindNode(v220, v221) = v219
            assert(v220.id == 313)
            matchClassExtendings(v221)
        }
          Some(v222)
      }
        val v209 = v223
        val v224 = v190.asInstanceOf[SequenceNode].children(6)
        val BindNode(v225, v226) = v224
        assert(v225.id == 326)
        val v228 = v190.asInstanceOf[SequenceNode].children(7)
        val BindNode(v229, v230) = v228
        assert(v229.id == 328)
        val BindNode(v231, v232) = v230
        val v241 = v231.id match {
        case 138 =>
        None
        case 329 =>
          val BindNode(v233, v234) = v232
          val v240 = v233.id match {
          case 330 =>
            val BindNode(v235, v236) = v234
            assert(v235.id == 331)
            val v237 = v236.asInstanceOf[SequenceNode].children(1)
            val BindNode(v238, v239) = v237
            assert(v238.id == 332)
            matchClassBody(v239)
        }
          Some(v240)
      }
        val v227 = v241
        ClassDef(matchSimpleName(v193), if (v194.isDefined) v194.get else List(), if (v209.isDefined) v209.get else List(), matchClassRealityDef(v226), if (v227.isDefined) v227.get else List())(v190)
    }
    v242
  }

  def matchClassExtending(node: Node): ClassExtending = {
    val BindNode(v243, v244) = node
    val v248 = v243.id match {
      case 261 =>
        val v245 = v244.asInstanceOf[SequenceNode].children.head
        val BindNode(v246, v247) = v245
        assert(v246.id == 123)
        ClassExtending(matchName(v247))(v244)
    }
    v248
  }

  def matchClassExtendings(node: Node): List[ClassExtending] = {
    val BindNode(v249, v250) = node
    val v266 = v249.id match {
      case 314 =>
        val v251 = v250.asInstanceOf[SequenceNode].children(2)
        val BindNode(v252, v253) = v251
        assert(v252.id == 320)
        val v254 = v250.asInstanceOf[SequenceNode].children(3)
        val v255 = unrollRepeat0(v254).map { elem =>
        val BindNode(v256, v257) = elem
        assert(v256.id == 323)
        val BindNode(v258, v259) = v257
        val v265 = v258.id match {
        case 324 =>
          val BindNode(v260, v261) = v259
          assert(v260.id == 325)
          val v262 = v261.asInstanceOf[SequenceNode].children(3)
          val BindNode(v263, v264) = v262
          assert(v263.id == 320)
          matchClassExtending(v264)
      }
        v265
        }
        List(matchClassExtending(v253)) ++ v255
    }
    v266
  }

  def matchClassRealityDef(node: Node): TypeExpr = {
    val BindNode(v267, v268) = node
    val v272 = v267.id match {
      case 327 =>
        val v269 = v268.asInstanceOf[SequenceNode].children(2)
        val BindNode(v270, v271) = v269
        assert(v270.id == 258)
        matchTypeExpr(v271)
    }
    v272
  }

  def matchCollectionType(node: Node): CollectionType = {
    val BindNode(v273, v274) = node
    val v293 = v273.id match {
      case 264 =>
        val v275 = v274.asInstanceOf[SequenceNode].children.head
        val BindNode(v276, v277) = v275
        assert(v276.id == 265)
        val JoinNode(_, v278, _) = v277
        val BindNode(v279, v280) = v278
        assert(v279.id == 266)
        val BindNode(v281, v282) = v280
        val v289 = v281.id match {
        case 267 =>
          val BindNode(v283, v284) = v282
          assert(v283.id == 268)
          val v285 = v284.asInstanceOf[SequenceNode].children.head
          "set"
        case 271 =>
          val BindNode(v286, v287) = v282
          assert(v286.id == 272)
          val v288 = v287.asInstanceOf[SequenceNode].children.head
          "list"
      }
        val v290 = v274.asInstanceOf[SequenceNode].children(2)
        val BindNode(v291, v292) = v290
        assert(v291.id == 275)
        CollectionType(v289, matchTypeParams(v292))(v274)
    }
    v293
  }

  def matchDef(node: Node): Def = {
    val BindNode(v294, v295) = node
    val v323 = v294.id match {
      case 244 =>
        val v296 = v295.asInstanceOf[SequenceNode].children.head
        val BindNode(v297, v298) = v296
        assert(v297.id == 245)
        matchClassDef(v298)
      case 115 =>
        val v299 = v295.asInstanceOf[SequenceNode].children.head
        val BindNode(v300, v301) = v299
        assert(v300.id == 116)
        matchImportDef(v301)
      case 398 =>
        val v302 = v295.asInstanceOf[SequenceNode].children.head
        val BindNode(v303, v304) = v302
        assert(v303.id == 399)
        matchActionDef(v304)
      case 57 =>
        val v305 = v295.asInstanceOf[SequenceNode].children.head
        val BindNode(v306, v307) = v305
        assert(v306.id == 58)
        matchNamespaceDef(v307)
      case 380 =>
        val v308 = v295.asInstanceOf[SequenceNode].children.head
        val BindNode(v309, v310) = v308
        assert(v309.id == 381)
        matchArgDef(v310)
      case 340 =>
        val v311 = v295.asInstanceOf[SequenceNode].children.head
        val BindNode(v312, v313) = v311
        assert(v312.id == 341)
        matchActionRuleDef(v313)
      case 242 =>
        val v314 = v295.asInstanceOf[SequenceNode].children.head
        val BindNode(v315, v316) = v314
        assert(v315.id == 243)
        matchNameDef(v316)
      case 395 =>
        val v317 = v295.asInstanceOf[SequenceNode].children.head
        val BindNode(v318, v319) = v317
        assert(v318.id == 396)
        matchBuildRuleDef(v319)
      case 408 =>
        val v320 = v295.asInstanceOf[SequenceNode].children.head
        val BindNode(v321, v322) = v320
        assert(v321.id == 409)
        matchEnumDef(v322)
    }
    v323
  }

  def matchDefs(node: Node): List[Def] = {
    val BindNode(v324, v325) = node
    val v341 = v324.id match {
      case 55 =>
        val v326 = v325.asInstanceOf[SequenceNode].children.head
        val BindNode(v327, v328) = v326
        assert(v327.id == 56)
        val v329 = v325.asInstanceOf[SequenceNode].children(1)
        val v330 = unrollRepeat0(v329).map { elem =>
        val BindNode(v331, v332) = elem
        assert(v331.id == 421)
        val BindNode(v333, v334) = v332
        val v340 = v333.id match {
        case 422 =>
          val BindNode(v335, v336) = v334
          assert(v335.id == 423)
          val v337 = v336.asInstanceOf[SequenceNode].children(1)
          val BindNode(v338, v339) = v337
          assert(v338.id == 56)
          matchDef(v339)
      }
        v340
        }
        List(matchDef(v328)) ++ v330
    }
    v341
  }

  def matchEnumDef(node: Node): EnumDef = {
    val BindNode(v342, v343) = node
    val v362 = v342.id match {
      case 410 =>
        val v344 = v343.asInstanceOf[SequenceNode].children(2)
        val BindNode(v345, v346) = v344
        assert(v345.id == 87)
        val v347 = v343.asInstanceOf[SequenceNode].children(6)
        val BindNode(v348, v349) = v347
        assert(v348.id == 87)
        val v350 = v343.asInstanceOf[SequenceNode].children(7)
        val v351 = unrollRepeat0(v350).map { elem =>
        val BindNode(v352, v353) = elem
        assert(v352.id == 416)
        val BindNode(v354, v355) = v353
        val v361 = v354.id match {
        case 417 =>
          val BindNode(v356, v357) = v355
          assert(v356.id == 418)
          val v358 = v357.asInstanceOf[SequenceNode].children(3)
          val BindNode(v359, v360) = v358
          assert(v359.id == 87)
          matchSimpleName(v360)
      }
        v361
        }
        EnumDef(matchSimpleName(v346), List(matchSimpleName(v349)) ++ v351)(v343)
    }
    v362
  }

  def matchEscapeChar(node: Node): EscapeChar = {
    val BindNode(v363, v364) = node
    val v368 = v363.id match {
      case 157 =>
        val v365 = v364.asInstanceOf[SequenceNode].children(1)
        val BindNode(v366, v367) = v365
        assert(v366.id == 159)
        EscapeChar(v367.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char)(v364)
    }
    v368
  }

  def matchExpr(node: Node): Expr = {
    val BindNode(v369, v370) = node
    val v380 = v369.id match {
      case 170 =>
        val v371 = v370.asInstanceOf[SequenceNode].children.head
        val BindNode(v372, v373) = v371
        assert(v372.id == 169)
        val v374 = v370.asInstanceOf[SequenceNode].children(4)
        val BindNode(v375, v376) = v374
        assert(v375.id == 172)
        MergeOp(matchExpr(v373), matchPrimary(v376))(v370)
      case 237 =>
        val v377 = v370.asInstanceOf[SequenceNode].children.head
        val BindNode(v378, v379) = v377
        assert(v378.id == 172)
        matchPrimary(v379)
    }
    v380
  }

  def matchImportDef(node: Node): ImportDef = {
    val BindNode(v381, v382) = node
    val v426 = v381.id match {
      case 117 =>
        val v383 = v382.asInstanceOf[SequenceNode].children(2)
        val BindNode(v384, v385) = v383
        assert(v384.id == 123)
        val v386 = v382.asInstanceOf[SequenceNode].children(3)
        val BindNode(v387, v388) = v386
        assert(v387.id == 131)
        val BindNode(v389, v390) = v388
        val v399 = v389.id match {
        case 138 =>
        None
        case 132 =>
          val BindNode(v391, v392) = v390
          val v398 = v391.id match {
          case 133 =>
            val BindNode(v393, v394) = v392
            assert(v393.id == 134)
            val v395 = v394.asInstanceOf[SequenceNode].children(3)
            val BindNode(v396, v397) = v395
            assert(v396.id == 87)
            matchSimpleName(v397)
        }
          Some(v398)
      }
        ImportName(matchName(v385), v399)(v382)
      case 139 =>
        val v400 = v382.asInstanceOf[SequenceNode].children(2)
        val BindNode(v401, v402) = v400
        assert(v401.id == 140)
        val v403 = v382.asInstanceOf[SequenceNode].children(6)
        val BindNode(v404, v405) = v403
        assert(v404.id == 87)
        ImportAll(matchImportSourceExpr(v402), matchSimpleName(v405))(v382)
      case 238 =>
        val v406 = v382.asInstanceOf[SequenceNode].children(2)
        val BindNode(v407, v408) = v406
        assert(v407.id == 140)
        val v409 = v382.asInstanceOf[SequenceNode].children(6)
        val BindNode(v410, v411) = v409
        assert(v410.id == 123)
        val v412 = v382.asInstanceOf[SequenceNode].children(7)
        val BindNode(v413, v414) = v412
        assert(v413.id == 131)
        val BindNode(v415, v416) = v414
        val v425 = v415.id match {
        case 138 =>
        None
        case 132 =>
          val BindNode(v417, v418) = v416
          val v424 = v417.id match {
          case 133 =>
            val BindNode(v419, v420) = v418
            assert(v419.id == 134)
            val v421 = v420.asInstanceOf[SequenceNode].children(3)
            val BindNode(v422, v423) = v421
            assert(v422.id == 87)
            matchSimpleName(v423)
        }
          Some(v424)
      }
        ImportFrom(matchImportSourceExpr(v408), matchName(v411), v425)(v382)
    }
    v426
  }

  def matchImportSourceExpr(node: Node): ImportSourceExpr = {
    val BindNode(v427, v428) = node
    val v435 = v427.id match {
      case 141 =>
        val v429 = v428.asInstanceOf[SequenceNode].children.head
        val BindNode(v430, v431) = v429
        assert(v430.id == 142)
        matchStringLiteral(v431)
      case 173 =>
        val v432 = v428.asInstanceOf[SequenceNode].children.head
        val BindNode(v433, v434) = v432
        assert(v433.id == 174)
        matchCallExpr(v434)
    }
    v435
  }

  def matchLiteral(node: Node): Literal = {
    val BindNode(v436, v437) = node
    val v444 = v436.id match {
      case 141 =>
        val v438 = v437.asInstanceOf[SequenceNode].children.head
        val BindNode(v439, v440) = v438
        assert(v439.id == 142)
        matchStringLiteral(v440)
      case 230 =>
        val v441 = v437.asInstanceOf[SequenceNode].children.head
        val BindNode(v442, v443) = v441
        assert(v442.id == 231)
        matchBooleanLiteral(v443)
    }
    v444
  }

  def matchMethodRef(node: Node): MethodRef = {
    val BindNode(v445, v446) = node
    val v467 = v445.id match {
      case 372 =>
        val v447 = v446.asInstanceOf[SequenceNode].children.head
        val BindNode(v448, v449) = v447
        assert(v448.id == 123)
        val v450 = v446.asInstanceOf[SequenceNode].children(4)
        val BindNode(v451, v452) = v450
        assert(v451.id == 123)
        val v453 = v446.asInstanceOf[SequenceNode].children(5)
        val BindNode(v454, v455) = v453
        assert(v454.id == 373)
        val BindNode(v456, v457) = v455
        val v466 = v456.id match {
        case 138 =>
        None
        case 374 =>
          val BindNode(v458, v459) = v457
          val v465 = v458.id match {
          case 375 =>
            val BindNode(v460, v461) = v459
            assert(v460.id == 376)
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
      case 124 =>
        val v470 = v469.asInstanceOf[SequenceNode].children.head
        val BindNode(v471, v472) = v470
        assert(v471.id == 87)
        val v473 = v469.asInstanceOf[SequenceNode].children(1)
        val v474 = unrollRepeat0(v473).map { elem =>
        val BindNode(v475, v476) = elem
        assert(v475.id == 127)
        val BindNode(v477, v478) = v476
        val v484 = v477.id match {
        case 128 =>
          val BindNode(v479, v480) = v478
          assert(v479.id == 129)
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
      case 197 =>
        val v488 = v487.asInstanceOf[SequenceNode].children.head
        val BindNode(v489, v490) = v488
        assert(v489.id == 87)
        val v491 = v487.asInstanceOf[SequenceNode].children(4)
        val BindNode(v492, v493) = v491
        assert(v492.id == 169)
        NameDef(matchSimpleName(v490), matchExpr(v493))(v487)
    }
    v494
  }

  def matchNamedExpr(node: Node): NamedExpr = {
    val BindNode(v495, v496) = node
    val v503 = v495.id match {
      case 221 =>
        val v497 = v496.asInstanceOf[SequenceNode].children.head
        val BindNode(v498, v499) = v497
        assert(v498.id == 87)
        val v500 = v496.asInstanceOf[SequenceNode].children(4)
        val BindNode(v501, v502) = v500
        assert(v501.id == 169)
        NamedExpr(matchSimpleName(v499), matchExpr(v502))(v496)
    }
    v503
  }

  def matchNamedParam(node: Node): NamedParam = {
    val BindNode(v504, v505) = node
    val v512 = v504.id match {
      case 197 =>
        val v506 = v505.asInstanceOf[SequenceNode].children.head
        val BindNode(v507, v508) = v506
        assert(v507.id == 87)
        val v509 = v505.asInstanceOf[SequenceNode].children(4)
        val BindNode(v510, v511) = v509
        assert(v510.id == 169)
        NamedParam(matchSimpleName(v508), matchExpr(v511))(v505)
    }
    v512
  }

  def matchNamedParams(node: Node): List[NamedParam] = {
    val BindNode(v513, v514) = node
    val v530 = v513.id match {
      case 195 =>
        val v515 = v514.asInstanceOf[SequenceNode].children.head
        val BindNode(v516, v517) = v515
        assert(v516.id == 196)
        val v518 = v514.asInstanceOf[SequenceNode].children(1)
        val v519 = unrollRepeat0(v518).map { elem =>
        val BindNode(v520, v521) = elem
        assert(v520.id == 201)
        val BindNode(v522, v523) = v521
        val v529 = v522.id match {
        case 202 =>
          val BindNode(v524, v525) = v523
          assert(v524.id == 203)
          val v526 = v525.asInstanceOf[SequenceNode].children(3)
          val BindNode(v527, v528) = v526
          assert(v527.id == 196)
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
      case 289 =>
        val v533 = v532.asInstanceOf[SequenceNode].children(2)
        val BindNode(v534, v535) = v533
        assert(v534.id == 290)
        val v536 = v532.asInstanceOf[SequenceNode].children(3)
        val v537 = unrollRepeat0(v536).map { elem =>
        val BindNode(v538, v539) = elem
        assert(v538.id == 293)
        val BindNode(v540, v541) = v539
        val v547 = v540.id match {
        case 294 =>
          val BindNode(v542, v543) = v541
          assert(v542.id == 295)
          val v544 = v543.asInstanceOf[SequenceNode].children(3)
          val BindNode(v545, v546) = v544
          assert(v545.id == 290)
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
      case 257 =>
        val v551 = v550.asInstanceOf[SequenceNode].children.head
        val BindNode(v552, v553) = v551
        assert(v552.id == 87)
        val v554 = v550.asInstanceOf[SequenceNode].children(4)
        val BindNode(v555, v556) = v554
        assert(v555.id == 258)
        NamedType(matchSimpleName(v553), matchTypeExpr(v556))(v550)
    }
    v557
  }

  def matchNamedTypeParam(node: Node): NamedTypeParam = {
    val BindNode(v558, v559) = node
    val v566 = v558.id match {
      case 257 =>
        val v560 = v559.asInstanceOf[SequenceNode].children.head
        val BindNode(v561, v562) = v560
        assert(v561.id == 87)
        val v563 = v559.asInstanceOf[SequenceNode].children(4)
        val BindNode(v564, v565) = v563
        assert(v564.id == 258)
        NamedTypeParam(matchSimpleName(v562), matchTypeExpr(v565))(v559)
    }
    v566
  }

  def matchNamedTypeParams(node: Node): List[NamedTypeParam] = {
    val BindNode(v567, v568) = node
    val v584 = v567.id match {
      case 255 =>
        val v569 = v568.asInstanceOf[SequenceNode].children(2)
        val BindNode(v570, v571) = v569
        assert(v570.id == 256)
        val v572 = v568.asInstanceOf[SequenceNode].children(3)
        val v573 = unrollRepeat0(v572).map { elem =>
        val BindNode(v574, v575) = elem
        assert(v574.id == 306)
        val BindNode(v576, v577) = v575
        val v583 = v576.id match {
        case 307 =>
          val BindNode(v578, v579) = v577
          assert(v578.id == 308)
          val v580 = v579.asInstanceOf[SequenceNode].children(3)
          val BindNode(v581, v582) = v580
          assert(v581.id == 256)
          matchNamedTypeParam(v582)
      }
        v583
        }
        List(matchNamedTypeParam(v571)) ++ v573
    }
    v584
  }

  def matchNamespaceDef(node: Node): NamespaceDef = {
    val BindNode(v585, v586) = node
    val v593 = v585.id match {
      case 59 =>
        val v587 = v586.asInstanceOf[SequenceNode].children(2)
        val BindNode(v588, v589) = v587
        assert(v588.id == 87)
        val v590 = v586.asInstanceOf[SequenceNode].children(5)
        val BindNode(v591, v592) = v590
        assert(v591.id == 2)
        NamespaceDef(matchSimpleName(v589), matchBuildScript(v592))(v586)
    }
    v593
  }

  def matchNoUnionType(node: Node): NoUnionType = {
    val BindNode(v594, v595) = node
    val v608 = v594.id match {
      case 261 =>
        val v596 = v595.asInstanceOf[SequenceNode].children.head
        val BindNode(v597, v598) = v596
        assert(v597.id == 123)
        matchName(v598)
      case 262 =>
        val v599 = v595.asInstanceOf[SequenceNode].children.head
        val BindNode(v600, v601) = v599
        assert(v600.id == 263)
        matchCollectionType(v601)
      case 284 =>
        val v602 = v595.asInstanceOf[SequenceNode].children.head
        val BindNode(v603, v604) = v602
        assert(v603.id == 285)
        matchTupleType(v604)
      case 287 =>
        val v605 = v595.asInstanceOf[SequenceNode].children.head
        val BindNode(v606, v607) = v605
        assert(v606.id == 288)
        matchNamedTupleType(v607)
    }
    v608
  }

  def matchParamDef(node: Node): ParamDef = {
    val BindNode(v609, v610) = node
    val v651 = v609.id match {
      case 356 =>
        val v611 = v610.asInstanceOf[SequenceNode].children.head
        val BindNode(v612, v613) = v611
        assert(v612.id == 87)
        val v614 = v610.asInstanceOf[SequenceNode].children(1)
        val BindNode(v615, v616) = v614
        assert(v615.id == 357)
        val BindNode(v617, v618) = v616
        val v627 = v617.id match {
        case 138 =>
        None
        case 358 =>
          val BindNode(v619, v620) = v618
          val v626 = v619.id match {
          case 359 =>
            val BindNode(v621, v622) = v620
            assert(v621.id == 360)
            val v623 = v622.asInstanceOf[SequenceNode].children(1)
            val BindNode(v624, v625) = v623
            assert(v624.id == 361)
            v625.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
        }
          Some(v626)
      }
        val v628 = v610.asInstanceOf[SequenceNode].children(5)
        val BindNode(v629, v630) = v628
        assert(v629.id == 258)
        val v631 = v610.asInstanceOf[SequenceNode].children(6)
        val BindNode(v632, v633) = v631
        assert(v632.id == 362)
        val BindNode(v634, v635) = v633
        val v644 = v634.id match {
        case 138 =>
        None
        case 363 =>
          val BindNode(v636, v637) = v635
          val v643 = v636.id match {
          case 364 =>
            val BindNode(v638, v639) = v637
            assert(v638.id == 365)
            val v640 = v639.asInstanceOf[SequenceNode].children(3)
            val BindNode(v641, v642) = v640
            assert(v641.id == 169)
            matchExpr(v642)
        }
          Some(v643)
      }
        ParamDef(matchSimpleName(v613), v627.isDefined, Some(matchTypeExpr(v630)), v644)(v610)
      case 197 =>
        val v645 = v610.asInstanceOf[SequenceNode].children.head
        val BindNode(v646, v647) = v645
        assert(v646.id == 87)
        val v648 = v610.asInstanceOf[SequenceNode].children(4)
        val BindNode(v649, v650) = v648
        assert(v649.id == 169)
        ParamDef(matchSimpleName(v647), false, None, Some(matchExpr(v650)))(v610)
    }
    v651
  }

  def matchParamsDef(node: Node): List[ParamDef] = {
    val BindNode(v652, v653) = node
    val v680 = v652.id match {
      case 350 =>
        val v655 = v653.asInstanceOf[SequenceNode].children(1)
        val BindNode(v656, v657) = v655
        assert(v656.id == 351)
        val BindNode(v658, v659) = v657
        val v679 = v658.id match {
        case 138 =>
        None
        case 352 =>
          val BindNode(v660, v661) = v659
          assert(v660.id == 353)
          val BindNode(v662, v663) = v661
          assert(v662.id == 354)
          val v664 = v663.asInstanceOf[SequenceNode].children(1)
          val BindNode(v665, v666) = v664
          assert(v665.id == 355)
          val v667 = v663.asInstanceOf[SequenceNode].children(2)
          val v668 = unrollRepeat0(v667).map { elem =>
          val BindNode(v669, v670) = elem
          assert(v669.id == 368)
          val BindNode(v671, v672) = v670
          val v678 = v671.id match {
          case 369 =>
            val BindNode(v673, v674) = v672
            assert(v673.id == 370)
            val v675 = v674.asInstanceOf[SequenceNode].children(3)
            val BindNode(v676, v677) = v675
            assert(v676.id == 355)
            matchParamDef(v677)
        }
          v678
          }
          Some(List(matchParamDef(v666)) ++ v668)
      }
        val v654 = v679
        if (v654.isDefined) v654.get else List()
    }
    v680
  }

  def matchPositionalParams(node: Node): List[Expr] = {
    val BindNode(v681, v682) = node
    val v698 = v681.id match {
      case 182 =>
        val v683 = v682.asInstanceOf[SequenceNode].children.head
        val BindNode(v684, v685) = v683
        assert(v684.id == 169)
        val v686 = v682.asInstanceOf[SequenceNode].children(1)
        val v687 = unrollRepeat0(v686).map { elem =>
        val BindNode(v688, v689) = elem
        assert(v688.id == 185)
        val BindNode(v690, v691) = v689
        val v697 = v690.id match {
        case 186 =>
          val BindNode(v692, v693) = v691
          assert(v692.id == 187)
          val v694 = v693.asInstanceOf[SequenceNode].children(3)
          val BindNode(v695, v696) = v694
          assert(v695.id == 169)
          matchExpr(v696)
      }
        v697
        }
        List(matchExpr(v685)) ++ v687
    }
    v698
  }

  def matchPrimary(node: Node): Primary = {
    val BindNode(v699, v700) = node
    val v794 = v699.id match {
      case 173 =>
        val v701 = v700.asInstanceOf[SequenceNode].children.head
        val BindNode(v702, v703) = v701
        assert(v702.id == 174)
        matchCallExpr(v703)
      case 206 =>
        val v704 = v700.asInstanceOf[SequenceNode].children.head
        val BindNode(v705, v706) = v704
        assert(v705.id == 87)
        NameRef(matchSimpleName(v706))(v700)
      case 215 =>
        val v708 = v700.asInstanceOf[SequenceNode].children(1)
        val BindNode(v709, v710) = v708
        assert(v709.id == 216)
        val BindNode(v711, v712) = v710
        val v732 = v711.id match {
        case 138 =>
        None
        case 217 =>
          val BindNode(v713, v714) = v712
          assert(v713.id == 218)
          val BindNode(v715, v716) = v714
          assert(v715.id == 219)
          val v717 = v716.asInstanceOf[SequenceNode].children(1)
          val BindNode(v718, v719) = v717
          assert(v718.id == 220)
          val v720 = v716.asInstanceOf[SequenceNode].children(2)
          val v721 = unrollRepeat0(v720).map { elem =>
          val BindNode(v722, v723) = elem
          assert(v722.id == 225)
          val BindNode(v724, v725) = v723
          val v731 = v724.id match {
          case 226 =>
            val BindNode(v726, v727) = v725
            assert(v726.id == 227)
            val v728 = v727.asInstanceOf[SequenceNode].children(3)
            val BindNode(v729, v730) = v728
            assert(v729.id == 220)
            matchNamedExpr(v730)
        }
          v731
          }
          Some(List(matchNamedExpr(v719)) ++ v721)
      }
        val v707 = v732
        NamedTupleExpr(if (v707.isDefined) v707.get else List())(v700)
      case 207 =>
        val v734 = v700.asInstanceOf[SequenceNode].children(1)
        val BindNode(v735, v736) = v734
        assert(v735.id == 209)
        val BindNode(v737, v738) = v736
        val v758 = v737.id match {
        case 138 =>
        None
        case 210 =>
          val BindNode(v739, v740) = v738
          assert(v739.id == 211)
          val BindNode(v741, v742) = v740
          assert(v741.id == 212)
          val v743 = v742.asInstanceOf[SequenceNode].children(1)
          val BindNode(v744, v745) = v743
          assert(v744.id == 169)
          val v746 = v742.asInstanceOf[SequenceNode].children(2)
          val v747 = unrollRepeat0(v746).map { elem =>
          val BindNode(v748, v749) = elem
          assert(v748.id == 185)
          val BindNode(v750, v751) = v749
          val v757 = v750.id match {
          case 186 =>
            val BindNode(v752, v753) = v751
            assert(v752.id == 187)
            val v754 = v753.asInstanceOf[SequenceNode].children(3)
            val BindNode(v755, v756) = v754
            assert(v755.id == 169)
            matchExpr(v756)
        }
          v757
          }
          Some(List(matchExpr(v745)) ++ v747)
      }
        val v733 = v758
        ListExpr(if (v733.isDefined) v733.get else List())(v700)
      case 228 =>
        val v759 = v700.asInstanceOf[SequenceNode].children.head
        val BindNode(v760, v761) = v759
        assert(v760.id == 229)
        matchLiteral(v761)
      case 205 =>
        val v762 = v700.asInstanceOf[SequenceNode].children.head
        val BindNode(v763, v764) = v762
        assert(v763.id == 172)
        val v765 = v700.asInstanceOf[SequenceNode].children(4)
        val BindNode(v766, v767) = v765
        assert(v766.id == 87)
        MemberAccess(matchPrimary(v764), matchSimpleName(v767))(v700)
      case 214 =>
        val v769 = v700.asInstanceOf[SequenceNode].children(1)
        val BindNode(v770, v771) = v769
        assert(v770.id == 209)
        val BindNode(v772, v773) = v771
        val v793 = v772.id match {
        case 138 =>
        None
        case 210 =>
          val BindNode(v774, v775) = v773
          assert(v774.id == 211)
          val BindNode(v776, v777) = v775
          assert(v776.id == 212)
          val v778 = v777.asInstanceOf[SequenceNode].children(1)
          val BindNode(v779, v780) = v778
          assert(v779.id == 169)
          val v781 = v777.asInstanceOf[SequenceNode].children(2)
          val v782 = unrollRepeat0(v781).map { elem =>
          val BindNode(v783, v784) = elem
          assert(v783.id == 185)
          val BindNode(v785, v786) = v784
          val v792 = v785.id match {
          case 186 =>
            val BindNode(v787, v788) = v786
            assert(v787.id == 187)
            val v789 = v788.asInstanceOf[SequenceNode].children(3)
            val BindNode(v790, v791) = v789
            assert(v790.id == 169)
            matchExpr(v791)
        }
          v792
          }
          Some(List(matchExpr(v780)) ++ v782)
      }
        val v768 = v793
        TupleExpr(if (v768.isDefined) v768.get else List())(v700)
    }
    v794
  }

  def matchSimpleName(node: Node): String = {
    val BindNode(v795, v796) = node
    val v827 = v795.id match {
      case 88 =>
        val v797 = v796.asInstanceOf[SequenceNode].children.head
        val BindNode(v798, v799) = v797
        assert(v798.id == 89)
        val BindNode(v800, v801) = v799
        assert(v800.id == 90)
        val BindNode(v802, v803) = v801
        assert(v802.id == 91)
        val BindNode(v804, v805) = v803
        val v826 = v804.id match {
        case 92 =>
          val BindNode(v806, v807) = v805
          assert(v806.id == 93)
          val v808 = v807.asInstanceOf[SequenceNode].children.head
          val BindNode(v809, v810) = v808
          assert(v809.id == 94)
          val JoinNode(_, v811, _) = v810
          val BindNode(v812, v813) = v811
          assert(v812.id == 95)
          val BindNode(v814, v815) = v813
          val v825 = v814.id match {
          case 96 =>
            val BindNode(v816, v817) = v815
            assert(v816.id == 97)
            val v818 = v817.asInstanceOf[SequenceNode].children.head
            val BindNode(v819, v820) = v818
            assert(v819.id == 98)
            val v821 = v817.asInstanceOf[SequenceNode].children(1)
            val v822 = unrollRepeat0(v821).map { elem =>
            val BindNode(v823, v824) = elem
            assert(v823.id == 77)
            v824.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
            }
            v820.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char.toString + v822.map(x => x.toString).mkString("")
        }
          v825
      }
        v826
    }
    v827
  }

  def matchStringElem(node: Node): StringElem = {
    val BindNode(v828, v829) = node
    val v841 = v828.id match {
      case 152 =>
        val v830 = v829.asInstanceOf[SequenceNode].children.head
        val BindNode(v831, v832) = v830
        assert(v831.id == 153)
        val BindNode(v833, v834) = v832
        assert(v833.id == 30)
        JustChar(v834.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char)(v829)
      case 155 =>
        val v835 = v829.asInstanceOf[SequenceNode].children.head
        val BindNode(v836, v837) = v835
        assert(v836.id == 156)
        matchEscapeChar(v837)
      case 160 =>
        val v838 = v829.asInstanceOf[SequenceNode].children.head
        val BindNode(v839, v840) = v838
        assert(v839.id == 161)
        matchStringExpr(v840)
    }
    v841
  }

  def matchStringExpr(node: Node): StringExpr = {
    val BindNode(v842, v843) = node
    val v860 = v842.id match {
      case 162 =>
        val v844 = v843.asInstanceOf[SequenceNode].children.head
        val BindNode(v845, v846) = v844
        assert(v845.id == 163)
        val BindNode(v847, v848) = v846
        assert(v847.id == 164)
        val BindNode(v849, v850) = v848
        val v856 = v849.id match {
        case 165 =>
          val BindNode(v851, v852) = v850
          assert(v851.id == 166)
          val v853 = v852.asInstanceOf[SequenceNode].children(1)
          val BindNode(v854, v855) = v853
          assert(v854.id == 87)
          matchSimpleName(v855)
      }
        SimpleExpr(v856)(v843)
      case 168 =>
        val v857 = v843.asInstanceOf[SequenceNode].children(3)
        val BindNode(v858, v859) = v857
        assert(v858.id == 169)
        ComplexExpr(matchExpr(v859))(v843)
    }
    v860
  }

  def matchStringLiteral(node: Node): StringLiteral = {
    val BindNode(v861, v862) = node
    val v877 = v861.id match {
      case 143 =>
        val v863 = v862.asInstanceOf[SequenceNode].children(1)
        val v864 = unrollRepeat0(v863).map { elem =>
        val BindNode(v865, v866) = elem
        assert(v865.id == 147)
        val BindNode(v867, v868) = v866
        assert(v867.id == 148)
        val BindNode(v869, v870) = v868
        val v876 = v869.id match {
        case 149 =>
          val BindNode(v871, v872) = v870
          assert(v871.id == 150)
          val v873 = v872.asInstanceOf[SequenceNode].children.head
          val BindNode(v874, v875) = v873
          assert(v874.id == 151)
          matchStringElem(v875)
      }
        v876
        }
        StringLiteral(v864)(v862)
    }
    v877
  }

  def matchTupleType(node: Node): TupleType = {
    val BindNode(v878, v879) = node
    val v895 = v878.id match {
      case 286 =>
        val v880 = v879.asInstanceOf[SequenceNode].children(2)
        val BindNode(v881, v882) = v880
        assert(v881.id == 258)
        val v883 = v879.asInstanceOf[SequenceNode].children(3)
        val v884 = unrollRepeat0(v883).map { elem =>
        val BindNode(v885, v886) = elem
        assert(v885.id == 280)
        val BindNode(v887, v888) = v886
        val v894 = v887.id match {
        case 281 =>
          val BindNode(v889, v890) = v888
          assert(v889.id == 282)
          val v891 = v890.asInstanceOf[SequenceNode].children(3)
          val BindNode(v892, v893) = v891
          assert(v892.id == 258)
          matchTypeExpr(v893)
      }
        v894
        }
        TupleType(List(matchTypeExpr(v882)) ++ v884)(v879)
    }
    v895
  }

  def matchTypeExpr(node: Node): TypeExpr = {
    val BindNode(v896, v897) = node
    val v904 = v896.id match {
      case 259 =>
        val v898 = v897.asInstanceOf[SequenceNode].children.head
        val BindNode(v899, v900) = v898
        assert(v899.id == 260)
        matchNoUnionType(v900)
      case 296 =>
        val v901 = v897.asInstanceOf[SequenceNode].children.head
        val BindNode(v902, v903) = v901
        assert(v902.id == 297)
        matchUnionType(v903)
    }
    v904
  }

  def matchTypeParams(node: Node): TypeParams = {
    val BindNode(v905, v906) = node
    val v922 = v905.id match {
      case 276 =>
        val v907 = v906.asInstanceOf[SequenceNode].children(2)
        val BindNode(v908, v909) = v907
        assert(v908.id == 258)
        val v910 = v906.asInstanceOf[SequenceNode].children(3)
        val v911 = unrollRepeat0(v910).map { elem =>
        val BindNode(v912, v913) = elem
        assert(v912.id == 280)
        val BindNode(v914, v915) = v913
        val v921 = v914.id match {
        case 281 =>
          val BindNode(v916, v917) = v915
          assert(v916.id == 282)
          val v918 = v917.asInstanceOf[SequenceNode].children(3)
          val BindNode(v919, v920) = v918
          assert(v919.id == 258)
          matchTypeExpr(v920)
      }
        v921
        }
        TypeParams(List(matchTypeExpr(v909)) ++ v911)(v906)
    }
    v922
  }

  def matchUnionType(node: Node): UnionType = {
    val BindNode(v923, v924) = node
    val v940 = v923.id match {
      case 298 =>
        val v925 = v924.asInstanceOf[SequenceNode].children(2)
        val BindNode(v926, v927) = v925
        assert(v926.id == 260)
        val v928 = v924.asInstanceOf[SequenceNode].children(3)
        val v929 = unrollRepeat0(v928).map { elem =>
        val BindNode(v930, v931) = elem
        assert(v930.id == 301)
        val BindNode(v932, v933) = v931
        val v939 = v932.id match {
        case 302 =>
          val BindNode(v934, v935) = v933
          assert(v934.id == 303)
          val v936 = v935.asInstanceOf[SequenceNode].children(3)
          val BindNode(v937, v938) = v936
          assert(v937.id == 260)
          matchNoUnionType(v938)
      }
        v939
        }
        UnionType(List(matchNoUnionType(v927)) ++ v929)(v924)
    }
    v940
  }

  def matchStart(node: Node): BuildScript = {
    val BindNode(start, BindNode(_, body)) = node
    assert(start.id == 1)
    matchBuildScript(body)
  }

    val milestoneParserData = MilestoneParserProtobufConverter.convertProtoToMilestoneParserData(
      MilestoneParserDataProto.MilestoneParserData.parseFrom(readFileBytes("/home/joonsoo/Documents/workspace/bibix/bibix-ast/src/generated/resources/parserdata.pb")))

val milestoneParser = new MilestoneParser(milestoneParserData)

def parse(text: String): Either[ParseForest, ParsingErrors.ParsingError] =
  milestoneParser.parseAndReconstructToForest(text)

def parseAst(text: String): Either[BuildScript, ParsingErrors.ParsingError] =
  parse(text) match {
    case Left(forest) => Left(matchStart(forest.trees.head))
    case Right(error) => Right(error)
  }


}
