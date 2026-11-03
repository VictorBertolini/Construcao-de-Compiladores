package com.compiladores.ast;

import com.compiladores.ast.Ast.*;

/** Double dispatch: cada nó conhece a sobrecarga que deve chamar. */
public interface Visitor<T> {
    T visitProgram(Program n);
    T visitType(Type n);
    T visitParam(Param n);
    T visitFunctionDecl(FunctionDecl n);
    T visitProcedureDecl(ProcedureDecl n);
    T visitVarDecl(VarDecl n);
    T visitBlock(Block n);
    T visitAssign(Assign n);
    T visitCallStmt(CallStmt n);
    T visitBranch(Branch n);
    T visitIf(If n);
    T visitWhile(While n);
    T visitFor(For n);
    T visitReturn(Return n);
    T visitId(Id n);
    T visitIntLiteral(IntLiteral n);
    T visitRealLiteral(RealLiteral n);
    T visitStringLiteral(StringLiteral n);
    T visitBoolLiteral(BoolLiteral n);
    T visitUnary(Unary n);
    T visitBin(Bin n);
    T visitCallExpr(CallExpr n);
}
