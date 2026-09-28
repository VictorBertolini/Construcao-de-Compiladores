package com.compiladores.entity;

public class Token {
    private TokenType type;
    private String text;
    private int line;
    private int column;

    public Token(TokenType type, String text, int line, int column) {
        this.type = type;
        this.text = text;
        this.line = line;
        this.column = column;
    }

    public TokenType getType() {
        return type;
    }

    public String getText() {
        return text;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    @Override
    public String toString() {
        return type + "('" + text + "') [linha " + line + ", coluna " + column + "]";
    }
}

