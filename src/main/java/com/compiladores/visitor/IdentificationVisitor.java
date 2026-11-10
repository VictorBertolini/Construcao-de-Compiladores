package com.compiladores.visitor;

import com.compiladores.ast.Ast.*;
import com.compiladores.ast.Visitor;
import com.compiladores.symbols.StackSymbolTable;
import com.compiladores.symbols.Symbol;
import com.compiladores.symbols.Symbol.Category;
import com.compiladores.symbols.SymbolTable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** CP4: identificação de nomes sem instanceof, switch de nó ou checagem de tipos. */
public final class IdentificationVisitor implements Visitor<Void> {
    private final SymbolTable table;
    private final List<String> errors = new ArrayList<>();
    private final List<String> resolutions = new ArrayList<>();

    // CallExpr do CP3 guarda String em vez de Id; a associação fica externa à AST.
    private final Map<CallExpr, Symbol> callDeclarations = new IdentityHashMap<>();

    public IdentificationVisitor() { this(new StackSymbolTable()); }
    public IdentificationVisitor(SymbolTable table) {
        this.table = java.util.Objects.requireNonNull(table, "tabela de símbolos");
    }

    /** O Main só chama a passada para árvores aceitas; null é ignorado com segurança. */
    public void analyze(Program program) {
        if (program != null) program.accept(this);
    }

    public List<String> getErrors() { return List.copyOf(errors); }
    public List<String> getResolutions() { return List.copyOf(resolutions); }
    public Map<CallExpr, Symbol> getCallDeclarations() {
        return Collections.unmodifiableMap(callDeclarations);
    }
    public int currentLevel() { return table.level(); }

    /** Formato fixo também utilizado nos arquivos test/esperado. */
    public String report() {
        StringBuilder b = new StringBuilder("RESOLUÇÕES\n");
        if (resolutions.isEmpty()) b.append("(nenhuma)\n");
        else resolutions.forEach(line -> b.append(line).append('\n'));
        b.append("ERROS\n");
        if (errors.isEmpty()) b.append("(nenhum)\n");
        else errors.forEach(line -> b.append(line).append('\n'));
        return b.toString();
    }

    private void declare(String name, Category category, Node node, Type type) {
        Symbol symbol = new Symbol(name, category, node, table.level(), type);
        if (!table.declare(symbol)) {
            Symbol original = table.lookupLocal(name);
            errors.add("linha " + node.line + ": '" + name
                    + "' já declarado neste escopo (linha " + original.declaration.line + ")");
        }
    }

    private Symbol resolve(String name, int line) {
        Symbol result = table.lookup(name);
        if (result == null) {
            errors.add("linha " + line + ": '" + name + "' não declarado");
        } else {
            resolutions.add("linha " + line + ": '" + name + "' -> "
                    + result.category + " '" + result.name + "'@"
                    + result.declaration.line + " (nível " + result.level + ")");
        }
        return result;
    }

    private void descend(Node node) {
        if (node != null) node.accept(this);
    }

    private void functionBody(List<Param> params, Block body) {
        table.openScope();
        try {
            for (Param param : params) descend(param);
            // Escolha de projeto: parâmetros e corpo da função compartilham o mesmo escopo.
            // Blocos internos continuam abrindo escopos em visitBlock.
            if (body != null) for (Node node : body.statements) descend(node);
        } finally {
            table.closeScope();
        }
    }

    @Override public Void visitProgram(Program n) {
        for (Item item : n.items) descend(item);
        return null;
    }
    @Override public Void visitType(Type n) { return null; }
    @Override public Void visitParam(Param n) {
        declare(n.name, Category.PARAMETER, n, n.type);
        return null;
    }
    @Override public Void visitFunctionDecl(FunctionDecl n) {
        // A declaração existe antes da análise do corpo: recursão direta funciona.
        declare(n.name, Category.FUNCTION, n, n.returnType);
        functionBody(n.params, n.body);
        return null;
    }
    @Override public Void visitProcedureDecl(ProcedureDecl n) {
        declare(n.name, Category.PROCEDURE, n, null);
        functionBody(n.params, n.body);
        return null;
    }
    @Override public Void visitVarDecl(VarDecl n) {
        declare(n.name, Category.VARIABLE, n, n.type);
        descend(n.initial);
        return null;
    }
    @Override public Void visitBlock(Block n) {
        table.openScope();
        try {
            for (Node node : n.statements) descend(node);
        } finally {
            table.closeScope();
        }
        return null;
    }
    @Override public Void visitAssign(Assign n) {
        descend(n.target);
        descend(n.value);
        return null;
    }
    @Override public Void visitCallStmt(CallStmt n) {
        descend(n.call);
        return null;
    }
    @Override public Void visitBranch(Branch n) {
        descend(n.condition);
        descend(n.body);
        return null;
    }
    @Override public Void visitIf(If n) {
        for (Branch branch : n.branches) descend(branch);
        descend(n.elseBody);
        return null;
    }
    @Override public Void visitWhile(While n) {
        descend(n.condition);
        descend(n.body);
        return null;
    }
    @Override public Void visitFor(For n) {
        // O nome do inicializador pertence ao cabeçalho e ao corpo, não ao que vem após o laço.
        table.openScope();
        try {
            descend(n.init);
            descend(n.condition);
            descend(n.step);
            descend(n.body);
        } finally {
            table.closeScope();
        }
        return null;
    }
    @Override public Void visitReturn(Return n) {
        descend(n.value);
        return null;
    }
    @Override public Void visitId(Id n) {
        n.decl = resolve(n.name, n.line);
        return null;
    }
    @Override public Void visitIntLiteral(IntLiteral n) { return null; }
    @Override public Void visitRealLiteral(RealLiteral n) { return null; }
    @Override public Void visitStringLiteral(StringLiteral n) { return null; }
    @Override public Void visitCharLiteral(CharLiteral n) { return null; }
    @Override public Void visitBoolLiteral(BoolLiteral n) { return null; }
    @Override public Void visitUnary(Unary n) {
        descend(n.operand);
        return null;
    }
    @Override public Void visitBin(Bin n) {
        descend(n.left);
        descend(n.right);
        return null;
    }
    @Override public Void visitCallExpr(CallExpr n) {
        Symbol symbol = resolve(n.name, n.line);
        if (symbol != null) callDeclarations.put(n, symbol);
        for (Expr arg : n.args) descend(arg);
        return null;
    }
}
