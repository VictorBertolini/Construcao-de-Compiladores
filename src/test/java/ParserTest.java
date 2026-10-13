import com.compiladores.entity.Scanner;
import com.compiladores.entity.Parser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ParserTest {

    private static boolean aceita(String codigo) {
        return new Parser(new Scanner(codigo)).parse();
    }

    private static Parser parserPara(String codigo) {
        Parser parser = new Parser(new Scanner(codigo));
        parser.parse();
        return parser;
    }

    // ---------- programas válidos (pelo menos 3, exigidos pela Seção 3.3) ----------

    @Test
    @DisplayName("Válido: função com parâmetros, variável local e retorno com valor")
    void funcaoComParametrosEVariavelLocal() {
        String codigo = """
            function int soma(int a, int b) {
                int total = a + b;
                return total;
            }
            """;
        assertTrue(aceita(codigo));
    }

    @Test
    @DisplayName("Válido: if/elif/else, while, chamada e relacional combinado com 'and'")
    void controleDeFluxoEChamada() {
        String codigo = """
            function void checar(int a, int b, int c) {
                if (a < b and b < c) {
                    print("crescente");
                } elif (a equal b) {
                    print("igual");
                } else {
                    print("outro");
                }
                while (a < c) {
                    a = a + 1;
                }
                return;
            }
            """;
        assertTrue(aceita(codigo));
    }

    @Test
    @DisplayName("Válido: for, boolean, string e precedência aritmética")
    void forETiposEPrecedencia() {
        String codigo = """
            function double calcular() {
                boolean ok = true;
                string nome = "teste";
                double resultado = 2 + 3 * 4 - 1;
                for (int i = 0; i < 10; i = i + 1) {
                    resultado = resultado + i;
                }
                return resultado;
            }
            """;
        assertTrue(aceita(codigo));
    }

    // ---------- programas inválidos (pelo menos 3, cada um com erro diferente) ----------

    @Test
    @DisplayName("Inválido: falta ';' depois de uma declaração")
    void erroFaltaPontoEVirgula() {
        String codigo = """
            function void f() {
                int x = 5
                return;
            }
            """;
        Parser parser = parserPara(codigo);
        assertFalse(parser.getErrors().isEmpty());
    }

    @Test
    @DisplayName("Inválido: falta ')' na condição do if")
    void erroFaltaFechaParenteses() {
        String codigo = """
            function void f() {
                if (a < b {
                    return;
                }
            }
            """;
        Parser parser = parserPara(codigo);
        assertFalse(parser.getErrors().isEmpty());
    }

    @Test
    @DisplayName("Inválido: expressão vazia depois de '='")
    void erroExpressaoVazia() {
        String codigo = """
            function void f() {
                int x = ;
            }
            """;
        Parser parser = parserPara(codigo);
        assertFalse(parser.getErrors().isEmpty());
    }

    @Test
    @DisplayName("Inválido: relacional encadeado (a < b < c) não é mais permitido")
    void erroRelacionalEncadeado() {
        String codigo = """
            function void f() {
                int x = 0;
                if (a < b < c) {
                    x = 1;
                }
            }
            """;
        Parser parser = parserPara(codigo);
        assertFalse(parser.getErrors().isEmpty());
    }

    // ---------- múltiplos erros numa única rodada (exigido pela Seção 3.3) ----------

    @Test
    @DisplayName("Múltiplos erros: dois erros distintos no mesmo programa, numa só análise")
    void doisErrosNoMesmoPrograma() {
        String codigo = """
            function void f() {
                int x = 5
                int y = ;
                return y;
            }
            """;
        Parser parser = parserPara(codigo);
        assertTrue(parser.getErrors().size() >= 2,
                "esperava pelo menos 2 erros, achou " + parser.getErrors().size());
    }
}
