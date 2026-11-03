import com.compiladores.entity.Parser;
import com.compiladores.visitor.SourcePrinter;
import com.compiladores.visitor.TreePrinter;
import java.nio.file.Files;
import java.nio.file.Path;

/** Suíte executável sem dependências: java -ea -cp out Cp3SmokeTest */
public final class Cp3SmokeTest {
    private static int assertions = 0;
    private static void check(boolean ok, String reason) {
        assertions++;
        if (!ok) throw new AssertionError(reason);
    }
    private static String expected(String name) throws Exception {
        return Files.readString(Path.of("test", "esperado", name + ".txt"));
    }
    private static Parser.ParseResult valid(String code) {
        Parser.ParseResult result = new Parser(code).parse();
        check(result.accepted(), "programa rejeitado: " + result.errors + "\n" + code);
        check(result.ast != null, "AST nula em entrada válida");
        return result;
    }
    private static void compareAndRoundTrip(String code, String name) throws Exception {
        var original = valid(code);
        String tree = new TreePrinter().print(original.ast);
        check(expected(name).equals(tree), "AST incorreta para " + name + "\nESPERADA:\n" + expected(name) + "OBTIDA:\n" + tree);
        String canonical = new SourcePrinter().print(original.ast);
        var reparsed = valid(canonical);
        String second = new SourcePrinter().print(reparsed.ast);
        check(canonical.equals(second), "reimpressão instável: " + name);
        check(tree.equals(new TreePrinter().print(reparsed.ast)), "AST alterada na ida e volta: " + name);
    }
    private static void invalid(String code) {
        Parser.ParseResult r = new Parser(code).parse();
        check(!r.accepted(), "entrada inválida foi aceita: " + code);
        check(r.ast == null, "árvore inválida foi liberada para outras fases");
        check(!r.errors.isEmpty(), "nenhum diagnóstico");
    }
    public static void main(String[] args) throws Exception {
        for (String name : new String[]{"soma", "controle", "para"}) {
            compareAndRoundTrip(Files.readString(Path.of("test", "programas", name + ".txt")), name);
        }
        compareAndRoundTrip("function int f() { return a + b * c; }", "precedencia");
        compareAndRoundTrip("function int f() { return a - b - c; }", "associatividade");
        compareAndRoundTrip("function int f() { return a - (b - c); }", "parenteses");

        String plain = new TreePrinter().print(valid("function int f() { return a; }").ast);
        String redundant = new TreePrinter().print(valid("function int f() { return (((a))); }").ast);
        check(plain.equals(redundant), "parênteses supérfluos introduziram nós de derivação");

        // Casos inválidos originais do CP2: falta ;, falta ), expressão vazia e relacional encadeado.
        invalid("function void f() { int x = 5 return; }");
        invalid("function void f() { if (a < b { return; } }");
        invalid("function void f() { int x = ; }");
        invalid("function void f() { int x = 0; if (a < b < c) { x = 1; } }");

        Parser.ParseResult multiple = new Parser("function void f() { int x = 5 int y = ; return y; }").parse();
        check(multiple.errors.size() >= 2, "modo pânico não recuperou dois erros: " + multiple.errors);
        check(multiple.ast == null, "AST parcial escapou após múltiplos erros");
        invalid("function void f() { return @; }"); // erro léxico também impede AST válida
        invalid("function void f() { for (int i = ; i < 2; i = i + 1) { return; } }");
        System.out.println("OK: " + assertions + " verificações CP3 e regressão CP2.");
    }
}
