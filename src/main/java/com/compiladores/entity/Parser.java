package com.compiladores.entity;

import com.compiladores.ast.Ast.*;
import java.util.ArrayList;
import java.util.List;

/** Parser LL(1) do CP2, agora com construção de AST e recuperação em modo pânico. */
public final class Parser {
    private static final class ParseError extends RuntimeException { }

    public static final class ParseResult {
        /** Nula se houver erros sintáticos ou léxicos. */
        public final Program ast;
        public final List<String> errors;
        private ParseResult(Program ast, List<String> errors) {
            this.ast = ast; this.errors = List.copyOf(errors);
        }
        public boolean accepted() { return ast != null && errors.isEmpty(); }
    }

    private final List<Token> tokens = new ArrayList<>();
    private final List<String> errors = new ArrayList<>();
    private int pos = 0;
    private ParseResult result;

    public Parser(Scanner scanner) {
        Token t;
        while ((t = scanner.nextToken()) != null) tokens.add(t);
        errors.addAll(scanner.getErrors());
    }
    public Parser(String source) { this(new Scanner(source)); }
    public List<String> getErrors() { return List.copyOf(errors); }
    public ParseResult parse() {
        if (result != null) return result;
        Program tree = parsePrograma();
        if (!isAtEnd()) error("token inesperado após o fim do programa, encontrado " + describe(peek()));
        result = new ParseResult(errors.isEmpty() ? tree : null, errors);
        return result;
    }
    /** Compatibilidade para consumidores do resultado booleano do CP2. */
    public boolean parseAceito() { return parse().accepted(); }

    private Token peek() { return isAtEnd() ? null : tokens.get(pos); }
    private boolean isAtEnd() { return pos >= tokens.size(); }
    private Token advance() { return tokens.get(pos++); }
    private boolean isType(Token t, TokenType type) { return t != null && t.getType() == type; }
    private boolean isOperator(Token t, String text) { return isType(t, TokenType.OPERATOR) && t.getText().equals(text); }
    private boolean isDelimiter(Token t, String text) { return isType(t, TokenType.DELIMITER) && t.getText().equals(text); }
    private boolean isReservedWord(Token t, String text) { return isType(t, TokenType.RESERVED_WORD) && t.getText().equals(text); }
    private Token match(TokenType type) {
        if (isType(peek(), type)) return advance();
        invalid("esperado " + nomeAmigavel(type));
        throw new AssertionError();
    }
    private Token match(TokenType type, String text) {
        if (isType(peek(), type) && peek().getText().equals(text)) return advance();
        invalid("esperado '" + text + "'");
        throw new AssertionError();
    }
    private void error(String message) {
        String at = isAtEnd() ? "fim do arquivo" : "linha " + peek().getLine() + ", coluna " + peek().getColumn();
        errors.add("[" + at + "] " + message);
    }
    private void invalid(String expected) { error(expected + ", encontrado " + describe(peek())); throw new ParseError(); }
    private String describe(Token t) { return t == null ? "fim do arquivo" : "'" + t.getText() + "'"; }
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

    // Mesmas decisões FIRST/FOLLOW do CP2.
    private boolean startsTipo(Token t) {
        return isReservedWord(t, "int") || isReservedWord(t, "float") || isReservedWord(t, "double")
                || isReservedWord(t, "string") || isType(t, TokenType.LONG) || isType(t, TokenType.BOOLEAN);
    }
    private boolean startsItem(Token t) { return isType(t, TokenType.FUNCTION) || startsTipo(t); }
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
    private boolean emFollowExpr(Token t) { return isDelimiter(t, ")") || isDelimiter(t, ",") || isDelimiter(t, ";"); }
    private boolean emFollowCaudaAnd(Token t) { return isType(t, TokenType.OR) || emFollowExpr(t); }
    private boolean emFollowCaudaIgual(Token t) { return isType(t, TokenType.AND) || emFollowCaudaAnd(t); }
    private boolean emFollowCaudaRel(Token t) { return isType(t, TokenType.EQUAL) || emFollowCaudaIgual(t); }
    private boolean emFollowCaudaSoma(Token t) {
        return isOperator(t, "<") || isOperator(t, ">") || isOperator(t, "<=") || isOperator(t, ">=") || emFollowCaudaRel(t);
    }
    private boolean emFollowCaudaProd(Token t) { return isOperator(t, "+") || isOperator(t, "-") || emFollowCaudaSoma(t); }
    private boolean emFollowPrimario(Token t) {
        return isOperator(t, "*") || isOperator(t, "/") || isOperator(t, "%") || emFollowCaudaProd(t);
    }

    private void sincronizarComItem(int start) {
        if (pos == start && !isAtEnd()) advance();
        while (!isAtEnd() && !startsItem(peek())) advance();
    }
    private void sincronizarComComando(int start) {
        if (pos == start && !isAtEnd()) advance();
        while (!isAtEnd() && !startsComando(peek()) && !isDelimiter(peek(), "}")) advance();
    }

    // <programa> ::= <itens>; <itens> ::= <item> <itens> | ε
    private Program parsePrograma() {
        Token first = peek();
        List<Item> items = new ArrayList<>();
        while (startsItem(peek())) {
            int start = pos;
            try { items.add(parseItem()); } catch (ParseError e) { sincronizarComItem(start); }
        }
        return new Program(first == null ? 1 : first.getLine(), first == null ? 1 : first.getColumn(), items);
    }
    private Item parseItem() {
        if (isType(peek(), TokenType.FUNCTION)) return parseFuncao();
        if (startsTipo(peek())) return parseDeclVar(true);
        invalid("esperada uma função ou declaração de variável");
        throw new AssertionError();
    }
    // <funcao> ::= function <tipo_retorno> ID ( <params_opt> ) <bloco>
    private Item parseFuncao() {
        Token keyword = match(TokenType.FUNCTION);
        boolean isVoid = isType(peek(), TokenType.VOID);
        Type ret = new Type(isVoid ? advance() : parseTipoToken());
        Token name = match(TokenType.IDENTIFIER);
        match(TokenType.DELIMITER, "(");
        List<Param> params = parseParamsOpt();
        match(TokenType.DELIMITER, ")");
        Block body = parseBloco();
        return isVoid ? new ProcedureDecl(keyword, name.getText(), params, body)
                : new FunctionDecl(keyword, ret, name.getText(), params, body);
    }
    private Token parseTipoToken() {
        if (startsTipo(peek())) return advance();
        invalid("esperado um tipo (int, long, float, double, boolean ou string)");
        throw new AssertionError();
    }
    private Type parseTipo() { return new Type(parseTipoToken()); }
    // <params_opt> ::= <params> | ε; <params_rest> ::= "," <param> <params_rest> | ε
    private List<Param> parseParamsOpt() {
        List<Param> params = new ArrayList<>();
        if (startsTipo(peek())) {
            params.add(parseParam());
            while (isDelimiter(peek(), ",")) { advance(); params.add(parseParam()); }
        }
        if (!isDelimiter(peek(), ")")) invalid("esperado um parâmetro, ',' ou ')'");
        return params;
    }
    private Param parseParam() { Type type = parseTipo(); return new Param(type, match(TokenType.IDENTIFIER)); }

    // <decl_var> ::= <tipo> ID <inicializador> ";"; no for o ; pertence ao cabeçalho.
    private VarDecl parseDeclVar(boolean semicolon) {
        Type type = parseTipo();
        String name = match(TokenType.IDENTIFIER).getText();
        Expr initial = parseInicializador();
        if (semicolon) match(TokenType.DELIMITER, ";");
        return new VarDecl(type, name, initial);
    }
    private Expr parseInicializador() {
        if (isOperator(peek(), "=")) { advance(); return parseExpr(); }
        if (isDelimiter(peek(), ";")) return null;
        invalid("esperado '=' ou ';'");
        throw new AssertionError();
    }

    // <bloco> ::= "{" <comandos> "}"
    private Block parseBloco() {
        Token opening = match(TokenType.DELIMITER, "{");
        List<Node> statements = new ArrayList<>();
        while (startsComando(peek())) {
            int start = pos;
            try { statements.add(parseComando()); }
            catch (ParseError e) { sincronizarComComando(start); }
        }
        match(TokenType.DELIMITER, "}");
        return new Block(opening, statements);
    }
    private Node parseComando() {
        Token t = peek();
        if (startsTipo(t)) return parseDeclVar(true);
        if (isType(t, TokenType.IDENTIFIER)) return parseComandoId();
        if (isType(t, TokenType.IF)) return parseCondicional();
        if (isType(t, TokenType.WHILE)) return parseEnquanto();
        if (isType(t, TokenType.FOR)) return parsePara();
        if (isType(t, TokenType.RETURN)) return parseRetorno();
        if (isDelimiter(t, "{")) return parseBloco();
        invalid("comando inválido");
        throw new AssertionError();
    }
    // <comando_id> ::= ID ("=" <expr> | "(" <args_opt> ")") ";"
    private Stmt parseComandoId() {
        Token name = match(TokenType.IDENTIFIER);
        Stmt s;
        if (isOperator(peek(), "=")) { advance(); s = new Assign(name, parseExpr()); }
        else if (isDelimiter(peek(), "(")) {
            advance();
            List<Expr> args = parseArgsOpt();
            match(TokenType.DELIMITER, ")");
            s = new CallStmt(name, args);
        } else { invalid("esperado '=' ou '('"); throw new AssertionError(); }
        match(TokenType.DELIMITER, ";");
        return s;
    }
    // <condicional> ::= if "(" <expr> ")" <bloco> <cauda_cond>
    private If parseCondicional() {
        Token token = match(TokenType.IF);
        List<Branch> branches = new ArrayList<>();
        branches.add(parseBranch(token));
        while (isType(peek(), TokenType.ELIF)) branches.add(parseBranch(advance()));
        Block elseBody = null;
        if (isType(peek(), TokenType.ELSE)) { advance(); elseBody = parseBloco(); }
        else if (!startsComando(peek()) && !isDelimiter(peek(), "}")) invalid("esperado 'elif', 'else' ou o fim do bloco");
        return new If(token, branches, elseBody);
    }
    private Branch parseBranch(Token keyword) {
        match(TokenType.DELIMITER, "(");
        Expr condition = parseExpr();
        match(TokenType.DELIMITER, ")");
        return new Branch(keyword, condition, parseBloco());
    }
    private While parseEnquanto() {
        Token t = match(TokenType.WHILE);
        match(TokenType.DELIMITER, "(");
        Expr condition = parseExpr();
        match(TokenType.DELIMITER, ")");
        return new While(t, condition, parseBloco());
    }
    private For parsePara() {
        Token t = match(TokenType.FOR);
        match(TokenType.DELIMITER, "(");
        Node init = parseInitFor();
        match(TokenType.DELIMITER, ";");
        Expr condition = parseCondFor();
        match(TokenType.DELIMITER, ";");
        Assign step = parsePassoFor();
        match(TokenType.DELIMITER, ")");
        return new For(t, init, condition, step, parseBloco());
    }
    private Node parseInitFor() {
        if (startsTipo(peek())) return parseDeclVar(false);
        if (isType(peek(), TokenType.IDENTIFIER)) return parseAssignFor();
        if (isDelimiter(peek(), ";")) return null;
        invalid("esperada uma declaração, uma atribuição ou ';'");
        throw new AssertionError();
    }
    private Assign parseAssignFor() {
        Token id = match(TokenType.IDENTIFIER);
        match(TokenType.OPERATOR, "=");
        return new Assign(id, parseExpr());
    }
    private Expr parseCondFor() {
        if (startsExpr(peek())) return parseExpr();
        if (isDelimiter(peek(), ";")) return null;
        invalid("esperada uma expressão ou ';'");
        throw new AssertionError();
    }
    private Assign parsePassoFor() {
        if (isType(peek(), TokenType.IDENTIFIER)) return parseAssignFor();
        if (isDelimiter(peek(), ")")) return null;
        invalid("esperada uma atribuição ou ')'");
        throw new AssertionError();
    }
    private Return parseRetorno() {
        Token t = match(TokenType.RETURN);
        Expr value = null;
        if (startsExpr(peek())) value = parseExpr();
        else if (!isDelimiter(peek(), ";")) invalid("esperada uma expressão ou ';'");
        match(TokenType.DELIMITER, ";");
        return new Return(t, value);
    }

    // Caudas por laços: recebem a AST esquerda e acumulam Bin, com associatividade à esquerda.
    private Expr parseExpr() {
        Expr left = parseExprAnd();
        while (isType(peek(), TokenType.OR)) { Token op = advance(); left = new Bin(op, left, parseExprAnd()); }
        if (!emFollowExpr(peek())) invalid("esperado 'or' ou o fim da expressão");
        return left;
    }
    private Expr parseExprAnd() {
        Expr left = parseExprIgual();
        while (isType(peek(), TokenType.AND)) { Token op = advance(); left = new Bin(op, left, parseExprIgual()); }
        if (!emFollowCaudaAnd(peek())) invalid("esperado 'and' ou o fim da expressão");
        return left;
    }
    private Expr parseExprIgual() {
        Expr left = parseExprRel();
        if (isType(peek(), TokenType.EQUAL)) { Token op = advance(); left = new Bin(op, left, parseExprRel()); }
        if (!emFollowCaudaIgual(peek())) invalid("esperado 'equal' ou o fim da expressão");
        return left;
    }
    private Expr parseExprRel() {
        Expr left = parseExprSoma();
        if (isRel(peek())) { Token op = advance(); left = new Bin(op, left, parseExprSoma()); }
        if (!emFollowCaudaRel(peek())) invalid("esperado um operador relacional ou o fim da expressão");
        return left;
    }
    private boolean isRel(Token t) { return isOperator(t, "<") || isOperator(t, ">") || isOperator(t, "<=") || isOperator(t, ">="); }
    private Expr parseExprSoma() {
        Expr left = parseExprProd();
        while (isOperator(peek(), "+") || isOperator(peek(), "-")) {
            Token op = advance(); left = new Bin(op, left, parseExprProd());
        }
        if (!emFollowCaudaSoma(peek())) invalid("esperado '+', '-' ou o fim da expressão");
        return left;
    }
    private Expr parseExprProd() {
        Expr left = parseExprUnaria();
        while (isOperator(peek(), "*") || isOperator(peek(), "/") || isOperator(peek(), "%")) {
            Token op = advance(); left = new Bin(op, left, parseExprUnaria());
        }
        if (!emFollowCaudaProd(peek())) invalid("esperado '*', '/', '%' ou o fim da expressão");
        return left;
    }
    private Expr parseExprUnaria() {
        Token t = peek();
        if (isType(t, TokenType.NOT) || isOperator(t, "-") || isOperator(t, "+")) {
            advance(); return new Unary(t, parseExprUnaria());
        }
        return parsePrimario();
    }
    private Expr parsePrimario() {
        Token t = peek();
        if (isType(t, TokenType.INT)) { advance(); return new IntLiteral(t); }
        if (isType(t, TokenType.REAL)) { advance(); return new RealLiteral(t); }
        if (isType(t, TokenType.STRING)) { advance(); return new StringLiteral(t); }
        if (isReservedWord(t, "true") || isReservedWord(t, "false")) { advance(); return new BoolLiteral(t); }
        if (isType(t, TokenType.IDENTIFIER)) {
            advance();
            if (isDelimiter(peek(), "(")) {
                advance();
                List<Expr> args = parseArgsOpt();
                match(TokenType.DELIMITER, ")");
                return new CallExpr(t, args);
            }
            if (!emFollowPrimario(peek())) invalid("esperado '(' ou o fim da expressão");
            return new Id(t);
        }
        if (isDelimiter(t, "(")) {
            advance(); Expr expr = parseExpr(); match(TokenType.DELIMITER, ")"); return expr;
        }
        invalid("esperado um valor (número, texto, identificador ou expressão entre parênteses)");
        throw new AssertionError();
    }
    private List<Expr> parseArgsOpt() {
        List<Expr> args = new ArrayList<>();
        if (startsExpr(peek())) {
            args.add(parseExpr());
            while (isDelimiter(peek(), ",")) { advance(); args.add(parseExpr()); }
        }
        if (!isDelimiter(peek(), ")")) invalid("esperado um argumento, ',' ou ')'");
        return args;
    }
}
