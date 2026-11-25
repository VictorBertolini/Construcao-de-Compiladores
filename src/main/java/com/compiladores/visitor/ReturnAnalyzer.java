package com.compiladores.visitor;

import com.compiladores.ast.Ast.*;
import com.compiladores.ast.Visitor;

/**
 * Decisão 9 — análise estrutural de "return em todos os caminhos".
 * Devolve true se o comando sempre termina com return:
 *   bloco: algum comando do bloco sempre retorna;
 *   if: só com else, e todos os ramos (if, elif e else) sempre retornam;
 *   while/for: nunca contam (o corpo pode não executar).
 */
public final class ReturnAnalyzer implements Visitor<Boolean> {
    public Boolean visitBlock(Block n) {
        for (Node s : n.statements) if (s.accept(this)) return true;
        return false;
    }
    public Boolean visitIf(If n) {
        if (n.elseBody == null) return false;
        for (Branch b : n.branches) if (!b.accept(this)) return false;
        return n.elseBody.accept(this);
    }
    public Boolean visitBranch(Branch n) { return n.body.accept(this); }
    public Boolean visitReturn(Return n) { return true; }

    public Boolean visitWhile(While n) { return false; }
    public Boolean visitFor(For n) { return false; }
    public Boolean visitProgram(Program n) { return false; }
    public Boolean visitType(Type n) { return false; }
    public Boolean visitParam(Param n) { return false; }
    public Boolean visitFunctionDecl(FunctionDecl n) { return false; }
    public Boolean visitProcedureDecl(ProcedureDecl n) { return false; }
    public Boolean visitVarDecl(VarDecl n) { return false; }
    public Boolean visitAssign(Assign n) { return false; }
    public Boolean visitCallStmt(CallStmt n) { return false; }
    public Boolean visitId(Id n) { return false; }
    public Boolean visitIntLiteral(IntLiteral n) { return false; }
    public Boolean visitRealLiteral(RealLiteral n) { return false; }
    public Boolean visitStringLiteral(StringLiteral n) { return false; }
    public Boolean visitCharLiteral(CharLiteral n) { return false; }
    public Boolean visitBoolLiteral(BoolLiteral n) { return false; }
    public Boolean visitUnary(Unary n) { return false; }
    public Boolean visitBin(Bin n) { return false; }
    public Boolean visitCallExpr(CallExpr n) { return false; }
}
