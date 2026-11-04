package com.compiladores.visitor;

import com.compiladores.ast.Ast.*;
import com.compiladores.ast.Visitor;

/** Impressão pré-ordem: dois espaços por nível, sem posições (estáveis no round-trip). */
public final class TreePrinter implements Visitor<Void> {
    private final StringBuilder out = new StringBuilder();
    private int depth = 0;

    public String print(Node root) {
        out.setLength(0);
        depth = 0;
        root.accept(this);
        return out.toString();
    }
    private void line(String label) {
        out.append("  ".repeat(depth)).append(label).append('\n');
    }
    private void child(Node child) {
        if (child != null) { depth++; child.accept(this); depth--; }
    }
    private void children(Iterable<? extends Node> nodes) {
        for (Node n : nodes) child(n);
    }
    private void optional(String label, Node node) {
        if (node == null) { depth++; line(label + "(empty)"); depth--; }
        else child(node);
    }
    public Void visitProgram(Program n) { line("Program"); children(n.items); return null; }
    public Void visitType(Type n) { line("Type(" + n.name + ")"); return null; }
    public Void visitParam(Param n) { line("Param(" + n.name + ")"); child(n.type); return null; }
    public Void visitFunctionDecl(FunctionDecl n) { line("Function(" + n.name + ")"); child(n.returnType); children(n.params); child(n.body); return null; }
    public Void visitProcedureDecl(ProcedureDecl n) { line("Procedure(" + n.name + ")"); children(n.params); child(n.body); return null; }
    public Void visitVarDecl(VarDecl n) { line("VarDecl(" + n.name + ")"); child(n.type); optional("Initializer", n.initial); return null; }
    public Void visitBlock(Block n) { line("Block"); children(n.statements); return null; }
    public Void visitAssign(Assign n) { line("Assign"); child(n.target); child(n.value); return null; }
    public Void visitCallStmt(CallStmt n) { line("CallStmt"); child(n.call); return null; }
    public Void visitBranch(Branch n) { line("Branch"); child(n.condition); child(n.body); return null; }
    public Void visitIf(If n) { line("If"); children(n.branches); if (n.elseBody != null) { depth++; line("Else"); child(n.elseBody); depth--; } return null; }
    public Void visitWhile(While n) { line("While"); child(n.condition); child(n.body); return null; }
    public Void visitFor(For n) { line("For"); optional("Init", n.init); optional("Condition", n.condition); optional("Step", n.step); child(n.body); return null; }
    public Void visitReturn(Return n) { line("Return"); optional("Value", n.value); return null; }
    public Void visitId(Id n) { line("Id(" + n.name + ")"); return null; }
    public Void visitIntLiteral(IntLiteral n) { line("Int(" + n.value + ")"); return null; }
    public Void visitRealLiteral(RealLiteral n) { line("Real(" + n.value + ")"); return null; }
    public Void visitStringLiteral(StringLiteral n) { line("String(" + n.value + ")"); return null; }
    public Void visitCharLiteral(CharLiteral n) { line("Char(" + n.value + ")"); return null; }
    public Void visitBoolLiteral(BoolLiteral n) { line("Bool(" + n.value + ")"); return null; }
    public Void visitUnary(Unary n) { line("Unary(" + n.op + ")"); child(n.operand); return null; }
    public Void visitBin(Bin n) { line("Bin(" + n.op + ")"); child(n.left); child(n.right); return null; }
    public Void visitCallExpr(CallExpr n) { line("CallExpr(" + n.name + ")"); children(n.args); return null; }
}
