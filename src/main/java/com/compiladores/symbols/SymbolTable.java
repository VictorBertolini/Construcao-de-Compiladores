package com.compiladores.symbols;

/** API da Aula 11: profundidade zero corresponde ao escopo global. */
public interface SymbolTable {
    void openScope();
    void closeScope();
    boolean declare(Symbol s);
    Symbol lookupLocal(String name);
    Symbol lookup(String name);
    int level();
}
