import com.compiladores.ast.Ast.*;
import com.compiladores.entity.Parser;
import com.compiladores.entity.Token;
import com.compiladores.entity.TokenType;
import com.compiladores.symbols.Symbol;
import com.compiladores.visitor.IdentificationVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IdentificationVisitorTest {
    private static Program parse(String code) {
        Parser.ParseResult result = new Parser(code).parse();
        assertTrue(result.accepted(), "erro sintático/léxico: " + result.errors + "\n" + code);
        return result.ast;
    }

    @Test
    void programasConferemComOsResultadosPrevistosManualmente() throws Exception {
        String[] fixtures = {
            "ambiente_manual", "escopos_sombreamento", "redeclaracao", "nao_declarado",
            "uso_antes_declaracao", "recursao_mutua", "parametro_corpo", "espaco_nomes",
            "tres_erros", "procedimento_recursivo", "fora_do_escopo"
        };
        for (String fixture : fixtures) {
            String source = Files.readString(Path.of("test", "programas", fixture + ".txt"));
            String expected = Files.readString(Path.of("test", "esperado", fixture + ".txt"));
            IdentificationVisitor visitor = new IdentificationVisitor();
            visitor.analyze(parse(source));
            assertEquals(expected, visitor.report(), "erro na saída de " + fixture);
            assertEquals(0, visitor.currentLevel(), "escopo vazou em " + fixture);
        }
    }

    @Test
    void declApontaParaDeclaracaoCertaMesmoComSombreamento() {
        Program program = parse("int x = 10;\nfunction int f(int x) {\n int y = x;\n { int x = y; y = x; }\n return x;\n}");
        FunctionDecl function = (FunctionDecl) program.items.get(1);
        VarDecl y = (VarDecl) function.body.statements.get(0);
        assertSame(function.params.get(0), ((Id) y.initial).decl.declaration);

        Block inner = (Block) function.body.statements.get(1);
        VarDecl localX = (VarDecl) inner.statements.get(0);
        Assign inside = (Assign) inner.statements.get(1);
        assertSame(y, inside.target.decl.declaration);
        assertSame(localX, ((Id) inside.value).decl.declaration);

        Return returned = (Return) function.body.statements.get(2);
        assertSame(function.params.get(0), ((Id) returned.value).decl.declaration);
    }

    @Test
    void recursaoDiretaEChamadasTemVinculoExternoSemMudarAST() {
        Program program = parse("function void repetir(int n) { repetir(n); }");
        ProcedureDecl procedure = (ProcedureDecl) program.items.get(0);
        CallStmt invocation = (CallStmt) procedure.body.statements.get(0);
        IdentificationVisitor visitor = new IdentificationVisitor();
        visitor.analyze(program);
        assertTrue(visitor.getErrors().isEmpty());
        Symbol found = visitor.getCallDeclarations().get(invocation.call);
        assertNotNull(found);
        assertSame(procedure, found.declaration);
        assertSame(procedure.params.get(0), ((Id) invocation.call.args.get(0)).decl.declaration);
    }

    @Test
    void todosErrosSaoAcumuladosNaOrdemTextual() {
        Program program = parse("function void f() {\nint a = 1;\nint a = 2;\nb = 3;\nc = 4;\n}");
        IdentificationVisitor visitor = new IdentificationVisitor();
        visitor.analyze(program);
        assertEquals(List.of(
            "linha 3: 'a' já declarado neste escopo (linha 2)",
            "linha 4: 'b' não declarado",
            "linha 5: 'c' não declarado"
        ), visitor.getErrors());
    }

    @Test
    void inicializadorDeForNaoVazaEArvoreNulaEhIgnorada() {
        Program program = parse("function void f() {\n"
            + "for (int i = 0; i < 2; i = i + 1) { i = 4; }\n"
            + "i = 3;\n}");
        IdentificationVisitor visitor = new IdentificationVisitor();
        visitor.analyze(program);
        assertEquals(List.of("linha 3: 'i' não declarado"), visitor.getErrors());
        assertEquals(0, visitor.currentLevel());

        Parser.ParseResult invalid = new Parser("function void f() { int x = ; }").parse();
        assertFalse(invalid.accepted());
        assertDoesNotThrow(() -> new IdentificationVisitor().analyze(invalid.ast));
    }

    @Test
    void funcaoAninhadaNaAstUsaAsMesmasRegras() {
        // O parser CP3 não aceita funções aninhadas, mas seu Block já permite Node.
        Token fn = new Token(TokenType.FUNCTION, "function", 2, 1);
        Token call = new Token(TokenType.IDENTIFIER, "interna", 4, 1);
        Type returnType = new Type(new Token(TokenType.VOID, "void", 2, 10));
        Block innerBody = new Block(new Token(TokenType.DELIMITER, "{", 2, 24), List.of());
        ProcedureDecl inner = new ProcedureDecl(fn, "interna", List.of(), innerBody);
        CallStmt invocation = new CallStmt(call, List.of());
        Block outerBody = new Block(new Token(TokenType.DELIMITER, "{", 1, 24), List.of(inner, invocation));
        ProcedureDecl outer = new ProcedureDecl(
            new Token(TokenType.FUNCTION, "function", 1, 1), "externa", List.of(), outerBody);
        Program ast = new Program(1, 1, List.of(outer));
        IdentificationVisitor visitor = new IdentificationVisitor();
        visitor.analyze(ast);
        assertTrue(visitor.getErrors().isEmpty());
        assertSame(inner, visitor.getCallDeclarations().get(invocation.call).declaration);
        assertEquals(0, visitor.currentLevel());
    }
}
