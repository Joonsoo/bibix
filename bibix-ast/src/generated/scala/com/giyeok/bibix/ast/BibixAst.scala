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

  sealed trait ClassDef extends Def with WithParseNode

  case class ClassField(name: String, optional: Boolean, typ: TypeExpr)(override val parseNode: Node) extends WithParseNode

  case class CollectionType(name: String, typeParams: TypeParams)(override val parseNode: Node) extends NoUnionType with WithParseNode

  case class ComplexExpr(expr: Expr)(override val parseNode: Node) extends StringExpr with WithParseNode

  case class DataClassDef(name: String, fields: List[ClassField], body: List[ClassBodyElem])(override val parseNode: Node) extends ClassDef with WithParseNode

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

  case class SuperClassDef(name: String, subs: List[String])(override val parseNode: Node) extends ClassDef with WithParseNode

  case class TupleExpr(elems: List[Expr])(override val parseNode: Node) extends Primary with WithParseNode

  case class TupleType(elems: List[TypeExpr])(override val parseNode: Node) extends NoUnionType with WithParseNode

  sealed trait TypeExpr extends WithParseNode

  case class TypeParams(params: List[TypeExpr])(override val parseNode: Node) extends WithParseNode

  case class UnionType(elems: List[NoUnionType])(override val parseNode: Node) extends TypeExpr with WithParseNode


  def matchActionDef(node: Node): ActionDef = {
    val BindNode(v1, v2) = node
    val v23 = v1.id match {
      case 407 =>
        val v3 = v2.asInstanceOf[SequenceNode].children(2)
        val BindNode(v4, v5) = v3
        assert(v4.id == 87)
        val v6 = v2.asInstanceOf[SequenceNode].children(3)
        val BindNode(v7, v8) = v6
        assert(v7.id == 408)
        val BindNode(v9, v10) = v8
        val v19 = v9.id match {
          case 141 =>
            None
          case 409 =>
            val BindNode(v11, v12) = v10
            val v18 = v11.id match {
              case 410 =>
                val BindNode(v13, v14) = v12
                assert(v13.id == 411)
                val v15 = v14.asInstanceOf[SequenceNode].children(1)
                val BindNode(v16, v17) = v15
                assert(v16.id == 412)
                matchActionParams(v17)
            }
            Some(v18)
        }
        val v20 = v2.asInstanceOf[SequenceNode].children(7)
        val BindNode(v21, v22) = v20
        assert(v21.id == 414)
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
      case 413 =>
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
        assert(v42.id == 350)
        val v44 = v37.asInstanceOf[SequenceNode].children(10)
        val BindNode(v45, v46) = v44
        assert(v45.id == 367)
        ActionRuleDef(matchSimpleName(v40), matchParamsDef(v43), matchMethodRef(v46))(v37)
    }
    v47
  }

  def matchArgDef(node: Node): ArgDef = {
    val BindNode(v48, v49) = node
    val v94 = v48.id match {
      case 389 =>
        val v50 = v49.asInstanceOf[SequenceNode].children(1)
        val BindNode(v51, v52) = v50
        assert(v51.id == 394)
        val BindNode(v53, v54) = v52
        val v62 = v53.id match {
          case 141 =>
            None
          case 395 =>
            val BindNode(v55, v56) = v54
            assert(v55.id == 396)
            val BindNode(v57, v58) = v56
            assert(v57.id == 397)
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
        assert(v67.id == 398)
        val BindNode(v69, v70) = v68
        val v79 = v69.id match {
          case 141 =>
            None
          case 399 =>
            val BindNode(v71, v72) = v70
            val v78 = v71.id match {
              case 400 =>
                val BindNode(v73, v74) = v72
                assert(v73.id == 401)
                val v75 = v74.asInstanceOf[SequenceNode].children(3)
                val BindNode(v76, v77) = v75
                assert(v76.id == 194)
                matchTypeExpr(v77)
            }
            Some(v78)
        }
        val v80 = v49.asInstanceOf[SequenceNode].children(5)
        val BindNode(v81, v82) = v80
        assert(v81.id == 358)
        val BindNode(v83, v84) = v82
        val v93 = v83.id match {
          case 141 =>
            None
          case 359 =>
            val BindNode(v85, v86) = v84
            val v92 = v85.id match {
              case 360 =>
                val BindNode(v87, v88) = v86
                assert(v87.id == 361)
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
      case 404 =>
        val v113 = v112.asInstanceOf[SequenceNode].children(2)
        val BindNode(v114, v115) = v113
        assert(v114.id == 87)
        val v116 = v112.asInstanceOf[SequenceNode].children(4)
        val BindNode(v117, v118) = v116
        assert(v117.id == 350)
        val v119 = v112.asInstanceOf[SequenceNode].children(8)
        val BindNode(v120, v121) = v119
        assert(v120.id == 194)
        val v122 = v112.asInstanceOf[SequenceNode].children(12)
        val BindNode(v123, v124) = v122
        assert(v123.id == 367)
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
      case 373 =>
        val v176 = v172.asInstanceOf[SequenceNode].children.head
        val BindNode(v177, v178) = v176
        assert(v177.id == 374)
        matchClassCastDef(v178)
    }
    v179
  }

  def matchClassCastDef(node: Node): ClassCastDef = {
    val BindNode(v180, v181) = node
    val v188 = v180.id match {
      case 375 =>
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
    val v197 = v189.id match {
      case 304 =>
        val v191 = v190.asInstanceOf[SequenceNode].children.head
        val BindNode(v192, v193) = v191
        assert(v192.id == 305)
        matchDataClassDef(v193)
      case 376 =>
        val v194 = v190.asInstanceOf[SequenceNode].children.head
        val BindNode(v195, v196) = v194
        assert(v195.id == 377)
        matchSuperClassDef(v196)
    }
    v197
  }

  def matchClassFieldDef(node: Node): ClassField = {
    val BindNode(v198, v199) = node
    val v220 = v198.id match {
      case 317 =>
        val v200 = v199.asInstanceOf[SequenceNode].children.head
        val BindNode(v201, v202) = v200
        assert(v201.id == 87)
        val v203 = v199.asInstanceOf[SequenceNode].children(1)
        val BindNode(v204, v205) = v203
        assert(v204.id == 318)
        val BindNode(v206, v207) = v205
        val v216 = v206.id match {
          case 141 =>
            None
          case 319 =>
            val BindNode(v208, v209) = v207
            val v215 = v208.id match {
              case 320 =>
                val BindNode(v210, v211) = v209
                assert(v210.id == 321)
                val v212 = v211.asInstanceOf[SequenceNode].children(1)
                val BindNode(v213, v214) = v212
                assert(v213.id == 322)
                v214.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
            }
            Some(v215)
        }
        val v217 = v199.asInstanceOf[SequenceNode].children(5)
        val BindNode(v218, v219) = v217
        assert(v218.id == 194)
        ClassField(matchSimpleName(v202), v216.isDefined, matchTypeExpr(v219))(v199)
    }
    v220
  }

  def matchClassFields(node: Node): List[ClassField] = {
    val BindNode(v221, v222) = node
    val v238 = v221.id match {
      case 315 =>
        val v223 = v222.asInstanceOf[SequenceNode].children.head
        val BindNode(v224, v225) = v223
        assert(v224.id == 316)
        val v226 = v222.asInstanceOf[SequenceNode].children(1)
        val v227 = unrollRepeat0(v226).map { elem =>
          val BindNode(v228, v229) = elem
          assert(v228.id == 325)
          val BindNode(v230, v231) = v229
          val v237 = v230.id match {
            case 326 =>
              val BindNode(v232, v233) = v231
              assert(v232.id == 327)
              val v234 = v233.asInstanceOf[SequenceNode].children(3)
              val BindNode(v235, v236) = v234
              assert(v235.id == 316)
              matchClassFieldDef(v236)
          }
          v237
        }
        List(matchClassFieldDef(v225)) ++ v227
    }
    v238
  }

  def matchCollectionType(node: Node): CollectionType = {
    val BindNode(v239, v240) = node
    val v259 = v239.id match {
      case 180 =>
        val v241 = v240.asInstanceOf[SequenceNode].children.head
        val BindNode(v242, v243) = v241
        assert(v242.id == 181)
        val JoinNode(_, v244, _) = v243
        val BindNode(v245, v246) = v244
        assert(v245.id == 182)
        val BindNode(v247, v248) = v246
        val v255 = v247.id match {
          case 183 =>
            val BindNode(v249, v250) = v248
            assert(v249.id == 184)
            val v251 = v250.asInstanceOf[SequenceNode].children.head
            "set"
          case 187 =>
            val BindNode(v252, v253) = v248
            assert(v252.id == 188)
            val v254 = v253.asInstanceOf[SequenceNode].children.head
            "list"
        }
        val v256 = v240.asInstanceOf[SequenceNode].children(2)
        val BindNode(v257, v258) = v256
        assert(v257.id == 191)
        CollectionType(v255, matchTypeParams(v258))(v240)
    }
    v259
  }

  def matchDataClassDef(node: Node): DataClassDef = {
    val BindNode(v260, v261) = node
    val v295 = v260.id match {
      case 306 =>
        val v262 = v261.asInstanceOf[SequenceNode].children(2)
        val BindNode(v263, v264) = v262
        assert(v263.id == 87)
        val v266 = v261.asInstanceOf[SequenceNode].children(5)
        val BindNode(v267, v268) = v266
        assert(v267.id == 310)
        val BindNode(v269, v270) = v268
        val v279 = v269.id match {
          case 141 =>
            None
          case 311 =>
            val BindNode(v271, v272) = v270
            val v278 = v271.id match {
              case 312 =>
                val BindNode(v273, v274) = v272
                assert(v273.id == 313)
                val v275 = v274.asInstanceOf[SequenceNode].children(1)
                val BindNode(v276, v277) = v275
                assert(v276.id == 314)
                matchClassFields(v277)
            }
            Some(v278)
        }
        val v265 = v279
        val v281 = v261.asInstanceOf[SequenceNode].children(8)
        val BindNode(v282, v283) = v281
        assert(v282.id == 328)
        val BindNode(v284, v285) = v283
        val v294 = v284.id match {
          case 141 =>
            None
          case 329 =>
            val BindNode(v286, v287) = v285
            val v293 = v286.id match {
              case 330 =>
                val BindNode(v288, v289) = v287
                assert(v288.id == 331)
                val v290 = v289.asInstanceOf[SequenceNode].children(1)
                val BindNode(v291, v292) = v290
                assert(v291.id == 332)
                matchClassBody(v292)
            }
            Some(v293)
        }
        val v280 = v294
        DataClassDef(matchSimpleName(v264), if (v265.isDefined) v265.get else List(), if (v280.isDefined) v280.get else List())(v261)
    }
    v295
  }

  def matchDef(node: Node): Def = {
    val BindNode(v296, v297) = node
    val v325 = v296.id match {
      case 57 =>
        val v298 = v297.asInstanceOf[SequenceNode].children.head
        val BindNode(v299, v300) = v298
        assert(v299.id == 58)
        matchNamespaceDef(v300)
      case 402 =>
        val v301 = v297.asInstanceOf[SequenceNode].children.head
        val BindNode(v302, v303) = v301
        assert(v302.id == 403)
        matchBuildRuleDef(v303)
      case 119 =>
        val v304 = v297.asInstanceOf[SequenceNode].children.head
        val BindNode(v305, v306) = v304
        assert(v305.id == 120)
        matchImportDef(v306)
      case 340 =>
        val v307 = v297.asInstanceOf[SequenceNode].children.head
        val BindNode(v308, v309) = v307
        assert(v308.id == 341)
        matchActionRuleDef(v309)
      case 302 =>
        val v310 = v297.asInstanceOf[SequenceNode].children.head
        val BindNode(v311, v312) = v310
        assert(v311.id == 303)
        matchClassDef(v312)
      case 415 =>
        val v313 = v297.asInstanceOf[SequenceNode].children.head
        val BindNode(v314, v315) = v313
        assert(v314.id == 416)
        matchEnumDef(v315)
      case 387 =>
        val v316 = v297.asInstanceOf[SequenceNode].children.head
        val BindNode(v317, v318) = v316
        assert(v317.id == 388)
        matchArgDef(v318)
      case 300 =>
        val v319 = v297.asInstanceOf[SequenceNode].children.head
        val BindNode(v320, v321) = v319
        assert(v320.id == 301)
        matchNameDef(v321)
      case 405 =>
        val v322 = v297.asInstanceOf[SequenceNode].children.head
        val BindNode(v323, v324) = v322
        assert(v323.id == 406)
        matchActionDef(v324)
    }
    v325
  }

  def matchDefs(node: Node): List[Def] = {
    val BindNode(v326, v327) = node
    val v343 = v326.id match {
      case 55 =>
        val v328 = v327.asInstanceOf[SequenceNode].children.head
        val BindNode(v329, v330) = v328
        assert(v329.id == 56)
        val v331 = v327.asInstanceOf[SequenceNode].children(1)
        val v332 = unrollRepeat0(v331).map { elem =>
          val BindNode(v333, v334) = elem
          assert(v333.id == 423)
          val BindNode(v335, v336) = v334
          val v342 = v335.id match {
            case 424 =>
              val BindNode(v337, v338) = v336
              assert(v337.id == 425)
              val v339 = v338.asInstanceOf[SequenceNode].children(1)
              val BindNode(v340, v341) = v339
              assert(v340.id == 56)
              matchDef(v341)
          }
          v342
        }
        List(matchDef(v330)) ++ v332
    }
    v343
  }

  def matchEnumDef(node: Node): EnumDef = {
    val BindNode(v344, v345) = node
    val v364 = v344.id match {
      case 417 =>
        val v346 = v345.asInstanceOf[SequenceNode].children(2)
        val BindNode(v347, v348) = v346
        assert(v347.id == 87)
        val v349 = v345.asInstanceOf[SequenceNode].children(6)
        val BindNode(v350, v351) = v349
        assert(v350.id == 87)
        val v352 = v345.asInstanceOf[SequenceNode].children(7)
        val v353 = unrollRepeat0(v352).map { elem =>
          val BindNode(v354, v355) = elem
          assert(v354.id == 384)
          val BindNode(v356, v357) = v355
          val v363 = v356.id match {
            case 385 =>
              val BindNode(v358, v359) = v357
              assert(v358.id == 386)
              val v360 = v359.asInstanceOf[SequenceNode].children(3)
              val BindNode(v361, v362) = v360
              assert(v361.id == 87)
              matchSimpleName(v362)
          }
          v363
        }
        EnumDef(matchSimpleName(v348), List(matchSimpleName(v351)) ++ v353)(v345)
    }
    v364
  }

  def matchEscapeChar(node: Node): EscapeChar = {
    val BindNode(v365, v366) = node
    val v370 = v365.id match {
      case 160 =>
        val v367 = v366.asInstanceOf[SequenceNode].children(1)
        val BindNode(v368, v369) = v367
        assert(v368.id == 162)
        EscapeChar(v369.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char)(v366)
    }
    v370
  }

  def matchExpr(node: Node): Expr = {
    val BindNode(v371, v372) = node
    val v382 = v371.id match {
      case 173 =>
        val v373 = v372.asInstanceOf[SequenceNode].children.head
        val BindNode(v374, v375) = v373
        assert(v374.id == 172)
        val v376 = v372.asInstanceOf[SequenceNode].children(4)
        val BindNode(v377, v378) = v376
        assert(v377.id == 174)
        CastExpr(matchExpr(v375), matchNoUnionType(v378))(v372)
      case 227 =>
        val v379 = v372.asInstanceOf[SequenceNode].children.head
        val BindNode(v380, v381) = v379
        assert(v380.id == 228)
        matchMergeOpOrPrimary(v381)
    }
    v382
  }

  def matchImportDef(node: Node): ImportDef = {
    val BindNode(v383, v384) = node
    val v428 = v383.id match {
      case 121 =>
        val v385 = v384.asInstanceOf[SequenceNode].children(2)
        val BindNode(v386, v387) = v385
        assert(v386.id == 126)
        val v388 = v384.asInstanceOf[SequenceNode].children(3)
        val BindNode(v389, v390) = v388
        assert(v389.id == 134)
        val BindNode(v391, v392) = v390
        val v401 = v391.id match {
          case 141 =>
            None
          case 135 =>
            val BindNode(v393, v394) = v392
            val v400 = v393.id match {
              case 136 =>
                val BindNode(v395, v396) = v394
                assert(v395.id == 137)
                val v397 = v396.asInstanceOf[SequenceNode].children(3)
                val BindNode(v398, v399) = v397
                assert(v398.id == 87)
                matchSimpleName(v399)
            }
            Some(v400)
        }
        ImportName(matchName(v387), v401)(v384)
      case 142 =>
        val v402 = v384.asInstanceOf[SequenceNode].children(2)
        val BindNode(v403, v404) = v402
        assert(v403.id == 143)
        val v405 = v384.asInstanceOf[SequenceNode].children(6)
        val BindNode(v406, v407) = v405
        assert(v406.id == 87)
        ImportAll(matchImportSourceExpr(v404), matchSimpleName(v407))(v384)
      case 296 =>
        val v408 = v384.asInstanceOf[SequenceNode].children(2)
        val BindNode(v409, v410) = v408
        assert(v409.id == 143)
        val v411 = v384.asInstanceOf[SequenceNode].children(6)
        val BindNode(v412, v413) = v411
        assert(v412.id == 126)
        val v414 = v384.asInstanceOf[SequenceNode].children(7)
        val BindNode(v415, v416) = v414
        assert(v415.id == 134)
        val BindNode(v417, v418) = v416
        val v427 = v417.id match {
          case 141 =>
            None
          case 135 =>
            val BindNode(v419, v420) = v418
            val v426 = v419.id match {
              case 136 =>
                val BindNode(v421, v422) = v420
                assert(v421.id == 137)
                val v423 = v422.asInstanceOf[SequenceNode].children(3)
                val BindNode(v424, v425) = v423
                assert(v424.id == 87)
                matchSimpleName(v425)
            }
            Some(v426)
        }
        ImportFrom(matchImportSourceExpr(v410), matchName(v413), v427)(v384)
    }
    v428
  }

  def matchImportSourceExpr(node: Node): ImportSourceExpr = {
    val BindNode(v429, v430) = node
    val v437 = v429.id match {
      case 144 =>
        val v431 = v430.asInstanceOf[SequenceNode].children.head
        val BindNode(v432, v433) = v431
        assert(v432.id == 145)
        matchStringLiteral(v433)
      case 232 =>
        val v434 = v430.asInstanceOf[SequenceNode].children.head
        val BindNode(v435, v436) = v434
        assert(v435.id == 233)
        matchCallExpr(v436)
    }
    v437
  }

  def matchLiteral(node: Node): Literal = {
    val BindNode(v438, v439) = node
    val v449 = v438.id match {
      case 144 =>
        val v440 = v439.asInstanceOf[SequenceNode].children.head
        val BindNode(v441, v442) = v440
        assert(v441.id == 145)
        matchStringLiteral(v442)
      case 285 =>
        val v443 = v439.asInstanceOf[SequenceNode].children.head
        val BindNode(v444, v445) = v443
        assert(v444.id == 286)
        matchBooleanLiteral(v445)
      case 292 =>
        val v446 = v439.asInstanceOf[SequenceNode].children.head
        val BindNode(v447, v448) = v446
        assert(v447.id == 293)
        matchNoneLiteral(v448)
    }
    v449
  }

  def matchMergeOpOrPrimary(node: Node): MergeOpOrPrimary = {
    val BindNode(v450, v451) = node
    val v461 = v450.id match {
      case 229 =>
        val v452 = v451.asInstanceOf[SequenceNode].children.head
        val BindNode(v453, v454) = v452
        assert(v453.id == 172)
        val v455 = v451.asInstanceOf[SequenceNode].children(4)
        val BindNode(v456, v457) = v455
        assert(v456.id == 231)
        MergeOp(matchExpr(v454), matchPrimary(v457))(v451)
      case 295 =>
        val v458 = v451.asInstanceOf[SequenceNode].children.head
        val BindNode(v459, v460) = v458
        assert(v459.id == 231)
        matchPrimary(v460)
    }
    v461
  }

  def matchMethodRef(node: Node): MethodRef = {
    val BindNode(v462, v463) = node
    val v484 = v462.id match {
      case 368 =>
        val v464 = v463.asInstanceOf[SequenceNode].children.head
        val BindNode(v465, v466) = v464
        assert(v465.id == 126)
        val v467 = v463.asInstanceOf[SequenceNode].children(4)
        val BindNode(v468, v469) = v467
        assert(v468.id == 126)
        val v470 = v463.asInstanceOf[SequenceNode].children(5)
        val BindNode(v471, v472) = v470
        assert(v471.id == 369)
        val BindNode(v473, v474) = v472
        val v483 = v473.id match {
          case 141 =>
            None
          case 370 =>
            val BindNode(v475, v476) = v474
            val v482 = v475.id match {
              case 371 =>
                val BindNode(v477, v478) = v476
                assert(v477.id == 372)
                val v479 = v478.asInstanceOf[SequenceNode].children(3)
                val BindNode(v480, v481) = v479
                assert(v480.id == 87)
                matchSimpleName(v481)
            }
            Some(v482)
        }
        MethodRef(matchName(v466), matchName(v469), v483)(v463)
    }
    v484
  }

  def matchName(node: Node): Name = {
    val BindNode(v485, v486) = node
    val v502 = v485.id match {
      case 127 =>
        val v487 = v486.asInstanceOf[SequenceNode].children.head
        val BindNode(v488, v489) = v487
        assert(v488.id == 87)
        val v490 = v486.asInstanceOf[SequenceNode].children(1)
        val v491 = unrollRepeat0(v490).map { elem =>
          val BindNode(v492, v493) = elem
          assert(v492.id == 130)
          val BindNode(v494, v495) = v493
          val v501 = v494.id match {
            case 131 =>
              val BindNode(v496, v497) = v495
              assert(v496.id == 132)
              val v498 = v497.asInstanceOf[SequenceNode].children(3)
              val BindNode(v499, v500) = v498
              assert(v499.id == 87)
              matchSimpleName(v500)
          }
          v501
        }
        Name(List(matchSimpleName(v489)) ++ v491)(v486)
    }
    v502
  }

  def matchNameDef(node: Node): NameDef = {
    val BindNode(v503, v504) = node
    val v511 = v503.id match {
      case 253 =>
        val v505 = v504.asInstanceOf[SequenceNode].children.head
        val BindNode(v506, v507) = v505
        assert(v506.id == 87)
        val v508 = v504.asInstanceOf[SequenceNode].children(4)
        val BindNode(v509, v510) = v508
        assert(v509.id == 172)
        NameDef(matchSimpleName(v507), matchExpr(v510))(v504)
    }
    v511
  }

  def matchNamedExpr(node: Node): NamedExpr = {
    val BindNode(v512, v513) = node
    val v520 = v512.id match {
      case 277 =>
        val v514 = v513.asInstanceOf[SequenceNode].children.head
        val BindNode(v515, v516) = v514
        assert(v515.id == 87)
        val v517 = v513.asInstanceOf[SequenceNode].children(4)
        val BindNode(v518, v519) = v517
        assert(v518.id == 172)
        NamedExpr(matchSimpleName(v516), matchExpr(v519))(v513)
    }
    v520
  }

  def matchNamedParam(node: Node): NamedParam = {
    val BindNode(v521, v522) = node
    val v529 = v521.id match {
      case 253 =>
        val v523 = v522.asInstanceOf[SequenceNode].children.head
        val BindNode(v524, v525) = v523
        assert(v524.id == 87)
        val v526 = v522.asInstanceOf[SequenceNode].children(4)
        val BindNode(v527, v528) = v526
        assert(v527.id == 172)
        NamedParam(matchSimpleName(v525), matchExpr(v528))(v522)
    }
    v529
  }

  def matchNamedParams(node: Node): List[NamedParam] = {
    val BindNode(v530, v531) = node
    val v547 = v530.id match {
      case 251 =>
        val v532 = v531.asInstanceOf[SequenceNode].children.head
        val BindNode(v533, v534) = v532
        assert(v533.id == 252)
        val v535 = v531.asInstanceOf[SequenceNode].children(1)
        val v536 = unrollRepeat0(v535).map { elem =>
          val BindNode(v537, v538) = elem
          assert(v537.id == 257)
          val BindNode(v539, v540) = v538
          val v546 = v539.id match {
            case 258 =>
              val BindNode(v541, v542) = v540
              assert(v541.id == 259)
              val v543 = v542.asInstanceOf[SequenceNode].children(3)
              val BindNode(v544, v545) = v543
              assert(v544.id == 252)
              matchNamedParam(v545)
          }
          v546
        }
        List(matchNamedParam(v534)) ++ v536
    }
    v547
  }

  def matchNamedTupleType(node: Node): NamedTupleType = {
    val BindNode(v548, v549) = node
    val v565 = v548.id match {
      case 218 =>
        val v550 = v549.asInstanceOf[SequenceNode].children(2)
        val BindNode(v551, v552) = v550
        assert(v551.id == 219)
        val v553 = v549.asInstanceOf[SequenceNode].children(3)
        val v554 = unrollRepeat0(v553).map { elem =>
          val BindNode(v555, v556) = elem
          assert(v555.id == 224)
          val BindNode(v557, v558) = v556
          val v564 = v557.id match {
            case 225 =>
              val BindNode(v559, v560) = v558
              assert(v559.id == 226)
              val v561 = v560.asInstanceOf[SequenceNode].children(3)
              val BindNode(v562, v563) = v561
              assert(v562.id == 219)
              matchNamedType(v563)
          }
          v564
        }
        NamedTupleType(List(matchNamedType(v552)) ++ v554)(v549)
    }
    v565
  }

  def matchNamedType(node: Node): NamedType = {
    val BindNode(v566, v567) = node
    val v574 = v566.id match {
      case 220 =>
        val v568 = v567.asInstanceOf[SequenceNode].children.head
        val BindNode(v569, v570) = v568
        assert(v569.id == 87)
        val v571 = v567.asInstanceOf[SequenceNode].children(4)
        val BindNode(v572, v573) = v571
        assert(v572.id == 194)
        NamedType(matchSimpleName(v570), matchTypeExpr(v573))(v567)
    }
    v574
  }

  def matchNamespaceDef(node: Node): NamespaceDef = {
    val BindNode(v575, v576) = node
    val v583 = v575.id match {
      case 59 =>
        val v577 = v576.asInstanceOf[SequenceNode].children(2)
        val BindNode(v578, v579) = v577
        assert(v578.id == 87)
        val v580 = v576.asInstanceOf[SequenceNode].children(5)
        val BindNode(v581, v582) = v580
        assert(v581.id == 2)
        NamespaceDef(matchSimpleName(v579), matchBuildScript(v582))(v576)
    }
    v583
  }

  def matchNoUnionType(node: Node): NoUnionType = {
    val BindNode(v584, v585) = node
    val v598 = v584.id match {
      case 175 =>
        val v586 = v585.asInstanceOf[SequenceNode].children.head
        val BindNode(v587, v588) = v586
        assert(v587.id == 126)
        matchName(v588)
      case 211 =>
        val v589 = v585.asInstanceOf[SequenceNode].children.head
        val BindNode(v590, v591) = v589
        assert(v590.id == 212)
        matchTupleType(v591)
      case 176 =>
        NoneType()(v585)
      case 178 =>
        val v592 = v585.asInstanceOf[SequenceNode].children.head
        val BindNode(v593, v594) = v592
        assert(v593.id == 179)
        matchCollectionType(v594)
      case 216 =>
        val v595 = v585.asInstanceOf[SequenceNode].children.head
        val BindNode(v596, v597) = v595
        assert(v596.id == 217)
        matchNamedTupleType(v597)
    }
    v598
  }

  def matchNoneLiteral(node: Node): NoneLiteral = {
    val BindNode(v599, v600) = node
    val v601 = v599.id match {
      case 176 =>
        NoneLiteral()(v600)
    }
    v601
  }

  def matchParamDef(node: Node): ParamDef = {
    val BindNode(v602, v603) = node
    val v644 = v602.id match {
      case 357 =>
        val v604 = v603.asInstanceOf[SequenceNode].children.head
        val BindNode(v605, v606) = v604
        assert(v605.id == 87)
        val v607 = v603.asInstanceOf[SequenceNode].children(1)
        val BindNode(v608, v609) = v607
        assert(v608.id == 318)
        val BindNode(v610, v611) = v609
        val v620 = v610.id match {
          case 141 =>
            None
          case 319 =>
            val BindNode(v612, v613) = v611
            val v619 = v612.id match {
              case 320 =>
                val BindNode(v614, v615) = v613
                assert(v614.id == 321)
                val v616 = v615.asInstanceOf[SequenceNode].children(1)
                val BindNode(v617, v618) = v616
                assert(v617.id == 322)
                v618.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char
            }
            Some(v619)
        }
        val v621 = v603.asInstanceOf[SequenceNode].children(5)
        val BindNode(v622, v623) = v621
        assert(v622.id == 194)
        val v624 = v603.asInstanceOf[SequenceNode].children(6)
        val BindNode(v625, v626) = v624
        assert(v625.id == 358)
        val BindNode(v627, v628) = v626
        val v637 = v627.id match {
          case 141 =>
            None
          case 359 =>
            val BindNode(v629, v630) = v628
            val v636 = v629.id match {
              case 360 =>
                val BindNode(v631, v632) = v630
                assert(v631.id == 361)
                val v633 = v632.asInstanceOf[SequenceNode].children(3)
                val BindNode(v634, v635) = v633
                assert(v634.id == 172)
                matchExpr(v635)
            }
            Some(v636)
        }
        ParamDef(matchSimpleName(v606), v620.isDefined, Some(matchTypeExpr(v623)), v637)(v603)
      case 253 =>
        val v638 = v603.asInstanceOf[SequenceNode].children.head
        val BindNode(v639, v640) = v638
        assert(v639.id == 87)
        val v641 = v603.asInstanceOf[SequenceNode].children(4)
        val BindNode(v642, v643) = v641
        assert(v642.id == 172)
        ParamDef(matchSimpleName(v640), false, None, Some(matchExpr(v643)))(v603)
    }
    v644
  }

  def matchParamsDef(node: Node): List[ParamDef] = {
    val BindNode(v645, v646) = node
    val v673 = v645.id match {
      case 351 =>
        val v648 = v646.asInstanceOf[SequenceNode].children(1)
        val BindNode(v649, v650) = v648
        assert(v649.id == 352)
        val BindNode(v651, v652) = v650
        val v672 = v651.id match {
          case 141 =>
            None
          case 353 =>
            val BindNode(v653, v654) = v652
            assert(v653.id == 354)
            val BindNode(v655, v656) = v654
            assert(v655.id == 355)
            val v657 = v656.asInstanceOf[SequenceNode].children(1)
            val BindNode(v658, v659) = v657
            assert(v658.id == 356)
            val v660 = v656.asInstanceOf[SequenceNode].children(2)
            val v661 = unrollRepeat0(v660).map { elem =>
              val BindNode(v662, v663) = elem
              assert(v662.id == 364)
              val BindNode(v664, v665) = v663
              val v671 = v664.id match {
                case 365 =>
                  val BindNode(v666, v667) = v665
                  assert(v666.id == 366)
                  val v668 = v667.asInstanceOf[SequenceNode].children(3)
                  val BindNode(v669, v670) = v668
                  assert(v669.id == 356)
                  matchParamDef(v670)
              }
              v671
            }
            Some(List(matchParamDef(v659)) ++ v661)
        }
        val v647 = v672
        if (v647.isDefined) v647.get else List()
    }
    v673
  }

  def matchPositionalParams(node: Node): List[Expr] = {
    val BindNode(v674, v675) = node
    val v691 = v674.id match {
      case 239 =>
        val v676 = v675.asInstanceOf[SequenceNode].children.head
        val BindNode(v677, v678) = v676
        assert(v677.id == 172)
        val v679 = v675.asInstanceOf[SequenceNode].children(1)
        val v680 = unrollRepeat0(v679).map { elem =>
          val BindNode(v681, v682) = elem
          assert(v681.id == 242)
          val BindNode(v683, v684) = v682
          val v690 = v683.id match {
            case 243 =>
              val BindNode(v685, v686) = v684
              assert(v685.id == 244)
              val v687 = v686.asInstanceOf[SequenceNode].children(3)
              val BindNode(v688, v689) = v687
              assert(v688.id == 172)
              matchExpr(v689)
          }
          v690
        }
        List(matchExpr(v678)) ++ v680
    }
    v691
  }

  def matchPrimary(node: Node): Primary = {
    val BindNode(v692, v693) = node
    val v794 = v692.id match {
      case 263 =>
        val v695 = v693.asInstanceOf[SequenceNode].children(1)
        val BindNode(v696, v697) = v695
        assert(v696.id == 265)
        val BindNode(v698, v699) = v697
        val v719 = v698.id match {
          case 141 =>
            None
          case 266 =>
            val BindNode(v700, v701) = v699
            assert(v700.id == 267)
            val BindNode(v702, v703) = v701
            assert(v702.id == 268)
            val v704 = v703.asInstanceOf[SequenceNode].children(1)
            val BindNode(v705, v706) = v704
            assert(v705.id == 172)
            val v707 = v703.asInstanceOf[SequenceNode].children(2)
            val v708 = unrollRepeat0(v707).map { elem =>
              val BindNode(v709, v710) = elem
              assert(v709.id == 242)
              val BindNode(v711, v712) = v710
              val v718 = v711.id match {
                case 243 =>
                  val BindNode(v713, v714) = v712
                  assert(v713.id == 244)
                  val v715 = v714.asInstanceOf[SequenceNode].children(3)
                  val BindNode(v716, v717) = v715
                  assert(v716.id == 172)
                  matchExpr(v717)
              }
              v718
            }
            Some(List(matchExpr(v706)) ++ v708)
        }
        val v694 = v719
        ListExpr(if (v694.isDefined) v694.get else List())(v693)
      case 294 =>
        val v720 = v693.asInstanceOf[SequenceNode].children(2)
        val BindNode(v721, v722) = v720
        assert(v721.id == 172)
        Paren(matchExpr(v722))(v693)
      case 283 =>
        val v723 = v693.asInstanceOf[SequenceNode].children.head
        val BindNode(v724, v725) = v723
        assert(v724.id == 284)
        matchLiteral(v725)
      case 270 =>
        val v726 = v693.asInstanceOf[SequenceNode].children(2)
        val BindNode(v727, v728) = v726
        assert(v727.id == 172)
        val v730 = v693.asInstanceOf[SequenceNode].children(5)
        val BindNode(v731, v732) = v730
        assert(v731.id == 265)
        val BindNode(v733, v734) = v732
        val v755 = v733.id match {
          case 141 =>
            None
          case 266 =>
            val BindNode(v735, v736) = v734
            val v754 = v735.id match {
              case 267 =>
                val BindNode(v737, v738) = v736
                assert(v737.id == 268)
                val v739 = v738.asInstanceOf[SequenceNode].children(1)
                val BindNode(v740, v741) = v739
                assert(v740.id == 172)
                val v742 = v738.asInstanceOf[SequenceNode].children(2)
                val v743 = unrollRepeat0(v742).map { elem =>
                  val BindNode(v744, v745) = elem
                  assert(v744.id == 242)
                  val BindNode(v746, v747) = v745
                  val v753 = v746.id match {
                    case 243 =>
                      val BindNode(v748, v749) = v747
                      assert(v748.id == 244)
                      val v750 = v749.asInstanceOf[SequenceNode].children(3)
                      val BindNode(v751, v752) = v750
                      assert(v751.id == 172)
                      matchExpr(v752)
                  }
                  v753
                }
                List(matchExpr(v741)) ++ v743
            }
            Some(v754)
        }
        val v729 = v755
        TupleExpr(List(matchExpr(v728)) ++ (if (v729.isDefined) v729.get else List()))(v693)
      case 232 =>
        val v756 = v693.asInstanceOf[SequenceNode].children.head
        val BindNode(v757, v758) = v756
        assert(v757.id == 233)
        matchCallExpr(v758)
      case 261 =>
        val v759 = v693.asInstanceOf[SequenceNode].children.head
        val BindNode(v760, v761) = v759
        assert(v760.id == 231)
        val v762 = v693.asInstanceOf[SequenceNode].children(4)
        val BindNode(v763, v764) = v762
        assert(v763.id == 87)
        MemberAccess(matchPrimary(v761), matchSimpleName(v764))(v693)
      case 262 =>
        val v765 = v693.asInstanceOf[SequenceNode].children.head
        val BindNode(v766, v767) = v765
        assert(v766.id == 87)
        NameRef(matchSimpleName(v767))(v693)
      case 271 =>
        val v769 = v693.asInstanceOf[SequenceNode].children(1)
        val BindNode(v770, v771) = v769
        assert(v770.id == 272)
        val BindNode(v772, v773) = v771
        val v793 = v772.id match {
          case 141 =>
            None
          case 273 =>
            val BindNode(v774, v775) = v773
            assert(v774.id == 274)
            val BindNode(v776, v777) = v775
            assert(v776.id == 275)
            val v778 = v777.asInstanceOf[SequenceNode].children(1)
            val BindNode(v779, v780) = v778
            assert(v779.id == 276)
            val v781 = v777.asInstanceOf[SequenceNode].children(2)
            val v782 = unrollRepeat0(v781).map { elem =>
              val BindNode(v783, v784) = elem
              assert(v783.id == 280)
              val BindNode(v785, v786) = v784
              val v792 = v785.id match {
                case 281 =>
                  val BindNode(v787, v788) = v786
                  assert(v787.id == 282)
                  val v789 = v788.asInstanceOf[SequenceNode].children(3)
                  val BindNode(v790, v791) = v789
                  assert(v790.id == 276)
                  matchNamedExpr(v791)
              }
              v792
            }
            Some(List(matchNamedExpr(v780)) ++ v782)
        }
        val v768 = v793
        NamedTupleExpr(if (v768.isDefined) v768.get else List())(v693)
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
      case 155 =>
        val v830 = v829.asInstanceOf[SequenceNode].children.head
        val BindNode(v831, v832) = v830
        assert(v831.id == 156)
        val BindNode(v833, v834) = v832
        assert(v833.id == 30)
        JustChar(v834.asInstanceOf[TerminalNode].input.asInstanceOf[Inputs.Character].char)(v829)
      case 158 =>
        val v835 = v829.asInstanceOf[SequenceNode].children.head
        val BindNode(v836, v837) = v835
        assert(v836.id == 159)
        matchEscapeChar(v837)
      case 163 =>
        val v838 = v829.asInstanceOf[SequenceNode].children.head
        val BindNode(v839, v840) = v838
        assert(v839.id == 164)
        matchStringExpr(v840)
    }
    v841
  }

  def matchStringExpr(node: Node): StringExpr = {
    val BindNode(v842, v843) = node
    val v860 = v842.id match {
      case 165 =>
        val v844 = v843.asInstanceOf[SequenceNode].children.head
        val BindNode(v845, v846) = v844
        assert(v845.id == 166)
        val BindNode(v847, v848) = v846
        assert(v847.id == 167)
        val BindNode(v849, v850) = v848
        val v856 = v849.id match {
          case 168 =>
            val BindNode(v851, v852) = v850
            assert(v851.id == 169)
            val v853 = v852.asInstanceOf[SequenceNode].children(1)
            val BindNode(v854, v855) = v853
            assert(v854.id == 87)
            matchSimpleName(v855)
        }
        SimpleExpr(v856)(v843)
      case 171 =>
        val v857 = v843.asInstanceOf[SequenceNode].children(3)
        val BindNode(v858, v859) = v857
        assert(v858.id == 172)
        ComplexExpr(matchExpr(v859))(v843)
    }
    v860
  }

  def matchStringLiteral(node: Node): StringLiteral = {
    val BindNode(v861, v862) = node
    val v877 = v861.id match {
      case 146 =>
        val v863 = v862.asInstanceOf[SequenceNode].children(1)
        val v864 = unrollRepeat0(v863).map { elem =>
          val BindNode(v865, v866) = elem
          assert(v865.id == 150)
          val BindNode(v867, v868) = v866
          assert(v867.id == 151)
          val BindNode(v869, v870) = v868
          val v876 = v869.id match {
            case 152 =>
              val BindNode(v871, v872) = v870
              assert(v871.id == 153)
              val v873 = v872.asInstanceOf[SequenceNode].children.head
              val BindNode(v874, v875) = v873
              assert(v874.id == 154)
              matchStringElem(v875)
          }
          v876
        }
        StringLiteral(v864)(v862)
    }
    v877
  }

  def matchSuperClassDef(node: Node): SuperClassDef = {
    val BindNode(v878, v879) = node
    val v898 = v878.id match {
      case 378 =>
        val v880 = v879.asInstanceOf[SequenceNode].children(4)
        val BindNode(v881, v882) = v880
        assert(v881.id == 87)
        val v883 = v879.asInstanceOf[SequenceNode].children(8)
        val BindNode(v884, v885) = v883
        assert(v884.id == 87)
        val v886 = v879.asInstanceOf[SequenceNode].children(9)
        val v887 = unrollRepeat0(v886).map { elem =>
          val BindNode(v888, v889) = elem
          assert(v888.id == 384)
          val BindNode(v890, v891) = v889
          val v897 = v890.id match {
            case 385 =>
              val BindNode(v892, v893) = v891
              assert(v892.id == 386)
              val v894 = v893.asInstanceOf[SequenceNode].children(3)
              val BindNode(v895, v896) = v894
              assert(v895.id == 87)
              matchSimpleName(v896)
          }
          v897
        }
        SuperClassDef(matchSimpleName(v882), List(matchSimpleName(v885)) ++ v887)(v879)
    }
    v898
  }

  def matchTupleType(node: Node): TupleType = {
    val BindNode(v899, v900) = node
    val v916 = v899.id match {
      case 213 =>
        val v901 = v900.asInstanceOf[SequenceNode].children(2)
        val BindNode(v902, v903) = v901
        assert(v902.id == 194)
        val v904 = v900.asInstanceOf[SequenceNode].children(3)
        val v905 = unrollRepeat0(v904).map { elem =>
          val BindNode(v906, v907) = elem
          assert(v906.id == 207)
          val BindNode(v908, v909) = v907
          val v915 = v908.id match {
            case 208 =>
              val BindNode(v910, v911) = v909
              assert(v910.id == 209)
              val v912 = v911.asInstanceOf[SequenceNode].children(3)
              val BindNode(v913, v914) = v912
              assert(v913.id == 194)
              matchTypeExpr(v914)
          }
          v915
        }
        TupleType(List(matchTypeExpr(v903)) ++ v905)(v900)
    }
    v916
  }

  def matchTypeExpr(node: Node): TypeExpr = {
    val BindNode(v917, v918) = node
    val v925 = v917.id match {
      case 195 =>
        val v919 = v918.asInstanceOf[SequenceNode].children.head
        val BindNode(v920, v921) = v919
        assert(v920.id == 174)
        matchNoUnionType(v921)
      case 196 =>
        val v922 = v918.asInstanceOf[SequenceNode].children.head
        val BindNode(v923, v924) = v922
        assert(v923.id == 197)
        matchUnionType(v924)
    }
    v925
  }

  def matchTypeParams(node: Node): TypeParams = {
    val BindNode(v926, v927) = node
    val v943 = v926.id match {
      case 192 =>
        val v928 = v927.asInstanceOf[SequenceNode].children(2)
        val BindNode(v929, v930) = v928
        assert(v929.id == 194)
        val v931 = v927.asInstanceOf[SequenceNode].children(3)
        val v932 = unrollRepeat0(v931).map { elem =>
          val BindNode(v933, v934) = elem
          assert(v933.id == 207)
          val BindNode(v935, v936) = v934
          val v942 = v935.id match {
            case 208 =>
              val BindNode(v937, v938) = v936
              assert(v937.id == 209)
              val v939 = v938.asInstanceOf[SequenceNode].children(3)
              val BindNode(v940, v941) = v939
              assert(v940.id == 194)
              matchTypeExpr(v941)
          }
          v942
        }
        TypeParams(List(matchTypeExpr(v930)) ++ v932)(v927)
    }
    v943
  }

  def matchUnionType(node: Node): UnionType = {
    val BindNode(v944, v945) = node
    val v961 = v944.id match {
      case 198 =>
        val v946 = v945.asInstanceOf[SequenceNode].children(2)
        val BindNode(v947, v948) = v946
        assert(v947.id == 174)
        val v949 = v945.asInstanceOf[SequenceNode].children(3)
        val v950 = unrollRepeat0(v949).map { elem =>
          val BindNode(v951, v952) = elem
          assert(v951.id == 201)
          val BindNode(v953, v954) = v952
          val v960 = v953.id match {
            case 202 =>
              val BindNode(v955, v956) = v954
              assert(v955.id == 203)
              val v957 = v956.asInstanceOf[SequenceNode].children(3)
              val BindNode(v958, v959) = v957
              assert(v958.id == 174)
              matchNoUnionType(v959)
          }
          v960
        }
        UnionType(List(matchNoUnionType(v948)) ++ v950)(v945)
    }
    v961
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
