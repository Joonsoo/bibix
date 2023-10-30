package com.giyeok.bibix.graph.runner2

import com.giyeok.bibix.ast.BibixAst
import com.giyeok.bibix.base.*
import com.giyeok.bibix.graph.*

// ExprNode -> ValueResult
fun GlobalTaskRunner.runExprTask(gid: GlobalTaskId, exprNode: ExprNode<*>): TaskRunResult =
  when (exprNode) {
    is NoneLiteralNode -> cfValueResult(gid, NoneValue)
    is BooleanLiteralNode -> cfValueResult(gid, BooleanValue(exprNode.literal.value))

    is MemberAccessExprNode -> {
      withResult(gid.contextId, exprNode.target) { target ->
        when (target) {
          is NodeResult.ValueResult -> {
            fun memberValueOf(value: BibixValue, memberNames: List<String>): BibixValue =
              if (memberNames.isEmpty()) value else {
                check(value is ClassInstanceValue)
                val memberValue = value.fieldValues[memberNames.first()]
                  ?: throw IllegalStateException()
                memberValueOf(memberValue, memberNames.drop(1))
              }

            val memberValue = memberValueOf(target.value, exprNode.memberNames)
            valueResult(gid, target.isContextFree, memberValue)
          }

          else -> TODO()
        }
      }
    }

    is CallExprCallNode ->
      // evaluateCallExprCallNode(prjInstanceId, node)
      TODO()

    is CallExprNode -> TODO()
    is CastExprNode -> TODO()
    is MergeExprNode -> TODO()

    is StringNode -> {
      val builder = StringBuilder()
      var elemCounter = 0
      var isContextFree = true
      exprNode.stringExpr.elems.forEach { elem ->
        when (elem) {
          is BibixAst.EscapeChar -> {
            // TODO escape
            builder.append("\\${elem.code}")
          }

          is BibixAst.JustChar -> builder.append(elem.chr)
          is BibixAst.ComplexExpr -> {
            val elemValue = getValueResult(gid.contextId, exprNode.exprElems[elemCounter++])!!
            isContextFree = isContextFree && elemValue.isContextFree
            // TODO 이 앞단에 coercion node 넣어서 String type임이 보장되도록
            // elemValue.value as StringValue
            TODO()
          }

          is BibixAst.SimpleExpr -> {
            val elemValue = getValueResult(gid.contextId, exprNode.exprElems[elemCounter++])!!
            isContextFree = isContextFree && elemValue.isContextFree
            // TODO 이 앞단에 coercion node 넣어서 String type임이 보장되도록
            elemValue.value as StringValue
          }
        }
      }
      check(elemCounter == exprNode.exprElems.size)
      valueResult(gid, isContextFree, StringValue(builder.toString()))
    }

    is NameRefNode -> {
      withValueResult(gid.contextId, exprNode.valueNode) { valueResult(gid, it) }
    }

    is ParenExprNode -> {
      withValueResult(gid.contextId, exprNode.body) { valueResult(gid, it) }
    }

    is ThisRefNode -> {
      val context = getContext(gid.contextId)
      checkNotNull(context.thisValue) { "this is not allowed here" }
      csValueResult(gid, context.thisValue)
    }

    is TupleNode -> {
      var isContextFree = true
      val elems = mutableListOf<BibixValue>()
      exprNode.elemNodes.forEach { elem ->
        val elemValue = getResult(gid.contextId, elem)!!
        check(elemValue is NodeResult.ValueResult)
        isContextFree = isContextFree && elemValue.isContextFree
        elems.add(elemValue.value)
      }
      valueResult(gid, isContextFree, TupleValue(elems))
    }

    is NamedTupleNode -> {
      var isContextFree = true
      val pairs = mutableListOf<Pair<String, BibixValue>>()
      exprNode.elemNodes.forEach { (name, value) ->
        val elemValue = getResult(gid.contextId, value)!!
        check(elemValue is NodeResult.ValueResult)
        isContextFree = isContextFree && elemValue.isContextFree
        pairs.add(name to elemValue.value)
      }
      valueResult(gid, isContextFree, NamedTupleValue(pairs))
    }

    is ListExprNode -> {
      val elems = mutableListOf<BibixValue>()
      var isContextFree = true
      exprNode.elems.forEach { elem ->
        val elemValue = getResult(gid.contextId, elem.valueNode)!!
        check(elemValue is NodeResult.ValueResult)
        isContextFree = isContextFree && elemValue.isContextFree
        if (!elem.isEllipsis) {
          elems.add(elemValue.value)
        } else {
          when (val collValue = elemValue.value) {
            is ListValue -> elems.addAll(collValue.values)
            is SetValue -> elems.addAll(collValue.values)
            else -> throw IllegalStateException()
          }
        }
      }
      valueResult(gid, isContextFree, ListValue(elems))
    }
  }
