package com.compiladores.entity;

import java.util.ArrayList;
import java.util.List;


public class Parser {

    private static class ParseError extends RuntimeException {
    }

    private final List<Token> tokens;
    private int pos = 0;
    private final List<String> errors = new ArrayList<>();

    public Parser(Scanner scanner) {
        tokens = new ArrayList<>();
        Token token;
        while ((token = scanner.nextToken()) != null) {
            tokens.add(token);
        }
    }

    public Parser(String source) {
        this(new Scanner(source));
    }

    public List<String> getErrors() {
        return errors;
    }

    /** Reconhece o programa inteiro. Retorna true se não houve nenhum erro. */
    public boolean parse() {
        parsePrograma();
        if (!isAtEnd()) {
            error("token inesperado após o fim do programa, encontrado " + describe(peek()));
        }
        return errors.isEmpty();
    }

    // ---------- leitura dos tokens ----------

    private Token peek() {
        return isAtEnd() ? null : tokens.get(pos);
    }

    private boolean isAtEnd() {
        return pos >= tokens.size();
    }

    private Token advance() {
        Token token = tokens.get(pos);
        pos++;
        return token;
    }

    private boolean isType(Token t, TokenType type) {
        return t != null && t.getType() == type;
    }

    private boolean isOperator(Token t, String text) {
        return isType(t, TokenType.OPERATOR) && t.getText().equals(text);
    }

    private boolean isDelimiter(Token t, String text) {
        return isType(t, TokenType.DELIMITER) && t.getText().equals(text);
    }

    private boolean isReservedWord(Token t, String text) {
        return isType(t, TokenType.RESERVED_WORD) && t.getText().equals(text);
    }

    private Token match(TokenType type) {
        if (isType(peek(), type)) {
            return advance();
        }
        error("esperado " + nomeAmigavel(type) + ", encontrado " + describe(peek()));
        throw new ParseError();
    }

    private Token match(TokenType type, String text) {
        if (isType(peek(), type) && peek().getText().equals(text)) {
            return advance();
        }
        error("esperado '" + text + "', encontrado " + describe(peek()));
        throw new ParseError();
    }

    private void error(String message) {
        String local = isAtEnd() ? "fim do arquivo" : "linha " + peek().getLine() + ", coluna " + peek().getColumn();
        errors.add("[" + local + "] " + message);
    }

    private String describe(Token t) {
        return t == null ? "fim do arquivo" : "'" + t.getText() + "'";
    }

    private String nomeAmigavel(TokenType type) {
        switch (type) {
            case FUNCTION: return "'function'";
            case IDENTIFIER: return "um identificador";
            case IF: return "'if'";
            case WHILE: return "'while'";
            case FOR: return "'for'";
            case RETURN: return "'return'";
            default: return type.toString();
        }
    }

    // ---------- conjuntos FIRST (T, I, C, E da Seção 4 do documento) ----------

    private boolean startsTipo(Token t) {
        return isReservedWord(t, "int") || isReservedWord(t, "float") || isReservedWord(t, "double")
                || isReservedWord(t, "string") || isType(t, TokenType.LONG) || isType(t, TokenType.BOOLEAN);
    }

    private boolean startsItem(Token t) {
        return isType(t, TokenType.FUNCTION) || startsTipo(t);
    }

    private boolean startsComando(Token t) {
        return startsTipo(t) || isType(t, TokenType.IDENTIFIER) || isType(t, TokenType.IF)
                || isType(t, TokenType.WHILE) || isType(t, TokenType.FOR) || isType(t, TokenType.RETURN)
                || isDelimiter(t, "{");
    }

    private boolean startsExpr(Token t) {
        return isType(t, TokenType.IDENTIFIER) || isType(t, TokenType.INT) || isType(t, TokenType.REAL)
                || isType(t, TokenType.STRING) || isReservedWord(t, "true") || isReservedWord(t, "false")
                || isDelimiter(t, "(") || isType(t, TokenType.NOT) || isOperator(t, "+") || isOperator(t, "-");
    }

    // ---------- conjuntos FOLLOW usados nas checagens de ε (Seção 3 do documento) ----------

    private boolean emFollowExpr(Token t) {
        return isDelimiter(t, ")") || isDelimiter(t, ",") || isDelimiter(t, ";");
    }

    private boolean emFollowCaudaAnd(Token t) {
        return isType(t, TokenType.OR) || emFollowExpr(t);
    }

    private boolean emFollowCaudaIgual(Token t) {
        return isType(t, TokenType.AND) || emFollowCaudaAnd(t);
    }

    private boolean emFollowCaudaRel(Token t) {
        return isType(t, TokenType.EQUAL) || emFollowCaudaIgual(t);
    }

    private boolean emFollowCaudaSoma(Token t) {
        return isOperator(t, "<") || isOperator(t, ">") || isOperator(t, "<=") || isOperator(t, ">=")
                || emFollowCaudaRel(t);
    }

    private boolean emFollowCaudaProd(Token t) {
        return isOperator(t, "+") || isOperator(t, "-") || emFollowCaudaSoma(t);
    }

    private boolean emFollowPrimario(Token t) {
        return isOperator(t, "*") || isOperator(t, "/") || isOperator(t, "%") || emFollowCaudaProd(t);
    }

    // ---------- sincronização em modo pânico ----------

    private void sincronizarComItem() {
        while (!isAtEnd() && !startsItem(peek())) {
            advance();
        }
    }

    private void sincronizarComComando() {
        while (!isAtEnd() && !startsComando(peek()) && !isDelimiter(peek(), "}")) {
            advance();
        }
    }

    // ---------- programa e itens globais ----------

    // <programa> ::= <itens>
    private void parsePrograma() {
        parseItens();
    }

    // <itens> ::= <item> <itens> | ε
    private void parseItens() {
        while (startsItem(peek())) {
            try {
                parseItem();
            } catch (ParseError e) {
                sincronizarComItem();
            }
        }
    }

    // <item> ::= <funcao> | <decl_var>
    private void parseItem() {
        if (isType(peek(), TokenType.FUNCTION)) {
            parseFuncao();
            return;
        }
        if (startsTipo(peek())) {
            parseDeclVar();
            return;
        }
        error("esperada uma função ou declaração de variável, encontrado " + describe(peek()));
        throw new ParseError();
    }

    // <funcao> ::= "function" <tipo_retorno> ID "(" <params_opt> ")" <bloco>
    private void parseFuncao() {
        match(TokenType.FUNCTION);
        parseTipoRetorno();
        match(TokenType.IDENTIFIER);
        match(TokenType.DELIMITER, "(");
        parseParamsOpt();
        match(TokenType.DELIMITER, ")");
        parseBloco();
    }

    // <tipo_retorno> ::= <tipo> | "void"
    private void parseTipoRetorno() {
        if (isType(peek(), TokenType.VOID)) {
            advance();
            return;
        }
        parseTipo();
    }

    // <tipo> ::= "int" | "long" | "float" | "double" | "boolean" | "string"
    private void parseTipo() {
        Token t = peek();
        if (startsTipo(t)) {
            advance();
            return;
        }
        error("esperado um tipo (int, long, float, double, boolean ou string), encontrado " + describe(t));
        throw new ParseError();
    }

    // <params_opt> ::= <params> | ε
    private void parseParamsOpt() {
        if (startsTipo(peek())) {
            parseParams();
            return;
        }
        if (isDelimiter(peek(), ")")) {
            return;
        }
        error("esperado um parâmetro ou ')', encontrado " + describe(peek()));
        throw new ParseError();
    }

    // <params> ::= <param> <params_rest>
    private void parseParams() {
        parseParam();
        parseParamsRest();
    }

    // <params_rest> ::= "," <param> <params_rest> | ε
    private void parseParamsRest() {
        if (isDelimiter(peek(), ",")) {
            advance();
            parseParam();
            parseParamsRest();
            return;
        }
        if (isDelimiter(peek(), ")")) {
            return;
        }
        error("esperado ',' ou ')', encontrado " + describe(peek()));
        throw new ParseError();
    }

    // <param> ::= <tipo> ID
    private void parseParam() {
        parseTipo();
        match(TokenType.IDENTIFIER);
    }

    // <decl_var> ::= <tipo> ID <inicializador> ";"
    private void parseDeclVar() {
        parseTipo();
        match(TokenType.IDENTIFIER);
        parseInicializador();
        match(TokenType.DELIMITER, ";");
    }

    // <inicializador> ::= "=" <expr> | ε
    private void parseInicializador() {
        if (isOperator(peek(), "=")) {
            advance();
            parseExpr();
            return;
        }
        if (isDelimiter(peek(), ";")) {
            return;
        }
        error("esperado '=' ou ';', encontrado " + describe(peek()));
        throw new ParseError();
    }

    // ---------- comandos ----------

    // <bloco> ::= "{" <comandos> "}"
    private void parseBloco() {
        match(TokenType.DELIMITER, "{");
        parseComandos();
        match(TokenType.DELIMITER, "}");
    }

    // <comandos> ::= <comando> <comandos> | ε
    private void parseComandos() {
        while (startsComando(peek())) {
            try {
                parseComando();
            } catch (ParseError e) {
                sincronizarComComando();
            }
        }
    }

    // <comando> ::= <decl_var> | <comando_id> | <condicional> | <enquanto> | <para> | <retorno> | <bloco>
    private void parseComando() {
        Token t = peek();
        if (startsTipo(t)) { parseDeclVar(); return; }
        if (isType(t, TokenType.IDENTIFIER)) { parseComandoId(); return; }
        if (isType(t, TokenType.IF)) { parseCondicional(); return; }
        if (isType(t, TokenType.WHILE)) { parseEnquanto(); return; }
        if (isType(t, TokenType.FOR)) { parsePara(); return; }
        if (isType(t, TokenType.RETURN)) { parseRetorno(); return; }
        if (isDelimiter(t, "{")) { parseBloco(); return; }
        error("comando inválido, encontrado " + describe(t));
        throw new ParseError();
    }

    // <comando_id> ::= ID <resto_comando_id> ";"
    private void parseComandoId() {
        match(TokenType.IDENTIFIER);
        parseRestoComandoId();
        match(TokenType.DELIMITER, ";");
    }

    // <resto_comando_id> ::= "=" <expr> | "(" <args_opt> ")"
    private void parseRestoComandoId() {
        if (isOperator(peek(), "=")) {
            advance();
            parseExpr();
            return;
        }
        if (isDelimiter(peek(), "(")) {
            advance();
            parseArgsOpt();
            match(TokenType.DELIMITER, ")");
            return;
        }
        error("esperado '=' ou '(', encontrado " + describe(peek()));
        throw new ParseError();
    }

    // <condicional> ::= "if" "(" <expr> ")" <bloco> <cauda_cond>
    private void parseCondicional() {
        match(TokenType.IF);
        match(TokenType.DELIMITER, "(");
        parseExpr();
        match(TokenType.DELIMITER, ")");
        parseBloco();
        parseCaudaCond();
    }

    // <cauda_cond> ::= "elif" "(" <expr> ")" <bloco> <cauda_cond> | "else" <bloco> | ε
    private void parseCaudaCond() {
        if (isType(peek(), TokenType.ELIF)) {
            advance();
            match(TokenType.DELIMITER, "(");
            parseExpr();
            match(TokenType.DELIMITER, ")");
            parseBloco();
            parseCaudaCond();
            return;
        }
        if (isType(peek(), TokenType.ELSE)) {
            advance();
            parseBloco();
            return;
        }
        if (startsComando(peek()) || isDelimiter(peek(), "}")) {
            return;
        }
        error("esperado 'elif', 'else' ou o fim do bloco, encontrado " + describe(peek()));
        throw new ParseError();
    }

    // <enquanto> ::= "while" "(" <expr> ")" <bloco>
    private void parseEnquanto() {
        match(TokenType.WHILE);
        match(TokenType.DELIMITER, "(");
        parseExpr();
        match(TokenType.DELIMITER, ")");
        parseBloco();
    }

    // <para> ::= "for" "(" <init_for> ";" <cond_for> ";" <passo_for> ")" <bloco>
    private void parsePara() {
        match(TokenType.FOR);
        match(TokenType.DELIMITER, "(");
        parseInitFor();
        match(TokenType.DELIMITER, ";");
        parseCondFor();
        match(TokenType.DELIMITER, ";");
        parsePassoFor();
        match(TokenType.DELIMITER, ")");
        parseBloco();
    }

    // <init_for> ::= <tipo> ID <inicializador> | ID "=" <expr> | ε
    private void parseInitFor() {
        if (startsTipo(peek())) {
            parseTipo();
            match(TokenType.IDENTIFIER);
            parseInicializador();
            return;
        }
        if (isType(peek(), TokenType.IDENTIFIER)) {
            advance();
            match(TokenType.OPERATOR, "=");
            parseExpr();
            return;
        }
        if (isDelimiter(peek(), ";")) {
            return;
        }
        error("esperada uma declaração, uma atribuição ou ';', encontrado " + describe(peek()));
        throw new ParseError();
    }

    // <cond_for> ::= <expr> | ε
    private void parseCondFor() {
        if (startsExpr(peek())) {
            parseExpr();
            return;
        }
        if (isDelimiter(peek(), ";")) {
            return;
        }
        error("esperada uma expressão ou ';', encontrado " + describe(peek()));
        throw new ParseError();
    }

    // <passo_for> ::= ID "=" <expr> | ε
    private void parsePassoFor() {
        if (isType(peek(), TokenType.IDENTIFIER)) {
            advance();
            match(TokenType.OPERATOR, "=");
            parseExpr();
            return;
        }
        if (isDelimiter(peek(), ")")) {
            return;
        }
        error("esperada uma atribuição ou ')', encontrado " + describe(peek()));
        throw new ParseError();
    }

    // <retorno> ::= "return" <retorno_opt> ";"
    private void parseRetorno() {
        match(TokenType.RETURN);
        parseRetornoOpt();
        match(TokenType.DELIMITER, ";");
    }

    // <retorno_opt> ::= <expr> | ε
    private void parseRetornoOpt() {
        if (startsExpr(peek())) {
            parseExpr();
            return;
        }
        if (isDelimiter(peek(), ";")) {
            return;
        }
        error("esperada uma expressão ou ';', encontrado " + describe(peek()));
        throw new ParseError();
    }

    // ---------- expressões, da menor para a maior precedência ----------

    // <expr> ::= <expr_and> <cauda_or>
    private void parseExpr() {
        parseExprAnd();
        parseCaudaOr();
    }

    // <cauda_or> ::= "or" <expr_and> <cauda_or> | ε
    private void parseCaudaOr() {
        if (isType(peek(), TokenType.OR)) {
            advance();
            parseExprAnd();
            parseCaudaOr();
            return;
        }
        if (emFollowExpr(peek())) {
            return;
        }
        error("esperado 'or' ou o fim da expressão, encontrado " + describe(peek()));
        throw new ParseError();
    }

    // <expr_and> ::= <expr_igual> <cauda_and>
    private void parseExprAnd() {
        parseExprIgual();
        parseCaudaAnd();
    }

    // <cauda_and> ::= "and" <expr_igual> <cauda_and> | ε
    private void parseCaudaAnd() {
        if (isType(peek(), TokenType.AND)) {
            advance();
            parseExprIgual();
            parseCaudaAnd();
            return;
        }
        if (emFollowCaudaAnd(peek())) {
            return;
        }
        error("esperado 'and' ou o fim da expressão, encontrado " + describe(peek()));
        throw new ParseError();
    }

    // <expr_igual> ::= <expr_rel> <cauda_igual>
    private void parseExprIgual() {
        parseExprRel();
        parseCaudaIgual();
    }

    // <cauda_igual> ::= "equal" <expr_rel> | ε  (sem encadear: "equal" não é recursivo, por decisão do grupo)
    private void parseCaudaIgual() {
        if (isType(peek(), TokenType.EQUAL)) {
            advance();
            parseExprRel();
            return;
        }
        if (emFollowCaudaIgual(peek())) {
            return;
        }
        error("esperado 'equal' ou o fim da expressão, encontrado " + describe(peek()));
        throw new ParseError();
    }

    // <expr_rel> ::= <expr_soma> <cauda_rel>
    private void parseExprRel() {
        parseExprSoma();
        parseCaudaRel();
    }

    // <cauda_rel> ::= "<" <expr_soma> | ">" <expr_soma> | "<=" <expr_soma> | ">=" <expr_soma> | ε
    // (sem encadear: "a < b < c" não é válido; combine com "and", como em "a < b and b < c")
    private void parseCaudaRel() {
        Token t = peek();
        if (isOperator(t, "<") || isOperator(t, ">") || isOperator(t, "<=") || isOperator(t, ">=")) {
            advance();
            parseExprSoma();
            return;
        }
        if (emFollowCaudaRel(t)) {
            return;
        }
        error("esperado um operador relacional ou o fim da expressão, encontrado " + describe(t));
        throw new ParseError();
    }

    // <expr_soma> ::= <expr_prod> <cauda_soma>
    private void parseExprSoma() {
        parseExprProd();
        parseCaudaSoma();
    }

    // <cauda_soma> ::= "+" <expr_prod> <cauda_soma> | "-" <expr_prod> <cauda_soma> | ε
    private void parseCaudaSoma() {
        Token t = peek();
        if (isOperator(t, "+") || isOperator(t, "-")) {
            advance();
            parseExprProd();
            parseCaudaSoma();
            return;
        }
        if (emFollowCaudaSoma(t)) {
            return;
        }
        error("esperado '+', '-' ou o fim da expressão, encontrado " + describe(t));
        throw new ParseError();
    }

    // <expr_prod> ::= <expr_unaria> <cauda_prod>
    private void parseExprProd() {
        parseExprUnaria();
        parseCaudaProd();
    }

    // <cauda_prod> ::= "*" <expr_unaria> <cauda_prod> | "/" <expr_unaria> <cauda_prod> | "%" <expr_unaria> <cauda_prod> | ε
    private void parseCaudaProd() {
        Token t = peek();
        if (isOperator(t, "*") || isOperator(t, "/") || isOperator(t, "%")) {
            advance();
            parseExprUnaria();
            parseCaudaProd();
            return;
        }
        if (emFollowCaudaProd(t)) {
            return;
        }
        error("esperado '*', '/', '%' ou o fim da expressão, encontrado " + describe(t));
        throw new ParseError();
    }

    // <expr_unaria> ::= "not" <expr_unaria> | "-" <expr_unaria> | "+" <expr_unaria> | <primario>
    private void parseExprUnaria() {
        Token t = peek();
        if (isType(t, TokenType.NOT) || isOperator(t, "-") || isOperator(t, "+")) {
            advance();
            parseExprUnaria();
            return;
        }
        parsePrimario();
    }

    // <primario> ::= LIT_INT | LIT_REAL | LIT_STRING | "true" | "false" | ID <sufixo_primario> | "(" <expr> ")"
    private void parsePrimario() {
        Token t = peek();
        if (isType(t, TokenType.INT) || isType(t, TokenType.REAL) || isType(t, TokenType.STRING)) {
            advance();
            return;
        }
        if (isReservedWord(t, "true") || isReservedWord(t, "false")) {
            advance();
            return;
        }
        if (isType(t, TokenType.IDENTIFIER)) {
            advance();
            parseSufixoPrimario();
            return;
        }
        if (isDelimiter(t, "(")) {
            advance();
            parseExpr();
            match(TokenType.DELIMITER, ")");
            return;
        }
        error("esperado um valor (número, texto, identificador ou expressão entre parênteses), encontrado " + describe(t));
        throw new ParseError();
    }

    // <sufixo_primario> ::= "(" <args_opt> ")" | ε
    private void parseSufixoPrimario() {
        if (isDelimiter(peek(), "(")) {
            advance();
            parseArgsOpt();
            match(TokenType.DELIMITER, ")");
            return;
        }
        if (emFollowPrimario(peek())) {
            return;
        }
        error("esperado '(' ou o fim da expressão, encontrado " + describe(peek()));
        throw new ParseError();
    }

    // <args_opt> ::= <args> | ε
    private void parseArgsOpt() {
        if (startsExpr(peek())) {
            parseArgs();
            return;
        }
        if (isDelimiter(peek(), ")")) {
            return;
        }
        error("esperado um argumento ou ')', encontrado " + describe(peek()));
        throw new ParseError();
    }

    // <args> ::= <expr> <args_rest>
    private void parseArgs() {
        parseExpr();
        parseArgsRest();
    }

    // <args_rest> ::= "," <expr> <args_rest> | ε
    private void parseArgsRest() {
        if (isDelimiter(peek(), ",")) {
            advance();
            parseExpr();
            parseArgsRest();
            return;
        }
        if (isDelimiter(peek(), ")")) {
            return;
        }
        error("esperado ',' ou ')', encontrado " + describe(peek()));
        throw new ParseError();
    }
}
