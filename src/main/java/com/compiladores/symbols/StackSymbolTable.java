package com.compiladores.symbols;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Pilha de mapas: o primeiro mapa criado é o ambiente global (nível zero). */
public final class StackSymbolTable implements SymbolTable {
    private final Deque<Map<String, Symbol>> scopes = new ArrayDeque<>();

    public StackSymbolTable() {
        scopes.push(new LinkedHashMap<>());
    }

    @Override
    public void openScope() {
        scopes.push(new LinkedHashMap<>());
    }

    /** Nunca fecha o escopo global; lança exceção explicativa se solicitado. */
    @Override
    public void closeScope() {
        if (scopes.size() == 1) {
            throw new IllegalStateException("não é permitido fechar o escopo global (nível 0)");
        }
        scopes.pop();
    }

    @Override
    public boolean declare(Symbol s) {
        Objects.requireNonNull(s, "símbolo a declarar");
        Map<String, Symbol> current = scopes.peek();
        if (current.containsKey(s.name)) return false;
        current.put(s.name, s);
        return true;
    }

    @Override
    public Symbol lookupLocal(String name) {
        return scopes.peek().get(name);
    }

    @Override
    public Symbol lookup(String name) {
        for (Map<String, Symbol> scope : scopes) {
            Symbol found = scope.get(name);
            if (found != null) return found;
        }
        return null;
    }

    @Override
    public int level() {
        return scopes.size() - 1;
    }
}
