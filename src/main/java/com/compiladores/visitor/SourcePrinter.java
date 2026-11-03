package com.compiladores.visitor;

import com.compiladores.ast.Ast.*;
import com.compiladores.ast.Visitor;
import java.util.stream.Collectors;

/** Código canônico, com parênteses em cada operação para preservar associatividade. */
public final class SourcePrinter implements Visitor<String> {
    public String print(Program root) { return root.accept(this); }
    private String expr(Expr n) { return n == null ? "" : n.accept(this); }
    private String withoutSemicolon(Node n) {
        return n == null ? "" : n.accept(this).replaceFirst(";$", "");
    }
    private String indent(String s) { return "    " + s.replace("\n", "\n    "); }
    public String visitProgram(Program n) { return n.items.stream().map(x -> x.accept(this)).collect(Collectors.joining("\n")); }
    public String visitType(Type n) { return n.name; }
    public String visitParam(Param n) { return n.type.accept(this) + " " + n.name; }
    private String params(java.util.List<Param> ps) { return ps.stream().map(x -> x.accept(this)).collect(Collectors.joining(", ")); }
    public String visitFunctionDecl(FunctionDecl n) { return "function " + n.returnType.accept(this) + " " + n.name + "(" + params(n.params) + ") " + n.body.accept(this); }
    public String visitProcedureDecl(ProcedureDecl n) { return "function void " + n.name + "(" + params(n.params) + ") " + n.body.accept(this); }
    public String visitVarDecl(VarDecl n) { return n.type.accept(this) + " " + n.name + (n.initial == null ? "" : " = " + expr(n.initial)) + ";"; }
    public String visitBlock(Block n) {
        if (n.statements.isEmpty()) return "{}";
        return "{\n" + n.statements.stream().map(s -> indent(s.accept(this))).collect(Collectors.joining("\n")) + "\n}";
    }
    public String visitAssign(Assign n) { return n.target.accept(this) + " = " + expr(n.value) + ";"; }
    public String visitCallStmt(CallStmt n) { return n.call.accept(this) + ";"; }
    public String visitBranch(Branch n) { return "(" + expr(n.condition) + ") " + n.body.accept(this); }
    public String visitIf(If n) {
        StringBuilder b = new StringBuilder("if ").append(n.branches.get(0).accept(this));
        for (int i = 1; i < n.branches.size(); i++) b.append(" elif ").append(n.branches.get(i).accept(this));
        if (n.elseBody != null) b.append(" else ").append(n.elseBody.accept(this));
        return b.toString();
    }
    public String visitWhile(While n) { return "while (" + expr(n.condition) + ") " + n.body.accept(this); }
    public String visitFor(For n) { return "for (" + withoutSemicolon(n.init) + "; " + expr(n.condition) + "; " + withoutSemicolon(n.step) + ") " + n.body.accept(this); }
    public String visitReturn(Return n) { return "return" + (n.value == null ? "" : " " + expr(n.value)) + ";"; }
    public String visitId(Id n) { return n.name; }
    public String visitIntLiteral(IntLiteral n) { return n.value; }
    public String visitRealLiteral(RealLiteral n) { return n.value; }
    public String visitStringLiteral(StringLiteral n) { return n.value; }
    public String visitBoolLiteral(BoolLiteral n) { return Boolean.toString(n.value); }
    public String visitUnary(Unary n) { return "(" + n.op + " " + expr(n.operand) + ")"; }
    public String visitBin(Bin n) { return "(" + expr(n.left) + " " + n.op + " " + expr(n.right) + ")"; }
    public String visitCallExpr(CallExpr n) { return n.name + "(" + n.args.stream().map(this::expr).collect(Collectors.joining(", ")) + ")"; }
}
