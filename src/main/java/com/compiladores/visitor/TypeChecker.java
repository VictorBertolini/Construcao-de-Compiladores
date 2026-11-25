package com.compiladores.visitor;

import com.compiladores.ast.Ast.*;
import com.compiladores.ast.Visitor;
import com.compiladores.symbols.Symbol;
import com.compiladores.symbols.Symbol.Category;
import com.compiladores.types.Tipo;
import com.compiladores.types.TypeRules;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * CP5 — verificador de tipos sobre a AST decorada pelo CP4.
 * Sem instanceof e sem switch sobre o tipo do nó; não declara nomes nem abre escopos.
 * Expressões devolvem e gravam seu tipo em Expr.inferredType; comandos devolvem null.
 * Mensagens: ver README.md (seção "Formato das mensagens").
 */
public final class TypeChecker implements Visitor<Tipo> {
    private final Map<CallExpr, Symbol> calls;
    private final List<String> errors = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();
    private final ReturnAnalyzer returns = new ReturnAnalyzer();

    // Contexto: rotina corrente (para return) e o rótulo da condição do ramo atual.
    private String routineName;
    private Tipo routineReturn;
    private String branchLabel = "if";

    // Análise conservadora de "variável sem valor": declarações (nós) que com certeza têm valor.
    private Set<Node> assigned = identitySet();
    private final Set<Node> warned = identitySet();

    public TypeChecker(Map<CallExpr, Symbol> callDeclarations) {
        this.calls = callDeclarations == null ? Map.of() : callDeclarations;
    }

    public void check(Program program) { if (program != null) program.accept(this); }
    public List<String> getErrors() { return List.copyOf(errors); }
    public List<String> getWarnings() { return List.copyOf(warnings); }

    // ---------- utilidades ----------
    private static Set<Node> identitySet() { return Collections.newSetFromMap(new IdentityHashMap<>()); }
    private static Set<Node> copy(Set<Node> s) { Set<Node> c = identitySet(); c.addAll(s); return c; }
    private void error(int line, String msg) { errors.add("linha " + line + ": " + msg); }
    private void warning(int line, String msg) { warnings.add("linha " + line + ": aviso: " + msg); }
    private Tipo expr(Expr e) { return e.accept(this); }
    private static boolean isRoutine(Symbol s) {
        return s.category == Category.FUNCTION || s.category == Category.PROCEDURE;
    }
    private static String kind(Symbol s) {
        return s.category == Category.FUNCTION ? "função" : "procedimento";
    }
    private Tipo set(Expr e, Tipo t) { e.inferredType = t; return t; }

    /** [IF]/[WHILE]/[FOR] — condição precisa ser boolean (decisão 3). */
    private void condition(Expr e, String label) {
        Tipo t = expr(e);
        if (t != Tipo.BOOLEAN && t != Tipo.ERROR)
            error(e.line, "condição de '" + label + "' deve ser boolean, mas é " + t);
    }

    // ---------- programa e rotinas ----------
    @Override public Tipo visitProgram(Program n) {
        for (Item item : n.items) item.accept(this);
        return null;
    }
    @Override public Tipo visitFunctionDecl(FunctionDecl n) {
        routine(n.name, Tipo.fromName(n.returnType.name), n.body, n.line);
        return null;
    }
    @Override public Tipo visitProcedureDecl(ProcedureDecl n) {
        routine(n.name, Tipo.VOID, n.body, n.line);
        return null;
    }
    private void routine(String name, Tipo ret, Block body, int line) {
        String savedName = routineName;
        Tipo savedRet = routineReturn;
        Set<Node> savedAssigned = assigned;
        routineName = name;
        routineReturn = ret;
        assigned = copy(savedAssigned);   // vê o que já tinha valor fora; não devolve o que fizer aqui
        try {
            body.accept(this);
            // [RET-CAMINHOS] decisão 9
            if (ret != Tipo.VOID && !body.accept(returns))
                error(line, "função '" + name + "' pode terminar sem 'return'");
        } finally {
            routineName = savedName;
            routineReturn = savedRet;
            assigned = savedAssigned;
        }
    }
    @Override public Tipo visitParam(Param n) { return null; }
    @Override public Tipo visitType(Type n) { return null; }

    // ---------- comandos ----------
    /** [DECL] — inicializador conferido antes de o nome ter valor. */
    @Override public Tipo visitVarDecl(VarDecl n) {
        Tipo declared = Tipo.fromName(n.type.name);
        if (n.initial != null) {
            Tipo v = expr(n.initial);
            if (!TypeRules.assignable(declared, v))
                error(n.line, "inicialização de '" + n.name + "': esperado " + declared + ", veio " + v);
            assigned.add(n);
        }
        return null;
    }
    @Override public Tipo visitBlock(Block n) {
        for (Node s : n.statements) s.accept(this);
        return null;
    }
    /** [ATRIB] — alvo precisa ser variável ou parâmetro (decisão 11). */
    @Override public Tipo visitAssign(Assign n) {
        Tipo v = expr(n.value);
        Symbol s = n.target.decl;
        Tipo t;
        if (s == null) t = Tipo.ERROR;
        else if (isRoutine(s)) {
            error(n.line, "'" + s.name + "' é " + kind(s) + " e não pode receber atribuição");
            t = Tipo.ERROR;
        } else t = s.tipo;
        set(n.target, t);
        if (!TypeRules.assignable(t, v))
            error(n.line, "atribuição a '" + n.target.name + "': esperado " + t + ", veio " + v);
        if (s != null) assigned.add(s.declaration);
        return null;
    }
    /** Decisão 6 — função como comando: o valor é descartado. */
    @Override public Tipo visitCallStmt(CallStmt n) {
        call(n.call, true);
        return null;
    }
    @Override public Tipo visitBranch(Branch n) {
        condition(n.condition, branchLabel);
        n.body.accept(this);
        return null;
    }
    @Override public Tipo visitIf(If n) {
        Set<Node> before = assigned;
        List<Set<Node>> results = new ArrayList<>();
        for (int i = 0; i < n.branches.size(); i++) {
            branchLabel = i == 0 ? "if" : "elif";
            assigned = copy(before);
            n.branches.get(i).accept(this);
            results.add(assigned);
        }
        if (n.elseBody != null) {
            assigned = copy(before);
            n.elseBody.accept(this);
            results.add(assigned);
            Set<Node> all = copy(results.get(0));
            for (Set<Node> r : results) all.retainAll(r);
            assigned = all;          // só tem valor se recebeu em todos os ramos
        } else {
            assigned = before;       // sem else, algum caminho não passa por nenhum ramo
        }
        return null;
    }
    @Override public Tipo visitWhile(While n) {
        condition(n.condition, "while");
        Set<Node> before = assigned;
        assigned = copy(before);
        n.body.accept(this);
        assigned = before;           // o corpo pode não executar
        return null;
    }
    @Override public Tipo visitFor(For n) {
        if (n.init != null) n.init.accept(this);
        if (n.condition != null) condition(n.condition, "for");
        Set<Node> before = assigned;
        assigned = copy(before);
        if (n.step != null) n.step.accept(this);
        n.body.accept(this);
        assigned = before;
        return null;
    }
    /** [RET] / [RET-VOID]. */
    @Override public Tipo visitReturn(Return n) {
        Tipo v = n.value == null ? Tipo.VOID : expr(n.value);
        if (routineReturn == null) {
            error(n.line, "'return' fora de função");
        } else if (routineReturn == Tipo.VOID) {
            if (n.value != null) error(n.line, "'return' com valor no procedimento '" + routineName + "'");
        } else if (n.value == null) {
            error(n.line, "'return' sem valor na função '" + routineName + "', que retorna " + routineReturn);
        } else if (!TypeRules.assignable(routineReturn, v)) {
            error(n.line, "'return' em '" + routineName + "': esperado " + routineReturn + ", veio " + v);
        }
        return null;
    }

    // ---------- expressões ----------
    /** [ID] — lê o tipo da declaração; sem declaração, ERROR sem nova mensagem. */
    @Override public Tipo visitId(Id n) {
        Symbol s = n.decl;
        if (s == null) return set(n, Tipo.ERROR);
        if (isRoutine(s)) {   // decisão 11
            error(n.line, "'" + s.name + "' é " + kind(s) + " e foi usado sem parênteses");
            return set(n, Tipo.ERROR);
        }
        if (s.category == Category.VARIABLE && !assigned.contains(s.declaration) && warned.add(s.declaration))
            warning(n.line, "'" + s.name + "' pode ser usada sem valor");
        return set(n, s.tipo);
    }
    @Override public Tipo visitIntLiteral(IntLiteral n) { return set(n, Tipo.INT); }       // [LIT-INT]
    @Override public Tipo visitRealLiteral(RealLiteral n) { return set(n, Tipo.FLOAT); }   // [LIT-REAL]
    @Override public Tipo visitStringLiteral(StringLiteral n) { return set(n, Tipo.STRING); } // [LIT-STR]
    @Override public Tipo visitBoolLiteral(BoolLiteral n) { return set(n, Tipo.BOOLEAN); } // [LIT-BOOL]
    /** Decisão 2 — a linguagem não tem char; o nó só existe se for montado à mão. */
    @Override public Tipo visitCharLiteral(CharLiteral n) {
        error(n.line, "literal de caractere não é suportado (a linguagem não tem char)");
        return set(n, Tipo.ERROR);
    }
    @Override public Tipo visitUnary(Unary n) {
        Tipo t = expr(n.operand);
        Tipo r = TypeRules.unary(n.op, t);
        if (r == null) { error(n.line, "operador '" + n.op + "' não aceita " + t); r = Tipo.ERROR; }
        return set(n, r);
    }
    @Override public Tipo visitBin(Bin n) {
        Tipo l = expr(n.left);
        Tipo r = expr(n.right);
        Tipo res = TypeRules.binary(n.op, l, r);
        if (res == null) { error(n.line, "operador '" + n.op + "' não aceita " + l + " e " + r); res = Tipo.ERROR; }
        return set(n, res);
    }
    @Override public Tipo visitCallExpr(CallExpr n) { return call(n, false); }

    /** [CHAMADA] — é rotina? aridade; tipo de cada argumento; tipo = retorno. */
    private Tipo call(CallExpr n, boolean asStatement) {
        List<Tipo> args = new ArrayList<>();
        for (Expr a : n.args) args.add(expr(a));
        Symbol s = calls.get(n);
        if (s == null) return set(n, Tipo.ERROR);          // não declarado: CP4 já reportou
        if (!isRoutine(s)) {
            error(n.line, "'" + s.name + "' não é função nem procedimento");
            return set(n, Tipo.ERROR);
        }
        List<Tipo> params = s.paramTypes == null ? List.of() : s.paramTypes;
        if (params.size() != args.size()) {
            error(n.line, "'" + s.name + "' espera " + params.size() + " argumento(s), recebeu " + args.size());
        } else {
            for (int i = 0; i < args.size(); i++)
                if (!TypeRules.acceptsArgument(params.get(i), args.get(i)))
                    error(n.line, "argumento " + (i + 1) + " de '" + s.name + "': esperado "
                            + params.get(i) + ", veio " + args.get(i));
        }
        if (!asStatement && s.category == Category.PROCEDURE) {   // [CHAMADA-PROC]
            error(n.line, "procedimento '" + s.name + "' usado como expressão");
            return set(n, Tipo.ERROR);
        }
        return set(n, s.tipo);
    }
}
