import com.compiladores.entity.Scanner;
import com.compiladores.entity.Token;
import com.compiladores.entity.TokenType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;


public class ScannerTest {

    // ---------- utilidade compartilhada por vários testes ----------

    private static List<Token> tokenizarTudo(Scanner scanner) {
        List<Token> tokens = new ArrayList<>();
        Token t;
        while ((t = scanner.nextToken()) != null) {
            tokens.add(t);
        }
        return tokens;
    }

    // ---------- casos válidos (um de cada categoria) ----------

    @Test
    @DisplayName("Identificador simples")
    void identificadorSimples() {
        Scanner s = new Scanner("total");
        Token t = s.nextToken();
        assertEquals(TokenType.IDENTIFIER, t.getType());
        assertEquals("total", t.getText());
    }

    @Test
    @DisplayName("Palavra reservada 'while'")
    void palavraReservada() {
        Scanner s = new Scanner("while");
        Token t = s.nextToken();
        assertEquals(TokenType.WHILE, t.getType());
    }

    @Test
    @DisplayName("String simples")
    void stringSimples() {
        Scanner s = new Scanner("\"ok\"");
        Token t = s.nextToken();
        assertEquals(TokenType.STRING, t.getType());
        assertEquals("\"ok\"", t.getText());
    }

    @Test
    @DisplayName("Operadores, incluindo composto '<=' por maximal munch")
    void operadores() {
        Scanner s = new Scanner("+ <= =");
        List<Token> tokens = tokenizarTudo(s);

        assertEquals(TokenType.OPERATOR, tokens.get(0).getType());
        assertEquals("+", tokens.get(0).getText());

        assertEquals(TokenType.OPERATOR, tokens.get(1).getType());
        assertEquals("<=", tokens.get(1).getText());

        assertEquals(TokenType.OPERATOR, tokens.get(2).getType());
        assertEquals("=", tokens.get(2).getText());
    }

    @Test
    @DisplayName("Delimitadores")
    void delimitadores() {
        Scanner s = new Scanner("( ) { } , ;");
        List<Token> tokens = tokenizarTudo(s);

        assertEquals(6, tokens.size());
        for (Token t : tokens) {
            assertEquals(TokenType.DELIMITER, t.getType());
        }
    }

    @Test
    @DisplayName("Inteiro com zero à esquerda")
    void numeroInteiro() {
        Scanner s = new Scanner("007");
        Token t = s.nextToken();
        assertEquals(TokenType.INT, t.getType());
        assertEquals("007", t.getText());
    }

    @Test
    @DisplayName("Número real")
    void numeroReal() {
        Scanner s = new Scanner("3.14");
        Token t = s.nextToken();
        assertEquals(TokenType.REAL, t.getType());
        assertEquals("3.14", t.getText());
    }

    // ---------- casos de erro (pelo menos três exigidos pelo enunciado) ----------

    @Test
    @DisplayName("Erro: string não fechada até EOF")
    void erroStringNaoFechadaEOF() {
        Scanner s = new Scanner("\"abc");
        Token t = s.nextToken();
        assertEquals(TokenType.ERROR, t.getType());
        assertFalse(s.getErrors().isEmpty());
    }

    @Test
    @DisplayName("Erro: string não fechada até fim de linha")
    void erroStringNaoFechadaFimDeLinha() {
        Scanner s = new Scanner("\"abc\ndef\"");
        Token t = s.nextToken();
        assertEquals(TokenType.ERROR, t.getType());
    }

    @Test
    @DisplayName("Erro: caractere fora do alfabeto")
    void erroCaractereForaDoAlfabeto() {
        Scanner s = new Scanner("@");
        Token t = s.nextToken();
        assertEquals(TokenType.ERROR, t.getType());
    }

    @Test
    @DisplayName("Erro: número real mal formado ('3.')")
    void erroNumeroRealMalFormado() {
        Scanner s = new Scanner("3.");
        Token t = s.nextToken();
        assertEquals(TokenType.ERROR, t.getType());
    }

    @Test
    @DisplayName("Erro: identificador com mais de 127 caracteres")
    void erroIdentificadorMuitoLongo() {
        StringBuilder nomeGrande = new StringBuilder();
        for (int i = 0; i < 130; i++) nomeGrande.append('a');

        Scanner s = new Scanner(nomeGrande.toString());
        Token t = s.nextToken();
        assertEquals(TokenType.ERROR, t.getType());
    }

    @Test
    @DisplayName("Comentário e espaços em branco não geram token")
    void comentarioEEspacosSaoDescartados() {
        Scanner s = new Scanner("  $ isso é um comentário $   total");
        Token t = s.nextToken();
        assertEquals(TokenType.IDENTIFIER, t.getType());
        assertEquals("total", t.getText());
    }

    // ---------- trecho "realista", com vários tokens misturados ----------

    @Test
    @DisplayName("Trecho realista: função com parâmetros, corpo e comentário")
    void trechoRealista() {
        String codigo = "function int soma(int a, int b) {\n" +
                "    return a + b; $ soma dois inteiros $\n" +
                "}";
        Scanner s = new Scanner(codigo);
        List<Token> tokens = tokenizarTudo(s);

        TokenType[] esperado = {
                TokenType.FUNCTION, TokenType.RESERVED_WORD, TokenType.IDENTIFIER, TokenType.DELIMITER,
                TokenType.RESERVED_WORD, TokenType.IDENTIFIER, TokenType.DELIMITER,
                TokenType.RESERVED_WORD, TokenType.IDENTIFIER, TokenType.DELIMITER,
                TokenType.DELIMITER,
                TokenType.RETURN, TokenType.IDENTIFIER, TokenType.OPERATOR, TokenType.IDENTIFIER, TokenType.DELIMITER,
                TokenType.DELIMITER
        };

        assertEquals(esperado.length, tokens.size());
        for (int i = 0; i < esperado.length; i++) {
            assertEquals(esperado[i], tokens.get(i).getType(),
                    "token " + i + " (" + tokens.get(i).getText() + ")");
        }
    }
}