import com.compiladores.ast.Ast.*;
import com.compiladores.entity.Parser;
import com.compiladores.symbols.StackSymbolTable;
import com.compiladores.symbols.Symbol;
import com.compiladores.symbols.Symbol.Category;
import com.compiladores.symbols.SymbolTable;
import com.compiladores.visitor.IdentificationVisitor;
import com.compiladores.entity.Token;
import com.compiladores.entity.TokenType;
import java.nio.file.Files;
import java.nio.file.Path;

/** Executável sem JUnit; acompanha SymbolTableTest e IdentificationVisitorTest. */
public final class Cp4SmokeTest {
    private static int checks;

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static Program parse(String code) {
        Parser.ParseResult result = new Parser(code).parse();
        check(result.accepted(), "programa rejeitado: " + result.errors + "\n" + code);
        return result.ast;
    }

    private static Symbol symbol(String name, int line, int level) {
        Type type = new Type(new Token(TokenType.RESERVED_WORD, "int", line, 1));
        return new Symbol(name, Category.VARIABLE, type, level, type);
    }

    private static void testTable() {
        SymbolTable table = new StackSymbolTable();
        check(table.level() == 0, "nível global");
        check(table.lookup("x") == null, "busca inexistente");
        Symbol outer = symbol("x", 1, 0);
        check(table.declare(outer), "declaração global");
        check(!table.declare(symbol("x", 2, 0)), "redeclaração permitida indevidamente");
        check(table.lookup("x") == outer, "primeira declaração substituída");
        table.openScope();
        check(table.level() == 1, "nível local");
        check(table.lookupLocal("x") == null, "lookupLocal viu escopo externo");
        check(table.lookup("x") == outer, "lookup não viu escopo externo");
        Symbol inner = symbol("x", 3, 1);
        check(table.declare(inner), "sombreamento rejeitado");
        check(table.lookup("x") == inner, "lookup não preferiu escopo interno");
        table.closeScope();
        check(table.lookup("x") == outer && table.level() == 0, "escopo externo não restaurado");
        boolean explained = false;
        try { table.closeScope(); }
        catch (IllegalStateException e) { explained = e.getMessage().contains("global"); }
        check(explained, "closeScope global sem diagnóstico claro");
    }

    private static void testFixtures() throws Exception {
        String[] fixtures = {
            "ambiente_manual", "escopos_sombreamento", "redeclaracao", "nao_declarado",
            "uso_antes_declaracao", "recursao_mutua", "parametro_corpo", "espaco_nomes",
            "tres_erros", "procedimento_recursivo", "fora_do_escopo"
        };
        for (String name : fixtures) {
            String source = Files.readString(Path.of("test", "programas", name + ".txt"));
            String expected = Files.readString(Path.of("test", "esperado", name + ".txt"));
            IdentificationVisitor visitor = new IdentificationVisitor();
            visitor.analyze(parse(source));
            check(expected.equals(visitor.report()), "saída divergente de " + name
                    + "\nESPERADO:\n" + expected + "\nOBTIDO:\n" + visitor.report());
            check(visitor.currentLevel() == 0, "escopo aberto após " + name);
        }
    }

    private static void testDecorations() {
        Program program = parse("int x = 10;\nfunction int f(int x) {\n int y = x;\n { int x = y; y = x; }\n return x;\n}");
        FunctionDecl f = (FunctionDecl) program.items.get(1);
        VarDecl y = (VarDecl) f.body.statements.get(0);
        Block nested = (Block) f.body.statements.get(1);
        VarDecl innerX = (VarDecl) nested.statements.get(0);
        Assign nestedAssign = (Assign) nested.statements.get(1);
        Return last = (Return) f.body.statements.get(2);
        IdentificationVisitor visitor = new IdentificationVisitor();
        visitor.analyze(program);
        check(visitor.getErrors().isEmpty(), "programa válido não pode gerar erro");
        check(((Id) y.initial).decl.declaration == f.params.get(0), "parametro x");
        check(nestedAssign.target.decl.declaration == y, "atribuição y");
        check(((Id) nestedAssign.value).decl.declaration == innerX, "x interno");
        check(((Id) last.value).decl.declaration == f.params.get(0), "x após fechar bloco");

        Program self = parse("function void repetir(int n) { repetir(n); }");
        ProcedureDecl p = (ProcedureDecl) self.items.get(0);
        CallStmt call = (CallStmt) p.body.statements.get(0);
        IdentificationVisitor selfVisitor = new IdentificationVisitor();
        selfVisitor.analyze(self);
        check(selfVisitor.getErrors().isEmpty(), "recursão direta inválida");
        check(selfVisitor.getCallDeclarations().get(call.call).declaration == p, "resolução da chamada");
        check(((Id) call.call.args.get(0)).decl.declaration == p.params.get(0), "argumento da chamada");
    }

    private static void testInvalidAndFor() {
        IdentificationVisitor visitor = new IdentificationVisitor();
        visitor.analyze(parse("function void f() {\nfor (int i = 0; i < 2; i = i + 1) { i = 4; }\ni = 3;\n}"));
        check(visitor.getErrors().size() == 1, "número de erros do for");
        check(visitor.getErrors().get(0).equals("linha 3: 'i' não declarado"), "variável do for vazou");
        check(visitor.currentLevel() == 0, "escopo do for vazou");
        Parser.ParseResult invalid = new Parser("function void f() { int x = ; }").parse();
        check(!invalid.accepted() && invalid.ast == null, "parser liberou AST inválida");
        new IdentificationVisitor().analyze(invalid.ast); // não lança NPE
    }

    public static void main(String[] args) throws Exception {
        testTable();
        testFixtures();
        testDecorations();
        testInvalidAndFor();
        System.out.println("OK: " + checks + " verificações CP4.");
    }
}
