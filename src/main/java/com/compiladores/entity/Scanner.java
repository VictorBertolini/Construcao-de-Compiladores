package com.compiladores.entity;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

public class Scanner {

    private static final Map<String, TokenType> KEYWORDS = new HashMap<>();
    static {
        KEYWORDS.put("int", TokenType.RESERVED_WORD);
        KEYWORDS.put("long", TokenType.LONG);
        KEYWORDS.put("float", TokenType.RESERVED_WORD);
        KEYWORDS.put("double", TokenType.RESERVED_WORD);
        KEYWORDS.put("boolean", TokenType.BOOLEAN);
        KEYWORDS.put("string", TokenType.RESERVED_WORD);
        KEYWORDS.put("void", TokenType.VOID);
        KEYWORDS.put("true", TokenType.RESERVED_WORD);
        KEYWORDS.put("false", TokenType.RESERVED_WORD);
        KEYWORDS.put("function", TokenType.FUNCTION);
        KEYWORDS.put("return", TokenType.RETURN);
        KEYWORDS.put("if", TokenType.IF);
        KEYWORDS.put("elif", TokenType.ELIF);
        KEYWORDS.put("else", TokenType.ELSE);
        KEYWORDS.put("while", TokenType.WHILE);
        KEYWORDS.put("for", TokenType.FOR);
        KEYWORDS.put("and", TokenType.AND);
        KEYWORDS.put("or", TokenType.OR);
        KEYWORDS.put("not", TokenType.NOT);
        KEYWORDS.put("equal", TokenType.EQUAL);
    }

    private static final int MAX_IDENTIFIER_LENGTH = 127;

    String source;
    int pos = 0, line = 1, column = 1;

    private final List<String> errors = new ArrayList<>();

    public Scanner(String source) {
        this.source = source;
    }

    public List<String> getErrors() {
        return errors;
    }

    char peek() {
        return source.charAt(pos);
    }

    char peekNext() {
        if (pos + 1 >= source.length()) return '\0';
        return source.charAt(pos + 1);
    }

    char advance() {
        char c = source.charAt(pos++);
        if (c == '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }
        return c;
    }

    boolean hasNext() {
        return pos < source.length();
    }

    public Token nextToken() {
        skipWhitespaceAndComments();

        if (!hasNext()) {
            return null;
        }

        char c = peek();

        if (Character.isDigit(c))
            return scanNumber();

        if (Character.isLetter(c) || c == '_')
            return scanIdentifierOrReserved();

        if (c == '"')
            return scanString();

        return scanOperatorOrDelimiter();
    }

    private void skipWhitespaceAndComments() {
        while (hasNext()) {
            char c = peek();

            if (c == ' ' || c == '\t' || c == '\r' || c == '\n') {
                advance();
                continue;
            }

            if (c == '$') {
                int lineInit = line, columnInit = column;
                // COM_INICIO
                advance();

                // COM_CORPO
                while (hasNext() && peek() != '$') {
                    advance();
                }

                if (!hasNext()) {
                    reportarErro("comentário não fechado até o fim do arquivo (EOF)", lineInit, columnInit);
                    return;
                }
                // COM_FIM
                advance();
                continue;
            }

            break;
        }
    }

    Token scanNumber() {
        int lineInit = line, columnInit = column;
        StringBuilder lexeme = new StringBuilder();

        // NUM_INICIO / NUM_INTEIRO
        while (hasNext() && Character.isDigit(peek()))
            lexeme.append(advance());

        if (hasNext() && peek() == '.') {
            // NUM_PONTO
            lexeme.append(advance());
            if (!hasNext() || !Character.isDigit(peek())) {
                reportarErro("número real mal formado: " + lexeme, lineInit, columnInit);
                return new Token(TokenType.ERROR, lexeme.toString(), lineInit, columnInit);
            }
            // NUM_REAL
            while (hasNext() && Character.isDigit(peek())) lexeme.append(advance());
            return new Token(TokenType.REAL, lexeme.toString(), lineInit, columnInit);
        }
        return new Token(TokenType.INT, lexeme.toString(), lineInit, columnInit);
    }

    private Token scanIdentifierOrReserved() {
        int lineInit = line, columnInit = column;
        StringBuilder lexeme = new StringBuilder();

        // ID_INICIO
        lexeme.append(advance());
        //ID_CORPO
        while (hasNext() && (Character.isLetterOrDigit(peek()) || peek() == '_')) {
            lexeme.append(advance());
        }

        String text = lexeme.toString();

        if (text.length() > MAX_IDENTIFIER_LENGTH) {
            reportarErro("identificador com mais de " + MAX_IDENTIFIER_LENGTH + " caracteres: " + text, lineInit, columnInit);
            return new Token(TokenType.ERROR, text, lineInit, columnInit);
        }

        TokenType type = KEYWORDS.getOrDefault(text, TokenType.IDENTIFIER);
        return new Token(type, text, lineInit, columnInit);
    }

    private Token scanString() {
        int lineInit = line, columnInit = column;
        StringBuilder lexeme = new StringBuilder();
        // STR_INICIO
        lexeme.append(advance());

        while (true) {
            if (!hasNext()) {
                reportarErro("string não fechada até o fim do arquivo (EOF)", lineInit, columnInit);
                return new Token(TokenType.ERROR, lexeme.toString(), lineInit, columnInit);
            }

            char c = peek();

            if (c == '\n') {
                reportarErro("string não fechada até o fim da linha", lineInit, columnInit);
                return new Token(TokenType.ERROR, lexeme.toString(), lineInit, columnInit);
            }

            if (c == '"') {
                lexeme.append(advance());
                // STR_FIM
                return new Token(TokenType.STRING, lexeme.toString(), lineInit, columnInit);
            }

            if (c == '\\') {
                // STR_ESCAPE
                lexeme.append(advance());

                if (!hasNext()) {
                    reportarErro("string não fechada até o fim do arquivo (EOF)", lineInit, columnInit);
                    return new Token(TokenType.ERROR, lexeme.toString(), lineInit, columnInit);
                }

                char escaped = peek();
                if (escaped == '"' || escaped == '\\' || escaped == 'n' || escaped == 't') {
                    lexeme.append(advance());
                } else {
                    reportarErro("sequência de escape inválida: \\" + escaped, line, column);
                    lexeme.append(advance());
                }
                continue;
            }

            // STR_CORPO
            lexeme.append(advance());
        }
    }

    private Token scanOperatorOrDelimiter() {
        int lineInit = line, columnInit = column;
        char c = peek();

        switch (c) {
            case '+':
            case '-':
            case '*':
            case '/':
            case '%':
            case '=':
                // OP_SIMPLES
                advance();
                return new Token(TokenType.OPERATOR, String.valueOf(c), lineInit, columnInit);

            case '<':
            case '>':
                // OP_RELACIONAL
                advance();
                if (hasNext() && peek() == '=') {
                    // OP_COMPOSTO
                    advance();
                    return new Token(TokenType.OPERATOR, c + "=", lineInit, columnInit);
                }
                return new Token(TokenType.OPERATOR, String.valueOf(c), lineInit, columnInit);

            case '(':
            case ')':
            case '{':
            case '}':
            case ',':
            case ';':
                // DEL_FIM
                advance();
                return new Token(TokenType.DELIMITER, String.valueOf(c), lineInit, columnInit);

            default:
                advance();
                reportarErro("caractere fora do alfabeto: '" + c + "'", lineInit, columnInit);
                return new Token(TokenType.ERROR, String.valueOf(c), lineInit, columnInit);
        }
    }

    private void reportarErro(String errorText, int lineInit, int columnInit) {
        String mensagem = "[linha " + lineInit + ", coluna " + columnInit + "] " + errorText;
        errors.add(mensagem);
        System.err.println(mensagem);
    }
}