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
  case class ClassExtending(name: Name, typeParams: List[Expr])(override val parseNode: Node) extends WithParseNode
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
  case class ParameterizedClassType(name: Name, typeParams: List[Expr])(override val parseNode: Node) extends NoUnionType with WithParseNode
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
      case 410 =>
        val v3 = v2.asInstanceOf[SequenceNode].children(2)
        val BindNode(v4, v5) = v3
        assert(v4.id == 87)
        val v6 = v2.asInstanceOf[SequenceNode].children(3)
        val BindNode(v7, v8) = v6
        assert(v7.id == 411)
        val BindNode(v9, v10) = v8
        val v19 = v9.id match {
        case 138 =>
        None
        case 412 =>
          val BindNode(v11, v12) = v10
          val v18 = v11.id match {
          case 413 =>
            val BindNode(v13, v14) = v12
            assert(v13.id == 414)
            val v15 = v14.asInstanceOf[SequenceNode].children(1)
            val BindNode(v16, v17) = v15
            assert(v16.id == 415)
            matchActionParams(v17)
        }
          Some(v18)
      }
        val v20 = v2.asInstanceOf[SequenceNode].children(7)
        val BindNode(v21, v22) = v20
        assert(v21.id == 417)
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
      case 416 =>
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
      case 352 =>
        val v38 = v37.asInstanceOf[SequenceNode].children(4)
        val BindNode(v39, v40) = v38
        assert(v39.id == 87)
        val v41 = v37.asInstanceOf[SequenceNode].children(6)
        val BindNode(v42, v43) = v41
        assert(v42.id == 359)
        val v44 = v37.asInstanceOf[SequenceNode].children(10)
        val BindNode(v45, v46) = v44
        assert(v45.id == 381)
        ActionRuleDef(matchSimpleName(v40), matchParamsDef(v43), matchMethodRef(v46))(v37)
    }
    v47
  }

  def matchArgDef(node: Node): ArgDef = {
    val BindNode(v48, v49) = node
    val v94 = v48.id match {
      case 392 =>
        val v50 = v49.asInstanceOf[SequenceNode].children(1)
        val BindNode(v51, v52) = v50
        assert(v51.id == 397)
        val BindNode(v53, v54) = v52
        val v62 = v53.id match {
        case 138 =>
        None
        case 398 =>
          val BindNode(v55, v56) = v54
          assert(v55.id == 399)
          val BindNode(v57, v58) = v56
          assert(v57.id == 400)
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
        assert(v67.id == 401)
        val BindNode(v69, v70) = v68
        val v79 = v69.id match {
        case 138 =>
        None
        case 402 =>
          val BindNode(v71, v72) = v70
          val v78 = v71.id match {
          case 403 =>
            val BindNode(v73, v74) = v72
            assert(v73.id == 404)
            val v75 = v74.asInstanceOf[SequenceNode].children(3)
            val BindNode(v76, v77) = v75
            assert(v76.id == 258)
            matchTypeExpr(v77)
        }
          Some(v78)
      }
        val v80 = v49.asInstanceOf[SequenceNode].children(5)
        val BindNode(v81, v82) = v80
        assert(v81.id == 372)
        val BindNode(v83, v84) = v82
        val v93 = v83.id match {
        case 138 =>
        None
        case 373 =>
          val BindNode(v85, v86) = v84
          val v92 = v85.id match {
          case 374 =>
            val BindNode(v87, v88) = v86
            assert(v87.id == 375)
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
      case 407 =>
        val v113 = v112.asInstanceOf[SequenceNode].children(2)
        val BindNode(v114, v115) = v113
        assert(v114.id == 87)
        val v116 = v112.asInstanceOf[SequenceNode].children(4)
        val BindNode(v117, v118) = v116
        assert(v117.id == 359)
        val v119 = v112.asInstanceOf[SequenceNode].children(8)
        val BindNode(v120, v121) = v119
        assert(v120.id == 258)
        val v122 = v112.asInstanceOf[SequenceNode].children(12)
        val BindNode(v123, v124) = v122
        assert(v123.id == 381)
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
      case 343 =>
        val v158 = v157.asInstanceOf[SequenceNode].children(1)
        val v159 = unrollRepeat0(v158).map { elem =>
        val BindNode(v160, v161) = elem
        assert(v160.id == 346)
        val BindNode(v162, v163) = v161
        val v169 = v162.id match {
        case 347 =>
          val BindNode(v164, v165) = v163
          assert(v164.id == 348)
          val v166 = v165.asInstanceOf[SequenceNode].children(1)
          val BindNode(v167, v168) = v166
          assert(v167.id == 349)
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
      case 350 =>
        val v173 = v172.asInstanceOf[SequenceNode].children.head
        val BindNode(v174, v175) = v173
        assert(v174.id == 351)
        matchActionRuleDef(v175)
      case 387 =>
        val v176 = v172.asInstanceOf[SequenceNode].children.head
        val BindNode(v177, v178) = v176
        assert(v177.id == 388)
        matchClassCastDef(v178)
    }
    v179
  }

  def matchClassCastDef(node: Node): ClassCastDef = {
    val BindNode(v180, v181) = node
    val v188 = v180.id match {
      case 389 =>
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
        assert(v211.id == 314)
        val BindNode(v213, v214) = v212
        val v223 = v213.id match {
        case 138 =>
        None
        case 315 =>
          val BindNode(v215, v216) = v214
          val v222 = v215.id match {
          case 316 =>
            val BindNode(v217, v218) = v216
            assert(v217.id == 317)
            val v219 = v218.asInstanceOf[SequenceNode].children(1)
            val BindNode(v220, v221) = v219
            assert(v220.id == 318)
            matchClassExtendings(v221)
        }
          Some(v222)
      }
        val v209 = v223
        val v224 = v190.asInstanceOf[SequenceNode].children(6)
        val BindNode(v225, v226) = v224
        assert(v225.id == 336)
        val v228 = v190.asInstanceOf[SequenceNode].children(7)
        val BindNode(v229, v230) = v228
        assert(v229.id == 338)
        val BindNode(v231, v232) = v230
        val v241 = v231.id match {
        case 138 =>
        None
        case 339 =>
          val BindNode(v233, v234) = v232
          val v240 = v233.id match {
          case 340 =>
            val BindNode(v235, v236) = v234
            assert(v235.id == 341)
            val v237 = v236.asInstanceOf[SequenceNode].children(1)
            val BindNode(v238, v239) = v237
            assert(v238.id == 342)
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
    val v263 = v243.id match {
      case 326 =>
        val v245 = v244.asInstanceOf[SequenceNode].children.head
        val BindNode(v246, v247) = v245
        assert(v246.id == 123)
        val v249 = v244.asInstanceOf[SequenceNode].children(1)
        val BindNode(v250, v251) = v249
        assert(v250.id == 327)
        val BindNode(v252, v253) = v251
        val v262 = v252.id match {
        case 138 =>
        None
        case 328 =>
          val BindNode(v254, v255) = v253
          val v261 = v254.id match {
          case 329 =>
            val BindNode(v256, v257) = v255
            assert(v256.id == 330)
            val v258 = v257.asInstanceOf[SequenceNode].children(1)
            val BindNode(v259, v260) = v258
            assert(v259.id == 265)
            matchClassTypeParams(v260)
        }
          Some(v261)
      }
        val v248 = v262
        ClassExtending(matchName(v247), if (v248.isDefined) v248.get else List())(v244)
    }
    v263
  }

  def matchClassExtendings(node: Node): List[ClassExtending] = {
    val BindNode(v264, v265) = node
    val v281 = v264.id match {
      case 319 =>
        val v266 = v265.asInstanceOf[SequenceNode].children(2)
        val BindNode(v267, v268) = v266
        assert(v267.id == 325)
        val v269 = v265.asInstanceOf[SequenceNode].children(3)
        val v270 = unrollRepeat0(v269).map { elem =>
        val BindNode(v271, v272) = elem
        assert(v271.id == 333)
        val BindNode(v273, v274) = v272
        val v280 = v273.id match {
        case 334 =>
          val BindNode(v275, v276) = v274
          assert(v275.id == 335)
          val v277 = v276.asInstanceOf[SequenceNode].children(3)
          val BindNode(v278, v279) = v277
          assert(v278.id == 325)
          matchClassExtending(v279)
      }
        v280
        }
        List(matchClassExtending(v268)) ++ v270
    }
    v281
  }

  def matchClassRealityDef(node: Node): TypeExpr = {
    val BindNode(v282, v283) = node
    val v287 = v282.id match {
      case 337 =>
        val v284 = v283.asInstanceOf[SequenceNode].children(2)
        val BindNode(v285, v286) = v284
        assert(v285.id == 258)
        matchTypeExpr(v286)
    }
    v287
  }

  def matchClassTypeParams(node: Node): List[Expr] = {
    val BindNode(v288, v289) = node
    val v305 = v288.id match {
      case 266 =>
        val v290 = v289.asInstanceOf[SequenceNode].children(2)
        val BindNode(v291, v292) = v290
        assert(v291.id == 169)
        val v293 = v289.asInstanceOf[SequenceNode].children(3)
        val v294 = unrollRepeat0(v293).map { elem =>
        val BindNode(v295, v296) = elem
        assert(v295.id == 185)
        val BindNode(v297, v298) = v296
        val v304 = v297.id match {
        case 186 =>
          val BindNode(v299, v300) = v298
          assert(v299.id == 187)
          val v301 = v300.asInstanceOf[SequenceNode].children(3)
          val BindNode(v302, v303) = v301
          assert(v302.id == 169)
          matchExpr(v303)
      }
        v304
        }
        List(matchExpr(v292)) ++ v294
    }
    v305
  }

  def matchCollectionType(node: Node): CollectionType = {
    val BindNode(v306, v307) = node
    val v326 = v306.id match {
      case 269 =>
        val v308 = v307.asInstanceOf[SequenceNode].children.head
        val BindNode(v309, v310) = v308
        assert(v309.id == 270)
        val JoinNode(_, v311, _) = v310
        val BindNode(v312, v313) = v311
        assert(v312.id == 271)
        val BindNode(v314, v315) = v313
        val v322 = v314.id match {
        case 272 =>
          val BindNode(v316, v317) = v315
          assert(v316.id == 273)
          val v318 = v317.asInstanceOf[SequenceNode].children.head
          "set"
        case 276 =>
          val BindNode(v319, v320) = v315
          assert(v319.id == 277)
          val v321 = v320.asInstanceOf[SequenceNode].children.head
          "list"
      }
        val v323 = v307.asInstanceOf[SequenceNode].children(2)
        val BindNode(v324, v325) = v323
        assert(v324.id == 280)
        CollectionType(v322, matchTypeParams(v325))(v307)
    }
    v326
  }

  def matchDef(node: Node): Def = {
    val BindNode(v327, v328) = node
    val v356 = v327.id match {
      case 244 =>
        val v329 = v328.asInstanceOf[SequenceNode].children.head
        val BindNode(v330, v331) = v329
        assert(v330.id == 245)
        matchClassDef(v331)
      case 115 =>
        val v332 = v328.asInstanceOf[SequenceNode].children.head
        val BindNode(v333, v334) = v332
        assert(v333.id == 116)
        matchImportDef(v334)
      case 408 =>
        val v335 = v328.asInstanceOf[SequenceNode].children.head
        val BindNode(v336, v337) = v335
        assert(v336.id == 409)
        matchActionDef(v337)
      case 57 =>
        val v338 = v328.asInstanceOf[SequenceNode].children.head
        val BindNode(v339, v340) = v338
        assert(v339.id == 58)
        matchNamespaceDef(v340)
      case 390 =>
        val v341 = v328.asInstanceOf[SequenceNode].children.head
        val BindNode(v342, v343) = v341
        assert(v342.id == 391)
        matchArgDef(v343)
      case 350 =>
        val v344 = v328.asInstanceOf[SequenceNode].children.head
        val BindNode(v345, v346) = v344
        assert(v345.id == 351)
        matchActionRuleDef(v346)
      case 242 =>
        val v347 = v328.asInstanceOf[SequenceNode].children.head
        val BindNode(v348, v349) = v347
        assert(v348.id == 243)
        matchNameDef(v349)
      case 405 =>
        val v350 = v328.asInstanceOf[SequenceNode].children.head
        val BindNode(v351, v352) = v350
        assert(v351.id == 406)
        matchBuildRuleDef(v352)
      case 418 =>
        val v353 = v328.asInstanceOf[SequenceNode].children.head
        val BindNode(v354, v355) = v353
        assert(v354.id == 419)
        matchEnumDef(v355)
    }
    v356
  }

  def matchDefs(node: Node): List[Def] = {
    val BindNode(v357, v358) = node
    val v374 = v357.id match {
      case 55 =>
        val v359 = v358.asInstanceOf[SequenceNode].children.head
        val BindNode(v360, v361) = v359
        assert(v360.id == 56)
        val v362 = v358.asInstanceOf[SequenceNode].children(1)
        val v363 = unrollRepeat0(v362).map { elem =>
        val BindNode(v364, v365) = elem
        assert(v364.id == 431)
        val BindNode(v366, v367) = v365
        val v373 = v366.id match {
        case 432 =>
          val BindNode(v368, v369) = v367
          assert(v368.id == 433)
          val v370 = v369.asInstanceOf[SequenceNode].children(1)
          val BindNode(v371, v372) = v370
          assert(v371.id == 56)
          matchDef(v372)
      }
        v373
        }
        List(matchDef(v361)) ++ v363
    }
    v374
  }

  def matchEnumDef(node: Node): EnumDef = {
    val BindNode(v375, v376) = node
    val v395 = v375.id match {
      case 420 =>
        val v377 = v376.asInstanceOf[SequenceNode].children(2)
        val BindNode(v378, v379) = v377
        assert(v378.id == 87)
        val v380 = v376.asInstanceOf[SequenceNode].children(6)
        val BindNode(v381, v382) = v380
        assert(v381.id == 87)
        val v383 = v376.asInstanceOf[SequenceNode].children(7)
        val v384 = unrollRepeat0(v383).map { elem =>
        val BindNode(v385, v386) = elem
        assert(v385.id == 426)
        val BindNode(v387, v388) = v386
        val v394 = v387.id match {
        case 427 =>
          val BindNode(v389, v390) = v388
          assert(v389.id == 428)
          val v391 = v390.asInstanceOf[SequenceNode].children(3)
          val BindNode(v392, v393) = v391
          assert(v392.id == 87)
          matchSimpleName(v393)
      }
        v394
        }
        EnumDef(matchSimpleName(v379), List(matchSimpleName(v382)) ++ v384)(v376)
    }
    v395
  }

  def matchEscapeChar(node: Node): EscapeChar = {
    val BindNode(v396, v397) = node
    val v401 = v396.id match {
      case 157 =>
        val v398 = v397.asInstanceOf[SequenceNode].children(1)
        val BindNode(v399, v400) = v398
        assert(v399.id == 159)
        EscapeChar(v400.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char)(v397)
    }
    v401
  }

  def matchExpr(node: Node): Expr = {
    val BindNode(v402, v403) = node
    val v413 = v402.id match {
      case 170 =>
        val v404 = v403.asInstanceOf[SequenceNode].children.head
        val BindNode(v405, v406) = v404
        assert(v405.id == 169)
        val v407 = v403.asInstanceOf[SequenceNode].children(4)
        val BindNode(v408, v409) = v407
        assert(v408.id == 172)
        MergeOp(matchExpr(v406), matchPrimary(v409))(v403)
      case 237 =>
        val v410 = v403.asInstanceOf[SequenceNode].children.head
        val BindNode(v411, v412) = v410
        assert(v411.id == 172)
        matchPrimary(v412)
    }
    v413
  }

  def matchImportDef(node: Node): ImportDef = {
    val BindNode(v414, v415) = node
    val v459 = v414.id match {
      case 117 =>
        val v416 = v415.asInstanceOf[SequenceNode].children(2)
        val BindNode(v417, v418) = v416
        assert(v417.id == 123)
        val v419 = v415.asInstanceOf[SequenceNode].children(3)
        val BindNode(v420, v421) = v419
        assert(v420.id == 131)
        val BindNode(v422, v423) = v421
        val v432 = v422.id match {
        case 138 =>
        None
        case 132 =>
          val BindNode(v424, v425) = v423
          val v431 = v424.id match {
          case 133 =>
            val BindNode(v426, v427) = v425
            assert(v426.id == 134)
            val v428 = v427.asInstanceOf[SequenceNode].children(3)
            val BindNode(v429, v430) = v428
            assert(v429.id == 87)
            matchSimpleName(v430)
        }
          Some(v431)
      }
        ImportName(matchName(v418), v432)(v415)
      case 139 =>
        val v433 = v415.asInstanceOf[SequenceNode].children(2)
        val BindNode(v434, v435) = v433
        assert(v434.id == 140)
        val v436 = v415.asInstanceOf[SequenceNode].children(6)
        val BindNode(v437, v438) = v436
        assert(v437.id == 87)
        ImportAll(matchImportSourceExpr(v435), matchSimpleName(v438))(v415)
      case 238 =>
        val v439 = v415.asInstanceOf[SequenceNode].children(2)
        val BindNode(v440, v441) = v439
        assert(v440.id == 140)
        val v442 = v415.asInstanceOf[SequenceNode].children(6)
        val BindNode(v443, v444) = v442
        assert(v443.id == 123)
        val v445 = v415.asInstanceOf[SequenceNode].children(7)
        val BindNode(v446, v447) = v445
        assert(v446.id == 131)
        val BindNode(v448, v449) = v447
        val v458 = v448.id match {
        case 138 =>
        None
        case 132 =>
          val BindNode(v450, v451) = v449
          val v457 = v450.id match {
          case 133 =>
            val BindNode(v452, v453) = v451
            assert(v452.id == 134)
            val v454 = v453.asInstanceOf[SequenceNode].children(3)
            val BindNode(v455, v456) = v454
            assert(v455.id == 87)
            matchSimpleName(v456)
        }
          Some(v457)
      }
        ImportFrom(matchImportSourceExpr(v441), matchName(v444), v458)(v415)
    }
    v459
  }

  def matchImportSourceExpr(node: Node): ImportSourceExpr = {
    val BindNode(v460, v461) = node
    val v468 = v460.id match {
      case 141 =>
        val v462 = v461.asInstanceOf[SequenceNode].children.head
        val BindNode(v463, v464) = v462
        assert(v463.id == 142)
        matchStringLiteral(v464)
      case 173 =>
        val v465 = v461.asInstanceOf[SequenceNode].children.head
        val BindNode(v466, v467) = v465
        assert(v466.id == 174)
        matchCallExpr(v467)
    }
    v468
  }

  def matchLiteral(node: Node): Literal = {
    val BindNode(v469, v470) = node
    val v477 = v469.id match {
      case 141 =>
        val v471 = v470.asInstanceOf[SequenceNode].children.head
        val BindNode(v472, v473) = v471
        assert(v472.id == 142)
        matchStringLiteral(v473)
      case 230 =>
        val v474 = v470.asInstanceOf[SequenceNode].children.head
        val BindNode(v475, v476) = v474
        assert(v475.id == 231)
        matchBooleanLiteral(v476)
    }
    v477
  }

  def matchMethodRef(node: Node): MethodRef = {
    val BindNode(v478, v479) = node
    val v500 = v478.id match {
      case 382 =>
        val v480 = v479.asInstanceOf[SequenceNode].children.head
        val BindNode(v481, v482) = v480
        assert(v481.id == 123)
        val v483 = v479.asInstanceOf[SequenceNode].children(4)
        val BindNode(v484, v485) = v483
        assert(v484.id == 123)
        val v486 = v479.asInstanceOf[SequenceNode].children(5)
        val BindNode(v487, v488) = v486
        assert(v487.id == 383)
        val BindNode(v489, v490) = v488
        val v499 = v489.id match {
        case 138 =>
        None
        case 384 =>
          val BindNode(v491, v492) = v490
          val v498 = v491.id match {
          case 385 =>
            val BindNode(v493, v494) = v492
            assert(v493.id == 386)
            val v495 = v494.asInstanceOf[SequenceNode].children(3)
            val BindNode(v496, v497) = v495
            assert(v496.id == 87)
            matchSimpleName(v497)
        }
          Some(v498)
      }
        MethodRef(matchName(v482), matchName(v485), v499)(v479)
    }
    v500
  }

  def matchName(node: Node): Name = {
    val BindNode(v501, v502) = node
    val v518 = v501.id match {
      case 124 =>
        val v503 = v502.asInstanceOf[SequenceNode].children.head
        val BindNode(v504, v505) = v503
        assert(v504.id == 87)
        val v506 = v502.asInstanceOf[SequenceNode].children(1)
        val v507 = unrollRepeat0(v506).map { elem =>
        val BindNode(v508, v509) = elem
        assert(v508.id == 127)
        val BindNode(v510, v511) = v509
        val v517 = v510.id match {
        case 128 =>
          val BindNode(v512, v513) = v511
          assert(v512.id == 129)
          val v514 = v513.asInstanceOf[SequenceNode].children(3)
          val BindNode(v515, v516) = v514
          assert(v515.id == 87)
          matchSimpleName(v516)
      }
        v517
        }
        Name(List(matchSimpleName(v505)) ++ v507)(v502)
    }
    v518
  }

  def matchNameDef(node: Node): NameDef = {
    val BindNode(v519, v520) = node
    val v527 = v519.id match {
      case 197 =>
        val v521 = v520.asInstanceOf[SequenceNode].children.head
        val BindNode(v522, v523) = v521
        assert(v522.id == 87)
        val v524 = v520.asInstanceOf[SequenceNode].children(4)
        val BindNode(v525, v526) = v524
        assert(v525.id == 169)
        NameDef(matchSimpleName(v523), matchExpr(v526))(v520)
    }
    v527
  }

  def matchNamedExpr(node: Node): NamedExpr = {
    val BindNode(v528, v529) = node
    val v536 = v528.id match {
      case 221 =>
        val v530 = v529.asInstanceOf[SequenceNode].children.head
        val BindNode(v531, v532) = v530
        assert(v531.id == 87)
        val v533 = v529.asInstanceOf[SequenceNode].children(4)
        val BindNode(v534, v535) = v533
        assert(v534.id == 169)
        NamedExpr(matchSimpleName(v532), matchExpr(v535))(v529)
    }
    v536
  }

  def matchNamedParam(node: Node): NamedParam = {
    val BindNode(v537, v538) = node
    val v545 = v537.id match {
      case 197 =>
        val v539 = v538.asInstanceOf[SequenceNode].children.head
        val BindNode(v540, v541) = v539
        assert(v540.id == 87)
        val v542 = v538.asInstanceOf[SequenceNode].children(4)
        val BindNode(v543, v544) = v542
        assert(v543.id == 169)
        NamedParam(matchSimpleName(v541), matchExpr(v544))(v538)
    }
    v545
  }

  def matchNamedParams(node: Node): List[NamedParam] = {
    val BindNode(v546, v547) = node
    val v563 = v546.id match {
      case 195 =>
        val v548 = v547.asInstanceOf[SequenceNode].children.head
        val BindNode(v549, v550) = v548
        assert(v549.id == 196)
        val v551 = v547.asInstanceOf[SequenceNode].children(1)
        val v552 = unrollRepeat0(v551).map { elem =>
        val BindNode(v553, v554) = elem
        assert(v553.id == 201)
        val BindNode(v555, v556) = v554
        val v562 = v555.id match {
        case 202 =>
          val BindNode(v557, v558) = v556
          assert(v557.id == 203)
          val v559 = v558.asInstanceOf[SequenceNode].children(3)
          val BindNode(v560, v561) = v559
          assert(v560.id == 196)
          matchNamedParam(v561)
      }
        v562
        }
        List(matchNamedParam(v550)) ++ v552
    }
    v563
  }

  def matchNamedTupleType(node: Node): NamedTupleType = {
    val BindNode(v564, v565) = node
    val v581 = v564.id match {
      case 294 =>
        val v566 = v565.asInstanceOf[SequenceNode].children(2)
        val BindNode(v567, v568) = v566
        assert(v567.id == 295)
        val v569 = v565.asInstanceOf[SequenceNode].children(3)
        val v570 = unrollRepeat0(v569).map { elem =>
        val BindNode(v571, v572) = elem
        assert(v571.id == 298)
        val BindNode(v573, v574) = v572
        val v580 = v573.id match {
        case 299 =>
          val BindNode(v575, v576) = v574
          assert(v575.id == 300)
          val v577 = v576.asInstanceOf[SequenceNode].children(3)
          val BindNode(v578, v579) = v577
          assert(v578.id == 295)
          matchNamedType(v579)
      }
        v580
        }
        NamedTupleType(List(matchNamedType(v568)) ++ v570)(v565)
    }
    v581
  }

  def matchNamedType(node: Node): NamedType = {
    val BindNode(v582, v583) = node
    val v590 = v582.id match {
      case 257 =>
        val v584 = v583.asInstanceOf[SequenceNode].children.head
        val BindNode(v585, v586) = v584
        assert(v585.id == 87)
        val v587 = v583.asInstanceOf[SequenceNode].children(4)
        val BindNode(v588, v589) = v587
        assert(v588.id == 258)
        NamedType(matchSimpleName(v586), matchTypeExpr(v589))(v583)
    }
    v590
  }

  def matchNamedTypeParam(node: Node): NamedTypeParam = {
    val BindNode(v591, v592) = node
    val v599 = v591.id match {
      case 257 =>
        val v593 = v592.asInstanceOf[SequenceNode].children.head
        val BindNode(v594, v595) = v593
        assert(v594.id == 87)
        val v596 = v592.asInstanceOf[SequenceNode].children(4)
        val BindNode(v597, v598) = v596
        assert(v597.id == 258)
        NamedTypeParam(matchSimpleName(v595), matchTypeExpr(v598))(v592)
    }
    v599
  }

  def matchNamedTypeParams(node: Node): List[NamedTypeParam] = {
    val BindNode(v600, v601) = node
    val v617 = v600.id match {
      case 255 =>
        val v602 = v601.asInstanceOf[SequenceNode].children(2)
        val BindNode(v603, v604) = v602
        assert(v603.id == 256)
        val v605 = v601.asInstanceOf[SequenceNode].children(3)
        val v606 = unrollRepeat0(v605).map { elem =>
        val BindNode(v607, v608) = elem
        assert(v607.id == 311)
        val BindNode(v609, v610) = v608
        val v616 = v609.id match {
        case 312 =>
          val BindNode(v611, v612) = v610
          assert(v611.id == 313)
          val v613 = v612.asInstanceOf[SequenceNode].children(3)
          val BindNode(v614, v615) = v613
          assert(v614.id == 256)
          matchNamedTypeParam(v615)
      }
        v616
        }
        List(matchNamedTypeParam(v604)) ++ v606
    }
    v617
  }

  def matchNamespaceDef(node: Node): NamespaceDef = {
    val BindNode(v618, v619) = node
    val v626 = v618.id match {
      case 59 =>
        val v620 = v619.asInstanceOf[SequenceNode].children(2)
        val BindNode(v621, v622) = v620
        assert(v621.id == 87)
        val v623 = v619.asInstanceOf[SequenceNode].children(5)
        val BindNode(v624, v625) = v623
        assert(v624.id == 2)
        NamespaceDef(matchSimpleName(v622), matchBuildScript(v625))(v619)
    }
    v626
  }

  def matchNoUnionType(node: Node): NoUnionType = {
    val BindNode(v627, v628) = node
    val v644 = v627.id match {
      case 261 =>
        val v629 = v628.asInstanceOf[SequenceNode].children.head
        val BindNode(v630, v631) = v629
        assert(v630.id == 123)
        matchName(v631)
      case 267 =>
        val v632 = v628.asInstanceOf[SequenceNode].children.head
        val BindNode(v633, v634) = v632
        assert(v633.id == 268)
        matchCollectionType(v634)
      case 289 =>
        val v635 = v628.asInstanceOf[SequenceNode].children.head
        val BindNode(v636, v637) = v635
        assert(v636.id == 290)
        matchTupleType(v637)
      case 262 =>
        val v638 = v628.asInstanceOf[SequenceNode].children.head
        val BindNode(v639, v640) = v638
        assert(v639.id == 263)
        matchParameterizedClassType(v640)
      case 292 =>
        val v641 = v628.asInstanceOf[SequenceNode].children.head
        val BindNode(v642, v643) = v641
        assert(v642.id == 293)
        matchNamedTupleType(v643)
    }
    v644
  }

  def matchParamDef(node: Node): ParamDef = {
    val BindNode(v645, v646) = node
    val v687 = v645.id match {
      case 366 =>
        val v647 = v646.asInstanceOf[SequenceNode].children.head
        val BindNode(v648, v649) = v647
        assert(v648.id == 87)
        val v650 = v646.asInstanceOf[SequenceNode].children(1)
        val BindNode(v651, v652) = v650
        assert(v651.id == 367)
        val BindNode(v653, v654) = v652
        val v663 = v653.id match {
        case 138 =>
        None
        case 368 =>
          val BindNode(v655, v656) = v654
          val v662 = v655.id match {
          case 369 =>
            val BindNode(v657, v658) = v656
            assert(v657.id == 370)
            val v659 = v658.asInstanceOf[SequenceNode].children(1)
            val BindNode(v660, v661) = v659
            assert(v660.id == 371)
            v661.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
        }
          Some(v662)
      }
        val v664 = v646.asInstanceOf[SequenceNode].children(5)
        val BindNode(v665, v666) = v664
        assert(v665.id == 258)
        val v667 = v646.asInstanceOf[SequenceNode].children(6)
        val BindNode(v668, v669) = v667
        assert(v668.id == 372)
        val BindNode(v670, v671) = v669
        val v680 = v670.id match {
        case 138 =>
        None
        case 373 =>
          val BindNode(v672, v673) = v671
          val v679 = v672.id match {
          case 374 =>
            val BindNode(v674, v675) = v673
            assert(v674.id == 375)
            val v676 = v675.asInstanceOf[SequenceNode].children(3)
            val BindNode(v677, v678) = v676
            assert(v677.id == 169)
            matchExpr(v678)
        }
          Some(v679)
      }
        ParamDef(matchSimpleName(v649), v663.isDefined, Some(matchTypeExpr(v666)), v680)(v646)
      case 197 =>
        val v681 = v646.asInstanceOf[SequenceNode].children.head
        val BindNode(v682, v683) = v681
        assert(v682.id == 87)
        val v684 = v646.asInstanceOf[SequenceNode].children(4)
        val BindNode(v685, v686) = v684
        assert(v685.id == 169)
        ParamDef(matchSimpleName(v683), false, None, Some(matchExpr(v686)))(v646)
    }
    v687
  }

  def matchParameterizedClassType(node: Node): ParameterizedClassType = {
    val BindNode(v688, v689) = node
    val v696 = v688.id match {
      case 264 =>
        val v690 = v689.asInstanceOf[SequenceNode].children.head
        val BindNode(v691, v692) = v690
        assert(v691.id == 123)
        val v693 = v689.asInstanceOf[SequenceNode].children(2)
        val BindNode(v694, v695) = v693
        assert(v694.id == 265)
        ParameterizedClassType(matchName(v692), matchClassTypeParams(v695))(v689)
    }
    v696
  }

  def matchParamsDef(node: Node): List[ParamDef] = {
    val BindNode(v697, v698) = node
    val v725 = v697.id match {
      case 360 =>
        val v700 = v698.asInstanceOf[SequenceNode].children(1)
        val BindNode(v701, v702) = v700
        assert(v701.id == 361)
        val BindNode(v703, v704) = v702
        val v724 = v703.id match {
        case 138 =>
        None
        case 362 =>
          val BindNode(v705, v706) = v704
          assert(v705.id == 363)
          val BindNode(v707, v708) = v706
          assert(v707.id == 364)
          val v709 = v708.asInstanceOf[SequenceNode].children(1)
          val BindNode(v710, v711) = v709
          assert(v710.id == 365)
          val v712 = v708.asInstanceOf[SequenceNode].children(2)
          val v713 = unrollRepeat0(v712).map { elem =>
          val BindNode(v714, v715) = elem
          assert(v714.id == 378)
          val BindNode(v716, v717) = v715
          val v723 = v716.id match {
          case 379 =>
            val BindNode(v718, v719) = v717
            assert(v718.id == 380)
            val v720 = v719.asInstanceOf[SequenceNode].children(3)
            val BindNode(v721, v722) = v720
            assert(v721.id == 365)
            matchParamDef(v722)
        }
          v723
          }
          Some(List(matchParamDef(v711)) ++ v713)
      }
        val v699 = v724
        if (v699.isDefined) v699.get else List()
    }
    v725
  }

  def matchPositionalParams(node: Node): List[Expr] = {
    val BindNode(v726, v727) = node
    val v743 = v726.id match {
      case 182 =>
        val v728 = v727.asInstanceOf[SequenceNode].children.head
        val BindNode(v729, v730) = v728
        assert(v729.id == 169)
        val v731 = v727.asInstanceOf[SequenceNode].children(1)
        val v732 = unrollRepeat0(v731).map { elem =>
        val BindNode(v733, v734) = elem
        assert(v733.id == 185)
        val BindNode(v735, v736) = v734
        val v742 = v735.id match {
        case 186 =>
          val BindNode(v737, v738) = v736
          assert(v737.id == 187)
          val v739 = v738.asInstanceOf[SequenceNode].children(3)
          val BindNode(v740, v741) = v739
          assert(v740.id == 169)
          matchExpr(v741)
      }
        v742
        }
        List(matchExpr(v730)) ++ v732
    }
    v743
  }

  def matchPrimary(node: Node): Primary = {
    val BindNode(v744, v745) = node
    val v839 = v744.id match {
      case 173 =>
        val v746 = v745.asInstanceOf[SequenceNode].children.head
        val BindNode(v747, v748) = v746
        assert(v747.id == 174)
        matchCallExpr(v748)
      case 206 =>
        val v749 = v745.asInstanceOf[SequenceNode].children.head
        val BindNode(v750, v751) = v749
        assert(v750.id == 87)
        NameRef(matchSimpleName(v751))(v745)
      case 207 =>
        val v753 = v745.asInstanceOf[SequenceNode].children(1)
        val BindNode(v754, v755) = v753
        assert(v754.id == 209)
        val BindNode(v756, v757) = v755
        val v777 = v756.id match {
        case 138 =>
        None
        case 210 =>
          val BindNode(v758, v759) = v757
          assert(v758.id == 211)
          val BindNode(v760, v761) = v759
          assert(v760.id == 212)
          val v762 = v761.asInstanceOf[SequenceNode].children(1)
          val BindNode(v763, v764) = v762
          assert(v763.id == 169)
          val v765 = v761.asInstanceOf[SequenceNode].children(2)
          val v766 = unrollRepeat0(v765).map { elem =>
          val BindNode(v767, v768) = elem
          assert(v767.id == 185)
          val BindNode(v769, v770) = v768
          val v776 = v769.id match {
          case 186 =>
            val BindNode(v771, v772) = v770
            assert(v771.id == 187)
            val v773 = v772.asInstanceOf[SequenceNode].children(3)
            val BindNode(v774, v775) = v773
            assert(v774.id == 169)
            matchExpr(v775)
        }
          v776
          }
          Some(List(matchExpr(v764)) ++ v766)
      }
        val v752 = v777
        ListExpr(if (v752.isDefined) v752.get else List())(v745)
      case 228 =>
        val v778 = v745.asInstanceOf[SequenceNode].children.head
        val BindNode(v779, v780) = v778
        assert(v779.id == 229)
        matchLiteral(v780)
      case 205 =>
        val v781 = v745.asInstanceOf[SequenceNode].children.head
        val BindNode(v782, v783) = v781
        assert(v782.id == 172)
        val v784 = v745.asInstanceOf[SequenceNode].children(4)
        val BindNode(v785, v786) = v784
        assert(v785.id == 87)
        MemberAccess(matchPrimary(v783), matchSimpleName(v786))(v745)
      case 214 =>
        val v788 = v745.asInstanceOf[SequenceNode].children(1)
        val BindNode(v789, v790) = v788
        assert(v789.id == 209)
        val BindNode(v791, v792) = v790
        val v812 = v791.id match {
        case 138 =>
        None
        case 210 =>
          val BindNode(v793, v794) = v792
          assert(v793.id == 211)
          val BindNode(v795, v796) = v794
          assert(v795.id == 212)
          val v797 = v796.asInstanceOf[SequenceNode].children(1)
          val BindNode(v798, v799) = v797
          assert(v798.id == 169)
          val v800 = v796.asInstanceOf[SequenceNode].children(2)
          val v801 = unrollRepeat0(v800).map { elem =>
          val BindNode(v802, v803) = elem
          assert(v802.id == 185)
          val BindNode(v804, v805) = v803
          val v811 = v804.id match {
          case 186 =>
            val BindNode(v806, v807) = v805
            assert(v806.id == 187)
            val v808 = v807.asInstanceOf[SequenceNode].children(3)
            val BindNode(v809, v810) = v808
            assert(v809.id == 169)
            matchExpr(v810)
        }
          v811
          }
          Some(List(matchExpr(v799)) ++ v801)
      }
        val v787 = v812
        TupleExpr(if (v787.isDefined) v787.get else List())(v745)
      case 215 =>
        val v814 = v745.asInstanceOf[SequenceNode].children(1)
        val BindNode(v815, v816) = v814
        assert(v815.id == 216)
        val BindNode(v817, v818) = v816
        val v838 = v817.id match {
        case 138 =>
        None
        case 217 =>
          val BindNode(v819, v820) = v818
          assert(v819.id == 218)
          val BindNode(v821, v822) = v820
          assert(v821.id == 219)
          val v823 = v822.asInstanceOf[SequenceNode].children(1)
          val BindNode(v824, v825) = v823
          assert(v824.id == 220)
          val v826 = v822.asInstanceOf[SequenceNode].children(2)
          val v827 = unrollRepeat0(v826).map { elem =>
          val BindNode(v828, v829) = elem
          assert(v828.id == 225)
          val BindNode(v830, v831) = v829
          val v837 = v830.id match {
          case 226 =>
            val BindNode(v832, v833) = v831
            assert(v832.id == 227)
            val v834 = v833.asInstanceOf[SequenceNode].children(3)
            val BindNode(v835, v836) = v834
            assert(v835.id == 220)
            matchNamedExpr(v836)
        }
          v837
          }
          Some(List(matchNamedExpr(v825)) ++ v827)
      }
        val v813 = v838
        NamedTupleExpr(if (v813.isDefined) v813.get else List())(v745)
    }
    v839
  }

  def matchSimpleName(node: Node): String = {
    val BindNode(v840, v841) = node
    val v872 = v840.id match {
      case 88 =>
        val v842 = v841.asInstanceOf[SequenceNode].children.head
        val BindNode(v843, v844) = v842
        assert(v843.id == 89)
        val BindNode(v845, v846) = v844
        assert(v845.id == 90)
        val BindNode(v847, v848) = v846
        assert(v847.id == 91)
        val BindNode(v849, v850) = v848
        val v871 = v849.id match {
        case 92 =>
          val BindNode(v851, v852) = v850
          assert(v851.id == 93)
          val v853 = v852.asInstanceOf[SequenceNode].children.head
          val BindNode(v854, v855) = v853
          assert(v854.id == 94)
          val JoinNode(_, v856, _) = v855
          val BindNode(v857, v858) = v856
          assert(v857.id == 95)
          val BindNode(v859, v860) = v858
          val v870 = v859.id match {
          case 96 =>
            val BindNode(v861, v862) = v860
            assert(v861.id == 97)
            val v863 = v862.asInstanceOf[SequenceNode].children.head
            val BindNode(v864, v865) = v863
            assert(v864.id == 98)
            val v866 = v862.asInstanceOf[SequenceNode].children(1)
            val v867 = unrollRepeat0(v866).map { elem =>
            val BindNode(v868, v869) = elem
            assert(v868.id == 77)
            v869.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
            }
            v865.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char.toString + v867.map(x => x.toString).mkString("")
        }
          v870
      }
        v871
    }
    v872
  }

  def matchStringElem(node: Node): StringElem = {
    val BindNode(v873, v874) = node
    val v886 = v873.id match {
      case 152 =>
        val v875 = v874.asInstanceOf[SequenceNode].children.head
        val BindNode(v876, v877) = v875
        assert(v876.id == 153)
        val BindNode(v878, v879) = v877
        assert(v878.id == 30)
        JustChar(v879.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char)(v874)
      case 155 =>
        val v880 = v874.asInstanceOf[SequenceNode].children.head
        val BindNode(v881, v882) = v880
        assert(v881.id == 156)
        matchEscapeChar(v882)
      case 160 =>
        val v883 = v874.asInstanceOf[SequenceNode].children.head
        val BindNode(v884, v885) = v883
        assert(v884.id == 161)
        matchStringExpr(v885)
    }
    v886
  }

  def matchStringExpr(node: Node): StringExpr = {
    val BindNode(v887, v888) = node
    val v905 = v887.id match {
      case 162 =>
        val v889 = v888.asInstanceOf[SequenceNode].children.head
        val BindNode(v890, v891) = v889
        assert(v890.id == 163)
        val BindNode(v892, v893) = v891
        assert(v892.id == 164)
        val BindNode(v894, v895) = v893
        val v901 = v894.id match {
        case 165 =>
          val BindNode(v896, v897) = v895
          assert(v896.id == 166)
          val v898 = v897.asInstanceOf[SequenceNode].children(1)
          val BindNode(v899, v900) = v898
          assert(v899.id == 87)
          matchSimpleName(v900)
      }
        SimpleExpr(v901)(v888)
      case 168 =>
        val v902 = v888.asInstanceOf[SequenceNode].children(3)
        val BindNode(v903, v904) = v902
        assert(v903.id == 169)
        ComplexExpr(matchExpr(v904))(v888)
    }
    v905
  }

  def matchStringLiteral(node: Node): StringLiteral = {
    val BindNode(v906, v907) = node
    val v922 = v906.id match {
      case 143 =>
        val v908 = v907.asInstanceOf[SequenceNode].children(1)
        val v909 = unrollRepeat0(v908).map { elem =>
        val BindNode(v910, v911) = elem
        assert(v910.id == 147)
        val BindNode(v912, v913) = v911
        assert(v912.id == 148)
        val BindNode(v914, v915) = v913
        val v921 = v914.id match {
        case 149 =>
          val BindNode(v916, v917) = v915
          assert(v916.id == 150)
          val v918 = v917.asInstanceOf[SequenceNode].children.head
          val BindNode(v919, v920) = v918
          assert(v919.id == 151)
          matchStringElem(v920)
      }
        v921
        }
        StringLiteral(v909)(v907)
    }
    v922
  }

  def matchTupleType(node: Node): TupleType = {
    val BindNode(v923, v924) = node
    val v940 = v923.id match {
      case 291 =>
        val v925 = v924.asInstanceOf[SequenceNode].children(2)
        val BindNode(v926, v927) = v925
        assert(v926.id == 258)
        val v928 = v924.asInstanceOf[SequenceNode].children(3)
        val v929 = unrollRepeat0(v928).map { elem =>
        val BindNode(v930, v931) = elem
        assert(v930.id == 285)
        val BindNode(v932, v933) = v931
        val v939 = v932.id match {
        case 286 =>
          val BindNode(v934, v935) = v933
          assert(v934.id == 287)
          val v936 = v935.asInstanceOf[SequenceNode].children(3)
          val BindNode(v937, v938) = v936
          assert(v937.id == 258)
          matchTypeExpr(v938)
      }
        v939
        }
        TupleType(List(matchTypeExpr(v927)) ++ v929)(v924)
    }
    v940
  }

  def matchTypeExpr(node: Node): TypeExpr = {
    val BindNode(v941, v942) = node
    val v949 = v941.id match {
      case 259 =>
        val v943 = v942.asInstanceOf[SequenceNode].children.head
        val BindNode(v944, v945) = v943
        assert(v944.id == 260)
        matchNoUnionType(v945)
      case 301 =>
        val v946 = v942.asInstanceOf[SequenceNode].children.head
        val BindNode(v947, v948) = v946
        assert(v947.id == 302)
        matchUnionType(v948)
    }
    v949
  }

  def matchTypeParams(node: Node): TypeParams = {
    val BindNode(v950, v951) = node
    val v967 = v950.id match {
      case 281 =>
        val v952 = v951.asInstanceOf[SequenceNode].children(2)
        val BindNode(v953, v954) = v952
        assert(v953.id == 258)
        val v955 = v951.asInstanceOf[SequenceNode].children(3)
        val v956 = unrollRepeat0(v955).map { elem =>
        val BindNode(v957, v958) = elem
        assert(v957.id == 285)
        val BindNode(v959, v960) = v958
        val v966 = v959.id match {
        case 286 =>
          val BindNode(v961, v962) = v960
          assert(v961.id == 287)
          val v963 = v962.asInstanceOf[SequenceNode].children(3)
          val BindNode(v964, v965) = v963
          assert(v964.id == 258)
          matchTypeExpr(v965)
      }
        v966
        }
        TypeParams(List(matchTypeExpr(v954)) ++ v956)(v951)
    }
    v967
  }

  def matchUnionType(node: Node): UnionType = {
    val BindNode(v968, v969) = node
    val v985 = v968.id match {
      case 303 =>
        val v970 = v969.asInstanceOf[SequenceNode].children(2)
        val BindNode(v971, v972) = v970
        assert(v971.id == 260)
        val v973 = v969.asInstanceOf[SequenceNode].children(3)
        val v974 = unrollRepeat0(v973).map { elem =>
        val BindNode(v975, v976) = elem
        assert(v975.id == 306)
        val BindNode(v977, v978) = v976
        val v984 = v977.id match {
        case 307 =>
          val BindNode(v979, v980) = v978
          assert(v979.id == 308)
          val v981 = v980.asInstanceOf[SequenceNode].children(3)
          val BindNode(v982, v983) = v981
          assert(v982.id == 260)
          matchNoUnionType(v983)
      }
        v984
        }
        UnionType(List(matchNoUnionType(v972)) ++ v974)(v969)
    }
    v985
  }

  def matchStart(node: Node): BuildScript = {
    val BindNode(start, BindNode(_, body)) = node
    assert(start.id == 1)
    matchBuildScript(body)
  }

    val milestoneParserData = MilestoneParserProtobufConverter.convertProtoToMilestoneParserData(
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
