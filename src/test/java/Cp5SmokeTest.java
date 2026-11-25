import com.compiladores.ast.Ast.*;
import com.compiladores.entity.Parser;
import com.compiladores.entity.Token;
import com.compiladores.entity.TokenType;
import com.compiladores.types.Tipo;
import com.compiladores.types.TypeRules;
import com.compiladores.visitor.SemanticAnalyzer;
import com.compiladores.visitor.IdentificationVisitor;
import com.compiladores.visitor.TypeChecker;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static com.compiladores.types.Tipo.*;

/** Suíte CP5 executável sem JUnit: java -ea -cp out Cp5SmokeTest (rodar na pasta do projeto). */
public final class Cp5SmokeTest {
    private static int checks;
    private static void check(boolean ok, String msg) { checks++; if (!ok) throw new AssertionError(msg); }

    public static final String[] FIXTURES = {
        "tipos_valido_completo", "tipos_erro_expressao", "cond_int", "return_incompativel",
        "chamada_errada", "procedimento_expressao", "varios_erros", "recursiva", "sem_valor",
        "d01_alargamento", "d02_sem_char", "d03_bool_aritmetica", "d04_string", "d05_equal_tipos",
        "d06_funcao_comando", "d07_recursao_mutua", "d08_argumentos", "d09_return_caminhos",
        "d10_sobrecarga", "d11_sem_parenteses"
    };

    /** Saída usada nos arquivos test/esperado (formato documentado no README). */
    public static String run(String source) {
        Parser.ParseResult r = new Parser(source).parse();
        if (!r.accepted()) {
            StringBuilder b = new StringBuilder("REJEITADO NA ANÁLISE SINTÁTICA\n");
            r.errors.forEach(e -> b.append(e).append('\n'));
            return b.toString();
        }
        SemanticAnalyzer a = new SemanticAnalyzer();
        a.analyze(r.ast);
        return a.report();
    }

    // ---------- 1. regras isoladas: cada linha da tabela, aceita e recusada ----------
    private static void bin(String op, Tipo l, Tipo r, Tipo expected) {
        Tipo got = TypeRules.binary(op, l, r);
        check(got == expected, l + " " + op + " " + r + ": esperado " + expected + ", obtido " + got);
    }
    private static void un(String op, Tipo t, Tipo expected) {
        Tipo got = TypeRules.unary(op, t);
        check(got == expected, op + " " + t + ": esperado " + expected + ", obtido " + got);
    }
    static void testRules() {
        for (String op : new String[]{"+", "-", "*", "/"}) {          // [ARIT]
            bin(op, INT, INT, INT); bin(op, INT, LONG, LONG); bin(op, LONG, FLOAT, FLOAT);
            bin(op, FLOAT, DOUBLE, DOUBLE); bin(op, INT, DOUBLE, DOUBLE);
            bin(op, INT, BOOLEAN, null); bin(op, BOOLEAN, BOOLEAN, null); bin(op, INT, STRING, null);
        }
        bin("+", STRING, STRING, STRING); bin("-", STRING, STRING, null);   // [CONCAT]
        bin("+", STRING, INT, null);
        bin("%", INT, INT, INT); bin("%", INT, LONG, LONG);                 // [MOD]
        bin("%", INT, DOUBLE, null); bin("%", FLOAT, FLOAT, null);
        for (String op : new String[]{"<", ">", "<=", ">="}) {        // [REL]
            bin(op, INT, INT, BOOLEAN); bin(op, INT, DOUBLE, BOOLEAN);
            bin(op, STRING, STRING, null); bin(op, BOOLEAN, BOOLEAN, null);
        }
        bin("equal", INT, DOUBLE, BOOLEAN); bin("equal", BOOLEAN, BOOLEAN, BOOLEAN);   // [IGUAL]
        bin("equal", STRING, STRING, BOOLEAN); bin("equal", INT, BOOLEAN, null);
        bin("equal", STRING, INT, null);
        for (String op : new String[]{"and", "or"}) {                 // [LOG]
            bin(op, BOOLEAN, BOOLEAN, BOOLEAN); bin(op, INT, BOOLEAN, null); bin(op, INT, INT, null);
        }
        un("not", BOOLEAN, BOOLEAN); un("not", INT, null);                  // [NOT]
        for (String op : new String[]{"-", "+"}) {                    // [NEG]
            un(op, INT, INT); un(op, DOUBLE, DOUBLE); un(op, BOOLEAN, null); un(op, STRING, null);
        }
        bin("+", ERROR, BOOLEAN, ERROR); un("not", ERROR, ERROR);           // sem cascata
        bin("and", VOID, BOOLEAN, null);
        // [ATRIB]/[ALARGA]
        check(TypeRules.assignable(DOUBLE, INT), "int -> double");
        check(TypeRules.assignable(LONG, INT), "int -> long");
        check(TypeRules.assignable(FLOAT, LONG), "long -> float");
        check(!TypeRules.assignable(INT, DOUBLE), "double -> int recusado");
        check(!TypeRules.assignable(FLOAT, DOUBLE), "double -> float recusado");
        check(!TypeRules.assignable(INT, BOOLEAN), "boolean -> int recusado");
        check(!TypeRules.assignable(STRING, INT), "int -> string recusado");
        check(TypeRules.assignable(INT, ERROR) && TypeRules.assignable(ERROR, STRING), "ERROR compatível");
        check(TypeRules.acceptsArgument(ANY, STRING) && !TypeRules.acceptsArgument(ANY, VOID), "print");
    }

    // ---------- 2. programas com saída esperada ----------
    static void testFixtures() throws Exception {
        for (String name : FIXTURES) {
            String src = Files.readString(Path.of("test", "programas", name + ".txt"));
            String exp = Files.readString(Path.of("test", "esperado", name + ".txt"));
            String got = run(src);
            check(exp.equals(got), "saída divergente de " + name + "\nESPERADO:\n" + exp + "OBTIDO:\n" + got);
        }
    }

    // ---------- 3. tipo gravado no nó e função aninhada (AST montada à mão) ----------
    static void testDecorationAndNested() {
        Program p = new Parser("function double f(int a) {\n return a + 1.5;\n}").parse().ast;
        new SemanticAnalyzer().analyze(p);
        Return ret = (Return) ((FunctionDecl) p.items.get(0)).body.statements.get(0);
        Bin sum = (Bin) ret.value;
        check(sum.left.inferredType == INT && sum.right.inferredType == FLOAT && sum.inferredType == FLOAT,
              "tipos gravados em a + 1.5");

        // procedimento externo com variável local 'base'; função interna usa 'base' (nível acima)
        Token fn = new Token(TokenType.FUNCTION, "function", 1, 1);
        VarDecl base = new VarDecl(new Type(new Token(TokenType.RESERVED_WORD, "int", 2, 5)), "base",
                new IntLiteral(new Token(TokenType.INT, "10", 2, 16)));
        Id useBase = new Id(new Token(TokenType.IDENTIFIER, "base", 4, 16));
        Bin plus = new Bin(new Token(TokenType.OPERATOR, "+", 4, 21), useBase,
                new IntLiteral(new Token(TokenType.INT, "1", 4, 23)));
        Block innerBody = new Block(new Token(TokenType.DELIMITER, "{", 3, 30),
                List.of(new Return(new Token(TokenType.RETURN, "return", 4, 9), plus)));
        FunctionDecl inner = new FunctionDecl(new Token(TokenType.FUNCTION, "function", 3, 5),
                new Type(new Token(TokenType.RESERVED_WORD, "int", 3, 14)), "proximo", List.of(), innerBody);
        CallStmt callInner = new CallStmt(new Token(TokenType.IDENTIFIER, "proximo", 6, 5), List.of());
        Block outerBody = new Block(new Token(TokenType.DELIMITER, "{", 1, 25), List.of(base, inner, callInner));
        Program nested = new Program(1, 1, List.of(new ProcedureDecl(fn, "externa", List.of(), outerBody)));
        SemanticAnalyzer a = new SemanticAnalyzer();
        a.analyze(nested);
        check(a.getErrors().isEmpty() && a.getWarnings().isEmpty(), "aninhada: " + a.report());
        check(useBase.decl != null && useBase.decl.declaration == base, "aninhada: 'base' do nível acima");
        check(plus.inferredType == INT, "aninhada: base + 1 é int");

        // CharLiteral só existe montado à mão (decisão 2)
        CharLiteral ch = new CharLiteral(new Token(TokenType.STRING, "'a'", 1, 1));
        TypeChecker tc = new TypeChecker(null);
        check(ch.accept(tc) == ERROR && tc.getErrors().size() == 1, "char recusado");

        // nenhuma exceção com árvore nula ou entrada inválida
        new SemanticAnalyzer().analyze(null);
        check(run("function int f( {").startsWith("REJEITADO"), "inválido não chega à análise");
    }

    public static void main(String[] args) throws Exception {
        testRules();
        testFixtures();
        testDecorationAndNested();
        System.out.println("OK: " + checks + " verificações CP5.");
    }
}
