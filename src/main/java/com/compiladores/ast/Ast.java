package com.compiladores.ast;

import com.compiladores.entity.Token;
import java.util.List;

/** Nós imutáveis da AST. As posições não participam da igualdade estrutural. */
public final class Ast {
    private Ast() { }

    public abstract static class Node {
        public final int line;
        public final int column;
        protected Node(Token token) { this(token.getLine(), token.getColumn()); }
        protected Node(int line, int column) { this.line = line; this.column = column; }
        public abstract <T> T accept(Visitor<T> visitor);
    }
    public abstract static class Item extends Node {
        protected Item(Token t) { super(t); }
        protected Item(int line, int column) { super(line, column); }
    }
    public abstract static class Stmt extends Node {
        protected Stmt(Token t) { super(t); }
    }
    public abstract static class Expr extends Node {
        protected Expr(Token t) { super(t); }
        protected Expr(int l, int c) { super(l, c); }
    }

    public static final class Program extends Node {
        public final List<Item> items;
        public Program(int l, int c, List<Item> items) { super(l, c); this.items = List.copyOf(items); }
        public <T> T accept(Visitor<T> v) { return v.visitProgram(this); }
    }
    public static final class Type extends Node {
        public final String name;
        public Type(Token t) { super(t); this.name = t.getText(); }
        public <T> T accept(Visitor<T> v) { return v.visitType(this); }
    }
    public static final class Param extends Node {
        public final Type type;
        public final String name;
        public Param(Type type, Token name) { super(type.line, type.column); this.type = type; this.name = name.getText(); }
        public <T> T accept(Visitor<T> v) { return v.visitParam(this); }
    }
    public static final class FunctionDecl extends Item {
        public final Type returnType;
        public final String name;
        public final List<Param> params;
        public final Block body;
        public FunctionDecl(Token t, Type ret, String name, List<Param> ps, Block body) {
            super(t); this.returnType = ret; this.name = name; this.params = List.copyOf(ps); this.body = body;
        }
        public <T> T accept(Visitor<T> v) { return v.visitFunctionDecl(this); }
    }
    public static final class ProcedureDecl extends Item {
        public final String name;
        public final List<Param> params;
        public final Block body;
        public ProcedureDecl(Token t, String name, List<Param> ps, Block body) {
            super(t); this.name = name; this.params = List.copyOf(ps); this.body = body;
        }
        public <T> T accept(Visitor<T> v) { return v.visitProcedureDecl(this); }
    }
    /** Permite declaração em escopo global, local ou no cabeçalho do for. */
    public static final class VarDecl extends Item {
        public final Type type;
        public final String name;
        public final Expr initial;
        public VarDecl(Type type, String name, Expr initial) {
            super(type.line, type.column); this.type = type; this.name = name; this.initial = initial;
        }
        public <T> T accept(Visitor<T> v) { return v.visitVarDecl(this); }
    }
    public static final class Block extends Stmt {
        public final List<Node> statements;
        public Block(Token t, List<Node> stmts) { super(t); this.statements = List.copyOf(stmts); }
        public <T> T accept(Visitor<T> v) { return v.visitBlock(this); }
    }
    public static final class Assign extends Stmt {
        public final Id target;
        public final Expr value;
        public Assign(Token t, Expr value) { super(t); this.target = new Id(t); this.value = value; }
        public <T> T accept(Visitor<T> v) { return v.visitAssign(this); }
    }
    public static final class CallStmt extends Stmt {
        public final CallExpr call;
        public CallStmt(Token t, List<Expr> args) { super(t); this.call = new CallExpr(t, args); }
        public <T> T accept(Visitor<T> v) { return v.visitCallStmt(this); }
    }
    public static final class Branch extends Node {
        public final Expr condition;
        public final Block body;
        public Branch(Token t, Expr condition, Block body) { super(t); this.condition = condition; this.body = body; }
        public <T> T accept(Visitor<T> v) { return v.visitBranch(this); }
    }
    public static final class If extends Stmt {
        public final List<Branch> branches;
        public final Block elseBody;
        public If(Token t, List<Branch> branches, Block elseBody) {
            super(t); this.branches = List.copyOf(branches); this.elseBody = elseBody;
        }
        public <T> T accept(Visitor<T> v) { return v.visitIf(this); }
    }
    public static final class While extends Stmt {
        public final Expr condition;
        public final Block body;
        public While(Token t, Expr e, Block body) { super(t); this.condition = e; this.body = body; }
        public <T> T accept(Visitor<T> v) { return v.visitWhile(this); }
    }
    public static final class For extends Stmt {
        public final Node init;
        public final Expr condition;
        public final Assign step;
        public final Block body;
        public For(Token t, Node i, Expr c, Assign s, Block b) {
            super(t); this.init = i; this.condition = c; this.step = s; this.body = b;
        }
        public <T> T accept(Visitor<T> v) { return v.visitFor(this); }
    }
    public static final class Return extends Stmt {
        public final Expr value;
        public Return(Token t, Expr value) { super(t); this.value = value; }
        public <T> T accept(Visitor<T> v) { return v.visitReturn(this); }
    }
    public static final class Id extends Expr {
        public final String name;
        public Id(Token t) { super(t); this.name = t.getText(); }
        public <T> T accept(Visitor<T> v) { return v.visitId(this); }
    }
    public static final class IntLiteral extends Expr {
        public final String value;
        public IntLiteral(Token t) { super(t); this.value = t.getText(); }
        public <T> T accept(Visitor<T> v) { return v.visitIntLiteral(this); }
    }
    public static final class RealLiteral extends Expr {
        public final String value;
        public RealLiteral(Token t) { super(t); this.value = t.getText(); }
        public <T> T accept(Visitor<T> v) { return v.visitRealLiteral(this); }
    }
    public static final class StringLiteral extends Expr {
        public final String value;
        public StringLiteral(Token t) { super(t); this.value = t.getText(); }
        public <T> T accept(Visitor<T> v) { return v.visitStringLiteral(this); }
    }
    /** Quinto tipo básico; o scanner do CP1 ainda não emite token de char, então o parser não o constrói. */
    public static final class CharLiteral extends Expr {
        public final String value;
        public CharLiteral(Token t) { super(t); this.value = t.getText(); }
        public <T> T accept(Visitor<T> v) { return v.visitCharLiteral(this); }
    }
    public static final class BoolLiteral extends Expr {
        public final boolean value;
        public BoolLiteral(Token t) { super(t); this.value = Boolean.parseBoolean(t.getText()); }
        public <T> T accept(Visitor<T> v) { return v.visitBoolLiteral(this); }
    }
    public static final class Unary extends Expr {
        public final String op;
        public final Expr operand;
        public Unary(Token t, Expr operand) { super(t); this.op = t.getText(); this.operand = operand; }
        public <T> T accept(Visitor<T> v) { return v.visitUnary(this); }
    }
    public static final class Bin extends Expr {
        public final String op;
        public final Expr left, right;
        public Bin(Token t, Expr left, Expr right) { super(left.line, left.column); this.op = t.getText(); this.left = left; this.right = right; }
        public <T> T accept(Visitor<T> v) { return v.visitBin(this); }
    }
    public static final class CallExpr extends Expr {
        public final String name;
        public final List<Expr> args;
        public CallExpr(Token t, List<Expr> args) { super(t); this.name = t.getText(); this.args = List.copyOf(args); }
        public <T> T accept(Visitor<T> v) { return v.visitCallExpr(this); }
    }
}
