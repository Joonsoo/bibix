package com.giyeok.bibix.buildscript

import com.giyeok.bibix.ast.BibixAst
import com.giyeok.bibix.base.CName
import com.giyeok.bibix.base.SourceId
import com.giyeok.bibix.runner.BibixType
import com.giyeok.bibix.utils.toKtList
import com.giyeok.jparser.ParseResultTree

data class ExprGraph(
  val mainNode: ExprNode,
  val nodes: Set<ExprNode>,
  val edges: Set<ExprEdge>,
  val callExprs: Registry<CallExprDef>,
  // 이 식이 나오는 소스코드/위치
  val exprLocation: ExprLocation,
) {
  // TODO ExprNode 객체의 크기가 너무 커지지 않도록 callExpr처럼 registry를 더 많이 활용하도록 수정
  class Builder(
    val nodes: MutableSet<ExprNode>,
    val edges: MutableSet<ExprEdge>,
    val callExprs: Registry.Builder<CallExprDef>,
  ) {
    constructor() : this(mutableSetOf(), mutableSetOf(), Registry.Builder())

    private fun addNode(node: ExprNode): ExprNode {
      nodes.add(node)
      return node
    }

    fun traverse(
      expr: BibixAst.Expr,
      lookup: NameLookupContext,
      thisClassTypeName: CName?,
      typeTraverser: (BibixAst.TypeExpr, NameLookupContext) -> BibixType,
    ): ExprNode = when (expr) {
      is BibixAst.MergeOp -> {
        val lhs = traverse(expr.lhs(), lookup, thisClassTypeName, typeTraverser)
        val rhs = traverse(expr.rhs(), lookup, thisClassTypeName, typeTraverser)
        val node = addNode(ExprNode.MergeOpNode(lhs, rhs))
        edges.add(ExprEdge(node, lhs))
        edges.add(ExprEdge(node, rhs))
        node
      }
      is BibixAst.CallExpr -> {
        val target = addNode(ExprNode.NameNode(lookup.findName(expr.name())))
        val posParams =
          expr.params().posParams().toKtList()
            .map { traverse(it, lookup, thisClassTypeName, typeTraverser) }
        val namedParams = expr.params().namedParams().toKtList()
          .associate { it.name() to traverse(it.value(), lookup, thisClassTypeName, typeTraverser) }
        val callExprNode = addNode(
          ExprNode.CallExprNode(
            callExprs.register(CallExprDef(target, ParamNodes(posParams, namedParams)))
          )
        )
        edges.add(ExprEdge(callExprNode, target))
        posParams.forEach { edges.add(ExprEdge(callExprNode, it)) }
        namedParams.forEach { edges.add(ExprEdge(callExprNode, it.value)) }
        callExprNode
      }
      is BibixAst.MemberAccess -> {
        val target = traverse(expr.target(), lookup, thisClassTypeName, typeTraverser)
        val node = addNode(ExprNode.MemberAccessNode(target, expr.name()))
        edges.add(ExprEdge(node, target))
        node
      }
      is BibixAst.NameRef -> {
        when {
          thisClassTypeName != null && expr.name() == "this" ->
            addNode(ExprNode.ClassThisRef(thisClassTypeName))
          lookup.argsName != null && lookup.argsName == expr.name() ->
            addNode(ExprNode.ActionArgsRef)
          else -> {
            val target = lookup.findName(expr.name())
            addNode(ExprNode.NameNode(target))
          }
        }
      }
      is BibixAst.ListExpr -> {
        val elems =
          expr.elems().toKtList().map { traverse(it, lookup, thisClassTypeName, typeTraverser) }
        val node = addNode(ExprNode.ListNode(elems))
        elems.forEach { edges.add(ExprEdge(node, it)) }
        node
      }
      is BibixAst.TupleExpr -> {
        val elems =
          expr.elems().toKtList().map { traverse(it, lookup, thisClassTypeName, typeTraverser) }
        val node = addNode(ExprNode.TupleNode(elems))
        elems.forEach { edges.add(ExprEdge(node, it)) }
        node
      }
      is BibixAst.NamedTupleExpr -> {
        val elems = expr.elems().toKtList()
          .map { it.name() to traverse(it.expr(), lookup, thisClassTypeName, typeTraverser) }
        val node = addNode(ExprNode.NamedTupleNode(elems))
        elems.forEach { edges.add(ExprEdge(node, it.second)) }
        node
      }
      is BibixAst.StringLiteral -> {
        val builder = StringElemsBuilder()
        expr.elems().toKtList().forEach { elem ->
          when (elem) {
            is BibixAst.JustChar -> builder.addChar(elem.chr())
            is BibixAst.EscapeChar -> {
              val c = when (elem.code()) {
                'n' -> '\n'
                'b' -> '\b'
                'r' -> '\r'
                't' -> '\t'
                '$' -> '$'
                '"' -> '"'
                else -> throw AssertionError()
              }
              builder.addChar(c)
            }
            is BibixAst.SimpleExpr -> {
              builder.addExpr(
                traverse(
                  BibixAst.NameRef(elem.name(), elem.parseNode()),
                  lookup,
                  thisClassTypeName,
                  typeTraverser
                )
              )
            }
            is BibixAst.ComplexExpr -> {
              builder.addExpr(traverse(elem.expr(), lookup, thisClassTypeName, typeTraverser))
            }
            else -> throw AssertionError()
          }
        }
        val elems = builder.build()
        val node = addNode(ExprNode.StringLiteralNode(elems))
        elems.filterIsInstance<ExprChunk>().forEach {
          edges.add(ExprEdge(node, it.expr))
        }
        node
      }
      is BibixAst.BooleanLiteral -> addNode(ExprNode.BooleanLiteralNode(expr.value()))
      is BibixAst.Paren -> traverse(expr.expr(), lookup, thisClassTypeName, typeTraverser)
      is BibixAst.CastExpr -> {
        val exprBody = traverse(expr.expr(), lookup, thisClassTypeName, typeTraverser)
        val targetType = typeTraverser(expr.castTo(), lookup)
        addNode(ExprNode.TypeCastNode(exprBody, targetType))
      }
      else -> throw AssertionError()
    }
  }

  companion object {
    fun fromExpr(
      expr: BibixAst.Expr,
      lookup: NameLookupContext,
      thisClassTypeName: CName?,
      typeTraverser: (BibixAst.TypeExpr, NameLookupContext) -> BibixType
    ): ExprGraph {
      val builder = Builder()
      val mainNode = builder.traverse(expr, lookup, thisClassTypeName, typeTraverser)
      return ExprGraph(
        mainNode,
        builder.nodes,
        builder.edges,
        builder.callExprs.build(),
        ExprLocation(lookup.chain.scope.cname.sourceId, expr.parseNode())
      )
    }
  }
}

sealed class ExprNode {
  data class CallExprNode(val callExprId: Int) : ExprNode()

  data class NameNode(val name: CName) : ExprNode()
  data class MergeOpNode(val lhs: ExprNode, val rhs: ExprNode) : ExprNode()
  data class MemberAccessNode(val target: ExprNode, val name: String) : ExprNode()

  data class ListNode(val elems: List<ExprNode>) : ExprNode()
  data class TupleNode(val elems: List<ExprNode>) : ExprNode()
  data class NamedTupleNode(val elems: List<Pair<String, ExprNode>>) : ExprNode()
  data class StringLiteralNode(val stringElems: List<StringElem>) : ExprNode()
  data class BooleanLiteralNode(val value: Boolean) : ExprNode()
  data class TypeCastNode(val value: ExprNode, val type: BibixType) : ExprNode()

  data class ClassThisRef(val className: CName) : ExprNode()
  object ActionArgsRef : ExprNode()
}

// start가 end를 필요로 함
data class ExprEdge(val start: ExprNode, val end: ExprNode)

data class ParamNodes(
  val posParams: List<ExprNode>,
  val namedParams: Map<String, ExprNode>
)

data class CallExprDef(
  val target: ExprNode,
  val params: ParamNodes,
)

data class ExprLocation(
  val sourceId: SourceId,
  val parseNode: ParseResultTree.Node,
)
