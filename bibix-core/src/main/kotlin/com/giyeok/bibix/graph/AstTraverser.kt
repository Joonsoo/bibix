package com.giyeok.bibix.graph

import com.giyeok.bibix.ast.BibixAst


fun traverseAst(ast: BibixAst.AstNode, visitor: (BibixAst.AstNode) -> Unit) {
  visitor(ast)
  val visitChild = { node: BibixAst.AstNode -> traverseAst(node, visitor) }
  when (ast) {
    is BibixAst.BuildScript -> {
      ast.packageName?.let(visitChild)
      ast.defs.forEach { traverseAst(it, visitor) }
    }

    is BibixAst.MultiCallActions -> {
      ast.exprs.forEach(visitChild)
    }

    is BibixAst.SingleCallAction -> {
      visitChild(ast.expr)
    }

    is BibixAst.ActionDef -> {
      visitChild(ast.body)
    }

    is BibixAst.CallExpr -> {
      visitChild(ast.name)
      ast.params.posParams.forEach(visitChild)
      ast.params.namedParams.forEach(visitChild)
    }

    is BibixAst.Name -> {
      // do nothing
    }

    is BibixAst.ActionRuleDef -> {
      ast.params.forEach(visitChild)
      visitChild(ast.impl)
    }

    is BibixAst.BooleanLiteral -> {
      // do nothing
    }

    is BibixAst.BuildRuleDef -> {
      ast.params.forEach(visitChild)
      visitChild(ast.returnType)
      visitChild(ast.impl)
    }

    is BibixAst.CallParams -> {
      ast.posParams.forEach(visitChild)
      ast.namedParams.forEach(visitChild)
    }

    is BibixAst.CastExpr -> {
      visitChild(ast.expr)
      visitChild(ast.castTo)
    }

    is BibixAst.ClassCastDef -> {
      visitChild(ast.castTo)
      visitChild(ast.expr)
    }

    is BibixAst.DataClassDef -> {
      ast.fields.forEach(visitChild)
      ast.body.forEach(visitChild)
    }

    is BibixAst.SuperClassDef -> {
      // do nothing
    }

    is BibixAst.CollectionType -> {
      visitChild(ast.typeParams)
    }

    is BibixAst.ComplexExpr -> {
      visitChild(ast.expr)
    }

    is BibixAst.DefsWithVarRedefs -> {
      ast.varRedefs.forEach(visitChild)
      ast.defs.forEach(visitChild)
    }

    is BibixAst.EnumDef -> {
      // do nothing
    }

    is BibixAst.ImportAll -> {
      visitChild(ast.source)
    }

    is BibixAst.ImportFrom -> {
      visitChild(ast.source)
      visitChild(ast.importing)
    }

    is BibixAst.NamespaceDef -> {
      ast.body.forEach(visitChild)
    }

    is BibixAst.TargetDef -> {
      visitChild(ast.value)
    }

    is BibixAst.VarDef -> {
      ast.typ?.let(visitChild)
      ast.defaultValue?.let(visitChild)
    }

    is BibixAst.VarRedefs -> {
      ast.redefs.forEach(visitChild)
    }

    is BibixAst.EllipsisElem -> {
      visitChild(ast.value)
    }

    is BibixAst.EscapeChar -> {
      // do nothing
    }

    is BibixAst.MergeOp -> {
      visitChild(ast.lhs)
      visitChild(ast.rhs)
    }

    is BibixAst.ListExpr -> {
      ast.elems.forEach(visitChild)
    }

    is BibixAst.NoneLiteral -> {
      // do nothing
    }

    is BibixAst.StringLiteral -> {
      ast.elems.forEach(visitChild)
    }

    is BibixAst.MemberAccess -> {
      visitChild(ast.target)
    }

    is BibixAst.NameRef -> {
      // do nothing
    }

    is BibixAst.NamedTupleExpr -> {
      ast.elems.forEach(visitChild)
    }

    is BibixAst.Paren -> {
      visitChild(ast.expr)
    }

    is BibixAst.This -> {
      // do nothing
    }

    is BibixAst.TupleExpr -> {
      ast.elems.forEach(visitChild)
    }

    is BibixAst.JustChar -> {
      // do nothing
    }

    is BibixAst.MethodRef -> {
      visitChild(ast.targetName)
      visitChild(ast.className)
    }

    is BibixAst.NamedExpr -> {
      visitChild(ast.expr)
    }

    is BibixAst.NamedParam -> {
      visitChild(ast.value)
    }

    is BibixAst.NamedTupleType -> {
      ast.elems.forEach(visitChild)
    }

    is BibixAst.NamedType -> {
      visitChild(ast.typ)
    }

    is BibixAst.NoneType -> {
      // do nothing
    }

    is BibixAst.TupleType -> {
      ast.elems.forEach(visitChild)
    }

    is BibixAst.ParamDef -> {
      ast.typ?.let(visitChild)
      ast.defaultValue?.let(visitChild)
    }

    is BibixAst.SimpleExpr -> {
      // do nothing
    }

    is BibixAst.UnionType -> {
      ast.elems.forEach(visitChild)
    }

    is BibixAst.TypeParams -> {
      ast.params.forEach(visitChild)
    }

    is BibixAst.VarRedef -> {
      traverseAst(ast.redefValue, visitor)
    }
  }
}
