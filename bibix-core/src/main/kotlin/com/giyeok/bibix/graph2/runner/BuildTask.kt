package com.giyeok.bibix.graph2.runner

import com.giyeok.bibix.graph2.BibixName
import com.giyeok.bibix.graph2.ExprNodeId
import com.giyeok.bibix.graph2.TypeNodeId

sealed class BuildTask

// TODO project id 추가

data class EvalExpr(val exprNodeId: ExprNodeId): BuildTask()

data class EvalType(val typeNodeId: TypeNodeId): BuildTask()


data class GlobalExprNodeId(val projectId: Int, val instanceId: Int, val exprNodeId: ExprNodeId)