package com.compiladores;

import com.compiladores.entity.Scanner;
import com.compiladores.entity.Token;
import com.compiladores.entity.TokenType;

import java.util.ArrayList;
import java.util.List;


/**
 * Testes simples com assert, sem JUnit. Para rodar, veja as instruções
 * no final da conversa (é preciso compilar com "java -ea" para os
 * asserts funcionarem).
 */
public class Main {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        testIdentificador();
        testPalavraReservada();
        testString();
        testOperadores();
        testDelimitadores();
        testNumeroInteiro();
        testNumeroReal();
        testErroStringNaoFechadaEOF();
        testErroStringNaoFechadaFimDeLinha();
        testErroCaractereForaDoAlfabeto();
        testErroNumeroRealMalFormado();
        testErroIdentificadorMuitoLongo();
        testComentarioEEspacosSaoDescartados();
        testTrechoRealista();

        System.out.println("\n" + passed + " passaram, " + failed + " falharam.");
        if (failed > 0) System.exit(1);
    }

    // ---------- utilidades ----------

    private static List<Token> tokenizarTudo(Scanner scanner) {
        List<Token> tokens = new ArrayList<>();
        Token t;
        while ((t = scanner.nextToken()) != null) {
            tokens.add(t);
        }
        return tokens;
    }

    private static void check(String nomeDoTeste, boolean condicao) {
        if (condicao) {
            passed++;
        } else {
            failed++;
            System.out.println("FALHOU: " + nomeDoTeste);
        }
    }

    // ---------- casos válidos (um de cada categoria) ----------

    private static void testIdentificador() {
        Scanner s = new Scanner("total");
        Token t = s.nextToken();
        check("identificador simples", t.getType() == TokenType.IDENTIFIER && t.getText().equals("total"));
    }

    private static void testPalavraReservada() {
        Scanner s = new Scanner("while");
        Token t = s.nextToken();
        check("palavra reservada 'while'", t.getType() == TokenType.WHILE);
    }

    private static void testString() {
        Scanner s = new Scanner("\"ok\"");
        Token t = s.nextToken();
        check("string simples", t.getType() == TokenType.STRING && t.getText().equals("\"ok\""));
    }

    private static void testOperadores() {
        Scanner s = new Scanner("+ <= =");
        List<Token> tokens = tokenizarTudo(s);
        check("operador '+'", tokens.get(0).getType() == TokenType.OPERATOR && tokens.get(0).getText().equals("+"));
        check("operador composto '<=' (maximal munch)", tokens.get(1).getType() == TokenType.OPERATOR && tokens.get(1).getText().equals("<="));
        check("operador '='", tokens.get(2).getType() == TokenType.OPERATOR && tokens.get(2).getText().equals("="));
    }

    private static void testDelimitadores() {
        Scanner s = new Scanner("( ) { } , ;");
        List<Token> tokens = tokenizarTudo(s);
        check("seis delimitadores lidos", tokens.size() == 6);
        for (Token t : tokens) {
            check("token é DELIMITER: " + t.getText(), t.getType() == TokenType.DELIMITER);
        }
    }

    private static void testNumeroInteiro() {
        Scanner s = new Scanner("007");
        Token t = s.nextToken();
        check("inteiro com zero à esquerda", t.getType() == TokenType.INT && t.getText().equals("007"));
    }

    private static void testNumeroReal() {
        Scanner s = new Scanner("3.14");
        Token t = s.nextToken();
        check("número real", t.getType() == TokenType.REAL && t.getText().equals("3.14"));
    }

    // ---------- casos de erro (pelo menos três exigidos) ----------

    private static void testErroStringNaoFechadaEOF() {
        Scanner s = new Scanner("\"abc");
        Token t = s.nextToken();
        check("string não fechada até EOF gera ERROR", t.getType() == TokenType.ERROR);
        check("erro foi registrado", !s.getErrors().isEmpty());
    }

    private static void testErroStringNaoFechadaFimDeLinha() {
        Scanner s = new Scanner("\"abc\ndef\"");
        Token t = s.nextToken();
        check("string não fechada até fim de linha gera ERROR", t.getType() == TokenType.ERROR);
    }

    private static void testErroCaractereForaDoAlfabeto() {
        Scanner s = new Scanner("@");
        Token t = s.nextToken();
        check("caractere fora do alfabeto gera ERROR", t.getType() == TokenType.ERROR);
    }

    private static void testErroNumeroRealMalFormado() {
        Scanner s = new Scanner("3.");
        Token t = s.nextToken();
        check("'3.' sem dígito depois do ponto gera ERROR", t.getType() == TokenType.ERROR);
    }

    private static void testErroIdentificadorMuitoLongo() {
        StringBuilder nomeGrande = new StringBuilder();
        for (int i = 0; i < 130; i++) nomeGrande.append('a');
        Scanner s = new Scanner(nomeGrande.toString());
        Token t = s.nextToken();
        check("identificador > 127 caracteres gera ERROR", t.getType() == TokenType.ERROR);
    }

    private static void testComentarioEEspacosSaoDescartados() {
        Scanner s = new Scanner("  $ isso é um comentário $   total");
        Token t = s.nextToken();
        check("comentário e espaços não geram token", t.getType() == TokenType.IDENTIFIER && t.getText().equals("total"));
    }

    // ---------- trecho "realista", com vários tokens misturados ----------

    private static void testTrechoRealista() {
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

        check("trecho realista tem a quantidade certa de tokens", tokens.size() == esperado.length);
        for (int i = 0; i < Math.min(tokens.size(), esperado.length); i++) {
            check("trecho realista, token " + i + " (" + tokens.get(i).getText() + ")",
                    tokens.get(i).getType() == esperado[i]);
        }
    }
}