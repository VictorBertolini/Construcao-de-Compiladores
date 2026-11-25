package com.compiladores.symbols;

import com.compiladores.ast.Ast;
import com.compiladores.types.Tipo;
import java.util.List;
import java.util.Objects;

/** Uma declaração visível em algum ambiente léxico. */
public final class Symbol {
    public enum Category { VARIABLE, CONSTANT, PARAMETER, FUNCTION, PROCEDURE }

    public final String name;
    public final Category category;
    public final Ast.Node declaration;
    public final int level;

    /** Reservados para a análise de tipos (CP5) e alocação futura. */
    public Ast.Type type;
    public Integer offset;

    /** CP5: tipo do nome (variável/parâmetro) ou de retorno (função; VOID em procedimento). */
    public Tipo tipo;
    /** CP5: assinatura — tipos dos parâmetros; null para variáveis e parâmetros. */
    public List<Tipo> paramTypes;

    public Symbol(String name, Category category, Ast.Node declaration, int level, Ast.Type type) {
        this.name = Objects.requireNonNull(name, "nome do símbolo");
        this.category = Objects.requireNonNull(category, "categoria do símbolo");
        this.declaration = Objects.requireNonNull(declaration, "nó da declaração");
        this.level = level;
        this.type = type;
    }
}
