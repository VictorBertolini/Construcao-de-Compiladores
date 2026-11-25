package com.compiladores.types;

/**
 * Regras de tipagem do CP5 (as mesmas do papel tipos-a-mao.pdf).
 * Cada método indica a(s) regra(s) que implementa. Devolve null quando a regra recusa
 * a combinação; devolve ERROR quando algum operando já é ERROR (sem cascata).
 */
public final class TypeRules {
    private TypeRules() { }

    private static Tipo maior(Tipo a, Tipo b) { return a.rank() >= b.rank() ? a : b; }

    /** [ATRIB] / [ALARGA] — decisões 1 e 8: alargamento int → long → float → double. */
    public static boolean assignable(Tipo target, Tipo source) {
        if (target == Tipo.ERROR || source == Tipo.ERROR) return true;
        if (target == source) return target != Tipo.VOID;
        return target.isNumeric() && source.isNumeric() && source.rank() <= target.rank();
    }

    /** Argumento de chamada — [CHAMADA]; ANY (parâmetro de print) aceita qualquer valor. */
    public static boolean acceptsArgument(Tipo param, Tipo arg) {
        if (param == Tipo.ANY) return arg != Tipo.VOID;
        return assignable(param, arg);
    }

    /** Operadores binários — tabela de operadores. */
    public static Tipo binary(String op, Tipo l, Tipo r) {
        if (l == Tipo.ERROR || r == Tipo.ERROR) return Tipo.ERROR;
        switch (op) {
            case "+":                                   // [ARIT] e [CONCAT] (decisão 4)
                if (l.isNumeric() && r.isNumeric()) return maior(l, r);
                if (l == Tipo.STRING && r == Tipo.STRING) return Tipo.STRING;
                return null;
            case "-": case "*": case "/":               // [ARIT]
                return l.isNumeric() && r.isNumeric() ? maior(l, r) : null;
            case "%":                                   // [MOD]
                return l.isIntegral() && r.isIntegral() ? maior(l, r) : null;
            case "<": case ">": case "<=": case ">=":   // [REL] (string não, decisão 4)
                return l.isNumeric() && r.isNumeric() ? Tipo.BOOLEAN : null;
            case "equal":                               // [IGUAL] (decisão 5)
                if (l.isNumeric() && r.isNumeric()) return Tipo.BOOLEAN;
                if (l == r && (l == Tipo.BOOLEAN || l == Tipo.STRING)) return Tipo.BOOLEAN;
                return null;
            case "and": case "or":                      // [LOG]
                return l == Tipo.BOOLEAN && r == Tipo.BOOLEAN ? Tipo.BOOLEAN : null;
            default:
                return null;
        }
    }

    /** Operadores unários — [NOT] e [NEG]. */
    public static Tipo unary(String op, Tipo t) {
        if (t == Tipo.ERROR) return Tipo.ERROR;
        switch (op) {
            case "not": return t == Tipo.BOOLEAN ? Tipo.BOOLEAN : null;
            case "-": case "+": return t.isNumeric() ? t : null;
            default: return null;
        }
    }
}
