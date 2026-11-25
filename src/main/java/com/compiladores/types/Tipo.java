package com.compiladores.types;

/**
 * Tipos da análise semântica (CP5).
 * Numéricos têm um posto (rank) que define o alargamento: int < long < float < double.
 * ERROR é o tipo de erro (compatível com tudo, evita cascata).
 * ANY só aparece no parâmetro da rotina pré-declarada print.
 */
public enum Tipo {
    INT("int", 0), LONG("long", 1), FLOAT("float", 2), DOUBLE("double", 3),
    BOOLEAN("boolean", -1), STRING("string", -1), VOID("void", -1),
    ERROR("<erro>", -1), ANY("qualquer", -1);

    public final String nome;
    private final int rank;

    Tipo(String nome, int rank) { this.nome = nome; this.rank = rank; }

    public boolean isNumeric() { return rank >= 0; }
    public boolean isIntegral() { return this == INT || this == LONG; }
    int rank() { return rank; }

    /** Converte o nome escrito no programa (Ast.Type.name) no tipo correspondente. */
    public static Tipo fromName(String name) {
        for (Tipo t : values()) if (t.nome.equals(name) && t != ERROR && t != ANY) return t;
        throw new IllegalArgumentException("tipo desconhecido: " + name);
    }

    @Override public String toString() { return nome; }
}
